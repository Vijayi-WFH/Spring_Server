package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentTaskIdTaskTitleCommentFrom {

    Long taskId;
    String taskNumber;
    Long orgId;
    Long teamId;
    String teamCode;
    String taskTitle;
    String comment;
    LocalDateTime createdDateTime;
    String lastCommentFromEmail;
    String lastCommentFromFullName;

    public CommentTaskIdTaskTitleCommentFrom(Long taskId, String taskNumber, Long orgId, Long teamId, String teamCode, Object taskTitle, Object comment, LocalDateTime createdDateTime, Object lastCommentFromEmail, Object lastCommentFromFullName) {
        this.taskId = taskId;
        this.taskNumber = taskNumber;
        this.orgId = orgId;
        this.teamId = teamId;
        this.teamCode = teamCode;
        this.taskTitle = (String) taskTitle;
        this.comment = (String) comment;
        this.createdDateTime = createdDateTime;
        this.lastCommentFromEmail = (String) lastCommentFromEmail;
        this.lastCommentFromFullName = (String) lastCommentFromFullName;
    }
}
