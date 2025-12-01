package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SprintStatusResponse {
    private String Sprintmessage;
    private List<TaskNumberTaskTitleSprintName> taskList;
    private List<SprintInfo> futureSprintDetails;
    private List<SprintInfo> pastSprintDetails;
}
