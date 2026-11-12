package start;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Standalone checks for baseline JSON schema, numeric validation, and useful error context. */
public class NormalizationBaselinesRegressionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("plant-baselines-test-");
        try {
            Path input = root.resolve("baseline.json");
            Files.writeString(input, """
                    {
                      "method": "Methods.docx Methods S2",
                      "thresholds": {"P_w_min": 5},
                      "a56_CARMEN": 2.5e1,
                      "a57_SN055525": 7,
                      "comparison_bundle_k_ratio": {"a56": 999, "a57_SN055525": 888}
                    }
                    """);
            var baselines = NormalizationBaselines.read(input);
            check(baselines.baseline56() == 25 && baselines.baseline57() == 7,
                    "uses only top-level baselines; accepts exponent and integer JSON numbers");
            Files.writeString(input, "\uFEFF{\"a56_CARMEN\": 19.2, \"a57_SN055525\": 19.93}\n");
            baselines = NormalizationBaselines.read(input);
            check(baselines.baseline56() == 19.2 && baselines.baseline57() == 19.93,
                    "UTF-8 BOM and decimal baselines");
            for (String key : new String[] {"a56_CARMEN", "a57_SN055525"}) {
                for (String invalid : new String[] {"0", "-1", "-0.0", "null", "true", "\"19.2\"", "\"NaN\"", "1e999", "[]", "{}"}) {
                    String other = key.equals("a56_CARMEN") ? "a57_SN055525" : "a56_CARMEN";
                    Files.writeString(input, "{\"" + key + "\":" + invalid + ",\"" + other + "\":2}");
                    expectReadFailure(input, key);
                }
                Files.writeString(input, "{\"comparison_bundle_k_ratio\":{\"" + key + "\":10}}");
                expectReadFailure(input, "a56_CARMEN");
            }
            Files.writeString(input, "{\"a56_CARMEN\":2}");
            expectReadFailure(input, "a57_SN055525");
            Files.writeString(input, "{\"a57_SN055525\":2}");
            expectReadFailure(input, "a56_CARMEN");
            for (String invalidJson : new String[] {"", "[]", "{", "{\"a56_CARMEN\":2,\"a57_SN055525\":3} trailing"}) {
                Files.writeString(input, invalidJson);
                expectReadFailure(input, "");
            }
            Files.delete(input);
            expectReadFailure(input, "baseline.json");
            entryPoint(root);
            System.out.println("Baseline regression checks passed: " + checks);
        } finally {
            StatisticsCalculator.deleteTree(root);
        }
    }

    private static void entryPoint(Path root) throws Exception {
        Path base = Files.createDirectory(root.resolve("entry"));
        Path parameters = base.resolve("parameters.tsv");
        Files.writeString(parameters, "config_id\ttype\talpha\tbeta\tdelta\tgamma\tlambda\trho\n"
                + "AB_test\tAB\t6\tNA\t0.2\t100\t60\t0\n");
        Path regions = Files.createDirectories(base.resolve("regions/AB_test"));
        Files.writeString(regions.resolve("final.bed"), "1A\t0\t2\n");
        Files.writeString(regions.resolve("core.bed"), "1A\t0\t2\n");
        Files.writeString(regions.resolve("gap.bed"), "");
        Path depth = base.resolve("depth.tsv");
        Files.writeString(depth, "1A\t1\t1\t2\n");
        Path baseline = base.resolve("baseline.json");
        Files.writeString(baseline, "{\"a56_CARMEN\":2,\"a57_SN055525\":4}");
        Path output = base.resolve("output");
        String[] command = {depth.toString(), depth.toString(), parameters.toString(),
                regions.getParent().toString(), output.toString(), baseline.toString()};
        Start.main(command);
        String metadata = Files.readString(output.resolve("README.txt"));
        check(metadata.contains("XP56 = CARMEN; baseline = 2.0") && metadata.contains("XP57 = SN055525; baseline = 4.0"),
                "entry point uses supplied JSON baselines rather than former constants");
        List<String> ranges = Files.readAllLines(output.resolve("range_statistics.tsv"));
        check(ranges.size() == 2, "entry point publishes the expected number of ranges");
        String[] first = ranges.get(1).split("\t");
        check(Double.parseDouble(first[14]) == 0.5 && Double.parseDouble(first[15]) == 0.5,
                "JSON baselines propagate into normalized depths");
        List<Path> results = List.of(output.resolve("range_statistics.tsv"),
                output.resolve("configuration_statistics.tsv"), output.resolve("README.txt"));
        byte[][] saved = new byte[results.size()][];
        for (int i = 0; i < results.size(); i++) saved[i] = Files.readAllBytes(results.get(i));
        Files.writeString(baseline, "{\"a56_CARMEN\":2}");
        try {
            Start.main(command);
            throw new AssertionError("Expected invalid baseline rejection");
        } catch (UncheckedIOException e) {
            check(e.getMessage().contains(baseline.toString()) && e.getMessage().contains("a57_SN055525"),
                    "entry point reports baseline file and missing field");
        }
        for (int i = 0; i < results.size(); i++) {
            check(Arrays.equals(saved[i], Files.readAllBytes(results.get(i))), "baseline read failure preserves " + results.get(i));
        }
        command[4] = base.resolve("failed-output").toString();
        try {
            Start.main(command);
            throw new AssertionError("Expected invalid baseline rejection");
        } catch (UncheckedIOException e) {
            check(!Files.exists(Path.of(command[4])), "baseline read failure creates no output directory");
        }
        try {
            Start.main(new String[] {"q20", "q0", "parameters", "regions", "out", "19.2", "19.93"});
            throw new AssertionError("Expected old numeric-baseline arguments to be rejected");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("baseline.json"), "usage documents the replacement JSON argument");
        }
    }

    private static void expectReadFailure(Path input, String message) throws Exception {
        try {
            NormalizationBaselines.read(input);
            throw new AssertionError("Expected baseline read failure: " + message);
        } catch (IOException e) {
            check(e.getMessage().contains(input.toAbsolutePath().toString()) && e.getMessage().contains(message),
                    "baseline errors include file path and context: " + e.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
