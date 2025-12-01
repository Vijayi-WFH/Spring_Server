package com.tse.core_application.repository;

import com.tse.core_application.model.TaskAttachmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface TaskAttachmentHistoryRepository extends JpaRepository<TaskAttachmentHistory, Long> {

    @Query("SELECT MAX(t.version) FROM TaskAttachmentHistory t WHERE t.taskId = :taskId")
    Long findMaxVersionByTaskId(Long taskId);

    @Query("SELECT DISTINCT t.version FROM TaskAttachmentHistory t WHERE t.taskId = :taskId ORDER BY t.version DESC")
    List<Long> findDistinctVersionsInDescendingOrder(Long taskId);

    List<TaskAttachmentHistory> findByTaskIdAndVersion(Long taskId, Long version);
}
