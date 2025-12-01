package com.tse.core_application.custom.model;

import com.tse.core_application.model.Team;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.WorkFlowTaskStatus;
import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

@Value
public class BoardViewProjection {

    private Long taskId;

    private Integer taskTypeId;

    private String taskNumber;

    private Long sprintId;

    private String taskTitle;

    private LocalDateTime taskExpEndDate;

    private LocalDateTime taskActEndDate;

    private java.time.LocalTime taskActEndTime;

    private LocalDateTime taskActStDate;

    private java.time.LocalTime taskActStTime;

    private Integer taskEstimate;

    private Boolean currentlyScheduledTaskIndicator;

    private Integer userPerceivedPercentageTaskCompleted;

    private Integer taskWorkflowId;

    private WorkFlowTaskStatus fkWorkflowTaskStatus;

    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSystem;

    private Boolean unplannedScheduledTaskIndicator;

    private UserAccount fkAccountIdAssigned;

    private Team fkTeamId;

    private String taskPriority;

    private Integer recordedEffort;

    private LocalDateTime systemDerivedEndTs;
}
