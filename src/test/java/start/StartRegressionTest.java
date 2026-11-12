package start;

import dutils.Chromosome;
import dutils.Range;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPOutputStream;

/** Standalone regression checks; run with the main and test classes plus the Maven dependencies on the classpath. */
public class StartRegressionTest {
    private static final String HEADER = "config_id\ttype\talpha\tbeta\tdelta\tgamma\tlambda\trho\tratio\n";
    private static int checks;
    private static Path root;

    public static void main(String[] args) throws Exception {
        root = Files.createTempDirectory("plant-start-test-");
        try {
            testDepths();
            testConfigurationsAndEntryPoint();
            System.out.println("Regression checks passed: " + checks);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void testDepths() throws Exception {
        PosDepth single = new PosDepth(file("single.tsv", "1A\t10\t4\t5\n1A\t20\t6\t7\n"));
        check(single.getTotalPositionCount() == 2, "flush final chromosome");
        check(single.getXp56Depth("1A", 20) == 12 && single.getXp57Depth("1A", 20) == 14, "equivalent depths are doubled on input");
        check(single.xp56_depth[Chromosome.A1.ordinal()][0] == 8, "stored arrays contain equivalent depth");
        check(single.getPositionCount(Chromosome.A1) == 2, "enum count");
        check(single.getPositionIndex("1A", 15) == -1, "sparse position");
        check(single.getXp56Depth("1A", 15) == 0 && single.getXp57Depth("1A", 15) == 0, "omitted positions have zero depth");
        check(single.getXp56Depth("1B", 1) == 0, "omitted known chromosome has zero depth");
        check(single.getXp56Depth("missing", 1) == -1, "unknown query");
        check(single.getPositionCount("missing") == 0, "unknown count");
        expect(IllegalArgumentException.class, () -> single.getPositionIndex("1A", 0), "positive");
        expect(IllegalArgumentException.class, () -> single.getPositionIndex("1A", -1), "positive");
        expect(IllegalArgumentException.class, () -> single.getXp56Depth("1A", 0), "positive");
        expect(IllegalArgumentException.class, () -> single.getXp57Depth("missing", -1), "positive");

        String content = "\n1B\t10\t11\t12\n1B\t20\t21\t22\n\n"
                + "1A\t1\t31\t32\n1D\t2\t41\t42\nUn\t3\t51\t52\ncontigX\t4\t61\t62\n";
        PosDepth multi = new PosDepth(file("multi.tsv", content));
        check(multi.getTotalPositionCount() == 6, "all blocks counted");
        check(Arrays.equals(multi.ref_pos[Chromosome.B1.ordinal()], new int[] {10, 20}), "isolated first block");
        check(Arrays.equals(multi.ref_pos[Chromosome.A1.ordinal()], new int[] {1}), "single row middle block");
        check(multi.getXp56Depth("1A", 1) == 62 && multi.getXp57Depth("1D", 2) == 84, "transition first rows");
        check(multi.getChromosomeIndex("1A") == Chromosome.A1.ordinal(), "stable known indices");
        check(multi.getChromosomeIndex("Un") == Chromosome.values().length, "extra contig index");
        check(multi.getPositionCount("Un") == 1 && multi.getXp57Depth("Un", 3) == 104, "extra contig lookup");
        check(multi.getXp56Depth("Un", 4) == 0, "omitted site on a recorded extra contig has zero depth");
        check(multi.getXp56Depth("contigX", 4) == 122, "extra final block");
        check(multi.getPositionCount(Chromosome.A2) == 0, "absent chromosome");
        String[] names = multi.getChromosomeNames();
        names[0] = "broken";
        check(multi.getChromosomeIndex("1A") == 0, "defensive names copy");

        Path gz = root.resolve("depth.tsv.gz");
        try (var out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        PosDepth compressed = new PosDepth(gz.toFile());
        check(compressed.getTotalPositionCount() == 6 && compressed.getXp56Depth("Un", 3) == 102, "gzip equivalent depths");
        PosDepth empty = new PosDepth(file("empty.tsv", "\n \t\n"));
        check(empty.getTotalPositionCount() == 0, "empty depth file");

        String[] malformed = {
                "1A\t0\t1\t1\n", "1A\t-1\t1\t1\n", "1A\t1\t-1\t1\n",
                "1A\t1\t1\t-1\n", "1A\t1\t\t1\n", "1A\t1\t1\n",
                "1A\t1\t1\t1\textra\n", "\t1\t1\t1\n", "1A\t2147483648\t1\t1\n",
                "1A\t20\t1\t1\n1A\t10\t1\t1\n", "1A\t1\t1\t1\n1A\t1\t1\t1\n",
                "1A\t1\t1\t1\n1B\t1\t1\t1\n1A\t2\t1\t1\n",
                "Un\t1\t1\t1\n1A\t1\t1\t1\nUn\t2\t1\t1\n",
                "1A\t1\t1073741824\t1\n", "1A\t1\t1\t2147483647\n"
        };
        int[][] originalPositions = multi.ref_pos;
        for (int i = 0; i < malformed.length; i++) {
            File input = file("bad-depth-" + i + ".tsv", malformed[i]);
            expect(IllegalArgumentException.class, () -> multi.buildFromDepthFile(input), input.getAbsolutePath() + ":");
            check(multi.ref_pos == originalPositions && multi.getXp57Depth("Un", 3) == 104, "failed depth read preserves data");
        }
        PosDepth max = new PosDepth(file("max-depth.tsv", "1A\t1\t1073741823\t0\n"));
        check(max.getXp56Depth("1A", 1) == 2147483646, "largest representable equivalent depth");
        multi.buildFromDepthFile(file("replacement.tsv", "1A\t5\t0\t0\n"));
        check(multi.getTotalPositionCount() == 1 && multi.getChromosomeIndex("Un") == -1, "successful replacement resets contigs");
        check(multi.getXp56Depth("1A", 5) == 0, "zero depth is valid");
    }

    private static void testConfigurationsAndEntryPoint() throws Exception {
        File parameters = file("parameters.tsv", "\uFEFF# parameters\n\n" + HEADER
                + "AB_test\tAB\t6\tNA\t0.2\t100\t60\t0\t0\n"
                + "D_test\tD\t6\t0.25\tNA\t100\t60\t12\t0.2\n");
        Configurations configs = new Configurations();
        expect(IllegalStateException.class, () -> configs.addRegions(root.toFile()), "Build configurations");
        StatisticsCalculator calculator = new StatisticsCalculator(19.20, 19.93, 2, 1);
        File depth = file("entry-depth.tsv", "1A\t1\t1\t2\n1D\t2\t3\t4\n");
        expect(IllegalStateException.class, () -> calculator.calculateAndWrite(configs,
                root.resolve("regions").toFile(), depth, depth, root.resolve("unbuilt-statistics")), "Build configurations");
        configs.buildFromParameterFile(parameters);
        check(configs.config_id.length == 2, "configuration count");
        check(configs.beta[0] == -1.0 && configs.beta[1] == 0.25, "fractional beta and NA");
        check(configs.delta[0] == 0.2 && configs.delta[1] == -1.0, "delta and NA");

        String bed = "\n# comment\ntrack name=regions\nbrowser position 1A:1-10\n1A\t0\t2\tname\n1A 5 7\n";
        File bedA = file("regions/AB_test/final.bed", bed);
        File bedD = file("regions/D_test/final.bed", "1D\t0\t3\n");
        String coresA = "1A\t5\t7\n1A\t0\t1\n";
        String gapsA = "1A\t1\t2\n";
        File coreA = file("regions/AB_test/core.bed", coresA);
        File gapA = file("regions/AB_test/gap.bed", gapsA);
        file("regions/D_test/core.bed", "1D\t0\t3\n");
        file("regions/D_test/gap.bed", "");
        // The current statistics path reads BED files itself, without addRegions() or full depth arrays.
        check(Arrays.stream(configs.ranges).allMatch(ranges -> ranges == null), "parameters alone do not load regions");
        var sparse = calculator.calculateAndWrite(configs, root.resolve("regions").toFile(), depth, depth,
                root.resolve("sparse-statistics"));
        check(sparse.configurationCount() == 2 && sparse.rangeCount() == 3, "sparse depth cross-file evaluation");
        check(sparse.q20Records() == 2 && sparse.q0Records() == 2, "sparse depth inputs are streamed");
        check(Arrays.stream(configs.ranges).allMatch(ranges -> ranges == null)
                && Arrays.stream(configs.coreRanges).allMatch(ranges -> ranges == null)
                && Arrays.stream(configs.gapRanges).allMatch(ranges -> ranges == null),
                "statistics calculation does not populate configuration region arrays");
        File allZero = file("all-zero.tsv", "");
        var zero = calculator.calculateAndWrite(configs, root.resolve("regions").toFile(), allZero, allZero,
                root.resolve("zero-statistics"));
        check(zero.rangeCount() == 3 && zero.q20Records() == 0 && zero.q0Records() == 0,
                "all-zero known chromosomes are valid");
        try (var lines = Files.lines(zero.rangeStatistics())) {
            check(lines.skip(1).allMatch(line -> {
                String[] fields = line.split("\t");
                return fields[8].equals("0") && fields[9].equals("0")
                        && fields[10].equals("0") && fields[11].equals("0");
            }), "omitted chromosomes produce zero depth sums");
        }
        configs.addRegions(root.resolve("regions").toFile());
        check(configs.ranges[0].length == 2 && configs.ranges[1].length == 1, "BED special lines and config order");
        check(configs.ranges[0][0].equals(new Range("1A", 0, 2)), "BED retains 0-based half-open coordinates");
        check(configs.ranges[0][1].equals(new Range("1A", 5, 7)), "BED order and whitespace");
        check(configs.coreRanges[0][0].equals(new Range("1A", 5, 7)), "core BED order is preserved");
        check(configs.gapRanges[0][0].equals(new Range("1A", 1, 2)), "gap intervals are loaded");
        check(configs.gapRanges[1].length == 0, "empty gap file is valid");

        Range[][] originalRanges = configs.ranges;
        Range[][] originalCores = configs.coreRanges;
        Range[][] originalGaps = configs.gapRanges;
        double[] originalBeta = configs.beta;
        String validRow = "AB_test\tAB\t6\t0.0\t0.2\t100\t60\t0\t0\n";
        String[] badParameters = {
                "", HEADER, validRow,
                HEADER + validRow + validRow,
                HEADER + validRow.replace("\t0.0\t", "\t\t"),
                HEADER + validRow.replace("\t0.0\t", "\tNaN\t"),
                HEADER + validRow.replace("\t0.0\t", "\tInfinity\t"),
                HEADER + validRow.replace("\t0.0\t", "\t-0.5\t"),
                HEADER + validRow.replace("\t6\t", "\t-6\t"),
                HEADER + validRow.replace("\tAB\t", "\tUNKNOWN\t"),
                HEADER + validRow.replace("\t0\t0\n", "\t0\n")
        };
        for (int i = 0; i < badParameters.length; i++) {
            File input = file("bad-parameters-" + i + ".tsv", badParameters[i]);
            expect(IllegalArgumentException.class, () -> configs.buildFromParameterFile(input), input.getAbsolutePath());
            check(configs.beta == originalBeta && configs.ranges == originalRanges, "failed parameter read preserves data");
        }

        for (String badBed : new String[] {"1A\t\t2\n", "\t0\t2\n", "1A 0\n", "1A -1 2\n", "1A 2 2\n", "1A 3 2\n"}) {
            Files.writeString(bedA.toPath(), badBed);
            expect(IllegalArgumentException.class, () -> configs.addRegions(root.resolve("regions").toFile()), bedA.getAbsolutePath() + ":1");
            check(configs.ranges == originalRanges, "failed BED read preserves all regions");
            check(configs.coreRanges == originalCores && configs.gapRanges == originalGaps, "failed BED read preserves cores and gaps");
        }
        Files.delete(bedD.toPath());
        expect(RuntimeException.class, () -> configs.addRegions(root.resolve("regions").toFile()), "final.bed");
        check(configs.ranges == originalRanges, "missing BED preserves regions");
        file("regions/AB_test/final.bed", bed);
        file("regions/D_test/final.bed", "1D\t0\t3\n");
        for (String badCore : new String[] {"", "1A\t0\t2\n1A\t5\t7\n", "1A\t0\t1\n1A\t5\t8\n", coresA + "1B\t0\t1\n"}) {
            Files.writeString(coreA.toPath(), badCore);
            expect(IllegalArgumentException.class, () -> configs.addRegions(root.resolve("regions").toFile()), "core.bed");
            check(configs.ranges == originalRanges && configs.coreRanges == originalCores
                    && configs.gapRanges == originalGaps, "partition failure preserves all region arrays");
        }
        Files.writeString(coreA.toPath(), coresA);
        Files.writeString(gapA.toPath(), "1A\t1\t2\n1A\t1\t2\n");
        expect(IllegalArgumentException.class, () -> configs.addRegions(root.resolve("regions").toFile()), "partition");
        Files.writeString(gapA.toPath(), gapsA);
        Files.delete(gapA.toPath());
        expect(RuntimeException.class, () -> configs.addRegions(root.resolve("regions").toFile()), "gap.bed");
        file("regions/AB_test/gap.bed", gapsA);

        Configurations integral = new Configurations();
        integral.buildFromParameterFile(file("integral-beta.tsv", HEADER + validRow));
        check(integral.beta[0] == 0.0, "beta 0.0 parses");
        File q0 = file("entry-q0-depth.tsv", "1A\t1\t2\t3\n1A\t2\t1\t1\n1D\t2\t4\t5\n");
        // Chromosome validation now uses the BED on disk and the configuration's genome type.
        for (String chromosome : new String[] {"missing", "1A"}) {
            Files.writeString(bedD.toPath(), chromosome + "\t0\t3\n");
            Path rejected = root.resolve("rejected-" + chromosome);
            expect(IllegalArgumentException.class, () -> Start.main(new String[] {depth.toString(), q0.toString(),
                    parameters.toString(), root.resolve("regions").toString(), rejected.toString()}),
                    "D_test: chromosome " + chromosome + " does not belong");
            check(!Files.exists(rejected), "invalid chromosome produces no output");
        }
        Files.writeString(bedD.toPath(), "1D\t0\t3\n");
        Path entryOutput = root.resolve("entry-statistics");
        Start.main(new String[] {depth.toString(), q0.toString(), parameters.toString(), root.resolve("regions").toString(), entryOutput.toString()});
        check(Files.readAllLines(entryOutput.resolve("range_statistics.tsv")).size() == 4,
                "entry point writes three range rows");
        check(Files.readAllLines(entryOutput.resolve("configuration_statistics.tsv")).size() == 3,
                "entry point writes two configuration rows");
        check(Files.readString(entryOutput.resolve("README.txt")).contains("Q20 records: 2"),
                "entry point writes run metadata");
        Path emptyOutput = root.resolve("empty-statistics");
        Start.main(new String[] {root.resolve("empty.tsv").toString(), q0.toString(), parameters.toString(), root.resolve("regions").toString(), emptyOutput.toString()});
        check(Files.readString(emptyOutput.resolve("README.txt")).contains("Q20 records: 0"),
                "entry point accepts empty Q20 input");
        expect(RuntimeException.class, () -> Start.main(new String[] {root.resolve("entry-depth.tsv").toString(), root.resolve("missing-q0.tsv").toString(), parameters.toString(), root.resolve("regions").toString(), root.resolve("failed-statistics").toString()}), "missing-q0.tsv");
        expect(IllegalArgumentException.class, () -> Start.main(new String[] {"q20", "parameters", "regions"}), "Usage:");
        expect(IllegalArgumentException.class, () -> Start.main(new String[] {"only-one-path"}), "Usage:");
    }

    private static File file(String name, String content) throws IOException {
        Path path = root.resolve(name);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path.toFile();
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static void expect(Class<? extends Throwable> type, Action action, String message) throws Exception {
        try {
            action.run();
        } catch (Throwable e) {
            if (!type.isInstance(e)) throw new AssertionError("Expected " + type.getSimpleName() + ", got " + e, e);
            check(e.getMessage() != null && e.getMessage().contains(message), "Error context: " + e);
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " containing " + message);
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }
}
