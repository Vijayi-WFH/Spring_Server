package com.tse.core_application.dto.jitsi;

import com.tse.core_application.dto.meeting.ScheduledMeetingsResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ActiveUpcomingMeetingsResponse {

    private List<JitsiActiveMeetingResponse> ongoingMeetings;
    private List<ScheduledMeetingsResponse> upcomingMeetings;

}
