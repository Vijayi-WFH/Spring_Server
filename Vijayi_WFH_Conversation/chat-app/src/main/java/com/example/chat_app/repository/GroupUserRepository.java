package com.example.chat_app.repository;

import com.example.chat_app.model.GroupUser;
import com.example.chat_app.model.GroupUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupUserRepository extends JpaRepository<GroupUser, GroupUserId> {

    @Modifying
    @Query("UPDATE GroupUser g SET g.isDeleted = true , g.isAdmin = false WHERE g.user.id IN :accountId AND g.group.id = :groupId ")
    void markUsersAsDeletedInGroup(@Param("accountId") List<Long> accountId, @Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE GroupUser g SET g.isAdmin = true WHERE g.user.id = :accountId AND g.group.id = :groupId ")
    void markUserAsAdminInGroup(@Param("accountId") Long accountId,@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE GroupUser g SET isDeleted = true, isAdmin = false WHERE g.user.accountId =:accountId")
    Integer deleteUserFromGroups(@Param("accountId") Long accountId);

    @Query(value = "SELECT * FROM group_user gu WHERE gu.group_id = :groupId AND gu.account_id = :accountId AND gu.is_deleted = :isDeleted", nativeQuery = true)
    GroupUser findGroupUser(@Param("groupId") Long groupId, @Param("accountId") Long accountId, @Param("isDeleted") boolean isDeleted);

    @Query(value = "SELECT gu FROM GroupUser gu where gu.user.accountId = :accountId and gu.group.groupId = :groupId")
    Optional<GroupUser> findByAccountIdAndGroupId(@Param("accountId") Long accountId, @Param("groupId") Long groupId);

    @Query(value = "SELECT gu FROM GroupUser gu where gu.user.accountId in :accountId and gu.group.groupId = :groupId")
    List<GroupUser> findByAccountIdInAndGroupId(@Param("accountId") List<Long> accountId, @Param("groupId") Long groupId);

    @Query(value = "SELECT gu FROM GroupUser gu where gu.user.accountId in :accountIds ")
    List<GroupUser> findAllByAccountIdIn(@Param("accountIds") List<Long> accountIds);

    @Query(value = "SELECT gu.user.accountId FROM GroupUser gu where gu.group.groupId = :groupId and gu.isDeleted =:isDeleted")
    List<Long> findAllAccountIdByGroupIdAndIsDeleted(@Param("groupId") Long groupId, @Param("isDeleted") Boolean isDeleted);

    @Modifying
    @Query("DELETE FROM GroupUser gu WHERE gu.group.groupId IN :groupIds")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);
}
