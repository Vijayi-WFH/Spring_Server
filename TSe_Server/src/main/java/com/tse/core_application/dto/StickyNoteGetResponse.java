package com.tse.core_application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StickyNoteGetResponse {

    private Long noteId;
    private String note;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;

    /**
     * This indicator is for front end.
     */
    private Boolean isEditable;
}
