package com.example.chat_app.repository;

import com.example.chat_app.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {
    @Query(value = "SELECT * FROM history h WHERE h.message_id = :messageId ORDER BY h.timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<History> findMostRecentByMessageId(@Param("messageId") Long messageId);

    @Transactional
    @Modifying
    @Query("DELETE FROM History h WHERE h.messageId IN (SELECT m.messageId FROM Message m WHERE m.groupId IN :groupIds)")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);

}
