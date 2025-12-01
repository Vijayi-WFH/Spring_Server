package com.tse.core_application.exception;

public class DateAndTimePairFailedException extends RuntimeException{

    public DateAndTimePairFailedException(String dateName, String timeName) {
        super("DateAndTimePair Failed: Provide both " + dateName + " and " + timeName);
    }
}
