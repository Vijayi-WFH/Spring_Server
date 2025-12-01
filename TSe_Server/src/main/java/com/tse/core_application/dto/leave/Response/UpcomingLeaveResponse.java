package com.tse.core_application.dto.leave.Response;

import lombok.Data;

import java.util.List;

@Data
public class UpcomingLeaveResponse {
    private Float upcomingLeavesCount;
    private List<LeaveApplicationResponse> upcomingLeaves;
}
