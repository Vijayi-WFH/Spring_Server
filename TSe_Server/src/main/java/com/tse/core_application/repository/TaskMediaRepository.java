package com.tse.core_application.repository;

import com.tse.core_application.model.TaskMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskMediaRepository extends JpaRepository<TaskMedia, Long> {

//    TaskMedia findTaskMediaByTaskNumber(Long taskNumber);

    TaskMedia findTaskMediaByTaskId(Long taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskMedia tm WHERE tm.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);

}
