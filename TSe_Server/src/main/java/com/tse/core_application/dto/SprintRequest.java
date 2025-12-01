package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.UserAccount;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
public class SprintRequest {

    private Long sprintId;

    @NotBlank(message = ErrorConstant.Sprint.SPRINT_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Sprint.TITLE_LIMIT)
    private String sprintTitle;

    @NotBlank(message = ErrorConstant.Sprint.SPRINT_OBJECTIVE)
    @Size(min = 3, max = 1000, message = ErrorConstant.Sprint.OBJECTIVE_LIMIT)
    private String sprintObjective;

    @NotNull(message = ErrorConstant.Sprint.START_DATE)
    private LocalDateTime sprintExpStartDate;

    @NotNull(message = ErrorConstant.Sprint.END_DATE)
    private LocalDateTime sprintExpEndDate;

    private LocalDateTime capacityAdjustmentDeadline;

    private LocalDateTime sprintActStartDate;

    private LocalDateTime sprintActEndDate;

    private Integer sprintStatus;

    @NotNull(message = ErrorConstant.Sprint.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.Sprint.ENTITY_ID)
    private Long entityId;
    
    private Boolean autoUpdateTaskDates = true;

    private Boolean canModifyEstimates = false;

    private Boolean canModifyIndicatorStayActiveInStartedSprint = false;

    private Long fetchLoadFactorOfSprint;
}
