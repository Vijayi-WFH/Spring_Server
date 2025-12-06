package com.tse.core_application.repository;

import com.tse.core_application.model.ActionItem;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {


    @Modifying
    @Query("update ActionItem a set a.isDeleted = true where a.actionItemId = :actionItemId")
    Integer setIsDeletedByActionItemId(@Param("actionItemId") Long actionItemId);

    @Query(value = "SELECT * FROM tse.action_item WHERE meeting_id=:meetingId AND (is_deleted is null or is_deleted=:isDeleted)",nativeQuery = true)
    List<ActionItem> findByFkMeetingIdMeetingIdAndIsDeleted(Long meetingId, boolean isDeleted);

    @Query("SELECT a FROM ActionItem a WHERE a.meeting.meetingId IN :meetingIds AND (a.isDeleted is null or a.isDeleted = :isDeleted)")
    List<ActionItem> findActionsByMeetingIdInAndIsDeleted(List<Long> meetingIds, boolean isDeleted);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActionItem ai WHERE ai.meeting.meetingId IN :meetingIds")
    void deleteByMeetingIdIn(List<Long> meetingIds);
}
