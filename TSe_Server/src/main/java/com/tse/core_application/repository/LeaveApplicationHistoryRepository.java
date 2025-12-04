package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for LeaveApplicationHistory entity.
 * Provides methods to query leave edit/delete audit trail.
 */
@Repository
public interface LeaveApplicationHistoryRepository extends JpaRepository<LeaveApplicationHistory, Long> {

    /**
     * Find all history records for a specific leave application
     */
    List<LeaveApplicationHistory> findByLeaveApplicationIdOrderByUpdatedOnDesc(Long leaveApplicationId);

    /**
     * Find all history records created by a specific admin
     */
    List<LeaveApplicationHistory> findByUpdatedByAccountIdOrderByUpdatedOnDesc(Long accountId);

    /**
     * Find history records for multiple leave applications (batch query for report)
     */
    List<LeaveApplicationHistory> findByLeaveApplicationIdInOrderByUpdatedOnDesc(List<Long> leaveApplicationIds);

    /**
     * Find history records for leaves belonging to a specific employee
     * Used when employee wants to see their own leave history
     */
    @Query("SELECT lah FROM LeaveApplicationHistory lah " +
            "JOIN LeaveApplication la ON lah.leaveApplicationId = la.leaveApplicationId " +
            "WHERE la.accountId = :employeeAccountId " +
            "ORDER BY lah.updatedOn DESC")
    List<LeaveApplicationHistory> findByEmployeeAccountId(@Param("employeeAccountId") Long employeeAccountId);

    /**
     * Find history records for leaves belonging to a specific employee within a date range
     * Filters based on old/new from dates within the range
     */
    @Query("SELECT lah FROM LeaveApplicationHistory lah " +
            "JOIN LeaveApplication la ON lah.leaveApplicationId = la.leaveApplicationId " +
            "WHERE la.accountId = :employeeAccountId " +
            "AND (lah.oldFromDate BETWEEN :fromDate AND :toDate " +
            "OR lah.newFromDate BETWEEN :fromDate AND :toDate) " +
            "ORDER BY lah.updatedOn DESC")
    List<LeaveApplicationHistory> findByEmployeeAccountIdAndDateRange(
            @Param("employeeAccountId") Long employeeAccountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Find all history records within a date range (for admin report)
     */
    @Query("SELECT lah FROM LeaveApplicationHistory lah " +
            "WHERE lah.oldFromDate BETWEEN :fromDate AND :toDate " +
            "OR lah.newFromDate BETWEEN :fromDate AND :toDate " +
            "ORDER BY lah.updatedOn DESC")
    List<LeaveApplicationHistory> findByDateRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
