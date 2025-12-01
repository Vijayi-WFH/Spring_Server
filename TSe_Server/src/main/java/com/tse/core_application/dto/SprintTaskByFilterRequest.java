package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class SprintTaskByFilterRequest {
    @NotNull(message = ErrorConstant.Sprint.SPRINT)
    private Long sprintId;

    private Long accountId;

    private String taskPriority;

    private Integer taskTypeId;

    private List<Long> labelIds;

    private Boolean isStarred;

    private List<Long>starredBy;
}
