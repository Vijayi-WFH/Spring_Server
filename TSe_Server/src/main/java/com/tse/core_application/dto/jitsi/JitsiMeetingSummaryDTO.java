package com.tse.core_application.dto.jitsi;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JitsiMeetingSummaryDTO {

    private String room;
    @JsonProperty("startedAt")
    private Long startedAt;
    @JsonProperty("endedAt")
    private Long endedAt;
    private List<JitsiParticipantDTO> participants;
}
