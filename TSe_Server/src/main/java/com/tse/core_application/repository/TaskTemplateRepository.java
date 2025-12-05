package com.tse.core_application.repository;

import com.tse.core_application.model.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    TaskTemplate findByTemplateId (Long templateId);

    @Query(value = "select (max (t.templateNumber)) from TaskTemplate t")
    Long getMaxTemplateNumber();

    @Query("SELECT t FROM TaskTemplate t WHERE ((t.fkProjectId.isDeleted IS NULL OR t.fkProjectId.isDeleted = false) AND (t.fkTeamId.isDeleted IS NULL OR t.fkTeamId.isDeleted = false))")
    List<TaskTemplate> findAllTemplatesByFkAccountIdCreatorAccountId (Long accountId);

    List<TaskTemplate> findAllTemplateByEntityIdAndEntityTypeId (Long entityId, Integer entityTypeId);

    @Query("SELECT DISTINCT t FROM TaskTemplate t WHERE ((t.fkProjectId.isDeleted IS NULL OR t.fkProjectId.isDeleted = false) AND (t.fkTeamId.isDeleted IS NULL OR t.fkTeamId.isDeleted = false)) AND (t.fkOrgId.orgId IN :orgIdList OR t.fkProjectId.projectId IN :projectIdList OR t.fkTeamId.teamId IN :teamIdList OR t.fkAccountIdCreator.accountId IN :accountIdList)")
    List<TaskTemplate> findAllUserTemplates(List<Long> orgIdList, List<Long> projectIdList, List<Long> teamIdList, List<Long> accountIdList);

    @Query("SELECT count(t) FROM TaskTemplate t WHERE ((t.fkProjectId.isDeleted IS NULL OR t.fkProjectId.isDeleted = false) AND (t.fkTeamId.isDeleted IS NULL OR t.fkTeamId.isDeleted = false)) AND t.fkOrgId.orgId = :orgId")
    Integer findTaskTemplateCountByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskTemplate tt WHERE tt.fkAccountIdCreator.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}
