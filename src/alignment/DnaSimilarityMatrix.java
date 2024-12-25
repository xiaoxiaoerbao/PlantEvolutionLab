package alignment;

import it.unimi.dsi.fastutil.bytes.Byte2IntMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap;

public class DnaSimilarityMatrix implements SimilarityMatrix {

    /**
     * 4 * 4
     * sort by base ascii
     */
    int[][] similarityMatrix;

    private static final Byte2IntMap BASE_ASCII_TO_INDEX_MAP = baseAsciiToIndexMap();

    private static Byte2IntMap baseAsciiToIndexMap(){
        byte[] bases = {'A', 'C', 'G', 'T'};
        Byte2IntMap indexMap = new Byte2IntOpenHashMap();
        for(int i = 0; i < bases.length; i++){
            indexMap.put(bases[i], i);
        }
        return indexMap;
    }

    /**
     * default matrix, 1 for match, -1 for mismatch
     */
    private static final int[][] defaultMatrix = {{1,-1,-1,-1},{-1,1,-1,-1},{-1,-1,1,-1},{-1,-1,-1,1}};

    public DnaSimilarityMatrix(int[][] matrix) {
        assert matrix.length == matrix[0].length : "check your similarityMatrix, it must be a square matrix";
        assert matrix.length == 4 : "DNA similarityMatrix length must be 4";
        this.similarityMatrix = matrix;
    }

    @Override
    public int getSimilarityValueBetween(byte baseAscii1, byte baseAscii2) {
        return similarityMatrix[BASE_ASCII_TO_INDEX_MAP.get(baseAscii1)][BASE_ASCII_TO_INDEX_MAP.get(baseAscii2)];
    }
}
