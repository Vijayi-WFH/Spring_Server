package com.tse.core_application.repository.geo_fencing.attendance;

import com.tse.core_application.model.geo_fencing.attendance.AttendanceDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AttendanceDay.
 * Phase 6a: Basic CRUD operations.
 */
@Repository
public interface AttendanceDayRepository extends JpaRepository<AttendanceDay, Long> {

    Optional<AttendanceDay> findByOrgIdAndAccountIdAndDateKey(Long orgId, Long accountId, LocalDate dateKey);

    List<AttendanceDay> findByOrgIdAndAccountIdAndDateKeyBetween(Long orgId, Long accountId, LocalDate startDate, LocalDate endDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM AttendanceDay ad WHERE ad.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
