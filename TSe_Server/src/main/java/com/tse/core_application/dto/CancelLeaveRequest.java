package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CancelLeaveRequest {
    @NotNull(message = ErrorConstant.Leave.APPLICATION_ID)
    private Long leaveApplicationId;
    private Boolean updateCapacity= false;

    @NotNull(message = ErrorConstant.Leave.CANCELLATION_REASON)
    @Size(min = 3, max =70, message = ErrorConstant.Leave.CANCELLATION_REASON_LENGTH)
    private String leaveCancellationReason;
}
