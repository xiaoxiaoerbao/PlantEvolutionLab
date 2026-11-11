package start;

import dutils.Range;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evaluates candidate regions against independent Q20/Q0 depth files.
 * The dominant heap allocations are the current configuration's BED/quantile arrays,
 * one endpoint-sort chunk and a fixed index cache, plus parameter/run metadata.
 * Neither all configurations' ranges nor full depth records are retained.
 */
public final class StatisticsCalculator {
    public static final double DEFAULT_BASELINE_XP56 = 19.20; // CARMEN
    public static final double DEFAULT_BASELINE_XP57 = 19.93; // SN055525
    private final double baseline56;
    private final double baseline57;
    private final int sortChunkInts;
    private final int cacheMiB;

    public StatisticsCalculator() {
        this(DEFAULT_BASELINE_XP56, DEFAULT_BASELINE_XP57);
    }

    public StatisticsCalculator(double baseline56, double baseline57) {
        this(baseline56, baseline57, Integer.getInteger("statistics.sortChunkInts", 1_048_576),
                Integer.getInteger("statistics.cacheMiB", 64));
    }

    public StatisticsCalculator(double baseline56, double baseline57, int sortChunkInts, int cacheMiB) {
        if (!Double.isFinite(baseline56) || baseline56 <= 0 || !Double.isFinite(baseline57) || baseline57 <= 0) {
            throw new IllegalArgumentException("Normalization baselines must be finite and positive");
        }
        if (sortChunkInts < 2 || cacheMiB < 1 || cacheMiB > 4096) {
            throw new IllegalArgumentException("sortChunkInts must be >= 2; cacheMiB must be between 1 and 4096");
        }
        this.baseline56 = baseline56;
        this.baseline57 = baseline57;
        this.sortChunkInts = sortChunkInts;
        this.cacheMiB = cacheMiB;
    }

    public record Output(Path rangeStatistics, Path configurationStatistics, Path metadata,
                         long configurationCount, long rangeCount, long q20Records, long q0Records, long indexedEndpoints) {}

    /**
     * Publishes completed files into a new or existing directory. If any result filename
     * is already present, publishes a unique run subdirectory instead of overwriting it.
     * Existing files are preserved. Temporary prefix/sort files are deleted on success or failure.
     * Only parameters need to be loaded in configs; addRegions() is deliberately unnecessary.
     */
    public Output calculateAndWrite(Configurations configs, File regionsDir, File q20File, File q0File,
                                    Path outputDirectory) throws IOException {
        if (configs.config_id == null || configs.genomeType == null) {
            throw new IllegalStateException("Build configurations before calculating statistics");
        }
        Path output = outputDirectory.toAbsolutePath().normalize();
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(output)) {
            throw new FileAlreadyExistsException("Output path is not a directory: " + output);
        }
        Files.createDirectories(output.getParent());
        Path stage = Files.createTempDirectory(output.getParent(), ".statistics-");
        try {
            Path work = Files.createDirectory(stage.resolve("work"));
            long rangeCount = 0;
            long q20Records, q0Records, indexedEndpoints;
            try (EndpointDepthIndex index = EndpointDepthIndex.build(configs, regionsDir, q20File, q0File,
                    work, sortChunkInts, cacheMiB);
                 BufferedWriter ranges = writer(stage.resolve("range_statistics.tsv"));
                 BufferedWriter summaries = writer(stage.resolve("configuration_statistics.tsv"))) {
                q20Records = index.q20Records();
                q0Records = index.q0Records();
                indexedEndpoints = index.endpointCount();
                ranges.write(RangeStatistics.HEADER);
                summaries.write(ConfigurationStatistics.HEADER);
                for (int i = 0; i < configs.config_id.length; i++) {
                    rangeCount += evaluateConfiguration(configs, i, regionsDir, index, ranges, summaries);
                    if (i % 1000 == 0 || i == configs.config_id.length - 1) {
                        System.out.println("Evaluated configurations: " + (i + 1) + "/" + configs.config_id.length
                                + "; ranges written: " + rangeCount);
                    }
                }
            }
            Files.writeString(stage.resolve("README.txt"), metadata(q20File, q0File, regionsDir,
                    configs.config_id.length, rangeCount, q20Records, q0Records, indexedEndpoints), StandardCharsets.UTF_8);
            deleteTree(work);
            Path published = publish(stage, output);
            return new Output(published.resolve("range_statistics.tsv"), published.resolve("configuration_statistics.tsv"),
                    published.resolve("README.txt"), configs.config_id.length, rangeCount, q20Records, q0Records, indexedEndpoints);
        } catch (IOException | RuntimeException | Error failure) {
            try { deleteTree(stage); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /** Moves only this run's files; an existing empty directory need not be deleted/replaced. */
    static Path publish(Path stage, Path output) throws IOException {
        if (!Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.move(stage, output);
                return output;
            } catch (FileAlreadyExistsException concurrentDirectory) {
                // Another process may have created it while the calculation was running.
            }
        }
        if (!Files.isDirectory(output)) throw new FileAlreadyExistsException("Output path is not a directory: " + output);
        List<String> filenames = List.of("range_statistics.tsv", "configuration_statistics.tsv", "README.txt");
        for (String filename : filenames) {
            if (Files.exists(output.resolve(filename), LinkOption.NOFOLLOW_LINKS)) return publishNewRun(stage, output);
        }
        List<String> moved = new ArrayList<>();
        try {
            for (String filename : filenames) {
                Files.move(stage.resolve(filename), output.resolve(filename));
                moved.add(filename);
            }
            Files.delete(stage);
            return output;
        } catch (IOException failure) {
            // Restore partial moves on failure; never remove any pre-existing output files.
            for (int i = moved.size() - 1; i >= 0; i--) {
                String filename = moved.get(i);
                try { Files.move(output.resolve(filename), stage.resolve(filename)); }
                catch (IOException rollback) { failure.addSuppressed(rollback); }
            }
            if (failure instanceof FileAlreadyExistsException && failure.getSuppressed().length == 0) {
                return publishNewRun(stage, output);
            }
            throw failure;
        }
    }

    private static Path publishNewRun(Path stage, Path output) throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path run = output.resolve("run-" + stamp + "-" + UUID.randomUUID());
        Files.move(stage, run);
        System.out.println("Existing result files preserved; writing this run to: " + run);
        return run;
    }

    private long evaluateConfiguration(Configurations configs, int i, File regionsDir, EndpointDepthIndex index,
                                       BufferedWriter ranges, BufferedWriter summaries) throws IOException {
        Configurations.RegionSet inputs = Configurations.readRegionSet(new File(regionsDir, configs.config_id[i]));
        GapIndex gaps = new GapIndex(inputs.gaps());
        ConfigurationStatistics.Accumulator aggregate = new ConfigurationStatistics.Accumulator(
                configs.config_id[i], configs.genomeType[i], inputs.regions().length);
        for (int j = 0; j < inputs.regions().length; j++) {
            Range range = inputs.regions()[j];
            RangeStatistics statistics = index.query(configs.config_id[i], configs.genomeType[i], j + 1,
                    range, gaps.length(range), baseline56, baseline57);
            statistics.writeTsv(ranges);
            aggregate.add(statistics);
        }
        aggregate.finish().writeTsv(summaries, configs, i);
        return inputs.regions().length;
    }

    private String metadata(File q20, File q0, File regionsDir, int configs, long ranges, long records20, long records0, long endpoints) {
        return "Candidate target-region evaluation\n"
                + "Q20 input: " + q20.getAbsolutePath() + "\nQ0 input: " + q0.getAbsolutePath()
                + "\nRegions directory: " + regionsDir.getAbsolutePath()
                + "\nConfigurations: " + configs + "\nRanges: " + ranges
                + "\nQ20 records: " + records20 + "\nQ0 records: " + records0 + "\nUnique endpoints: " + endpoints
                + "\nXP56 = CARMEN; baseline = " + baseline56 + "\nXP57 = SN055525; baseline = " + baseline57
                + "\nDepth inputs are observed evaluation-subset depths; multiplied by 2 once on reading."
                + "\nQ20 and Q0 depth sums in range_statistics.tsv are both equivalent-depth sums; the factor cancels in U."
                + "\nOmitted positions/chromosomes have zero depth. All interval means/breadths use the full BED length."
                + "\nBED coordinates: 0-based [start,end); range_index: 1-based original final.bed record order."
                + "\nRows follow parameter-file order, then original BED order."
                + "\nB5/B10 and Y10 use equivalent depth >= 5 / >= 10 (observed depth >= 3 / >= 5)."
                + "\nJoint breadth counts bases where BOTH parents meet the threshold."
                + "\nAB/A: A=abs(log2((z_XP56+0.05)/(z_XP57+0.05))); u=min(U_XP56,U_XP57)."
                + "\nD: positive=XP57, negative=XP56. R: positive=XP56, negative=XP57."
                + "\nD/R: T=log2((z_positive+0.05)/(z_negative+0.05)); u=U_positive."
                + "\nZero Q0 denominator makes parental U undefined. AB u requires both U values; D/R requires positive U only."
                + "\nU is the exact depth-sum ratio, without clipping. q20_gt_q0 flags numerator > denominator for each parent."
                + "\nu_gt1 / n_u_gt1 flag finite regional u > 1. Such values remain in U10; investigate Q20/Q0 input consistency."
                + "\nU10 excludes invalid u values; invalid_U_fraction = invalid range count / all range count."
                + "\nQuantiles are unweighted across ranges, type 7: h=(n-1)*p, linear interpolation."
                + "\nC10, B1_negative and G are length-weighted ratios. B1_negative uses equivalent depth >= 1."
                + "\nCore and gap intervals must partition final intervals exactly. G=sum(gap lengths)/sum(final lengths)."
                + "\nNA means undefined or inapplicable. Empty configurations have zero counts/length/yield and NA ratios/quantiles."
                + "\nNo parameter optimization/selection (Methods S4) is performed."
                + "\nIndex cache limit: " + cacheMiB + " MiB; external-sort chunk: " + sortChunkInts + " integers."
                + "\nHeap: fixed cache + sort chunk + one configuration's BED/quantile arrays; no full depth arrays."
                + "\nTemporary disk prefix tables: approximately 68 bytes per unique endpoint, plus sort work files; removed after completion.\n";
    }

    private static BufferedWriter writer(Path path) throws IOException {
        return new BufferedWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), 256 * 1024);
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) throw exception;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Gap prefix lengths support unsorted BED records without a quadratic overlap loop. */
    private static final class GapIndex {
        private record ChromosomeGaps(int[] starts, int[] ends, long[] prefix) {
            long prefixAt(int boundary) {
                int low = 0, high = ends.length;
                while (low < high) {
                    int mid = (low + high) >>> 1;
                    if (ends[mid] <= boundary) low = mid + 1;
                    else high = mid;
                }
                long result = prefix[low];
                if (low < starts.length && starts[low] < boundary) result += boundary - starts[low];
                return result;
            }
        }
        private final Map<String, ChromosomeGaps> chromosomes = new HashMap<>();

        GapIndex(Range[] gaps) {
            Map<String, List<Range>> grouped = new HashMap<>();
            for (Range gap : gaps) grouped.computeIfAbsent(gap.chr(), ignored -> new ArrayList<>()).add(gap);
            for (Map.Entry<String, List<Range>> entry : grouped.entrySet()) {
                List<Range> sorted = entry.getValue();
                sorted.sort(Comparator.comparingInt(Range::start));
                int[] starts = new int[sorted.size()], ends = new int[sorted.size()];
                long[] prefix = new long[sorted.size() + 1];
                for (int i = 0; i < sorted.size(); i++) {
                    starts[i] = sorted.get(i).start();
                    ends[i] = sorted.get(i).end();
                    prefix[i + 1] = prefix[i] + sorted.get(i).length();
                }
                chromosomes.put(entry.getKey(), new ChromosomeGaps(starts, ends, prefix));
            }
        }

        int length(Range range) {
            ChromosomeGaps gaps = chromosomes.get(range.chr());
            return gaps == null ? 0 : Math.toIntExact(gaps.prefixAt(range.end()) - gaps.prefixAt(range.start()));
        }
    }
}
