package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EntityLeaveReportRequest {
    @NotNull(message = ErrorConstant.Sprint.ORG)
    private Long orgId;
    private Integer entityTypeId;
    private Long entityId;
    private List<Long> accountIdList;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean calculateYearlyAllocation = false;
}
