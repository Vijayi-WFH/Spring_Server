package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AllMeetingsExpandedResponse {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private List<MeetingResponse> meetingResponses;

}
