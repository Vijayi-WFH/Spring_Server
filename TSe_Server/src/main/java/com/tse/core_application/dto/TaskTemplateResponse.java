package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.ProjectIdProjectName;
import com.tse.core_application.custom.model.TeamIdAndTeamName;
import com.tse.core_application.model.*;
import lombok.Data;

import javax.validation.constraints.Size;


@Data
public class TaskTemplateResponse {

    private Long templateId;

    private Long templateNumber;

    private String templateTitle;

    private EmailFirstLastAccountId creatorDetails;

    private Integer entityTypeId;

    private Long entityId;

    private String taskTitle;

    private String taskDesc;

    private Integer taskWorkflowId;

    private Integer taskEstimate;

    private String taskPriority;

    private WorkFlowTaskStatus fkWorkflowTaskStatus;

    private TeamIdAndTeamName team;

    private ProjectIdProjectName project;

    private OrgIdOrgName org;

    private Boolean isEditable = false;
}
