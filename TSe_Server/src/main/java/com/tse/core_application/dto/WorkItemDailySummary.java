package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkItemDailySummary {
    private LocalDate date;
    private int workItemRemaining;
    private int workItemNotStarted;
    private int workItemCompleted;
}
