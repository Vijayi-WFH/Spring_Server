package com.tse.core_application.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmailFirstLastAccountIdIsActive {
    private String email;
    private Long accountId;
    private String firstName;
    private String lastName;
    private Boolean isActive;

    public EmailFirstLastAccountIdIsActive(Object email, Long accountId, Object firstName, Object lastName, Boolean isActive) {
        this.email = (String) email;
        this.accountId = accountId;
        this.firstName = (String) firstName;
        this.lastName = (String) lastName;
        this.isActive = isActive;
    }
}
