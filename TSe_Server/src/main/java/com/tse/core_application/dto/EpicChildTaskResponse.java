package com.tse.core_application.dto;

import com.tse.core_application.model.StatType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EpicChildTaskResponse {
    private String taskNumber;
    private Long teamId;
    private Long taskId;
    private String taskTitle;
    private String taskDesc;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private StatType taskProgressSystem;
    private String taskPriority;
    private Integer taskTypeId;
    private Long assignedTo;
    private String workflowTaskStatus;
    private Integer taskEstimate;
    private Boolean addedInEpicAfterCompletion;
    private String message;

}
