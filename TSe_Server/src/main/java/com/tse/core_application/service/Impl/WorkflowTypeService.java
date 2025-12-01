package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.WorkFlowEpicStatus;
import com.tse.core_application.repository.WorkFlowTypeRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowTypeService {

    private static final Logger logger = LogManager.getLogger(WorkflowTypeService.class.getName());

    @Autowired
    private WorkFlowTypeRepository workFlowTypeRepository;

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private WorkflowTaskStatusService workflowTaskStatusService;

    @Autowired
    private WorkflowEpicStatusService workflowEpicStatusService;

    public List<WorkflowTypeIdDesc> getAllWorkflowType() {
        return workFlowTypeRepository.findWorkFlowTypeIdWorkFlowDescByWorkFlowTypeIdIn(com.tse.core_application.model.Constants.DEFAULT_WORKFLOW);
    }

    public ResponseEntity<Object> getFormattedResponseOfGetAllWorkflowsAndPriorities(WorkflowTypeStatusPriorityResponse workflowTypeStatusPriorityResponse) {
        if (workflowTypeStatusPriorityResponse.getWorkflowType().isEmpty() || workflowTypeStatusPriorityResponse.getWorkflowTaskStatus().isEmpty() ||
                workflowTypeStatusPriorityResponse.getPriority().isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No Data found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        } else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, workflowTypeStatusPriorityResponse);
        }
    }

    public WorkflowTypeStatusPriorityResponse getAllWorkflowTypeStatusPriority() {
        List<PriorityIdDescDisplayAs> priorityIdDescDisplayAsList = priorityService.getAllPriority();
        List<WorkflowTypeIdDesc> workflowTypeIdDescList = getAllWorkflowType();
        List<WorkflowTaskStatusIdTypeState> workflowTaskStatusIdTypeList = workflowTaskStatusService.getWorkflowTaskStatusIdTypeState();
        WorkflowTypeStatusPriorityResponse workflowTypeStatusPriorityResponse = new WorkflowTypeStatusPriorityResponse();
        workflowTypeStatusPriorityResponse.setWorkflowType(workflowTypeIdDescList);
        workflowTypeStatusPriorityResponse.setPriority(priorityIdDescDisplayAsList);
        workflowTypeStatusPriorityResponse.setWorkflowTaskStatus(workflowTaskStatusIdTypeList);

        return workflowTypeStatusPriorityResponse;
    }

    public ResponseEntity<Object> getFormattedResponseOfGetAllWorkflowsAndPrioritiesEpic(WorkflowTypeStatusPriorityResponseForEpic workflowTypeStatusPriorityResponseForEpic) {
        if (workflowTypeStatusPriorityResponseForEpic.getWorkflowEpicStatusList().isEmpty() || workflowTypeStatusPriorityResponseForEpic.getPriority().isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No Data found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        } else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, workflowTypeStatusPriorityResponseForEpic);
        }
    }

    public WorkflowTypeStatusPriorityResponseForEpic getAllWorkflowTypeStatusPriorityEpic() {
        List<PriorityIdDescDisplayAs> priorityIdDescDisplayAsList = priorityService.getAllPriority();
        List<WorkFlowEpicStatus> workflowEpicStatusList = workflowEpicStatusService.getWorkflowTaskStatusIdEpic();
        WorkflowTypeStatusPriorityResponseForEpic workflowTypeStatusPriorityResponse = new WorkflowTypeStatusPriorityResponseForEpic();
        workflowTypeStatusPriorityResponse.setPriority(priorityIdDescDisplayAsList);
        workflowTypeStatusPriorityResponse.setWorkflowEpicStatusList(workflowEpicStatusList);

        return workflowTypeStatusPriorityResponse;
    }
}
