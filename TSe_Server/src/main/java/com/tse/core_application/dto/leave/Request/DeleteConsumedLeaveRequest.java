package com.tse.core_application.dto.leave.Request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Request DTO for deleting a consumed leave by Org Admin.
 * Reason is mandatory for audit trail.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteConsumedLeaveRequest {

    @NotNull(message = "Leave application ID is required")
    private Long leaveApplicationId;

    // Mandatory reason for deleting consumed leave
    @NotBlank(message = "Reason for deletion is mandatory")
    @Size(min = 3, max = 2000, message = "Reason must be between 3 and 2000 characters")
    private String reason;
}
