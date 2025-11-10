package dutils;

import java.util.Objects;


/**
 * Represents a genomic range on a chromosome with 0-based coordinates.
 * The range is half-open: [start, end), where start is inclusive and end is exclusive.
 * Provides methods for comparison, overlap detection, intersection, union, and distance calculations.
 * @author Xudaxing
 * @version 1.0
 * @since 2025-11-10
 */
public class Range implements Comparable<Range>{

     private final String chr; // Chromosome name, e.g., "1A", "2B". Cannot be null.
     private final int start; // 0-based inclusive, must be non-negative
     private final int end;   // 0-based exclusive, must be greater than start

    /**
     * Constructs a Range with the specified chromosome, start, and end positions.
     * @param chr  the chromosome name, cannot be null
     * @param start the 0-based inclusive start position, must be non-negative
     * @param end   the 0-based exclusive end position, must be greater than start
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Range(String chr, int start, int end) {
        if (chr == null) throw new IllegalArgumentException("chr cannot be null");
        if (start < 0) throw new IllegalArgumentException("start must be non-negative");
        if (end <= start) throw new IllegalArgumentException("end must be greater than start");
        this.chr = chr;
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the length of the range, which is end - start.
     * @return the length of the range
     */
     public int length(){
        return end-start;
    }

    /**
     * Returns the chromosome name of this range.
     * @return the chromosome name
     */
    public String getChr(){
        return chr;
    }

    /**
     * Returns the 0-based inclusive start position of this range.
     * @return the start position
     */
    public int getStart(){
        return start;
    }

    /**
     * Returns the 0-based exclusive end position of this range.
     * @return the end position
     */
    public int getEnd(){
        return end;
    }

    /**
     * Compares this range to another range primarily by chromosome, then by start position, and finally by end position.
     * @param o the other range to compare to
     * @return a negative integer, zero, or a positive integer as this range is less than, equal to, or greater than the specified range
     */
    @Override
    public int compareTo(Range o) {
        int chrCompare = this.chr.compareTo(o.chr);
        if (chrCompare != 0) return chrCompare;
        int s = Integer.compare(this.start, o.start);
        if (s != 0) return s;
        return Integer.compare(this.end, o.end);
    }

    /**
     * Returns a string representation of this range in the format "chr:start-end".
     * @return the string representation of this range
     */
    @Override
    public String toString() {
        return chr + ":" + start + "-" + end;
    }

    /**
     * Returns true if this range is equal to the specified object.
     * Two ranges are equal if they have the same chromosome, start, and end positions.
     * @param o the object to compare to
     * @return true if this range is equal to the specified object, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Range range = (Range) o;
        return chr.equals(range.chr) && start == range.start && end == range.end;
    }

    /**
     * Returns a hash code value for this range.
     * @return the hash code value for this range
     */
    @Override
    public int hashCode() {
        return Objects.hash(chr, start, end);
    }

    /**
     * Returns true if this range overlaps with the specified range.
     * @param range the other range to check overlap with
     * @return true if this range overlaps with the specified range, false otherwise
     */
    public boolean overlaps(Range range){
        if (range == null) return false;
        return this.chr.equals(range.getChr()) && this.start < range.getEnd() && this.end > range.getStart();
    }

    /**
     * Returns the intersection of this range with the specified range.
     * If the ranges do not overlap, returns null.
     * @param other the other range to intersect with
     * @return the intersection range, or null if the ranges do not overlap
     */
    public Range intersect(Range other){
        if (other == null) return null;
        assert this.chr.equals(other.getChr()) : "Chromosome mismatch: " + this.chr + " vs " + other.getChr();
        if (this.overlaps(other)){
            int start = Math.max(this.start, other.getStart());
            int end = Math.min(this.end, other.getEnd());
            return new Range(this.chr, start, end);
        }
        return null;
    }

    /**
     * Returns true if this range contains the specified range.
     * @param other the other range to check containment with
     * @return true if this range contains the specified range, false otherwise
     */
    public boolean contains(Range other) {
        if (other == null) return false;
        return this.chr.equals(other.getChr()) && this.start <= other.getStart() && this.end >= other.getEnd();
    }

    /**
     * union two ranges
     * if two ranges do not overlap, return null
     * Adjacent ranges are NOT merged: e.g. [0,2) and [2,5) => null
     * @param other the other range to union with
     * @return the union range, or null if two ranges do not overlap
     */
    public Range union(Range other) {
        if (other == null) return null;
        assert this.chr.equals(other.getChr()) : "Chromosome mismatch: " + this.chr + " vs " + other.getChr();
        if (this.overlaps(other)){
            int start = Math.min(this.start, other.getStart());
            int end = Math.max(this.end, other.getEnd());
            return new Range(this.chr, start, end);
        }
        return null;
    }

    /**
     * calculate the distance between two ranges
     * if two ranges overlap, return 0
     * [0，2) and [2，5) don't overlap, distance return 1, not 0
     * @param other the other range to calculate distance to
     * @return the distance between two ranges, or Integer.MAX_VALUE if other is null. note this distance is 1-based
     */
    public int distanceTo(Range other){
        if (other == null) return Integer.MAX_VALUE;
        assert this.chr.equals(other.getChr()) : "Chromosome mismatch: " + this.chr + " vs " + other.getChr();
        if (this.overlaps(other)){
            return 0;
        }
        if (this.end <= other.start){
            return other.start - this.end + 1;
        }
        return this.start - other.end + 1;
    }

     /**
     * union two ranges with a specified distance
     * @param other the other range to union with
     * @param distance the specified distance between two ranges
     * @return the union range, or null if two ranges do not overlap with the specified distance
     */
    public Range unionWithDistance(Range other, int distance){
        if (other == null) return null;
        assert distance > 0 : "distance must be positive integer";
        assert this.chr.equals(other.getChr()) : "Chromosome mismatch";
        // If they overlap normally, merge directly
        if (this.overlaps(other)) {
            return union(other);
        }

        // If they do not overlap, check if the distance is sufficient
        int oneBasedDistance = this.distanceTo(other);

        if (oneBasedDistance <= distance) {
            int start = Math.min(this.start, other.getStart());
            int end = Math.max(this.end, other.getEnd());
            return new Range(this.chr, start, end);
        }

        return null;
    }


}
