package com.tse.core_application.repository;

import com.tse.core_application.custom.model.FileMetadata;
import com.tse.core_application.custom.model.TaskAttachmentMetaInfo;
import com.tse.core_application.custom.model.TaskAttachmentMetadata;
import com.tse.core_application.model.TaskAttachment;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long>, JpaSpecificationExecutor<TaskAttachment> {

    @Modifying
    @Query("update TaskAttachment t set t.fileStatus = :fileStatus, t.removerAccountId = :removerAccountId, t.deletedDateTime = CURRENT_TIMESTAMP where t.taskId = :taskId AND t.fileName = :fileName")
    Integer updateTaskAttachmentStatusByTaskIAndFileName(Long taskId, String fileName, Long removerAccountId, Character fileStatus);

    @Modifying
    @Query("update TaskAttachment t set t.fileStatus = :fileStatus, t.removerAccountId = :removerAccountId, t.deletedDateTime = CURRENT_TIMESTAMP where t.taskId = :taskId")
    Integer updateAllTaskAttachmentsStatusByTaskId(Long taskId, Long removerAccountId, Character fileStatus);

    @Modifying
    @Query("update TaskAttachment t set t.fileStatus = :fileStatus, t.removerAccountId = :removerAccountId, t.deletedDateTime = CURRENT_TIMESTAMP where t.taskId = :taskId and t.comment is null")
    Integer updateAllTaskAttachmentsStatusByTaskIdWithoutComment(Long taskId, Long removerAccountId, Character fileStatus);

//    List<FileMetadata> findFileNameByTaskIdAndFileStatus(Long taskId, Character fileStatus);

    @Query("SELECT new com.tse.core_application.custom.model.FileMetadata(t.fileName, t.fileSize, t.comment.commentLogId) " +
            "FROM TaskAttachment t " +
            "WHERE t.taskId = :taskId AND t.fileStatus = :fileStatus")
    List<FileMetadata> findFileMetadataByTaskIdAndFileStatus(Long taskId, Character fileStatus);

    @Query("SELECT NEW com.tse.core_application.custom.model.TaskAttachmentMetadata(t.taskAttachmentId, t.fileName, t.fileType, t.fileSize, t.uploaderAccountId, t.fileStatus, c.commentLogId) FROM TaskAttachment t JOIN t.comment c WHERE c.task.taskId = :taskId")
    List<TaskAttachmentMetadata> findTaskAttachmentMetadataByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT new com.tse.core_application.custom.model.TaskAttachmentMetaInfo(" +
            "t.taskAttachmentId, " +
            "t.fileName, " +
            "t.fileType, " +
            "t.fileSize, " +
            "t.comment.commentLogId) " +
            "FROM TaskAttachment t " +
            "WHERE t.comment.commentLogId IN :commentLogIds")
    List<TaskAttachmentMetaInfo> findAllAttachmentsByCommentLogIds(@Param("commentLogIds") List<Long> commentLogIds);

}
