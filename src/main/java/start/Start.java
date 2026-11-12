package start;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class Start {

    public static void main(String[] args) {
        if (args.length != 0 && args.length != 4 && args.length != 5 && args.length != 7) {
            throw new IllegalArgumentException("Usage: start.Start [q20Depth.tsv q0Depth.tsv parameters.tsv regionsDir [outputDir [baselineXP56 baselineXP57]]]");
        }
        String positionDepth = args.length == 0 ? "/Users/xudaxing/Desktop/temp/parents.evaluation.depth.txt" : args[0];
        String positionQ0Depth = args.length == 0 ? "/Users/xudaxing/Desktop/temp/parents.evaluation.q0.depth.txt" : args[1];
        String parameterFile = args.length == 0 ? "/Users/xudaxing/Desktop/temp/parameters.tsv" : args[2];
        String regionsDir = args.length == 0 ? "/Users/xudaxing/Desktop/temp/regions" : args[3];
        Path outputDir = Path.of(args.length >= 5 ? args[4] : "/Users/xudaxing/Desktop/temp/output");
        double baseline56 = args.length == 7 ? Double.parseDouble(args[5]) : StatisticsCalculator.DEFAULT_BASELINE_XP56;
        double baseline57 = args.length == 7 ? Double.parseDouble(args[6]) : StatisticsCalculator.DEFAULT_BASELINE_XP57;

        Configurations configurations = new Configurations();
        configurations.buildFromParameterFile(new File(parameterFile));
        try {
            StatisticsCalculator.Output output = new StatisticsCalculator(baseline56, baseline57).calculateAndWrite(
                    configurations, new File(regionsDir), new File(positionDepth), new File(positionQ0Depth), outputDir);
            System.out.println("Range statistics: " + output.rangeStatistics());
            System.out.println("Configuration statistics: " + output.configurationStatistics());
            System.out.println("Completed: configurations=" + output.configurationCount() + ", ranges=" + output.rangeCount());
        } catch (IOException e) {
            throw new UncheckedIOException("Statistics calculation failed: " + e.getMessage(), e);
        }
    }
}
