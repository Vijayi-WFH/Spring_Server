package com.tse.core_application.dto.org_response;

import lombok.Data;

@Data
public class UserDetail {
    private Long accountId;
    private String firstName;
    private String lastName;
    private String email;
}
