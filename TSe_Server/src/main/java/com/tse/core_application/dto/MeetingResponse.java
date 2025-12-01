package com.tse.core_application.dto;
import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.dto.meeting.ActionItemResponseDto;
import com.tse.core_application.model.ActionItem;
import com.tse.core_application.model.Attendee;
import com.tse.core_application.model.MeetingStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingResponse {
    private String meetingKey;
    private String meetingNumber; // new field added in Task 2539
    private Long meetingId;
    private Long recurringMeetingId;  // added in task 2676
    private MeetingStats meetingProgress;
    private Boolean isCancelled; // added in task 2851
    private String title;   // added new field task 2676
    private Long organizerAccountId;
    private String meetingType;
    private String venue;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String agenda;
    private String minutesOfMeeting;  // added new field task 2676
    private Integer reminderTime;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;  // new field added in Task 2539
    private Long createdAccountId;
    private Long updatedAccountId;
    private Long orgId;  // fields orgId , buId , teamId, projectId added in Task 2539
    private Long buId;
    private Long teamId;
    private Long projectId;
    private String entityName; // will store org, bu, project or team name.
    private Integer duration;
    private Long attendeeId;
    private Boolean isEditable;
    private Boolean isFetched;
    private Boolean showUserPerceivedPercentage = false;
    private Integer referenceTaskUserPerceivedPercentage;

    // added 2 fields below in task 2676
    private String referenceEntityNumber;
    private Integer referenceEntityTypeId;
    private List<Attendee> AttendeeRequestList;   // added this field in Task 2539
    private List<ActionItemResponseDto> actionItems;
    private List<LabelResponse> labels;
    private Integer referencedMeetingReasonId;
    private List<ModelFetchedDto> modelFetchedList;
    private List<MeetingNoteResponse> meetingNoteResponseList;
    private List<UploadFileForModelResponse> uploadFileForModelResponseList;
    private Boolean viewTranscription;

    // added below 2 field in task 9119
    private String recurDays;
    private Integer recurEvery;

    private Boolean canEditMeeting = true;
}
