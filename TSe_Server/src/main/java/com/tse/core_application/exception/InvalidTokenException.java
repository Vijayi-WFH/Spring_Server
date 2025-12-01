package com.tse.core_application.exception;

public class InvalidTokenException extends RuntimeException{

    public InvalidTokenException(String currentTimestampUTC){
        super("Invalid Token ! The current time is " + currentTimestampUTC);

    }
}
