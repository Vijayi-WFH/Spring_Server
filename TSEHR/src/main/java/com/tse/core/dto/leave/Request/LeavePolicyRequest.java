package com.tse.core.dto.leave.Request;

import com.tse.core.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeavePolicyRequest {

    @NotNull(message = ErrorConstant.Leave.ACCOUNT_ID_ERROR)
    private Long accountId;

    @NotNull(message = ErrorConstant.Leave.LEAVE_TYPE_ID_ERROR)
    private Short leaveTypeId;

    @NotNull(message = ErrorConstant.Leave.ORG_ID_ERROR)
    private Long orgId;

    private Long buId;

    private Long projectId;

    private Long teamId;

    private String leavePolicyTitle;

    @NotNull(message = ErrorConstant.Leave.INITIAL_LEAVE_ERROR)
    private Float initialLeaves;

    @NotNull(message = ErrorConstant.Leave.IS_LEAVE_CARRY_FORWARD_ERROR)
    private Boolean isLeaveCarryForward;

    private Float maxLeaveCarryForward;

    @NotNull
    private Boolean includeNonBusinessDaysInLeave;

    private Float maxNegativeLeaves;

    private Boolean isNegativeLeaveAllowed;


}
