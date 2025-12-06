package com.tse.core_application.repository;

import com.tse.core_application.model.performance_notes.PerfNoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PerfNoteHistoryRepository extends JpaRepository<PerfNoteHistory, Long> {

    List<PerfNoteHistory> findAllByPerfNoteId(Long perfNoteId);

    Boolean existsByVersion (int version);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerfNoteHistory pnh WHERE pnh.orgId = :orgId")
    void deleteByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerfNoteHistory pnh WHERE pnh.postedByAccountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}
