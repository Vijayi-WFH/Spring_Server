package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class ImportJiraUsersManualRequest {
    @NotNull(message = ErrorConstant.TEAM_ID_ERROR)
    private Long teamId;

    @NotEmpty(message = ErrorConstant.Jira.JIRA_USER_LIST)
    private List<JiraUsers> jiraUsers;
}
