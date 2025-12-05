package com.tse.core_application.repository;

import com.tse.core_application.custom.model.TaskHistoryMappingKeyColumnsDesc;
import com.tse.core_application.model.TaskHistoryColumnsMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskHistoryColumnsMappingRepository extends JpaRepository<TaskHistoryColumnsMapping, Integer> {

    @Query("select DISTINCT new com.tse.core_application.custom.model.TaskHistoryMappingKeyColumnsDesc(t.taskHistoryColumnsMappingKey, t.columnsDesc) from TaskHistoryColumnsMapping t where t.isActive = :isActive")
    List<TaskHistoryMappingKeyColumnsDesc> getAllMappingKeysAndColumnDesc(Integer isActive);

    List<TaskHistoryColumnsMapping> findAll();

    List<TaskHistoryColumnsMapping> findColumnNameByTaskHistoryColumnsMappingKeyInAndIsActive(List<Integer> taskHistoryColumnsMappingKey, Integer isActive);

    List<TaskHistoryColumnsMapping> findByTaskHistoryColumnsMappingKeyIn(List<Integer> taskHistoryColumnsMappingKey);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskHistoryColumnsMapping thcm WHERE thcm.taskHistoryId IN :taskHistoryIds")
    void deleteByTaskHistoryIdIn(List<Long> taskHistoryIds);
}
