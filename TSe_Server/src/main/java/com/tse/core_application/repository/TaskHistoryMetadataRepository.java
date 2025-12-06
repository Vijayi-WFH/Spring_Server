package com.tse.core_application.repository;

import com.tse.core_application.model.TaskHistoryMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskHistoryMetadataRepository extends JpaRepository<TaskHistoryMetadata, Long> {

    List<TaskHistoryMetadata> findByTaskHistoryColumnsMappingIdAndTaskId(Integer mappingId, Long taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskHistoryMetadata thm WHERE thm.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);
}
