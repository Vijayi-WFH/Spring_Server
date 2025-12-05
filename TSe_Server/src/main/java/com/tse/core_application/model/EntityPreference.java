package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core_application.utils.IntegerListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "entity_preference", schema= Constants.SCHEMA_NAME, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type_id", "entity_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_preference_id")
    private Long entityPreferenceId;

    @Column(name = "entity_type_id")
    private Integer entityTypeId;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "allowed_file_size")
    private Long allowedFileSize; // in bytes

    @Column(name = "office_hrs_start_time")
    private LocalTime officeHrsStartTime;

    @Column(name = "office_hrs_end_time")
    private LocalTime officeHrsEndTime;

    @Column(name = "break_duration")
    private Integer breakDuration; // in minutes

    @Column(name = "off_days")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> offDays;

    @OneToMany(mappedBy = "entityPreference")
    @JsonManagedReference
    private List<HolidayOffDay> holidayOffDays;

    @Column(name = "task_effort_edit_duration")
    private Integer taskEffortEditDuration;

    @Column(name = "meeting_effort_edit_duration")
    private Integer meetingEffortEditDuration;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time")
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "meeting_effort_preference_id")
    private Integer meetingEffortPreferenceId = Constants.MeetingPreferenceEnum.NO_EFFORTS.getMeetingPreferenceId(); // no effort

    @Column(name = "reference_task_meeting_role_id_list")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> referenceTaskMeetingRoleIdList;

    @Column(name = "monthly_leave_update_pro_rata")
    private Boolean isMonthlyLeaveUpdateOnProRata = true;

    @Column(name = "yearly_leave_update_pro_rata")
    private Boolean isYearlyLeaveUpdateOnProRata = true;

    @Column(name = "require_minimum_sign_up_details", nullable = false)
    private Boolean requireMinimumSignUpDetails = false;

    @Column(name = "capacity_limit")
    private Long capacityLimit;

    @Column(name = "minutes_to_work_daily")
    private Integer minutesToWorkDaily;

    @Column(name = "roles_with_status_inquiry_rights")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> rolesWithStatusInquiryRights;

    @Column(name = "delay_inquiry_enabled")
    private Boolean delayInquiryEnabled;

    @Column(name = "roles_with_perf_note_rights")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> rolesWithPerfNoteRights;

    @Column(name = "quick_create_workflow_status")
    private String quickCreateWorkflowStatus;

    @Column(name = "leave_requester_cancel_time")
    private LocalTime leaveRequesterCancelTime;

    @Column(name = "leave_requester_cancel_date")
    private Integer leaveRequesterCancelDate;

    @Column(name = "sick_leave_alias")
    private String sickLeaveAlias;

    @Column(name = "time_off_alias")
    private String timeOffAlias;

    @Column(name = "min_approved_sick_days_without_medical_cert")
    private Integer minApprovedSickDaysWithoutMedicalCert = 1;

    @Column(name = "should_otp_send_to_org_admin")
    private Boolean shouldOtpSendToOrgAdmin = false;

    @Column(name = "should_invite_link_send_to_org_admin")
    private Boolean shouldInviteLinkSendToOrgAdmin = false;

    // ZZZZZZ 14-04-2025
    @Column(name = "office_hrs_start_date_time")
    private LocalDateTime officeHrsStartDateTime;

    @Column(name = "office_hrs_end_date_time")
    private LocalDateTime officeHrsEndDateTime;

    @Column(name = "buffer_time_to_start_sprint_early")
    private Integer bufferTimeToStartSprintEarly;

    @Column(name = "starring_work_item_role_id_list")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> starringWorkItemRoleIdList;

    @Column(name = "is_geofencing_allowed", nullable = false)
    private Boolean isGeoFencingAllowed = false;

    @Column(name = "is_geofencing_active", nullable = false)
    private Boolean isGeoFencingActive = false;

    @Column(name = "meet_auto_log_effort_enabled")
    private Boolean meetAutoLogEffortEnabled = false;

    private Integer forwardDated;

    private Integer backwardDated;

    private Integer payRollGenerationDay;
    private Boolean isPayRollAtLastDayOfMonth;
    private Boolean isPayRollAtLastSecondDayOfMonth;

    @Column(name = "distance_unit_id")
    private Integer distanceUnitId;  // Default: 1 (KM) - see DistanceUnitEnum
}
