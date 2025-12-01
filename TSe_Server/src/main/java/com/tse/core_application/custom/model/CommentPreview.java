package com.tse.core_application.custom.model;

import java.util.Date;
import java.util.List;

import com.tse.core_application.dto.RelatedCommentInfo;
import lombok.Data;

@Data
public class CommentPreview {

	Long commentLogId;
	Date createdDateTime;
	String comment;
	Long postedByAccountId;
	String[] commentsTags;
	Long parentCommentLogId;
	List<Long> childCommentLogIds;
	List<TaskAttachmentMetaInfo> taskAttachmentMetaInfoList;


	public CommentPreview(Long commentLogId, Date createdDateTime, Object comment, Long postedByAccountId, String[] commentsTags, Long parentCommentLogId, List<Long> childCommentLogIds) {
		this.commentLogId = commentLogId;
		this.createdDateTime = createdDateTime;
		this.comment = (String) comment;
		this.postedByAccountId = postedByAccountId;
		this.commentsTags = commentsTags;
		this.taskAttachmentMetaInfoList = null;
		this.parentCommentLogId = parentCommentLogId;
		this.childCommentLogIds = childCommentLogIds;
	}
}
