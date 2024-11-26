package utils;

public class PStringUtils {

    /**
     * Return a string of number filled with 0 on the left
     * @param n The total length of the returned string, including the leading zeros.
     * @param num The number to be padded with leading zeros.
     * @return A string of the number padded with leading zeros to the specified length of {@code n}.
     */
    public static String getNDigitNumber (int n, int num) {
        StringBuilder s = new StringBuilder(String.valueOf(num));
        int cnt = n-1;
        for (int i = 0; i < cnt; i++) {
            s.insert(0, "0");
        }
        return s.toString();
    }

    /**
     * Formats a string into multiple lines, each containing a specified maximum number of characters.
     * This method takes an input string and breaks it into multiple lines, inserting a newline character ('\n') after every
     * {@code maxCharactersPerLine} characters. This is useful for formatting long strings into manageable chunks, such as in
     * display purposes or file output.
     * @param inputString The string to be formatted into multiple lines.
     * @param maxCharactersPerLine The maximum number of characters allowed in each line.
     * @return A string where the original string is split into multiple lines, each containing up to {@code maxCharactersPerLine} characters.
     */
    public static String formatStringToMultipleLines(String inputString, int maxCharactersPerLine) {
        StringBuilder formattedString = new StringBuilder();
        int characterCount = 0;
        for (int i = 0; i < inputString.length(); i++) {
            formattedString.append(inputString.charAt(i));
            characterCount++;
            if (characterCount % maxCharactersPerLine == 0) {
                formattedString.append("\n");
            }
        }
        return formattedString.toString();
    }

}
