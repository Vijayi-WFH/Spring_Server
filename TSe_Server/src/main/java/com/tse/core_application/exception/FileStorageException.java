package com.tse.core_application.exception;

public class FileStorageException extends RuntimeException{

    public FileStorageException() {
        super("Could not save file");
    }

}
