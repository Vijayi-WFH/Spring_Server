package com.tse.core.utils;

import java.sql.Date;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {

    public static Long differanceBetweenTwoDates(LocalDate from, LocalDate to) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        java.util.Date firstDate = sdf.parse(from.toString());
        java.util.Date secondDate = sdf.parse(to.toString());
        long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public static LocalTime convertLocalTimeToServerTimeZone(LocalTime localTimeToConvert, String localTimeZone){
        if (localTimeToConvert == null) return null;

        ZoneId zoneId = ZoneId.of(localTimeZone); // Get the ZoneId object for the time zone
        ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDate.now(), localTimeToConvert, zoneId); // Combine the local time with the time zone to create a ZonedDateTime
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime(); // Convert the ZonedDateTime to the server time
    }

    public static LocalDateTime convertServerDateToLocalTimezone(LocalDateTime serverDateToConvert, String localTimeZone) {
        if (serverDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZoneId serverTimeZone = ZoneId.of(String.valueOf(ZoneId.systemDefault()));
        String stringDate = serverDateToConvert.format(timeFormatter);
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, timeFormatter);
        ZonedDateTime zonedDateTimeInServerTimeZone = ZonedDateTime.of(localDateTime, serverTimeZone);
        ZonedDateTime zonedDateTimeInLocalTimeZone = zonedDateTimeInServerTimeZone.withZoneSameInstant(ZoneId.of(localTimeZone));
        LocalDateTime localDateTimeInLocalTimeZone = LocalDateTime.from(zonedDateTimeInLocalTimeZone);
        LocalDate onlyLocalDate = localDateTimeInLocalTimeZone.toLocalDate();
        LocalTime onlyLocalTime = localDateTimeInLocalTimeZone.toLocalTime();
        return LocalDateTime.of(onlyLocalDate, onlyLocalTime);
    }

}
