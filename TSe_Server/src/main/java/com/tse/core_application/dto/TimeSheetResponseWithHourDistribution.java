package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSheetResponseWithHourDistribution {
    private List<TimeSheetResponse> timeSheetResponsesList;
    private List<EntityTypeHourDistribution> entityTypeHourDistributionsList;
    private List<OrgHourDistribution> orgHourDistributionList;
}
