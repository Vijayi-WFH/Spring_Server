package com.tse.core_application.exception;

public class CommentNotFoundException extends RuntimeException{

    public CommentNotFoundException() {
        super("No Comments Found");
    }
}
