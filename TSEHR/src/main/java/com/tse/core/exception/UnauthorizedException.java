package com.tse.core.exception;

public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException() {
        super("Unauthorized attempt");
    }
}
