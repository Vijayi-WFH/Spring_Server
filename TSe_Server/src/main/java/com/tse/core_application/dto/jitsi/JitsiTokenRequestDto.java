package com.tse.core_application.dto.jitsi;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JitsiTokenRequestDto {

    private Boolean isOrganiser = false;
//    @NotNull(message = "meetingId is a mandatory field for the Token generation")
    private Long meetingId;
    private Long teamId;
    private Long projectId;
}
