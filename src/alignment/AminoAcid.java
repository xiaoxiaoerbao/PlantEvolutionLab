package alignment;

public enum AminoAcid {

    A("Ala", "Alanine", (byte) 'A'),
    C( "Cys", "Cysteine", (byte) 'C'),
    D( "Asp", "Aspartic acid", (byte) 'D'),
    E( "Glu", "Glutamic acid", (byte) 'E'),
    F( "Phe", "Phenylalanine", (byte) 'F'),
    G( "Gly", "Glycine", (byte) 'G'),
    H( "His", "Histidine", (byte) 'H'),
    I( "Ile", "Isoleucine", (byte) 'I'),
    K( "Lys", "Lysine", (byte) 'K'),
    L( "Leu", "Leucine", (byte) 'L'),
    M( "Met", "Methionine", (byte) 'M'),
    N( "Asn", "Asparagine", (byte) 'N'),
    P( "Pro", "Proline", (byte) 'P'),
    Q( "Gln", "Glutamine", (byte) 'Q'),
    R("Arg", "Arginine", (byte) 'R'),
    S( "Ser", "Serine", (byte) 'S'),
    T( "Thr", "Threonine", (byte) 'T'),
    V( "Val", "Valine", (byte) 'V'),
    W( "Trp", "Tryptophan", (byte) 'W'),
    Y( "Tyr", "Tyrosine", (byte) 'Y');

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
