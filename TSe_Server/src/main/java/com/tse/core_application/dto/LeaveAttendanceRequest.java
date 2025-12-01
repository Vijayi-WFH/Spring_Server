package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LeaveAttendanceRequest {
    @NotNull(message = ErrorConstant.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.ENTITY_ID)
    private Long entityId;

    @NotNull(message = ErrorConstant.FROM_DATE)
    private LocalDate fromDate;

    @NotNull(message = ErrorConstant.TO_DATE)
    private LocalDate toDate;

    private List<Long> accountIdList;
}
