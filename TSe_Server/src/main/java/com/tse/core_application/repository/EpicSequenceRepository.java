package com.tse.core_application.repository;

import com.tse.core_application.model.EpicSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

@Repository
public interface EpicSequenceRepository extends JpaRepository<EpicSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT es FROM EpicSequence es WHERE es.projectId = :projectId")
    EpicSequence findByProjectIdForUpdate(Long projectId);
}
