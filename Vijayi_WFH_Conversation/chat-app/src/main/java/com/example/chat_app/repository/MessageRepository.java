package com.example.chat_app.repository;

import com.example.chat_app.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByMessageId(Long messageId);

    List<Message> findByGroupId(Long groupId);

    @Query("SELECT m FROM Message m " +
            "WHERE ((m.senderId = :userId AND m.receiverId = :receiverId) " +
            "OR (m.senderId = :receiverId AND m.receiverId = :userId))")
    List<Message> getMessagesBySenderIdAndReceiverId(@Param("userId") Long userId, @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Message m " +
            "WHERE (m.senderId = :userId AND m.receiverId = :receiverId) " +
            "OR (m.senderId = :receiverId AND m.receiverId = :userId) " +
            "ORDER BY m.timestamp DESC")
    Page<Message> findBySenderIdAndReceiverId(Long userId, Long receiverId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "WHERE m.messageId < :messageId AND ((m.senderId = :userId AND m.receiverId = :receiverId) " +
            "OR (m.senderId = :receiverId AND m.receiverId = :userId)) " +
            "ORDER BY m.timestamp DESC")
    Page<Message> findByMessageIdLessThanAndSenderIdAndReceiverId(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("receiverId") Long receiverId, Pageable pageable);

    List<Message> findByGroupIdOrderByTimestampDesc(Long groupId);

    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND (:cursorTimestamp IS NULL OR m.timestamp < :cursorTimestamp) ORDER BY m.timestamp DESC")
    List<Message> findByGroupId(@Param("groupId") Long groupId, @Param("cursorTimestamp") Instant cursorTimestamp, Pageable pageable);

    @Query(value = "SELECT * FROM chat.message m " +
                    "WHERE m.group_id = :groupId " +
                    "AND (:messageId = 0 OR m.message_id < :messageId) " +
                    "ORDER BY timestamp DESC " ,
            nativeQuery = true
    )
    Page<Message> findByGroupIdWithCursor(@Param("groupId") Long groupId, @Param("messageId") Long messageId, Pageable pageable);

    @Query(value = "Select * FROM chat.message m where m.group_id = :groupId and m.timestamp < :timestamp " +
            "and (:messageId = 0 OR m.message_id < :messageId) ORDER BY m.timestamp DESC ", nativeQuery = true)
    Page<Message> findByGroupIdAndTimestampAndMessageIdLessThanEqual(Long groupId, Instant timestamp, Long messageId, Pageable pageable);

    Message findTopByGroupIdAndIsDeletedOrderByTimestampDescMessageIdDesc(Long groupId, boolean isDeleted);

    @Query(value = "SELECT m FROM MessageUser mu JOIN Message m ON m.messageId = mu.message.messageId where m.senderId IN :senderIds" +
            " AND m.receiverId=:receiverId AND (:onlyDelivered=false  OR mu.isDelivered=false) AND mu.isRead= false ORDER BY m.senderId ")
    List<Message> findAllBySenderIdInIsReadAndAccountId(@Param("receiverId") Long receiverId,
                                                        @Param("senderIds") List<Long> senderIds,
                                                        @Param("onlyDelivered") Boolean onlyDelivered);

    @Query(value = "SELECT m FROM MessageUser mu JOIN Message m ON m.messageId = mu.message.messageId where m.groupId IN :groupIds" +
            " AND (:onlyDelivered=false  OR mu.isDelivered=false)  AND mu.isRead=false ORDER BY m.groupId ")
    List<Message> findMessageUserBySenderIdInGroupAndIsDelivered(@Param("groupIds") List<Long> groupIds,
                                                                 @Param("onlyDelivered") Boolean onlyDelivered);
    @Query(value = "Select m FROM Message m where m.messageId IN :messagesIds")
    List<Message> findByMessageIdIn(List<Long> messagesIds);

    @Query(
            value = "WITH events AS ( " +
                    "SELECT account_id, group_id, event_type, occurred_at, " +
                    "LEAD(occurred_at) OVER (PARTITION BY account_id, group_id ORDER BY occurred_at) AS next_time " +
                    "FROM chat.user_group_event " +
                    "WHERE account_id = :accountId AND group_id = :groupId " +
                    "), " +
                    "intervals AS ( " +
                    "SELECT occurred_at AS joined_at, next_time AS left_at " +
                    "FROM events WHERE event_type = 'JOIN' " +
                    ") " +
                    "SELECT m.* FROM chat.message m " +
                    "JOIN intervals i ON m.group_id = :groupId " +
                    "AND m.timestamp BETWEEN i.joined_at AND COALESCE(i.left_at, now()) " +
                    "WHERE (:messageId = 0 OR m.message_id < :messageId) " +
                    "ORDER BY m.timestamp DESC ",
            countQuery = "WITH events AS ( " +
                    "    SELECT account_id, group_id, event_type, occurred_at, " +
                    "           LEAD(occurred_at) OVER (PARTITION BY account_id, group_id ORDER BY occurred_at) AS next_time " +
                    "    FROM chat.user_group_event " +
                    "    WHERE account_id = :accountId AND group_id = :groupId " +
                    "), " +
                    "intervals AS ( " +
                    "    SELECT occurred_at AS joined_at, next_time AS left_at " +
                    "    FROM events WHERE event_type = 'JOIN' " +
                    ") " +
                    "SELECT count(m.message_id) FROM chat.message m " +
                    "JOIN intervals i ON m.group_id = :groupId " +
                    "AND m.timestamp BETWEEN i.joined_at AND COALESCE(i.left_at, now())",
            nativeQuery = true
    )
    Page<Message> findMessagesWithIntervals(@Param("accountId") Long accountId, @Param("groupId") Long groupId,
                                            @Param("messageId") Long messageId, Pageable pageable);

    Optional<Message> findByMessageIdAndIsDeleted(Long messageId, Boolean isDeleted);

}
