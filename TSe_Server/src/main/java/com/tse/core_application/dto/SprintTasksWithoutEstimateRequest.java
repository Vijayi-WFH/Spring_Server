package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class SprintTasksWithoutEstimateRequest {
    private Long accountId;
    @NotNull(message = ErrorConstant.Sprint.SPRINT)
    private Long sprintId;
}
