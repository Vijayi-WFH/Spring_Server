package com.tse.core_application.repository;

import com.tse.core_application.custom.model.AttendeeId;
import com.tse.core_application.dto.MeetingAccountEntityMapDto;
import com.tse.core_application.model.Attendee;
import com.tse.core_application.model.Meeting;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    @Query(value = "select new com.tse.core_application.custom.model.AttendeeId (max(a.attendeeId)) from Attendee a")
    AttendeeId getMaxAttendeeId();
    List<AttendeeId> findByAccountId(Long accountId);
    @Query("SELECT a FROM Attendee a WHERE a.accountId = :accountId AND a.meeting.meetingId = :meetingId")
    Optional<Attendee> findByAccountIdAndMeetingId(@Param("accountId") Long accountId, @Param("meetingId") Long meetingId);

    @Modifying
    @Query("update Attendee a set a.attendeeInvitationStatusId = :attendeeInvitationStatusId where a.attendeeLogId IN (:attendeeLogIds)")
    Integer setAttendeeInvitationStatusIdByAttendeeLogIdIn(List<Long> attendeeLogIds, Integer attendeeInvitationStatusId);

    @Modifying
    @Query("update Attendee a set a.attendeeInvitationStatusId = :statusId, a.attendeeInvitationStatus = :status, " +
            "didYouAttend = NULL, attendeeDuration = NULL, initialEffortDateTime = NULL " +
            "where a.meeting.meetingId = :meetingId and a.accountId IN :accountIds")
    void updateAttendeeInvitationStatusIdAndAttendeeInvitationStatus(@Param("statusId") Integer statusId,  @Param("status")String status, @Param("meetingId") Long meetingId, @Param("accountIds") List<Long> accountIds );

    @Modifying
    @Query("update Attendee a set a.attendeeInvitationStatusId = :statusId, a.attendeeInvitationStatus = :status where a.attendeeLogId = :attendeeLogId")
    void setAttendeeInvitationStatusIdAndAttendeeInvitationStatusByAttendeeLogId(@Param("statusId") Integer statusId,  @Param("status")String status,  @Param("attendeeLogId") Long attendeeLogId);

    @Modifying
    @Query("update Attendee a set a.attendeeInvitationStatusId = :statusId, a.attendeeInvitationStatus = :status where a.accountId = :accountId and a.meeting.meetingId in :meetingIds")
    void setAttendeeInvitationStatusIdAndAttendeeInvitationStatusByMeetingIdAndAccountId(@Param("statusId") Integer statusId,  @Param("status")String status,  @Param("accountId") Long accountId, @Param("meetingIds") List<Long> meetingIds);

    //@Query("SELECT COUNT(a) > 0 FROM Attendee a WHERE a.accountId = :accountId AND a.meeting.meetingId = :meetingId")
    Boolean existsByAccountIdAndMeetingMeetingId(@Param("accountId") Long accountId, @Param("meetingId") Long meetingId);
    Attendee findByAccountIdAndMeetingMeetingId(@Param("accountId") Long accountId, @Param("meetingId") Long meetingId);

    @Query("select a.accountId from Attendee a where a.attendeeId=:attendeeId AND meeting=:meeting")
    List<Long> findAccountIdByAttendeeIdAndMeeting(Long attendeeId, Meeting meeting);

    @Query("select NEW com.tse.core_application.dto.MeetingAccountEntityMapDto(a.meeting.meetingId, a.accountId, a.teamId, 5) from Attendee a where a.accountId IN :accountIds and a.teamId IN :teamIds and a.attendeeInvitationStatusId = :statusId")
    List<MeetingAccountEntityMapDto> findMeetingIdByAccountIdInAndTeamIdIn(@Param("accountId") List<Long> accountIds, @Param("teamId") List<Long> teamIds, @Param("statusId") Integer statusId);

    @Query("select a.meeting.meetingId from Attendee a where a.accountId in :accountIds and a.teamId in :teamIds and a.attendeeInvitationStatusId = :statusId")
    List<Long> findAllMeetingIdsByAccountIdsAndTeamIds(@Param("accountIds") List<Long> accountIds, @Param("teamIds") List<Long> teamIds, @Param("statusId") Integer statusId);

    @Query("select a.meeting.meetingId from Attendee a where a.accountId in :accountIds and a.teamId IS NULL and a.projectId in :projectIds and a.attendeeInvitationStatusId = :statusId")
    List<Long> findAllMeetingIdsByAccountIdsAndProjectIds(@Param("accountIds") List<Long> accountIds, @Param("projectIds") List<Long> projectIds, @Param("statusId") Integer statusId);

    @Query("select a.meeting.meetingId from Attendee a where a.accountId in :accountIds and a.teamId IS NULL and a.projectId IS NULL and a.attendeeInvitationStatusId = :statusId")
    List<Long> findAllMeetingIdsByAccountIdsAndTeamIdIsNullAndProjectIdIsNull(@Param("accountIds") List<Long> accountIds, @Param("statusId") Integer statusId);

    @Query("select a.accountId from Attendee a where meeting=:meeting AND attendeeInvitationStatusId=:status")
    List<Long> findAccountIdByMeetingIdAndAttendeeInvitationStatus(Meeting meeting, Integer status);

    List<Attendee> findByTeamId(Long teamId);

    @Query("select a.meeting.meetingId from Attendee a where a.accountId in :accountIds and a.attendeeInvitationStatusId = :statusId")
    List<Long> findAllMeetingIdsByAccountIds(@Param("accountIds") List<Long> accountIds, @Param("statusId") Integer statusId);

    @Query("select NEW com.tse.core_application.dto.MeetingAccountEntityMapDto(a.meeting.meetingId, a.accountId, a.projectId, 4) from Attendee a where a.accountId IN :accountIds and a.teamId is null and a.projectId IN :projectIds and a.attendeeInvitationStatusId = :statusId")
    List<MeetingAccountEntityMapDto> findMeetingIdByAccountIdInAndProjectIdInAndTeamIdNull(@Param("accountId") List<Long> accountIds, @Param("teamId") List<Long> projectIds, @Param("statusId") Integer statusId);

    @Query("select NEW com.tse.core_application.dto.MeetingAccountEntityMapDto(a.meeting.meetingId, a.accountId) from Attendee a where a.accountId IN :accountIds and a.teamId is null and a.projectId is null and a.attendeeInvitationStatusId = :statusId")
    List<MeetingAccountEntityMapDto> findMeetingIdByAccountIdInAndTeamIdNullAndProjectIdNull(@Param("accountId") List<Long> accountIds, @Param("statusId") Integer statusId);

    List<Attendee> findByMeeting(Meeting meeting);

    List<Attendee> findByAttendeeId(Long attendeeId);

    @Query("SELECT a FROM Attendee a WHERE a.accountId in (:accountId) AND a.meeting.meetingId in (:meetingId)")
    Optional<List<Attendee>> findByAccountIdInAndMeetingIdIn(@Param("accountId") List<Long> accountId, @Param("meetingId") List<Long> meetingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attendee a WHERE a.meeting.meetingId IN :meetingIds")
    void deleteByMeetingIdIn(List<Long> meetingIds);
}

