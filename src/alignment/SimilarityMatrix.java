package alignment;

public interface SimilarityMatrix {

    int[][] getSimilarityMatrix();

    int getSimilarityValueBetween(byte characterAscii1, byte characterAscii2);

}
