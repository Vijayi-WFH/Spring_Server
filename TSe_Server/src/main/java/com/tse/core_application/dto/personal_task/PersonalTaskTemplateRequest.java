package com.tse.core_application.dto.personal_task;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.UserAccount;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PersonalTaskTemplateRequest {

    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String templateTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_DESC)
    @Size(min = 3, max = 1000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

    private Integer taskEstimate;

    private String taskPriority;

    @NotNull(message = ErrorConstant.Task.fk_WORK_FLOW_TASK_STATUS_ID)
    private Integer taskWorkFlowStatus;
}
