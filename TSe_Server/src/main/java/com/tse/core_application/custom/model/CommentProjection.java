package com.tse.core_application.custom.model;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

public interface CommentProjection {
    Long getCommentLogId();
    Long getCommentId();
    String getComment();
    String[] getCommentsTags();
    Long getPostedByAccountId();
    Timestamp getCreatedDateTime();
    Timestamp getLastUpdatedDateTime();
    List<TaskAttachmentMetadata> getTaskAttachments();
}
