package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveRemaining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRemainingRepository extends JpaRepository<LeaveRemaining,Long> {

    @Query("select lr from LeaveRemaining lr where lr.currentlyActive=:bool")
    List<LeaveRemaining> findByCurrentlyActive(boolean bool);

    /**
     * Find active leave remaining record for a specific account and leave type.
     * Used when restoring leave balance after deleting a consumed leave.
     */
    @Query("SELECT lr FROM LeaveRemaining lr WHERE lr.accountId = :accountId AND lr.leaveTypeId = :leaveTypeId AND lr.currentlyActive = true")
    Optional<LeaveRemaining> findActiveByAccountIdAndLeaveTypeId(@Param("accountId") Long accountId, @Param("leaveTypeId") Short leaveTypeId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LeaveRemaining lr WHERE lr.accountId IN :accountIds")
    void deleteAllByAccountIdIn(List<Long> accountIds);

}
