package com.tse.core_application.exception;

public class UserAlreadyExistException extends RuntimeException {

    public UserAlreadyExistException() {
        super("User Already Exist");
    }

}
