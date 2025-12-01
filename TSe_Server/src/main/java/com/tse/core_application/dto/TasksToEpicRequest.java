package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class TasksToEpicRequest {
    @NotNull(message = ErrorConstant.Epic.EPIC_ID)
    private Long epicId;
    @NotNull(message = ErrorConstant.Epic.TASK_LIST)
    private List<Long> taskIds;
}
