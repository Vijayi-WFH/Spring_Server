package com.example.chat_app.exception;

public class UnauthorizedLoginException extends RuntimeException {
    public UnauthorizedLoginException(String s) {
        super(s);
    }
}
