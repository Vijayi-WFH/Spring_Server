package com.tse.core.dto.leave.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeLeaveStatusRequest {

    @NotNull
    private Long accountId;

    @NotNull
    private Long applicationId;

    @NotNull
    private Short leaveApplicationStatusId;

    private String approverReason;

    @NotNull
    private Long orgId;

    private Long buId;
    private Long projectId;

    @NotNull
    private Long teamId;
    private Boolean updateCapacity= false;
    private Boolean isSprintCapacityAdjustment;
}
