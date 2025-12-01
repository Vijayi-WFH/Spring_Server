package com.tse.core_application.dto;


import com.tse.core_application.model.Attendee;
import com.tse.core_application.model.MeetingStats;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceMeetingDetail {
    private String meetingKey;
    private String meetingNumber;
    private Long meetingId;
    private MeetingStats meetingProgress;
    private Boolean isCancelled;
    private String title;
    private Long organizerAccountId;
    private String meetingType;
    private String venue;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String agenda;
    private String minutesOfMeeting;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private Long createdAccountId;
    private Long updatedAccountId;
    private Long orgId;
    private Long buId;
    private Long teamId;
    private Long projectId;
    private Integer duration;

    private String referenceEntityNumber;
    private Integer referenceEntityTypeId;
    private List<Attendee> attendeeList;
    private Integer referencedMeetingReasonId;
}
