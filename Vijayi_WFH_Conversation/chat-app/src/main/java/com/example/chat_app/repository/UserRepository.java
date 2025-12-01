package com.example.chat_app.repository;

import com.example.chat_app.dto.ChatResponse;
import com.example.chat_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByUserIdAndOrgId(Long userId, Long orgId);

    List<User> findByAccountIdIn(List<Long> accountIds);

    @Query(value = "SELECT org_id FROM chat.user WHERE user_id=:userId", nativeQuery = true)
    List<Long> getAllOrgIdsByUserId(Long userId);

    @Query("SELECT DISTINCT u FROM User u WHERE u.accountId IN :accountIdsList AND u.isActive=:isActive")
    List<User> findDistinctByAccountIdInAndIsActive(@Param("accountIdsList") List<Long> accountIdsList, @Param("isActive") Boolean isActive);

    @Query(value = "SELECT org_id FROM chat.user WHERE account_id IN :accountIdsList", nativeQuery = true)
    List<Long> findOrgIdByAccountIdIn(List<Long> accountIdsList);

    List<User> findDistinctByOrgIdIn(List<Long> accountIdsList);

    List<User> findByUserId(Long userId);

    User findFirstByOrgIdInAndAccountIdInAndIsActive(List<Long> orgIds, List<Long> accountIds, boolean isActive);

    User findFirstByAccountIdInAndIsActive(List<Long> accountIds, boolean b);

    User findByAccountIdAndIsActive(Long receiverId, boolean isActive);

    @Query("SELECT new com.example.chat_app.dto.ChatResponse(" +
            "u.accountId, u.firstName, u.orgId, u.isActive, u.lastName, " +
            "CASE WHEN m.isDeleted = true THEN 'This message was deleted!' WHEN m.content IS NULL THEN '' ELSE m.content END, " +
            "CASE WHEN m.senderId IS NULL THEN 0L ELSE m.senderId END, " +
            "CASE WHEN m.messageId IS NULL THEN 0L ELSE m.messageId END, m.timestamp, u.userId, u.email) " +
            "FROM User u " +
            "LEFT JOIN Message m ON ((m.receiverId = u.accountId AND m.senderId IN :accountIds) OR (m.senderId = u.accountId AND m.receiverId IN :accountIds)) " +
            "WHERE u.orgId IN :orgIds " +
            "AND (m.timestamp IS NULL OR m.timestamp = (" +
            "SELECT MAX(m2.timestamp) " +
            "FROM Message m2 " +
            "WHERE ((m2.receiverId = u.accountId AND m2.senderId IN :accountIds) OR (m2.senderId = u.accountId AND m2.receiverId IN :accountIds)) ))" +
            "ORDER BY m.timestamp DESC NULLS LAST")
    List<ChatResponse> findUserChatResponses(@Param("orgIds") List<Long> orgIds, @Param("accountIds") List<Long> accountIds);

    @Query("SELECT new com.example.chat_app.dto.ChatResponse(" +
            "u.accountId, u.firstName, u.orgId, u.isActive, u.lastName, COALESCE(m.content, ''), " +
            "CASE WHEN m.senderId IS NULL THEN 0L ELSE m.senderId END, " +
            "CASE WHEN m.messageId IS NULL THEN 0L ELSE m.messageId END, m.timestamp, u.userId, u.email) " +
            "FROM User u " +
            "LEFT JOIN Message m ON ((m.receiverId = u.accountId AND m.senderId IN :accountIds) OR " +
            "(m.senderId = u.accountId AND m.receiverId IN :accountIds)) " +
            "WHERE u.orgId IN :orgIds " +
            "AND (:searchText IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (m.timestamp IS NULL OR m.timestamp = (" +
            "SELECT MAX(m2.timestamp) " +
            "FROM Message m2 " +
            "WHERE ((m2.receiverId = u.accountId AND m2.senderId IN :accountIds) OR " +
            "(m2.senderId = u.accountId AND m2.receiverId IN :accountIds)) " +
            "AND m2.isDeleted = false)) " +
            "ORDER BY m.timestamp DESC NULLS LAST")
    List<ChatResponse> findUserChatResponses(
            @Param("orgIds") List<Long> orgIds,
            @Param("accountIds") List<Long> accountIds,
            @Param("searchText") String searchText);


    User findByAccountId(long accountId);

    List<User> findByUserIdInAndOrgId(List<Long> userIds, Long orgId);

    @Modifying
    @Query("Update User u set u.isActive = false Where u.accountId = :accountId")
    Integer markInActiveByAccountId(@Param("accountId")  Long accountId);

    User findFirstByUserId(Long userId);

    @Query(value = "SELECT DISTINCT user_id FROM chat.user WHERE org_id IN (SELECT org_id FROM chat.user WHERE user_id = :userId)", nativeQuery = true)
    List<Long> findUserIdsByOrgIdsOfUserId(Long userId);

    @Query(value = "SELECT DISTINCT u.user_id FROM chat.user u WHERE u.account_id IN (SELECT account_id FROM chat.user_group WHERE group_id = :groupId)", nativeQuery = true)
    List<Long> findUserIdsByGroupId(Long groupId);

    @Query(value = "SELECT u.userId FROM User u where u.accountId =:accountId ")
    Long findUserIdByAccountId(Long accountId);
}
