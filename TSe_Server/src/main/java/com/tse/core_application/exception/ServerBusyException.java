package com.tse.core_application.exception;

public class ServerBusyException extends RuntimeException {

    public ServerBusyException() {
        super("Server Busy");
    }
}
