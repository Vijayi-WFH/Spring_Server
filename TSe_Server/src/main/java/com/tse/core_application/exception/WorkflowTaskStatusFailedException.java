package com.tse.core_application.exception;

public class WorkflowTaskStatusFailedException extends RuntimeException{

    public WorkflowTaskStatusFailedException(String oldWorkflowTaskStatus, String newWorkflowTaskStatus) {
        super("WorkflowTaskStatus Failed: Cannot update " + newWorkflowTaskStatus + " from " + oldWorkflowTaskStatus);
    }
}
