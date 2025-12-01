package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSummary {
    private long totalEstimate = 0;
    private long totalEarnedTime = 0;
    private List<Long> taskIds = new ArrayList<>();
}
