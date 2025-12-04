package com.tse.core_application.dto.leave.Request;

import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for fetching leave application history/audit trail.
 * All filters are optional.
 * - Admin can see all history (with optional employee filter)
 * - Employee can only see their own history
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApplicationHistoryRequest {

    // Filter by specific employee (optional - only for admin)
    private Long accountId;

    // Filter by date range (optional)
    private LocalDate fromDate;
    private LocalDate toDate;

    // Filter by specific leave application (optional)
    private Long leaveApplicationId;
}
