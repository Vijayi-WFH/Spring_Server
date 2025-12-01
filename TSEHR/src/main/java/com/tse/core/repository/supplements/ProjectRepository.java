package com.tse.core.repository.supplements;
import com.tse.core.custom.model.ProjectIdProjectName;
import com.tse.core.model.supplements.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("Select NEW com.tse.core.custom.model.ProjectIdProjectName(p.projectId, p.projectName) from Project p where p.orgId = :orgId")
    List<ProjectIdProjectName> findByOrgId(Long orgId);

    @Query("Select p from Project p where p.projectId = :projectId")
    Project findByProjectId(Long projectId);

    @Query("Select p.projectId from Project p where p.buId = :id")
    List<Long> findProjectIdsByBuId(Long id);

    @Query("Select p.buId from Project p where p.projectId = :id")
    Long findBuIdByProjectId(Long id);

    @Query("Select p.orgId from Project p where p.projectId = :id")
    Long findOrgIdByProjectId(Long id);
}
