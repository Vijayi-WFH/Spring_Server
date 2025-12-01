package com.tse.core_application.dto.report;

import lombok.Data;

@Data
public class UserOrganizationsReport {
    private String orgName;
    private String orgOwnerEmail;
    private Integer buCount;
    private Integer projectCount;
    private Integer teamCount;
    private Integer userCount;
    private Boolean isDisabled;
    private Integer deletedProjectCount;
    private Integer deletedTeamCount;
}
