package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class ScheduledTaskViewResponse  {



    private Long accountIdAssigned;

    private String taskNumber;

    private Long taskIdentifier;

    private Long taskId;

    private Long teamId;

    private Integer taskTypeId;

    private String taskTitle;

    private String taskDesc;

    private Boolean currentlyScheduledTaskIndicator;

    private Integer currentActivityIndicator;

    private Integer taskWorkflowId;

    private String workflowTaskStatus;

    private Boolean isPersonalTask = false;

}
