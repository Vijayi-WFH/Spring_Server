package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.Constants;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
public class EntityPreferenceRequest {
    @NotNull(message = Constants.EntityPreference.ENTITY_TYPE_ID)
    private Integer entityTypeId;
    @NotNull(message = Constants.EntityPreference.ENTITY_ID)
    private Long entityId;

    private Long allowedFileSize; // in Mb

    @Min(value = 0, message = ErrorConstant.EntityPreference.BREAK_DURATION)
    @Max(value = 1440, message = ErrorConstant.EntityPreference.BREAK_DURATION)
    private Integer breakDuration; // in minutes

    private LocalTime officeHrsStartTime;
    private LocalTime officeHrsEndTime;
    private List<Integer> offDays;
    private Set<HolidayRequest> holidays;

    @Min(value = 0, message = ErrorConstant.EntityPreference.TASK_DURATION)
    @Max(value = 1440, message = ErrorConstant.EntityPreference.TASK_DURATION)
    private Integer taskEffortEditDuration; // in minutes

    @Min(value = 0, message = ErrorConstant.EntityPreference.MEETING_EFFORT_DURATION)
    @Max(value = 1440, message = ErrorConstant.EntityPreference.MEETING_EFFORT_DURATION)
    private Integer meetingEffortEditDuration; // in minutes

    private Integer meetingEffortPreferenceId;
    private List<Integer> referenceTaskMeetingRoleIdList;
    private Boolean requireMinimumSignUpDetails;
    private Long capacityLimit;
    private Integer minutesToWorkDaily;
    private List<Integer> rolesWithPerfNoteRights;
    private String quickCreateWorkflowStatus;
    private LocalTime leaveRequesterCancelTime;
    private Integer leaveRequesterCancelDate;
    @Size(min = 3, max = 30, message = ErrorConstant.Leave.LEAVE_ALIAS_LIMIT)
    private String sickLeaveAlias = Constants.LeaveTypeNameConstant.SICK_LEAVE;
    @Size(min = 3, max = 30, message = ErrorConstant.Leave.LEAVE_ALIAS_LIMIT)
    private String timeOffAlias = Constants.LeaveTypeNameConstant.TIME_OFF;
    private Integer minApprovedSickDaysWithoutMedicalCert;
    private List<Integer> starringWorkItemRoleIdList;
    private Boolean meetAutoLogEffortEnabled;
    private Integer forwardDated;

    private Integer backwardDated;

    private Integer payRollGenerationDay;

    private Boolean isPayRollAtLastDayOfMonth;

    private Boolean isPayRollAtLastSecondDayOfMonth;
}
