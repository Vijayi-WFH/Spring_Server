package com.tse.core.dto.leave.Response;

import lombok.Data;

import java.util.List;

@Data
public class PeopleOnLeaveResponse {
    private Long teamId;
    private String teamName;
    private String projectName;
    private String orgName;
    private Long projectId;
    private Long orgId;
    private Long buId;
    private List<LeaveApplicationDashboardResponse> leaveApplicationDashboardResponse;
}
