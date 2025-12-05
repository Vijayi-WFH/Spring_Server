package com.tse.core_application.repository;

import com.tse.core_application.model.DeletedOrganizationStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DeletedOrganizationStats entity.
 * Used for storing and querying statistics of hard-deleted organizations.
 */
@Repository
public interface DeletedOrganizationStatsRepository extends JpaRepository<DeletedOrganizationStats, Long> {

    /**
     * Check if a deleted organization exists with the given name.
     * Used for org name uniqueness validation (org names should remain unique even after deletion).
     */
    @Query("SELECT CASE WHEN COUNT(dos) > 0 THEN true ELSE false END FROM DeletedOrganizationStats dos WHERE dos.orgName = :orgName")
    Boolean existsByOrgName(@Param("orgName") String orgName);

    /**
     * Find deleted organization stats by original org ID.
     */
    Optional<DeletedOrganizationStats> findByOrgId(Long orgId);

    /**
     * Find deleted organization stats by organization name.
     */
    @Query("SELECT dos FROM DeletedOrganizationStats dos WHERE dos.orgName = :orgName")
    Optional<DeletedOrganizationStats> findByOrgName(@Param("orgName") String orgName);
}
