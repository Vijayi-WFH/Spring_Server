package com.tse.core_application.model;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class ReferenceWorkItem {
    @Nullable
    private String taskNumber;

    @Nullable
    private Long taskId;

    @Nullable
    private String taskTitle;

    private Integer userPerceivedPercentageTaskCompleted;
}
