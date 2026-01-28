package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTime {

    /**
     * Gets the current date and time formatted as "yyyy-MM-dd HH:mm:ss".
     * 2019-10-26 18:03:08
     *
     * @return A string representing the current date and time in the format "yyyy-MM-dd HH:mm:ss".
     */
    public static String getDateTimeOfNow(){
        LocalDateTime localDateTime=LocalDateTime.now();
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
