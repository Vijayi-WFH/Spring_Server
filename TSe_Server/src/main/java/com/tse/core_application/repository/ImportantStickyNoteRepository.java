package com.tse.core_application.repository;

import com.tse.core_application.model.ImportantStickyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportantStickyNoteRepository extends JpaRepository<ImportantStickyNote, Long> {

    boolean existsByUserIdAndNoteId(Long userId, Long noteId);

    ImportantStickyNote findByUserIdAndNoteId(Long userId, Long noteId);

    @Query("SELECT isn.noteId FROM ImportantStickyNote isn WHERE isn.userId in :userId")
    List<Long> findAllImportantNoteIdsByUserIn (List<Long> userId);

}
