package com.tse.core_application.repository;

import com.tse.core_application.model.performance_notes.PerfNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PerfNoteRepository extends JpaRepository<PerfNote, Long> {

    PerfNote findByPerfNoteId(Long perfNoteId);

    List<PerfNote> findAllByTaskIdAndIsDeletedFalse(Long taskId);

    Boolean existsByTaskIdAndFkPostedByAccountIdAccountIdAndIsDeletedFalse (Long taskId, Long postedByAccountId);

    @Query("SELECT count(p) FROM PerfNote p WHERE p.fkPostedByAccountId.accountId IN :accountIdList")
    Integer findPerfNotesCountByOrgId(List<Long> accountIdList);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerfNote pn WHERE pn.orgId = :orgId")
    void deleteByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PerfNote pn WHERE pn.fkPostedByAccountId.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}
