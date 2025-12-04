package com.tse.core_application.dto;

import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.dto.meeting.MeetingLinkInfo;
import com.tse.core_application.model.Attendee;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RecurringMeetingResponse {

    private Long recurringMeetingId;
    private String recurringMeetingNumber;
    private LocalDateTime recurringMeetingStartDateTime;  // default date : current date
    private LocalDateTime recurringMeetingEndDateTime;   // default : current date + 30 days
    private Boolean isCancelled;
    private Integer numOfOccurrences;
    private Integer recurringFrequencyIndicator;
    private String recurDays;
    private Integer recurEvery;
    private String meetingKey;
    private Long organizerAccountId;
    private String meetingType;
    private String venue;
    private LocalTime meetingStartTime;  // default start time : 10:00 am
    private Integer duration;
    private String title;
    private String minutesOfMeeting;
    private String agenda;
    private Integer reminderTime;
    private List<Attendee> attendeeRequestList;
    private Long createdAccountId;
    private Long updatedAccountId;
    private Long orgId;
    private Long buId;
    private Long projectId;
    private Long teamId;
    private String referenceEntityNumber;
    private Integer referenceEntityTypeId;
    private String entityName;
    private List<LabelResponse> labels;

    // added for PT-13330: Meeting link display and toggle
    private Boolean isExternalLink;
    private MeetingLinkInfo meetingLinkInfo;

}
