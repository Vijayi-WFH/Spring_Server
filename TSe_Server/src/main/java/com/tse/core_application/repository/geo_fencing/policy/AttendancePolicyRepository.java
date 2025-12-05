package com.tse.core_application.repository.geo_fencing.policy;

import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, Long> {

    Optional<AttendancePolicy> findByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AttendancePolicy ap WHERE ap.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
