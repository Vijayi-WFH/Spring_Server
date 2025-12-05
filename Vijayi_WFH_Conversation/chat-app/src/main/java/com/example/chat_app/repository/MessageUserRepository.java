package com.example.chat_app.repository;

import com.example.chat_app.dto.MessageDTO;
import com.example.chat_app.model.MessageUser;
import com.example.chat_app.model.MessageUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageUserRepository extends JpaRepository<MessageUser, MessageUserId> {

    @Query("SELECT mu FROM MessageUser mu where mu.message.messageId = :messageId")
    List<MessageUser> findByMessageId(Long messageId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE chat.message_user mu " +
            "SET " +
            "    is_delivered = true," +
            "    delivered_at = COALESCE(delivered_at, NOW()) " +
            "WHERE " +
            "    mu.account_id = :accountId " +
            "    AND mu.message_id <= :messageId " +
            "    AND mu.is_delivered = FALSE " +
            "    AND EXISTS ( " +
            "        SELECT 1 FROM chat.message m " +
            "        WHERE m.message_id = mu.message_id " +
            "        AND m.group_id = :groupId " +
            "    )" +
            "RETURNING mu.message_id;", nativeQuery = true)
    List<Long> markAsDeliveredForGroupMessagesInBulk(@Param("groupId") Long groupId, @Param("messageId") Long messageId,
                                                  @Param("accountId")Long accountId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE chat.message_user mu " +
            "SET " +
            "    is_delivered = TRUE, " +
            "    delivered_at = COALESCE(delivered_at, NOW()) " +
            "WHERE " +
            "    mu.account_id = :accountId " +
            "    AND mu.message_id <= :messageId " +
            "    AND mu.is_delivered = FALSE " +
            "    AND EXISTS ( " +
            "        SELECT 1 FROM chat.message m " +
            "        WHERE m.message_id = mu.message_id " +
            "        AND m.group_id IS NULL " +
            "        AND m.sender_id = :senderId " +
            "    ) " +
            "RETURNING mu.message_id;", nativeQuery = true)
    List<Long> markAsDeliveredForDirectMessagesInBulk(@Param("senderId") Long senderId, @Param("messageId") Long messageId,
                                                  @Param("accountId")Long accountId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE chat.message_user mu " +
            "SET " +
            "    is_read = true," +
            "    read_at = NOW()," +
            "    is_delivered = true," +
            "    delivered_at = COALESCE(delivered_at, NOW()) " +
            "WHERE " +
            "    mu.account_id = :accountId " +
            "    AND mu.message_id <= :messageId " +
            "    AND mu.is_read = false " +
            "    AND EXISTS ( " +
            "        SELECT 1 FROM chat.message m " +
            "        WHERE m.message_id = mu.message_id " +
            "        AND m.group_id = :groupId " +
            "    ) " +
            "RETURNING mu.message_id;", nativeQuery = true)
    List<Long> markAsReadForGroupMessagesInBulk(@Param("groupId") Long groupId, @Param("messageId") Long messageId,
                                                  @Param("accountId")Long accountId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE chat.message_user mu " +
            "SET " +
            "    is_read = true," +
            "    read_at = NOW()," +
            "    is_delivered = true," +
            "    delivered_at = COALESCE(delivered_at, NOW()) " +
            "WHERE " +
            "    mu.account_id = :accountId " +
            "    AND mu.message_id <= :messageId " +
            "    AND mu.is_read = FALSE " +
            "    AND EXISTS ( " +
            "        SELECT 1 FROM chat.message m " +
            "        WHERE m.message_id = mu.message_id " +
            "        AND m.group_id IS NULL " +
            "        AND m.sender_id = :senderId " +
            "    ) " +
            "RETURNING mu.message_id;", nativeQuery = true)
    List<Long> markAsReadForDirectMessagesInBulk(@Param("senderId") Long senderId, @Param("messageId") Long messageId,
                                             @Param("accountId")Long accountId);

    @Query("SELECT NEW com.example.chat_app.dto.MessageDTO(mu.message.messageId ,m.senderId, m.receiverId, m.groupId, mu.user.accountId) " +
            "From MessageUser mu JOIN Message m on mu.message.messageId = m.messageId where mu.isRead = :isRead and mu.user.accountId IN :accountIds")
    List<MessageDTO> findUnreadMessagesByAccountIdIn(@Param("isRead") Boolean isRead, @Param("accountIds") List<Long> accountIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM MessageUser mu WHERE mu.message.messageId IN (SELECT m.messageId FROM Message m WHERE m.groupId IN :groupIds)")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM MessageUser mu WHERE mu.user.accountId IN :userIds")
    void deleteByUserIdIn(@Param("userIds") List<Long> userIds);
}
