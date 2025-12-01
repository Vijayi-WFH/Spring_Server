package com.tse.core_application.dto.personal_task;

import com.tse.core_application.model.personal_task.PersonalNote;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PersonalTaskResponse {

    private Long personalTaskId;

    private String personalTaskNumber;

    private Long personalTaskIdentifier;

    private String taskTitle;

    private String taskDesc;

    private Integer taskTypeId;

    private String taskPriority;

    private Integer taskEstimate;

    private String workflowStatus;

    private Integer taskWorkflowId;

    private LocalDateTime taskExpStartDate;

    private LocalDateTime taskExpEndDate;

    private String keyDecisions;

    private String parkingLot;

    private Long accountId;

    private String taskState;

    private com.tse.core_application.model.StatType taskProgressSystem;

    private LocalDateTime createdDateTime;

    private String fullName;

    private String email;

    // fields specific to update personal task

    private Integer recordedEffort;

    private Integer userPerceivedPercentageTaskCompleted;

    private LocalDateTime taskActStDate;

    private LocalDateTime taskActEndDate;

    private Boolean currentActivityIndicator;

    private Boolean currentlyScheduledTaskIndicator;

    private Integer earnedTimeTask;

    private String attachments;

    private Long version;

    private LocalDateTime lastUpdatedDateTime;

    private List<PersonalNote> notes;
}
