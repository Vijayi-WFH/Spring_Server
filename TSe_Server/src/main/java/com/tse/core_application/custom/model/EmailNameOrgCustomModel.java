package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class EmailNameOrgCustomModel {

    // This needs to be Object because in the database these values are encrypted and when they are converted, these are returned as object instead of String.
    // For all the values that are encrypted we need to use Object as the type
    Object email;
    Object org;
    Long orgId;
    Object firstName;
    Object lastName;
    Long accountId;
    Long teamId;
    Boolean isActive; // in team
}
