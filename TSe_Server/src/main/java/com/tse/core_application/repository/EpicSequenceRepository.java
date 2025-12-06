package com.tse.core_application.repository;

import com.tse.core_application.model.EpicSequence;
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
public interface EpicSequenceRepository extends JpaRepository<EpicSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT es FROM EpicSequence es WHERE es.projectId = :projectId")
    EpicSequence findByProjectIdForUpdate(Long projectId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EpicSequence es WHERE es.epicId IN :epicIds")
    void deleteByEpicIdIn(List<Long> epicIds);
}
