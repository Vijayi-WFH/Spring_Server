package com.tse.core.exception;

public class ValidationFailedException extends RuntimeException{

    public ValidationFailedException(String validationMessage) {
        super("Validation Failed: " + validationMessage);
    }
}
