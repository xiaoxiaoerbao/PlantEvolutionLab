package start;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/** Standalone regression checks for compatible parameter headers. */
public class ConfigurationHeaderRegressionTest {
    private static final String HEADER = "config_id\ttype\talpha\tbeta\tdelta\tgamma\tlambda\trho\tratio\n";
    private static Path root;
    private static int checks;

    public static void main(String[] args) throws Exception {
        root = Files.createTempDirectory("plant-header-test-");
        try {
            testParameterHeaders();
            testInvalidHeaders();
            System.out.println("Configuration header regression checks passed: " + checks);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void testParameterHeaders() throws Exception {
        String rows = "AB_00001\tAB\t6\tNA\t0.2\t100\t60\t0\t0.0\n"
                + "D_00001\tD\t6\t0.0\tNA\t100\t60\t12\t0.2\n";
        Configurations type = new Configurations();
        type.buildFromParameterFile(file("type-header.tsv",
                "config_id\ttype\tα\tβ\tδ\tγ\tλ\tρ\tρ/λ\n" + rows));
        Configurations genomeType = new Configurations();
        genomeType.buildFromParameterFile(file("genome-type-header.tsv",
                HEADER.replace("\ttype\t", "\tgenomeType\t") + rows));
        check(Arrays.equals(type.config_id, new String[] {"AB_00001", "D_00001"}), "real parameter header and config IDs");
        check(Arrays.equals(type.config_id, genomeType.config_id)
                && Arrays.equals(type.genomeType, genomeType.genomeType), "type and genomeType headers select identical configurations");
        check(type.beta[0] == -1.0 && type.beta[1] == 0.0
                && type.delta[0] == 0.2 && type.delta[1] == -1.0, "Greek parameter labels preserve NA and decimal values");
        check(Arrays.equals(type.alpha, genomeType.alpha) && Arrays.equals(type.beta, genomeType.beta)
                && Arrays.equals(type.delta, genomeType.delta) && Arrays.equals(type.gamma, genomeType.gamma)
                && Arrays.equals(type.lambda, genomeType.lambda) && Arrays.equals(type.rho, genomeType.rho),
                "header aliases preserve all parameter values");
    }

    private static void testInvalidHeaders() throws Exception {
        String row = "AB_test\tAB\t6\t0.0\t0.2\t100\t60\t0\t0\n";
        Configurations configs = new Configurations();
        configs.buildFromParameterFile(file("valid.tsv", HEADER + row));
        String[] originalIDs = configs.config_id;
        double[] originalBeta = configs.beta;
        String[] invalid = {
                HEADER.replace("config_id", "id") + row,
                HEADER.replace("\ttype\t", "\tunknown\t") + row,
                "config_id\ttype\talpha\tbeta\tdelta\tgamma\tlambda\n"
        };
        for (int i = 0; i < invalid.length; i++) {
            File input = file("invalid-" + i + ".tsv", invalid[i]);
            try {
                configs.buildFromParameterFile(input);
                throw new AssertionError("Invalid header accepted: " + input);
            } catch (IllegalArgumentException e) {
                check(e.getMessage().contains(input.getAbsolutePath() + ":1:"), "header error includes file and line");
                check(configs.config_id == originalIDs && configs.beta == originalBeta, "failed header read preserves data");
            }
        }
    }

    private static File file(String name, String content) throws IOException {
        Path path = root.resolve(name);
        Files.writeString(path, content);
        return path.toFile();
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
