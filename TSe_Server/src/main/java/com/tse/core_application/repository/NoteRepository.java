package com.tse.core_application.repository;

import com.tse.core_application.custom.model.NoteId;
import com.tse.core_application.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query(value = "select new com.tse.core_application.custom.model.NoteId (max(n.noteId)) from Note n")
    NoteId getMaxNoteId();

    Note findByNoteLogId(Long noteLogId);

    @Modifying
    @Query("update Note n set n.isDeleted = :isDeleted where n.noteLogId IN (:noteLogId)")
    Integer setIsDeletedByNoteLogIdIn(List<Long> noteLogId, Integer isDeleted);

    @Query("SELECT count(n) FROM Note n WHERE n.task.fkOrgId.orgId = :orgId")
    Integer findNotesCountByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Note n WHERE n.task.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);
}
