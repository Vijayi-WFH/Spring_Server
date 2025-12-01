package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveRemainingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRemainingHistoryRepository extends JpaRepository<LeaveRemainingHistory, Long> {
}
