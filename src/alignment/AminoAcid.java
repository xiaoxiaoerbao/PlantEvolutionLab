package alignment;

/**
 * Amino acid enumeration class
 * Order by A,R,N,D,C,Q,E,G,H,I,L,K,M,F,P,S,T,W,Y,V
 */
public enum AminoAcid {

    A("Ala", "Alanine", (byte) 'A'),
    R("Arg", "Arginine", (byte) 'R'),
    N("Asn", "Asparagine", (byte) 'N'),
    D("Asp", "Aspartic acid", (byte) 'D'),
    C("Cys", "Cysteine", (byte) 'C'),
    Q("Gln", "Glutamine", (byte) 'Q'),
    E("Glu", "Glutamic acid", (byte) 'E'),
    G("Gly", "Glycine", (byte) 'G'),
    H("His", "Histidine", (byte) 'H'),
    I("Ile", "Isoleucine", (byte) 'I'),
    L("Leu", "Leucine", (byte) 'L'),
    K("Lys", "Lysine", (byte) 'K'),
    M("Met", "Methionine", (byte) 'M'),
    F("Phe", "Phenylalanine", (byte) 'F'),
    P("Pro", "Proline", (byte) 'P'),
    S("Ser", "Serine", (byte) 'S'),
    T("Thr", "Threonine", (byte) 'T'),
    W("Trp", "Tryptophan", (byte) 'W'),
    Y("Tyr", "Tyrosine", (byte) 'Y'),
    V("Val", "Valine", (byte) 'V');

    private final String threeLetter;
    private final String fullName;
    private final byte ascii;

    AminoAcid(String threeLetter, String fullName, byte ascii) {
        this.threeLetter = threeLetter;
        this.fullName = fullName;
        this.ascii = ascii;
    }

    public String getThreeLetter() {
        return threeLetter;
    }

    public String getFullName() {
        return fullName;
    }

    public byte getAscii() {
        return ascii;
    }

    public static AminoAcid fromAscii(byte ascii) {
        for (AminoAcid aminoAcid : AminoAcid.values()) {
            if (aminoAcid.ascii == ascii) {
                return aminoAcid;
            }
        }
        throw new IllegalArgumentException("There is no corresponding amino acid enumeration instance. The input ASCII code: " + (int) ascii);
    }
}
