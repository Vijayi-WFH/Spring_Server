package com.tse.core_application.dto.leave.Response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for leave application history/audit trail.
 * Contains old and new values side-by-side for comparison.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApplicationHistoryResponse {

    private Long historyId;
    private Long leaveApplicationId;

    // Action details
    private String actionType; // EDIT or DELETE
    private String reason; // Reason for edit/delete
    private LocalDateTime updatedOn;

    // Who made the change
    private Long updatedByAccountId;
    private String updatedByName;

    // Employee whose leave was modified
    private Long employeeAccountId;
    private String employeeName;

    // Old values (before change)
    private LocalDate oldFromDate;
    private LocalTime oldFromTime;
    private LocalDate oldToDate;
    private LocalTime oldToTime;
    private Short oldLeaveTypeId;
    private String oldLeaveTypeName;
    private Float oldLeaveDays;
    private Boolean oldIsHalfDay;
    private Integer oldHalfDayLeaveType;
    private String oldLeaveReason;
    private String oldAddress;

    // New values (after change) - null for DELETE action
    private LocalDate newFromDate;
    private LocalTime newFromTime;
    private LocalDate newToDate;
    private LocalTime newToTime;
    private Short newLeaveTypeId;
    private String newLeaveTypeName;
    private Float newLeaveDays;
    private Boolean newIsHalfDay;
    private Integer newHalfDayLeaveType;
    private String newLeaveReason;
    private String newAddress;
}
