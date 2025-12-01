package com.tse.core_application.dto;

import com.tse.core_application.model.StatType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SprintTaskResponse {
    private String taskNumber;
    private Long teamId;
    private Long taskId;
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
    private Integer taskTypeId;
    private String teamName;
    private String assignedTo;
    private String workflowTaskStatus;
    private List<SprintTaskResponse> childTaskList;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private Boolean partOfSprint = true;
    private String sprintTitle;
    private Integer taskEstimate;
    private Integer userPerceivedPercentageTaskCompleted;
    private Boolean isBug = false;
    private String sprintMovementTag;
    private Boolean showCard = true;
    private Boolean isStarred;
    private EmailFirstLastAccountIdIsActive starredBy;
}
