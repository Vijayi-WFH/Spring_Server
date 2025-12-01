package com.tse.core_application.exception;

public class NoDataFoundException extends RuntimeException {

    public NoDataFoundException() {
        super("No Data Found");
    }
}
