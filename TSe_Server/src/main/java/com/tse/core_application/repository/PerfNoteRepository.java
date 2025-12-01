package com.tse.core_application.repository;

import com.tse.core_application.model.performance_notes.PerfNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerfNoteRepository extends JpaRepository<PerfNote, Long> {

    PerfNote findByPerfNoteId(Long perfNoteId);

    List<PerfNote> findAllByTaskIdAndIsDeletedFalse(Long taskId);

    Boolean existsByTaskIdAndFkPostedByAccountIdAccountIdAndIsDeletedFalse (Long taskId, Long postedByAccountId);

    @Query("SELECT count(p) FROM PerfNote p WHERE p.fkPostedByAccountId.accountId IN :accountIdList")
    Integer findPerfNotesCountByOrgId(List<Long> accountIdList);
}
