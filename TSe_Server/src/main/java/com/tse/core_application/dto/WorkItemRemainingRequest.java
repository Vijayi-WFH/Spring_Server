package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkItemRemainingRequest {
    @NotNull(message = "Sprint detail is missing")
    private Long sprintId;

    private Long accountId;
}
