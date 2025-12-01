package com.tse.core_application.dto.leave.Response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRemainingResponse {

    private Long accountId;

    private Long leavePolicyId;

    private Short leaveTypeId;

    private Float leaveRemaining;

    private Float leaveTaken;

    @JsonIgnore
    private Short calenderYear;

    private Float plannedLeaves;

    private Float ConsumedLeaves;

    private Float pendingLeaves;

    @JsonIgnore
    private LocalDateTime startLeavePolicyOn;
    @JsonIgnore
    private LocalDate fromDate;
    @JsonIgnore
    private LocalDate toDate;
    private String leaveTypeName;

    private Float rejectedLeaves;
    private Float cancelledLeaves;
    private Float expiredLeaves;
}
