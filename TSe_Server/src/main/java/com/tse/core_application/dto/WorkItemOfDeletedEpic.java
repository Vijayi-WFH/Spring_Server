package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WorkItemOfDeletedEpic {
    List<ProgressSystemSprintTask> baclogWorkItemList;
    List<ProgressSystemSprintTask> notStartedWorkItemList;
    List<ProgressSystemSprintTask> startedWorkItemList;
    List<ProgressSystemSprintTask> completedWorkItemList;
}
