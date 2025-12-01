package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.RelationType;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ValidateTaskRequest{
    private Long linkFrom;
    private String linkTo;
    private RelationType relationType;
    private Boolean isForCreation = false;
    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    private Long teamId;
}
