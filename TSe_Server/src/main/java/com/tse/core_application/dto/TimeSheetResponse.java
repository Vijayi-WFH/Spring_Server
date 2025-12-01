package com.tse.core_application.dto;

import com.tse.core_application.custom.model.timesheet.TotalEffort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSheetResponse {

    private Integer maxDailyWorkingHrs;

    private Integer dailyExpectedWorkingMinutes;

    @Nullable
    private Integer estimatedEntityTime;

    private Long account_Id;

    private List<Integer> offDays;

    @Nullable
    private List<TotalEffort> efforts;

}


