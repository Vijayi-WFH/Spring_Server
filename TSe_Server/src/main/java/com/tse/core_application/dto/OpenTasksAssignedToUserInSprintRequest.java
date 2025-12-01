package com.tse.core_application.dto;

import lombok.Data;

@Data
public class OpenTasksAssignedToUserInSprintRequest {
    Long sprintId;
    Long accountIdAssigned;
}
