package com.tse.core_application.repository;

import com.tse.core_application.custom.model.CommentId;
import com.tse.core_application.custom.model.TaskDetails;
import com.tse.core_application.dto.AiMLDtos.AiWorkItemDescResponse;
import com.tse.core_application.dto.TaskProgress;
import com.tse.core_application.dto.TaskValidationDto;
import com.tse.core_application.dto.meeting.WorkItemProgressDetailsDto;
import com.tse.core_application.model.*;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

//Specification executer is set for filter logic. See Specification<Task> in specification folder.
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Task findByTaskId(Long taskId);

    List<Task> findByFkTeamIdTeamId(Long teamId);

    List<Task> findByParentTaskId(Long parentTaskId);

    Boolean existsByTaskId(Long taskId);


//    // find a unique by task number, initials and teamId
//    Task findByTaskNumberAndInitialsAndFkTeamIdTeamId(Long taskNumber, String initials, Long teamId);
//
//    // find by task number, initials and orgId
//    Task findByTaskNumberAndInitialsAndFkTeamIdFkOrgIdOrgId(Long taskNumber, String initials, Long teamId);

    //	List<Task> findByAccountId(Long accountId);

    List<Task> findByFkAccountIdAccountId(Long accountId);

//    List<Task> findByTaskExpEndDateLessThan(Date date);

//	List<Task> findByAccountIdIn(List<Long> accountIds);

//    List<Task> findByTaskPriorityAndAccountIdIn(TaskPriority priority, List<Long> accountId);

    List<Task> findByTaskPriorityAndFkAccountIdAccountIdIn(TaskPriority priority, List<Long> accountId);

    void deleteByTaskId(Long taskId);

    // find teamId, taskTitle, orgId, projectId by its taskId
//	@Query("select new com.tse.core_application.custom.model.TeamIdTaskTitleOrgIdProjectId( m.teamId, k.taskTitle, m.orgId, m.projectId ) from Task k inner join k.team m where k.taskId=:taskId")
//	public TeamIdTaskTitleOrgIdProjectId getTeamIdTaskTitleOrgIdProjectId(Long taskId);

    CommentId findCommentIdByTaskId(Long taskId);

    @Modifying
    @Transactional
    @Query("update Task t set t.commentId = :commentId where t.taskId = :taskId")
    Integer setTaskCommentIdByTaskId(Long commentId, Long taskId);

//    @Query(value = "select new com.tse.core_application.custom.model.TaskNumber (max (t.taskNumber)) from Task t")
//    TaskNumber getMaxTaskNumber();

    @Modifying
    @Query("update Task t set t.attachments = :attachments where t.taskId = :taskId")
    Integer setTaskAttachmentsByTaskId(String attachments, Long taskId);

    @Modifying
    @Query("update Task t set t.noteId = :noteId where t.taskId = :taskId")
    Integer setTaskNoteIdByTaskId(Long noteId, Long taskId);

    @Modifying
    @Query("update Task t set t.listOfDeliverablesDeliveredId = :listOfDeliverablesDeliveredId where t.taskId = :taskId")
    Integer setTaskListOfDeliverablesDeliveredIdByTaskId(Long listOfDeliverablesDeliveredId, Long taskId);

    List<Task> findByFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(List<Long> accountIds, Boolean indicatorValue);
    List<Task> findByFkOrgIdOrgIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(Long orgId, List<Long> accountIds, Boolean indicatorValue);

    List<Task> findByBuIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(Long BuId, List<Long> accountIds, Boolean indicatorValue);

    List<Task> findByFkProjectIdProjectIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(Long projectId, List<Long> accountIds, Boolean indicatorValue);

    List<Task> findByFkTeamIdTeamIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(Long teamId, List<Long> accountIds, Boolean indicatorValue);
    List<Task> findByFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicatorOrderByTaskPriority(List<Long> accountIds, Boolean indicatorValue);

    @Query("select t.fkWorkflowTaskStatus from Task t where t.taskId = :taskId")
    WorkFlowTaskStatus findAllFkWorkflowTaskStatusByTaskId(Long taskId);

    @Query("select t from Task t where t.immediateAttentionFrom=:userName and t.immediateAttention = 1 and t.fkTeamId.teamId in :teamList")
    List<Task> findByImmediateAttentionFromAndTeamIn(String userName, List<Long> teamList);

    //    @Query("SELECT t FROM Task t WHERE t.fkOrgId.orgId = :orgId AND t.fkTeamId.teamId = :teamId AND t.fkAccountIdAssigned IS NOT NULL AND (t.fkWorkflowTaskStatus.workflowTaskStatus IN :workFlowStatus OR t.taskActEndDate >= :date)")
//    List<Task> findTasksWithOrCondition(@Param("orgId") Long orgId, @Param("teamId") Long teamId, @Param("workFlowStatus") List<String> workFlowStatus, @Param("date") LocalDate date);

    @Query("select count(t.currentActivityIndicator) from Task t where t.currentActivityIndicator = :currentActivityIndicator and t.fkAccountIdAssigned.accountId in :accountIds")
    int getNumberOfTasksWithCurrentActivityIndicatorOn(@Param("accountIds") List<Long> accountIds, @Param("currentActivityIndicator") Integer currentActivityIndicator);

    @Modifying
    @Query("update Task t set t.currentActivityIndicator = :currentActivityIndicator where t.taskId = :taskId")
    Task setCurrentActivityIndicatorByTaskId(@Param("taskId") Long taskId, @Param("currentActivityIndicator") Integer currentActivityIndicator);

    List<Task> findByTaskIdIn(List<Long> taskIds);

    List<Task> findByTaskNumberIn(List<Long> taskNumbers);

    @Query("select t.taskProgressSystem from Task t where t.taskId in :taskIds")
    List<StatType> getStatsOfTaskIdsIn(@Param("taskIds") List<Long> taskIds);

    List<Task> findDistinctByLabels_LabelNameInAndFkTeamId_TeamId(List<String> labelNames, Long teamId);
    List<Task> findDistinctByLabels_LabelNameInAndFkTeamId_TeamIdAndFkAccountIdAssigned_AccountId(List<String> labelNames, Long teamId, Long accountId);

    @Query("select t from Task t " +
            "where t.blockedReasonTypeId in :reasonTypeIds " +
            "and t.fkWorkflowTaskStatus.workflowTaskStatus = :workflowTaskStatus " +
            "and t.reminderInterval is not null")
    List<Task> findTaskByReasonTypeIdAndWorkFlowStatusBlockedAndReminderIntervalNotNull(
            @Param("reasonTypeIds") List<Integer> reasonTypeIds,
            @Param("workflowTaskStatus") String workflowTaskStatus);

    @Transactional
    @Modifying
    @Query(value = "UPDATE Task t SET t.nextReminderDateTime = :nextReminderDateTime WHERE t.taskId = :taskId")
    void updateNextReminderDateTime(Long taskId, LocalDateTime nextReminderDateTime);

    List<Task> findBySprintId(Long sprintId);

    /** retrieves a list of tasks by sprint ID, with the condition that if a task has a type ID of 3 (child task type),
     * then it should be included only if its parent task's sprint ID differs from the given sprint ID i.e the parent task
     * is not part of the given sprint */
    @Query(value = "SELECT * FROM tse.task t WHERE (t.sprint_id = :sprintId OR :prevSprintId = ANY(STRING_TO_ARRAY(t.prev_sprints, ','))) " +
            "AND (t.task_type_id <> 3 OR NOT EXISTS " +
            "(SELECT 1 FROM tse.task parent WHERE parent.task_id = t.parent_task_id AND (parent.sprint_id = :sprintId)))", nativeQuery = true)
    List<Task> findTasksBySprintIdExcludingChildTasksWithSameSprint(Long sprintId, String prevSprintId);


    @Query("SELECT t FROM Task t WHERE t.sprintId = :sprintId AND t.fkAccountIdAssigned.accountId = :accountId AND t.taskTypeId != :taskTypeId And t.fkWorkflowTaskStatus.workflowTaskStatus != :workflowStatus")
    List<Task> findAllTaskForUserCapacity(Long sprintId, Long accountId, String workflowStatus, Integer taskTypeId);

    @Query("SELECT t FROM Task t WHERE t.sprintId = :sprintId AND t.fkAccountIdAssigned.accountId IS NULL AND t.taskTypeId != :taskTypeId And t.fkWorkflowTaskStatus.workflowTaskStatus != :workflowStatus")
    List<Task> findAllTaskForUnassignedCapacity(Long sprintId, String workflowStatus, Integer taskTypeId);

    @Query("Select NEW com.tse.core_application.dto.TaskProgress(t.taskId, t.taskNumber, t.taskEstimate, t.earnedTimeTask) from Task t where t.fkAccountIdAssigned.accountId in :accountIds and t.fkTeamId.teamId in :teamIds and ((t.taskExpStartDate BETWEEN :fromDate AND :toDate) OR (t.taskExpEndDate BETWEEN :fromDate AND :toDate)) and t.fkWorkflowTaskStatus.workflowTaskStatus != 'Backlog' and t.sprintId is null")
    List<TaskProgress> findAllTaskProgressBetweenDates(List<Long> accountIds, List<Long> teamIds, LocalDateTime fromDate, LocalDateTime toDate);

    @Query("SELECT DISTINCT t FROM Task t WHERE t.fkAccountIdAssigned.accountId IN :accountIds AND (t.taskProgressSystem = 'DELAYED' OR (t.taskExpEndDate = :expectedEndDate AND t.taskProgressSystem NOT IN ('COMPLETED')) OR t.taskProgressSystem = 'WATCHLIST')")
    List<Task> findTaskListForTodayFocus(List<Long> accountIds, LocalDateTime expectedEndDate);

    @Query("SELECT DISTINCT t FROM Task t WHERE t.fkAccountIdAssigned.accountId IN :accountIds AND (t.taskProgressSystem = 'DELAYED' OR (t.taskExpEndDate = :expectedEndDate AND t.taskProgressSystem NOT IN ('COMPLETED')) OR t.taskProgressSystem = 'WATCHLIST') AND t.fkOrgId.orgId = :orgId")
    List<Task> findTaskListForTodayFocusInOrg(List<Long> accountIds, LocalDateTime expectedEndDate, Long orgId);

    @Query("SELECT DISTINCT t FROM Task t WHERE t.fkAccountIdAssigned.accountId IN :accountIds AND (t.taskProgressSystem = 'DELAYED' OR (t.taskExpEndDate = :expectedEndDate AND t.taskProgressSystem NOT IN ('COMPLETED')) OR t.taskProgressSystem = 'WATCHLIST') AND t.fkTeamId.teamId = :teamId")
    List<Task> findTaskListForTodayFocusInTeam(List<Long> accountIds, LocalDateTime expectedEndDate, Long teamId);

    @Query("SELECT DISTINCT t FROM Task t WHERE t.fkAccountIdAssigned.accountId IN :accountIds AND (t.taskProgressSystem = 'DELAYED' OR (t.taskExpEndDate = :expectedEndDate AND t.taskProgressSystem NOT IN ('COMPLETED')) OR t.taskProgressSystem = 'WATCHLIST') AND t.fkProjectId.projectId = :projectId")
    List<Task> findTaskListForTodayFocusInProject(List<Long> accountIds, LocalDateTime expectedEndDate, Long projectId);

    List<Task> findAllByTaskProgressSystemAndDependencyIdsNotNull (StatType taskProgressSystem);

    @Query(value = "SELECT nextval('tse.task_number_seq')", nativeQuery = true)
    Long getNextTaskNumber();

    Task findByTaskIdentifierAndFkTeamIdTeamId(Long taskIdentifier, Long teamId);

    List<Task> findByTaskIdentifierInAndFkTeamIdTeamId(List<Long> taskIdentifierList, Long teamId);

    @Query("SELECT NEW com.tse.core_application.dto.TaskValidationDto(t.fkAccountIdAssigned.accountId, t.userPerceivedPercentageTaskCompleted, m.meetingId)" +
            " FROM Task t  JOIN  Meeting m ON t.taskNumber = m.referenceEntityNumber where t.taskNumber IN :taskIdentifierList and t.fkTeamId.teamId IN :teamIds AND t.fkTeamId.teamId = m.teamId")
    List<TaskValidationDto> findByTaskIdentifierInAndFkTeamIdTeamIdIn(List<String> taskIdentifierList, List<Long> teamIds);

    Task findByTaskIdentifierAndFkProjectIdProjectId(Long taskIdentifier, Long projectId);

    List<Task> findByFkTeamIdTeamIdAndTaskNumberIn(Long teamId, List<String> taskNumbers);

    Task findByFkTeamIdFkOrgIdOrgIdAndTaskNumber(Long orgId, String taskNumber);

    Task findByFkTeamIdTeamIdAndTaskNumber(Long teamId, String taskNumber);

    Task findByFkProjectIdProjectIdAndTaskNumber(Long projectId, String taskNumber);

    List<Task> findByFkAccountIdAssignedAccountIdAndSprintId(Long accountId, Long sprintId);

    @Query(value = "SELECT * FROM tse.task t WHERE (sprint_id = :sprintId OR :prevSprintId = ANY(STRING_TO_ARRAY(prev_sprints, ','))) AND account_id_assigned = :accountId AND parent_task_id = :parentTaskId", nativeQuery = true)
    List<Task> findByParentTaskIdAndFkAccountIdAssignedAccountIdAndSprintId(Long parentTaskId, Long accountId, Long sprintId, String prevSprintId);

    @Query(value = "SELECT * FROM tse.task t WHERE (sprint_id = :sprintId OR :prevSprintId = ANY(STRING_TO_ARRAY(prev_sprints, ','))) AND account_id_assigned IS NULL AND parent_task_id = :parentTaskId", nativeQuery = true)
    List<Task> findByParentTaskIdAndFkAccountIdAssignedIsNullAccountIdAndSprintId(Long parentTaskId, Long sprintId, String prevSprintId);

    List<Task> findBySprintIdAndFkWorkflowTaskStatusWorkflowTaskStatusNotInAndMeetingListNotNull(Long sprintId, List<String> workflowStatusList);

    @Query("SELECT t FROM Task t WHERE t.sprintId = :sprintId AND t.fkWorkflowTaskStatus.workflowTaskStatus NOT IN :workflowStatusList AND t.meetingList IS NOT NULL AND t.meetingList != ''")
    List<Task> findBySprintIdAndWorkflowStatusNotInAndMeetingListNotNullAndNotEmpty(Long sprintId, List<String> workflowStatusList);

    @Query(value = "SELECT t FROM tse.task t WHERE (t.sprint_id = :sprintId OR :prevSprintId = ANY(STRING_TO_ARRAY(t.prev_sprints, ','))) AND t.account_id_assigned = :accountId", nativeQuery = true)
    List<Task> findAllSprintTasksForUser(Long accountId, Long sprintId, String prevSprintId);

    @Query("SELECT t.taskId FROM Task t WHERE t.parentTaskId = :parentTaskId")
    List<Long> findTaskIdByParentTaskId(Long parentTaskId);


    Integer countBySprintIdAndFkAccountIdAssignedAccountIdIsNullAndTaskEstimateIsNull(Long sprintId);

    @Query("SELECT t FROM Task t WHERE t.fkAccountIdAssigned.accountId = :accountId " +
            "AND t.sprintId = :sprintId " +
            "AND (t.taskEstimate IS NULL OR t.taskEstimate = 0)")
    List<Task> findByFkAccountIdAssignedAccountIdAndSprintIdAndTaskEstimateIsNull(Long accountId, Long sprintId);

    @Query("SELECT t FROM Task t " +
            "WHERE t.sprintId = :sprintId " +
            "AND (t.taskEstimate IS NULL OR t.taskEstimate = 0)")
    List<Task> findBySprintIdAndTaskEstimateIsNull(Long sprintId);

    @Query("SELECT t FROM Task t " +
            "WHERE t.sprintId = :sprintId " +
            "AND t.fkAccountIdAssigned.accountId IS NULL " +
            "AND (t.taskEstimate IS NULL OR t.taskEstimate = 0)")
    List<Task> findBySprintIdAndFkAccountIdAssignedAccountIdIsNullAndTaskEstimateIsNull(Long sprintId);

    @Query("SELECT new com.tse.core_application.custom.model.TaskDetails(t.taskNumber, t.taskId, t.taskTitle, t.taskDesc, t.fkTeamId.teamId) from Task t WHERE t.taskId = :taskId")
    TaskDetails getTaskBasicDetailsByTaskId (Long taskId);

    Long findFkTeamIdTeamIdByTaskId(Long taskId);

    @Query("SELECT t FROM Task t WHERE t.fkEpicId.epicId = :epicId AND ("
            + "(:expEndDateTime IS NULL OR "
            + "((t.taskExpStartDate IS NOT NULL AND t.taskExpStartDate > :expEndDateTime) "
            + "OR (t.taskExpEndDate IS NOT NULL AND t.taskExpEndDate > :expEndDateTime))) "
            + "AND (:expStartDateTime IS NULL OR "
            + "((t.taskExpStartDate IS NOT NULL AND t.taskExpStartDate < :expStartDateTime) "
            + "OR (t.taskExpEndDate IS NOT NULL AND t.taskExpEndDate < :expStartDateTime))))")
    List<Task> findByFkEpicIdEpicIdAndTaskExpStartDateAndTaskExpEndDate(
            Long epicId,
            LocalDateTime expStartDateTime,
            LocalDateTime expEndDateTime);



    @Query("select t.taskId from Task t where t.fkTeamId.teamId = :teamId")
    List<Long> findTaskIdByFkTeamIdTeamId(Long teamId);

    @Query("SELECT count(t) FROM Task t WHERE t.fkOrgId.orgId = :orgId")
    Integer findTaskCountByOrgId(Long orgId);

    @Query("Select t.taskId from Task t where t.fkEpicId.epicId = :epicId And t.fkTeamId.teamId = :teamId")
    List<Long> findTaskIdByfkEpicIdEpicIdAndfkTeamIdTeamId(Long epicId, Long teamId);

    @Query("SELECT DISTINCT d.dependencyId FROM Task t JOIN Dependency d ON CAST(d.dependencyId AS java.lang.String) in t.dependencyIds WHERE t.taskId IN :taskIds")
    List<Long> findDependencyIdsByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    @Query("SELECT t.taskId FROM Task t WHERE ((t.taskExpStartDate BETWEEN :startDate AND :endDate) OR (t.taskExpEndDate BETWEEN :startDate AND :endDate)) AND t.fkTeamId.teamId IN :teamIds")
    List<Long> findTaskIdsWithExpDatesInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("teamIds") List<Long> teamIds);

    @Query("SELECT t.taskId FROM Task t WHERE t.sprintId IN :sprintIds")
    List<Long> findTaskIdsBySprintIdIn(@Param("sprintIds") List<Long> sprintIds);

    @Query("SELECT t.taskId FROM Task t WHERE t.fkEpicId.epicId IN :epicIds")
    List<Long> findTaskIdsByEpicIdsIn(@Param("epicIds") List<Long> epicIds);

    @Query("SELECT DISTINCT t.taskId FROM Task t JOIN Dependency d ON (t.taskId = d.predecessorTaskId OR t.taskId = d.successorTaskId) WHERE ((t.taskExpStartDate BETWEEN :startDate AND :endDate) OR (t.taskExpEndDate BETWEEN :startDate AND :endDate)) AND d.isRemoved = false AND t.fkTeamId.teamId IN :teamIds")
    List<Long> findTaskIdsWithExpDatesInRangeAndOnlyWithDependencies(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("teamIds") List<Long> teamIds);

    @Query("SELECT DISTINCT t.taskId FROM Task t JOIN Dependency d ON (t.taskId = d.predecessorTaskId OR t.taskId = d.successorTaskId) WHERE t.sprintId IN :sprintIds AND d.isRemoved = false")
    List<Long> findTaskIdsBySprintIdInAndOnlyWithDependencies(@Param("sprintIds") List<Long> sprintIds);

    @Query("SELECT DISTINCT t.taskId FROM Task t JOIN Dependency d ON (t.taskId = d.predecessorTaskId OR t.taskId = d.successorTaskId) WHERE t.fkEpicId.epicId IN :epicIds AND d.isRemoved = false")
    List<Long> findTaskIdsByEpicIdsInAndOnlyWithDependencies(@Param("epicIds") List<Long> epicIds);

    @Query(value = "select t.task_exp_end_date from tse.task t where t.task_id in (select d.predecessor_task_id  from tse.dependency d where d.successor_task_id in :successorTaskId) " +
            " and t.task_exp_end_date <= :succTaskTime order by t.task_exp_end_date desc limit 1;", nativeQuery = true)
    LocalDateTime findDependencyExpEndTimeFromTask(@Param("succTaskTime") LocalDateTime succTaskTime,
                                                   @Param("successorTaskId") List<Long> successorTaskId);

    @Query(value = "select t.task_exp_start_date from tse.task t where t.task_id in (select d.successor_task_id from tse.dependency d where d.predecessor_task_id in :predecessorTaskId) " +
            " and t.task_exp_start_date >= :predTaskTime order by t.task_exp_start_date limit 1;", nativeQuery = true)
    LocalDateTime findDependencyExpStartTimeFromTask(@Param("predTaskTime") LocalDateTime predTaskTime,
                                                     @Param("predecessorTaskId") List<Long> predecessorTaskId);

    List<Task> findByTaskTypeId(Integer taskTypeId);

    @Query("select t from Task t where t.fkTeamId.teamId = :teamId and t.taskNumber = :taskNumber")
    List<Task> findByTaskNumberAndFkTeamIdTeamId(String taskNumber,Long teamId);

    @Query("SELECT t.fkAccountIdBlockedBy.accountId FROM Task t " +
            "WHERE t.fkTeamId.teamId IN (:teamIds) " +
            "AND t.blockedReasonTypeId IN (:reasonTypeId) " +
            "AND t.fkWorkflowTaskStatus.workflowTaskStatus = :status")
    List<Long> findBlockedByAccountIdsByTeamIdAndReasonTypeIdAndStatus(
            @Param("teamIds") List<Long> teamIds,
            @Param("reasonTypeId") List<Integer> reasonTypeId,
            @Param("status") String status);

    /**
     * Streams all tasks, with optional filtering by createdDateTime.
     *
     * @param workflowStatus (Required) The status to exclude.
     * @param startDateTime (Optional) If not null, only tasks created ON or AFTER this time are included.
     * @param endDateTime (Optional) If not null, only tasks created ON or BEFORE this time are included.
     */
    @Query("SELECT new com.tse.core_application.dto.AiMLDtos.AiWorkItemDescResponse(t.taskId, t.taskNumber, t.taskTitle, t.taskDesc, " +
            "t.fkOrgId.orgId, t.fkTeamId.teamId, t.fkProjectId.projectId, t.fkTeamId.teamName, t.fkAccountIdAssigned.email, t.taskTypeId, t.createdDateTime) " +
            "FROM Task t " +
            "WHERE t.fkWorkflowTaskStatus.workflowTaskState != :workflowStatus " +
            "AND (CAST(:startDateTime AS java.time.LocalDateTime) IS NULL OR t.createdDateTime >= :startDateTime) " +
            "AND (CAST(:endDateTime AS java.time.LocalDateTime) IS NULL OR t.createdDateTime <= :endDateTime)")
    @Transactional(readOnly = true)
    Stream<AiWorkItemDescResponse> streamAllTask(@Param("workflowStatus") String workflowStatus, @Param("startDateTime") LocalDateTime startDateTime,
                                                 @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT new com.tse.core_application.dto.meeting.WorkItemProgressDetailsDto(t.taskId, t.taskNumber, t.taskTypeId, t.fkAccountIdAssigned.accountId, " +
            "t.fkAccountIdAssigned.email, t.userPerceivedRemainingTimeForCompletion, t.taskProgressSystem, t.fkWorkflowTaskStatus.workflowTaskStatus) from Task t where taskId IN :taskIds ")
    List<WorkItemProgressDetailsDto> findWorkItemProgressByTaskIdIn(@Param("taskIds") List<Long> taskIds);
}