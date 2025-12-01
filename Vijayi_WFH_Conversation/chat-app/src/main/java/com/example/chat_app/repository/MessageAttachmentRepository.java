package com.example.chat_app.repository;

import com.example.chat_app.dto.FileMetadata;
import com.example.chat_app.model.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long>, JpaSpecificationExecutor<MessageAttachment> {

    @Query("SELECT new com.example.chat_app.dto.FileMetadata(m.messageAttachmentId, m.fileName, m.fileSize) " +
            "FROM MessageAttachment m " +
            "WHERE m.messageId = :messageId AND m.fileStatus = :fileStatus")
    List<FileMetadata> findFileMetadataByMessageIdAndFileStatus(Long messageId, Character fileStatus);

    @Modifying
    @Transactional
    @Query("update MessageAttachment m set m.fileStatus = :fileStatus, m.deletedDateTime = CURRENT_TIMESTAMP where m.messageId = :messageId")
    Integer updateAllMessageAttachmentsStatusByTaskId(Long messageId, Character fileStatus);

    @Modifying
    @Transactional
    @Query("update MessageAttachment m set m.fileStatus = :fileStatus, m.deletedDateTime = CURRENT_TIMESTAMP where m.messageId = :messageId AND m.messageAttachmentId = :messageAttachmentId")
    Integer updateMessageAttachmentStatusByMessageIdAndFileName(Long messageId, Long messageAttachmentId, Character fileStatus);

    Optional<MessageAttachment> findByMessageAttachmentId(Long attachmentId);

    @Transactional
    @Modifying
    @Query(value = "Update MessageAttachment ma set ma.messageId =:messageId where ma.messageAttachmentId IN :attachmentIdList ")
    Integer updateMessageIdByMessageAttachmentIdIn(@Param("attachmentIdList") List<Long> messageAttachmentIdList,
                                                   @Param("messageId") Long messageId);

    @Query("SELECT new com.example.chat_app.dto.FileMetadata(m.messageAttachmentId, m.fileName, m.fileSize) " +
            "FROM MessageAttachment m " +
            "WHERE m.messageAttachmentId IN :messageAttachmentId AND m.fileStatus =:fileStatus ")
    Optional<List<FileMetadata>> findMetadataFromAttachmentIdIn(List<Long> messageAttachmentId, Character fileStatus);
}