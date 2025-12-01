package com.tse.core_application.dto;

import lombok.Value;

@Value
public class UserListResponse {
    String email;
    Long accountId;
    String firstName;
    String middleName;
    String lastName;
    Boolean isActive;
    Boolean isDisabledBySams;
    Integer deactivatedByRole;
    Long deactivatedByAccountId;

    public UserListResponse(Object email, Long accountId, Object firstName, Object middleName, Object lastName, Boolean isActive, Boolean isDisabledBySams, Integer deactivatedByRole, Long deactivatedByAccountId) {
        this.email = (String) email;
        this.accountId = accountId;
        this.firstName = (String) firstName;
        this.middleName = (String) middleName;
        this.lastName = (String) lastName;
        this.isActive = isActive;
        this.isDisabledBySams = isDisabledBySams;
        this.deactivatedByRole = deactivatedByRole;
        this.deactivatedByAccountId = deactivatedByAccountId;
    }
}
