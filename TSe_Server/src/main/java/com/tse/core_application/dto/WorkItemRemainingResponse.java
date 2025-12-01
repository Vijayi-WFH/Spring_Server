package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemRemainingResponse {
    private List<WorkItemDailySummary> dailySummaries;}
