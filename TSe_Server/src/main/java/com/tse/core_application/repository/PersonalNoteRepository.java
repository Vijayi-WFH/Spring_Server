package com.tse.core_application.repository;

import com.tse.core_application.model.personal_task.PersonalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PersonalNoteRepository extends JpaRepository<PersonalNote, Long> {

    @Query("SELECT n FROM PersonalNote n WHERE n.noteId = :noteId AND n.isDeleted = false")
    Optional<PersonalNote> findActiveNoteById(Long noteId);
}
