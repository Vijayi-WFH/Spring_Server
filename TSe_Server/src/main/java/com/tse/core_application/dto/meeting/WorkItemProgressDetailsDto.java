package com.tse.core_application.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WorkItemProgressDetailsDto {

    private Long taskId;
    private String taskNumber;
    private Integer taskTypeId;
    private Long assignedAccountId;
    private String assigneeEmail;
    private Integer percentageCompleted;
    private String statType;
    private String workflowTaskStatus;

    public WorkItemProgressDetailsDto(Long taskId, String taskNumber, Integer taskTypeId, Object assignedAccountId, Object assigneeEmail, Integer percentageCompleted, com.tse.core_application.model.StatType statType, Object workflowTaskStatus) {
        this.taskId = taskId;
        this.taskNumber = taskNumber;
        this.taskTypeId = taskTypeId;
        this.assignedAccountId = (Long) assignedAccountId;
        this.assigneeEmail = (String) assigneeEmail;
        this.percentageCompleted = percentageCompleted;
        this.statType = statType != null ? statType.toString() : null;
        this.workflowTaskStatus = (String) workflowTaskStatus;
    }
}
