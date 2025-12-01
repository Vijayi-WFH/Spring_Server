package com.tse.core.dto.leave.Response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AllLeavesByFilterResponse {
    private List<LeaveApplicationResponse> userLeaveReportResponseList;
    private Integer totalLeavesByFilter;
    private LocalDate fromDate;
    private LocalDate toDate;
}