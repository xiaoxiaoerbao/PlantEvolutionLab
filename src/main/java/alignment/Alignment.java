package alignment;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;

import java.util.Collections;

public class Alignment {

    // Define a unique byte value to represent insertions and deletions.
    // Here, we choose a value that is unlikely to appear in normal data.
    // It can be adjusted according to the actual situation.
    private static final byte GAP_REPRESENTATION = 45; // ASCII code for '-'

    /**
     *
     * @param s1 seq1
     * @param s2 seq2
     * @param similarityMatrix similarity matrix
     * @param gapPenalty gap penalty
     * @return alignment
     */
    public static Pair<ByteList, ByteList> alignNeedlemanWunschAlgorithm(byte[] s1, byte[] s2,
                                                                         alignment.SimilarityMatrix similarityMatrix, int gapPenalty) {
        int s1Length = s1.length;
        int s2Length = s2.length;

        // Initialize the dynamic programming matrix
        int[][] dpMatrix = new int[s1Length + 1][s2Length + 1];
        for (int i = 0; i < s1Length + 1; i++) {
            dpMatrix[i][0] = i * gapPenalty;
        }
        for (int j = 0; j < s2Length + 1; j++) {
            dpMatrix[0][j] = j * gapPenalty;
        }

        int matchScore, deletionScore, insertionScore;
        // Fill in the dynamic programming matrix
        for (int i = 1; i < s1Length + 1; i++) {
            for (int j = 1; j < s2Length + 1; j++) {
                try {
                    matchScore = dpMatrix[i - 1][j - 1] + similarityMatrix.getSimilarityValueBetween(s1[i - 1], s2[j - 1]);
                } catch (Exception e) {
                    // Handle the exception according to the actual situation.
                    // Here, we simply print the exception message and set a default match score value.
                    // Other handling methods can also be considered, such as throwing an exception to the upper layer.
                    System.err.println("An exception occurred while getting the similarity value: " + e.getMessage());
                    matchScore = 0;
                }
                deletionScore = dpMatrix[i - 1][j] + gapPenalty;
                insertionScore = dpMatrix[i][j - 1] + gapPenalty;
                dpMatrix[i][j] = Math.max(Math.max(matchScore, deletionScore), insertionScore);
            }
        }

        ByteList alignment1 = new ByteArrayList();
        ByteList alignment2 = new ByteArrayList();
        int i = s1Length;
        int j = s2Length;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                try {
                    if (dpMatrix[i][j] == (dpMatrix[i - 1][j - 1] + similarityMatrix.getSimilarityValueBetween(s1[i - 1], s2[j - 1]))) {
                        alignment1.add(s1[i - 1]);
                        alignment2.add(s2[j - 1]);
                        i--;
                        j--;
                    } else if (dpMatrix[i][j] == dpMatrix[i - 1][j] + gapPenalty) {
                        alignment1.add(s1[i - 1]);
                        alignment2.add(GAP_REPRESENTATION);
                        i--;
                    } else {
                        alignment1.add(GAP_REPRESENTATION);
                        alignment2.add(s2[j - 1]);
                        j--;
                    }
                } catch (Exception e) {
                    // Handle the exception that may occur during the backtracking process. The handling method can also be adjusted according to the actual requirements.
                    System.err.println("An exception occurred during the backtracking process: " + e.getMessage());
                    // Here, we simply choose to break the loop. Other more reasonable handling logics can also be considered.
                    break;
                }
            } else if (i > 0) {
                alignment1.add(s1[i - 1]);
                alignment2.add(GAP_REPRESENTATION);
                i--;
            } else {
                alignment1.add(GAP_REPRESENTATION);
                alignment2.add(s2[j - 1]);
                j--;
            }
        }
        Collections.reverse(alignment1);
        Collections.reverse(alignment2);
//        for (int k = 0; k < alignment1.size(); k++) {
//            System.out.println((char)alignment1.getByte(k)+ " " + (char)alignment2.getByte(k));
//        }
        return Pair.of(alignment1, alignment2);
    }
}