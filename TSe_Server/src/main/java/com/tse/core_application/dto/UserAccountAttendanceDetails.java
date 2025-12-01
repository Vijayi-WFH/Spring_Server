package com.tse.core_application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAccountAttendanceDetails {
    private String email;
    private Long accountId;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private List<AttendanceRecordDto> attendanceRecord;
}
