package com.tse.core.repository.leaves;

import com.tse.core.model.leave.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeavePolicyRepository extends JpaRepository<LeavePolicy,Long> {
    LeavePolicy findByLeavePolicyId(Long leavePolicyId);

    List<LeavePolicy> findByOrgId(Long orgId);

    @Query("select lp from LeavePolicy lp where lp.leavePolicyId=:leavePolicyId")
    LeavePolicy findLeavePolicyById(Long leavePolicyId);

    @Query("select lp from LeavePolicy lp where lp.leavePolicyId in " +
            "(select lr.leavePolicyId from LeaveRemaining lr where lr.accountId=:accountId)")
    List<LeavePolicy> findLeavePolicyByAccountId(Long accountId);

    @Query("select lp from LeavePolicy lp where lp.orgId=:orgId AND buId is null AND projectId is null and teamId is null")
    List<LeavePolicy> findByOrgIdBuIdProjectIdAndTeamId(Long orgId);

    @Query("select lp.initialLeaves from LeavePolicy lp where lp.leavePolicyId=:leavePolicyId")
    Float findInitialLeavesByLeavePolicyId(Long leavePolicyId);

}
