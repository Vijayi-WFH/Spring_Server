package com.tse.core_application.repository.geo_fencing.assignment;

import com.tse.core_application.model.geo_fencing.assignment.FenceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FenceAssignmentRepository extends JpaRepository<FenceAssignment, Long> {

    Optional<FenceAssignment> findByOrgIdAndFenceIdAndEntityTypeIdAndEntityId(
            Long orgId, Long fenceId, Integer entityTypeId, Long entityId);

    List<FenceAssignment> findByOrgIdAndEntityTypeIdAndEntityId(
            Long orgId, Integer entityTypeId, Long entityId);

    List<FenceAssignment> findByOrgIdAndFenceId(Long orgId, Long fenceId);

    @Modifying
    @Query("UPDATE FenceAssignment fa SET fa.isDefault = false " +
           "WHERE fa.orgId = :orgId AND fa.entityTypeId = :entityTypeId " +
           "AND fa.entityId = :entityId AND fa.isDefault = true")
    void unsetDefaultForEntity(@Param("orgId") Long orgId,
                               @Param("entityTypeId") Integer entityTypeId,
                               @Param("entityId") Long entityId);

    @Query("SELECT fa.fenceId FROM FenceAssignment fa " +
           "WHERE fa.orgId = :orgId AND fa.entityTypeId = :entityTypeId AND fa.entityId = :entityId")
    List<Long> findFenceIdsByEntity(@Param("orgId") Long orgId,
                                     @Param("entityTypeId") Integer entityTypeId,
                                     @Param("entityId") Long entityId);

    Optional<FenceAssignment> findByOrgIdAndEntityTypeIdAndEntityIdAndIsDefault(
            Long orgId, Integer entityTypeId, Long entityId, Boolean isDefault);

    List<FenceAssignment> findByOrgIdAndEntityTypeIdAndEntityIdIn(
            Long orgId, Integer entityTypeId, Collection<Long> entityIds);

    @Query("SELECT fa FROM FenceAssignment fa " +
           "WHERE fa.orgId = :orgId AND fa.entityTypeId IN :typeIds AND fa.entityId IN :entityIds")
    List<FenceAssignment> findByOrgIdAndEntityTypeIdInAndEntityIdIn(
            @Param("orgId") Long orgId,
            @Param("typeIds") Collection<Integer> typeIds,
            @Param("entityIds") Collection<Long> entityIds);

    List<FenceAssignment> findByOrgIdAndFenceIdIn(Long orgId, Collection<Long> fenceIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM FenceAssignment fa WHERE fa.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
