package com.tse.core.dto;

import com.tse.core.custom.model.TotalEffort;
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
    private Integer estimatedEntityTimeAchieved;

    private Long account_Id;

    @Nullable
    private List<Integer> offDays;

    @Nullable
    private List<TotalEffort> efforts;

}
