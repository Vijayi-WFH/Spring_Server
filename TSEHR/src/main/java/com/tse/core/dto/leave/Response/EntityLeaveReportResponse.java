package com.tse.core.dto.leave.Response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EntityLeaveReportResponse {
    private List<MemberLeaveReport> memberLeaveReportList;
    private Integer totalLeaveReport;
    private LocalDate fromDate;
    private LocalDate toDate;
}
