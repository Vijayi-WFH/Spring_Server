package com.tse.core_application.repository;

import com.tse.core_application.custom.model.MeetingDetails;
import com.tse.core_application.dto.MeetingProgress;
import com.tse.core_application.model.Meeting;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

//    List<Meeting> findByStartDateTimeAndEndDateTimeAndAttendeeId(LocalDateTime startDateTime, LocalDateTime endDateTime, Long attendeeId);

    Meeting findByMeetingId(Long meetingId);
    List<Meeting> findByTeamId(Long teamId);
    List<Meeting> findByTeamIdIn(List<Long> teamIds);
    List<Meeting> findByTeamIdInAndStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqualOrderByStartDateTimeAsc(List<Long> teamIds, LocalDateTime fromDate , LocalDateTime toDate);

    @Query("select m from Meeting m where m.meetingId IN :meetingIds")
    List<Meeting> findByMeetingIdsIn(@Param("meetingIds") List<Long> meetingIds);

    @Query("select m from Meeting m where m.startDateTime >= :fromDate and m.startDateTime <= :toDate and m.meetingId in :meetingIds order by m.startDateTime asc")
    List<Meeting> findByFromDateAndToDateAndMeetingIdsIn(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("meetingIds") List<Long> meetingIds);

    List<Meeting> findByTeamIdAndOrganizerAccountId(Long teamId, Long accountId);
    List<Meeting> findByTeamIdAndOrganizerAccountIdAndStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqualOrderByStartDateTimeAsc(Long teamId, Long organizerAccountId, LocalDateTime fromDate, LocalDateTime toDate);

    Page<Meeting> findByRecurringMeetingRecurringMeetingId(Long recurringMeetingId, Pageable pageable);

    @Query("select m.meetingId from Meeting m where m.recurringMeeting.recurringMeetingId = :recurringMeetingId")
    List<Long> findMeetingIdsByRecurringMeetingId(@Param("recurringMeetingId") Long recurringMeetingId);

//    Meeting findByMeetingNumber(String meetingNumber);

    @Query("select m.startDateTime from Meeting m where m.recurringMeeting.recurringMeetingId = :recurringMeetingId and (m.startDateTime between :fromDate and :toDate) order by m.startDateTime asc")
    List<LocalDateTime> findStartDateTimeByRecurringMeetingId(@Param("recurringMeetingId") Long recurringMeetingId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

//    @Query(value = "select new com.tse.core_application.custom.model.MeetingNumber (max (m.meetingNumber)) from Meeting m")
//    MeetingNumber getMaxMeetingNumber();

    @Modifying
    @Query("update Meeting m set m.attendeeId = :attendeeId where m.meetingId = :meetingId")
    Integer setAttendeeIdByMeetingId(Long attendeeId, Long meetingId);

    //    @Query("SELECT m FROM Meeting m JOIN m.attendeeList a WHERE a.accountId = :accountId AND m.startDateTime >= :fromDate AND m.endDateTime <= :toDate")
//    List<Meeting> findByAccountIdAndDateRange(@Param("accountId") Long accountId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
//    @Query("SELECT m FROM Meeting m JOIN m.attendeeList a WHERE m.startDateTime >= :fromDate AND m.endDateTime <= :toDate AND a.attendeeId IN (:attendeeIds)")
//    List<Meeting> findByDateRangeAndAttendeeIds( @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate,  @Param("attendeeIds") List<Long> attendeeIds);
//    @Query("SELECT m FROM Meeting m WHERE m.attendeeId IN (:attendeeIds) AND m.sprintStartTime BETWEEN :fromDate AND :toDate")
//    List<Meeting> findMeetingByAttendeeIdsAndToDateAndFromDate(@Param("attendeeIds") List<Long> attendeeIds, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
    @Query("SELECT DISTINCT a.attendeeId FROM Attendee a WHERE a.accountId = :accountId")
    List<Long> findByAccountId(Long accountId);

//    @Query("SELECT m FROM Meeting m JOIN m.attendeeList a WHERE a.attendeeId IN :attendeeIds AND m.startDateTime BETWEEN :fromDate AND :toDate")
//    List<Meeting> findByAttendeeIdInAndTimeRange(List<Long> attendeeIds, LocalDateTime fromDate, LocalDateTime toDate);

    List<Meeting> findByStartDateTimeAndEndDateTimeAndAttendeeIdIn(LocalDateTime fromDate, LocalDateTime toDate, List<Long> attendeeIds);

    //    List<Meeting>findByTimeRangeAndAttendeeIdIn(LocalTime fromTime, LocalTime toTime, List<Long> attendeeIds)
    List<Meeting> findByStartDateTimeBetweenAndEndDateTimeBetweenAndAttendeeIdIn(LocalDateTime fromDateTime, LocalDateTime toDateTime, LocalDateTime fromEndDateTime, LocalDateTime toEndDateTime, List<Long> attendeeIds);

    List<Meeting> findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqual(LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Meeting> findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqualAndOrgId(LocalDateTime startDateTime, LocalDateTime endDateTime, Long orgId);

    List<Meeting> findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqualOrderByStartDateTimeAsc(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("select m from Meeting m where m.startDateTime >= :fromDate and m.startDateTime <= :toDate and m.teamId in :teamIds order by m.startDateTime asc")
    List<Meeting> findByStartDateTimeAndTeamIds(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("accountIds") List<Long> teamIds);

    List<Meeting> findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqualAndRecurringMeetingRecurringMeetingIdIsNullOrderByStartDateTimeAsc(LocalDateTime fromDate , LocalDateTime toDate);
    List<Meeting> findByRecurringMeetingRecurringMeetingIdIsNullOrderByMeetingIdDesc();

    @Query(value = "SELECT m.* " +
            "FROM tse.meeting m " +
            "INNER JOIN tse.user_account ua ON ua.account_id = m.organizer_account_id " +
            "INNER JOIN tse.tse_users tu ON tu.user_id = ua.user_id " +
            "WHERE m.reminder_time > 0 " +
            "AND (m.start_date_time AT TIME ZONE 'utc' AT TIME ZONE tu.time_zone) BETWEEN " +
            "      ((NOW() + INTERVAL '1 MINUTE' * (m.reminder_time - 1)) AT TIME ZONE tu.time_zone) " +
            "      AND ((NOW() + INTERVAL '1 MINUTE' * (m.reminder_time)) AT TIME ZONE tu.time_zone);",nativeQuery = true)
    List<Meeting> findScheduledMeetings();

    @Query(value = "SELECT m.* " +
            "FROM tse.meeting m " +
            "INNER JOIN tse.user_account ua ON ua.account_id = m.organizer_account_id " +
            "INNER JOIN tse.tse_users tu ON tu.user_id = ua.user_id " +
            "WHERE m.reminder_time > 0 " +
            "AND (m.start_date_time AT TIME ZONE 'utc' AT TIME ZONE tu.time_zone) BETWEEN " +
            "      (NOW() AT TIME ZONE tu.time_zone) " +
            "      AND ((NOW() + INTERVAL '1 MINUTE' ) AT TIME ZONE tu.time_zone);",nativeQuery = true)
    List<Meeting> findStartedMeetings();

    @Query(value = "SELECT m.* " +
            "FROM tse.meeting m " +
            "INNER JOIN tse.user_account ua ON ua.account_id = m.organizer_account_id " +
            "INNER JOIN tse.tse_users tu ON tu.user_id = ua.user_id " +
            "WHERE m.reminder_time > 0 " +
            "AND (m.end_date_time AT TIME ZONE 'utc' AT TIME ZONE tu.time_zone) BETWEEN " +
            "      ((NOW() - INTERVAL '1 MINUTE' ) AT TIME ZONE tu.time_zone) " +
            "      AND (NOW() AT TIME ZONE tu.time_zone);",nativeQuery = true)
    List<Meeting> findEndedMeetings();
    
//    @Modifying
//    @Query("update Meeting m set m.isCancelled = true where m.organizerAccountId = :accountId and m.orgId = :orgId and m.recurringMeetingId is null")
//    void cancelNonRecurringMeetingsForOrg(@Param("accountId") Long accountId, @Param("orgId") Long orgId);

    List<Meeting> findByOrgIdAndOrganizerAccountIdAndIsCancelledAndStartDateTimeGreaterThan(
            @Param("orgId") Long orgId,
            @Param("accountId") Long accountId,
            @Param("isCancelled") Boolean isCancelled,
            @Param("startDateTime") LocalDateTime startDateTime);

    List<Meeting> findByTeamIdAndOrganizerAccountIdAndIsCancelledAndStartDateTimeGreaterThan(
            @Param("teamId") Long teamId,
            @Param("accountId") Long accountId,
            @Param("isCancelled") Boolean isCancelled,
            @Param("startDateTime") LocalDateTime startDateTime);

    @Query("SELECT m FROM Meeting m, Attendee a WHERE m = a.meeting AND m.startDateTime > :currentDateTime AND m.isCancelled = false AND a.accountId = :accountId AND m.orgId = :orgId")
    List<Meeting> findByStartDateTimeGreaterThanAndIsCancelledFalseAndAttendeeList_AccountIdAndOrgId(
            LocalDateTime currentDateTime,
            Long accountId,
            Long orgId);

    @Query("SELECT m FROM Meeting m, Attendee a WHERE m = a.meeting AND m.startDateTime > :currentDateTime AND m.isCancelled = false AND a.accountId = :accountId AND a.teamId = :teamId")
    List<Meeting> findByStartDateTimeGreaterThanAndIsCancelledFalseAndAttendeeList_AccountIdAndTeamId(
            LocalDateTime currentDateTime,
            Long accountId,
            Long teamId);

    @Query("SELECT m FROM Meeting m WHERE m.referenceEntityTypeId = :referenceEntityTypeId AND UPPER(m.referenceEntityNumber) like UPPER(:referenceEntityNumber) AND m.teamId = :teamId AND m.isCancelled = false")
    List<Meeting> findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Integer referenceEntityTypeId, String referenceEntityNumber, Long teamId);

    @Query("Select new com.tse.core_application.dto.MeetingProgress(m.meetingId, m.duration, a.attendeeDuration) from Meeting m join Attendee a on m.attendeeId = a.attendeeId where a.accountId in :accountIds and a.teamId in :teamIds and (m.startDateTime > :fromDate and m.startDateTime < :toDate)")
    List<MeetingProgress> findAllMeetingProgressBetweenDates (List<Long> accountIds, List<Long> teamIds, LocalDateTime fromDate, LocalDateTime toDate);

    @Query("SELECT NEW com.tse.core_application.custom.model.MeetingDetails(m.meetingNumber, m.meetingId, m.title, m.venue, m.startDateTime, m.agenda, m.meetingKey) FROM Meeting m where DATE(m.startDateTime) = :date AND m.attendeeId IN (SELECT a.attendeeId FROM Attendee a WHERE a.accountId IN :accountIdList)")
    List<MeetingDetails> findUserAllMeetingsForDate (LocalDate date, List<Long> accountIdList);

    @Query("SELECT NEW com.tse.core_application.custom.model.MeetingDetails(m.meetingNumber, m.meetingId, m.title, m.venue, m.startDateTime, m.agenda, m.meetingKey) FROM Meeting m where DATE(m.startDateTime) = :date AND m.attendeeId IN (SELECT a.attendeeId FROM Attendee a WHERE a.accountId IN :accountIdList) AND m.orgId = :orgId")
    List<MeetingDetails> findUserAllMeetingsForDateAndOrg (LocalDate date, List<Long> accountIdList, Long orgId);

    @Query("SELECT NEW com.tse.core_application.custom.model.MeetingDetails(m.meetingNumber, m.meetingId, m.title, m.venue, m.startDateTime, m.agenda, m.meetingKey) FROM Meeting m where DATE(m.startDateTime) = :date AND m.attendeeId IN (SELECT a.attendeeId FROM Attendee a WHERE a.accountId IN :accountIdList) AND m.teamId = :teamId")
    List<MeetingDetails> findUserAllMeetingsForDateAndTeam (LocalDate date, List<Long> accountIdList, Long teamId);

    @Query("SELECT NEW com.tse.core_application.custom.model.MeetingDetails(m.meetingNumber, m.meetingId, m.title, m.venue, m.startDateTime, m.agenda, m.meetingKey) FROM Meeting m where DATE(m.startDateTime) = :date AND m.attendeeId IN (SELECT a.attendeeId FROM Attendee a WHERE a.accountId IN :accountIdList) AND m.projectId = :projectId")
    List<MeetingDetails> findUserAllMeetingsForDateAndProject (LocalDate date, List<Long> accountIdList, Long projectId);

    @Query("SELECT count(m) FROM Meeting m WHERE m.orgId = :orgId")
    Integer findMeetingsCountByOrgId(Long orgId);

    @Query("SELECT m FROM Meeting m where ((:fieldName = 'orgId' AND m.orgId =:entityId) " +
            "OR (:fieldName = 'buId' AND m.buId =:entityId) " +
            "OR (:fieldName = 'projId' AND m.projectId =:entityId) " +
            "OR (:fieldName = 'teamId' AND m.teamId =:entityId)) AND m.meetingNumber=:meetingNumber")
    Meeting findMeetingByEntityAndMeetingNumber(@Param("fieldName") String fieldName,
                                                @Param("entityId") Long entityId, @Param("meetingNumber") String meetingNumber);
}