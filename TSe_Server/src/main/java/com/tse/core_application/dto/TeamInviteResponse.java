package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInviteResponse {
    private String inviteId;
    private String primaryEmail;
    private String firstName;
    private String middleName;
    private String lastName;
    private String teamName;
}
