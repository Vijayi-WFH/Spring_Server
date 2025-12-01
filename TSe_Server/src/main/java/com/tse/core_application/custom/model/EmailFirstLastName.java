package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class EmailFirstLastName {

    String email;
    String firstName;
    String lastName;

    public EmailFirstLastName(Object email, Object firstName, Object lastName) {
        this.email = (String) email;
        this.firstName = (String) firstName;
        this.lastName = (String) lastName;
    }
}
