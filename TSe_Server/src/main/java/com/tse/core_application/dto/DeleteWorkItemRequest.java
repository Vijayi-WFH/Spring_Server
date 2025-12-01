package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteWorkItemRequest {
    private Integer deleteReasonId;
    private String duplicateWorkItemNumber;
    private String deleteReason;
    private Boolean removeFromSprint;
}
