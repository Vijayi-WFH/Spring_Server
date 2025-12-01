package com.tse.core.dto.leave.Request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EntityLeaveReportRequest {
    private Long orgId;
    private Integer entityTypeId;
    private Long entityId;
    private List<Long> accountIdList;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean calculateYearlyAllocation = false;
}
