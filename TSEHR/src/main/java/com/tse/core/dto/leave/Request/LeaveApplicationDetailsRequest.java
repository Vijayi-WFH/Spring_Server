package com.tse.core.dto.leave.Request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LeaveApplicationDetailsRequest {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long orgId;
    private Boolean sortOnName;
    private List<Long> approverAccountIdList;
    private List<Short> leaveStatusIdList;
    private List<Long> accountIdList;      // Frontend doesn’t have to send this

}
