package com.tse.core_application.repository;

import com.tse.core_application.model.MeetingSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

@Repository
public interface MeetingSequenceRepository extends JpaRepository<MeetingSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ms FROM MeetingSequence ms WHERE ms.orgId = :orgId")
    MeetingSequence findByOrgIdForUpdate(@Param("orgId") Long orgId);
}
