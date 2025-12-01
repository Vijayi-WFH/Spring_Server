package com.tse.core_application.custom.model.childbugtask;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.DependentTaskDetail;
import com.tse.core_application.dto.label.LabelResponse;
import lombok.Data;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChildTask {

    @Nullable
    private String taskNumber;

    @Nullable
    private Long taskId;

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_DESC)
    @Size(min = 3, max = 1000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

    @Nullable
    private LocalDateTime taskExpStartDate;

    @Nullable
    private LocalDateTime taskExpEndDate;

    @Nullable
    private Integer taskEstimate;

    @Nullable
    private Long accountIdAssigned;

    @Nullable
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSystem;

    @Nullable
    private LocalDateTime systemDerivedEndTs;

    @Nullable
    private Integer userPerceivedPercentageTaskCompleted;

    @Nullable
    private Integer recordedEffort;

    @Nullable
    private Integer totalEffort;

    @Nullable
    private Integer recordedTaskEffort;

    @Nullable
    private Integer totalMeetingEffort;

    @Nullable
    private Integer billedMeetingEffort;

    @Nullable
    private String workflowTaskStatus;

    @NotNull
    private Long parentTaskId;

    @Nullable
    private List<LabelResponse> labelDetails;

    @Nullable
    private List<DependentTaskDetail> dependentTaskDetails;
}
