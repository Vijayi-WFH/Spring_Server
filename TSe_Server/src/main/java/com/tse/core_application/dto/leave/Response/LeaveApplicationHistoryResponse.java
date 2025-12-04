package com.tse.core_application.dto.leave.Response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Claude change: PT-14409 - Created response DTO for leave application history report
 * Shows old and new values side by side for audit comparison
 * Used to display the history of consumed leave edits/deletions
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApplicationHistoryResponse {

    // Claude change: History record identifier
    private Long historyId;

    // Claude change: Reference to the original leave application
    private Long leaveApplicationId;

    // Claude change: Action performed - "EDIT" or "DELETE"
    private String actionType;

    // Claude change: Mandatory reason provided for the edit/delete
    private String reason;

    // Claude change: Details of who made the change
    private Long updatedByAccountId;
    private String updatedByName;

    // Claude change: When the change was made
    private LocalDateTime updatedOn;

    // Claude change: Old version of leave (before change)
    private LeaveVersionSnapshot oldVersion;

    // Claude change: New version of leave (after change) - null for DELETE actions
    private LeaveVersionSnapshot newVersion;

    // Claude change: Employee details for the leave
    private Long employeeAccountId;
    private String employeeName;

    /**
     * Claude change: Inner class to hold a snapshot of leave values
     * Used for both old and new versions to show side-by-side comparison
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaveVersionSnapshot {
        private LocalDate fromDate;
        private LocalTime fromTime;
        private LocalDate toDate;
        private LocalTime toTime;
        private Short leaveTypeId;
        private String leaveTypeName;
        private Float leaveDays;
        private Boolean isHalfDay;
        private Integer halfDayLeaveType;
        private String leaveReason;
        private String address;
    }
}
