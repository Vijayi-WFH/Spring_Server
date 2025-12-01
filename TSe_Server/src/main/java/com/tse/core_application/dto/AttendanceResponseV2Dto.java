package com.tse.core_application.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceResponseV2Dto {

    private Long orgId;
    private Long projectId;
    private Long teamId;
    private List<UserAccountAttendanceDetails> usersDetails;
    private int totalHolidays;
    private int totalOffDays;
}
