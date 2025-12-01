package com.tse.core_application.exception;

public class ValidationFailedException extends RuntimeException{

    public ValidationFailedException(String validationMessage) {
        super("Validation Failed: " + validationMessage);
    }
}
