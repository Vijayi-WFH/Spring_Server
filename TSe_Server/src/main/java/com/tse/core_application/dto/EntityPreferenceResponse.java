package com.tse.core_application.dto;

import lombok.Data;

import javax.persistence.Column;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
public class EntityPreferenceResponse {
    private Long entityPreferenceId;
    private Integer entityTypeId;
    private Long entityId;
    private Long allowedFileSize;
    private Integer breakDuration;
    private LocalTime officeHrsStartTime;
    private LocalTime officeHrsEndTime;
    private List<Integer> offDays;
    private List<HolidayResponse> holidays;
    private Integer taskEffortEditDuration; // in minutes
    private Integer meetingEffortEditDuration; // in minutes
    private Integer meetingEffortPreferenceId;
    private List<Integer> referenceTaskMeetingRoleIdList;
    private Boolean requireMinimumSignUpDetails;
    private Long capacityLimit;
    private Integer defaultOfficeDuration;
    private List<Integer> rolesWithPerfNoteRights;
    private String quickCreateWorkflowStatus;
    private String sickLeaveAlias;
    private String timeOffAlias;
    private Integer minApprovedSickDaysWithoutMedicalCert;
    private List<Integer> starringWorkItemRoleIdList;
    private Boolean isGeoFencingAllowed;
    private Boolean isGeoFencingActive;
    private Boolean meetAutoLogEffortEnabled;
    private Integer forwardDated;
    private Integer backwardDated;
    private Integer payRollGenerationDay;
    private Boolean isPayRollAtLastDayOfMonth;
    private Boolean isPayRollAtLastSecondDayOfMonth;
}
