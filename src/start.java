import utils.PStringUtils;

public class start {

    public static void main(String[] args) {
        String aa = "AAAAAAAAAAAAAAAAAAAAAAAAAA";
        String bb = PStringUtils.formatStringToMultipleLines(aa, 5);
        System.out.println(bb);
        System.out.println(PStringUtils.getNDigitNumber(4, 5));
    }
}
