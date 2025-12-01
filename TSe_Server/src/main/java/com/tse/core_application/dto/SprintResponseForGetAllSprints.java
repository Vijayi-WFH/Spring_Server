package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SprintResponseForGetAllSprints {
    private SprintWithoutSprintMembers sprint;
    private Integer notStartedTasks = 0;
    private Integer watchListTasks = 0;
    private Integer onTrackTasks = 0;
    private Integer completedTasks = 0;
    private Integer delayedTasks = 0;
    private Integer totalTasks = 0;
    private Integer deletedTasks = 0;
    private Integer lateCompletedTasks = 0;

    private List<ProgressSystemSprintTask> notStartedTasksList = new ArrayList<>();
    private List<ProgressSystemSprintTask> watchListTasksList = new ArrayList<>();
    private List<ProgressSystemSprintTask> onTrackTasksList = new ArrayList<>();
    private List<ProgressSystemSprintTask> completedTasksList = new ArrayList<>();
    private List<ProgressSystemSprintTask> delayedTasksList = new ArrayList<>();
    private List<ProgressSystemSprintTask> deletedTasksList = new ArrayList<>();
    private List<ProgressSystemSprintTask> lateCompletedTasksList = new ArrayList<>();


    public void incrementDelayedTasks() {
        this.delayedTasks++;
    }

    public void incrementOnTrackTasks() {
        this.onTrackTasks++;
    }

    public void incrementCompletedTasks() {
        this.completedTasks++;
    }

    public void incrementNotStartedTasks() {
        this.notStartedTasks++;
    }

    public void incrementWatchListTasks() {
        this.watchListTasks++;
    }
    public void incrementTotalTasks() {
        this.totalTasks++;
    }
    public void incrementDeletedTasks() {
        this.deletedTasks++;
    }
    public void incrementLateCompletedTask() {
        this.lateCompletedTasks++;
    }
}
