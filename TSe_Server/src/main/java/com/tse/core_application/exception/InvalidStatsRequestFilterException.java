package com.tse.core_application.exception;

public class InvalidStatsRequestFilterException extends RuntimeException{

    public InvalidStatsRequestFilterException(String filterName) {
        super(filterName + " filter is not available.");
    }
}
