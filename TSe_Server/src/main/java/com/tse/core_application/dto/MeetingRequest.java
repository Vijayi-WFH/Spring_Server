package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.constants.MeetingType;
import com.tse.core_application.model.ActionItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingRequest {

    /**
     * NOTE : MeetingRequest should not contain any field with same name as meeting but different data type , otherwise the methods defined in service layer will throw errors
     */

    private String meetingKey;
    private String meetingNumber;
    @NotNull
    private String title;
    private String minutesOfMeeting;
    @NotNull
    private Long organizerAccountId;

    private String meetingType;

    private String venue;

    private LocalDateTime startDateTime;         // field changed in Task 2539
    private LocalDateTime endDateTime;          // field changed in Task 2539
    private LocalDateTime actualStartDateTime;   // added in task 2851
    private LocalDateTime actualEndDateTime;     // added in task 2851
    private Boolean isCancelled;              // added in task 2851
    @Size(max = 252, message = ErrorConstant.Meeting.AGENDA_LIMIT)
    private String agenda;  // Changed agenda to  nullable in task 2676

    private Integer reminderTime;
    private List<AttendeeRequest> attendeeRequestList;          // Changed datatype of this field in Task 2539
    @NotNull
    private Long createdAccountId;
    private Long updatedAccountId;
    private Integer duration;
    @NotNull(message = ErrorConstant.Meeting.ORGANISATION_ID)
    private Long orgId;
    private Long buId;
    private Long projectId;
    private Long teamId;
    private Long meetingId;

    // added these 2 fields below in task 2676
    private String referenceEntityNumber;
    private Integer referenceEntityTypeId;

    private List<ActionItem> actionItems;
    private List<String> labelsToAdd;
    private Integer referencedMeetingReasonId;
    private Integer meetingTypeId = MeetingType.MEETING.getValue(); // meeting type is meeting or collaboration

    private Boolean isFetched = false;
}
