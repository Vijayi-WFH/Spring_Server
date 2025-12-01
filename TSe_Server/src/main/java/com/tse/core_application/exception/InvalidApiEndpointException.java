package com.tse.core_application.exception;

public class InvalidApiEndpointException extends RuntimeException{

    public InvalidApiEndpointException() {
        super("Invalid API Endpoint");
    }
}
