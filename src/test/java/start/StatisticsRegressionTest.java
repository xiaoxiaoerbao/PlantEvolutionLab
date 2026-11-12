package start;

import dutils.Range;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/** Independent base-by-base oracle, disk-cache/sort stress, and end-to-end output checks. */
public class StatisticsRegressionTest {
    private static final String HEADER = "config_id\ttype\talpha\tbeta\tdelta\tgamma\tlambda\trho\n";
    private static Path root;
    private static int checks;
    private record Spec(String id, String type, List<Range> ranges, Map<Range, Integer> gaps) {}

    public static void main(String[] args) throws Exception {
        root = Files.createTempDirectory("plant-statistics-test-");
        try {
            relativeOutputPaths();
            randomizedOracle();
            cacheEvictionAndLargeCoordinates();
            zeroAndFailureCases();
            System.out.println("Statistics regression checks passed: " + checks);
        } finally {
            StatisticsCalculator.deleteTree(root);
        }
    }

    private static void relativeOutputPaths() throws Exception {
        Path base = Files.createDirectory(root.resolve("relative"));
        File parameters = parameters(base, List.of(new Spec("relative", "AB", List.of(new Range("1A", 0, 2)), Map.of())));
        File depth = file(base.resolve("depth.tsv"), "1A\t1\t4\t6\n");
        var calculator = new StatisticsCalculator(2, 3, 2, 1);
        // A one-component path has no lexical parent, regardless of whether it already exists.
        Path relative = Path.of("statistics-relative-" + java.util.UUID.randomUUID());
        Path absolute = relative.toAbsolutePath().normalize();
        try {
            check(relative.getParent() == null, "fixture reproduces a single-component relative output path");
            var result = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), depth, depth, relative);
            check(result.rangeStatistics().getParent().equals(absolute), "single-name relative output resolves against working directory");
            number(table(result.rangeStatistics()).getFirst(), "z_xp56", 2);
            Files.writeString(absolute.resolve("notes.txt"), "keep");
            calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), depth, depth, relative);
            check(Files.readString(absolute.resolve("notes.txt")).equals("keep"), "existing relative output preserves unrelated files");
            Path nested = relative.resolve("new-parent/nested-out");
            var nestedResult = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), depth, depth, nested);
            check(nestedResult.rangeStatistics().getParent().equals(nested.toAbsolutePath().normalize()),
                    "nested relative output creates missing parent directories");
        } finally {
            StatisticsCalculator.deleteTree(absolute);
        }
    }

    private static void randomizedOracle() throws Exception {
        Random random = new Random(481921);
        Map<String, int[][]> q20 = new LinkedHashMap<>(), q0 = new LinkedHashMap<>();
        for (String chr : List.of("1A", "1B", "1D", "1R")) {
            int[][] a = new int[2][256], b = new int[2][256];
            for (int parent = 0; parent < 2; parent++) for (int p = 0; p < 240; p++) {
                // Leave an entirely uncovered tail; negative parents have zero Q0 as well.
                boolean negative = chr.equals("1D") && parent == 0 || chr.equals("1R") && parent == 1;
                a[parent][p] = negative ? 0 : random.nextInt(8);
                b[parent][p] = negative ? 0 : a[parent][p] + random.nextInt(4);
            }
            q20.put(chr, a);
            q0.put(chr, b);
        }
        List<Spec> specs = new ArrayList<>();
        for (String type : List.of("AB", "AB", "D", "R")) {
            List<String> chromosomes = type.equals("AB") ? List.of("1A", "1B", "2A") : List.of("1" + type);
            List<Range> ranges = new ArrayList<>();
            Map<Range, Integer> gaps = new HashMap<>();
            for (String chr : chromosomes) for (int j = 0; j < 32; j++) {
                int length = 1 + random.nextInt(6);
                Range range = new Range(chr, j * 8, j * 8 + length);
                ranges.add(range);
                gaps.put(range, length >= 3 ? 1 : 0);
            }
            Collections.shuffle(ranges, random);
            specs.add(new Spec(type + "_" + specs.size(), type, ranges, gaps));
        }
        specs.add(new Spec("empty", "AB", List.of(), Map.of()));
        Path base = Files.createDirectory(root.resolve("oracle"));
        File parameters = parameters(base, specs);
        File depth20 = depthFile(base.resolve("q20.tsv"), q20, List.of("1A", "1B", "1D", "1R"));
        File depth0 = depthFile(base.resolve("q0.tsv"), q0, List.of("1R", "1D", "1B", "1A"));
        Configurations configs = load(parameters);
        StatisticsCalculator calculator = new StatisticsCalculator(19.20, 19.93, 7, 1);
        StatisticsCalculator.Output result = calculator.calculateAndWrite(configs, base.resolve("regions").toFile(),
                depth20, depth0, base.resolve("out"));
        check(configs.ranges[0] == null, "calculator does not load all configurations into Range[][]");
        List<Map<String, String>> rows = table(result.rangeStatistics());
        List<Map<String, String>> summaries = table(result.configurationStatistics());
        int rowIndex = 0;
        for (int c = 0; c < specs.size(); c++) {
            Spec spec = specs.get(c);
            List<Double> scores = new ArrayList<>(), validUs = new ArrayList<>();
            long total = 0, gapTotal = 0, yield = 0, negative = 0;
            for (int i = 0; i < spec.ranges.size(); i++) {
                Range range = spec.ranges.get(i);
                Map<String, Double> expected = brute(spec, range, q20.get(range.chr()), q0.get(range.chr()), 19.20, 19.93);
                Map<String, String> row = rows.get(rowIndex++);
                check(row.get("config_id").equals(spec.id), "configuration order");
                check(row.get("chr").equals(range.chr()), "original chromosome order");
                number(row, "range_index", i + 1);
                number(row, "start", range.start());
                number(row, "end", range.end());
                for (var entry : expected.entrySet()) number(row, entry.getKey(), entry.getValue());
                boolean valid = Double.isFinite(expected.get("u"));
                check(row.get("u_valid").equals(Boolean.toString(valid)), "u validity flag");
                scores.add(expected.get(spec.type.equals("AB") ? "A" : "T"));
                if (valid) validUs.add(expected.get("u"));
                total += range.length();
                gapTotal += spec.gaps.get(range);
                yield += Math.round(expected.get(spec.type.equals("AB") ? "B10_joint"
                        : spec.type.equals("D") ? "B10_xp57" : "B10_xp56") * range.length());
                if (!spec.type.equals("AB")) negative += Math.round(expected.get("B1_negative") * range.length());
            }
            Map<String, String> row = summaries.get(c);
            number(row, "n_regions", spec.ranges.size());
            number(row, "L_total_bp", total);
            number(row, "L_gap_bp", gapTotal);
            number(row, spec.type.equals("AB") ? "Y10_joint_bp" : "Y10_positive_bp", yield);
            number(row, spec.type.equals("AB") ? "Y10_positive_bp" : "Y10_joint_bp", Double.NaN);
            number(row, "C10", total == 0 ? Double.NaN : (double) yield / total);
            number(row, "G", total == 0 ? Double.NaN : (double) gapTotal / total);
            number(row, "A90", spec.type.equals("AB") ? quantile(scores, .9) : Double.NaN);
            number(row, "T10", spec.type.equals("AB") ? Double.NaN : quantile(scores, .1));
            number(row, "U10", quantile(validUs, .1));
            number(row, "n_invalid_U", spec.ranges.size() - validUs.size());
            number(row, "invalid_U_fraction", spec.ranges.isEmpty() ? Double.NaN
                    : (double) (spec.ranges.size() - validUs.size()) / spec.ranges.size());
            number(row, "B1_negative", spec.type.equals("AB") || total == 0 ? Double.NaN : (double) negative / total);
        }
        check(rowIndex == rows.size() && summaries.size() == specs.size(), "exact output row counts");
        check(result.rangeCount() == rows.size(), "reported output count");
        check(!Files.exists(base.resolve("out/work")), "temporary index cleaned up");
        check(Files.readString(result.metadata()).contains("XP56 = CARMEN"), "parent metadata");

        Path gz = base.resolve("q0.tsv.gz");
        try (var out = new GZIPOutputStream(Files.newOutputStream(gz))) { Files.copy(depth0.toPath(), out); }
        var compressed = calculator.calculateAndWrite(configs, base.resolve("regions").toFile(), depth20, gz.toFile(), base.resolve("gzip"));
        check(Files.mismatch(result.rangeStatistics(), compressed.rangeStatistics()) == -1, "gzip and text give identical range statistics");
        check(Files.mismatch(result.configurationStatistics(), compressed.configurationStatistics()) == -1, "gzip and text give identical summaries");
    }

    private static Map<String, Double> brute(Spec spec, Range range, int[][] d20, int[][] d0, double a56, double a57) {
        long[] sum = new long[4];
        int[] b5 = new int[2], b10 = new int[2], b1 = new int[2];
        int joint5 = 0, joint10 = 0;
        for (int p = range.start(); p < range.end(); p++) {
            int x = d20 == null ? 0 : 2 * d20[0][p], y = d20 == null ? 0 : 2 * d20[1][p];
            sum[0] += x; sum[1] += y;
            sum[2] += d0 == null ? 0 : 2 * d0[0][p]; sum[3] += d0 == null ? 0 : 2 * d0[1][p];
            int[] depths = {x, y};
            for (int s = 0; s < 2; s++) {
                if (depths[s] >= 1) b1[s]++;
                if (depths[s] >= 5) b5[s]++;
                if (depths[s] >= 10) b10[s]++;
            }
            if (x >= 5 && y >= 5) joint5++;
            if (x >= 10 && y >= 10) joint10++;
        }
        double length = range.length();
        double m56 = sum[0] / length, m57 = sum[1] / length, z56 = m56 / a56, z57 = m57 / a57;
        double u56 = sum[2] == 0 ? Double.NaN : (double) sum[0] / sum[2];
        double u57 = sum[3] == 0 ? Double.NaN : (double) sum[1] / sum[3];
        boolean ab = spec.type.equals("AB");
        double ratio = Math.log((z56 + .05) / (z57 + .05)) / Math.log(2);
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("length_bp", length); result.put("gap_bp", (double) spec.gaps.get(range));
        result.put("q20_equiv_sum_xp56", (double) sum[0]); result.put("q20_equiv_sum_xp57", (double) sum[1]);
        result.put("q0_equiv_sum_xp56", (double) sum[2]); result.put("q0_equiv_sum_xp57", (double) sum[3]);
        result.put("m_xp56", m56); result.put("m_xp57", m57); result.put("z_xp56", z56); result.put("z_xp57", z57);
        result.put("B5_xp56", b5[0] / length); result.put("B5_xp57", b5[1] / length);
        result.put("B10_xp56", b10[0] / length); result.put("B10_xp57", b10[1] / length);
        result.put("B5_joint", ab ? joint5 / length : Double.NaN); result.put("B10_joint", ab ? joint10 / length : Double.NaN);
        result.put("A", ab ? Math.abs(ratio) : Double.NaN);
        result.put("T", ab ? Double.NaN : spec.type.equals("R") ? ratio : -ratio);
        result.put("U_xp56", u56); result.put("U_xp57", u57);
        result.put("u", ab ? Math.min(u56, u57) : spec.type.equals("R") ? u56 : u57);
        result.put("B1_negative", ab ? Double.NaN : b1[spec.type.equals("R") ? 1 : 0] / length);
        result.put("gap_fraction", spec.gaps.get(range) / length);
        return result;
    }

    private static void cacheEvictionAndLargeCoordinates() throws Exception {
        Path base = Files.createDirectory(root.resolve("cache"));
        List<Range> ranges = new ArrayList<>();
        StringBuilder depths = new StringBuilder();
        for (int i = 0; i < 12_000; i++) {
            ranges.add(new Range("1A", 2 * i, 2 * i + 1));
            depths.append("1A\t").append(2 * i + 1).append("\t5\t7\n");
        }
        Collections.shuffle(ranges, new Random(71));
        Spec spec = new Spec("cache", "AB", ranges, Collections.emptyMap());
        File parameters = parameters(base, List.of(spec));
        File depth = file(base.resolve("depth.tsv"), depths.toString());
        var result = new StatisticsCalculator(1, 2, 2000, 1).calculateAndWrite(load(parameters),
                base.resolve("regions").toFile(), depth, depth, base.resolve("out"));
        check(result.indexedEndpoints() == 24_000, "index exceeds 1 MiB cache and forces eviction");
        try (BufferedReader input = Files.newBufferedReader(result.rangeStatistics())) {
            input.readLine();
            for (Range range : ranges) {
                String[] row = input.readLine().split("\t");
                check(row[4].equals(Integer.toString(range.start())), "original order across cache evictions");
                check(row[8].equals("10") && row[9].equals("14"), "prefix counts across cache blocks");
                check(row[16].equals("1.0") && row[26].equals("1.0"), "threshold and U after cache eviction");
            }
            check(input.readLine() == null, "no extra range rows");
        }
        number(table(result.configurationStatistics()).getFirst(), "Y10_joint_bp", 12_000);
        number(table(result.configurationStatistics()).getFirst(), "U10", 1);

        Path boundary = Files.createDirectory(root.resolve("boundary"));
        List<Range> extreme = List.of(new Range("1D", Integer.MAX_VALUE - 1, Integer.MAX_VALUE), new Range("1D", 0, 2));
        File params = parameters(boundary, List.of(new Spec("max", "D", extreme, Map.of())));
        File high = file(boundary.resolve("depth.tsv"), "1D\t1\t1073741823\t1073741823\n"
                + "1D\t2\t1073741823\t1073741823\n1D\t2147483647\t1\t5\n");
        var edge = new StatisticsCalculator(1, 1, 2, 1).calculateAndWrite(load(params), boundary.resolve("regions").toFile(),
                high, high, boundary.resolve("out"));
        List<Map<String, String>> output = table(edge.rangeStatistics());
        number(output.get(0), "m_xp57", 10);
        number(output.get(0), "B10_xp57", 1);
        number(output.get(1), "q20_equiv_sum_xp56", 4_294_967_292L);
        number(table(edge.configurationStatistics()).getFirst(), "Y10_positive_bp", 3);
    }

    private static void zeroAndFailureCases() throws Exception {
        Path base = Files.createDirectory(root.resolve("zero"));
        File parameters = parameters(base, List.of(new Spec("zero", "AB", List.of(new Range("1A", 0, 10)), Map.of())));
        File zero = file(base.resolve("zero.tsv"), "");
        var calculator = new StatisticsCalculator(19.20, 19.93, 2, 1);
        var result = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), zero, zero, base.resolve("out"));
        Map<String, String> row = table(result.configurationStatistics()).getFirst();
        number(row, "A90", 0); number(row, "U10", Double.NaN); number(row, "invalid_U_fraction", 1); number(row, "C10", 0);
        List<Path> resultFiles = List.of(result.rangeStatistics(), result.configurationStatistics(), result.metadata());
        List<byte[]> saved = new ArrayList<>();
        for (Path path : resultFiles) saved.add(Files.readAllBytes(path));
        File nonzero = file(base.resolve("nonzero.tsv"), "1A\t1\t3\t3\n");
        var rerun = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), nonzero, nonzero, base.resolve("out"));
        check(rerun.rangeStatistics().equals(result.rangeStatistics())
                && rerun.configurationStatistics().equals(result.configurationStatistics())
                && rerun.metadata().equals(result.metadata()), "rerun returns the same three output paths");
        for (int i = 0; i < resultFiles.size(); i++) {
            check(!Arrays.equals(saved.get(i), Files.readAllBytes(resultFiles.get(i))), "rerun replaces " + resultFiles.get(i).getFileName());
        }
        number(table(rerun.rangeStatistics()).getFirst(), "q20_equiv_sum_xp56", 6);
        number(table(rerun.configurationStatistics()).getFirst(), "U10", 1);
        try (var files = Files.list(base.resolve("out"))) {
            check(files.count() == 3, "rerun creates only three result files and no run directory");
        }
        Path empty = Files.createDirectory(base.resolve("existing-empty"));
        var existing = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), zero, zero, empty);
        check(existing.rangeStatistics().getParent().equals(empty), "existing empty directory is used directly");
        Path unrelated = Files.createDirectory(base.resolve("with-unrelated-file"));
        Files.writeString(unrelated.resolve("notes.txt"), "preserve me");
        Path historical = Files.createDirectory(unrelated.resolve("run-historical"));
        Files.writeString(historical.resolve("README.txt"), "old run");
        var shared = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), zero, zero, unrelated);
        check(shared.rangeStatistics().getParent().equals(unrelated), "unrelated files do not prevent direct output");
        check(Files.readString(unrelated.resolve("notes.txt")).equals("preserve me"), "unrelated files are preserved");
        check(Files.readString(historical.resolve("README.txt")).equals("old run"), "historical run directories are preserved");
        Path regular = file(base.resolve("not-a-directory"), "preserve me").toPath();
        expect(() -> calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), zero, zero, regular), "not a directory");
        check(Files.readString(regular).equals("preserve me"), "regular-file output target is not replaced");
        Path incomplete = Files.createDirectory(base.resolve("incomplete-stage"));
        Files.writeString(incomplete.resolve("range_statistics.tsv"), "range");
        Files.writeString(incomplete.resolve("configuration_statistics.tsv"), "summary");
        Path rollbackOutput = Files.createDirectory(base.resolve("rollback-output"));
        expect(() -> StatisticsCalculator.publish(incomplete, rollbackOutput), "README.txt");
        check(Files.exists(incomplete.resolve("range_statistics.tsv")) && Files.exists(incomplete.resolve("configuration_statistics.tsv")), "partial publication restores staged files");
        try (var files = Files.list(rollbackOutput)) { check(files.findAny().isEmpty(), "failed publication leaves no partial output"); }
        List<String> filenames = List.of("range_statistics.tsv", "configuration_statistics.tsv", "README.txt");
        for (String filename : filenames) {
            Files.writeString(incomplete.resolve(filename), "new " + filename);
            Files.writeString(rollbackOutput.resolve(filename), "old " + filename);
        }
        // The extra file forces final stage deletion to fail after all replacements.
        Files.writeString(incomplete.resolve("extra.txt"), "force rollback");
        expect(() -> StatisticsCalculator.publish(incomplete, rollbackOutput), "incomplete-stage");
        for (String filename : filenames) {
            check(Files.readString(rollbackOutput.resolve(filename)).equals("old " + filename), "failed replacement restores " + filename);
            check(Files.readString(incomplete.resolve(filename)).equals("new " + filename), "failed replacement restores staged " + filename);
        }
        var undefined = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), nonzero, zero, base.resolve("zero-denominator"));
        Map<String, String> flagged = table(undefined.configurationStatistics()).getFirst();
        number(flagged, "U10", Double.NaN); number(flagged, "n_q20_gt_q0_xp56", 1);
        number(flagged, "n_q20_gt_q0_xp57", 1); number(flagged, "n_u_gt1", 0);
        File lower = file(base.resolve("lower.tsv"), "1A\t1\t2\t2\n");
        var aboveOne = calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), nonzero, lower, base.resolve("above-one"));
        Map<String, String> above = table(aboveOne.configurationStatistics()).getFirst();
        number(above, "U10", 1.5); number(above, "n_u_gt1", 1);
        check(table(aboveOne.rangeStatistics()).getFirst().get("u_gt1").equals("true"), "U > 1 preserved and flagged");
        File malformed = file(base.resolve("malformed.tsv"), "Un\t2\t1\t1\nUn\t1\t1\t1\n");
        expect(() -> calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), malformed, zero, base.resolve("bad-depth")), "strictly increasing");
        check(!Files.exists(base.resolve("bad-depth")), "malformed unused chromosome is still validated");
        saved.clear();
        for (Path path : resultFiles) saved.add(Files.readAllBytes(path));
        expect(() -> calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), malformed, zero, base.resolve("out")), "strictly increasing");
        for (int i = 0; i < resultFiles.size(); i++) {
            check(Arrays.equals(saved.get(i), Files.readAllBytes(resultFiles.get(i))), "calculation failure preserves " + resultFiles.get(i).getFileName());
        }
        Files.writeString(base.resolve("regions/zero/gap.bed"), "1A\t0\t1\n");
        expect(() -> calculator.calculateAndWrite(load(parameters), base.resolve("regions").toFile(), zero, zero, base.resolve("bad-gap")), "partition");
        check(!Files.exists(base.resolve("bad-gap")), "invalid gap input is not published");
        try (var paths = Files.list(base)) {
            check(paths.noneMatch(p -> p.getFileName().toString().startsWith(".statistics-")), "all failed staging directories cleaned up");
        }
        expect(() -> new StatisticsCalculator(0, 1), "baselines");
        expect(() -> new StatisticsCalculator(1, 1, 1, 1), "sortChunkInts");
    }

    private static File parameters(Path base, List<Spec> specs) throws IOException {
        StringBuilder parameters = new StringBuilder(HEADER);
        for (Spec spec : specs) {
            parameters.append(spec.id).append('\t').append(spec.type).append("\t6\t")
                    .append(spec.type.equals("AB") ? "NA\t0.2" : "0\tNA").append("\t100\t60\t12\n");
            StringBuilder finals = new StringBuilder(), cores = new StringBuilder(), gaps = new StringBuilder();
            for (Range range : spec.ranges) {
                bed(finals, range);
                int gap = spec.gaps.getOrDefault(range, 0);
                if (gap == 0) bed(cores, range);
                else {
                    bed(cores, new Range(range.chr(), range.start(), range.start() + 1));
                    bed(gaps, new Range(range.chr(), range.start() + 1, range.start() + 1 + gap));
                    bed(cores, new Range(range.chr(), range.start() + 1 + gap, range.end()));
                }
            }
            file(base.resolve("regions/" + spec.id + "/final.bed"), finals.toString());
            file(base.resolve("regions/" + spec.id + "/core.bed"), cores.toString());
            file(base.resolve("regions/" + spec.id + "/gap.bed"), gaps.toString());
        }
        return file(base.resolve("parameters.tsv"), parameters.toString());
    }

    private static File depthFile(Path file, Map<String, int[][]> data, List<String> order) throws IOException {
        StringBuilder text = new StringBuilder();
        for (String chr : order) {
            int[][] depths = data.get(chr);
            for (int p = 0; p < depths[0].length; p++) if (depths[0][p] != 0 || depths[1][p] != 0) {
                text.append(chr).append('\t').append(p + 1).append('\t').append(depths[0][p]).append('\t').append(depths[1][p]).append('\n');
            }
        }
        return file(file, text.toString());
    }
    private static void bed(StringBuilder out, Range r) { out.append(r.chr()).append('\t').append(r.start()).append('\t').append(r.end()).append('\n'); }
    private static File file(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent()); Files.writeString(path, contents); return path.toFile();
    }
    private static Configurations load(File file) { Configurations configs = new Configurations(); configs.buildFromParameterFile(file); return configs; }
    private static List<Map<String, String>> table(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String[] header = lines.getFirst().split("\t");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\t", -1);
            check(fields.length == header.length, "TSV column alignment");
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < header.length; i++) row.put(header[i], fields[i]);
            rows.add(row);
        }
        return rows;
    }
    private static double quantile(List<Double> values, double p) {
        if (values.isEmpty()) return Double.NaN;
        List<Double> sorted = new ArrayList<>(values); Collections.sort(sorted);
        double index = p * (sorted.size() - 1);
        int a = (int) index, b = (int) Math.ceil(index);
        return (1 - (index - a)) * sorted.get(a) + (index - a) * sorted.get(b);
    }
    private static void number(Map<String, String> row, String key, double expected) {
        String text = row.get(key);
        if (Double.isNaN(expected)) check("NA".equals(text), key + " expected NA, got " + text);
        else {
            double actual = Double.parseDouble(text);
            check(Math.abs(actual - expected) <= 1e-11 * Math.max(1, Math.abs(expected)), key + ": " + actual + " != " + expected);
        }
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static void expect(Action action, String text) throws Exception {
        try { action.run(); }
        catch (Exception e) { check(e.getMessage().contains(text), "expected failure context: " + e); return; }
        throw new AssertionError("Expected failure containing " + text);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}
