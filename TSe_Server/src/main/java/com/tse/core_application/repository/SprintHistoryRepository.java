package com.tse.core_application.repository;

import com.tse.core_application.model.SprintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SprintHistoryRepository extends JpaRepository<SprintHistory, Long> {

    @Query("SELECT DISTINCT s.version FROM SprintHistory s WHERE s.sprintId = :sprintId ORDER BY s.version DESC")
    List<Long> findDistinctVersionsInDescendingOrder(Long sprintId);

    List<SprintHistory> findBySprintIdAndVersion(Long sprintId, Long versionId);
}
