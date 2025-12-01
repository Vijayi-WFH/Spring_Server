package com.example.chat_app.exception;

public class ValidationFailedException extends RuntimeException{

    public ValidationFailedException(String validationMessage) {
        super(validationMessage);
    }
}