package com.tse.core_application.dto.capacity;

import com.tse.core_application.dto.SprintDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SprintCapacityDetails {
    private SprintDetails sprintDetails;
    private Integer totalCapacity; // in minutes
    private Integer currentPlannedCapacity; // in minutes
    private Integer loadedCapacity; // in minutes
    private Integer percentPlannedCapacityUtilization;
    private Integer percentLoadedCapacityUtilization;
    private Integer totalWorkingDays;
    private Integer burnedEfforts;
    private Integer totalEarnedEfforts;
    private Integer earnedEffortsTask;
    private Integer earnedEffortsMeeting;
    private Integer minutesBehindSchedule;
    private Boolean capacityMismatchIndicator;
    private Integer totalUnassignedTasksWithNoEstimate = 0;
    private List<UserCapacityDetail> userCapacities;
}
