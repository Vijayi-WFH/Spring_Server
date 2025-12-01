package com.tse.core_application.dto.duplicate_task;


import com.tse.core_application.custom.model.DependentTaskDetail;
import com.tse.core_application.custom.model.childbugtask.LinkedTask;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class DuplicateTask {

    private String taskTitle;

    private String taskDesc;

    private String taskPriority;

    private Integer taskEstimate;

    private Integer isBallparkEstimate = 1;

    private Integer isEstimateSystemGenerated = 1;

    private String acceptanceCriteria;

    private Long orgId;

    private Long teamId;

    private Long projectId;

    private Integer taskWorkflowId;

    private String workflowTaskStatus;

    private String parentTaskNumber;

    private LocalDateTime taskExpStartDate;

    private LocalTime taskExpStartTime;

    private LocalDateTime taskExpEndDate;

    private LocalTime taskExpEndTime;

    private Integer taskTypeId;

    private Integer environmentId;

    private Integer severityId;

    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.PlaceOfIdentification placeOfIdentification;

    private Boolean customerImpact;

    private List<LinkedTask> linkedTaskList;

    @Nullable
    private List<DependentTaskDetail> dependentTaskDetailRequestList;

}
