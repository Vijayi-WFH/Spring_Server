package com.tse.core_application.dto;

import lombok.Data;

@Data
public class DeleteTeamResponse {
    private String message;
    private TaskListForBulkResponse taskListForBulkResponse;
}
