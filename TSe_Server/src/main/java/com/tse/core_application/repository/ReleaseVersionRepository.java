package com.tse.core_application.repository;

import com.tse.core_application.model.ReleaseVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseVersionRepository extends JpaRepository<ReleaseVersion, Long> {
    Boolean existsByEntityTypeIdAndEntityIdAndReleaseVersionName(Integer entityTypeId, Long entityId, String releaseVersionName);

    List<ReleaseVersion> findByEntityTypeIdAndEntityIdOrderByCreatedDateTimeDesc(Integer entityTypeId, Long entityId);
}
