package com.tse.core_application.service.Impl;

import com.tse.core_application.model.WorkFlowEpicStatus;
import com.tse.core_application.repository.WorkFlowEpicStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowEpicStatusService {

    @Autowired
    private WorkFlowEpicStatusRepository workFlowEpicStatusRepository;

    public List<WorkFlowEpicStatus> getWorkflowTaskStatusIdEpic() {
        return workFlowEpicStatusRepository.findAll();
    }
}
