package com.tse.core_application.dto;

import com.tse.core_application.model.UserAccount;
import lombok.Data;

@Data
public class WorkItem {
    private Long taskId;
    private String taskNumber;
    private String taskTitle;
    private EmailFirstLastAccountIdIsActive assignedTo;
    private Integer taskEstimate;
    private String workflowStatus;
    private Integer userPerceivedPercentageTaskCompleted;
}
