package alignment;


import it.unimi.dsi.fastutil.bytes.Byte2IntMap;
import it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap;

public class ProteinSimilarityMatrix implements SimilarityMatrix {

    /**
     * 20 * 20
     * sort by AminoAcid ascii
     */
    int[][] similarityMatrix;

    private static final Byte2IntMap AMINO_ACID_ASCII_TO_INDEX_MAP = aminoAcidAsciiToIndexMap();

    private static Byte2IntMap aminoAcidAsciiToIndexMap(){
        byte[] aminoAcids = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y'};
        Byte2IntMap indexMap = new Byte2IntOpenHashMap();
        for(int i = 0; i < aminoAcids.length; i++){
            indexMap.put(aminoAcids[i], i);
        }
        return indexMap;
    }

    public ProteinSimilarityMatrix(int[][] similarityMatrix) {
        assert similarityMatrix.length == similarityMatrix[0].length : "check your similarityMatrix, it must be a square matrix";
        assert similarityMatrix.length == 20 : "protein similarityMatrix length must be 20";
        this.similarityMatrix = similarityMatrix;
    }

    @Override
    public int getSimilarityValueBetween(byte aminoAcidAscii1, byte aminoAcidAscii2) {
        AminoAcid aminoAcid1 = AminoAcid.fromAscii(aminoAcidAscii1);
        AminoAcid aminoAcid2 = AminoAcid.fromAscii(aminoAcidAscii2);
        return similarityMatrix[aminoAcid1.ordinal()][aminoAcid2.ordinal()];
    }

}
