package com.tse.core_application.dto.personal_task;

import com.tse.core_application.validators.CleanedSize;
import lombok.Data;

@Data
public class NoteRequest {

    @CleanedSize(value = 500, message = "Note must not exceed 500 characters")
    private String note;

    private Long noteId;

    private Boolean isDeleted;

    private Boolean isUpdated;
}
