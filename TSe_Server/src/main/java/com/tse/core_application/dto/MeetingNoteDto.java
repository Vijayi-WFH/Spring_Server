package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.validators.annotations.TrimmedSize;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class MeetingNoteDto {
    private Long meetingNoteId;
    @NotNull
    @TrimmedSize(min = 3, max = 1000, message = ErrorConstant.Meeting.MEETING_NOTE_LENGTH)
    private String meetingNote;
    @NotNull
    private Boolean isImportant = false;
    @NotNull
    private Boolean isDeleted = false;
    @NotNull
    private Boolean isUpdated = false;

}
