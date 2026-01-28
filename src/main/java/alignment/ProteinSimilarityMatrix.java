package alignment;


/**
 * PAM score matrix and BLOSUM score matrix
 * https://www.ncbi.nlm.nih.gov/IEB/ToolBox/C_DOC/lxr/source/data/
 */
public class ProteinSimilarityMatrix implements alignment.SimilarityMatrix {

    private static final int AMINO_ACID_COUNT = 20; // Fixed size for the amino acids

    /**
     * 20 * 20
     * sort by AminoAcid
     */
    int[][] similarityMatrix;

    public ProteinSimilarityMatrix(int[][] similarityMatrix) {
        assert similarityMatrix.length == similarityMatrix[0].length : "check your similarityMatrix, it must be a square matrix";
        assert similarityMatrix.length == AMINO_ACID_COUNT : "protein similarityMatrix length must be 20";
        this.similarityMatrix = similarityMatrix;
    }

    @Override
    public int getSimilarityValueBetween(byte aminoAcidAscii1, byte aminoAcidAscii2) {
        int index1 = AminoAcid.AMINO_ACID_ASCII_TO_INDEX_MAP.get(aminoAcidAscii1);
        int index2 = AminoAcid.AMINO_ACID_ASCII_TO_INDEX_MAP.get(aminoAcidAscii2);
        return similarityMatrix[index1][index2];
    }

    public static int[][] calculatePAM_n_score_matrix(int n){
        double[][] pam_n_mutation_probability_matrix = DefaultSimilarityMatrix.calculate_PAM_N_MUTATION_PROBABILITY_MATRIX(n);
        return DefaultSimilarityMatrix.calculate_PAM_log_odds_MUTATION_PROBABILITY_MATRIX(pam_n_mutation_probability_matrix);
    }

    public static final int[][] PAM_30_SCORE_MATRIX = calculatePAM_n_score_matrix(30);

    public static final int[][] PAM_70_SCORE_MATRIX = calculatePAM_n_score_matrix(70);

    public static final int[][] PAM_250_SCORE_MATRIX = calculatePAM_n_score_matrix(250);

    /**
     * https://bioinformaticshome.com/bioinformatics_tutorials/sequence_alignment/substitution_matrices_page2.html
     */
    private static final class DefaultSimilarityMatrix {

        /**
         * 20 * 20
         * order by AminoAcid
         * col is original amino acid, row is mutated amino acid
         */
        private static final int[][] POINT_ACCEPTED_MUTATION_MATRIX = {
                {0, 30, 109, 154, 33, 93, 266, 579, 21, 66, 95, 57, 29, 20, 345, 772, 590, 0, 20, 365},
                {30,0,17,0,10,120,0,10,103,30,17,477,17,7,67,137,20,27,3,20},
                {109,17,0,532,0,50,94,156,226,36,37,322,0,7,27,432,169,3,36,13},
                {154,0,532,0,0,76,831,162,43,13,0,85,0,0,10,98,57,0,0,17},
                {33,10,0,0,0,0,0,10,10,17,0,0,0,0,10,117,10,0,30,33},
                {93,120,50,76,0,0,422,30,243,8,75,147,20,0,93,47,37,0,0,27},
                {266,0,94,831,0,422,0,112,23,35,15,104,7,0,40,86,31,0,10,37},
                {579,10,156,162,10,30,112,0,10,0,17,60,7,17,49,450,50,0,0,97},
                {21,103,226,43,10,243,23,10,0,3,40,23,0,20,50,26,14,3,40,30},
                {66,30,36,13,17,8,35,0,3,0,253,43,57,90,7,20,129,0,13,661},
                {95,17,37,0,0,75,15,17,40,253,0,39,207,167,43,32,52,13,23,303},
                {57,477,322,85,0,147,104,60,23,43,39,0,90,0,43,168,200,0,10,17},
                {29,17,0,0,0,20,7,7,0,57,207,90,0,17,4,20,28,0,0,77},
                {20,7,7,0,0,0,0,17,20,90,167,0,17,0,7,40,10,10,260,10},
                {345,67,27,10,10,93,40,49,50,7,43,43,4,7,0,269,73,0,0,50},
                {772,137,432,98,117,47,86,450,26,20,32,168,20,40,269,0,696,17,22,43},
                {590,20,169,57,10,37,31,50,14,129,52,200,28,10,73,696,0,0,23,186},
                {0,27,3,0,0,0,0,0,3,0,13,0,0,10,0,17,0,0,6,0},
                {20,3,36,0,30,0,10,0,40,13,23,10,0,260,0,22,23,6,0,17},
                {365,20,13,17,33,27,37,97,30,661,303,17,77,10,50,43,186,0,17,0}};

        /**
         * Sorting by AminoAcid
         */
        private static final int[] RELATIVE_MUTABILITIES = {100, 65, 134, 106, 20, 93, 102, 49, 66, 96, 40, 56, 94, 41, 56, 120, 97, 18, 41, 74};

        private static int[] sumCol(){
            int[] colSum = new int[AMINO_ACID_COUNT];
            for (int j = 0; j < AMINO_ACID_COUNT; j++) {
                for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                    colSum[j]=colSum[j]+POINT_ACCEPTED_MUTATION_MATRIX[i][j];
                }
            }
            return colSum;
        }

        private static double[] normalized_background_frequencies(){
            int[] colSum = DefaultSimilarityMatrix.sumCol();
            double[] background_frequencies = new double[AMINO_ACID_COUNT];
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                background_frequencies[i] = (colSum[i])/RELATIVE_MUTABILITIES[i];
            }
            double sumBackgroundFrequency = 0;
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                sumBackgroundFrequency += background_frequencies[i];
            }
            double[] normalized_background_frequencies = new double[AMINO_ACID_COUNT];
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                normalized_background_frequencies[i] = background_frequencies[i]/sumBackgroundFrequency;
            }
            return normalized_background_frequencies;
        }

        /**
         * mutation probability matrix is not symmetric
         * @return mutation probability matrix
         */
        private static double[][] calculateMutationProbabilityMatrix(){
            double[][] probabilityMatrix = new double[AMINO_ACID_COUNT][AMINO_ACID_COUNT];
            // j means the column, i means the row
            // Calculate column sums for normalization
            int[] colSum = DefaultSimilarityMatrix.sumCol();

            double[] normalized_background_frequencies = DefaultSimilarityMatrix.normalized_background_frequencies();

            double totalProbabilitySpace = 0;
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                totalProbabilitySpace += RELATIVE_MUTABILITIES[i] * normalized_background_frequencies[i];
            }
            double[] scaled_mutation_probability = new double[AMINO_ACID_COUNT];
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                scaled_mutation_probability[i] = RELATIVE_MUTABILITIES[i] / (totalProbabilitySpace * 100);
            }
            double[] scale_factor = new double[AMINO_ACID_COUNT];
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                scale_factor[i] = scaled_mutation_probability[i]/colSum[i];
            }
            for (int j = 0; j < AMINO_ACID_COUNT; j++) {
                for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                    probabilityMatrix[i][j] = scale_factor[j] * POINT_ACCEPTED_MUTATION_MATRIX[i][j];
                }
            }
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                probabilityMatrix[i][i] = (1 - scaled_mutation_probability[i]);
            }
            return probabilityMatrix;
        }

        public static final double[][] PAM1_MUTATION_PROBABILITY_MATRIX = calculateMutationProbabilityMatrix();

        public static double[][] PAM2_MUTATION_PROBABILITY_MATRIX(){
            return alignment.MatrixOperation.multiple(PAM1_MUTATION_PROBABILITY_MATRIX,
                    PAM1_MUTATION_PROBABILITY_MATRIX);
        }

        public static double[][] calculate_PAM_N_MUTATION_PROBABILITY_MATRIX(int n){
            assert n > 1 : "n needs to be greater than 1";
            double[][] probabilityMatrix = PAM1_MUTATION_PROBABILITY_MATRIX;
            for (int i = 1; i < n; i++) {
                probabilityMatrix = alignment.MatrixOperation.multiple(PAM1_MUTATION_PROBABILITY_MATRIX, probabilityMatrix);
            }
            return probabilityMatrix;
        }

        public static double[][] PAM250_MUTATION_PROBABILITY_MATRIX(){
            return DefaultSimilarityMatrix.calculate_PAM_N_MUTATION_PROBABILITY_MATRIX(250);
        }

        /**
         * log odds matrix
         * @param pam_mutation_probability_matrix pam mutation probability matrix, pam1 or pam2 or pam250 or pam with any length
         * @return pam score matrix
         */
        private static int[][] calculate_PAM_log_odds_MUTATION_PROBABILITY_MATRIX(double[][] pam_mutation_probability_matrix){
            double[] normalized_background_frequencies = DefaultSimilarityMatrix.normalized_background_frequencies();
            double[][] joint_probability_matrix = new double[AMINO_ACID_COUNT][AMINO_ACID_COUNT];
            for (int j = 0; j < AMINO_ACID_COUNT; j++) {
                for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                    joint_probability_matrix[i][j] = pam_mutation_probability_matrix[i][j] * normalized_background_frequencies[j];
                }
            }
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                for (int j = 0; j < i; j++) {
                    joint_probability_matrix[i][j] = (joint_probability_matrix[i][j]+joint_probability_matrix[j][i])/2;
                    joint_probability_matrix[j][i] = joint_probability_matrix[i][j];
                }
            }
            int[][] log_odds_MUTATION_PROBABILITY_MATRIX = new int[AMINO_ACID_COUNT][AMINO_ACID_COUNT];
            for (int i = 0; i < AMINO_ACID_COUNT; i++) {
                for (int j = 0; j < AMINO_ACID_COUNT; j++) {
                    log_odds_MUTATION_PROBABILITY_MATRIX[i][j] = (int) Math.round(Math.log(joint_probability_matrix[i][j]/(normalized_background_frequencies[i]*normalized_background_frequencies[j])));
                }
            }
            return log_odds_MUTATION_PROBABILITY_MATRIX;
        }

    }

}
