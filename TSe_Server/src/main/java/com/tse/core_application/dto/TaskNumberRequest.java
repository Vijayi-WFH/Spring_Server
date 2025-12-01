package com.tse.core_application.dto;

import lombok.Data;

@Data
public class TaskNumberRequest {

    private String taskNumber; // if taskNumber is provided, either teamId or orgId could be provided

    private Long taskIdentifier; // either taskIdentifier or taskNumber could be provided. If providing taskIdentifier, need to provide teamId

    private Long teamId; // if taskNumber is provided, either teamId, orgId, projectId could be provided.
    // If taskIdentifier is provided, team Id is mandatory

    private Long orgId;

    private Long projectId;
}
