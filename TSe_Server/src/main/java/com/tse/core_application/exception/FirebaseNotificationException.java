package com.tse.core_application.exception;

public class FirebaseNotificationException extends RuntimeException{

    public FirebaseNotificationException(String errorMessage){
        super("FirebaseException: " + errorMessage);
    }
}
