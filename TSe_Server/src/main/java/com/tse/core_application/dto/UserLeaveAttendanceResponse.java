package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserLeaveAttendanceResponse {
    LocalDate date;
    private Integer leaveTypeId;
    private String leaveTypeName;
    private Boolean isHalfDayLeave;
    private Long leaveApplicationId;
}
