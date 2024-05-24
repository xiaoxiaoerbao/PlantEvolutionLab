package utils;

public class Benchmark {

    public static long getTimeSpanNanoseconds (long timeStart) {
        return System.nanoTime()-timeStart;
    }

    public static double getTimeSpanMilliseconds (long timeStart) {
        return (double)getTimeSpanNanoseconds(timeStart)/1000000;
    }

    public static double getTimeSpanSeconds (long timeStart) {
        return (double)getTimeSpanNanoseconds(timeStart)/1000000000;
    }

    public static double getTimeSpanMinutes (long timeStart) {
        return getTimeSpanSeconds(timeStart) /60;
    }

    public static double getTimeSpanHours (long timeStart) {
        return getTimeSpanMinutes(timeStart) /60;
    }

    public static double getUsedMemoryGb () {
        return (double)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024/1024/1024;
    }
}
