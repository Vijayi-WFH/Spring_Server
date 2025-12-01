package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.DependentTaskDetail;
import com.tse.core_application.validators.annotations.TrimmedSize;
import lombok.Data;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChildTaskRequest {

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @TrimmedSize(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT_SPACES)
    private String taskTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_DESC)
    @TrimmedSize(min = 3, max = 5000, message = ErrorConstant.Task.DESC_LIMIT_SPACES)
    private String taskDesc;

    @Nullable
    private LocalDateTime taskExpStartDate;

    @Nullable
    private LocalDateTime taskExpEndDate;

    @Nullable
    private Integer taskEstimate;

    @Nullable
    private Long accountIdAssigned;

    @NotNull
    private Long parentTaskId;

    @Nullable
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
