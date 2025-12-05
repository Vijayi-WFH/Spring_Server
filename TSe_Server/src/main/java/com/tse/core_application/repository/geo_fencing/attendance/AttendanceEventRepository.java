package com.tse.core_application.repository.geo_fencing.attendance;

import com.tse.core_application.model.geo_fencing.attendance.AttendanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AttendanceEvent.
 * Phase 6a: Basic CRUD operations.
 */
@Repository
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    List<AttendanceEvent> findByOrgIdAndAccountIdOrderByTsUtcDesc(Long orgId, Long accountId);

    List<AttendanceEvent> findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
            Long orgId, Long accountId, LocalDateTime start, LocalDateTime end);

    @Modifying
    @Transactional
    @Query("DELETE FROM AttendanceEvent ae WHERE ae.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
