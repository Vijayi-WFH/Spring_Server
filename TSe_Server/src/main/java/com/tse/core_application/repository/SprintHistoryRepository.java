package com.tse.core_application.repository;

import com.tse.core_application.model.SprintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SprintHistoryRepository extends JpaRepository<SprintHistory, Long> {

    @Query("SELECT DISTINCT s.version FROM SprintHistory s WHERE s.sprintId = :sprintId ORDER BY s.version DESC")
    List<Long> findDistinctVersionsInDescendingOrder(Long sprintId);

    List<SprintHistory> findBySprintIdAndVersion(Long sprintId, Long versionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SprintHistory sh WHERE sh.sprintId IN :sprintIds")
    void deleteBySprintIdIn(List<Long> sprintIds);
}
