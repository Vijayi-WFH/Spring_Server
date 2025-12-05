package com.tse.core_application.repository;

import com.tse.core_application.custom.model.EnvironmentIdDescDisplayAs;
import com.tse.core_application.model.CustomEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashMap;
import java.util.List;

public interface CustomEnvironmentRepository extends JpaRepository<CustomEnvironment,Integer> {
    CustomEnvironment findByEnvironmentDisplayNameAndEntityId(String environmentDisplayName,Long entityId);

    @Query("SELECT c FROM CustomEnvironment c WHERE c.entityId = :entityId")
    List<CustomEnvironment> findAllByEntityId(@Param("entityId") Long entityId);

    List<CustomEnvironment>findAllByEntityIdAndIsActive(Long entityId,Boolean isActive);
    @Query("SELECT c FROM CustomEnvironment c " +
            "WHERE LOWER(c.environmentDisplayName) = :name " +
            "AND c.entityId = :entityId")
    CustomEnvironment findByLowerEnvironmentDisplayNameAndEntityId(@Param("name") String name, @Param("entityId") Long entityId);

    @Query("select NEW com.tse.core_application.custom.model.EnvironmentIdDescDisplayAs(e.customEnvironmentId, e.environmentDescription, e.environmentDisplayName, e.isActive) from CustomEnvironment e WHERE e.entityId = :entityId")
    List<EnvironmentIdDescDisplayAs> getEnvironmentIdDescDisplayAs(Long entityId);

    boolean existsByEntityTypeIdAndEntityIdAndIsActiveAndCustomEnvironmentId(
            Integer entityTypeId,
            Long entityId,
            Boolean isActive,
            Integer customEnvironmentId
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM CustomEnvironment ce WHERE ce.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
