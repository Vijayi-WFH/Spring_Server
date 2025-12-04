package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveApplication;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    @Query("SELECT la FROM LeaveApplication la WHERE la.leaveApplicationStatusId = :leaveApplicationStatusId AND la.fromDate <= :date")
    List<LeaveApplication> findByFromDateLessThanEqualAndLeaveApplicationStatusId(LocalDate date, Short leaveApplicationStatusId);

    @Query("SELECT la FROM LeaveApplication la WHERE la.leaveApplicationStatusId = :leaveApplicationStatusId AND la.fromDate BETWEEN :fromDate AND :toDate")
    List<LeaveApplication> findByFromDateBetweenAndLeaveApplicationStatusId(LocalDate fromDate, LocalDate toDate, Short leaveApplicationStatusId);

    @Query("SELECT count(l) FROM LeaveApplication l WHERE l.accountId in :accountIdsList")
    Integer findLeavesCountByAccountIdIn(List<Long> accountIdsList);

    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.leaveApplicationStatusId = :leaveApplicationStatusId " +
            "AND la.expiryLeaveDate = :today")
    List<LeaveApplication> findByExpiryLeaveDateAndStatus(
            @Param("today") LocalDate today,
            @Param("leaveApplicationStatusId") Short leaveApplicationStatusId
    );

    @Query("SELECT la FROM LeaveApplication la WHERE la.leaveApplicationStatusId = :leaveApplicationStatusId AND la.fromDate < :date")
    List<LeaveApplication> findLeavesToSetConsumed(LocalDate date, Short leaveApplicationStatusId);

    @Query("select count(la) from LeaveApplication la where la.accountId In :accountIds and la.fromDate <= :todayDate and la.toDate >= :todayDate and la.leaveApplicationStatusId in :statusIdList")
    Integer findMemberOnLeaveByAccountIdsAndDate(List<Long> accountIds, LocalDate todayDate, List<Short> statusIdList);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.fromDate <=:todayDate and la.toDate>=:todayDate and la.leaveApplicationStatusId IN :statusIdList")
    List<LeaveApplication> findByAccountIdAndDate(Long accountId, LocalDate todayDate, List<Short> statusIdList);

    @Query("SELECT l FROM LeaveApplication l WHERE l.accountId = :accountId AND l.leaveApplicationStatusId IN :statusIds AND (l.fromDate <= :toDate AND l.toDate >= :fromDate)")
    List<LeaveApplication> findByAccountIdAndOverlappingDateRange(Long accountId, LocalDate fromDate, LocalDate toDate, List<Short> statusIds);

    @Query("SELECT l FROM LeaveApplication l WHERE l.accountId IN :accountIds AND l.leaveApplicationStatusId IN :statusIds AND (l.fromDate <= :toDate AND l.toDate >= :fromDate)")
    List<LeaveApplication> findByAccountIdInAndOverlappingDateRange(List<Long> accountIds, LocalDate fromDate, LocalDate toDate, List<Short> statusIds);

    LeaveApplication findByLeaveApplicationId(Long leaveApplicationId);

    @Query("select case when count(la) > 0 then true else false end from LeaveApplication la where la.accountId in :accountIds and la.fromDate <= :todayDate and la.toDate >= :todayDate and la.leaveApplicationStatusId in :statusIdList")
    boolean existsByAccountIdsAndDate(
            List<Long> accountIds,
            LocalDate todayDate,
            List<Short> statusIdList
    );

    List<LeaveApplication> findByAccountIdInAndApproverAccountIdInAndLeaveApplicationStatusIdIn(List<Long> accountIdList, List<Long> removedApproverAccountIdList, List<Short> leaveApplicationStatusIdList);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LeaveApplication la " +
            "set la.approverAccountId = :newApproverAccountId " +
            "where la.accountId in :accountIds " +
            "and la.approverAccountId in :removedApproverAccountIds " +
            "and la.leaveApplicationStatusId in :statusIds")
    int bulkUpdateApprover(@Param("accountIds") List<Long> accountIds,
                           @Param("removedApproverAccountIds") List<Long> removedApproverAccountIds,
                           @Param("statusIds") List<Short> statusIds,
                           @Param("newApproverAccountId") Long newApproverAccountId);
}
