package com.tse.core_application.repository;

import com.tse.core_application.custom.model.SprintResponseForFilter;
import com.tse.core_application.custom.model.SprintTitleAndId;
import com.tse.core_application.custom.model.SprintWithTeamCode;
import com.tse.core_application.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    @Query("SELECT s from Sprint s where s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND s.sprintStatus != 4")
    List<Sprint> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    Boolean existsByEntityTypeIdAndEntityIdAndSprintStatusNotIn(Integer entityTypeId, Long entityId, List<Integer> sprintStatusList);

    @Query("SELECT NEW com.tse.core_application.custom.model.SprintResponseForFilter(s.sprintId, s.sprintTitle, t.teamName, t.fkProjectId.projectName, t.fkOrgId.organizationName, s.sprintExpStartDate, s.sprintExpEndDate, s.hoursOfSprint, s.earnedEfforts) " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE s.entityId in :entityIdList " +
            "AND s.entityTypeId = :entityTypeId " +
            "AND s.sprintActStartDate IS NOT NULL " +
            "AND s.sprintActEndDate IS NULL " +
            "AND s.sprintStatus != 4")
    List<SprintResponseForFilter> getCustomAllActiveSprintDetailsForEntities(List<Long> entityIdList, Integer entityTypeId);

    @Query("SELECT DISTINCT s from Sprint s where s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND ((s.sprintExpStartDate BETWEEN :startDate AND :endDate) OR (s.sprintExpEndDate BETWEEN :startDate AND :endDate) OR (:startDate BETWEEN s.sprintExpStartDate AND s.sprintExpEndDate) OR (:endDate BETWEEN s.sprintExpStartDate AND s.sprintExpEndDate)) AND s.sprintStatus != 4")
    List<Sprint> findSprintsBetweenDates(Integer entityTypeId, Long entityId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT DISTINCT s FROM Sprint s WHERE s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND s.sprintId != :sprintId AND ((s.sprintExpStartDate BETWEEN :startDate AND :endDate) OR (s.sprintExpEndDate BETWEEN :startDate AND :endDate) OR (:startDate BETWEEN s.sprintExpStartDate AND s.sprintExpEndDate) OR (:endDate BETWEEN s.sprintExpStartDate AND s.sprintExpEndDate)) AND s.sprintStatus != 4")
    List<Sprint> findOtherSprintsBetweenDates(Long sprintId, Integer entityTypeId, Long entityId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT s from Sprint s where s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND s.sprintStatus = :sprintStatusId AND s.sprintExpStartDate > :startDate ORDER BY s.sprintExpStartDate")
    List<Sprint> findAllFutureNotStartedSprints(Integer entityTypeId, Long entityId, LocalDateTime startDate, Integer sprintStatusId);

    @Query("SELECT s from Sprint s where s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND s.sprintStatus != :sprintStatusId AND s.sprintStatus != 4 AND s.sprintExpEndDate < :endDate ORDER BY s.sprintExpEndDate DESC")
    List<Sprint> findAllPreviousInCompleteSprints(Integer entityTypeId, Long entityId, LocalDateTime endDate, Integer sprintStatusId);

    @Query("SELECT s from Sprint s where s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND s.sprintStatus = :sprintStatusId AND s.sprintExpEndDate < :endDate ORDER BY s.sprintExpEndDate DESC")
    List<Sprint> findAllPreviousCompleteSprint(Integer entityTypeId, Long entityId, LocalDateTime endDate, Integer sprintStatusId);

    @Query("SELECT NEW com.tse.core_application.custom.model.SprintTitleAndId(s.sprintTitle, s.sprintId) FROM Sprint s WHERE s.entityId = :entityId AND s.entityTypeId = :entityTypeId AND s.sprintStatus != 4")
    List<SprintTitleAndId> getSprintTitleAndSprintIdByEntityTypeIdAndEntityId(Long entityId, Integer entityTypeId);

    SprintTitleAndId getSprintTitleAndSprintIdBySprintId(Long sprintId);

    Sprint findBySprintId (Long sprintId);

    @Query("SELECT s.sprintTitle FROM Sprint s WHERE s.sprintId = :sprintId")
    String findSprintTitleBySprintId (Long sprintId);

    // find sprints of particular status in the given entity
    List<Sprint> findByEntityTypeIdAndEntityIdAndSprintStatusIn(Integer entityTypeId, Long entityId, List<Integer> sprintStatuses);

    @Query("SELECT s " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE s.entityId in :entityIdList " +
            "AND s.entityTypeId = :entityTypeId " +
            "AND s.sprintActStartDate IS NOT NULL " +
            "AND (:leaveDate BETWEEN FUNCTION('DATE', s.sprintExpStartDate) AND FUNCTION('DATE', s.sprintExpEndDate)) " +
            "AND s.sprintActEndDate IS NULL " +
            "AND s.sprintStatus != 4")
    List<Sprint> getCustomAllActiveSprintsForEntitiesAndContainsLeaveDate(List<Long> entityIdList, Integer entityTypeId, LocalDate leaveDate);

    @Query("SELECT s " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE s.entityId in :entityIdList " +
            "AND s.entityTypeId = :entityTypeId " +
            "AND s.sprintStatus IN :sprintStatusId " +
            "AND (:date BETWEEN FUNCTION('DATE', s.sprintExpStartDate) AND FUNCTION('DATE', s.sprintExpEndDate))")
    List<Sprint> getCustomSprintsForEntitiesAndContainsDate(List<Long> entityIdList, Integer entityTypeId, LocalDate date, List<Integer> sprintStatusId);

    @Query("SELECT count(s) FROM Sprint s WHERE s.fkAccountIdCreator.accountId IN :accountIdList")
    Integer findSprintsCountByFkAccountIdCreatorAccountIdIn(List<Long> accountIdList);

    @Query("SELECT new com.tse.core_application.custom.model.SprintWithTeamCode(s, t.teamCode) " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE s.entityId in :entityIdList " +
            "AND s.entityTypeId = :entityTypeId " +
            "AND s.sprintStatus != 4")
    List<SprintWithTeamCode> findSprintWithTeamCodeByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityIdList);

    @Query("SELECT s " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE t.fkOrgId.orgId = :orgId " +
            "AND s.sprintStatus IN :sprintStatusList")
    List<Sprint> getCustomSprintForOrg(Long orgId, List<Integer> sprintStatusList);

    @Query("SELECT s " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE t.fkOrgId.orgId = :orgId " +
            "AND s.sprintStatus IN :sprintStatusId " +
            "AND (:date BETWEEN FUNCTION('DATE', s.sprintExpStartDate) AND FUNCTION('DATE', s.sprintExpEndDate))")
    List<Sprint> getCustomSprintsForOrgAndContainsDate(Long orgId, LocalDate date, List<Integer> sprintStatusId);

    @Query("SELECT s from Sprint s where s.entityTypeId = :entityTypeId AND s.entityId = :entityId AND s.sprintStatus = 4")
    List<Sprint> findDeletedSprintByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query("SELECT s " +
            "FROM Sprint s " +
            "JOIN Team t ON s.entityId = t.teamId " +
            "WHERE s.sprintStatus IN :sprintStatusId ")
    List<Sprint> getCustomSprints(List<Integer> sprintStatusId);

}
