package com.tse.core_application.dto.leave.Request;

import lombok.*;

import javax.validation.constraints.NotNull;

/**
 * Claude change: PT-14409 - Created DTO for requesting leave application history report
 * This request is used by Org Admin to get the audit trail of consumed leave edits/deletions
 * Can be filtered by employee accountId
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApplicationHistoryRequest {

    // Claude change: Account ID of the Org Admin making the request
    @NotNull(message = "Account ID is required")
    private Long accountId;

    // Claude change: Organization ID for access validation
    @NotNull(message = "Organization ID is required")
    private Long orgId;

    // Claude change: Optional - Employee account ID to filter history by specific employee
    private Long employeeAccountId;

    // Claude change: Optional - Specific leave application ID to get history for
    private Long leaveApplicationId;
}
