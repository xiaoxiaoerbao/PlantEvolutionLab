package alignment;

public class MatrixOperation {

    /**
     * @param matrix1 20 * 20
     * @param matrix2 20 * 20
     * @return matrix1 * matrix2
     */
    public static double[][] multiple(double[][] matrix1, double[][] matrix2){
        int rows1 = matrix1.length;
        int cols1 = matrix1[0].length;
        int cols2 = matrix2[0].length;

        // 初始化结果矩阵，其行数为 matrix1 的行数，列数为 matrix2 的列数
        double[][] result = new double[rows1][cols2];

        // 进行矩阵乘法运算
        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < cols2; j++) {
                for (int k = 0; k < cols1; k++) {
                    result[i][j] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }

        return result;
    }

}
