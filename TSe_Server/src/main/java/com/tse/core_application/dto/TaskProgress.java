package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class TaskProgress {
    private Long taskId;
    private String taskNumber;
    private Integer taskEstimate;
    private Integer earnedTimeTask;
}
