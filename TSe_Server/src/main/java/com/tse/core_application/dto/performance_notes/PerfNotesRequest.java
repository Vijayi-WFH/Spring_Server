package com.tse.core_application.dto.performance_notes;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PerfNotesRequest {

    private String perfNote;

    @NotNull(message = ErrorConstant.PerfNotes.POSTED_BY)
    private Long postedByAccountId;

    @NotNull(message = ErrorConstant.PerfNotes.ASSIGNED_TO)
    private Long assignedToAccountId;

    private Boolean isShared;

    private Boolean isPrivate;

    private Boolean isDeleted;

    @NotNull(message = ErrorConstant.PerfNotes.TASK_ID)
    private Long taskId;

    private Integer taskRatingId;

    private Long modifiedByAccountId;

}
