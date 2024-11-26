package utils;

/**
 * Utility class for benchmarking performance, including time and memory usage calculations.
 */
public class Benchmark {

    /**
     * Calculates the time span (in nanoseconds) from a given start time.
     * @param startTime The start time (in nanoseconds) captured using System.nanoTime().
     * @return The time span in nanoseconds.
     */
    public static long calculateTimeSpanNanoseconds(long startTime) {
        return System.nanoTime() - startTime;
    }

    /**
     * Calculates the time span in milliseconds from a given start time.
     * @param startTime The start time (in nanoseconds) captured using System.nanoTime().
     * @return The time span in milliseconds.
     */
    public static double calculateTimeSpanMilliseconds(long startTime) {
        return calculateTimeSpanNanoseconds(startTime) / 1_000_000.0;
    }

    /**
     * Calculates the time span in seconds from a given start time.
     *
     * @param startTime The start time (in nanoseconds) captured using System.nanoTime().
     * @return The time span in seconds.
     */
    public static double calculateTimeSpanSeconds(long startTime) {
        return calculateTimeSpanNanoseconds(startTime) / 1_000_000_000.0;
    }

    /**
     * Calculates the time span in minutes from a given start time.
     *
     * @param startTime The start time (in nanoseconds) captured using System.nanoTime().
     * @return The time span in minutes.
     */
    public static double calculateTimeSpanMinutes(long startTime) {
        return calculateTimeSpanSeconds(startTime) / 60.0;
    }

    /**
     * Calculates the time span in hours from a given start time.
     *
     * @param startTime The start time (in nanoseconds) captured using System.nanoTime().
     * @return The time span in hours.
     */
    public static double calculateTimeSpanHours(long startTime) {
        return calculateTimeSpanMinutes(startTime) / 60.0;
    }

    /**
     * Returns the used memory in gigabytes.
     *
     * @return The used memory in gigabytes.
     */
    public static double getUsedMemoryGb() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return usedMemory / 1_073_741_824.0; // 1024 * 1024 * 1024
    }
}
