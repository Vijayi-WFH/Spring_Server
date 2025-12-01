package com.tse.core_application.exception;

public class TaskNotFoundException extends RuntimeException{

    public TaskNotFoundException() {
        super("Work Item not found");
    }
}
