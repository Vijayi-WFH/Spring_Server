package com.tse.core_application.exception;

public class InternalServerErrorException  extends RuntimeException {

    public InternalServerErrorException(String message) {
        super("An unexpected error occurred! It seems like there is a problem at our end. " +
                "Please contact system administrator at support@vijayi-wfh.com");
    }
}
