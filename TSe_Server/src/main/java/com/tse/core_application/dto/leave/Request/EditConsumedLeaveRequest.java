package com.tse.core_application.dto.leave.Request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for editing a consumed leave by Org Admin.
 * All editable fields are optional - only provided fields will be updated.
 * Reason is mandatory for audit trail.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditConsumedLeaveRequest {

    @NotNull(message = "Leave application ID is required")
    private Long leaveApplicationId;

    // Editable fields - optional (only update if provided)
    private LocalDate fromDate;
    private LocalTime fromTime;
    private LocalDate toDate;
    private LocalTime toTime;
    private Short leaveTypeId;
    private Boolean isHalfDay;
    private Integer halfDayLeaveType;
    private Float numberOfLeaveDays;

    @Size(max = 1000, message = "Leave reason must not exceed 1000 characters")
    private String leaveReason;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    // Mandatory reason for editing consumed leave
    @NotBlank(message = "Reason for edit is mandatory")
    @Size(min = 3, max = 2000, message = "Reason must be between 3 and 2000 characters")
    private String reason;
}
