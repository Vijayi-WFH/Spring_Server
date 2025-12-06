package com.tse.core_application.repository;

import com.tse.core_application.custom.model.OrgId;
import com.tse.core_application.custom.model.ProjectIdProjectName;
import com.tse.core_application.model.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("select p from Project p where p.projectId in :projectIds and p.isDisabled = false")
    List<Project> findByProjectIdIn(List<Long> projectIds);

    /**
     * This method will find all the projects for the given list of orgIds.
     *
     * @param orgIds the list of orgIds.
     * @return List<Project>
     */
    @Query("Select p from Project p where p.orgId in :orgIds And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<Project> findByOrgIdIn(List<Long> orgIds);

    @Query("Select p from Project p where p.buId in :buIds And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<Project> findByBuIdIn(List<Long> buIds);

    @Query("Select NEW com.tse.core_application.custom.model.ProjectIdProjectName(p.projectId, p.projectName, p.isDeleted) from Project p where p.orgId = :orgId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<ProjectIdProjectName> findByOrgId(Long orgId);

    @Query("Select p from Project p where p.projectId = :projectId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    Project findByProjectId(Long projectId);

    @Query("Select p from Project p where p.buId = :buId And p.orgId = :orgId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<Project> findByBuIdAndOrgId(Long buId, Long orgId);

    @Query("Select p from Project p where p.projectId = :projectId And p.orgId = :orgId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    Project findByProjectIdAndOrgId(Long projectId, Long orgId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE p.orgId = :orgId AND p.projectId = :projectId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = :isDisabled")
    boolean existsByOrgIdAndProjectIdAndIsDisabled(Long orgId, Long projectId, Boolean isDisabled);

    @Query("Select new com.tse.core_application.custom.model.OrgId(p.orgId) from Project p where p.projectId = :projectId And (p.isDeleted = false Or p.isDeleted is null)")
    OrgId findOrgIdByProjectId(Long projectId);

    @Query("select p.projectName from Project p where p.projectId = :projectId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    String findProjectNameByProjectId(Long projectId);

    @Query("SELECT DISTINCT p.projectId FROM Project p where p.buId = :buId And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<Long> findProjectIdsByBuId(Long buId);

    @Query("Select p from Project p where p.orgId = :orgId And (p.isDeleted = false Or p.isDeleted is null)")
    Project findOneByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("update Project p set p.ownerAccountId =:ownerAccount where p.projectId=:projectId")
    void updateOwnerAccountIdByProjectId(Long ownerAccount, Long projectId);

    @Modifying
    @Transactional
    @Query("update Project p set p.isDisabled = :isDisabled where p.orgId = :orgId")
    void updateIsDisabledByOrgId (Long orgId, Boolean isDisabled);

    @Query("SELECT count(p) < :projectLimit FROM Project p WHERE p.orgId = :orgId")
    Boolean isProjectRegistrationAllowed (Long orgId, Long projectLimit);

    @Query("SELECT count(p) FROM Project p WHERE p.orgId = :orgId And (p.isDeleted = false Or p.isDeleted is null)")
    Integer findProjectCountByOrgId (Long orgId);

    @Query("SELECT count(p) FROM Project p WHERE p.orgId = :orgId And p.isDeleted = true")
    Integer findDeletedProjectCountByOrgId (Long orgId);

    @Query("select p from Project p join AccessDomain a on p.projectId = a.entityId where a.entityTypeId = 4 and p.orgId = :orgId and a.accountId = :accountId and a.isActive = true")
    List<Project> findProjectForUserPreference (Long orgId, Long accountId, Pageable pageable);

    List<Project> findByBuIdAndIsDeleted(Long buId, Boolean isDelete);

    @Query("SELECT p.buId FROM Project p WHERE p.projectId IN :projectIds")
    Set<Long> findBuByProjectId(@Param("projectIds") Set<Long> projectIds);

    @Query("SELECT DISTINCT p.projectId FROM Project p where p.orgId In :orgIds And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<Long> findProjectIdsByOrgIds(List<Long> orgIds);

    @Query("SELECT DISTINCT p.projectId FROM Project p where p.buId In :buIdList And (p.isDeleted = false Or p.isDeleted is null) and p.isDisabled = false")
    List<Long> findProjectIdsByBuIdIn(List<Long> buIdList);

    @Query("SELECT p.projectId FROM Project p WHERE p.orgId = :orgId")
    List<Long> findAllProjectIdsByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Project p WHERE p.orgId = :orgId")
    void deleteAllByOrgId(Long orgId);
}
