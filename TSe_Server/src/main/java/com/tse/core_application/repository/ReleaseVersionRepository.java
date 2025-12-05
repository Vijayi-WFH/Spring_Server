package com.tse.core_application.repository;

import com.tse.core_application.model.ReleaseVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ReleaseVersionRepository extends JpaRepository<ReleaseVersion, Long> {
    Boolean existsByEntityTypeIdAndEntityIdAndReleaseVersionName(Integer entityTypeId, Long entityId, String releaseVersionName);

    List<ReleaseVersion> findByEntityTypeIdAndEntityIdOrderByCreatedDateTimeDesc(Integer entityTypeId, Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReleaseVersion rv WHERE rv.entityTypeId = :entityTypeId AND rv.entityId IN :entityIds")
    void deleteByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityIds);
}
