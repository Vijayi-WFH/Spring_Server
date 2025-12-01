package com.tse.core_application.repository;

import com.tse.core_application.model.PinnedStickyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PinnedStickyNoteRepository extends JpaRepository<PinnedStickyNote, Long> {

    Boolean existsByUserIdAndNoteId(Long userId, Long noteId);

    PinnedStickyNote findByUserIdAndNoteId(Long userId, Long noteId);

    @Query("SELECT psn.noteId FROM PinnedStickyNote psn WHERE psn.userId in :userIdList")
    List<Long> findAllPinnedNoteIdsByUserIn (List<Long> userIdList);

}
