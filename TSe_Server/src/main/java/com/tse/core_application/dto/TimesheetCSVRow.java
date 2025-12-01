package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class TimesheetCSVRow {
    private Long accountId;
    private Long teamId;
    private Long projectId;
    private Long orgId;
    private String fullName;

    private int totalBurnedEffort;
    private int totalEarnedEffort;
    private int expectedWorkTime;

    private Map<LocalDate, Integer> dateBurnedEffortMap = new HashMap<>();
    private Map<LocalDate, Integer> dateEarnedEffortMap = new HashMap<>();
}

