package com.tse.core_application.repository;

import com.tse.core_application.model.personal_task.PersonalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalNoteRepository extends JpaRepository<PersonalNote, Long> {

    @Query("SELECT n FROM PersonalNote n WHERE n.noteId = :noteId AND n.isDeleted = false")
    Optional<PersonalNote> findActiveNoteById(Long noteId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PersonalNote pn WHERE pn.personalTaskId IN :personalTaskIds")
    void deleteByPersonalTaskIdIn(List<Long> personalTaskIds);
}
