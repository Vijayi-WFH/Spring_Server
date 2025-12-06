package com.tse.core_application.repository;

import com.tse.core_application.model.DeletedOrganizationStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeletedOrganizationStatsRepository extends JpaRepository<DeletedOrganizationStats, Long> {

    Optional<DeletedOrganizationStats> findByOrgId(Long orgId);

    @Query("SELECT d.organizationName FROM DeletedOrganizationStats d")
    List<String> findAllDeletedOrganizationNames();

    boolean existsByOrganizationName(String organizationName);
}
