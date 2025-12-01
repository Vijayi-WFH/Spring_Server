package com.tse.core.repository;

import com.tse.core.model.TimeSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSheetRepository extends JpaRepository<TimeSheet, Long> {

    public List<TimeSheet> findByUserIdInAndNewEffortDateBetween(List<Long> userId, LocalDate fromDate, LocalDate toDate);

    public List<TimeSheet> findByTeamIdAndNewEffortDateBetween(Long teamId, LocalDate fromDate, LocalDate toDate);
//
//    public List<TimeSheet> findByTaskId(Long taskId);

    public List<TimeSheet> findByEntityIdAndNewEffortDateBetween(Long taskId, LocalDate fromDate, LocalDate toDate);

//    @Query("select t from TimeSheet t where t.teamId is null AND (t.entityTypeId IS NOT null AND t.entityTypeId IN entityTypeIdList) AND t.accountId in :allAccountIdsList AND t.newEffortDate between :fromDate AND :toDate")
//    List<TimeSheet> findByEntityTypeIdAndAccountIdListAndNewEffortDateBetween(List<Integer> entityTypeIdList, List<Long> allAccountIdsList, LocalDate fromDate, LocalDate toDate);

    //Query to get timesheet for given account id, entity id and entity type id. Using for finding holiday timesheet
    @Query("SELECT ts FROM TimeSheet ts WHERE ts.accountId = :accountId And ts.newEffortDate= :newEffortdate AND ts.entityTypeId = :entityTypeId")
    TimeSheet findByAccountIdAndNewEffortDateAndEntityTypeId(Long accountId, LocalDate newEffortdate, Integer entityTypeId);

    List<TimeSheet> findByTeamIdInAndNewEffortDateBetween(List<Long> teamId, LocalDate fromDate, LocalDate toDate);

    List<TimeSheet> findByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(List<Long> teamId, LocalDate fromDate, LocalDate toDate, List<Long> accountIdList);

    @Query("select t from TimeSheet t where t.teamId is null AND (t.projectId is not null AND t.projectId IN :projectIdList) AND t.newEffortDate between :fromDate AND :toDate")
    List<TimeSheet> findTimeSheetAtProjectLevelByProjectIdInAndNewEffortDateBetween(List<Long> projectIdList, LocalDate fromDate, LocalDate toDate);

    @Query("select t from TimeSheet t where t.teamId is null AND (t.projectId is not null AND t.projectId IN :projectIdList) AND t.accountId in :accountIdList AND t.newEffortDate between :fromDate AND :toDate")
    List<TimeSheet> findTimeSheetAtProjectLevelByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(List<Long> projectIdList, LocalDate fromDate, LocalDate toDate, List<Long> accountIdList);

    @Query("select t from TimeSheet t where t.teamId is null and t.projectId is null and t.buId in :buIdList and t.newEffortDate between :fromDate and :toDate")
    List<TimeSheet> findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetween(List<Long> buIdList, LocalDate fromDate, LocalDate toDate);

    @Query("select t from TimeSheet t where t.teamId is null and t.projectId is null and t.buId in :buIdList and t.accountId in :accountIdList and t.newEffortDate between :fromDate and :toDate")
    List<TimeSheet> findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetweenAndAccountIdIn(List<Long> buIdList, LocalDate fromDate, LocalDate toDate, List<Long> accountIdList);

    @Query("select t from TimeSheet t where t.teamId is null and t.projectId is null and t.buId is null and t.orgId in :orgIdList and t.newEffortDate between :fromDate and :toDate")
    List<TimeSheet> findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetween(List<Long> orgIdList, LocalDate fromDate, LocalDate toDate);

    @Query("select t from TimeSheet t where t.teamId is null and t.projectId is null and t.buId is null and t.orgId in :orgIdList and t.accountId in :accountIdList and t.newEffortDate between :fromDate and :toDate")
    List<TimeSheet> findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetweenAndAccountIdIn(List<Long> orgIdList, LocalDate fromDate, LocalDate toDate, List<Long> accountIdList);

}