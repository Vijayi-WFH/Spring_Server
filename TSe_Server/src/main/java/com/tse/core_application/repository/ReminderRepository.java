package com.tse.core_application.repository;

import com.tse.core_application.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long>  {

    Reminder findByReminderId(Long reminderId);

    Reminder findByReminderIdAndReminderStatus(Long reminderId, String reminderStatus);

    List<Reminder> findAllByReminderDateAndReminderTimeAndReminderStatus(LocalDate date, LocalTime time, String reminderStatus);

    List<Reminder> findAllByFkAccountIdCreatorAccountIdIn(List<Long> accountIds);

    List<Reminder> findAllByFkAccountIdCreatorAccountIdInAndReminderDateAndReminderStatus(List<Long> accountIds, LocalDate date, String reminderStatus);

    List<Reminder> findAllByFkAccountIdCreatorAccountIdInAndReminderStatus(List<Long> accountIds, String reminderStatus);

    @Query("SELECT r FROM Reminder r " +
            "WHERE r.isEarlyReminderSet = true " +
            "AND r.reminderStatus = :reminderStatus " +
            "AND r.earlyReminderTime = :now")
    List<Reminder> findDueEarlyReminders(
            @Param("reminderStatus") String reminderStatus,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Reminder r " +
            "WHERE r.reminderStatus = :status " +
            "AND r.reminderDate = :date " +
            "AND r.reminderTime = :time " +
            "AND r.fkAccountIdCreator.accountId = :accountId " +
            "AND r.reminderTitle = :reminderTitle")
    Boolean existsReminder(
            @Param("accountId") Long accountId,
            @Param("status") String status,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            @Param("reminderTitle") String reminderTitle);

    List<Reminder> findAllByReminderDateAndReminderStatus(LocalDate date, String reminderStatus);

    @Modifying
    @Transactional
    @Query("DELETE FROM Reminder r WHERE r.entityTypeId = :entityTypeId AND r.entityId IN :entityIds")
    void deleteByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Reminder r WHERE r.fkAccountIdCreator.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);

}
