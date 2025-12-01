package com.tse.core_application.repository;

import com.tse.core_application.dto.EpicRequest;
import com.tse.core_application.model.WorkFlowEpicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkFlowEpicStatusRepository extends JpaRepository<WorkFlowEpicStatus, Integer> {
    WorkFlowEpicStatus findByWorkflowEpicStatus(String status);

    WorkFlowEpicStatus findByWorkflowEpicStatusId(Integer workflowEpicStatusId);
}
