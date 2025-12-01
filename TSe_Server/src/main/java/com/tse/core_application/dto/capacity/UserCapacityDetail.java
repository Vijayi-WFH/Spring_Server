package com.tse.core_application.dto.capacity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserCapacityDetail {
    private Long accountId;
    private String accountName;
    private String email;
    private Double totalCapacity;
    private Integer currentPlannedCapacity;
    private Integer loadedCapacity;
    private Integer percentCapacityUtilization;
    private Integer percentLoadedCapacityUtilization;
    private Double loadedCapacityRatio;
    private Double totalWorkingDays;
    private Integer workMinutes;
    private Integer burnedEfforts;
    private Integer totalEarnedEfforts;
    private Integer earnedEffortsTask;
    private Integer earnedEffortsMeeting;
    private Integer minutesBehindSchedule;
    private Boolean isPresent = true;
}
