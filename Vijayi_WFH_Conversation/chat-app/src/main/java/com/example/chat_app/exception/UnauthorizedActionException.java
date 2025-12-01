package com.example.chat_app.exception;

public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String s) {
        super("Unauthorized action: " + s);
    }
}
