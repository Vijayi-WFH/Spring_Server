package com.tse.core_application.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BurnDownRequest {

    @NotNull(message = "Sprint detail is missing")
    private Long sprintId;

//    @NotNull(message = "Organization detail is missing")
//    private Long orgId;
//
//    @NotNull(message = "Team detail is missing")
//    private Long teamId;

    private Long accountId;
}
