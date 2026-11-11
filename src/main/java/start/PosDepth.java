package start;

import dutils.Chromosome;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.lang3.StringUtils;
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

/**
 * Parental depths from a headerless, tab-separated file: chromosome, position,
 * XP56 depth, XP57 depth. Positions remain 1-based (unlike the BED-based Range).
 * Known chromosomes use {@link Chromosome#ordinal()} as their array index;
 * additional sequences (such as Un) are appended and can be queried by name.
 * The second index identifies a recorded site, not a genomic coordinate;
 * absent chromosomes have empty arrays and unrecorded sites are not filled with zeros.
 */
public class PosDepth {
    private String[] chromosomeNames = PosDepth.knownChromosomeNames();
    // dim1 is getChromosomeIndex(name), dim2 is the recorded site index
    int[][] ref_pos = PosDepth.emptyChromosomes();
    int[][] xp56_depth = PosDepth.emptyChromosomes();
    int[][] xp57_depth = PosDepth.emptyChromosomes();

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
        int previousPosition = 0;
        String currentChrName = null;
        long lineNumber = 0;
        try (BufferedReader br = IOTool.getBufferedReader(depthFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                try {
                    String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
                    if (columns.length != 4 || columns[0].isBlank()) {
                        throw new IllegalArgumentException("Expected chromosome, position, XP56 depth and XP57 depth (four TSV columns)");
                    }
                    String name = columns[0];
                    int position = Integer.parseInt(columns[1]);
                    int depth56 = Integer.parseInt(columns[2]);
                    int depth57 = Integer.parseInt(columns[3]);
                    if (position <= 0 || depth56 < 0 || depth57 < 0) {
                        throw new IllegalArgumentException("Position must be positive and depths must be non-negative");
                    }
                    if (!name.equals(currentChrName)) {
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
                        if (positions.get(chrIndex).length != 0) {
                            throw new IllegalArgumentException("Chromosome reappears in a non-contiguous block: " + name);
                        }
                        currentChrName = name;
                        previousPosition = 0;
                    }
                    if (position <= previousPosition) {
                        throw new IllegalArgumentException("Positions must be strictly increasing within " + name
                                + ": " + previousPosition + " followed by " + position);
                    }
                    positionList.add(position);
                    xp56List.add(depth56);
                    xp57List.add(depth57);
                    previousPosition = position;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(depthFile.getAbsolutePath() + ":" + lineNumber
                            + ": " + e.getMessage(), e);
                }
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
        int index = getPositionIndex(chromosome, position);
        return index < 0 ? -1 : xp56_depth[this.getChromosomeIndex(chromosome)][index];
    }

    public int getXp57Depth(String chromosome, int position) {
        int index = getPositionIndex(chromosome, position);
        return index < 0 ? -1 : xp57_depth[this.getChromosomeIndex(chromosome)][index];
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
