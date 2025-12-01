package com.tse.core_application.exception;

public class TaskViewException extends RuntimeException{

    public TaskViewException() {
        super("Forbidden: You do not have the authorisation to view this work item");
    }
}
