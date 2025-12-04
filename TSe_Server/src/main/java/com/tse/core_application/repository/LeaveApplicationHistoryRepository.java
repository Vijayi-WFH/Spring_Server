package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Claude change: PT-14409 - Created repository for LeaveApplicationHistory
 * Provides data access methods for consumed leave edit/delete audit trail
 */
@Repository
public interface LeaveApplicationHistoryRepository extends JpaRepository<LeaveApplicationHistory, Long> {

    // Claude change: Get all history records for a specific leave application
    List<LeaveApplicationHistory> findByLeaveApplicationIdOrderByUpdatedOnDesc(Long leaveApplicationId);

    // Claude change: Get all history records made by a specific admin
    List<LeaveApplicationHistory> findByUpdatedByAccountIdOrderByUpdatedOnDesc(Long updatedByAccountId);

    // Claude change: Get history for a specific employee's leaves (for report filtering by employee)
    @Query("SELECT lah FROM LeaveApplicationHistory lah " +
           "WHERE lah.leaveApplicationId IN " +
           "(SELECT la.leaveApplicationId FROM com.tse.core_application.model.LeaveApplication la " +
           "WHERE la.accountId = :accountId) " +
           "ORDER BY lah.updatedOn DESC")
    List<LeaveApplicationHistory> findByEmployeeAccountId(@Param("accountId") Long accountId);
}
