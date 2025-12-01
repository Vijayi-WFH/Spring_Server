package com.tse.core_application.dto.jitsi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JitsiActiveMeetingResponse {

    private Long startedAt;
    private Long elapsedTime;
    private Long orgId;
    private List<JitsiParticipantDTO> participants;
    private String room;
    private String meetingId;
    private Long organiserId;
}