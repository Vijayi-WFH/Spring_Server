package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.NewEffortTrack;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UpdateTaskRequest {
    @NotNull(message = ErrorConstant.Task.TASK_ID)
    private Long taskId;

    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @Size(min = 3, max = 5000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

    private LocalDateTime taskExpStartDate;

    private LocalDateTime taskActStDate;

    private LocalDateTime taskActEndDate;

    private java.time.LocalTime taskActStTime;

    private java.time.LocalTime taskActEndTime;

    private java.time.LocalTime taskExpStartTime;

    private LocalDateTime taskExpEndDate;

    private java.time.LocalTime taskExpEndTime;

    private Integer taskWorkflowId;

    private Integer taskEstimate;

    @Size(min = 3, max=1000, message= ErrorConstant.Task.PARKING_LOT)
    private String parkingLot;

    @Size(max=1000, message=ErrorConstant.Task.KEY_DECISIONS)
    private String keyDecisions;

    @Size(max=1000, message=ErrorConstant.Task.ACCEPTANCE_CRITERIA)
    private String acceptanceCriteria;

    private String taskPriority;

    private Integer increaseInUserPerceivedPercentageTaskCompleted;

    private Integer resolutionId;

    private Integer severityId;

    @Size(max=1000, message=ErrorConstant.Task.STEPS_TAKEN_TO_COMPLETE )
    private String stepsTakenToComplete;

    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.PlaceOfIdentification placeOfIdentification;

    private Boolean customerImpact;

    private Long accountIdAssignee;

    private Long accountIdAssigned;

    private List<NewEffortTrack> newEffortTracks;

    private Integer workflowTaskStatusId;

    private Integer blockedReasonTypeId;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.EXPLANATION_LIMIT)
    private String blockedReason;

    private Long respondentAccountId;

    private Integer reminderInterval;

    private LocalDateTime nextReminderDateTime;

    private Integer userPerceivedPercentageTaskCompleted;

}
