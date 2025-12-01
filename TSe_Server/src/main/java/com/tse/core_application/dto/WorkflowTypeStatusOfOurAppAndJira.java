package com.tse.core_application.dto;

import com.tse.core_application.custom.model.WorkflowTaskStatusIdTypeState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkflowTypeStatusOfOurAppAndJira {
    private List<String> jiraCustomWorkFlowStatusList;
    private List<WorkflowTaskStatusIdTypeState> workflowTaskStatusIdTypeStateList;
}
