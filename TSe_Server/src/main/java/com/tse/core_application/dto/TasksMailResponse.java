package com.tse.core_application.dto;

import lombok.Data;

import java.time.LocalDateTime;

//This is temporary fox for 8730
@Data
public class TasksMailResponse {
    private String SprintTitle;
    private String taskNumber;
    private String taskType;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private LocalDateTime sprintExpStartDate;
    private LocalDateTime sprintExpEndDate;
    private LocalDateTime parentTaskExpStartDate;
    private LocalDateTime parentTaskExpEndDate;
}
