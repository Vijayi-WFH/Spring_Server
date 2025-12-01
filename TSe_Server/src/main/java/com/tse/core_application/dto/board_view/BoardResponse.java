package com.tse.core_application.dto.board_view;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.NewEffortTrack;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.model.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BoardResponse {

    private Long taskId;

    private Integer taskTypeId;

    private String taskNumber;

    private String taskTitle;

    private LocalDateTime taskExpEndDate;

    private LocalDateTime taskActEndDate;

    private java.time.LocalTime taskActEndTime;

    private LocalDateTime taskActStDate;

    private java.time.LocalTime taskActStTime;

    private Integer taskEstimate;

    // this is for last working day's date & effort //
    private Integer previousDayEffort;

    private LocalDate previousDayEffortDate;

    private Integer previousDayIncreaseInUserPerceivedPercentageTaskCompleted;

    // this is for current day's date & effort //
    private Integer currentDayNewRecordedEffort;

    private LocalDate currentDayNewRecordedEffortDate;

    private Integer newIncreaseInUserPerceivedPercentageTaskCompleted;

    private Boolean currentlyScheduledTaskIndicator;

    private Integer userPerceivedPercentageTaskCompleted;

    private Integer taskWorkflowId;

    private String workflowTaskStatus;

    private com.tse.core_application.model.StatType taskProgressSystem;

    private Boolean unplannedScheduledTaskIndicator;

    private Long accountIdAssigned;

    private Long teamId;

    private String teamName;

    private List<NewEffortTrack> newEffortTracks;

    private String taskPriority;

    private Integer recordedEffort;

    private LocalDateTime systemDerivedEndTs;


    private Integer blockedReasonTypeId;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.EXPLANATION_LIMIT)
    private String blockedReason;

    private UserAccount fkAccountIdRespondent;

    private Integer reminderInterval;

    private Boolean isBug = false;

    private Boolean isStarred;

    private EmailFirstLastAccountIdIsActive starredBy;
}
