package com.tse.core.dto.leave.Response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllUserPolicyReportResponse {
    private List<UserLeaveReportResponse> userLeaveReportResponseList;
    private Integer totalUserPolicyReport;
}
