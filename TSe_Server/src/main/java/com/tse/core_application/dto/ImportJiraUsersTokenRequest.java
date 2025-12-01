package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ImportJiraUsersTokenRequest {
    @Valid
    @NotNull
    private GetJiraUsersDetailsRequest getJiraUsersDetailsRequest;

    private Long teamId;
}
