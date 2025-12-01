package com.tse.core_application.dto;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.ProjectIdProjectName;
import com.tse.core_application.custom.model.TeamIdAndTeamName;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;


@Getter
@Setter
public class AlertResponse {
    private Long alertId;
    private String alertTitle;
    private String alertReason;
    private String alertStatus;
    private String alertType;
    private String associatedTaskNumber;
    private Long associatedTaskId;
    private EmailFirstLastAccountId senderDetails;
    private EmailFirstLastAccountId receiverDetails;
    private TeamIdAndTeamName teamDetails;
    private ProjectIdProjectName projectDetails;
    private OrgIdOrgName orgDetails;
    private LocalDateTime createdDateTime;
}
