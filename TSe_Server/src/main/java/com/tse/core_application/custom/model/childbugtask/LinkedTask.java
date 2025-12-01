package com.tse.core_application.custom.model.childbugtask;

import lombok.Data;
import org.springframework.lang.Nullable;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

@Data
public class LinkedTask {

    @Nullable
    private String taskNumber;

    @Nullable
    private Long taskId;

    @Nullable
    private String taskTitle;

    @Nullable
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
}
