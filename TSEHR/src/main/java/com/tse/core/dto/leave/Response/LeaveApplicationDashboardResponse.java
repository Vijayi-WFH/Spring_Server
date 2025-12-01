package com.tse.core.dto.leave.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
