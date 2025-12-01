package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskIdAssignedTo {
    private Long taskId;
    private Long accountIdAssignedTo;
    private Integer actionId;
    private List<AttendeeRequest> attendeeRequests;
}
