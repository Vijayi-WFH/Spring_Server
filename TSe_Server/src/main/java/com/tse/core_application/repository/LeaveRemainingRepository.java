package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveRemaining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaveRemainingRepository extends JpaRepository<LeaveRemaining,Long> {

    @Query("select lr from LeaveRemaining lr where lr.currentlyActive=:bool")
    List<LeaveRemaining> findByCurrentlyActive(boolean bool);

    // Claude change: PT-14409 - Added method to find leave balance by accountId, leaveTypeId and calendar year
    // Used when updating leave balance after consumed leave edit or delete
    @Query("select lr from LeaveRemaining lr where lr.accountId = :accountId and lr.leaveTypeId = :leaveTypeId and lr.calenderYear = :calenderYear and lr.currentlyActive = true")
    LeaveRemaining findByAccountIdAndLeaveTypeIdAndCalenderYear(Long accountId, Short leaveTypeId, Short calenderYear);

}
