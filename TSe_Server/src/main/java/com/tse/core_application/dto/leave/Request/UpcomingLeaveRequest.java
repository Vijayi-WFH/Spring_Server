package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class UpcomingLeaveRequest {
    private Boolean returnLeaves = false;
    private LocalDate fromDate;
    private LocalDate toDate;
    @NotNull(message = ErrorConstant.ENTITY_DETAILS)
    private Integer entityTypeId;
    @NotNull(message = ErrorConstant.ENTITY_DETAILS)
    private Long entityId;
}
