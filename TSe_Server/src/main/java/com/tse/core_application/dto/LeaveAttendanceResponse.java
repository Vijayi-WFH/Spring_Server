package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LeaveAttendanceResponse {
    private Long accountId;
    private String firstName;
    private String lastName;
    private List<UserLeaveAttendanceResponse> userLeaveAttendanceResponseList;
}
