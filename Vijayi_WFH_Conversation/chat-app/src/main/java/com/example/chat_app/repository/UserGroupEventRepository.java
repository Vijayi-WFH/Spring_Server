package com.example.chat_app.repository;

import com.example.chat_app.model.UserGroupEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserGroupEventRepository extends JpaRepository<UserGroupEvent, Long> {

    List<UserGroupEvent> findByAccountIdAndGroupIdOrderByOccurredAtAsc(Long accountId, Long groupId);

    @Query(value = "SELECT ug.* FROM chat.user_group_event ug where ug.group_id = :groupId and ug.account_id IN (:accountIds) and ug.event_type = 'LEAVE' Order By ug.occurred_at desc limit 1 ", nativeQuery = true)
    UserGroupEvent findLastLeaveEventByGroupAndAccountId(Long groupId, List<Long> accountIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserGroupEvent uge WHERE uge.groupId IN :groupIds")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserGroupEvent uge WHERE uge.accountId IN :accountIds")
    void deleteByAccountIdIn(@Param("accountIds") List<Long> accountIds);
}
