package com.tse.core_application.repository;

import com.tse.core_application.custom.model.*;
import com.tse.core_application.model.Team;
import com.tse.core_application.model.UserAccount;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("select t from Team t where t.teamName = :teamName and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    Team findByTeamName(String teamName);

    @Query("select t from Team t where t.teamId in :teamIds and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Team> findByTeamIdIn(List<Long> teamIds);

    /**
     * This method will find the list of teams for the given list of orgIds.
     *
     * @param orgId the list of orgIds.
     * @return List<Team>
     */
    @Query("select t from Team t where t.fkOrgId.orgId in :orgId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Team> findByFkOrgIdOrgIdIn(List<Long> orgId);

    // find orgId, projectId by its teamId
//	Team findByProjectId(Long projectId);

    //  find by teamId
    @Query("select t from Team t where t.teamId = :teamId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    Team findByTeamId(Long teamId);

    //  find by list<teamIds> and list<orgIds>
//	List<Team> findByTeamIdInAndOrgIdIn(List<Long> teamIds, List<Long> orgIds);

    @Query("select t from Team t where t.teamId in :teamIds and t.fkOrgId.orgId in :orgIds and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Team> findByTeamIdInAndFkOrgIdOrgIdIn(List<Long> teamIds, List<Long> orgIds);

    //  find team by orgId
//	List<Team> findByOrgId(Long orgId);

    //  find teamId, teamName by orgId, projectId
//	List<TeamIdAndTeamName> findTeamIdTeamNameByOrgIdAndProjectId(Long orgId, Long projectId);

    @Query("select NEW com.tse.core_application.custom.model.TeamIdAndTeamName(t.teamId, t.teamName, t.teamCode, t.isDeleted) from Team t where t.fkOrgId.orgId = :orgId and t.fkProjectId.projectId = :projectId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<TeamIdAndTeamName> findTeamIdTeamNameByFkOrgIdOrgIdAndFkProjectIdProjectId(Long orgId, Long projectId);

    @Query("select NEW com.tse.core_application.custom.model.TeamIdAndTeamName(t.teamId, t.teamName, t.teamCode, t.isDeleted) from Team t where t.fkOrgId.orgId = :orgId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<TeamIdAndTeamName> findTeamIdTeamNameByFkOrgIdOrgId(Long orgId);

    @Query("select t from Team t where t.fkOrgId.orgId = :orgId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Team> findByFkOrgIdOrgId(Long orgId);

    @Query("select t.teamId from Team t where t.fkOrgId.orgId = :orgId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByOrgId(@Param("orgId") Long orgId);

    @Query("select t.teamId from Team t where t.fkProjectId.projectId = :projectId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByProjectId(@Param("projectId") Long projectId);

    @Query("select t.teamName from Team t where t.teamId = :teamId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    String findTeamNameByTeamId(Long teamId);

    @Query("select t from Team t where t.fkProjectId.projectId = :projectId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Team> findByFkProjectIdProjectId(Long projectId);

    boolean existsByFkOrgIdOrgIdAndTeamIdAndIsDisabled(Long orgId, Long teamId, Boolean isDisabled);

    boolean existsByFkOrgIdOrgIdAndTeamIdAndFkProjectIdProjectIdAndIsDisabled(Long orgId, Long teamId, Long projectId, Boolean isDisabled);

    boolean existsByFkProjectIdProjectIdAndTeamIdAndIsDisabled(Long projectId, Long teamId, Boolean isDisabled);

//    Organization findFkOrgIdByTeamId(Long teamId);

    @Query("select t.fkOrgId.orgId from Team t where t.teamId = :teamId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    Long findFkOrgIdOrgIdByTeamId(Long teamId);

    @Query("SELECT DISTINCT NEW com.tse.core_application.custom.model.TeamOrgBuAndProjectName(t.teamId, t.teamCode, t.fkOrgId.orgId, t.fkProjectId.projectId, t.fkProjectId.buId, t.teamName, t.fkOrgId.organizationName, t.fkProjectId.projectName, b.buName) " +
            "FROM Team t " +
            "JOIN AccessDomain ad ON ad.entityId = CAST(t.teamId AS java.lang.Integer) " +
            "JOIN UserAccount ua ON ua.accountId = ad.accountId " +
            "JOIN BU b ON t.fkProjectId.buId = b.buId " +
            "WHERE ua.fkUserId.userId IN :userId " +
            "AND (ua.isVerified IS null OR ua.isVerified = true) " +
            "AND ad.entityTypeId = :entityTypeId " +
            "AND ad.isActive = true " +
            "AND (t.isDeleted = false or t.isDeleted is null) " +
            "AND t.isDisabled = false")
    List<TeamOrgBuAndProjectName> getAllMyTeamsForUserIdIn(@Param("userId") List<Long> userId, @Param("entityTypeId") Integer entityTypeId);


    @Query("select t.teamId from Team t where t.fkOwnerAccountId.accountId=:account and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdByfkOwnerAccountIdAccountId(Long account);

    @Modifying
    @Transactional
    @Query("update Team t set t.fkOwnerAccountId =:ownerAccount where t.teamId=:teamId and (t.isDeleted = false or t.isDeleted is null)")
    void updateOwnerAccountIdByTeamId(UserAccount ownerAccount, Long teamId);

    @Query("select t from Team t where t.fkProjectId.projectId in :projectIds and t.isDisabled = false and (t.isDeleted = false or t.isDeleted is null)")
    List<Team> findByFkProjectIdProjectIdIn(List<Long> projectIds);

    @Query("SELECT DISTINCT t.teamId FROM Team t WHERE t.fkProjectId.projectId in :projectIds and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByFkProjectIdProjectIdIn(List<Long> projectIds);

    @Query("SELECT DISTINCT t.teamName FROM Team t WHERE t.fkProjectId.projectId = :projectId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<String> findTeamNamesByFkProjectIdProjectIdIn(Long projectId);

    @Query("SELECT t.teamCode FROM Team t WHERE t.fkOrgId.orgId = :orgId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<String> findTeamCodeByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.fkOrgId.orgId = :orgId AND t.teamCode = :teamCode and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    boolean existsByOrgIdAndTeamCode(@Param("orgId") Long orgId, @Param("teamCode") String teamCode);

    @Query("select t.fkProjectId.projectId from Team t where t.teamId = :teamId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    Long findFkProjectIdProjectIdByTeamId(Long teamId);

    @Modifying
    @Transactional
    @Query("update Team t set t.isDisabled = :isDisabled where t.fkOrgId.orgId = :orgId")
    void updateIsDisabledByOrgId (Long orgId, Boolean isDisabled);

    @Query("select t.teamId from Team t where t.fkProjectId.buId = :buId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByBuId(Long buId);

    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.fkOrgId.orgId = :orgId AND t.teamName = :teamName AND t.fkOwnerAccountId.accountId = :ownerAccountId")
    boolean existsByOrgIdAndTeamNameAndOwnerAccountId(Long orgId, String teamName, Long ownerAccountId);

    @Query("SELECT count(t) < :teamLimit FROM Team t WHERE t.fkOrgId.orgId = :orgId")
    Boolean isTeamRegistrationAllowed (Long orgId, Long teamLimit);

    @Query("SELECT count(t) FROM Team t WHERE t.fkOrgId.orgId = :orgId and (t.isDeleted = false or t.isDeleted is null)")
    Integer findTeamCountByOrgId (Long orgId);

    @Query("select NEW com.tse.core_application.custom.model.TeamIdAndTeamName(t.teamId, t.teamName, t.teamCode, t.isDeleted) from Team t where t.fkProjectId.projectId = :projectId and t.isDisabled = false")
    List<TeamIdAndTeamName> findTeamIdTeamNameByFkProjectIdProjectId(Long projectId);

    @Query("Select t.teamId from Team t where t.fkProjectId.projectId = :projectId and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdByFkProjectIdProjectId(Long projectId);

    Boolean existsByTeamIdAndIsDeleted(Long teamId, Boolean isDeleted);

    @Query("SELECT count(t) FROM Team t WHERE t.fkOrgId.orgId = :orgId and t.isDeleted = true")
    Integer findDeletedTeamCountByOrgId (Long orgId);

    @Query("select t from Team t where t.fkProjectId.projectId = :projectId and t.isDisabled = false and t.isDeleted = :isDeleted")
    List<Team> findByFkProjectIdProjectIdAndIsDeleted(Long projectId, Boolean isDeleted);

    @Query("select t from Team t join AccessDomain a on t.teamId = a.entityId where a.entityTypeId = 5 and t.fkOrgId.orgId = :orgId and a.accountId = :accountId and a.isActive = true")
    List<Team> findTeamForUserPreferenceByOrgId(Long orgId, Long accountId, Pageable pageable);

    @Query("select t from Team t join AccessDomain a on t.teamId = a.entityId where a.entityTypeId = 5 and t.fkOrgId.orgId = :orgId and t.fkProjectId.projectId = :projectId and a.accountId = :accountId and a.isActive = true")
    List<Team> findTeamForUserPreferenceByProjectId (Long orgId, Long projectId, Long accountId, Pageable pageable);

    @Query("select t from Team t join AccessDomain a on t.teamId = a.entityId where a.entityTypeId = 5 and a.accountId = :accountId and a.isActive = true")
    List<Team> findTeamForUserPreference( Long accountId, Pageable pageable);

    @Query("select t.fkOrgId.orgId from Team t where t.teamId in :teamIdList and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findFkOrgIdOrgIdByTeamIds(List<Long> teamIdList);

    @Query("select t.fkProjectId.projectId from Team t where t.teamId in :teamIds and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findFkProjectIdProjectIdByTeamIds(List<Long> teamIds);

    @Query("select Distinct t.teamId from Team t where t.fkOrgId.orgId in :orgIdList and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByOrgIds(List<Long> orgIdList);

    @Query("select Distinct t.teamId from Team t where t.fkProjectId.buId in :buIdList and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByBuIds(List<Long> buIdList);

    @Query("SELECT t.fkProjectId.projectId FROM Team t WHERE t.teamId IN :teamIds")
    Set<Long> findProjectIdsByTeamIds(@Param("teamIds") Set<Long> teamIds);

    @Query("select t.teamId from Team t where t.fkProjectId.projectId In :projectIdList and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByProjectIdIn(@Param("projectIdList") Long projectIdList);

    @Query("select Distinct t.teamId from Team t where t.fkProjectId.projectId in :projectIdList and (t.isDeleted = false or t.isDeleted is null) and t.isDisabled = false")
    List<Long> findTeamIdsByProjectIds(List<Long> projectIdList);
}
