package com.tse.core_application.dto.performance_notes;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.TaskDetails;
import com.tse.core_application.dto.TaskNumberTaskTitleSprintName;
import com.tse.core_application.model.performance_notes.TaskRating;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerfNotesResponse {
    
    private Long perfNoteId;

    private String perfNote;

    private EmailFirstLastAccountId postedByAccount;

    private EmailFirstLastAccountId assignedToAccount;

    private Boolean isShared;

    private Boolean isPrivate;

    private TaskDetails taskInfo;

    private TaskRating fkTaskRatingId;

    private EmailFirstLastAccountId modifiedByAccount;

    private LocalDateTime createdDateTime;

    private LocalDateTime lastUpdatedDateTime;

    private int version;
}
