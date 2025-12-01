package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProgressSystemSprintTask {
    private Long taskId;
    private String taskTitle;
    private Integer effort;
    private Integer percentageCompleted;
    private Integer estimate;
    private String expEndDate;
    private String accountIdAssigned;
    private Boolean isBug = false;
    private Integer taskTypeId;
}
