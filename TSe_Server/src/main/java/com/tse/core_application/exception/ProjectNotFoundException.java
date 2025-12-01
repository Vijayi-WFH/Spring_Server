package com.tse.core_application.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException() {
        super("No Project Found");
    }
}
