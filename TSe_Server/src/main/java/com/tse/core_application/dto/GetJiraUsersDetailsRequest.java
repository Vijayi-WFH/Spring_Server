package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class GetJiraUsersDetailsRequest {
    @Valid
    @NotNull
    private JiraConnectionRequest connection;

    @NotBlank(message = ErrorConstant.Jira.PROJECT_ID_REQUIRED)
    private String projectId;
}
