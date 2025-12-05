package com.tse.core_application.repository;

import com.tse.core_application.custom.model.AttachmentProjection;
import com.tse.core_application.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
//    List<AttachmentProjection> findByGroupConversation_IdIn(List<Long> groupConversationIds);

    @Query("SELECT a.attachmentId as attachmentId, a.fileName as fileName, a.fileType as fileType, a.fileSize as fileSize, a.groupConversation.groupConversationId as groupConversationId FROM Attachment a WHERE a.groupConversation.groupConversationId IN :ids")
    List<AttachmentProjection> findByGroupConversationIds(@Param("ids") List<Long> groupConversationIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attachment a WHERE a.groupConversation.groupConversationId IN :groupConversationIds")
    void deleteByGroupConversationIdIn(List<Long> groupConversationIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attachment a WHERE a.groupConversation.groupConversationId IN (SELECT gc.groupConversationId FROM GroupConversation gc WHERE gc.entityTypeId = :entityTypeId AND gc.entityId IN :entityIds)")
    void deleteByEntityTypeIdAndEntityIdIn(@Param("entityTypeId") Integer entityTypeId, @Param("entityIds") List<Long> entityIds);
}
