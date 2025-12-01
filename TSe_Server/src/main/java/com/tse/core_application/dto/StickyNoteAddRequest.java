package com.tse.core_application.dto;

import com.tse.core_application.validators.CleanedSize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StickyNoteAddRequest {

    private Long noteId;

    /**
     * 1. null, if no org is chosen for that note.
     * 2. accountId in that org , if an org has been chosen for the note.
     * Ex. If user is in orgId 1 and accountId 1 is here, and if, user has changed
     * the orgId to 2 for the note then accountId in orgId will be used here.
     */
    private Long postedByAccountId;

    @CleanedSize(value = 5000, message = "Note must not exceed 5000 characters")
    private String note;

    private Long orgId;

    private Long buId;

    private Long projectId;

    private Long teamId;

    /**
     * Whenever a note is getting shared/public then
     * this accessType has to be changed on frontend.
     * i.e. 0 -> private, 1 -> public.
     */
    private Integer accessType;

    private List<String> sharedAccountIds;

    /**
     * This is an indicator which will be used
     * while updating a note.
     */
    private Integer isModified;

    /**
     * This is an indicator which will be used
     * while updating a note.
     */
    private Integer isDeleted;


    /** if the note is shared, then this indicates whether the sticky note is updatable by other users or not*/
    private Boolean shareEditAllowed = false;

//    /** indicates whether a sticky note should be marked as pinned for a user */
//    private Boolean isPinned;
//
//    /** indicates whether a sticky note should be marked as important for a user*/
//    private Boolean isImportant;

}
