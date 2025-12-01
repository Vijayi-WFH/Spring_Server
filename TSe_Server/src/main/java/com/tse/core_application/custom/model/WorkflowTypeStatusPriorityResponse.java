package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkflowTypeStatusPriorityResponse {

    private List<PriorityIdDescDisplayAs> priority;
    private List<WorkflowTypeIdDesc> workflowType;
    private List<WorkflowTaskStatusIdTypeState> workflowTaskStatus;
}
