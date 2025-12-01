package com.tse.core_application.repository;

import com.tse.core_application.model.SprintCapacityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SprintCapacityMetricsRepository extends JpaRepository<SprintCapacityMetrics, Long> {

    SprintCapacityMetrics findBySprintId(Long sprintId);

    SprintCapacityMetrics findByTeamIdAndSprintId(Long teamId, Long sprintId);
}
