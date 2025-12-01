package com.tse.core_application.custom.model;

import com.tse.core_application.constants.RelationDirection;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependentTaskDetailResponse {
    private Long dependencyId;

    private Integer relationTypeId; // FS, FF, SS, SF

    private RelationDirection relationDirection; // Whether the relatedTaskNumber is a predecessor or successor to the given task

    private Long taskId;

    private String taskNumber;

    private Long teamId;

    private String taskTitle;

    private String taskDesc;

    private LocalDateTime taskExpStartDate;

    private LocalDateTime taskExpEndDate;

    private Integer taskEstimate;

    private Long accountIdAssigned;

    private String workflowTaskStatus;

    private Integer taskTypeId;

    private Integer userPerceivedPercentageTaskCompleted;

    private Integer recordedEffort;

    private com.tse.core_application.model.StatType taskProgressSystem;

    private LocalDateTime systemDerivedEndTs;

}

