package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class AddTasksToSprintRequest {
    @NotNull(message = ErrorConstant.Sprint.SPRINT)
    private Long sprintId;
    @NotNull(message = ErrorConstant.Sprint.TASK_LIST)
    private List<Long> taskIds;
}
