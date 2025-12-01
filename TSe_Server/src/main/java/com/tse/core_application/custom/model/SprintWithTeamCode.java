package com.tse.core_application.custom.model;

import com.tse.core_application.model.Sprint;
import lombok.Data;

@Data
public class SprintWithTeamCode {
    Sprint sprint;
    String teamCode;

    public SprintWithTeamCode (Sprint sprint, String teamCode) {
        this.sprint = sprint;
        this.teamCode = teamCode;
    }
}
