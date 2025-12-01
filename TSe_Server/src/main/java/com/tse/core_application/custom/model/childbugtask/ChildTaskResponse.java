package com.tse.core_application.custom.model.childbugtask;

import lombok.Data;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class ChildTaskResponse {

    @Nullable
    private String taskNumber;

    private Long taskId;

    private String taskTitle;

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
    private String workflowTaskStatus;

    @NotNull
    private Long parentTaskId;

    @Nullable
    private Long parentTaskNewVersion;

    @Nullable
    private LocalDateTime parentTaskNewLastUpdatedDateTime;
}
