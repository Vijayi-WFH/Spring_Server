package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingProgress {
    private Long meetingId;
    private Integer meetingDuration;//this is meeting duration earned by the attendee
    private Integer attendeeDuration;
}
