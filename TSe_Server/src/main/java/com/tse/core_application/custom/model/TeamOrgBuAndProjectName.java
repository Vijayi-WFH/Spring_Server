package com.tse.core_application.custom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeamOrgBuAndProjectName {

    Long teamId;
    String teamCode;
    Long orgId;
    Long projectId;
    Long buId;
    // change the type from String to Object because teamName and orgName are now encrypted and the converter returns Object type when we query the database
    Object teamName;
    Object orgName;
    Object projectName;
    Object buName;

    public void setTeamName(Object teamName) {
        this.teamName = teamName;
    }
}
