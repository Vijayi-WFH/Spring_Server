package com.tse.core_application.dto.project;

import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.dto.CustomTeamResponse;
import com.tse.core_application.dto.DeletedTeamReport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {
    private Long projectId;
    private String projectName;
    private String projectDesc;
    private Long orgId;
    private Long buId;
    private Long ownerAccountId;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private List<CustomTeamResponse> activeTeamDetails;
    private List<DeletedTeamReport> deletedTeamDetails;
    private List<CustomAccessDomain> accessDomains;
}
