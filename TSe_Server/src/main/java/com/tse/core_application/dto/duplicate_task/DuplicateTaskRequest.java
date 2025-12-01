package com.tse.core_application.dto.duplicate_task;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class DuplicateTaskRequest {

    @NotNull
    private String taskNumber;

    @NotNull
    private Long teamId;

    private Boolean isChild;

    private String parentTaskNumber;

    private Boolean isParent;

    private Boolean copyChildTasks;

    private Boolean copyParentChildDates;

}
