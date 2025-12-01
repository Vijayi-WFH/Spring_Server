package com.tse.core_application.repository;

import com.tse.core_application.model.CompletedSprintStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompletedSprintStatsRepository extends JpaRepository<CompletedSprintStats, Long> {
    CompletedSprintStats findBySprintId(Long sprintId);
}
