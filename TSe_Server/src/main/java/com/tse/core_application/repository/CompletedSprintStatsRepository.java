package com.tse.core_application.repository;

import com.tse.core_application.model.CompletedSprintStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CompletedSprintStatsRepository extends JpaRepository<CompletedSprintStats, Long> {
    CompletedSprintStats findBySprintId(Long sprintId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CompletedSprintStats css WHERE css.sprintId IN :sprintIds")
    void deleteBySprintIdIn(List<Long> sprintIds);
}
