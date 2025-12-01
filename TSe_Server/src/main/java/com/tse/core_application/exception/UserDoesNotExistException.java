package com.tse.core_application.exception;

public class UserDoesNotExistException extends RuntimeException {

    public UserDoesNotExistException() {
        super("User Does Not Exist");
    }

}
