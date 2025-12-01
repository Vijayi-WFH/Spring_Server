package com.tse.core.custom.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamDetails {
    private Long teamId;
    private String teamName;

    private Long projectId;

    private String projectName;

    private Long buId;

    private Long orgId;

    private String orgName;

    public TeamDetails(Long teamId, Object teamName, Long projectId, Object projectName, Long buId, Long orgId, Object orgName) {
        this.teamId = teamId;
        this.teamName = (String) teamName;
        this.projectId = projectId;
        this.projectName = (String) projectName;
        this.buId = buId;
        this.orgId = orgId;
        this.orgName = (String) orgName;
    }
}
