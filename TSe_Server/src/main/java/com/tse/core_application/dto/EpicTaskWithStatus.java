package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EpicTaskWithStatus {
    private String status;
    private List<EpicTaskResponse> epicTaskResponseList = new ArrayList<>();
    Integer taskCount = 0;
    public void incrementTaskCount () {
        this.taskCount++;
    }
}
