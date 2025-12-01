package com.example.chat_app.repository;

import com.example.chat_app.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {
    @Query(value = "SELECT * FROM history h WHERE h.message_id = :messageId ORDER BY h.timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<History> findMostRecentByMessageId(@Param("messageId") Long messageId);

}
