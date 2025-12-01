package com.tse.core_application.repository;

import com.tse.core_application.custom.model.WorkflowTypeIdDesc;
import com.tse.core_application.model.WorkFlowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkFlowTypeRepository extends JpaRepository<WorkFlowType, Integer> {

    @Query("select NEW com.tse.core_application.custom.model.WorkflowTypeIdDesc(w.workflowTypeId, w.workflowDesc) from WorkFlowType w WHERE w.workflowTypeId in :workflowTypeIdList")
    List<WorkflowTypeIdDesc> findWorkFlowTypeIdWorkFlowDescByWorkFlowTypeIdIn(List<Integer> workflowTypeIdList);

}
