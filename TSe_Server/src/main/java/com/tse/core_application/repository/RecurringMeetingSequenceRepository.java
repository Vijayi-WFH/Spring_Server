package com.tse.core_application.repository;

import com.tse.core_application.model.RecurringMeetingSequence;
import com.tse.core_application.model.TaskSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

@Repository
public interface RecurringMeetingSequenceRepository extends JpaRepository<RecurringMeetingSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rms FROM RecurringMeetingSequence rms WHERE rms.orgId = :orgId")
    RecurringMeetingSequence findByOrgIdForUpdate(@Param("orgId") Long orgId);

}
