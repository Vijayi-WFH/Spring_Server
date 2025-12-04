package com.tse.core_application.dto.leave.Request;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Claude change: PT-14409 - Created DTO for deleting consumed leaves
 * This request is used by Org Admin to soft delete consumed leaves
 * Reason field is mandatory to maintain audit trail
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteConsumedLeaveRequest {

    // Claude change: ID of the consumed leave to delete
    @NotNull(message = "Leave application ID is required")
    private Long leaveApplicationId;

    // Claude change: Account ID of the Org Admin making the deletion
    @NotNull(message = "Account ID is required")
    private Long accountId;

    // Claude change: Mandatory reason for deleting the consumed leave (for audit)
    @NotNull(message = "Reason for deletion is required")
    @Size(min = 3, max = 2000, message = "Reason must be between 3 and 2000 characters")
    private String reason;
}
