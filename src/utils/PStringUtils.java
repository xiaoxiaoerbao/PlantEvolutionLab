package utils;

public class PStringUtils {

    /**
     * Return a string of number filled with 0 on the left
     * @param n
     * @param num
     * @return
     */
    public static String getNDigitNumber (int n, int num) {
        String s = String.valueOf(num);
        int cnt = n-s.length();
        for (int i = 0; i < cnt; i++) {
            s = "0"+s;
        }
        return s;
    }
}
