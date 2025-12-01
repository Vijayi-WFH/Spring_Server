package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class UserName {

    String firstName;
    String lastName;

    public UserName(Object firstName, Object lastName) {
        this.firstName = (String) firstName;
        this.lastName = (String) lastName;
    }
}
