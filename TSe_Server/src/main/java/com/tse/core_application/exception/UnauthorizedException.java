package com.tse.core_application.exception;

public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException(String message) {
        super(message != null ? message : "Unauthorized attempt");
    }

}
