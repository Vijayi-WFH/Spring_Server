package com.tse.core_application.repository;

import com.tse.core_application.model.Epic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EpicRepository extends JpaRepository<Epic, Long> {

    Epic findByEpicId(Long epicId);

    @Query("SELECT e.fkOrgId.orgId from Epic e where e.epicId = :epicId")
    Long findFkOrgIdOrgIdByEpicId(Long epicId);

    @Query("SELECT e.teamIdList from Epic e where e.epicId = :epicId")
    List<Long> findTeamIdListByEpicId(Long epicId);

    String findFkWorkflowEpicStatusWorkflowEpicStatusByEpicId(Long epicId);

    @Query("SELECT count(e) FROM Epic e WHERE e.fkOrgId.orgId = :orgId")
    Integer findEpicsCountByOrgId(Long orgId);

    List<Epic> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query(value = "SELECT * from tse.epic e WHERE :teamId = ANY(string_to_array(e.team_id_list, ','))", nativeQuery = true)
    List<Epic> findByTeamId(String teamId);

    List<Epic> findByFkProjectIdProjectId(Long projectId);

    List<Epic> findByfkOrgIdOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM tse.epic e WHERE EXISTS (SELECT 1 FROM unnest(string_to_array(e.team_id_list, ',')) AS team_id WHERE team_id::bigint IN :teamIds)", nativeQuery = true)
    void deleteAllByTeamIdIn(List<Long> teamIds);
}
