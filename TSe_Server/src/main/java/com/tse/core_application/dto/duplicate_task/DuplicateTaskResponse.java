package com.tse.core_application.dto.duplicate_task;

import com.tse.core_application.dto.duplicate_task.DuplicateTask;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DuplicateTaskResponse {
    private String message;
    private DuplicateTask duplicateTask;
    private String parentTaskNumber;
    private Long parentTaskIdentifier;
}
