package com.tse.core_application.repository;

import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.EntityDesc;
import com.tse.core_application.dto.EntityPreferenceDto;
import com.tse.core_application.model.AccessDomain;
import com.tse.core_application.model.Project;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface AccessDomainRepository extends JpaRepository<AccessDomain, Long> {

    List<AccessDomain> findByEntityTypeIdAndAccountIdIn(Integer entityTypeId, List<Long> accountIds);

    List<AccessDomain> findByEntityTypeIdAndAccountIdInAndIsActive(Integer entityTypeId, List<Long> accountIds, Boolean isActive);

    List<AccessDomain> findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Integer entityTypeId, Long entityId, Long accountId, Boolean isActive);

    // imp: check this
    List<AccessDomain> findByEntityIdAndAccountIdInAndIsActive(Long entityId, List<Long> accountIds, Boolean isActive);

    List<AccessDomain> findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Integer entityTypeId, Long entityId, List<Long> accountIds, Boolean isActive);

    @Query("SELECT NEW com.tse.core_application.custom.model.AccountIdEntityTypeIdRoleId(a.accountId, a.entityTypeId, a.roleId) FROM AccessDomain a WHERE a.accountId = :accountId AND a.entityId = :entityId AND a.entityTypeId = :entityTypeId AND a.isActive = :isActive")
    List<AccountIdEntityTypeIdRoleId> findAccountIdEntityTypeIdRoleIdByAccountIdAndEntityIdAndIsActive(Long accountId,Integer entityTypeId, Long entityId, Boolean isActive);


    @Query("select new com.tse.core_application.custom.model.AccountIdEntityTypeIdRoleId( d.accountId, d.entityTypeId, d.roleId ) from AccessDomain d inner join d.userAccount u where u.fkUserId.userId=:userId")
    List<AccountIdEntityTypeIdRoleId> getAccountIdEntityTypeIdRoleIdByUserId(Long userId);

    @Query("select new com.tse.core_application.custom.model.AccountIdEntityIdRoleId( d.accountId, d.entityId, d.roleId ) from AccessDomain d inner join d.entities e where e.entityType=:entityType")
    List<AccountIdEntityIdRoleId> getAccountIdEntityIdRoleIdByEntityType(String entityType);

    List<EntityTypeId> findEntityTypeIdByAccountId(Long accountId);

    List<AccessDomain> findByAccountIdAndIsActive(Long accountId, Boolean isActive);

    List<AccessDomain> findByAccountIdAndRoleId(Long accountId, Integer roleId);

    List<EntityId> findEntityIdByAccountIdAndRoleIdAndIsActive(Long accountId, Integer roleId, Boolean isActive);

    List<AccessDomain> findByAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive);
    List<AccountId> findAccountIdByEntityId(Long entityId);

    List<RoleId> findRoleIdByAccountIdAndEntityIdAndIsActive(Long accountId, Long entityId, Boolean isActive);


    @Modifying
    @Query("update AccessDomain a set a.isActive = false where (a.accountId = :accountId and a.entityId = :entityId and a.entityTypeId = 5 and a.roleId = :roleId and a.isActive = true)")
    void deactivateUserAccessDomainFromTeam(@Param("accountId") Long accountId, @Param("roleId") Integer roleId, @Param("entityId") Long entityId);

    @Modifying
    @Query("update AccessDomain a set a.isActive = false where (a.accountId = :accountId and a.entityTypeId = 5 and a.isActive = true)")
    void deactivateUserAllAccessDomainsInAllTeams(@Param("accountId") Long accountId);

    List<AccessDomain> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    List<AccessDomain> findByEntityTypeIdAndEntityIdAndIsActive(Integer entityTypeId, Long entityId, Boolean isActive);

    @Query(value = "SELECT DISTINCT ua.user_id " +
            "FROM tse.access_domain ad " +
            "JOIN tse.user_account ua ON ad.account_id = ua.account_id " +
            "WHERE ad.entity_type_id = :entityTypeId " +
            "AND ad.entity_id = :entityId " +
            "AND ad.is_active = :isActive", nativeQuery = true)
    List<Long> findUserIdsByEntityTypeIdAndEntityIdAndActive(@Param("entityTypeId") Integer entityTypeId,
                                                             @Param("entityId") Long entityId,
                                                             @Param("isActive") Boolean isActive);

    List<AccessDomain> findDistinctByEntityIdAndRoleIdAndIsActive(Long entityId, Integer roleId, Boolean isActive);

    List<AccountId> findAccountIdByEntityTypeIdAndEntityId(Integer EntityTypeId, Long EntityId);

    List<AccessDomain> findByAccountIdAndEntityIdAndIsActive(Long accountId, Long entityId, Boolean isActive);

    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Integer entityTypeId, Long entityId, Boolean isActive);

    @Query("SELECT DISTINCT new com.tse.core_application.custom.model.AccountId( d.accountId) from AccessDomain d WHERE d.entityTypeId = :entityTypeId AND d.entityId IN :entityId AND d.isActive = :isActive")
    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Integer entityTypeId, List<Long> entityId, Boolean isActive);

    List<AccountIdIsActive> findDistinctAccountIdIsActiveByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    AccessDomain findByEntityTypeIdAndEntityIdAndRoleIdAndIsActive(Integer entityTypeId, Long entityId, Integer roleId, Boolean isActive);

    List<AccessDomain> findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Integer entityTypeId, Long entityId, List<Integer> roleIds, Boolean isActive);

    // Below query is used only for org not for teams
    // 13-09-2023: No requirement of checking isActive since we can't remove the orgAdmin from the org
    @Query("select a.accountId from AccessDomain a where a.entityTypeId = 2 and a.entityId = :entityId")
    Long getAccountIdByOrgId(@Param("entityId") Long entityId);

    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Integer EntityTypeId, Long EntityId, List<Integer> roleIds, Boolean isActive);
    // ------
    @Query("select distinct a.entityId from AccessDomain a where a.entityTypeId = :entityTypeId and a.roleId = :roleId and a.accountId in :accountIds and a.isActive = true")
    List<Long> findDistinctOrgIdByEntityTypeIdAndRoleIdAndAccountIdInAndIsActive(@Param("entityTypeId") Integer entityTypeId, @Param("roleId") Integer roleId, @Param("accountIds") List<Long> accountIds);

    @Query("Select new com.tse.core_application.dto.EntityDesc(a.entityId, a.entityTypeId, a.accountId) from AccessDomain a" +
            " where a.accountId IN :accountIds and a.entityTypeId = :entityTypeId and a.isActive = :isActive")
    List<EntityDesc> findEntityIdByAccountIdInAndEntityTypeIdAndIsActive(List<Long> accountIds, Integer entityTypeId, Boolean isActive);

    @Query("select a.entityId from AccessDomain a where a.entityTypeId = :entityTypeId and a.accountId in :accountIds and a.isActive = true")
    List<Integer> findEntityIdByEntityTypeIdAndAccountIdsInAndIsActive(@Param("entityTypeId") Integer entityTypeId, @Param("accountIds") List<Long> accountIds);

    @Query("select a.roleId from AccessDomain a where a.accountId = :accountId and a.entityTypeId = :typeId and a.entityId = :entityId and a.isActive = true")
    List<Integer> findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(@Param("accountId") Long accountId, @Param("typeId") Integer typeId, @Param("entityId") Long entityId);

    @Query("select a.roleId from AccessDomain a where a.accountId in  :accountIds and a.entityTypeId = :typeId and a.entityId in  :entityIds and a.isActive = :isActive")
    List<Integer> findAllRoleIdsByAccountIdsEntityTypeIdAndEntityIdsAndIsActive(@Param("accountIds") List<Long> accountIds, @Param("typeId") Integer typeId, @Param("entityIds") List<Long> entityIds, @Param("isActive") Boolean isActive);

    @Query("select distinct a.accountId from AccessDomain a where a.entityTypeId = :entityTypeId and a.entityId = :entityId and a.accountId in :accountIdList and a.isActive = :isActive")
    Long findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Integer entityTypeId, Long entityId, List<Long> accountIdList, Boolean isActive);

    @Query("select distinct a.accountId from AccessDomain a where a.entityId=:teamId and a.entityTypeId=:team and a.isActive = true")
    List<Long> findDistinctAccountIdsByEntityTypeAndEntityTypeIdAndIsActive(Long teamId, Integer team);

    @Query("select max(a.roleId) from AccessDomain a where a.accountId = :accountId and a.entityTypeId = :entityTypeId and a.entityId = :entityId and a.isActive = :isActive and a.roleId <= 15")
    Integer getMaxRoleIdForAccountIdAndTeamIdAndIsActive(@Param("accountId") Long accountId , @Param("entityTypeId") Integer entityTypeId, @Param("entityId") Long entityId, @Param("isActive") Boolean isActive);

    List<AccessDomain> findByEntityTypeIdAndRoleIdAndAccountIdAndIsActive(Integer entityTypeId, Integer roleId, Long accountId, Boolean isActive);
    AccessDomain findFirstByEntityTypeIdAndEntityIdAndRoleIdAndAccountIdNotAndIsActive(Integer entityTypeId, Long entityId, Integer roleId, Long accountId, Boolean isActive);

    AccessDomain findFirstByEntityTypeIdAndEntityIdAndRoleIdAndAccountIdAndIsActive(Integer entityTypeId, Long entityId, Integer roleId, Long accountId, Boolean isActive);

    @Query("SELECT NEW com.tse.core_application.custom.model.AccountIdEntityIdRoleId(" +
            "CASE " +
            "   WHEN ra.actionId = :teamTaskViewActionId THEN 0L " +
            "   WHEN ra.actionId = :taskBasicUpdateActionId THEN ad.accountId " +
            "END, " +
            "ad.entityId, " +
            "ad.roleId) " +
            "FROM AccessDomain ad " +
            "JOIN RoleAction ra ON ad.roleId = ra.roleId " +
            "WHERE ad.accountId IN :accountIds " +
            "AND ad.entityTypeId = :entityTypeId " +
            "AND ad.isActive = true " +
            "AND (" +
            "   ra.actionId = :teamTaskViewActionId OR " +
            "   (ra.actionId = :taskBasicUpdateActionId AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM AccessDomain ad2 " +
            "       JOIN RoleAction ra2 ON ad2.roleId = ra2.roleId " +
            "       WHERE ad2.accountId = ad.accountId " +
            "       AND ra2.actionId = :teamTaskViewActionId" +
            "       AND ad2.entityTypeId = :entityTypeId " +
            "       AND ad2.entityId = ad.entityId"+
            "       AND ad2.isActive = true " +
            "   ))" +
            ")")
    List<AccountIdEntityIdRoleId> findAccountIdEntityIdRoleIdsByAccountIdsAndEntityTypeIdAndActionIds(@Param("accountIds") List<Long> accountIds,
                                                                                                      @Param("entityTypeId") int entityTypeId,
                                                                                                      @Param("teamTaskViewActionId") Integer teamTaskViewActionId,
                                                                                                      @Param("taskBasicUpdateActionId") Integer taskBasicUpdateActionId);

    @Query("SELECT NEW com.tse.core_application.custom.model.AccountIdEntityIdRoleId(" +
            "CASE " +
            "   WHEN ra.actionId = :teamTaskViewActionId THEN 0L " +
            "   WHEN ra.actionId = :taskBasicUpdateActionId THEN ad.accountId " +
            "END, " +
            "ad.entityId, " +
            "ad.roleId) " +
            "FROM AccessDomain ad " +
            "JOIN RoleAction ra ON ad.roleId = ra.roleId " +
            "WHERE ad.accountId IN :accountIds " +
            "AND ad.entityTypeId = :entityTypeId " +
            "AND ad.entityId = :entityId " +
            "AND ad.isActive = true " +
            "AND (" +
            "   ra.actionId = :teamTaskViewActionId OR " +
            "   (ra.actionId = :taskBasicUpdateActionId AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM AccessDomain ad2 " +
            "       JOIN RoleAction ra2 ON ad2.roleId = ra2.roleId " +
            "       WHERE ad2.accountId = ad.accountId " +
            "       AND ad2.entityTypeId = :entityTypeId " +
            "       AND ad2.entityId = :entityId " +
            "       AND ra2.actionId = :teamTaskViewActionId" +
            "       AND ad2.isActive = true " +
            "   ))" +
            ")")
    List<AccountIdEntityIdRoleId> findAccountIdEntityIdRoleIdByAccountIdsEntityIdEntityTypeIdAndActionIds(@Param("accountIds") List<Long> accountIds,
                                                                                                          @Param("entityTypeId") int entityTypeId,
                                                                                                          @Param("entityId") Long entityId, @Param("teamTaskViewActionId") Integer teamTaskViewActionId,
                                                                                                          @Param("taskBasicUpdateActionId") Integer taskBasicUpdateActionId);

    @Query("SELECT DISTINCT NEW com.tse.core_application.custom.model.EmailNameOrgCustomModel(u.email, o.organizationName, o.orgId, u.fkUserId.firstName, u.fkUserId.lastName, u.accountId, a.entityId, a.isActive) " +
            "FROM AccessDomain a " +
            "JOIN UserAccount u ON a.accountId = u.accountId " +
            "JOIN Organization o ON u.orgId = o.orgId " +
            "WHERE a.entityId IN :teamIds " +
            "AND a.entityTypeId = :entityTypeId " +
            "AND NOT ((o.orgId = :personalOrgDefaultId AND a.entityId = :personalOrgDefaultId) AND (u.accountId NOT IN :accountIdList))")
    List<EmailNameOrgCustomModel> getEmailNameOrgActiveStatusList(@Param("teamIds") List<Long> teamIds, @Param("entityTypeId") int entityTypeId, @Param("personalOrgDefaultId") Long personalOrgDefaultId, @Param("accountIdList") List<Long> accountIdList);

    // this method gets the information of the teams that are associated with the given accountIds
    @Query("SELECT DISTINCT new com.tse.core_application.custom.model.EntityInfo(t.teamId, a.entityTypeId, t.teamName) " +
            "FROM AccessDomain a INNER JOIN Team t ON a.entityId = t.teamId " +
            "WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 and a.isActive = true")
    List<EntityInfo> getTeamInfoByAccountIdsAndIsActive(@Param("accountIds") List<Long> accountIds);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Integer entityTypeId, Long entityId, Long accountId, Boolean isActive);

    /**  Retrieves a list of unique team IDs for which the specified accounts have the 'teamTaskView' action.*/
    @Query("SELECT DISTINCT ad.entityId " +
            "FROM AccessDomain ad " +
            "JOIN RoleAction ra ON ad.roleId = ra.roleId " +
            "WHERE ad.accountId IN :accountIds " +
            "AND ad.entityTypeId = :entityTypeId " +
            "AND ra.actionId = :teamTaskViewActionId " +
            "AND ad.isActive = true")
    List<Long> findTeamIdsByAccountIdsAndActionId(@Param("accountIds") List<Long> accountIds, @Param("entityTypeId") int entityTypeId, @Param("teamTaskViewActionId") Integer teamTaskViewActionId);

    @Query("SELECT DISTINCT ad.entityId " +
            "FROM AccessDomain ad " +
            "WHERE ad.accountId IN :accountIds " +
            "AND ad.entityTypeId = 5 " +
            "AND ad.isActive = true")
    List<Long> findTeamIdsByAccountIdsAndIsActiveTrue(@Param("accountIds") List<Long> accountIds);

    @Query("SELECT DISTINCT a.entityId from AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.roleId in :roleIdList AND a.accountId in :accountIdList AND a.isActive = true")
    List<Integer> findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(Integer entityTypeId, List<Integer> roleIdList, List<Long> accountIdList);


    @Query("SELECT DISTINCT CAST(a.entityId AS java.lang.Long) from AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.accountId IN :accountIdList AND a.isActive = true")
    List<Long> findDistinctEntityIdsByActiveAccountIds(Integer entityTypeId, List<Long> accountIdList);

    @Query("SELECT DISTINCT p " +
            "FROM Project p WHERE p.projectId IN ( SELECT t.fkProjectId.projectId FROM AccessDomain a INNER JOIN Team t ON a.entityId = t.teamId " +
            "WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.isActive = true ) ")
    List<Project> getProjectInfoByAccountIdsAndIsActiveTrue(@Param("accountIds") List<Long> accountIds);

    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Integer EntityTypeId, List<Long> EntityId, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT NEW com.tse.core_application.dto.EntityPreferenceDto(a.entityTypeId, a.entityId, a.accountId) from AccessDomain a where entityTypeId = :entityTypeId and entityId IN :entityIds and roleId IN :roleIds and a.isActive = :isActive GROUP BY a.entityTypeId, a.entityId, a.accountId")
    List<EntityPreferenceDto> findAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Integer entityTypeId, List<Long> entityIds, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT DISTINCT new com.tse.core_application.custom.model.EmailFirstLastAccountId( u.email, u.accountId, u.fkUserId.firstName, u.fkUserId.lastName ) FROM UserAccount u JOIN AccessDomain a ON u.accountId = a.accountId WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityId AND a.roleId IN :roleIds AND a.isActive = :isActive")
    List<EmailFirstLastAccountId> getUserInfoWithRolesInEntities(Integer entityTypeId, List<Long> entityId, List<Integer> roleIds, Boolean isActive);


    @Query("SELECT DISTINCT new com.tse.core_application.custom.model.EmailFirstLastAccountId( u.email, u.accountId, u.fkUserId.firstName, u.fkUserId.lastName ) FROM UserAccount u JOIN AccessDomain a ON u.accountId = a.accountId WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityId AND a.isActive = :isActive")
    List<EmailFirstLastAccountId> getUserInfoInEntities(Integer entityTypeId, List<Long> entityId, Boolean isActive);

    @Modifying
    @Query("update AccessDomain a set a.isActive = false where (a.accountId = :accountId and a.entityTypeId = 4 and a.isActive = true)")
    void deactivateUserAllAccessDomainsInAllProjects(@Param("accountId") Long accountId);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Integer entityTypeId, Long entityId, List<Long> accountIds, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END " +
            "FROM RoleAction r JOIN Action a ON r.actionId = a.actionId " +
            "WHERE r.roleId in (SELECT ad.roleId FROM AccessDomain ad " +
            "WHERE ad.entityTypeId = :entityTypeId AND ad.entityId = :entityId " +
            "AND ad.accountId = :accountId AND ad.isActive = :isActive) " +
            "AND a.actionId = :actionId")
    Boolean findUserRoleInEntity(Integer entityTypeId, Long entityId, Long accountId, Boolean isActive, Integer actionId);

    Boolean existsByEntityTypeIdAndEntityIdAndRoleIdAndIsActiveAndAccountIdNot(Integer entityTypeId, Long entityId, Integer roleId, Boolean isActive, Long accountIds);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Integer entityTypeId, Long entityId, Long userAccountId, boolean isActive, List<Integer> authorizeRoleIdList);

    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query("SELECT DISTINCT new com.tse.core_application.custom.model.AccountId( d.accountId) from AccessDomain d WHERE d.entityTypeId = :entityTypeId AND d.entityId IN :entityId")
    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityId);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdNotIn(Integer entityTypeId, Long entityId, Long accountId, Boolean isActive, List<Integer> roleId);
    @Modifying
    @Query("update AccessDomain a set a.isActive = false where a.entityId = :entityId and a.entityTypeId = 5 and a.isActive = true")
    void deactivateAllUserAccessDomainFromTeam(@Param("entityId") Long entityId);

    @Modifying
    @Query("update AccessDomain a set a.isActive = false where a.entityId = :entityId and a.entityTypeId = 4 and a.isActive = true")
    void deactivateAllUserAccessDomainFromProject(@Param("entityId") Long entityId);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND t.fkOrgId.orgId = :orgId  AND a.isActive = :isActive")
    List<Long> findByAccountIdInAndRoleIdInAndOrgIdAndIsActive(List<Long> accountIds, List<Integer> roleIds, Long orgId, Boolean isActive);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND t.fkProjectId.projectId = :projectId AND a.isActive = :isActive")
    List<Long> findByAccountIdInAndRoleIdInAndProjectIdAndIsActive(List<Long> accountIds, List<Integer> roleIds, Long projectId, Boolean isActive);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive AND t.fkOrgId.orgId = :orgId")
    List<Long> findByOrgIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long orgId);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive AND t.fkProjectId.buId = :buId")
    List<Long> findByBuIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long buId);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive AND t.fkProjectId.projectId = :projectId")
    List<Long> findByProjectIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long projectId);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive And t.teamId = :teamId")
    List<Long> findByTeamIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long teamId);

    @Query("SELECT DISTINCT a.accountId FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityIds AND a.isActive = :isActive")
    List<Long> findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndIsActive(Integer entityTypeId, List<Long> entityIds, Boolean isActive);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Integer entityTypeId, Long entityId, List<Long> accountIdList, boolean isActive, List<Integer> authorizeRoleIdList);

    @Query("SELECT DISTINCT a.accountId FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityIds AND a.roleId IN :roleIds AND a.isActive = :isActive")
    List<Long> findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Integer entityTypeId, List<Long> entityIds, List<Integer> roleIds, Boolean isActive);

    Boolean existsByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Integer entityTypeId, List<Long> entityIds, List<Long> accountIdList, List<Integer> roleIds, Boolean isActive);

    List<AccessDomain> findByEntityTypeIdAndEntityIdAndAccountIdAndRoleIdInAndIsActive(Integer entityTypeId, Long entityId, Long accountId, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT COUNT(ad) > 0 " +
            "FROM AccessDomain ad " +
            "LEFT JOIN Team t ON ad.entityId = t.teamId AND ad.entityTypeId = 5 " +             // 5 for team entity type
            "LEFT JOIN Project p ON ad.entityId = p.projectId AND ad.entityTypeId = 4 " +       // 4 for project entity type
            "WHERE ((ad.entityTypeId = 2 AND ad.entityId = :orgId) " +                          // 2 for organization
            "OR (ad.entityTypeId = 5 AND t.fkOrgId.orgId = :orgId) " +                          // team roles within the org
            "OR (ad.entityTypeId = 4 AND p.orgId = :orgId)) " +                                 // project roles within the org
            "AND ad.accountId IN :accountIds " +
            "AND ad.roleId IN :roleIdList " +
            "AND ad.isActive = true")
    Boolean existsByRolesInOrg (List<Integer> roleIdList, Long orgId, List<Long> accountIds);

    @Query("SELECT DISTINCT ad.entityId FROM AccessDomain ad WHERE ad.entityTypeId = :entityTypeId AND ad.entityId IN :entityIdList AND ad.accountId IN :accountIds AND ad.roleId IN :roleList AND ad.isActive = :isActive")
    List<Long> findDistinctEntityIdByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Integer entityTypeId, List<Long> entityIdList, List<Long> accountIds, List<Integer> roleList, Boolean isActive);

    @Query("SELECT DISTINCT a.entityId FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityIdList AND a.accountId IN :accountIdList AND a.isActive = :isActive")
    List<Long> findDistinctEntityIdsByEntityTypeIdAndEntityIdInAndAccountIdInAndIsActive(@Param("entityTypeId") Integer entityTypeId, @Param("entityIdList") List<Long> entityIdList, @Param("accountIdList") List<Long> accountIdList, @Param("isActive") Boolean isActive);

    @Modifying
    @Transactional
    @Query("update AccessDomain a set a.isActive = true, a.lastAccessBeforeDeactivation = false where (a.accountId in :accountIds and a.isActive = false and a.lastAccessBeforeDeactivation is not null and a.lastAccessBeforeDeactivation = true)")
    void activateAccountIdsInAccessDomainOnReactivateUser(List<Long> accountIds);

    @Modifying
    @Transactional
    @Query("update AccessDomain a set a.isActive = false, a.lastAccessBeforeDeactivation = true where (a.accountId in :accountIds and a.isActive = true)")
    void deactivateAccountIdsInAccessDomainOnDeactivateUser(List<Long> accountIds);

    @Query("SELECT new com.tse.core_application.custom.model.AccountIdIsActive(a.accountId, a.isActive) FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.entityId = :entityId AND a.roleId NOT IN :excludedRoleIds AND a.isActive = true ")
    List<AccountIdIsActive> findDistinctAccountIdIsActiveByEntityTypeIdAndEntityIdAndRoleIdNotIn(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityId") Long entityId,
            @Param("excludedRoleIds") List<Integer> excludedRoleIds
    );

    Boolean existsByEntityTypeIdAndAccountIdInAndIsActiveAndRoleIdIn(Integer entityTypeId, List<Long> accountIds, Boolean isActive, List<Integer> roleIds);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Integer entityTypeId, Long entityId, List<Long> accountIdList, Boolean isActive);
    
    @Query("SELECT a.entityId FROM AccessDomain a WHERE a.accountId IN :accountIds AND a.isActive = :isActive AND a.entityTypeId = :entityTypeId")
    Set<Long> findEntityIdByAccountIdAndEntityTypeId(
            @Param("accountIds") Set<Long> accountIds,
            @Param("isActive") Boolean isActive,
            @Param("entityTypeId") Integer entityTypeId
    );

    @Query("SELECT t.teamId FROM Team t " +
            "WHERE t.teamId IN :teamIds " +
            "AND t.teamId NOT IN (" +
            "   SELECT ad.entityId FROM AccessDomain ad " +
            "   WHERE ad.accountId = :accountId " +
            "   AND ad.entityTypeId = :entityTypeId " +
            "   AND ad.roleId IN :roleIds " +
            "   AND ad.isActive = true" +
            ")")
    List<Long> findTeamIdsWithoutAccessDomainForAccountAndRoles(
            @Param("teamIds") List<Long> teamIds,
            @Param("accountId") Long accountId,
            @Param("entityTypeId") Integer entityTypeId,
            @Param("roleIds") List<Integer> roleIds
    );

    @Query(value = "SELECT ad.accountId FROM AccessDomain ad where ad.entityId = :entityId and ad.entityTypeId = :entityTypeId and ad.isActive = true")
    List<Long> findAllActiveAccountIdsByEntityAndTypeIds(Integer entityTypeId, Long entityId);

    @Query("SELECT DISTINCT a.accountId FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityIdList AND a.roleId IN :roleIdsForMeetingAnalysis AND a.accountId IN :accountIdList AND a.isActive = :isActive")
    List<Long> findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndAccountIdInAndIsActive(Integer entityTypeId, List<Long> entityIdList, List<Integer> roleIdsForMeetingAnalysis, List<Long> accountIdList, Boolean isActive);

    @Query("SELECT a FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.isActive = :isActive")
    List<AccessDomain> findDistinctAccountIdsByEntityTypeIdAndIsActive(Integer entityTypeId, Boolean isActive);

    List<AccessDomain> findByEntityTypeIdAndEntityIdInAndIsActive(Integer entityTypeId, List<Long> entityIdList, Boolean isActive);
    Boolean existsByAccountIdInAndRoleIdInAndIsActive(List<Long> accountIdLong, List<Integer> roleId, Boolean isActive);

    @Query("select ad " +
            "from AccessDomain ad " +
            "join fetch ad.userAccount ua " +
            "join fetch ua.fkUserId u " +
            "where ad.isActive = true " +
            "and ( " +
            "  (ad.entityTypeId = :projectType and ad.entityId = :projectId) " +
            "  or " +
            "  (ad.entityTypeId = :orgType and ad.entityId = :orgId and ad.roleId in :orgAdminRoleIds) " +
            ")")
    List<AccessDomain> findAllForProjectAndOrgWithUsers(
            @Param("projectType") Integer projectType,
            @Param("projectId") Long projectId,
            @Param("orgType") Integer orgType,
            @Param("orgId") Long orgId,
            @Param("orgAdminRoleIds") List<Integer> orgAdminRoleIds
    );

    // ==================== Organization Deletion Methods ====================

    /**
     * Hard delete access domains by entity type and entity ID.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AccessDomain a WHERE a.entityTypeId = :entityTypeId AND a.entityId = :entityId")
    void deleteByEntityTypeIdAndEntityId(@Param("entityTypeId") Integer entityTypeId, @Param("entityId") Long entityId);

    /**
     * Hard delete access domains by account ID.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AccessDomain a WHERE a.accountId = :accountId")
    void deleteByAccountId(@Param("accountId") Long accountId);
}
