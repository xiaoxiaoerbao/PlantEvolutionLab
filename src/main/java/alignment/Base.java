package alignment;

public enum Base {

    A((byte) 'A'),
    C((byte) 'C'),
    G((byte) 'G'),
    T((byte) 'T'),;

    private final byte ascii;

    Base(byte ascii) {
        this.ascii = ascii;
    }

    public byte getAscii() {
        return ascii;
    }

    public static Base fromAscii(byte ascii) {
        for (Base base : Base.values()) {
            if (base.ascii == ascii) {
                return base;
            }
        }
        throw new IllegalArgumentException("There is no corresponding base enumeration instance. The input ASCII code: " + (int) ascii);
    }
}
