package com.tse.core_application.dto.jitsi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JitsiParticipantDTO {

        private String id;
        private String name;
        private String email;
        private String userType;

        @JsonProperty("joinTime")
        private Long joinTime;

        @JsonProperty("leaveTime")
        private Long leaveTime;
        private Long duration;
        private Long accountId;
        private Long userId;
        private Long orgId;
        private String senderUserId; // to track the guest users in JitsiMeet who sent the invite.
        private Long meetingId;
        private String isOrganiser;
        private String timezone;
        private Long projectId;
        private Long teamId;
}