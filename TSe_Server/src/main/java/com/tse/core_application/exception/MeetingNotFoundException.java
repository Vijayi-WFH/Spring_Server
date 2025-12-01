package com.tse.core_application.exception;

public class MeetingNotFoundException extends RuntimeException{

    public MeetingNotFoundException(){super("No Meeting Found for this MeetingId");}

}
