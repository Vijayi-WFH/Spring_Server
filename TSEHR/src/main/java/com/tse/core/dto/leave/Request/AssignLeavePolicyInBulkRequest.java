package com.tse.core.dto.leave.Request;

import com.tse.core.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssignLeavePolicyInBulkRequest {

    @NotNull(message = ErrorConstant.Leave.ACCOUNT_ID_ERROR)
    private List<Long> accountIdList;

    @NotNull(message = ErrorConstant.Leave.LEAVE_POLICY_ID)
    private Long leavePolicyId;

    @NotNull(message = ErrorConstant.Leave.ORG_ID_ERROR)
    private Long orgId;

    private Long teamId;
}
