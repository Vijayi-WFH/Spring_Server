package com.tse.core_application.exception;

public class IncorrectRequestHeaderException extends RuntimeException{

    public IncorrectRequestHeaderException() {
        super("Incorrect Request Header");
    }
}
