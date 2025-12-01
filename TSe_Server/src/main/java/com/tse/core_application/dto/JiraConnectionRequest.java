package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class JiraConnectionRequest {
    @NotBlank(message = ErrorConstant.Jira.SITE_URL_REQUIRED)
    private String siteUrl;

    @NotBlank(message = ErrorConstant.Jira.EMAIL_REQUIRED)
    private String jiraEmail;

    @NotBlank(message = ErrorConstant.Jira.TOKEN_REQUIRED)
    private String jiraToken;
}
