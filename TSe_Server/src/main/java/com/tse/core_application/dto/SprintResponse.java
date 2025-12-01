package com.tse.core_application.dto;

import com.tse.core_application.model.Sprint;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SprintResponse {
    private Sprint sprint;
    private Integer notStartedTasks = 0;
    private Integer watchListTasks = 0;
    private Integer onTrackTasks = 0;
    private Integer completedTasks = 0;
    private Integer delayedTasks = 0;
    private Integer totalTasks = 0;
    private Integer deletedTasks = 0;

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
}
