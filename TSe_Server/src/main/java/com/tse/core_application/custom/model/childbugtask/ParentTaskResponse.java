package com.tse.core_application.custom.model.childbugtask;


import lombok.Data;
import org.springframework.lang.Nullable;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ParentTaskResponse {

    private String taskNumber;
    private Long taskId;
    private String taskTitle;

    @Nullable
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSystem;

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
    private LocalDateTime systemDerivedEndTs;

    @Nullable
    private Integer userPerceivedPercentageTaskCompleted;

    @Nullable
    private Integer recordedEffort;

    @Nullable
    private String workflowTaskStatus;

    @Nullable
    private List<Long> childTaskIds;
}
