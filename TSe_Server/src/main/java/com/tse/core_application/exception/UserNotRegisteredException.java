package com.tse.core_application.exception;

public class UserNotRegisteredException extends RuntimeException{
    public UserNotRegisteredException(){super("User not registered. Please register and then try to login.");}
}
