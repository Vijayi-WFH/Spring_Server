package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MappedJiraUser {
    private String jiraUserId;
    private String jiraUserName;
    private Long accountId;
}
