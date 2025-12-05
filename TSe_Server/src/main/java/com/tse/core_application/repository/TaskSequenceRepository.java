package com.tse.core_application.repository;

import com.tse.core_application.model.TaskSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
public interface TaskSequenceRepository extends JpaRepository<TaskSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TaskSequence ts WHERE ts.teamId = :teamId")
    TaskSequence findByTeamIdForUpdate(@Param("teamId") Long teamId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskSequence ts WHERE ts.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);

}

