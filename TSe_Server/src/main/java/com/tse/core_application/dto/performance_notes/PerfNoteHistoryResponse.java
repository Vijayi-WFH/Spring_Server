package com.tse.core_application.dto.performance_notes;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.TaskNumberTaskTitleSprintName;
import com.tse.core_application.model.performance_notes.TaskRating;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerfNoteHistoryResponse {
    private Long perfNoteId;

    private String perfNote;

    private Boolean isShared;

    private Boolean isPrivate;

    private TaskRating fkTaskRatingId;

    private EmailFirstLastAccountId modifiedByAccount;

    private LocalDateTime createdDateTime;

    private int version;
}
