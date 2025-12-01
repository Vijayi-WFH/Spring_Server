package com.tse.core.repository.supplements;

import com.tse.core.custom.model.TeamDetails;
import com.tse.core.model.supplements.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Team findByTeamId(long entity);

    @Query("select t.teamId from Team t where t.fkProjectId.projectId = :projectId")
    List<Long> findTeamIdsByProjectId(Long projectId);

    @Query("select t.teamId from Team t where t.fkOrgId.orgId = :orgId")
    List<Long> findTeamIdsByOrgId(Long orgId);

    @Query("select t.teamId from Team t where t.fkProjectId.buId = :buId")
    List<Long> findTeamIdsByBuId(Long buId);

    @Query("select t.fkOrgId.orgId from Team t where t.teamId = :teamId")
    Long findOrgIdByTeamId(Long teamId);

    @Query("SELECT DISTINCT t.teamId FROM Team t WHERE t.fkProjectId.projectId in :projectIds")
    List<Long> findTeamIdsByFkProjectIdProjectIdIn(List<Long> projectIds);

    @Query("select t.fkOrgId.orgId from Team t where t.teamId = :teamId")
    Long findFkOrgIdOrgIdByTeamId(Long teamId);

    @Query("select t.fkProjectId.projectId from Team t where t.teamId = :teamId")
    Long findFkProjectIdProjectIdByTeamId(Long teamId);

    @Query("select t.fkProjectId.buId from Team t where t.teamId = :teamId")
    Long findFkProjectIdBuIdByTeamId(Long teamId);
}
