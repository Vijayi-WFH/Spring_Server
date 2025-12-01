package com.tse.core_application.repository;

import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.EntityDesc;
import com.tse.core_application.model.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {
	
//    List<UserAccount> findByUserId(Long userId);

    List<UserAccount> findByFkUserIdUserIdAndIsActive(Long userId, Boolean isActive);

    @Query("select u from UserAccount u " +
            "where u.fkUserId.userId in :userIds " +
            "and u.orgId in :orgIds " +
            "and u.isActive = :isActive " +
            "and (u.isVerified is null or u.isVerified = true)")
    List<UserAccount> findByFkUserIdUserIdInAndOrgIdInAndIsActive(List<Long> userIds, List<Long> orgIds, Boolean isActive);

    // find user's userId by accountId
    @Query("select u.fkUserId.userId from UserAccount u where u.accountId = :accountId")
    Long findUserIdByAccountId(Long accountId);

    // find all accountIds from userId
    @Query("select u.accountId from UserAccount u where u.fkUserId.userId = :userId and u.isActive = :isActive")
    List<Long> findAllAccountIdsByUserIdAndIsActive(Long userId, Boolean isActive);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.accountId = :accountId AND ua.isActive = true")
    UserAccount findFkUserIdByAccountIdAndIsActiveTrue(Long accountId);

    List<AccountId> findAccountIdByOrgIdAndIsActive(Long orgId, Boolean isActive);

    List<AccountId> findAccountIdByOrgId(Long orgId);
    //   find user by email -- check again
    List<UserAccount> findByEmail(String email);

    List<UserAccount> findByEmailAndIsActive(String email, Boolean isActive);

    @Query("select new com.tse.core_application.custom.model.EmailFirstLastAccountId( a.email, a.accountId, u.firstName, u.lastName ) from UserAccount a inner join a.fkUserId u where a.accountId=:accountId")
    EmailFirstLastAccountId getEmailFirstNameLastNameAccountIdByAccountId(Long accountId);

    @Query("SELECT ua FROM UserAccount ua WHERE ua.accountId IN :accountIds AND ua.isActive = true")
    List<UserAccount> findActiveUserAccountsByAccountIds(@Param("accountIds") List<Long> accountIds);

    //    @Query(value = "SELECT new com.tse.core_application.custom.model.EmailFirstLastAccountId( a.email, a.account_id, u.first_name, u.last_name ) " +
//            "FROM tse.user_account a " +
//            "INNER JOIN tse.tse_users u ON u.user_id = a.user_id " +
//            "WHERE a.account_id = :accountId " +
//            "LIMIT 1", nativeQuery = true)
//    EmailFirstLastAccountId getEmailFirstNameLastNameAccountIdByAccountIdActiveOrInActive(Long accountId);

    @Query("SELECT new com.tse.core_application.custom.model.EmailFirstLastAccountId( a.email, a.accountId, u.firstName, u.lastName ) " +
            "FROM UserAccount a " +
            "INNER JOIN User u ON u.userId = a.fkUserId.userId " +
            "WHERE a.accountId = :accountId")
    List<EmailFirstLastAccountId> getEmailFirstNameLastNameAccountIdByAccountIdActiveOrInActive(Long accountId, Pageable pageable);


    @Query("select new com.tse.core_application.custom.model.EmailFirstLastAccountId( a.email, a.accountId, u.firstName, u.lastName ) from UserAccount a inner join a.fkUserId u where a.accountId=:accountId and a.isActive=true")
    EmailFirstLastAccountId getEmailFirstNameLastNameAccountIdByAccountIdAndIsActive(Long accountId);

    //  find by accountId
    UserAccount findByAccountId(Long accountId);

    UserAccount findFirstByAccountId(Long accountId);

    UserAccount findByAccountIdAndIsActive(Long accountId, Boolean isActive);

    UserAccount findByOrgIdAndFkUserIdUserIdAndIsActive(Long orgId, Long userId, Boolean isActive);

    UserAccount findByEmailAndOrgIdAndIsActive(String email, Long orgId, Boolean isActive);

    List<UserAccount> findByOrgId(Long orgId);

    List<UserAccount> findByAccountIdInAndIsActive(List<Long> accountIds, Boolean isActive);

    UserAccount findByAccountIdInAndOrgIdAndIsActive(List<Long> accountIds, Long orgId, Boolean isActive);

    /* This function get distinct user Ids of accountIds list */
    @Query("SELECT DISTINCT ua.fkUserId.userId FROM UserAccount ua WHERE ua.accountId IN :accountIds and ua.isActive = true")
    List<Long> findActiveUserIdsFromAccountIds(@Param("accountIds") List<Long> accountIds);
    UserAccount findByAccountIdAndOrgId(Long accountId, Long orgId);

    boolean existsByAccountIdAndOrgIdAndIsActive(Long accountId, Long orgId, Boolean isActive);

    boolean existsByAccountIdAndIsActive(Long accountId, Boolean isActive);

    boolean existsByFkUserIdUserIdAndOrgIdAndIsActive(Long userId, Long orgId, Boolean isActive);

    @Query("select new com.tse.core_application.custom.model.OrgId(u.orgId) from UserAccount u where u.fkUserId.userId = :userId and u.isActive = :isActive And (u.isVerified is null Or u.isVerified = true)")
    List<OrgId> findOrgIdByFkUserIdUserIdAndIsActive(Long userId, Boolean isActive);

     OrgId findOrgIdByAccountIdAndIsActive(Long accountId, Boolean isActive);

    @Query(value = "select account_id from tse.user_account where account_id not in(select account_id from tse.time_tracking where org_id = :orgId and new_effort_date = CURRENT_DATE ) and org_id =:orgId and is_active = :isActive",nativeQuery = true)
    List<Long> findAccountIdForTimeSheetReminderAndIsActive(Long orgId, Boolean isActive);

    @Query(value = "select account_id from tse.user_account where account_id not in(select account_id from tse.time_tracking where org_id = :orgId and new_effort_date = (CURRENT_DATE - INTERVAL '1 DAY') ) and org_id =:orgId and is_active = :isActive",nativeQuery = true)
    List<Long> findAccountIdForTimeSheetReminderNextDayAndIsActive(Long orgId, Boolean isActive);

    @Query("select u.accountId from UserAccount u where fkUserId=:user and u.isActive = :isActive")
    List<Long> findAccountIdByFkUserIdUserIdAndIsActive(User user, Boolean isActive);

    @Modifying
    @Transactional
    @Query("update UserAccount ua set ua.isActive = false where ua.orgId = :orgId and ua.accountId = :accountId")
    Integer updateIsActiveByOrgIdAndAccountIdIn(Long orgId, Long accountId);

    @Query("select ua.accountId from UserAccount ua where ua.fkUserId.userId = :userId and ua.isActive = :isActive")
    List<Long> getUserAccountIdsFromUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") Boolean isActive);

    /**
     * Retrieves a list of AccountIdEntityIdRoleId objects (filter criteria) based on the given user ID and action IDs.
     * For a userId we retrieve all userAccounts [ we get all accountIds for a userId ]
     * For each accountId we get all accessDomains [ all Teams in which the user is present & his role]
     * If in any team the user has Team_Task_View action, we create the AccountIdEntityIdRoleId with accountId = 0L
     * If in any team the user doesn't have Team_Task_View action but only has Task_Basic_Update action then we create AccountIdEntityIdRoleId with accountId = user's accountId
     */
    @Query("SELECT NEW com.tse.core_application.custom.model.AccountIdEntityIdRoleId(" +
            "CASE " +
            "   WHEN ra.actionId = :teamTaskViewActionId THEN 0L " +
            "   WHEN ra.actionId = :taskBasicUpdateActionId THEN ad.accountId " +
            "END, " +
            "ad.entityId, " +
            "ad.roleId) " +
            "FROM AccessDomain ad " +
            "JOIN RoleAction ra ON ad.roleId = ra.roleId " +
            "JOIN UserAccount ua ON ua.accountId = ad.accountId " +
            "WHERE ua.fkUserId.userId = :userId " +
            "AND ad.entityTypeId = :entityTypeId " +
            "AND ad.isActive = true " +    // Adding isActive check for AccessDomain
            "AND ua.isActive = true " +    // Adding isActive check for UserAccount
            "AND (" +
            "   ra.actionId = :teamTaskViewActionId OR " +
            "   (ra.actionId = :taskBasicUpdateActionId AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM AccessDomain ad2 " +
            "       JOIN RoleAction ra2 ON ad2.roleId = ra2.roleId " +
            "       WHERE ad2.accountId = ad.accountId " +
            "       AND ra2.actionId = :teamTaskViewActionId" +
            "       AND ad2.isActive = true " + // Adding isActive check for nested query on AccessDomain
            "   ))" +
            ")")
    List<AccountIdEntityIdRoleId> getAccountIdEntityIdRoleIdForUserId(
            @Param("userId") Long userId,
            @Param("entityTypeId") Integer entityTypeId,
            @Param("teamTaskViewActionId") Integer teamTaskViewActionId,
            @Param("taskBasicUpdateActionId") Integer taskBasicUpdateActionId);

    // This method retrieves the information of the organizations that are associated with the given accountIds
    @Query("SELECT new com.tse.core_application.custom.model.EntityInfo(o.orgId, 2, o.organizationName) " +
            "FROM UserAccount u INNER JOIN Organization o ON u.orgId = o.orgId " +
            "WHERE u.accountId IN :accountIds and u.isActive = :isActive")
    List<EntityInfo> findOrgInfoByAccountIdsAndIsActive(@Param("accountIds") List<Long> accountIds,  @Param("isActive") Boolean isActive);

    boolean existsByEmailAndIsActive(String email, boolean isActive);

    boolean existsByEmailAndOrgIdAndIsActive(String email, Long orgId, boolean isActive);

    @Query("select new com.tse.core_application.custom.model.EmailFirstLastAccountId( a.email, a.accountId, u.firstName, u.lastName ) from UserAccount a inner join a.fkUserId u where a.accountId in :accountIds")
    List<EmailFirstLastAccountId> getEmailFirstNameLastNameAccountIdByAccountIdIn(List<Long> accountIds);

    @Query("SELECT a.orgId FROM UserAccount a WHERE a.accountId in :accountIdList AND a.isActive = :isActive")
    List<Long> getAllOrgIdByAccountIdInAndIsActive(List<Long> accountIdList, boolean isActive);

    Boolean existsByAccountIdInAndOrgIdAndIsActive(List<Long> accountIds, Long orgId, Boolean isActive);

    @Query("select u.accountId from UserAccount u where u.fkUserId.userId IN :userId and u.isActive = :isActive")
    List<Long> findAllAccountIdsByUserIdInAndIsActive(List<Long> userId, Boolean isActive);

    UserAccount findByOrgIdAndFkUserIdUserIdInAndIsActive(Long orgId, List<Long> userId, Boolean isActive);

    List<UserAccount> findByFkUserIdUserIdInAndIsActive(List<Long> userId, Boolean isActive);

    boolean existsByFkUserIdPrimaryEmailAndIsActive(String email, Boolean isActive);

    @Query("SELECT ua.accountId FROM UserAccount ua WHERE ua.orgId = :orgId AND ua.isActive = :isActive AND ua.accountId IN :accountIdList")
    Long findAccountIdByOrgIdAndIsActiveAndAccountIdIn(Long orgId, boolean isActive, List<Long> accountIdList);

    @Query("select new com.tse.core_application.dto.EmailFirstLastAccountIdIsActive( a.email, a.accountId, u.firstName, u.lastName, a.isActive ) from UserAccount a inner join a.fkUserId u where a.accountId in :accountIds")
    List<EmailFirstLastAccountIdIsActive> getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(List<Long> accountIds);

    List<UserAccount> findByAccountIdIn(List<Long> accountIds);


    @Modifying
    @Transactional
    @Query("UPDATE UserAccount ua SET " +
            "ua.isActive = :isActive, " +
            "ua.isDisabledBySams = :isDisabledBySams, " +
            "ua.deactivatedByRole = :deactivatedByRole, " +
            "ua.deactivatedByAccountId = :deactivatedByAccountId " +
            "WHERE ua.accountId = :accountId")
    void updateIsActiveAndIsDisabledBySamsByAccountId(
            @Param("accountId") Long accountId,
            @Param("isActive") Boolean isActive,
            @Param("isDisabledBySams") Boolean isDisabledBySams,
            @Param("deactivatedByRole") Integer deactivatedByRole,
            @Param("deactivatedByAccountId") Long deactivatedByAccountId
    );

    @Modifying
    @Transactional
    @Query("update UserAccount ua set ua.isActive = :isActive, ua.isDisabledBySams = :isDisabledBySams, ua.deactivatedByAccountId= :deactivatedByAccountId, ua.deactivatedByRole =:deactivatedByRole where ua.orgId = :orgId and ua.email = :email")
    void updateIsActiveAndIsDisabledBySamsByOrgIdAndEmail(Long orgId, String email, Boolean isActive, Boolean isDisabledBySams, Long deactivatedByAccountId, Integer deactivatedByRole);

    @Modifying
    @Transactional
    @Query("update UserAccount ua set ua.isActive = :isDisabled, ua.isDisabledBySams = :isDisabledBySams,  ua.deactivatedByAccountId = :deactivatedByAccountId, ua.deactivatedByRole =:deactivatedByRole where ua.orgId = :orgId")
    void updateIsActiveAndIsDisabledBySamsByOrgId(Long orgId, Boolean isDisabled, Boolean isDisabledBySams, Long deactivatedByAccountId, Integer deactivatedByRole);

    @Query("select count(u) < :userLimit from UserAccount u where u.orgId = :orgId")
    Boolean isUserRegistrationAllowed (Long orgId, Long userLimit);

    @Query("select count(u) from UserAccount u where u.orgId = :orgId")
    Integer findUserCountByOrgId(Long orgId);

    @Query("select u.accountId from UserAccount u where u.orgId = :orgId and u.isActive = :isActive")
    List<Long> findAllAccountIdByOrgIdAndIsActive(Long orgId, Boolean isActive);

    @Query("select u.fkUserId.firstName from UserAccount u where u.accountId in :accountIds")
    List<String> findFirstNameByAccountIdIn(List<Long> accountIds);

    UserAccount findByEmailAndOrgIdAndIsActiveAndIsDisabledBySams(String email, Long orgId, Boolean isActive, Boolean isDisabledBySams);

    @Modifying
    @Transactional
    @Query("update UserAccount ua set ua.isActive = :isActive, ua.isDisabledBySams = :isDisabledBySams, ua.deactivatedByRole =:deactivatedByRole, ua.deactivatedByAccountId =:deactivatedByAccountId where ua.email = :email")
    void updateIsActiveAndIsDisabledBySamsByEmail(String email, Boolean isActive, Boolean isDisabledBySams, Integer deactivatedByRole, Long deactivatedByAccountId);

    @Query("select u.accountId from UserAccount u where u.email = :email and u.isActive = :isActive")
    List<Long> findAllAccountIdsByEmailAndIsActive(String email, Boolean isActive);

    @Query("Select DISTINCT u.accountId from UserAccount u where u.accountId In :accountIdList And u.isActive = :isActive")
    List<Long> findAllAccountIdsByAccountIdInAndIsActive (List<Long> accountIdList, Boolean isActive);

    @Query("select ua.orgId from UserAccount ua where ua.accountId=:accountId AND ua.isActive = true")
    Long findOrgIdByAccountId(Long accountId);

    @Query("select ua.orgId from UserAccount ua where ua.accountId=:accountId")
    Long findOrgIdByAccount(Long accountId);

    @Query("select u.orgId from UserAccount u where u.email = :email and u.isActive = :isActive and u.orgId <> 0")
    List<Long> findAllOrgIdByEmailAndIsActive(String email, Boolean isActive);

    @Query("SELECT DISTINCT ua.fkUserId.primaryEmail FROM UserAccount ua WHERE ua.accountId In :accountIdList AND ua.isActive = :isActive")
    List<String> findDistinctFkUserIdEmailByAccountIdInAndIsActive(List<Long> accountIdList, Boolean isActive);

    @Query("Select DISTINCT u.accountId from UserAccount u where u.accountId In :accountIds And u.isActive = :isActive And (u.isVerified is null Or u.isVerified = true)")
    List<Long> findAllAccountIdsByAccountIdInAndIsActiveAndIsVerifiedTrue(List<Long> accountIds, Boolean isActive);

    @Query("select u.accountId from UserAccount u where u.fkUserId.userId IN :userId and u.isActive = :isActive And (u.isVerified is null Or u.isVerified = true)")
    List<Long> findAllAccountIdsByUserIdInAndIsActiveAndIsVerifiedTrue(List<Long> userId, Boolean isActive);

    @Query("select u from UserAccount u where u.fkUserId.userId IN :userList and u.isActive = :isActive And (u.isVerified is null Or u.isVerified = true)")
    List<UserAccount> findByUserIdInAndIsActiveAndIsVerifiedTrue(List<Long> userList, Boolean isActive);

    @Query("select u from UserAccount u where u.email = :email and u.isActive = :isActive And (u.isVerified is null Or u.isVerified = true)")
    List<UserAccount> findByEmailAndIsActiveAndIsVerifiedTrue(String email, Boolean isActive);

    @Query("SELECT COUNT(u) FROM UserAccount u WHERE u.orgId = :orgId AND u.isActive = :isActive")
    Integer findUserCountByOrgIdAndIsActive(@Param("orgId") Long orgId, @Param("isActive") Boolean isActive);

    @Query("SELECT new com.tse.core_application.dto.EmailFirstLastAccountIdIsActive(" +
            "a.email, a.accountId, u.firstName, u.lastName, a.isActive) " +
            "FROM UserAccount a INNER JOIN a.fkUserId u WHERE a.accountId = :accountId")
    EmailFirstLastAccountIdIsActive getEmailFirstNameLastNameAccountIdIsActiveByAccountId(@Param("accountId") Long accountId);

    @Query("select new com.tse.core_application.dto.EmailFirstLastAccountIdIsActive(a.email, a.accountId, u.firstName, u.lastName, a.isActive) from UserAccount a inner join a.fkUserId u where a.orgId=:orgId")
    List<EmailFirstLastAccountIdIsActive> getEmailAccountIdFirstAndLastNameByOrgId(Long orgId);

    @Query("SELECT new com.tse.core_application.dto.EntityDesc(o.orgId, 2, o.organizationName, u.accountId) " +
            "FROM UserAccount u INNER JOIN Organization o ON u.orgId = o.orgId " +
            "WHERE u.accountId IN :accountIds and u.isActive = :isActive")
    List<EntityDesc> findOrgDescByAccountIdsAndIsActive(@Param("accountIds") List<Long> accountIds, @Param("isActive") Boolean isActive);

    @Query("select u.orgId from UserAccount u where u.accountId IN :accountIdList and u.isActive = :isActive and u.orgId <> 0")
    List<Long> findAllOrgIdByAccountIdInAndIsActive(List<Long> accountIdList, Boolean isActive);

    @Modifying
    @Transactional
    @Query("update UserAccount ua set ua.isRegisteredInAiService = :value where ua.accountId = :accountId")
    void updateIsRegisteredInAiService(Long accountId, Boolean value);

    @Query("Select ua.accountId from UserAccount ua where ua.isActive = true and ua.isRegisteredInAiService is Null")
    List<Long> findAllAccountIdByIsRegisteredInAiService(Boolean value);
}
