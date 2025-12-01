package com.tse.core_application.dto.meeting;

import com.tse.core_application.dto.AttendeeParticipationRequest;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReferenceMeetingResponse {
    private Long meetingId;
    private String meetingNumber;
    private String title;
    private List<AttendeeParticipationRequest> attendeeRequestList;
}
