package com.tse.core_application.exception;

public class NumberFormatException extends RuntimeException{
    public NumberFormatException(String message) {
        super("Input Number Format is Invalid please check : " + message);
    }
}
