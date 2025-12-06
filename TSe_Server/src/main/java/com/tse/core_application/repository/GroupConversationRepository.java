package com.tse.core_application.repository;

import com.tse.core_application.model.GroupConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GroupConversationRepository extends JpaRepository<GroupConversation, Long> {

    List<GroupConversation> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId, Pageable pageable);

    GroupConversation findFirstByEntityIdAndEntityTypeIdOrderByCreatedDateTimeDesc(Long entityId, Integer entityTypeId);

    List<GroupConversation> findByGroupConversationIdIn(List<Long> gcIds);

    @Query("SELECT gc FROM GroupConversation gc WHERE gc.entityTypeId = :entityTypeId AND gc.entityId = :entityId AND gc.groupConversationId BETWEEN :startGcId AND :endGcId ORDER BY gc.createdDateTime")
    List<GroupConversation> findMessagesBetweenGroupConversationIds(@Param("entityTypeId") Integer entityTypeId,
                                                                    @Param("entityId") Long entityId,
                                                                    @Param("startGcId") Long startGcId,
                                                                    @Param("endGcId") Long endGcId);

//    @Query("SELECT new com.tse.core_application.model.GroupConversationDTO(" +
//            "gc.groupConversationId, gc.message, gc.messageTags, gc.postedByAccountId, " +
//            "gc.createdDateTime, gc.entityTypeId, gc.entityId, " +
//            "new com.tse.core_application.model.AttachmentMetadata(a.attachmentId, a.fileName, a.fileType, a.fileSize)) " +
//            "FROM GroupConversation gc " +
//            "LEFT JOIN gc.attachments a " +
//            "WHERE gc.entityTypeId = :entityTypeId AND gc.entityId = :entityId " +
//            "ORDER BY gc.createdDateTime")
//    List<GroupConversationDTO> findDetailedByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM GroupConversation gc WHERE gc.entityTypeId = :entityTypeId AND gc.entityId IN :entityIds")
    void deleteByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityIds);
}
