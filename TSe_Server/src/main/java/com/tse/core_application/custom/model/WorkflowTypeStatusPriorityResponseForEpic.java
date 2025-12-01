package com.tse.core_application.custom.model;

import com.tse.core_application.model.WorkFlowEpicStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkflowTypeStatusPriorityResponseForEpic {

    private List<PriorityIdDescDisplayAs> priority;
    private List<WorkFlowEpicStatus> workflowEpicStatusList;
}
