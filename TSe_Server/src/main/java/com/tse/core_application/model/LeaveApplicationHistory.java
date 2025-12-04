package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Claude change: PT-14409 - Created LeaveApplicationHistory entity for audit trail
 * This entity stores the history of consumed leave edits and deletions.
 * It captures both old and new values for transparency and audit reporting.
 * Org Admin can edit/delete consumed leaves with mandatory reason, and all changes are tracked here.
 */
@Entity
@Table(name = "leave_application_history", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaveApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    // Claude change: Reference to the original leave application being modified
    @Column(name = "leave_application_id", nullable = false)
    private Long leaveApplicationId;

    // Claude change: Account ID of the Org Admin who made the change
    @Column(name = "updated_by_account_id", nullable = false)
    private Long updatedByAccountId;

    // Claude change: Action type - "EDIT" or "DELETE"
    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    // Claude change: Mandatory reason for edit/delete operation
    @Column(name = "reason", nullable = false, length = 2000)
    @Convert(converter = DataEncryptionConverter.class)
    private String reason;

    // Claude change: Old values (snapshot before change) - used for audit comparison
    @Column(name = "old_from_date")
    private LocalDate oldFromDate;

    @Column(name = "old_from_time")
    private LocalTime oldFromTime;

    @Column(name = "old_to_date")
    private LocalDate oldToDate;

    @Column(name = "old_to_time")
    private LocalTime oldToTime;

    @Column(name = "old_leave_type_id")
    private Short oldLeaveTypeId;

    @Column(name = "old_leave_days")
    private Float oldLeaveDays;

    @Column(name = "old_is_half_day")
    private Boolean oldIsHalfDay;

    @Column(name = "old_half_day_leave_type")
    private Integer oldHalfDayLeaveType;

    @Column(name = "old_leave_reason", length = 4000)
    @Convert(converter = DataEncryptionConverter.class)
    private String oldLeaveReason;

    @Column(name = "old_address", length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String oldAddress;

    // Claude change: New values (snapshot after change) - null for DELETE actions
    @Column(name = "new_from_date")
    private LocalDate newFromDate;

    @Column(name = "new_from_time")
    private LocalTime newFromTime;

    @Column(name = "new_to_date")
    private LocalDate newToDate;

    @Column(name = "new_to_time")
    private LocalTime newToTime;

    @Column(name = "new_leave_type_id")
    private Short newLeaveTypeId;

    @Column(name = "new_leave_days")
    private Float newLeaveDays;

    @Column(name = "new_is_half_day")
    private Boolean newIsHalfDay;

    @Column(name = "new_half_day_leave_type")
    private Integer newHalfDayLeaveType;

    @Column(name = "new_leave_reason", length = 4000)
    @Convert(converter = DataEncryptionConverter.class)
    private String newLeaveReason;

    @Column(name = "new_address", length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String newAddress;

    // Claude change: Timestamp when the change was made
    @CreationTimestamp
    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedOn;
}
