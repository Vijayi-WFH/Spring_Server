package com.tse.core_application.repository;

import com.tse.core_application.model.RecurringMeetingSequence;
import com.tse.core_application.model.TaskSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
public interface RecurringMeetingSequenceRepository extends JpaRepository<RecurringMeetingSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rms FROM RecurringMeetingSequence rms WHERE rms.orgId = :orgId")
    RecurringMeetingSequence findByOrgIdForUpdate(@Param("orgId") Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RecurringMeetingSequence rms WHERE rms.recurringMeetingId IN :recurringMeetingIds")
    void deleteByRecurringMeetingIdIn(List<Long> recurringMeetingIds);

}
