package com.tse.core_application.exception;

public class DuplicateFileException extends RuntimeException{

    public DuplicateFileException() {
        super("Duplicate File exists.");
    }
}
