package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TotalEffortByDate {

    private Integer totalEffortMins;

    private Integer totalEarnedTime;

    private LocalDate totalEffortDate;
}
