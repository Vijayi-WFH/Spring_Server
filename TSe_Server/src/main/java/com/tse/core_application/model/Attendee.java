package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendee", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Attendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendee_log_id")
    private Long attendeeLogId;

    @Column(name = "attendee_id", nullable = false)
    private Long attendeeId;

    @Column(name = "account_id")
    private Long accountId;

    // added below 3 fields in task 2676
    @Column(name = "bu_id")
    private Long buId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "attendee_invitation_status_id", nullable = false)  // New field added in Task 2539
    private Integer attendeeInvitationStatusId;

    @Column(name = "attendee_invitation_status", nullable = false)  // New field added in task 2539
    private String attendeeInvitationStatus;

    @Column(name = "is_attendee_expected")
    private Integer isAttendeeExpected;

    @Column(name = "did_you_attend")
    private Integer didYouAttend;

    @Column(name = "attendee_duration")
    private Integer attendeeDuration;

    @Column(name = "reminder_attendee_expected_response")
    private Integer reminderAttendeeExpectedResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id", nullable = false)
    @JsonBackReference
    private Meeting meeting;

    @Column(name = "initial_effort_date_time")
    private LocalDateTime initialEffortDateTime;

    @Column(name = "system_gen_efforts")
    private Long systemGenEfforts;

}
