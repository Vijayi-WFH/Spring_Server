package com.tse.core_application.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OpenTaskDetails {
    private Long taskId;
    private String taskNumber;
    private Long taskIdentifier;
    private String taskTitle;
    private String taskPriority;
    private Long accountIdAssigned;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskExpEndDate;
    private String workflowStatus;
    private Integer taskWorkflowId;
    private Integer taskTypeId;
    private Long parentTaskId;
    private String parentTaskTitle;
    private Long parentTaskIdentifier;
    private String parentTaskNumber;
    private Long teamId;
    private String teamName;
    private Long orgId;
    private Boolean isBug;
}
