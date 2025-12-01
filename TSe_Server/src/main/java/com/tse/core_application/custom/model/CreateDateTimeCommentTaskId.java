package com.tse.core_application.custom.model;

import java.util.Date;

import lombok.Value;

@Value
public class CreateDateTimeCommentTaskId {

	Date createdDateTime;
	String comment;
	Long postedByAccountId;
	Long taskId;

	public CreateDateTimeCommentTaskId(Date createdDateTime, Object comment, Long postedByAccountId, Long taskId){
		this.createdDateTime = createdDateTime;
		this.comment = (String) comment;
		this.postedByAccountId = postedByAccountId;
		this.taskId = taskId;
	}
	
}
