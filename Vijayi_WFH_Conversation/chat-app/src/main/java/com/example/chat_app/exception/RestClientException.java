package com.example.chat_app.exception;

public class RestClientException extends  RuntimeException{
    public RestClientException(String message) {
        super(message);
    }
}
