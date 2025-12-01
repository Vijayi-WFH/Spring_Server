package com.tse.core_application.dto;

import com.tse.core_application.custom.model.TaskMaster;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TaskByFilterResponse {

    private List<TaskMaster> taskMasterList;
    private Boolean hasPagination;
    private Integer pageSize;
    private Integer pageNumber;
    private Integer totalTasks;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String fromDateType;
    private String toDateType;
}
