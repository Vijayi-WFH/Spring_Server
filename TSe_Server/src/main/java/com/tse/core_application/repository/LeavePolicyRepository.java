package com.tse.core_application.repository;

import com.tse.core_application.model.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy,Long> {


    LeavePolicy findByLeavePolicyId(Long leavePolicyId);

    List<LeavePolicy> findByTeamId(Long teamId);

}
