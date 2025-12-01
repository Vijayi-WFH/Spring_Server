package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.DependentTaskDetail;
import lombok.Data;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class QuickCreateTaskRequest {
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
    private Integer estimate;
    private Long epicId;
    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    private Long teamId;
    private Long sprintId;
    private List<String> labelsToAdd;
    @Nullable
    private List<DependentTaskDetail> dependentTaskDetailRequestList;
    private List<Long> referenceWorkItemId;
    @Pattern(
            regexp = "^(?:[a-zA-Z])?(0|[1-9]\\d*)(?:\\.(0|[1-9]\\d*))?(?:\\.(0|[1-9]\\d*))?(?:-(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
            message = ErrorConstant.ReleaseVersion.INVALID_VERSION_PATTERN
    )
    private String releaseVersionName;
    private Boolean isStarred;
    private Boolean isToCreateDuplicateTask;
}
