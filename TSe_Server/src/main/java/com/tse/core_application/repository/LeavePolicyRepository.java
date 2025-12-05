package com.tse.core_application.repository;

import com.tse.core_application.model.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LeavePolicyRepository extends JpaRepository<LeavePolicy,Long> {


    LeavePolicy findByLeavePolicyId(Long leavePolicyId);

    List<LeavePolicy> findByTeamId(Long teamId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LeavePolicy lp WHERE lp.orgId = :orgId")
    void deleteAllByOrgId(Long orgId);

}
