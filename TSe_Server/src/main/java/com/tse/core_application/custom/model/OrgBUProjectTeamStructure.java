package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBUProjectTeamStructure {

    private Long orgId;
    private String orgName;
    private List<BUProject> bu;

    // this field was used when default bu and project was removed from response
//    private LinkedHashMap<String, Object> organization;
}
