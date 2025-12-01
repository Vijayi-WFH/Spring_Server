package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskForBulkResponse {
    private Long taskId;
    private String taskNumber;
    private String taskTitle;
    private Long teamId;
    private String message;
}
