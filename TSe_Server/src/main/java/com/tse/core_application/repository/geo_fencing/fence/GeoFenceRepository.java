package com.tse.core_application.repository.geo_fencing.fence;

import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeoFenceRepository extends JpaRepository<GeoFence, Long>, JpaSpecificationExecutor<GeoFence> {

    Optional<GeoFence> findByIdAndOrgId(Long id, Long orgId);

    List<GeoFence> findByOrgIdAndIdIn(Long orgId, Collection<Long> ids);

    List<GeoFence> findByOrgIdAndIdInAndIsActiveTrue(Long orgId, Collection<Long> ids);

    List<GeoFence> findByOrgId(Long orgId);
}
