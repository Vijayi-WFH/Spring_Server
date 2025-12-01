package com.tse.core.dto.leave.Response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MemberLeaveReport {
    private String firstName;
    private String lastName;
    private String email;
    private Long accountId;
    private List<LeaveRemainingResponse> leaveRemainingResponseList;
    private Float yearlyAllocation;
    private Float allocationTillNow;
    private Float rejectedLeaves;
    private Float cancelledLeaves;
    private Float expiredLeaves;


}
