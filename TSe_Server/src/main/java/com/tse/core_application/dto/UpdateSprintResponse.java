package com.tse.core_application.dto;

import com.tse.core_application.model.Sprint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UpdateSprintResponse {
    private Sprint sprint;
    private List<TaskNumberTaskTitleSprintName> taskToMoveList;
    private List<TaskNumberTaskTitleSprintName> updatedTasks;
}
