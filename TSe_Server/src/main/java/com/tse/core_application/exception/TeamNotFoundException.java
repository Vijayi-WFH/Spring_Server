package com.tse.core_application.exception;

public class TeamNotFoundException extends RuntimeException{

    public TeamNotFoundException() {
        super("Team Not Found");
    }
}
