package com.tse.core_application.dto.jitsi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JitsiGuestTokenRequest {

    private String firstName;
    private String lastName;
    private String email;
    private Long expirationTime;
    private String roomName;
}
