package start;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Locale;

public class Start {

    public static void main(String[] args) {
        if (args.length != 0 && args.length != 4 && args.length != 5 && args.length != 6) {
            throw new IllegalArgumentException("Usage: start.Start [q20Depth.tsv q0Depth.tsv parameters.tsv regionsDir [outputDir [baseline.json]]]");
        }
        String positionDepth = args.length == 0 ? "/Users/xudaxing/Desktop/temp/parents.evaluation.depth.txt" : args[0];
        String positionQ0Depth = args.length == 0 ? "/Users/xudaxing/Desktop/temp/parents.evaluation.q0.depth.txt" : args[1];
        String parameterFile = args.length == 0 ? "/Users/xudaxing/Desktop/temp/parameters.tsv" : args[2];
        String regionsDir = args.length == 0 ? "/Users/xudaxing/Desktop/temp/regions" : args[3];
        Path outputDir = Path.of(args.length >= 5 ? args[4] : "/Users/xudaxing/Desktop/temp/output");
        Path baselineFile = Path.of(args.length == 6 ? args[5] : "/Users/xudaxing/Desktop/temp/baseline.json");
        long startNanos = System.nanoTime();
        try {
            NormalizationBaselines baselines = NormalizationBaselines.read(baselineFile);
            System.out.println("Normalization baselines from " + baselineFile.toAbsolutePath()
                    + ": XP56=" + baselines.baseline56() + ", XP57=" + baselines.baseline57());
            Configurations configurations = new Configurations();
            configurations.buildFromParameterFile(new File(parameterFile));
            StatisticsCalculator.Output output = new StatisticsCalculator(baselines.baseline56(), baselines.baseline57()).calculateAndWrite(
                    configurations, new File(regionsDir), new File(positionDepth), new File(positionQ0Depth), outputDir);
            System.out.println("Range statistics: " + output.rangeStatistics());
            System.out.println("Configuration statistics: " + output.configurationStatistics());
            System.out.println("Completed: configurations=" + output.configurationCount() + ", ranges=" + output.rangeCount());
        } catch (IOException e) {
            throw new UncheckedIOException("Statistics calculation failed: " + e.getMessage(), e);
        } finally {
            double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
            System.out.printf(Locale.ROOT, "Total runtime for range and configuration statistics: %.3f seconds%n", elapsedSeconds);
        }
    }
}
