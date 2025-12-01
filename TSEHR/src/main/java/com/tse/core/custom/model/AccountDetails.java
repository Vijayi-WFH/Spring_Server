package com.tse.core.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountDetails {
    private Long accountId;
    private String email;
    private String firstName;
    private String lastName;
}
