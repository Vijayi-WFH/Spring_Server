package com.example.chat_app.exception;

public class FileNameException extends RuntimeException{

    public FileNameException() {
        super("Illegal File Name");
    }

    public FileNameException(String fileName, String optionIndicator) {
        super("Illegal Combination of fileName = " + fileName + " and optionIndicator = " + optionIndicator);
    }
}