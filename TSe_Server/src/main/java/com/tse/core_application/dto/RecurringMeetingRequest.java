package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.constants.MeetingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecurringMeetingRequest {

    private Long recurringMeetingId;
    private String meetingKey;

    private String meetingNumber;
    private Boolean isCancelled;
    @NotNull
    private Long organizerAccountId;
    private String meetingType;
    private String venue;
    private LocalTime meetingStartTime;  // default start time : 10:00 am
    private String agenda;
    private Integer reminderTime;
    private List<AttendeeRequest> attendeeRequestList;
    @NotNull
    private Long createdAccountId;
    private Long updatedAccountId;
    private Integer duration;
    @NotNull(message = ErrorConstant.Meeting.ORGANISATION_ID)
    private Long orgId;
    private Long buId;
    private Long projectId;
    private Long teamId;
    @NotNull
    private String title;
    private String minutesOfMeeting;
    private String referenceEntityNumber;
    private Integer referenceEntityTypeId;

    // Recurring fields
    private LocalDateTime recurringMeetingStartDateTime;  // default date : current date
    private LocalDateTime recurringMeetingEndDateTime;   // default : current date + 30 days
    private Integer numOfOccurrences;
    @NotNull
    private Integer recurringFrequencyIndicator;
    private String recurDays;
    @Min(value = 1, message = ErrorConstant.Meeting.RECUR_DAYS_LIMIT)
    @Max(value = 30, message = ErrorConstant.Meeting.RECUR_DAYS_LIMIT)
    private Integer recurEvery;
    private Integer recurWeek;
    private List<String> labelsToAdd;
    private Integer recurringMeetingTypeId = MeetingType.MEETING.getValue(); // meeting type is meeting or collaboration
}
