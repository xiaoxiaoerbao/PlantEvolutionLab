import alignment.Alignment;
import alignment.DnaSimilarityMatrix;
import alignment.SimilarityMatrix;

public class Start {

    public static void main(String[] args) {
        String aa = "GATTACA";
        String bb = "GTCGACGCA";
        int[][] similarityMatrix = {{1,-1,-1,-1},{-1,1,-1,-1},{-1,-1,1,-1},{-1,-1,-1,1}};
        SimilarityMatrix dnaSimilarityMatrix = new DnaSimilarityMatrix(similarityMatrix);
        Alignment.alignNeedlemanWunschAlgorithm(aa.getBytes(), bb.getBytes(), dnaSimilarityMatrix, -2);
        System.out.println();
    }
}
