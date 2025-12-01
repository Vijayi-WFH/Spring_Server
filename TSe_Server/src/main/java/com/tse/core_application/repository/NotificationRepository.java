package com.tse.core_application.repository;

import com.tse.core_application.model.Notification;
import com.tse.core_application.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findAllByAccountId(UserAccount accountId);

    Notification findByNotificationId(Long notificationId);

//    List<Notification> findByTaskNumber(Long taskNumber);

    Notification findAllByNotificationId(Long notificationId);

    @Modifying
    @Transactional
    @Query("delete from Notification n where n.notificationId = :notificationId")
    void removeByNotificationId(Long notificationId);

    @Query(value = "SELECT n.* FROM tse.notification n JOIN tse.notification_category nc ON n.category_id = nc.category_id WHERE n.created_date_time < NOW() - (nc.retention_days || ' days')\\:\\:interval", nativeQuery = true)
    List<Notification> findOldNotifications();

    List<Notification> findByTeamIdTeamId(Long teamId);

    @Query("SELECT n FROM Notification n " +
            "WHERE n.notificationCreatorAccountId.accountId IN :creatorAccountIds " +
            "AND n.notificationTypeID.notificationTypeId IN :typeIds " +
            "AND n.orgId.orgId = :orgId " +
            "AND FUNCTION('date', n.createdDateTime) BETWEEN :fromDate AND :toDate")
    List<Notification> findByCreatorAccountIdAndTypeIdsAndOrgIdAndCreatedDateBetween(
            @Param("creatorAccountIds") List<Long> creatorAccountIds,
            @Param("typeIds") List<Long> typeIds,
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT n FROM Notification n " +
            "WHERE n.notificationCreatorAccountId.accountId IN :creatorAccountIds " +
            "AND n.notificationTypeID.notificationTypeId IN :typeIds " +
            "AND n.projectId.projectId = :projectId " +
            "AND FUNCTION('date', n.createdDateTime) BETWEEN :fromDate AND :toDate")
    List<Notification> findByCreatorAccountIdAndTypeIdsAndProjectIdAndCreatedDateBetween(
            @Param("creatorAccountIds") List<Long> creatorAccountIds,
            @Param("typeIds") List<Long> typeIds,
            @Param("projectId") Long projectId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(
            value = "SELECT * FROM tse.notification n " +
                    "WHERE CAST(:taggedId AS TEXT) = ANY(string_to_array(n.tagged_account_ids, ',')) " +
                    "AND n.notification_type_id IN (:typeIds) " +
                    "AND n.team_id = :teamId " +
                    "AND n.created_date_time\\:\\:date BETWEEN CAST(:fromDate AS date) AND CAST(:toDate AS date)",
            nativeQuery = true
    )
    List<Notification> findByTaggedIdAndNotificationTypeIdsAndTeamIdAndCreatedDateBetween(
            @Param("taggedId") Long taggedId,
            @Param("typeIds") List<Long> typeIds,
            @Param("teamId") Long teamId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query(
            value = "SELECT * FROM tse.notification n " +
                    "WHERE CAST(:taggedId AS TEXT) = ANY(string_to_array(n.tagged_account_ids, ',')) " +
                    "AND n.notification_type_id IN (:typeIds) " +
                    "AND n.project_id = :projectId " +
                    "AND n.created_date_time\\:\\:date BETWEEN CAST(:fromDate AS date) AND CAST(:toDate AS date)",
            nativeQuery = true
    )
    List<Notification> findByTaggedIdAndNotificationTypeIdsAndProjectIdAndCreatedDateBetween(
            @Param("taggedId") Long taggedId,
            @Param("typeIds") List<Long> typeIds,
            @Param("projectId") Long projectId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );


    @Query(
            value = "SELECT * FROM tse.notification n " +
                    "WHERE CAST(:taggedId AS TEXT) = ANY(string_to_array(n.tagged_account_ids, ',')) " +
                    "AND n.notification_type_id IN (:typeIds) " +
                    "AND n.org_id = :orgId " +
                    "AND n.created_date_time\\:\\:date BETWEEN CAST(:fromDate AS date) AND CAST(:toDate AS date)",
            nativeQuery = true
    )
    List<Notification> findByTaggedIdAndNotificationTypeIdsAndOrgIdAndCreatedDateBetween(
            @Param("taggedId") Long taggedId,
            @Param("typeIds") List<Long> typeIds,
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT n FROM Notification n " +
            "WHERE n.notificationCreatorAccountId.accountId IN :creatorAccountIds " +
            "AND n.notificationTypeID.notificationTypeId IN :typeIds " +
            "AND n.teamId.teamId = :teamId " +
            "AND FUNCTION('date', n.createdDateTime) BETWEEN :fromDate AND :toDate")
    List<Notification> findByCreatorAccountIdAndTypeIdsAndTeamIdAndCreatedDateBetween(
            @Param("creatorAccountIds") List<Long> creatorAccountIds,
            @Param("typeIds") List<Long> typeIds,
            @Param("teamId") Long teamId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
