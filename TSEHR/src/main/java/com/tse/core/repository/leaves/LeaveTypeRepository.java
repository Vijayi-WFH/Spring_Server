package com.tse.core.repository.leaves;

import com.tse.core.model.leave.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType,Short> {
    LeaveType findByLeaveTypeId(Short leaveTypeId);
}
