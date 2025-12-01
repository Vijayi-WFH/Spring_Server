package com.tse.core_application.exception;

public class AuthenticationFailedException extends RuntimeException{

    public AuthenticationFailedException() {
        super("Authentication Failed for AccountIds. Please, login again.");
    }
}
