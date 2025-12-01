package com.tse.core_application.dto;

import lombok.Data;

import java.util.List;

@Data
public class SprintTasksWithoutEstimateResponse {
    List<SprintTaskResponse> TaskList;
}
