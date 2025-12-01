package com.tse.core_application.dto;

import com.tse.core_application.model.StatType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EpicTaskResponse {
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
    private Long assignedTo;
    private String workflowTaskStatus;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private Integer taskEstimate;
    private Boolean isBug = false;
    private Integer userPerceivedPercentageTaskCompleted;
    List<EpicChildTaskResponse> childTaskResponses;
    private Boolean addedInEpicAfterCompletion;
    private String message;
}
