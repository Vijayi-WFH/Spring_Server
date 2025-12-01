package com.tse.core.repository.leaves;

import com.tse.core.model.leave.LeaveApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication,Long>, LeaveApplicationRepositoryCustom {

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.fromDate >=:fromDate and la.fromDate<=:toDate")
    List<LeaveApplication> findByAccountIdAndFromToDate(Long accountId, LocalDate fromDate, LocalDate toDate);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and :date between la.fromDate and la.toDate")
    List<LeaveApplication> findByAccountIdAndDate(Long accountId, LocalDate date);

    @Query("select la.doctorCertificate from LeaveApplication la where la.leaveApplicationId=:applicationId")
    byte[] findDoctorCertificateByLeaveApplicationId(Long applicationId);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.leaveApplicationStatusId=:defaultLeaveApplicationStatusId")
    List<LeaveApplication> findByAccountIdAndApplicationStatus(Long accountId, Short defaultLeaveApplicationStatusId);

    //added repository method for getApprovedleaves
    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.leaveApplicationStatusId=:defaultLeaveApplicationStatusId AND la.toDate>current_date AND extract(year from la.toDate)=extract(year from current_date)")
    List<LeaveApplication> findByAccountIdAndApplicationStatusForYear(Long accountId, Short defaultLeaveApplicationStatusId );


    @Query("select la from LeaveApplication la where la.approverAccountId=:approverAccountId and la.leaveApplicationStatusId=:leaveApplicationStatusId")
    List<LeaveApplication> findByApproverAccountIdAndApplicationStatus(Long approverAccountId, Short leaveApplicationStatusId);

    LeaveApplication findByLeaveApplicationId(Long leaveApplicationId);

    @Modifying
    @Transactional
    @Query("update LeaveApplication la set la.leaveApplicationStatusId =:leaveApplicationStatusId, la.leaveCancellationReason = :leaveCancellationReason where la.leaveApplicationId=:applicationId")
    Integer changeLeaveApplicationStatus(Short leaveApplicationStatusId, String leaveCancellationReason, Long applicationId);

    List<LeaveApplication> findByAccountId(Long accountId);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.fromDate <=:todayDate and la.toDate>=:todayDate and la.leaveApplicationStatusId IN :statusIdList")
    List<LeaveApplication> findByAccountIdAndDate(Long accountId, LocalDate todayDate, List<Short> statusIdList);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.leaveTypeId=:leaveTypeId and la.leaveApplicationStatusId=:waitingApprovalLeaveApplicationStatusId")
    List<LeaveApplication> findByAccountIdAndLeaveTypeIdAndApplicationStatus(Long accountId, Short leaveTypeId, Short waitingApprovalLeaveApplicationStatusId);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.isLeaveForHalfDay = :isLeaveForHalfDay and :date between la.fromDate and la.toDate")
    List<LeaveApplication> findByAccountIdAndisLeaveForHalfDayAndDate(Long accountId, Boolean isLeaveForHalfDay, LocalDate date);

    @Query("select coalesce(sum(coalesce(la.numberOfLeaveDays, 0)), 0) from LeaveApplication la where la.accountId=:accountId and la.leaveApplicationStatusId in :leaveApplicationStatusIds and la.fromDate between :fromDate and :toDate and la.leaveTypeId=:leaveTypeId")
    Float findCountByLeaveTypeAndApplicationStatusIdInAndYear(Long accountId, List<Short> leaveApplicationStatusIds, LocalDate fromDate, LocalDate toDate,Short leaveTypeId);

    @Query("select la from LeaveApplication la where la.accountId=:accountId and la.leaveApplicationStatusId in :leaveApplicationStatusIds and la.fromDate between :fromDate and :toDate")
    List<LeaveApplication> findCountByLeaveTypeAndApplicationStatusIdInAndYear1(Long accountId, List<Short> leaveApplicationStatusIds, LocalDate fromDate, LocalDate toDate);

    @Query(" select la from LeaveApplication la where la.accountId = :accountId and la.leaveApplicationStatusId in :statusIds and la.leaveTypeId = :leaveTypeId and la.toDate >= :startOfNextYear and la.toDate <= :endOfNextYear ")
    List<LeaveApplication> findNextYearLeaves(
            @Param("accountId") Long accountId,
            @Param("leaveTypeId") Short leaveTypeId,
            @Param("statusIds") List<Short> statusIds,
            @Param("startOfNextYear") LocalDate startOfNextYear,
            @Param("endOfNextYear") LocalDate endOfNextYear
    );

    Page<Object[]> findLeaveApplicationsExpanded(Long orgId, LocalDate fromDate, LocalDate toDate, List<Long> accountIdList, List<Long> approverAccountIdList, List<Short> leaveStatusIdList, Sort unsorted, Pageable unpaged);
}
