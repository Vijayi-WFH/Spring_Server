package com.tse.core_application.dto;

import com.tse.core_application.custom.model.SprintTitleAndId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamAndSprintResponse {
    private String teamName;
    private Long teamId;
    private String displayName;
    private String projectName;
    private String orgName;
    private List<SprintTitleAndId> sprintList;
}
