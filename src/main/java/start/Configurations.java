package start;

import dutils.GenomeType;
import dutils.Range;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.StringUtils;
import utils.IOTool;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class Configurations {

    //dim1 is configuration
    String[] config_id;
    GenomeType[] genomeType;
    int[] alpha;
    double[] beta;
    double[] delta;
    int[] gamma;
    int[] lambda;
    int[] rho;
    Range[][] ranges;

    /**
     * Reads a TSV header beginning with config_id/type and at least eight columns.
     * Extra columns (such as rho/lambda) are ignored. NA in beta/delta is stored as -1.0.
     * Blank lines and # comments are skipped; failed reads preserve existing data.
     */
    public void buildFromParameterFile(File parameterFile) {
        List<String> configIDList = new ArrayList<>();
        List<GenomeType> genomeTypeList = new ArrayList<>();
        IntList alphaList = new IntArrayList();
        DoubleList betaList = new DoubleArrayList();
        DoubleList deltaList = new DoubleArrayList();
        IntList gammaList = new  IntArrayList();
        IntList lambdaList = new  IntArrayList();
        IntList rhoList = new  IntArrayList();
        Set<String> seenIDs = new HashSet<>();
        int columnCount = 0;
        long lineNumber = 0;
        try (BufferedReader br = IOTool.getBufferedReader(parameterFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                try {
                    String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
                    for (int i = 0; i < columns.length; i++) columns[i] = columns[i].trim();
                    if (columnCount == 0) {
                        if (columns.length < 8 || !"config_id".equals(columns[0]) || !"type".equals(columns[1])) {
                            throw new IllegalArgumentException("Expected a TSV header beginning with config_id/type and at least eight columns");
                        }
                        columnCount = columns.length;
                        continue;
                    }
                    if (columns.length != columnCount) {
                        throw new IllegalArgumentException("Expected " + columnCount + " TSV columns, found " + columns.length);
                    }
                    for (int i = 0; i < 8; i++) {
                        if (columns[i].isEmpty()) throw new IllegalArgumentException("Empty field in column " + (i + 1));
                    }
                    if (!seenIDs.add(columns[0])) {
                        throw new IllegalArgumentException("Duplicate config_id: " + columns[0]);
                    }
                    configIDList.add(columns[0]);
                    genomeTypeList.add(GenomeType.fromShortName(columns[1]));
                    alphaList.add(parseNonNegativeInt(columns[2]));
                    betaList.add(parseOptionalDouble(columns[3]));
                    deltaList.add(parseOptionalDouble(columns[4]));
                    gammaList.add(parseNonNegativeInt(columns[5]));
                    lambdaList.add(parseNonNegativeInt(columns[6]));
                    rhoList.add(parseNonNegativeInt(columns[7]));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(parameterFile.getAbsolutePath() + ":" + lineNumber
                            + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read parameters from " + parameterFile.getAbsolutePath(), e);
        }
        if (columnCount == 0 || configIDList.isEmpty()) {
            throw new IllegalArgumentException("No configurations found in " + parameterFile.getAbsolutePath());
        }
        this.config_id = configIDList.toArray(String[]::new);
        this.genomeType = genomeTypeList.toArray(GenomeType[]::new);
        this.alpha = alphaList.toIntArray();
        this.beta = betaList.toDoubleArray();
        this.delta = deltaList.toDoubleArray();
        this.gamma = gammaList.toIntArray();
        this.lambda = lambdaList.toIntArray();
        this.rho = rhoList.toIntArray();
        this.ranges = new Range[configIDList.size()][];
        System.out.println("Built " + configIDList.size() + " configurations from " + parameterFile.getAbsolutePath());
    }

    private static int parseNonNegativeInt(String value) {
        int number = Integer.parseInt(value);
        if (number < 0) throw new IllegalArgumentException("Expected a non-negative integer, found " + value);
        return number;
    }

    private static double parseOptionalDouble(String value) {
        if ("NA".equals(value)) return -1.0;
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number) || number < 0) {
            throw new IllegalArgumentException("Expected NA or a finite non-negative number, found " + value);
        }
        return number;
    }

    /**
     * Reads regionsDir/config_id/final.bed in parallel across configurations.
     * ranges[i] corresponds to config_id[i] and preserves the BED record order.
     * BED coordinates are 0-based and half-open; only the first three columns are used.
     * Blank lines, # comments, track lines and browser lines are skipped.
     */
    public void addRegions(File regionsDir) {
        if (this.config_id == null) {
            throw new IllegalStateException("Build configurations before adding regions");
        }
        File[] regionFiles = new File[this.config_id.length];
        File subdir;
        for (int i = 0; i < this.config_id.length; i++) {
            subdir = new File(regionsDir, this.config_id[i]);
            regionFiles[i] = new File(subdir, "final.bed");
        }
        int threads = Math.min(32, regionFiles.length); // 设置目标并行度

        try (ForkJoinPool pool = new ForkJoinPool(threads)) {
            this.ranges = pool.submit(() ->
                    IntStream.range(0, regionFiles.length)
                            .parallel()
                            .mapToObj(i -> readRegions(regionFiles[i]))
                            .toArray(Range[][]::new)
            ).join();
        }
    }

    private static Range[] readRegions(File regionFile) {
        List<Range> regionList = new ArrayList<>();
        long lineNumber = 0;
        try (BufferedReader br = IOTool.getBufferedReader(regionFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String leading = line.stripLeading();
                if (leading.isEmpty() || leading.startsWith("#") || leading.equals("track")
                        || leading.startsWith("track ") || leading.startsWith("track\t")
                        || leading.equals("browser") || leading.startsWith("browser ") || leading.startsWith("browser\t")) continue;
                try {
                    String[] columns = line.indexOf('\t') >= 0
                            ? StringUtils.splitPreserveAllTokens(line, '\t') : StringUtils.split(line);
                    if (columns.length < 3) throw new IllegalArgumentException("Expected at least three BED columns");
                    for (int i = 0; i < 3; i++) {
                        columns[i] = columns[i].trim();
                        if (columns[i].isEmpty()) throw new IllegalArgumentException("Empty BED field in column " + (i + 1));
                    }
                    regionList.add(new Range(columns[0], Integer.parseInt(columns[1]), Integer.parseInt(columns[2])));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(regionFile.getAbsolutePath() + ":" + lineNumber
                            + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read regions from "
                    + regionFile.getAbsolutePath(), e);
        }
        return regionList.toArray(new Range[0]);
    }

}
