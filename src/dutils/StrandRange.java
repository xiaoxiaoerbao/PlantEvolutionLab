package dutils;


/**
 * Represents a genomic range on a chromosome with a specific DNA strand orientation.
 * <p>
 * Provides methods to get the start and end coordinates of the range on both
 * the forward (plus) and reverse (minus) strands.
 * Coordinates are 0-based and use half-open intervals [start, end).
 * </p>
 * Example:
 * <pre>
 * StrandRange range1 = new StrandRange(Strand.PLUS, 2, 5, 10);
 * int plusStart = range1.getPlusStart();   // 2
 * int plusEnd = range1.getPlusEnd();       // 7
 * int minusStart = range1.getMinusStart(); // 3
 * int minusEnd = range1.getMinusEnd();     // 8
 *
 * StrandRange range2 = new StrandRange(Strand.MINUS, 2, 5, 10);
 * int plusStart = range2.getPlusStart();   // 3
 * int plusEnd = range2.getPlusEnd();       // 8
 * int minusStart = range2.getMinusStart(); // 2
 * int minusEnd = range2.getMinusEnd();     // 7
 * </pre>
 *
 * @author Xu Daxing
 * @version 1.1
 * @since 2025-11-10
 */
public class StrandRange {

    /** DNA strand of this range, either PLUS or MINUS */
    Strand strand;

    /** Start position of the range (0-based, inclusive) */
    private final int start;

    /** Length of the range */
    private final int len;

    /** Total length of the chromosome */
    private final int chromSize;

    /**
     * Constructs a StrandRange object with the specified parameters.
     * @param strand     the DNA strand of the range, must be either PLUS or MINUS
     * @param start      the start position of the range (0-based, inclusive)
     * @param len        the length of the range
     * @param chromSize  the total length of the chromosome, must be greater than or equal to start + len
     */
    public StrandRange(Strand strand, int start, int len, int chromSize) {
        assert strand != null : "strand cannot be null";
        assert start >= 0 : "start cannot be negative";
        assert len >= 0 : "len cannot be negative";
        assert start + len <= chromSize : "start + len must be less than or equal to chromSize";
        this.strand = strand;
        this.start = start;
        this.len = len;
        this.chromSize = chromSize;
    }

    /**
     * Returns the start coordinate of the range on the minus strand.
     * <p>
     * For a range on the PLUS strand, this is calculated as (chromSize - (start + len)).
     * For a range on the MINUS strand, this is the same as the original start.
     * </p>
     * @return the start coordinate on the minus strand
     */
    public int getMinusStart () {
       if (strand.equals(Strand.PLUS)){
           return chromSize - (start+len);
       }else {
           return start;
       }
    }

    /**
     * Returns the end coordinate of the range on the minus strand.
     * <p>
     * For a range on the PLUS strand, this is calculated as (chromSize - start).
     * For a range on the MINUS strand, this is the original end (start + len).
     * </p>
     * @return the end coordinate on the minus strand
     */
    public int getMinusEnd () {
       if (strand.equals(Strand.PLUS)){
           return chromSize -start;
       }else  {
           return start+len;
       }
    }

    /**
     * Returns the start coordinate of the range on the plus strand.
     * <p>
     * For a range on the PLUS strand, this is the original start.
     * For a range on the MINUS strand, this is calculated as (chromSize - (start + len)).
     * </p>
     * @return the start coordinate on the plus strand
     */
    public int getPlusStart () {
       if (strand.equals(Strand.PLUS)){
           return start;
       }else {
           return chromSize-(start+len);
       }
    }

    /**
     * Returns the end coordinate of the range on the plus strand.
     * <p>
     * For a range on the PLUS strand, this is (start + len).
     * For a range on the MINUS strand, this is calculated as (chromSize - start).
     * </p>
     * @return the end coordinate on the plus strand
     */
    public int getPlusEnd () {
        if (strand.equals(Strand.PLUS)){
            return start+len;
        }else {
            return chromSize-start;
        }
    }

}
