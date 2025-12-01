package com.tse.core_application.dto;

import lombok.Data;

@Data
public class OpenTaskAssignedToUserRequest {
    private Long accountIdAssigned;
    private Integer entityTypeId;
    private Long entityId;
}
