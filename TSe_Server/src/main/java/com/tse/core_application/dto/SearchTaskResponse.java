package com.tse.core_application.dto;

import com.tse.core_application.model.StatType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchTaskResponse {
    private Long taskId;
    private String taskNumber;
    private Long taskIdentifier;
    private String taskPriority;
    private String taskTitle;
    private String taskDesc;
    private Integer taskTypeId;
    private Integer taskEstimate;
    private Integer taskWorkflowId;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private String workflowTaskStatus;
    private Integer currentActivityIndicator;
    private Boolean currentlyScheduledTaskIndicator;
    private String teamName;
    private String fullName; // account assigned
    private String email; // account assigned
    private StatType taskProgressSystem;
    private Long sprintId;
    private Long teamId;
    private Boolean isPersonalTask = false;
    private Boolean isBug = false;
    private Integer userPerceivedPercentageTaskCompleted;
    private Boolean isStarred;
    private EmailFirstLastAccountIdIsActive starredBy;
}
