package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityTypeHourDistribution implements HourDistribution {
    private String entityName;
    private Integer totalBurnedEffortMin = 0;
    private Integer totalEarnedEffortMin = 0;
    private Integer percentageOfBurnedEffort = 0;
    private Integer percentageOfEarnedEffort = 0;

    @Override
    public int getTotalBurnedEffortMin() {
        return totalBurnedEffortMin != null ? totalBurnedEffortMin : 0;
    }

    @Override
    public int getTotalEarnedEffortMin() {
        return totalEarnedEffortMin != null ? totalEarnedEffortMin : 0;
    }

    @Override
    public void setPercentageOfBurnedEffort(int percentage) {
        this.percentageOfBurnedEffort = percentage;
    }

    @Override
    public void setPercentageOfEarnedEffort(int percentage) {
        this.percentageOfEarnedEffort = percentage;
    }
}

