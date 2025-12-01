package com.tse.core_application.exception;

public class OrganizationDoesNotExistException extends RuntimeException {

    public OrganizationDoesNotExistException() {
        super("Organization Does Not Exist");
    }
}
