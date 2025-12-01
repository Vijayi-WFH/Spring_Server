package com.tse.core_application.exception;

public class StickyNoteFailedException extends RuntimeException{

    public StickyNoteFailedException(String message) {
        super("Sticky Note Share Failed : " + " " + message);
    }
}
