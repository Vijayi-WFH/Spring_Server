package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.WorkflowTaskStatusIdTypeState;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.WorkFlowTaskStatus;
import com.tse.core_application.repository.WorkFlowTaskStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WorkflowTaskStatusService {

    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;

    public List<WorkFlowTaskStatus> getAllWorkflowTaskStatus() {
        List<WorkFlowTaskStatus> allWorkflowsFoundDb = workFlowTaskStatusRepository.findAll();
        return allWorkflowsFoundDb;
    }

    public List<WorkflowTaskStatusIdTypeState> getWorkflowTaskStatusIdTypeState() {
        return workFlowTaskStatusRepository.getWorkflowTaskStatusIdTypeState(Constants.DEFAULT_WORKFLOW);
    }


        public List<WorkFlowTaskStatus> getAllWorkflowTaskStatusByWorkflowTaskStatus(String workflowTaskStatus) {
        List<WorkFlowTaskStatus> allWorkflowFoundDb = workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatus(workflowTaskStatus);
        return allWorkflowFoundDb;
    }

    public WorkFlowTaskStatus getWorkflowTaskStatusByWorkflowTaskStatusId(Integer workflowTaskStatusId) {
        if(workflowTaskStatusId != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatusDb = workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatusId(workflowTaskStatusId);
            return foundWorkflowTaskStatusDb;
        }
        return null;
    }

    public WorkFlowTaskStatus getWorkFlowTaskStatusByWorkflowTaskStatusAndWorkflowTypeId(String workflowTaskStatus, Integer workflowTypeId){
        WorkFlowTaskStatus WorkFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(workflowTaskStatus, workflowTypeId);
        return WorkFlowTaskStatus;

    }

    public boolean isTaskWorkflowStatusCompleted(Task task) {
        boolean isWorkflowStatusCompleted = false;
        if(task != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if(foundWorkflowTaskStatus != null) {
                if(Objects.equals(foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED.toLowerCase())) {
                    isWorkflowStatusCompleted = true;
                }
            }
        }
        return isWorkflowStatusCompleted;
    }

    public boolean isTaskWorkflowStatusBacklog(Task task) {
        boolean isWorkflowStatusBacklog = false;
        if(task != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if(foundWorkflowTaskStatus != null) {
                if(Objects.equals(foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase())) {
                    isWorkflowStatusBacklog = true;
                }
            }
        }
        return isWorkflowStatusBacklog;
    }

    @Transactional(readOnly = true)
    public void setTaskWorkflowStatusCompletedByWorkflowType(Task task) {
        if(task != null) {
            Integer taskWorkflowTypeId = task.getTaskWorkflowId();
            List<WorkFlowTaskStatus> foundWorkflowStatus = workFlowTaskStatusRepository.findByFkWorkFlowTypeWorkflowTypeId(taskWorkflowTypeId);
            for(WorkFlowTaskStatus workflowStatus: foundWorkflowStatus) {
                if(Objects.equals(workflowStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED.toLowerCase())) {
                   task.setFkWorkflowTaskStatus(workflowStatus);
                }
            }
        }
    }

    public List<WorkFlowTaskStatus> getAllWorkflowStatusByWorkflowTypeId(Integer workflowTypeId) {
        if(workflowTypeId != null) {
            List<WorkFlowTaskStatus> allWorkflowStatusFoundDb = workFlowTaskStatusRepository.findByFkWorkFlowTypeWorkflowTypeId(workflowTypeId);
            return  allWorkflowStatusFoundDb;
        }
        return null;
    }

    /* This method is used to get all the workflowTaskStatus from the WorkflowTaskStatus table without the
    * key "FkWorkflowType". */
    public List<WorkFlowTaskStatus> getAllWorkflowTaskStatusesExcludingWorkflowType() {
        List<WorkFlowTaskStatus> allWorkflowsFoundDb = workFlowTaskStatusRepository.findAll();
        List<WorkflowTaskStatusIdTypeState> workflowTaskStatusIdTypeStateList = new ArrayList<>();
        if(!allWorkflowsFoundDb.isEmpty()) {
            for(WorkFlowTaskStatus workFlowTaskStatus: allWorkflowsFoundDb) {
                workFlowTaskStatus.setFkWorkFlowType(null);
            }
        }
        return allWorkflowsFoundDb;
    }

    public Map<String, List<WorkFlowTaskStatus>> createWorkflowStatusMap(List<WorkFlowTaskStatus> allWorkflowTaskStatuses) {

        final Map<String, List<WorkFlowTaskStatus>> values = new HashMap<>();

        allWorkflowTaskStatuses.forEach(workFlowTaskStatus -> {
            if (values.containsKey(workFlowTaskStatus.getWorkflowTaskStatus())) {
                List<WorkFlowTaskStatus> existingList = values.get(workFlowTaskStatus.getWorkflowTaskStatus());
                existingList.add(workFlowTaskStatus);
                values.put(workFlowTaskStatus.getWorkflowTaskStatus(), existingList);
            } else {
                List<WorkFlowTaskStatus> wfStatus = new ArrayList<>();
                wfStatus.add(workFlowTaskStatus);
                values.put(workFlowTaskStatus.getWorkflowTaskStatus(), wfStatus);
            }
        });

        return values;
    }
}
