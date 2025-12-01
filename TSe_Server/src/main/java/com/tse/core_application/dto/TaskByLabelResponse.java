package com.tse.core_application.dto;

import com.tse.core_application.model.StatType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskByLabelResponse {
    private String taskNumber;
    private Long taskIdentifier;
    private String taskTitle;
    private String taskDesc;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private Integer currentActivityIndicator;
    private Boolean currentlyScheduledTaskIndicator;
    private StatType taskProgressSystem;
    private String taskPriority;

    private String teamName;
    private String fullName;
    private String email;
    private String workflowTaskStatus;

    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private Integer taskTypeId;
    private Boolean isBug;
}
