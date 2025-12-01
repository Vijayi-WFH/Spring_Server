package com.tse.core_application.repository;

import com.tse.core_application.model.performance_notes.PerfNoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerfNoteHistoryRepository extends JpaRepository<PerfNoteHistory, Long> {

    List<PerfNoteHistory> findAllByPerfNoteId(Long perfNoteId);

    Boolean existsByVersion (int version);
}
