package com.tse.core_application.custom.model;

import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.model.StatType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class TaskMaster {

    private String taskNumber;
    private Long taskId;
    private Long taskIdentifier;
    private String taskTitle;
    private String taskDesc;
    private Integer taskTypeId;
    private String taskType;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private LocalDate newEffortDate;
    private String taskWorkflowType;
    private String workflowTaskStatusType;
    private Integer currentActivityIndicator;
    private Boolean currentlyScheduledTaskIndicator;
    private StatType taskProgressSystem;
    private String taskPriority;
    private Long teamId;
    private Long orgId;
    private String teamName;
    private String teamCode;
    private String fullName;
    private String email;
    private EmailFirstLastAccountId createdBy;
    private EmailFirstLastAccountId mentor1;
    private EmailFirstLastAccountId mentor2;
    private EmailFirstLastAccountId observer1;
    private EmailFirstLastAccountId observer2;
    private LocalDateTime createdDateTime;  // added in task 3698
    private LocalDateTime lastUpdatedDateTime;  // added in task 3698
    private Boolean isPersonalTask = false;
    private Boolean isBug = false;
    private Boolean isStarred;
    private EmailFirstLastAccountIdIsActive starredBy;
    private EmailFirstLastAccountIdIsActive blockedBy;
    private Integer blockedReasonTypeId;
    private EmailFirstLastAccountIdIsActive lastUpdatedBy;
}
