package com.tse.core_application.repository;

import com.tse.core_application.custom.model.RecurringMeetingNumber;
import com.tse.core_application.model.RecurringMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringMeetingRepository extends JpaRepository<RecurringMeeting, Long> {

    @Query(value = "select new com.tse.core_application.custom.model.RecurringMeetingNumber (max (m.recurringMeetingNumber)) from RecurringMeeting m")
    RecurringMeetingNumber getMaxRecurringMeetingNumber();

    RecurringMeeting findByRecurringMeetingId(Long recurringMeetingId);

    List<RecurringMeeting> findByRecurringMeetingIdIn(List<Long> recurringMeetingIds);

    List<RecurringMeeting> findByRecurringMeetingEndDateTimeGreaterThanEqualAndRecurringMeetingStartDateTimeLessThanEqual(LocalDateTime fromDate,LocalDateTime toDate);

    List<RecurringMeeting> findByTeamId(Long teamId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RecurringMeeting rm WHERE rm.orgId = :orgId")
    void deleteByOrgId(Long orgId);

    @Query("SELECT rm.recurringMeetingId FROM RecurringMeeting rm WHERE rm.orgId = :orgId")
    List<Long> findAllRecurringMeetingIdsByOrgId(Long orgId);
}
