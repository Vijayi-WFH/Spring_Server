package com.tse.core_application.repository;

import com.tse.core_application.model.TaskHistory;
import com.tse.core_application.model.WorkFlowTaskStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory,Long>, TaskHistoryCustomInterface {

      List<TaskHistory> findByTaskIdOrderByVersionDesc(Long taskId);
      public List<TaskHistory> findByTaskIdOrderByVersionAsc(long taskId);

      @Query(value = "SELECT * FROM tse.task_history t WHERE t.task_id = :taskId AND t.version = :version ORDER BY created_date_time DESC LIMIT 1", nativeQuery = true) //temporary fix
      public TaskHistory findByTaskIdAndVersion(Long taskId, Long version);

//      List<TaskHistory> findByTaskNumberOrderByVersionAsc(Long taskNumber);

//      List<TaskHistory> findByTaskNumberAndVersionIn(Long taskNumber, List<Long> version);
      List<TaskHistory> findByTaskIdAndVersionInOrderByVersionAscCreatedDateTimeAsc(Long taskId, List<Long> version);

//      TaskHistory findByTaskNumberAndVersion(Long taskNumber, Long version);

      @Query("SELECT th.fkWorkflowTaskStatus FROM TaskHistory th WHERE th.taskId = :taskId AND th.fkWorkflowTaskStatus.workflowTaskStatusId NOT IN :blockedStatusIds ORDER BY th.version DESC")
      List<WorkFlowTaskStatus> findWorkflowToRevertFromBlockedByTaskNumber(@Param("taskId") Long taskId, @Param("blockedStatusIds") List<Integer> blockedStatusIds);

      List<TaskHistory> findByFkTeamIdTeamId(Long teamId);

      /**
       * Finds distinct task IDs from the task history by the given sprint ID.
       */
      @Query("SELECT DISTINCT th.taskId FROM TaskHistory th WHERE th.sprintId = :sprintId")
      List<Long> findDistinctTaskIdsBySprintId(Long sprintId);

      @Query("SELECT DISTINCT th.taskId FROM TaskHistory th WHERE th.sprintId = :sprintId AND th.fkAccountIdAssigned.accountId = :accountId")
      List<Long> findDistinctTaskIdsBySprintIdAndAccountIdAssigned(Long sprintId, Long accountId);

      @Query("SELECT t FROM TaskHistory t WHERE taskId = :taskId AND version = (SELECT MAX(version) FROM TaskHistory WHERE taskId = :taskId)")
      TaskHistory findByTaskIdAndLastVersion(Long taskId);

      @Modifying
      @Transactional
      @Query("DELETE FROM TaskHistory th WHERE th.fkOrgId.orgId = :orgId")
      void deleteByOrgId(Long orgId);

      @Modifying
      @Transactional
      @Query("DELETE FROM TaskHistory th WHERE th.taskId IN :taskIds")
      void deleteByTaskIdIn(List<Long> taskIds);

      @Query("SELECT th.taskHistoryId FROM TaskHistory th WHERE th.taskId IN :taskIds")
      List<Long> findAllTaskHistoryIdsByTaskIds(List<Long> taskIds);
}
