package com.tse.core_application.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SprintInfoWithSprintStatusResponse {
    private List<SprintInfo> activeSprintList = new ArrayList<>();
    private List<SprintInfo> plannedSprintList = new ArrayList<>();
    private List<SprintInfo> completedSprintList = new ArrayList<>();
}
