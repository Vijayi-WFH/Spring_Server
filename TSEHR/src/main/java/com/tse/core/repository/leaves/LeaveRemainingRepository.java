package com.tse.core.repository.leaves;

import com.tse.core.model.leave.LeaveRemaining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRemainingRepository extends JpaRepository<LeaveRemaining,Long> {

    @Query("select lr from LeaveRemaining lr where lr.accountId=:accountId AND lr.leavePolicyId=:leavePolicyId AND lr.currentlyActive=true")
    LeaveRemaining findByAccountIdAndLeavePolicyId(Long accountId, Long leavePolicyId);

    @Query("select lr from LeaveRemaining lr where lr.accountId=:accountId AND lr.leaveTypeId=:leaveTypeId AND lr.currentlyActive=true")
    LeaveRemaining findByAccountIdAndLeaveType(Long accountId, Short leaveTypeId);

    LeaveRemaining findByAccountIdAndLeaveTypeIdAndCalenderYear(Long accountId, Short leaveTypeId, Short year);

    @Query("select lr from LeaveRemaining lr where lr.accountId=:accountId AND lr.calenderYear=:year")
    List<LeaveRemaining> findByAccountIdAndCalenderYear(Long accountId, Short year);

    List<LeaveRemaining> findAllByLeavePolicyIdAndCalenderYear (Long leavePolicyId, Short year);

    @Query("select lr.accountId from LeaveRemaining lr where lr.leavePolicyId = :leavePolicyId AND lr.currentlyActive=true")
    List<Long> findAllByLeavePolicyId(Long leavePolicyId);

    @Query("select lr.accountId from LeaveRemaining lr where lr.leaveTypeId=:leaveTypeId AND lr.currentlyActive=true")
    List<Long> findByLeaveType(Short leaveTypeId);

    List<LeaveRemaining> findByAccountIdInAndLeaveTypeIdInAndCalenderYear(List<Long> accountIds, List<Short> leaveTypeIds, short currentYear);

    Boolean existsByAccountId(Long accountId);
}
