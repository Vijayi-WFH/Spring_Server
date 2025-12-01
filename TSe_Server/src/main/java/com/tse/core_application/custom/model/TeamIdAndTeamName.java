package com.tse.core_application.custom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class TeamIdAndTeamName {

    Long teamId;
    String teamName;
    String teamCode;

    Boolean isDeleted = false;

    public TeamIdAndTeamName(Long teamId, Object teamName, String teamCode, Boolean isDeleted) {
        this.teamId = teamId;
        this.teamName = (String) teamName;
        this.teamCode = (String) teamCode;
        this.isDeleted = isDeleted;
    }
}
