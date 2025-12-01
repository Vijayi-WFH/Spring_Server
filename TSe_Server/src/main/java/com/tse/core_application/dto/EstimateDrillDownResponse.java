package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class EstimateDrillDownResponse {
    private Integer totalParentTaskEstimate;
    private Map<String, Integer> currentChildTasks;
    private Map<String, Map<String, Integer>> deletedChildTasks;
}
