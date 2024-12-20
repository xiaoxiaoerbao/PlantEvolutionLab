package alignment;

public class DnaSimilarityMatrix {

    /**
     * 4 * 4
     * sort by Base enum
     */
    int[][] similarityMatrix;

    public DnaSimilarityMatrix(int[][] matrix) {
        assert matrix.length == matrix[0].length : "check your similarityMatrix, it must be a square matrix";
        assert matrix.length == 4 : "DNA similarityMatrix length must be 4";
        this.similarityMatrix = matrix;
    }

    public int[][] getSimilarityMatrix() {
        return similarityMatrix;
    }

    public int getSimilarityValueBetween(byte baseAscii1, byte baseAscii2) {
        Base base1 = Base.fromAscii(baseAscii1);
        Base base2 = Base.fromAscii(baseAscii2);
        return similarityMatrix[base1.ordinal()][base2.ordinal()];
    }
}
