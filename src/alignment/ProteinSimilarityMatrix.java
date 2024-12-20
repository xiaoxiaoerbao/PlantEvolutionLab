package alignment;


public class ProteinSimilarityMatrix implements SimilarityMatrix {

    /**
     * 20 * 20
     * sort by AminoAcids enum
     */
    int[][] similarityMatrix;

    public ProteinSimilarityMatrix(int[][] similarityMatrix) {
        assert similarityMatrix.length == similarityMatrix[0].length : "check your similarityMatrix, it must be a square matrix";
        assert similarityMatrix.length == 20 : "protein similarityMatrix length must be 20";
        this.similarityMatrix = similarityMatrix;
    }

    @Override
    public int[][] getSimilarityMatrix() {
        return similarityMatrix;
    }

    @Override
    public int getSimilarityValueBetween(byte aminoAcidAscii1, byte aminoAcidAscii2) {
        AminoAcid aminoAcid1 = AminoAcid.fromAscii(aminoAcidAscii1);
        AminoAcid aminoAcid2 = AminoAcid.fromAscii(aminoAcidAscii2);
        return similarityMatrix[aminoAcid1.ordinal()][aminoAcid2.ordinal()];
    }

}
