package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class UpdateLeavePolicyForUsersRequest {

    @NotNull(message = ErrorConstant.ENTITY_DETAILS)
    private Integer entityTypeId;
    @NotNull(message = ErrorConstant.ENTITY_DETAILS)
    private Long entityId;
    @NotNull(message = ErrorConstant.Leave.ACCOUNT_ID_ERROR)
    private Long accountId;
    private Float sickLeaves;
    private Float timeOffLeaves;
    private LocalDateTime startLeavePolicyOn;

}
