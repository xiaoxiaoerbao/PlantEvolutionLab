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
import java.util.Arrays;
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
    Range[][] coreRanges;
    Range[][] gapRanges;

    /**
     * Reads a TSV header beginning with config_id/type and at least eight columns.
     * Extra columns (such as rho/lambda ratio) are ignored. NA in beta/delta is stored as -1.0.
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
        this.coreRanges = new Range[configIDList.size()][];
        this.gapRanges = new Range[configIDList.size()][];
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
     * Reads final.bed, core.bed and gap.bed from regionsDir/config_id in parallel.
     * All three arrays correspond to config_id and preserve the BED record order.
     * Core and gap intervals must partition the final intervals without overlap.
     * Empty gap files are valid. Failed reads preserve all existing region arrays.
     * BED coordinates are 0-based and half-open; only the first three columns are used.
     * Blank lines, # comments, track lines and browser lines are skipped.
     */
    public void addRegions(File regionsDir) {
        if (this.config_id == null) {
            throw new IllegalStateException("Build configurations before adding regions");
        }
        int threads = Math.min(32, this.config_id.length); // 设置目标并行度

        try (ForkJoinPool pool = new ForkJoinPool(threads)) {
            RegionSet[] loaded = pool.submit(() ->
                    IntStream.range(0, this.config_id.length)
                            .parallel()
                            .mapToObj(i -> readRegionSet(new File(regionsDir, this.config_id[i])))
                            .toArray(RegionSet[]::new)
            ).join();
            Range[][] newRanges = new Range[loaded.length][];
            Range[][] newCoreRanges = new Range[loaded.length][];
            Range[][] newGapRanges = new Range[loaded.length][];
            for (int i = 0; i < loaded.length; i++) {
                newRanges[i] = loaded[i].regions();
                newCoreRanges[i] = loaded[i].cores();
                newGapRanges[i] = loaded[i].gaps();
            }
            this.ranges = newRanges;
            this.coreRanges = newCoreRanges;
            this.gapRanges = newGapRanges;
        }
    }

    record RegionSet(Range[] regions, Range[] cores, Range[] gaps) {}

    // The statistics path reads one configuration at a time, without populating Range[][].
    static RegionSet readRegionSet(File directory) {
        Range[] regions = readRegions(new File(directory, "final.bed"));
        Range[] cores = readRegions(new File(directory, "core.bed"));
        Range[] gaps = readRegions(new File(directory, "gap.bed"));
        validatePartition(directory, regions, cores, gaps);
        return new RegionSet(regions, cores, gaps);
    }

    /** Validates on sorted copies, retaining original BED order in the stored arrays. */
    private static void validatePartition(File directory, Range[] regions, Range[] cores, Range[] gaps) {
        Range[] sortedRegions = regions.clone();
        Arrays.sort(sortedRegions);
        Range[] parts = new Range[cores.length + gaps.length];
        System.arraycopy(cores, 0, parts, 0, cores.length);
        System.arraycopy(gaps, 0, parts, cores.length, gaps.length);
        Arrays.sort(parts);
        int index = 0;
        Range previous = null;
        for (Range region : sortedRegions) {
            if (previous != null && previous.overlaps(region)) {
                throw new IllegalArgumentException(directory + ": overlapping final.bed intervals: " + previous + ", " + region);
            }
            int cursor = region.start();
            while (cursor < region.end()) {
                if (index >= parts.length || !parts[index].chr().equals(region.chr())
                        || parts[index].start() != cursor || parts[index].end() > region.end()) {
                    throw new IllegalArgumentException(directory + ": core.bed and gap.bed must partition final.bed exactly near " + region);
                }
                cursor = parts[index++].end();
            }
            previous = region;
        }
        if (index != parts.length) {
            throw new IllegalArgumentException(directory + ": core.bed or gap.bed contains intervals outside final.bed");
        }
    }

    static Range[] readRegions(File regionFile) {
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
