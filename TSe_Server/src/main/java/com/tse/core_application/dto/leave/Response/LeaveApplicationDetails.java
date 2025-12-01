package com.tse.core_application.dto.leave.Response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class LeaveApplicationDetails {
    private Long leaveApplicationId;
    private Long accountId;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
    private Short leaveTypeId;
    private String leaveTypeAlias;
    private Short leaveApplicationStatusId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean includeLunchTime;
    private String leaveReason;
    private String approverReason;
    private Long approverAccountId;
    private String approverFirstName;
    private String approverLastName;
    private String approverEmail;
    private String phone;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
    private Boolean isLeaveForHalfDay;
    private Float numberOfLeaveDays;
    private String leaveCancellationReason;
    private LocalDate date;
    private Boolean isAttachmentPresent;
}
