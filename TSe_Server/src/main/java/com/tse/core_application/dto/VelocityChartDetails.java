package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VelocityChartDetails {
    private LocalDate sprintExpStartDate;
    private LocalDate sprintExpEndDate;
    private long totalEstimate;
    private long totalEarnedTime;
}
