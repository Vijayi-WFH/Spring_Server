package com.tse.core_application.repository;

import com.tse.core_application.model.performance_notes.TaskRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRatingRepository extends JpaRepository<TaskRating, Long> {

    TaskRating findByTaskRatingId(Integer taskRatingId);


}
