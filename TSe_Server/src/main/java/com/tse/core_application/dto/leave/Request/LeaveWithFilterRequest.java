package com.tse.core_application.dto.leave.Request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveWithFilterRequest {
    private Long approverAccountId;
    private List<Short> leaveTypeList;
    private List<Long> accountIdList;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<Short> applicationStatusIds;
    private Long orgId;
    private Long page;
    private Long size;
}
