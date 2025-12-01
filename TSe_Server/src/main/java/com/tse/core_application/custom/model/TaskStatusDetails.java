package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskStatusDetails {

    private Long taskId;
    private String taskNumber;
    private String taskTitle;
    private String taskDesc;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private String taskState;
    private String firstName;
    private String lastName;
    private String email;

}
