package com.tse.core_application.exception;

public class NoTaskMediaFoundException extends RuntimeException{

    public NoTaskMediaFoundException() {
        super("Task Media Not Found");
    }
}
