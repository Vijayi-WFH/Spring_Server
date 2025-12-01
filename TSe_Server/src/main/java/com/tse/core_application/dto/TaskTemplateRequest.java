package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.*;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class TaskTemplateRequest {

    @NotNull(message = ErrorConstant.Task.ACCOUNT_ID)
    private UserAccount fkAccountIdCreator;

    @NotNull(message = ErrorConstant.Sprint.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.Sprint.ENTITY_ID)
    private Long entityId;

    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String templateTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_DESC)
    @Size(min = 3, max = 1000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

    @NotNull(message = ErrorConstant.Task.TASK_WORKFLOW_ID)
    private Integer taskWorkflowId;

    private Integer taskEstimate;

    private String taskPriority;

    @NotNull(message = ErrorConstant.Task.fk_WORK_FLOW_TASK_STATUS_ID)
    private Integer taskWorkFlowStatus;

    private Long teamId;

    private Long projectId;

    private Long orgId;
}
