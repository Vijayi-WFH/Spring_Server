package com.tse.core_application.repository;

import com.tse.core_application.dto.TimeSheetSummary;
import com.tse.core_application.model.TimeSheet;
import com.tse.core_application.model.UserAccount;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSheetRepository extends JpaRepository<TimeSheet, Long> {

    List<TimeSheet> findAllByUserIdAndNewEffortDateBetween(Long userId, LocalDate fromDate, LocalDate toDate);

//        @Query("SELECT ts FROM TimeSheet ts WHERE ts.userId = :userId AND ts.newEffortDate BETWEEN :fromDate AND :toDate")
//        List<TimeSheet> findAllByUserIdAndNewEffortDateBetween(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    List<TimeSheet> findAllByUserId(Long userId);

    @Query("SELECT ts FROM TimeSheet ts WHERE ts.entityId = :taskId AND ts.newEffortDate = :effortDate AND ts.taskTypeId <> 8")
    List<TimeSheet> findAllByEntityIdAndNewEffortDateAndTaskTypeNotPersonal(Long taskId, LocalDate effortDate);


//    @Query("SELECT MAX(ts.createdDateTime) FROM TimeSheet ts WHERE ts.taskId = :taskId")
//    LocalDateTime findLatestCreatedDateTimeByTaskId(Long taskId);

    // assuming this is for task only not personal task
    @Query("SELECT ts FROM TimeSheet ts WHERE ts.entityTypeId = :entityTypeId AND ts.entityId = :taskId AND ts.taskTypeId <> 8 AND ts.newEffortDate = (SELECT MAX(t.newEffortDate) FROM TimeSheet t WHERE t.entityTypeId = :entityTypeId AND t.entityId = :taskId AND t.taskTypeId <> 8)")
    List<TimeSheet> findLatestNewEffortDateTimeSheetByTaskIdAndTaskTypeNotPersonal(Long taskId, Integer entityTypeId);

    List<TimeSheet> findByEntityNumber(Long entityNumber);

    List<TimeSheet> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    //Query to get timesheet for given account id, entity id and entity type id. Using for finding meeting efforts
    @Query("SELECT ts FROM TimeSheet ts WHERE ts.accountId = :accountId And ts.entityTypeId = :entityTypeId AND ts.entityId= :entityId AND ts.newEffortDate= :newEffortDate")
    TimeSheet findByAccountIdAndEntityIdandEntityTypeId(Long accountId, Integer entityTypeId, Long entityId,LocalDate newEffortDate);

    @Query("SELECT ts FROM TimeSheet ts WHERE ts.accountId = :accountId And ts.entityTypeId = :entityTypeId AND ts.entityId= :entityId ORDER BY ts.newEffortDate")
    List<TimeSheet> findAllByAccountIdAndEntityIdandEntityTypeIdAndOrderByStartDate(Long accountId, Integer entityTypeId, Long entityId);

    Boolean existsByEntityTypeIdAndEntityIdAndNewEffortDate(Integer entityTypeId, Long entityId, LocalDate newEffortDate);

    List<TimeSheet> findByReferenceEntityTypeIdAndReferenceEntityId(Integer referenceEntityTypeId, Long referenceEntityId);

    List<TimeSheet> findByTeamId(Long teamId);

    List<TimeSheet> findByTeamIdAndSprintIdAndNewEffortDateBetween(Long teamId, Long sprintId, LocalDate fromDate, LocalDate toDate);

    List<TimeSheet> findByTeamIdAndSprintIdAndAccountIdAndNewEffortDateBetween(Long teamId, Long sprintId, Long accountId, LocalDate fromDate, LocalDate toDate);


    /**
     * Finds the summary of time tracking by the given task ID.
     * It returns total recorded effort and total earned time for the given task in a specific sprint
     */
    @Query("SELECT new com.tse.core_application.dto.TimeSheetSummary(" +
            "COALESCE(SUM(tt.newEffort), 0) as totalRecordedEffort, " +
            "COALESCE(SUM(tt.earnedTime), 0) as totalEarnedTime) " +
            "FROM TimeSheet tt WHERE tt.entityId = :taskId AND tt.entityTypeId = 6 AND tt.sprintId = :sprintId")
    TimeSheetSummary findEffortSummaryByTaskIdInSprint(Long taskId, Long sprintId);

    /**
     * Finds the summary of time tracking by the given task ID and accountId.
     * It returns total recorded effort and total earned time for the given task in a specific sprint
     */
    @Query("SELECT new com.tse.core_application.dto.TimeSheetSummary(" +
            "COALESCE(SUM(tt.newEffort), 0) as totalRecordedEffort, " +
            "COALESCE(SUM(tt.earnedTime), 0) as totalEarnedTime) " +
            "FROM TimeSheet tt WHERE tt.entityId = :taskId AND tt.entityTypeId = 6 AND tt.accountId = :accountId AND tt.sprintId = :sprintId")
    TimeSheetSummary findEffortSummaryByTaskIdAndAccountIdInSprint(Long taskId, Long accountId, Long sprintId);

    void deleteByEntityTypeIdAndEntityId (Integer entityTypeId, Long entityId);

    List<TimeSheet> findByAccountIdAndNewEffortDate(Long accountId, LocalDate newEffortDate);

    @Modifying
    @Query(" UPDATE TimeSheet t set t.earnedTime = 0, t.newEffort = 0 " +
            "WHERE t.entityTypeId =:entityTypeId and t.entityId = :entityId and t.accountId IN :accountIds")
    void updateTimesheetEffortToZeroForMeetingOnRemoving(@Param("entityTypeId") Integer entityTypeId, @Param("entityId") Long entityId, @Param("accountIds") List<Long> accountIds );
}
