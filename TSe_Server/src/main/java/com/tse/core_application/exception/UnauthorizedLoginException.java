package com.tse.core_application.exception;

public class UnauthorizedLoginException extends RuntimeException {

    public UnauthorizedLoginException() {
        super("Otp is invalid or expired");
    }
}
