package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetJiraTaskIdAndTitleUsingTokenRequest {
    private GetJiraUsersDetailsRequest getJiraUsersRequest;
    private List<JiraIssueTypeMapping> jiraIssueTypeMappingList;
}
