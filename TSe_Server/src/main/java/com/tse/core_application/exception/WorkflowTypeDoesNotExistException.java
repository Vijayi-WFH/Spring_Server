package com.tse.core_application.exception;

public class WorkflowTypeDoesNotExistException extends RuntimeException{

    public WorkflowTypeDoesNotExistException() {
        super("Invalid Data: Workflow Type Does Not Exist");
    }
}
