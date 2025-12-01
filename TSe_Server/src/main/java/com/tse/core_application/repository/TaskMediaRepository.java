package com.tse.core_application.repository;

import com.tse.core_application.model.TaskMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskMediaRepository extends JpaRepository<TaskMedia, Long> {

//    TaskMedia findTaskMediaByTaskNumber(Long taskNumber);

    TaskMedia findTaskMediaByTaskId(Long taskId);

}
