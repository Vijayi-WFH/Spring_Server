package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MoveSprintTaskRequest {
    private List<MoveSprintTask> sprintTaskList;
    private Long entityId;
    private Integer entityTypeId;
}
