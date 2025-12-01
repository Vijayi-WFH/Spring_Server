package com.tse.core.repository.leaves;

import com.tse.core.model.leave.LeaveApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveApplicationStatusRepository extends JpaRepository<LeaveApplicationStatus,Short> {
    LeaveApplicationStatus findByLeaveApplicationStatus(String leaveApplicationStatus);

    LeaveApplicationStatus findByLeaveApplicationStatusId(Short leaveApplicationStatusId);
}
