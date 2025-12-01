package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TimesheetExportToCSVRequest {
    private List<Long> accountIdList;
    @NotNull(message = ErrorConstant.ENTITY_TYPE_ID)
    private Integer entityTypeId;
    @NotNull(message = ErrorConstant.ENTITY_ID)
    private Long entityId;
    @NotNull(message = ErrorConstant.FROM_DATE)
    private LocalDate fromDate;
    @NotNull(message = ErrorConstant.TO_DATE)
    private LocalDate toDate;
    private Boolean fileAtTeamLevel;
    private Boolean fileAtDateLevel;

    private Boolean getLoggedEffort;   // default: include if null
    private Boolean getEarnedEffort;   // default: include if null
}
