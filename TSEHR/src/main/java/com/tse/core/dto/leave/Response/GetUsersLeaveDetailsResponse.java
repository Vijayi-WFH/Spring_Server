package com.tse.core.dto.leave.Response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetUsersLeaveDetailsResponse {
    private List<LeaveApplicationDetails> leaveApplicationDetailsList;
    private Integer numberOfLeaveCount;
}
