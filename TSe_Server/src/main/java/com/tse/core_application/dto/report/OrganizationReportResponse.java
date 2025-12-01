package com.tse.core_application.dto.report;

import com.tse.core_application.dto.report.UserLoginInfo;
import lombok.Data;

import java.util.List;

@Data
public class OrganizationReportResponse {
    private String orgName;
    private String orgOwnerEmail;
    private Integer buCount;
    private Integer projectCount;
    private Integer teamCount;
    private Integer userCount;
    private Integer activeUserCount;
    private Integer inactiveUserCount;
    private Integer epicCount;
    private Integer sprintCount;
    private Integer taskCount;
    private Integer noteCount;
    private Integer commentCount;
    private Integer templateCount;
    private Integer meetingCount;
    private Integer stickyNotesCount;
    private Integer leavesCount;
    private Integer feedbackCount;
    private Long memoryUsed;
    private Long memoryRemaining;
    private Integer deletedProjectCount;
    private Integer deletedTeamCount;
    private List<UserLoginInfo> userLoginInfo;
}
