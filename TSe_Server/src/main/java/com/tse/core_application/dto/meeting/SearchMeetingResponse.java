package com.tse.core_application.dto.meeting;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SearchMeetingResponse {
    private Long meetingId;
    private String meetingNumber;
    private String title;
    private Long organizerAccountId;
    private String meetingType;
    private String venue;
    private String meetingKey;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long teamId;
    private Long buId;
    private Long projectId;
    private Long orgId;
    private String entityName;
    List<Long> attendeAccountIdList;
    List<String> labels;
    private Long recurringMeetingId;
    private String recurDays;
    private Integer recurEvery;
}
