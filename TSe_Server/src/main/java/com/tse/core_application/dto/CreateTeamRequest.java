package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateTeamRequest {

    @Size(min = 3, max = 50, message = ErrorConstant.Team.TEAM_NAME)
    @NotNull(message = ErrorConstant.Team.TEAM_NAME_NOT_NULL)
    private String teamName;

    @Size(min = 3, max = 1000, message = ErrorConstant.Team.TEAM_DESC)
    @NotNull(message = ErrorConstant.Team.TEAM_DESC_NOT_NULL)
    private String teamDesc;

    private Long parentTeamId;

    @NotNull(message = ErrorConstant.Team.TEAM_CODE)
    private String teamCode;

    @NotNull(message = ErrorConstant.Team.PROJECT)
    private Long projectId;

    @NotNull(message = ErrorConstant.Team.ORG)
    private Long orgId;

    @NotNull(message = ErrorConstant.Team.OWNER_ACCOUNT)
    private Long ownerAccountId;

    @NotNull(message = ErrorConstant.Team.TEAM_ADMIN)
    private Long teamAdmin;
}
