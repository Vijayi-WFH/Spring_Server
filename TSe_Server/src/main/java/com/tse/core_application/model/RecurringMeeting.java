package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.constants.MeetingType;
import com.tse.core_application.utils.LongListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_meeting", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(value = { "recurMeetingLabels" }, allowGetters = true)
public class RecurringMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurring_meeting_id")
    private Long recurringMeetingId;

    @Column(name = "recurring_meeting_number", length = 30, nullable = false)
    private String recurringMeetingNumber;

    @Column(name = "recurring_meeting_start_date")
    private LocalDateTime recurringMeetingStartDateTime;

    @Column(name = "recurring_meeting_end_date")
    private LocalDateTime recurringMeetingEndDateTime;

    @Column(name = "is_cancelled")
    private Boolean isCancelled;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "num_of_occurrences")
    private Integer numOfOccurrences;

    @Column(name = "recurring_frequency_indicator", nullable = false)
    private Integer recurringFrequencyIndicator;

    @Column(name = "recur_days")
    private String recurDays;

    @Column(name = "recur_every")
    private Integer recurEvery;

//    @Column(name = "all_meetings_start_date_time_list")
//    @Convert(converter = LocalDateTimeListConverter.class)
//    private List<LocalDateTime> meetingStartDateTimeList;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_meeting_id")
    @JsonManagedReference
    private List<Meeting> meetingList;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "recurring_meeting_label", schema = Constants.SCHEMA_NAME, joinColumns = @JoinColumn(name = "recurring_meeting_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    private List<Label> recurMeetingLabels = new ArrayList<>();

    // added all meetings updatable fields in recurring meeting
    @Column(name = "meeting_key")
    private String meetingKey;

    @Column(name = "organiser_account_id", nullable = false)
    private Long organizerAccountId;

    @Column(name = "meeting_type")
    private String meetingType;

    @Column(name = "venue")
    private String venue;

    @Column(name = "meeting_start_time")
    private LocalTime meetingStartTime;  // default start time : 10:00 am

    @Column(name = "agenda")
    private String agenda;

    @Column(name = "reminder_time")
    private Integer reminderTime;

    @Column(name = "created_account_id", nullable = false)
    private Long createdAccountId;

    @Column(name = "updated_account_id")
    private Long updatedAccountId;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "org_id")
    @NotNull(message = ErrorConstant.Meeting.ORGANISATION_ID)
    private Long orgId;

    @Column(name = "bu_id")
    private Long buId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "minutes_of_meeting")
    private String minutesOfMeeting;

    @Column(name = "reference_entity_number")
    private String referenceEntityNumber;

    @Column(name = "reference_entity_type_id")
    private Integer referenceEntityTypeId;

    @Column(name = "attendee_accounts")
    @Convert(converter = LongListConverter.class)
    private List<Long> attendeeAccounts;

    @Column(name = "recurring_meeting_type_id")
    private Integer recurringMeetingTypeId = MeetingType.MEETING.getValue();

    @Column(name = "is_external_link")
    private Boolean isExternalLink = false;
}
