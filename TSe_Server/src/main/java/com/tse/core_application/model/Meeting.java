package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.*;

import com.tse.core_application.configuration.DataEncryptionConverter;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.constants.MeetingType;
import com.tse.core_application.dto.ModelFetchedDto;
import com.tse.core_application.utils.ModelFetchedDtoListConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(value = { "meetingLabels" }, allowGetters = true)
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Long meetingId;

    @Column(name = "meeting_number", nullable = false, length = 30)
    private String meetingNumber;

    @Column(name = "meeting_key", length=1000)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=255)
    private String meetingKey;

    @Column(name = "title", nullable = false)
    @Size(max = 255)
    private String title;

    @Column(name = "organizer_account_id")
    private Long organizerAccountId;

    @Column(name = "meeting_type_indicator", length = 30)
    private Integer meetingTypeIndicator;

    @Column(name = "venue", length=500)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=100)
    private String venue;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "actual_start_date_time")
    private LocalDateTime actualStartDateTime;  // added in task 2851

    @Column(name = "actual_end_date_time")
    private LocalDateTime actualEndDateTime;    // added in task 2851

    @Column(name = "meeting_progress")
    @Enumerated(EnumType.STRING)
    private MeetingStats meetingProgress;      // added in task 2851

    @Column(name = "is_cancelled")
    private Boolean isCancelled = false;      // added in task 2851

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "agenda", length=1000)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=255)
    private String agenda;

    @Column(name = "reminder_time")
    private Integer reminderTime;

    @Column(name = "created_date_time", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdDateTime;

    @NotNull
    @Column(name = "created_account_id")
    private Long createdAccountId;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "updated_account_id")
    private Long updatedAccountId;

    @Column(name = "team_id")   // team id , bu id , project id , org id added in Task 2539
    private Long teamId;

    @Column(name = "bu_id")
    private Long buId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "attendee_id")
    private Long attendeeId;

    // following 5 fields added in task 2676
    @Column(name = "minutes_of_meeting",length = 20000)
    @Size(max = 5000, message = ErrorConstant.Meeting.MOM_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String minutesOfMeeting;

    @Column(name = "reference_entity_number")
    private String referenceEntityNumber;

    @Column(name = "reference_entity_type_id")
    private Integer referenceEntityTypeId;

    @Column(name="is_fetched" ,nullable=false)
    private Boolean isFetched=false;

    @Column(name = "model_fetched_list", columnDefinition = "TEXT")
    @Convert(converter = ModelFetchedDtoListConverter.class)
    private List<ModelFetchedDto> modelFetchedList;

//    @Column(name = "recurring_meeting_id", updatable = false, insertable = false)
//    private Long recurringMeetingId;

//    @Column(name = "recurring_reference_meeting_num")
//    private Long recurringReferenceMeetingNum;

    @OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Attendee> attendeeList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_meeting_id", referencedColumnName = "recurring_meeting_id")
    @JsonBackReference
    private RecurringMeeting recurringMeeting;

    @OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ActionItem> actionItems;

    @OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MeetingNote> meetingNotes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "meeting_label", schema = Constants.SCHEMA_NAME, joinColumns = @JoinColumn(name = "meeting_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    private List<Label> meetingLabels = new ArrayList<>();

    @Column(name = "referenced_meeting_reason_id")
    private Integer referencedMeetingReasonId;

    @Column(name = "meeting_type_id")
    private Integer meetingTypeId = MeetingType.MEETING.getValue();

    @Column(name = "view_transcription")
    private Boolean viewTranscription = false;

    @Column(name = "is_external_link")
    private Boolean isExternalLink = false;

}
