package com.tse.core_application.dto;

public interface HourDistribution {
    int getTotalBurnedEffortMin();
    int getTotalEarnedEffortMin();
    void setPercentageOfBurnedEffort(int percentage);
    void setPercentageOfEarnedEffort(int percentage);
}

