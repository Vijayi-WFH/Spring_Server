package com.tse.core_application.dto;

import com.tse.core_application.model.Attendee;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AllMeetingsCondensedResponse {

    private Long recurringMeetingId;
    private Long meetingId;
    private String meetingKey;
    private String meetingNumber;
    private String recurringMeetingNumber;
    private Boolean isCancelled;
    private List<LocalDateTime> recurringMeetingsStartDates;

//    private LocalDateTime startDateTime;
//    private LocalDateTime endDateTime;
    private String title;
    private String meetingType;
    private String venue;
    private String agenda;
    private Integer reminderTime;
    private String entityName;
    private Integer duration;
    private Long organizerAccountId;
    private Long createdAccountId;
    private Long updatedAccountId;
    private Long teamId;
    private Long orgId;

    private List<Attendee> attendeeResponseList;


}
