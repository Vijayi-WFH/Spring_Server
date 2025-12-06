package com.tse.core_application.repository;

import com.tse.core_application.model.performance_notes.TaskRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskRatingRepository extends JpaRepository<TaskRating, Long> {

    TaskRating findByTaskRatingId(Integer taskRatingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskRating tr WHERE tr.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);

}
