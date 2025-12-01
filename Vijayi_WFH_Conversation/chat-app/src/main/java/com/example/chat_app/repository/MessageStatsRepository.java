package com.example.chat_app.repository;

import com.example.chat_app.dto.MessageStatsResultInterface;
import com.example.chat_app.model.MessageStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageStatsRepository extends JpaRepository<MessageStats,Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE chat.message_stats " +
        " SET delivered_count = delivered_count + 1 " +
        " WHERE message_id IN (:ids) " +
        " RETURNING " +
            " message_id AS messageId, " +
            " sender_id AS senderId, " +
            " delivered_count AS deliveredCount, "+
            " read_count AS readCount, " +
            " group_size AS groupSize ", nativeQuery = true)
    List<MessageStatsResultInterface> incrementAndFetchDelivered(@Param("ids") List<Long> ids);

    @Transactional
    @Modifying
    @Query(value = " UPDATE chat.message_stats "+
        " SET read_count = read_count + 1, delivered_count = delivered_count + 1 " +
        " WHERE message_id IN (:ids) " +
        " RETURNING " +
            " message_id AS messageId, " +
            " sender_id AS senderId, " +
            " delivered_count AS deliveredCount, " +
            " read_count AS readCount, " +
            " group_size AS groupSize " , nativeQuery = true)
    List<MessageStatsResultInterface> incrementAndFetchRead(@Param("ids") List<Long> ids);

    @Query("SELECT ms FROM MessageStats ms where ms.messageId IN :messageIds and ms.senderId = :accountId ")
    List<MessageStats> findAllByMessageIdIn(@Param("messageIds") List<Long> messageIds, @Param("accountId") Long accountId);
}
