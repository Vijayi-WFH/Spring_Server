package com.tse.core_application.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ScheduledMeetingsResponse {

    private Long meetingId;
    private String meetingKey; //room name
    private String meetingNumber;
    private String title;
    private Long organizerAccountId;
    private String meetingType;
    private String venue;
    private LocalDateTime startDateTime;
    private Long createdAccountId;
    private Long orgId;
    private Long buId;
    private Long projectId;
    private Long teamId;
}
