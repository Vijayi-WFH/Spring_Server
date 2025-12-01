package com.tse.core_application.repository;

import com.tse.core_application.custom.model.WorkflowTaskStatusIdTypeState;
import com.tse.core_application.model.WorkFlowTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkFlowTaskStatusRepository extends JpaRepository<WorkFlowTaskStatus, Integer> {

    List<WorkFlowTaskStatus> findWorkflowTaskStatusByWorkflowTaskStatus(String workflowTaskStatus);

    WorkFlowTaskStatus findWorkflowTaskStatusByWorkflowTaskStatusId(Integer workflowTaskStatusId);

    List<WorkFlowTaskStatus> findByFkWorkFlowTypeWorkflowTypeId(Integer workflowTypeId);

    WorkFlowTaskStatus findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(String workflowTaskStatus, Integer workflowTypeId);
    WorkFlowTaskStatus findByWorkflowTaskStatusIdAndFkWorkFlowTypeWorkflowTypeId(Integer workflowTaskStatusId, Integer workflowTypeId);

    @Query("select NEW com.tse.core_application.custom.model.WorkflowTaskStatusIdTypeState(w.workflowTaskStatusId, w.workflowTaskStatus, w.fkWorkFlowType.workflowTypeId, w.workflowTaskState) from WorkFlowTaskStatus w WHERE w.fkWorkFlowType.workflowTypeId in :workflowTypeIdList")
    List<WorkflowTaskStatusIdTypeState> getWorkflowTaskStatusIdTypeState(List<Integer> workflowTypeIdList);

    @Query("select w.workflowTaskStatusId from WorkFlowTaskStatus w where w.workflowTaskStatus in :workFlowTaskStatuses")
    List<Integer> findWorkflowTaskStatusIdByWorkflowTaskStatusIn(List<String> workFlowTaskStatuses);


    WorkFlowTaskStatus findByWorkflowTaskStatusId(Integer workflowTaskStatusId);
}
