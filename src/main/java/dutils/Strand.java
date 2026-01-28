package dutils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enum representing DNA strand orientation.
 * There are two possible strands: FORWARD ("+") and REVERSE ("-").
 * Provides methods to get the string representation and retrieve enum instance from a string.
 *
 * @author Xu Daxing
 * @version 1.0
 * @since 2025-11-10
 */
public enum Strand {

    /** Forward strand represented by "+" */
    PLUS("+"),
    /** Reverse strand represented by "-" */
    MINUS("-");

    /** String representation of the strand */
    final String str;

    /**
     * Constructs a Strand enum constant with the specified string representation.
     *
     * @param str the string representation of the strand, must be either "+" or "-"
     */
    Strand(String str){
        this.str=str;
    }

    /**
     * Returns the string representation of the strand.
     * @return the string representation of the strand, either "+" or "-"
     */
    public String getStr() {
        return str;
    }

    /**
     * Maps string representations of strands to their corresponding enum constants.
     */
    private static final Map<String, Strand> strandMap= Arrays.stream(values()).collect(Collectors.toMap(Strand::getStr, e->e));

    /**
     * Returns the Strand enum constant corresponding to the specified string representation.
     * @param strand the string representation of the strand, must be either "+" or "-"
     * @return the Strand enum constant corresponding to the specified string representation
     */
    public static Strand getInstanceFrom(String strand) {
        Strand s = strandMap.get(strand);
        if (s == null) throw new IllegalArgumentException("Invalid strand: " + strand);
        return s;
    }

}
