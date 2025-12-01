package com.tse.core_application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StickyNoteUpdateRequest {

    private Long postedByAccountId;
    private Long noteId;
    private String note;
    private Long orgId;
    private Long buId;
    private Long projectId;
    private Long teamId;
    private Integer accessType;
    private List<String> sharedAccountIds;
    private Integer isDeleted;
    private Integer isModified;
}
