package com.tse.core_application.dto.leave.Response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LeaveApplicationDashboardResponse {
    private Long leaveApplicationId;
    private Short leaveApplicationStatusId;
    private Long accountId;
    private String leaveType;

    private LocalDate fromDate;
    private LocalTime fromTime;

    private LocalDate toDate;
    private LocalTime toTime;

    private String leaveReason;

    private String approverReason;

    private String approver;
    private Short leaveTypeId;
}
