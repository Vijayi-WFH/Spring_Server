package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SprintProgress {
    private Long sprintId;
    private Integer totalSprintHours;
    private Integer consumedSprintHours;
}
