package maf;


import alignment.AminoAcid;
import dutils.Range;

public class Start {

    public static void main(String[] args) {
        AminoAcid aminoAcid = AminoAcid.N;
        System.out.println(aminoAcid.getAscii());
        Range range = new Range("1A", 3, 4);
        System.out.println(range.chr());
    }
}
