package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MeetingNoteResponse {
    private Long meetingNoteId;
    private String meetingNote;
    private EmailFirstLastAccountIdIsActive postedByAccountIdDetails;
    private EmailFirstLastAccountIdIsActive modifiedByAccountIdDetails;
    private Boolean isImportant;
    private Boolean isDeleted;
    private Long version;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;

}
