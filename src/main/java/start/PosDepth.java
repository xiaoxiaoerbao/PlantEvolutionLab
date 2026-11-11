package start;

import dutils.Chromosome;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import utils.IOTool;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Parental depths from a headerless, tab-separated file: chromosome, position,
 * observed XP56 (CARMEN) depth, observed XP57 (SN055525) depth.
 * Depths are multiplied by two exactly once on input and stored/returned as
 * full-dataset-equivalent depths, for both MAPQ >= 20 and MAPQ >= 0 files.
 * Positions remain 1-based (unlike the BED-based Range).
 * Known chromosomes use {@link Chromosome#ordinal()} as their array index;
 * additional sequences (such as Un) are appended and can be queried by name.
 * The second index identifies a recorded site, not a genomic coordinate;
 * Absent chromosomes have empty arrays. Unrecorded sites on recognized chromosomes
 * represent zero depth in both parents; zeros are implicit rather than allocated.
 * An unrecognized chromosome name returns -1 from the depth lookup methods.
 */
public class PosDepth {
    public static final int EQUIVALENT_DEPTH_FACTOR = 2;
    private String[] chromosomeNames = PosDepth.knownChromosomeNames();
    // dim1 is getChromosomeIndex(name), dim2 is the recorded site index
    int[][] ref_pos = PosDepth.emptyChromosomes();
    int[][] xp56_depth = PosDepth.emptyChromosomes(); // CARMEN, equivalent depth
    int[][] xp57_depth = PosDepth.emptyChromosomes(); // SN055525, equivalent depth

    public PosDepth(File depthFile) {
        this.buildFromDepthFile(depthFile);
    }

    /**
     * Reads plain text or .gz files without retaining the input text in memory.
     * Each chromosome must form one contiguous block, with strictly increasing
     * positive positions and non-negative integer depths. Chromosome blocks may
     * occur in any order. Blank lines are ignored; headers are not accepted.
     * Existing data is replaced only after the complete file has been read successfully.
     *
     */
    public void buildFromDepthFile(File depthFile) {
        List<int[]> positions = new ArrayList<>(Arrays.asList(emptyChromosomes()));
        List<int[]> xp56 = new ArrayList<>(Arrays.asList(emptyChromosomes()));
        List<int[]> xp57 = new ArrayList<>(Arrays.asList(emptyChromosomes()));
        List<String> names = new ArrayList<>(Arrays.asList(knownChromosomeNames()));
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < names.size(); i++) indices.put(names.get(i), i);
        IntArrayList positionList = new IntArrayList();
        IntArrayList xp56List = new IntArrayList();
        IntArrayList xp57List = new IntArrayList();
        int chrIndex = -1;
        try (DepthReader reader = new DepthReader(depthFile)) {
            while (reader.next()) {
                String name = reader.chromosome();
                if (chrIndex < 0 || !names.get(chrIndex).equals(name)) {
                    if (chrIndex >= 0) {
                        positions.set(chrIndex, positionList.toIntArray());
                        xp56.set(chrIndex, xp56List.toIntArray());
                        xp57.set(chrIndex, xp57List.toIntArray());
                        positionList.clear();
                        xp56List.clear();
                        xp57List.clear();
                    }
                    Integer index = indices.get(name);
                    if (index == null) {
                        index = names.size();
                        indices.put(name, index);
                        names.add(name);
                        positions.add(new int[0]);
                        xp56.add(new int[0]);
                        xp57.add(new int[0]);
                    }
                    chrIndex = index;
                }
                positionList.add(reader.position());
                xp56List.add(reader.xp56Depth());
                xp57List.add(reader.xp57Depth());
            }
            if (chrIndex >= 0) {
                positions.set(chrIndex, positionList.toIntArray());
                xp56.set(chrIndex, xp56List.toIntArray());
                xp57.set(chrIndex, xp57List.toIntArray());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read depths from " + depthFile.getAbsolutePath(), e);
        }
        String[] newNames = names.toArray(String[]::new);
        int[][] newPositions = positions.toArray(int[][]::new);
        int[][] newXp56 = xp56.toArray(int[][]::new);
        int[][] newXp57 = xp57.toArray(int[][]::new);
        this.chromosomeNames = newNames;
        this.ref_pos = newPositions;
        this.xp56_depth = newXp56;
        this.xp57_depth = newXp57;
    }

    /**
     * Constant-memory reader shared by the in-memory loader and the disk prefix index.
     * No row objects or depth arrays are retained. Returned depths are already doubled.
     * Each chromosome must occupy a single block, in any chromosome order.
     */
    public static final class DepthReader implements AutoCloseable {
        private final File file;
        private final BufferedReader input;
        private final Set<String> seen = new HashSet<>();
        private long lineNumber;
        private String chromosome;
        private int position;
        private int depth56;
        private int depth57;

        public DepthReader(File file) {
            this.file = file;
            this.input = IOTool.getBufferedReader(file);
        }

        public boolean next() throws IOException {
            String line;
            while ((line = input.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                try {
                    int a = line.indexOf('\t');
                    int b = a < 0 ? -1 : line.indexOf('\t', a + 1);
                    int c = b < 0 ? -1 : line.indexOf('\t', b + 1);
                    if (a <= 0 || b < 0 || c < 0 || line.indexOf('\t', c + 1) >= 0) {
                        throw new IllegalArgumentException("Expected chromosome, position, XP56 depth and XP57 depth (four TSV columns)");
                    }
                    int newPosition = Integer.parseInt(line, a + 1, b, 10);
                    int observed56 = Integer.parseInt(line, b + 1, c, 10);
                    int observed57 = Integer.parseInt(line, c + 1, line.length(), 10);
                    if (newPosition <= 0 || observed56 < 0 || observed57 < 0) {
                        throw new IllegalArgumentException("Position must be positive and depths must be non-negative");
                    }
                    if (observed56 > Integer.MAX_VALUE / EQUIVALENT_DEPTH_FACTOR
                            || observed57 > Integer.MAX_VALUE / EQUIVALENT_DEPTH_FACTOR) {
                        throw new IllegalArgumentException("Equivalent depth exceeds the integer limit after multiplying by two");
                    }
                    if (chromosome == null || chromosome.length() != a || !line.regionMatches(0, chromosome, 0, a)) {
                        String name = line.substring(0, a);
                        if (name.isBlank()) throw new IllegalArgumentException("Blank chromosome name");
                        if (!seen.add(name)) {
                            throw new IllegalArgumentException("Chromosome reappears in a non-contiguous block: " + name);
                        }
                        chromosome = name;
                        position = 0;
                    }
                    if (newPosition <= position) {
                        throw new IllegalArgumentException("Positions must be strictly increasing within " + chromosome
                                + ": " + position + " followed by " + newPosition);
                    }
                    position = newPosition;
                    depth56 = observed56 * EQUIVALENT_DEPTH_FACTOR;
                    depth57 = observed57 * EQUIVALENT_DEPTH_FACTOR;
                    return true;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(file.getAbsolutePath() + ":" + lineNumber + ": " + e.getMessage(), e);
                }
            }
            return false;
        }

        public String chromosome() { return chromosome; }
        public int position() { return position; }
        public int xp56Depth() { return depth56; }
        public int xp57Depth() { return depth57; }
        @Override public void close() throws IOException { input.close(); }
    }

    public String[] getChromosomeNames() {
        return this.chromosomeNames.clone();
    }

    public int getChromosomeIndex(String chromosome) {
        return PosDepth.chromosomeIndex(chromosomeNames, chromosome);
    }

    public int getPositionCount(Chromosome chromosome) {
        return ref_pos[chromosome.ordinal()].length;
    }

    public int getPositionCount(String chromosome) {
        int index = getChromosomeIndex(chromosome);
        return index < 0 ? 0 : ref_pos[index].length;
    }

    public long getTotalPositionCount() {
        long count = 0;
        for (int[] positions : ref_pos) count += positions.length;
        return count;
    }

    public int getPositionIndex(String chromosome, int position) {
        if (position <= 0) throw new IllegalArgumentException("Position must be 1-based and positive");
        int chrIndex = getChromosomeIndex(chromosome);
        if (chrIndex < 0) return -1;
        int index = Arrays.binarySearch(ref_pos[chrIndex], position);
        return index < 0 ? -1 : index;
    }

    public int getXp56Depth(String chromosome, int position) {
        return getDepth(xp56_depth, chromosome, position);
    }

    public int getXp57Depth(String chromosome, int position) {
        return getDepth(xp57_depth, chromosome, position);
    }

    private int getDepth(int[][] depths, String chromosome, int position) {
        if (position <= 0) throw new IllegalArgumentException("Position must be 1-based and positive");
        int chrIndex = getChromosomeIndex(chromosome);
        if (chrIndex < 0) return -1;
        int index = Arrays.binarySearch(ref_pos[chrIndex], position);
        return index < 0 ? 0 : depths[chrIndex][index];
    }

    private static int[][] emptyChromosomes() {
        int[][] data = new int[Chromosome.values().length][];
        Arrays.setAll(data, i -> new int[0]);
        return data;
    }

    private static String[] knownChromosomeNames() {
        return Arrays.stream(Chromosome.values()).map(Chromosome::getName).toArray(String[]::new);
    }

    private static int chromosomeIndex(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) return i;
        }
        return -1;
    }
}
