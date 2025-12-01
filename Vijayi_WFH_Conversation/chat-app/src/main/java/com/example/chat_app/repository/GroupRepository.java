package com.example.chat_app.repository;

import com.example.chat_app.model.Group;
import com.example.chat_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByGroupId(Long groupId);

    // Modify this query to join on GroupUser instead of directly on users
    @Query("SELECT g FROM Group g JOIN g.groupUsers gu JOIN gu.user u WHERE u IN :users")
    List<Group> findByUsersIn(@Param("users") List<User> users);

    // Modify to use GroupUser join table for fetching groups with users
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.groupUsers gu LEFT JOIN gu.user u WHERE g.groupId = :groupId")
    Group findGroupWithUsers(@Param("groupId") Long groupId);

    // Modify to use GroupUser join table for the query
    @Query("SELECT g FROM Group g " +
            "JOIN g.groupUsers gu " +
            "JOIN gu.user u " +
            "WHERE u IN :users " +
            "ORDER BY CASE WHEN g.lastMessageTimestamp IS NULL THEN 1 ELSE 0 END, " +
            "g.lastMessageTimestamp DESC, g.groupId DESC")
    List<Group> findByUsersInOrderByLastMessageTimestampDescGroupIdDesc(@Param("users") List<User> users);

    @Query("SELECT g FROM Group g " +
            "JOIN g.groupUsers gu " +
            "JOIN gu.user u " +
            "WHERE g.orgId = :orgId " +
            "ORDER BY CASE WHEN g.lastMessageTimestamp IS NULL THEN 1 ELSE 0 END, " +
            "g.lastMessageTimestamp DESC, g.groupId DESC")
    List<Group> findForAdminOrderByLastMessageTimestampDescGroupIdDesc(@Param("orgId") Long orgId);

    @Query("SELECT g FROM Group g " +
            "JOIN g.groupUsers gu " +
            "JOIN gu.user u " +
            "WHERE u IN :users " +
            "AND g.orgId IN :orgIds " +
            "ORDER BY CASE WHEN g.lastMessageTimestamp IS NULL THEN 1 ELSE 0 END, " +
            "g.lastMessageTimestamp DESC, g.groupId DESC")
    List<Group> findByUsersInAndOrgIdInOrderByLastMessageTimestampDescGroupIdDesc(@Param("users") List<User> users, @Param("orgIds") List<Long> orgIds);

    @Query(value = "SELECT * FROM chat.group g  " +
            "JOIN chat.user_group gu on g.group_id = gu.group_id " +
            "left JOIN chat.user u on u.account_id = gu.account_id " +
            "where g.org_id IN :orgIds " +
            "and u.user_id = :userId " +
            "ORDER BY CASE WHEN g.last_message_timestamp IS NULL THEN 1 ELSE 0 END, " +
            "g.last_message_timestamp DESC, g.group_id desc", nativeQuery = true)
    List<Group> findByUserAndOrgIdOrderByLastMessageTimestampDescGroupIdDesc(@Param("userId") Long userId, @Param("orgIds") List<Long> orgIds);

    @Query("SELECT g FROM Group g " +
            "JOIN g.groupUsers gu " +
            "JOIN gu.user u " +
            "WHERE u IN :users " +
            "AND g.orgId IN :orgIds " +
            "AND LOWER(g.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "ORDER BY CASE WHEN g.lastMessageTimestamp IS NULL THEN 1 ELSE 0 END, " +
            "g.lastMessageTimestamp DESC, g.groupId DESC")
    List<Group> findByUsersInAndOrgIdInOrderByLastMessageTimestampDescGroupIdDesc(@Param("users") List<User> users, @Param("orgIds") List<Long> orgIds, @Param("searchText") String searchText);

    Group findTopByOrgIdAndType(Long orgId, String orgDefault);

    @Query(value = "SELECT * FROM chat.group g where g.entity_id = :entityId and g.entity_type_id = :entityTypeId limit 1", nativeQuery = true)
    Group findTopByEntityTypeAndEntityTypeId(Long entityId, Long entityTypeId);

    @Query(value = "Select Count(g.groupId) from Group g where g.name =:name")
    Integer findGroupByName(@Param("name") String name);
}