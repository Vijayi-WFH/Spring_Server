package com.tse.core_application.repository;

import com.tse.core_application.model.SprintCapacityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SprintCapacityMetricsRepository extends JpaRepository<SprintCapacityMetrics, Long> {

    SprintCapacityMetrics findBySprintId(Long sprintId);

    SprintCapacityMetrics findByTeamIdAndSprintId(Long teamId, Long sprintId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SprintCapacityMetrics scm WHERE scm.sprintId IN :sprintIds")
    void deleteBySprintIdIn(List<Long> sprintIds);
}
