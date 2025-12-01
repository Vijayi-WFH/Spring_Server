package com.example.chat_app.exception;

public class FileNotFoundException extends RuntimeException{

    public FileNotFoundException(String fileName) {
        super("The file " + fileName + " is not found or deleted.");
    }
}