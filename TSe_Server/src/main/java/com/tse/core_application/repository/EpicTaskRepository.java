package com.tse.core_application.repository;

import com.tse.core_application.model.EpicTask;
import com.tse.core_application.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EpicTaskRepository extends JpaRepository<EpicTask, Long> {
    List<Task> findFkTaskIdByFkEpicIdEpicId(Long epicId);

    @Query("SELECT e.fkTaskId FROM EpicTask e WHERE e.fkEpicId.epicId = :epicId AND e.isDeleted = :isDeleted")
    List<Task> findFkTaskIdByFkEpicIdEpicIdAndIsDeleted(Long epicId, Boolean isDeleted);

    Boolean existsByFkEpicIdEpicIdAndFkTaskIdTaskId(Long epicId, Long taskId);

    EpicTask findByFkEpicIdEpicIdAndFkTaskIdTaskId(Long epicId, Long taskId);

    @Query("SELECT e.fkTaskId FROM EpicTask e WHERE e.fkEpicId.epicId = :epicId AND e.fkTaskId.fkTeamId.teamId IN :userTeamIdList AND e.isDeleted = :isDeleted")
    List<Task> findFkTaskIdByFkEpicIdEpicIdAndFkTeamIdTeamIdInAndIsDeleted(Long epicId, List<Long> userTeamIdList, Boolean isDeleted);

    @Query("SELECT e.fkTaskId FROM EpicTask e WHERE e.fkEpicId.epicId = :epicId AND e.fkTaskId.fkTeamId.teamId IN :userTeamIdList AND e.fkTaskId.taskTypeId <> 3 AND e.isDeleted = :isDeleted")
    List<Task> findFkTaskIdByFkEpicIdEpicIdAndFkTeamIdTeamIdInAndIsDeletedWithoutChildTask(Long epicId, List<Long> userTeamIdList, Boolean isDeleted);
}
