package com.tse.core_application.repository;

import java.util.*;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tse.core_application.custom.model.*;
import com.tse.core_application.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query(value = "select new com.tse.core_application.custom.model.CreateDateTimeCommentTaskId (c.createdDateTime, c.comment, c.postedByAccountId, t.taskId) from Comment c inner join c.task t where c.createdDateTime = :createdDateTime")
	List<CreateDateTimeCommentTaskId> getCreateDateTimeCommentTaskIdByCreateDateTime(Date createdDateTime);

	List<CreateDateTime> findCreatedDateTimeByCommentId(Long commentId);

	ArrayList<CreateDateTime> findByCommentIdOrderByCreatedDateTimeDesc(Long commentId);

//	ArrayList<CreateDateTimeComment> findAllTop20ByCommentIdOrderByCreatedDateTimeDesc(Long commentId, Pageable pageable);

	@Query("SELECT new com.tse.core_application.custom.model.CommentPreview(" +
			"c.commentLogId, " +
			"c.createdDateTime, " +
			"c.comment, " +
			"c.postedByAccountId, " +
			"c.commentsTags, " +
			"c.parentCommentLogId, " +
			"c.childCommentLogIds) " +
			"FROM Comment c " +
			"WHERE c.commentId = :commentId " +
			"ORDER BY c.createdDateTime DESC")
	ArrayList<CommentPreview> findAllCommentsByCommentIdOrderByCreatedDateTimeDesc(@Param("commentId") Long commentId);

	@Query("SELECT new com.tse.core_application.custom.model.CommentPreview(" +
			"c.commentLogId, " +
			"c.createdDateTime, " +
			"c.comment, " +
			"c.postedByAccountId, " +
			"c.commentsTags, " +
			"c.parentCommentLogId, " +
			"c.childCommentLogIds) " +
			"FROM Comment c " +
			"WHERE c.commentId in :commentIds " +
			"ORDER BY c.createdDateTime DESC")
	ArrayList<CommentPreview> findAllCommentsCustomResponseByCommentIds(@Param("commentIds") List<Long> commentIds);


	@Query(value = "select new com.tse.core_application.custom.model.CommentId (max(c.commentId)) from Comment c")
	CommentId getMaxCommentId();

//	@Query("select c.comment from Comment c where c.task.taskId = :taskId and :labelToSearch in (SELECT t FROM c.commentsTags t) order by c.createdDateTime desc")
//	List<String> getAllCommentsForTasksByLabel(@Param("taskId") Long taskId, @Param("labelToSearch") String labelToSearch, Pageable pageable);

	@Query(value = "SELECT * FROM tse.comment WHERE task_id = :taskId AND :labelToSearch = ANY(comments_tags) AND NOT :labelToExclude = ANY(comments_tags) order by created_date_time desc", nativeQuery = true)
	List<Comment> getAllCommentsForTasksByLabel(@Param("taskId") Long taskId, @Param("labelToSearch") String labelToSearch, @Param("labelToExclude") String labelToExclude, Pageable pageable);


	@Query("SELECT c FROM Comment c WHERE c.task.taskId = :taskId")
	List<Comment> findCommentsByTaskId(@Param("taskId") Long taskId);

	@Query("SELECT c.commentLogId as commentLogId, c.commentId as commentId, c.comment as comment, c.commentsTags as commentsTags, c.postedByAccountId as postedByAccountId, c.createdDateTime as createdDateTime, c.lastUpdatedDateTime as lastUpdatedDateTime FROM Comment c WHERE c.task.taskId = :taskId")
	List<CommentProjection> findCommentProjectionByTaskId(@Param("taskId") Long taskId);

	@Query("SELECT count(c) FROM Comment c WHERE c.task.fkOrgId.orgId = :orgId")
	Integer findCommentsCountByOrgId(Long orgId);

	Comment findByCommentLogId(Long commentLogId);

	@Modifying
	@Transactional
	@Query("DELETE FROM Comment c WHERE c.task.taskId IN :taskIds")
	void deleteAllByTaskIdIn(List<Long> taskIds);
}
