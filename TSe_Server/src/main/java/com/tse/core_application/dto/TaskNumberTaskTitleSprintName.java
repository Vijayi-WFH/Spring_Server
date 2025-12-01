package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class
TaskNumberTaskTitleSprintName {
    private String taskNumber;
    private Long taskId;
    private Long teamId;
    private String taskTitle;
    private String sprintTitle;
    private String message;
    private Integer taskTypeId;
}
