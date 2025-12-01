package com.tse.core_application.exception;

public class InvalidMeetingTypeException extends RuntimeException{
    public InvalidMeetingTypeException (){
        super("Invalid Meeting Type.");
    }
}
