package com.tse.core.repository.supplements;

import com.tse.core.custom.model.*;
import com.tse.core.model.*;

import com.tse.core.model.supplements.AccessDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessDomainRepository extends JpaRepository<AccessDomain, Long> {

//    List<AccessDomain> findByEntityTypeIdAndAccountIdIn(Integer entityTypeId, List<Long> accountIds);
//
//    List<AccessDomain> findByEntityIdAndAccountIdIn(Integer entityId, List<Long> accountIds);
//
//    List<AccountIdEntityTypeIdRoleId> findAccountIdEntityTypeIdRoleIdByAccountId(Long accountId);
//
//    @Query("select new com.tse.core.custom.model.AccountIdEntityTypeIdRoleId( d.accountId, d.entityTypeId, d.roleId ) from AccessDomain d inner join d.userAccount u where u.fkUserId.userId=:userId")
//    List<AccountIdEntityTypeIdRoleId> getAccountIdEntityTypeIdRoleIdByUserId(Long userId);
//
//    @Query("select new com.tse.core.custom.model.AccountIdEntityIdRoleId( d.accountId, d.entityId, d.roleId ) from AccessDomain d inner join d.entities e where e.entityType=:entityType")
//    List<AccountIdEntityIdRoleId> getAccountIdEntityIdRoleIdByEntityType(String entityType);
//
//    List<EntityTypeId> findEntityTypeIdByAccountId(Long accountId);
//
//    List<AccessDomain> findByAccountId(Long accountId);
//
//    List<AccessDomain> findByAccountIdAndRoleId(Long accountId, Integer roleId);
//
//    List<EntityId> findEntityIdByAccountIdAndRoleId(Long accountId, Integer roleId);
//
//    List<AccessDomain> findByAccountIdInAndRoleId(List<Long> accountIds, Integer roleId);
//
//    List<AccountId> findAccountIdByEntityId(Integer entityId);
//
//    List<RoleId> findRoleIdByAccountIdAndEntityId(Long accountId, Integer entityId);
//
//    @Modifying
//    @Query("delete from AccessDomain a where (a.accountId=:accountId and a.entityId=:entityId) and a.roleId=:roleId")
//    void deleteAccountIdAndEntityId(Long accountId, Integer roleId, Integer entityId);
//
//    List<AccessDomain> findByEntityId(Integer entityId);
//
//    List<AccessDomain> findDistinctByEntityIdAndRoleId(Integer entityId, Integer roleId);
//
//    List<AccountId> findAccountIdByEntityTypeIdAndEntityId(Integer EntityTypeId, Integer EntityId);
//
//    List<AccessDomain> findByAccountIdAndEntityId(Long accountId, Integer entityId);

    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityId(Integer EntityTypeId, Long EntityId);

    @Query("SELECT DISTINCT new com.tse.core.custom.model.AccountId( d.accountId) from AccessDomain d WHERE d.entityTypeId = :entityTypeId AND d.entityId IN :entityId AND d.isActive = :isActive")
    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Integer entityTypeId, List<Long> entityId, Boolean isActive);


    @Query("SELECT DISTINCT NEW com.tse.core.custom.model.TeamDetails(t.teamId, t.teamName, t.fkProjectId.projectId, t.fkProjectId.projectName, t.fkProjectId.buId, t.fkOrgId.orgId, t.fkOrgId.organizationName) from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive AND t.fkOrgId.orgId = :orgId")
    List<TeamDetails> findByOrgIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long orgId);

    List<AccountId> findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Integer EntityTypeId, Long EntityId, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT DISTINCT d.accountId from AccessDomain d WHERE d.entityTypeId = :entityTypeId AND d.entityId = :entityId AND d.isActive = :isActive")
    List<Long> findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Integer entityTypeId, Long entityId, Boolean isActive);

    Boolean existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Integer entityTypeId, Long entityId, List<Long> accountIds, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT DISTINCT ad.entityId " +
            "FROM AccessDomain ad " +
            "WHERE ad.accountId IN :accountIds " +
            "AND ad.entityTypeId = 5 " +
            "AND ad.isActive = true")
    List<Long> findTeamIdsByAccountIdsAndIsActiveTrue(List<Long> accountIds);

    @Query("SELECT DISTINCT u.accountId FROM UserAccount u JOIN AccessDomain a ON u.accountId = a.accountId WHERE a.entityTypeId = :entityTypeId AND a.entityId IN :entityId AND a.roleId IN :roleIds AND a.isActive = :isActive")
    List<Long> getUserInfoWithRolesInEntities(Integer entityTypeId, List<Long> entityId, List<Integer> roleIds, Boolean isActive);

    @Query("SELECT DISTINCT NEW com.tse.core.custom.model.TeamDetails(t.teamId, t.teamName, t.fkProjectId.projectId, t.fkProjectId.projectName, t.fkProjectId.buId, t.fkOrgId.orgId, t.fkOrgId.organizationName) from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive AND t.fkProjectId.buId = :buId")
    List<TeamDetails> findByBuIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long buId);

    @Query("SELECT DISTINCT NEW com.tse.core.custom.model.TeamDetails(t.teamId, t.teamName, t.fkProjectId.projectId, t.fkProjectId.projectName, t.fkProjectId.buId, t.fkOrgId.orgId, t.fkOrgId.organizationName) from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive AND t.fkProjectId.projectId = :projectId")
    List<TeamDetails> findByProjectIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long projectId);

    @Query("SELECT DISTINCT NEW com.tse.core.custom.model.TeamDetails(t.teamId, t.teamName, t.fkProjectId.projectId, t.fkProjectId.projectName, t.fkProjectId.buId, t.fkOrgId.orgId, t.fkOrgId.organizationName) from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND a.isActive = :isActive And t.teamId = :teamId")
    List<TeamDetails> findByTeamIdAccountIdInAndRoleIdInAndIsActive(List<Long> accountIds, List<Integer> roleIds, Boolean isActive, Long teamId);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND t.fkProjectId.projectId = :projectId AND a.isActive = :isActive")
    List<Long> findByAccountIdInAndRoleIdInAndProjectIdAndIsActive(List<Long> accountIds, List<Integer> roleIds, Long projectId, Boolean isActive);

    @Query("SELECT DISTINCT ad.entityId FROM AccessDomain ad WHERE ad.entityTypeId = :entityTypeId AND ad.entityId IN :entityIdList AND ad.accountId IN :accountIds AND ad.roleId IN :roleList AND ad.isActive = :isActive")
    List<Long> findDistinctEntityIdByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Integer entityTypeId, List<Long> entityIdList, List<Long> accountIds, List<Integer> roleList, Boolean isActive);

    @Query("SELECT DISTINCT t.teamId from Team t JOIN AccessDomain a ON t.teamId = CAST(a.entityId AS java.lang.Long) WHERE a.accountId IN :accountIds AND a.entityTypeId = 5 AND a.roleId IN :roleIds AND t.fkOrgId.orgId = :orgId  AND a.isActive = :isActive")
    List<Long> findByAccountIdInAndRoleIdInAndOrgIdAndIsActive(List<Long> accountIds, List<Integer> roleIds, Long orgId, Boolean isActive);

}
