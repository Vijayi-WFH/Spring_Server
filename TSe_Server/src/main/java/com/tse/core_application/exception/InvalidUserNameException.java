package com.tse.core_application.exception;

public class InvalidUserNameException extends RuntimeException{

    public InvalidUserNameException() {
        super("Validation Failed: Wrong UserName");
    }
}
