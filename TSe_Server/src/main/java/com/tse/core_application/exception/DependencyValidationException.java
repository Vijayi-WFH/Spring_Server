package com.tse.core_application.exception;

public class DependencyValidationException extends RuntimeException{

    public DependencyValidationException(String errorMessage){
        super(errorMessage);
    }
}
