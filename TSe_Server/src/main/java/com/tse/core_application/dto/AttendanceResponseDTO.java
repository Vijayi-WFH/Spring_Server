package com.tse.core_application.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AttendanceResponseDTO {
    private Long orgId;
    private Long projectId;
    private Long teamId;
    private List<Long> accountIds;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalHolidays;
    private int totalOffDays;
    private Map<LocalDate, Map<Long, AttendanceDataDTO>> attendance;
}
