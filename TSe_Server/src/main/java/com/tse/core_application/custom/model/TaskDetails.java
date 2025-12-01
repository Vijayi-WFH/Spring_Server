package com.tse.core_application.custom.model;

import lombok.Data;
import lombok.Value;

@Data
public class TaskDetails {
    String taskNumber;
    Long taskId;
    String taskTitle;
    String taskDesc;

    Long teamId;

    public TaskDetails(Object taskNumber, Long taskId, Object taskTitle, Object taskDesc, Long teamId) {
        this.taskNumber = (String) taskNumber;
        this.taskId = taskId;
        this.taskTitle = (String) taskTitle;
        this.taskDesc = (String) taskDesc;
        this.teamId = teamId;
    }
}
