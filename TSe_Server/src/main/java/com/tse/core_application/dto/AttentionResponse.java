package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttentionResponse {
    private String taskNumber;
    private Long taskIdentifier;
    private Long teamId;
    private String taskTitle;
    private String taskDesc;
    private LocalDateTime taskActStDate;
    private LocalDateTime taskActEndDate;
    private String workflowTaskStatus;
    private String taskPriority;
    private String taskAssignedTo;
}
