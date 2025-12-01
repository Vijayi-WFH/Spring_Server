package com.tse.core_application.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class RelatedCommentInfo {
    private Long commentLogId;
    private String comment;
    private String[] commentsTags;
    private Long postedByAccountId;
    private String lastCommentFromEmail;
    private String lastCommentFromFullName;
    private String attachmentName;
    private Timestamp createdDateTime;
}
