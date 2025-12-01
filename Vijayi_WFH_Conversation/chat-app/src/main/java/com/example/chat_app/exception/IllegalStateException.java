package com.example.chat_app.exception;

public class IllegalStateException extends RuntimeException{
    public IllegalStateException(String errorMessage){
        super(errorMessage);
    }
}
