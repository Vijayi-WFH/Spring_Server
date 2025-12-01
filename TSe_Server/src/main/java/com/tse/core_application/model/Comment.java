package com.tse.core_application.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.utils.LongListConverter;
import lombok.*;
import org.hibernate.annotations.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "comment", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//@TypeDefs(@TypeDef(name = "string-array", typeClass = StringArrayType.class))
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_log_id", nullable = false)
	private Long commentLogId;

	@Column(name = "comment_id", nullable = false)
	private Long commentId;

	@Column(name = "comment", nullable = false, length=20000)
//	@Size(max = 1000, message = ErrorConstant.Task.COMMENT_LIMIT)
	@Convert(converter = DataEncryptionConverter.class)
	private String comment;

	@Column(name = "comments_tags", columnDefinition = "varchar[]")
	@Type(type = "com.tse.core_application.utils.CustomStringArrayUserTypesEntity")
	private String[] commentsTags;

	@Column(name = "posted_by_account_id", nullable = false)
	private Long postedByAccountId;

	@CreationTimestamp
	@Column(name = "created_date_time", nullable = false, updatable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;

	@Column(name = "parent_comment_log_id")
	private Long parentCommentLogId;

	@Column(name = "child_comment_log_ids")
	@Convert(converter = LongListConverter.class)
	private List<Long> childCommentLogIds = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", referencedColumnName = "task_id")
	@JsonBackReference
	private Task task;

	@OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<TaskAttachment> taskAttachments;
}
