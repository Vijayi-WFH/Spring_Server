package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Node {
    private Long taskId;
    private String taskNumber;
    private Long teamId;
    private String taskTitle;
    private String taskDesc;
    private Integer taskEstimate;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private com.tse.core_application.model.StatType taskProgressSystem;
    private Integer taskTypeId;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private String workflowStatus;
    private Long accountIdAssigned;
    private Integer userPerceivedPercentageTaskCompleted;
    private Integer recordedEffort;
    private Long sprintId;
}
