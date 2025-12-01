package com.tse.core_application.custom.model;

import lombok.*;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmailFirstLastAccountId {

    String email;
    Long accountId;
    String firstName;
    String lastName;


//    public EmailFirstLastAccountId(String email, Long accountId, Object firstName, Object lastName) {
//        this.email = email;
//        this.accountId = accountId;
//        this.firstName = (String) firstName;
//        this.lastName = (String) lastName;
//    }

    public EmailFirstLastAccountId(Object email, Long accountId, Object firstName, Object lastName) {
        this.email = (String) email;
        this.accountId = accountId;
        this.firstName = (String) firstName;
        this.lastName = (String) lastName;
    }

    @Override
    public boolean equals(Object obj) {
        // Check if the object is the same reference
        if (this == obj) return true;

        // Check if the object is null or not the same class
        if (obj == null || getClass() != obj.getClass()) return false;

        // Cast the object to EmailFirstLastAccountId for comparison
        EmailFirstLastAccountId that = (EmailFirstLastAccountId) obj;

        // Compare fields that define equality
        return Objects.equals(email, that.email) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        // Use the fields to generate a consistent hash code
        return Objects.hash(email, accountId, firstName, lastName);
    }
}
