package com.example.chat_app.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    public static LocalDateTime convertUserDateToServerTimezone(LocalDateTime localDateToConvert, String localTimeZone) {
        if (localDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        ZoneId headerTimeZone = ZoneId.of(localTimeZone);
        String stringDate = localDateToConvert.format(timeFormatter);
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, timeFormatter);
        ZonedDateTime zonedDateTimeInLocalTimeZone = ZonedDateTime.of(localDateTime, headerTimeZone);
        ZonedDateTime zonedDateTimeInServerTimeZone = zonedDateTimeInLocalTimeZone.withZoneSameInstant(ZoneId.of(String.valueOf(ZoneId.systemDefault())));
        LocalDateTime localDateTimeInServerTimeZone = LocalDateTime.from(zonedDateTimeInServerTimeZone);
        LocalDate onlyLocalDate = localDateTimeInServerTimeZone.toLocalDate();
        LocalTime onlyLocalTime = localDateTimeInServerTimeZone.toLocalTime();
        return LocalDateTime.of(onlyLocalDate, onlyLocalTime);
    }

    //    this method will convert the server date into the date according to the local timezone
    public static LocalDateTime convertServerDateToUserTimezone(LocalDateTime serverDateToConvert, String localTimeZone) {
        if (serverDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
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
