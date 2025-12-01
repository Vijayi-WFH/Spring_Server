package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomTeamResponse {
    private Long teamId;
    private String teamName;
    private String teamDesc;
    private Long parentTeamId;
    private Boolean isDeleted = false;
    private String teamCode;
}
