package com.tse.core_application.dto;

import com.tse.core_application.custom.model.SprintTitleAndId;
import com.tse.core_application.model.Sprint;
import com.tse.core_application.model.Task;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SprintAllTasksResponse {
    private Sprint sprint;
    private SprintTitleAndId nextSprint;
    private SprintTitleAndId previousSprint;
    private List<SprintTaskResponse> sprintStartedTaskList;
    private List<SprintTaskResponse> sprintNotStartedTaskList;
    private List<SprintTaskResponse> sprintCompletedTaskList;
    private Integer totalStartedTask = 0;
    private Integer totalNotStartedTask = 0;
    private Integer totalCompletedTask = 0;
    private List<ProgressSystemSprintTask> toDoWorkItemList;
    private List<ProgressSystemSprintTask> inProgressWorkItemList;
    private List<ProgressSystemSprintTask> completedWorkItemList;
}
