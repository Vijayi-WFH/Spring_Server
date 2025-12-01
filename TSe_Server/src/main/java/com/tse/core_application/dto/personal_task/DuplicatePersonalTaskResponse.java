package com.tse.core_application.dto.personal_task;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DuplicatePersonalTaskResponse {

    private String taskTitle;

    private String taskDesc;

    private Integer taskTypeId;

    private String taskPriority;

    private Integer taskEstimate;

    private Integer taskWorkflowId;

    private String workflowTaskStatus;

    private LocalDateTime taskExpStartDate;

    private LocalDateTime taskExpEndDate;

    private String keyDecisions;

}
