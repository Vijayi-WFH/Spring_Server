package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsForBulkResponse {
    private String email;
    private Long accountId;
    private String firstName;
    private String lastName;
    private String message;
}
