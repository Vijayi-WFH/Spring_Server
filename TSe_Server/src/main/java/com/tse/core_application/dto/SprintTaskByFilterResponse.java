package com.tse.core_application.dto;

import com.tse.core_application.dto.capacity.UserCapacityDetail;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SprintTaskByFilterResponse {
    private UserCapacityDetail userCapacityDetails;
    private List<SprintTaskResponse> sprintStartedTaskList;
    private List<SprintTaskResponse> sprintNotStartedTaskList;
    private List<SprintTaskResponse> sprintCompletedTaskList;
    private Integer totalStartedTask = 0;
    private Integer totalNotStartedTask = 0;
    private Integer totalCompletedTask = 0;

    public void incrementTotalStartedTask () {
        this.totalStartedTask++;
    }

    public void incrementTotalNotStartedTask () {
        this.totalNotStartedTask++;
    }

    public void incrementTotalCompeletedTask () {
        this.totalCompletedTask++;
    }
}
