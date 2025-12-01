package com.tse.core_application.exception;

public class InvalidRequestHeaderException extends RuntimeException {

    public InvalidRequestHeaderException() {
        super("Incorrect Request Header");
    }
}
