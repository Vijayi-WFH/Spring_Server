package com.tse.core_application.dto;

import com.tse.core_application.model.WorkFlowEpicStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EpicDetailsResponse {
    private Long epicId;
    private String epicNumber;
    private String epicTitle;
    private WorkFlowEpicStatus fkworkFlowEpicStatus;
    private EmailFirstLastAccountIdIsActive emailFirstLastAccountIdIsActive;
    private String priority;
    private LocalDateTime expStartDateTime;
    private LocalDateTime actStartDateTime;
    private LocalDateTime expEndDateTime;
    private LocalDateTime actEndDateTime;
    private LocalDateTime dueDateTime;
    private Integer loggedEffort;
    private Integer EarnedEffort;
    private Integer estimate;
    private Integer originalEstimate;
    private Integer runningEstimate;
    private Integer numberOfBacklogTask;
    private Integer numberOfTotalNotStartedTask;
    private Integer numberOfNotStartedTask;
    private Integer numberOfNotStartedBlockedTask;
    private Integer numberOfTotalStartedTask;
    private Integer numberOfStartedTask;
    private Integer numberOfStartedOnHoldTask;
    private Integer numberOfStartedBlockedTask;
    private Integer numberOfCompletedTask;
    private Integer numberOfDeletedTask;
    private Integer numberOfTask;
    private String entityName;
    private String color;
}
