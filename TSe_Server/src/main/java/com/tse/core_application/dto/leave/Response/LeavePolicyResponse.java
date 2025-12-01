package com.tse.core_application.dto.leave.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeavePolicyResponse {


    private Long leavePolicyId;

    private String leaveType;

    private Long orgId;

    private Long buId;

    private Long projectId;

    private Long teamId;

    private String leavePolicyTitle;

    private Float initialLeaves;

    private Boolean isLeaveCarryForward;

    private Float maxLeaveCarryForward;

    private Long createdByAccountId;

    private Long lastUpdatedByAccountId;

    private LocalDateTime createdDateTime;

    private LocalDateTime modifiedDateTime;

    private Boolean includeNonBusinessDaysInLeave;

    private Float maxNegativeLeaves;

    private Boolean isNegativeLeaveAllowed;

    private Short leaveTypeId;
}
