package com.tse.core_application.repository.geo_fencing.policy;

import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, Long> {

    Optional<AttendancePolicy> findByOrgId(Long orgId);
}
