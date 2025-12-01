package com.tse.core_application.custom.model;

import com.tse.core_application.dto.RelatedCommentInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    Long commentLogId;
    String comment;
    String[] commentsTags;
    Long postedByAccountId;
    String commentFromEmail;
    String commentFromName;
    List<TaskAttachmentMetaInfo> taskAttachmentMetaInfoList;
//    List<RelatedCommentInfo> replyCommentInfoList;
    List<CommentResponse> replyCommentInfoList = new ArrayList<>();
    RelatedCommentInfo parentCommentInfo;
    LocalDateTime createdDateTime;

    public CommentResponse(LocalDateTime createdDateTime, Object comment, Object commentFromEmail, Object commentFromName, String[] commentsTags){
        this.createdDateTime = createdDateTime;
        this.comment = (String) comment;
        this.commentFromEmail = (String) commentFromEmail;
        this.commentFromName = (String) commentFromName;
        this.commentsTags = commentsTags;
    }
}
