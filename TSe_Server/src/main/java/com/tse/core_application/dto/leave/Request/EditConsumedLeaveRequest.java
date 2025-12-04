package com.tse.core_application.dto.leave.Request;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Claude change: PT-14409 - Created DTO for editing consumed leaves
 * This request is used by Org Admin to edit consumed leave details
 * Reason field is mandatory to maintain audit trail
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditConsumedLeaveRequest {

    // Claude change: ID of the consumed leave to edit
    @NotNull(message = "Leave application ID is required")
    private Long leaveApplicationId;

    // Claude change: Account ID of the Org Admin making the change
    @NotNull(message = "Account ID is required")
    private Long accountId;

    // Claude change: New leave dates (optional - only include if changing)
    private LocalDate fromDate;
    private LocalTime fromTime;
    private LocalDate toDate;
    private LocalTime toTime;

    // Claude change: New leave type (optional - only include if changing)
    private Short leaveTypeId;

    // Claude change: New leave reason (optional - only include if changing)
    @Size(max = 1000, message = "Leave reason cannot exceed 1000 characters")
    private String leaveReason;

    // Claude change: New address (optional - only include if changing)
    @Size(max = 1000, message = "Address cannot exceed 1000 characters")
    private String address;

    // Claude change: Half day leave settings
    private Boolean isHalfDay;
    private Integer halfDayLeaveType;

    // Claude change: Mandatory reason for editing the consumed leave (for audit)
    @NotNull(message = "Reason for edit is required")
    @Size(min = 3, max = 2000, message = "Reason must be between 3 and 2000 characters")
    private String reason;
}
