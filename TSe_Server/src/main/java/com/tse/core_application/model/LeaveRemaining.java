package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_remaining", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRemaining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_remaining_id", nullable = false, unique = true)
    private Long leaveRemainingId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "currently_active", nullable = false)
    private Boolean currentlyActive;

    @Column(name = "leave_policy_id", nullable = false)
    private Long leavePolicyId;

    @Column(name = "leave_type_id",nullable = false)
    private Short leaveTypeId;

    @Column(name = "leave_remaining",precision = 4,scale = 2, nullable = false)
    private Float leaveRemaining;

    @Column(name = "leave_taken",precision = 4,scale = 2, nullable = false)
    private Float leaveTaken;

    @Column(name = "calender_year", nullable = false)
    private Short calenderYear;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time")
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "start_leave_policy_on")
    private LocalDateTime startLeavePolicyOn = LocalDateTime.now();

    @Column(name = "start_leave_policy_used_once")
    private Boolean startLeavePolicyUsedOnce = false;
}
