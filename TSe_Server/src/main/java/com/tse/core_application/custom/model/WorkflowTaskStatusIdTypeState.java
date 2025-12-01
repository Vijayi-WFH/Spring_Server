package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
public class WorkflowTaskStatusIdTypeState {

    Integer workflowTaskStatusId;
    String workflowTaskStatus;
    Integer workflowTypeId;
    String workflowTaskState;

}
