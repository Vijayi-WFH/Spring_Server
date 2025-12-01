package com.tse.core_application.repository.geo_fencing.attendance;

import com.tse.core_application.model.geo_fencing.attendance.AttendanceDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
