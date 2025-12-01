package com.tse.core_application.exception;

public class OpenfireException extends RuntimeException{
    public OpenfireException(String message) {

        super("Error:" + message);
    }
}
