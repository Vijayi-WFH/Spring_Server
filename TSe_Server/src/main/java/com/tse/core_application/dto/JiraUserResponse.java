package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JiraUserResponse {
    private String accountId;
    private String displayName;
    private String emailAddress;
    private String avatarUrl;
    private Boolean status;
}
