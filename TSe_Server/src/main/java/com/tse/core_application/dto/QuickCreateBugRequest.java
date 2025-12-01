package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.DependentTaskDetail;
import com.tse.core_application.custom.model.childbugtask.LinkedTask;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class QuickCreateBugRequest {
    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    private String taskTitle;
    private String taskDesc;
    private String taskPriority;
    @NotNull(message = ErrorConstant.Task.TASK_WORKFLOW_ID)
    private Integer taskWorkFlowId;
    @NotNull(message = ErrorConstant.Task.fk_WORK_FLOW_TASK_STATUS_ID)
    private Integer taskWorkFlowStatus;
    private LocalDateTime expStartDateTime;
    private LocalTime expStartTime;
    private LocalDateTime expEndDateTime;
    private LocalTime expEndTime;
    private Long assignTo;
    private Long bugReportedBy;
    private Integer estimate;
    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    private Long teamId;
    private Long sprintId;
    private Boolean customerImpact;
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.PlaceOfIdentification placeOfIdentification;
    @NotNull(message = ErrorConstant.BugTask.SEVERITY)
    private Integer severityId;
    @NotNull(message = ErrorConstant.BugTask.ENVIRONMENT)
    private Integer environmentId;
    private List<LinkedTask> linkedTaskList;
    private List<String> labelsToAdd;
    private List<Long> referenceWorkItemId;
    private Long epicId;
    private List<DependentTaskDetail> dependentTaskDetailRequestList;
    private Integer rcaId;
    @Pattern(
            regexp = "^(?:[a-zA-Z])?(0|[1-9]\\d*)(?:\\.(0|[1-9]\\d*))?(?:\\.(0|[1-9]\\d*))?(?:-(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
            message = ErrorConstant.ReleaseVersion.INVALID_VERSION_PATTERN
    )
    private String releaseVersionName;
    private Boolean isStarred;
    private Boolean isToCreateDuplicateTask;
}
