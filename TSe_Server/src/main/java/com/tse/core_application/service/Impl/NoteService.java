package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.NoteId;
import com.tse.core_application.dto.personal_task.NoteRequest;
import com.tse.core_application.model.Note;
import com.tse.core_application.model.personal_task.PersonalNote;
import com.tse.core_application.model.personal_task.PersonalTask;
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.NoteRepository;
import com.tse.core_application.repository.PersonalNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private NoteHistoryService noteHistoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private PersonalNoteRepository personalNoteRepository;

    public List<Note> saveAllNotesOnAddTask(List<Note> notes, Task taskAdded) {
        List<Note> noteAdded = new ArrayList<>();
        List<Note> notesToAdd = new ArrayList<>();

        if (notes != null && !notes.isEmpty()) {
            Long maxNoteId = taskAdded.getNoteId() != null ? taskAdded.getNoteId() : getMaxNoteId();

            for (Note n : notes) {
                Note note = new Note();
                note.setNote(n.getNote());
                note.setIsDeleted(Constants.Task_Note_Status.NOTE_NOT_DELETED);
                note.setPostedByAccountId(n.getPostedByAccountId());
                note.setNoteId(maxNoteId);
                note.setTask(taskAdded);
                notesToAdd.add(note);
            }
            noteAdded = noteRepository.saveAll(notesToAdd);
        }
        return noteAdded;
    }

    public List<Note> updateAllNotes(List<Note> notes, Task task) {
        List<Note> notesToDelete = new ArrayList<>();
        List<Note> notesUpdated = new ArrayList<>();
        List<Note> noteToAdd = new ArrayList<>();
        if (notes != null && !notes.isEmpty()) {
            Long noteId = task.getNoteId() != null ? task.getNoteId() : getMaxNoteId();
            for (Note n : notes) {
                if (isNoteDeleted(n)) {
                    notesToDelete.add(n);
                } else {
                    if (isNoteUpdated(n)) {
                        notesUpdated.add(n);
                        noteHistoryService.addNoteHistory(n);
                        noteRepository.save(n);
                    } else {
                        if (isNewNote(n)) {
                            noteToAdd.add(n);
                        }
                    }
                }
            }
            notesUpdated.addAll(addNewNoteOnUpdateTask(noteToAdd, noteId, task));
            if (!notesToDelete.isEmpty())
                deleteAllNotes(notesToDelete);
        }
        return notesUpdated;
    }

    public List<Note> addNewNoteOnUpdateTask(List<Note> notes, Long noteId, Task taskAdded) {
        List<Note> notesToAdd = new ArrayList<>();
        if(notes != null && !notes.isEmpty()) {
            for (Note n : notes) {
                Note addNote = new Note();
                addNote.setNote(n.getNote());
                addNote.setIsDeleted(Constants.Task_Note_Status.NOTE_NOT_DELETED);
                addNote.setPostedByAccountId(n.getPostedByAccountId());
                addNote.setNoteId(noteId);
                addNote.setTask(taskAdded);
                notesToAdd.add(addNote);
            }
            return noteRepository.saveAll(notesToAdd);
        }
        return notesToAdd;
    }

    public boolean isNewNote(Note note) {
        boolean isNewNote = false;
        if (note.getVersion() == null) {
            isNewNote = true;
        }
        return isNewNote;
    }

    public Long getMaxNoteId() {
        NoteId noteIdDb = noteRepository.getMaxNoteId();
        if (noteIdDb.getNoteId() == null) {
            int intNoteId = 1;
            return (long) intNoteId;
        } else {
            return noteIdDb.getNoteId() + 1;
        }
    }

    public boolean isNoteUpdated(Note note) {
        boolean isUpdated = false;
        if (note.getIsUpdated() != null && note.getIsUpdated() == 1) {
            isUpdated = true;
        }
        return isUpdated;
    }

    public boolean isNoteDeleted(Note note) {
        boolean isDeleted = false;
        if (note.getIsDeleted() != null && note.getIsDeleted() == 1) {
            isDeleted = true;
        }
        return isDeleted;
    }

    public Integer deleteAllNotes(List<Note> notes) {
        List<Long> noteLogIds = new ArrayList<>();
        for (Note note : notes) {
            noteLogIds.add(note.getNoteLogId());
        }
        return noteRepository.setIsDeletedByNoteLogIdIn(noteLogIds, Constants.Task_Note_Status.NOTE_DELETED);
    }

    public List<Note> removeDeletedNotes(List<Note> notes) {
        notes = notes.stream().sorted(Comparator.comparingLong(Note::getNoteLogId)).collect(Collectors.toList());
        Iterator<Note> itr = notes.iterator();
        while (itr.hasNext()) {
            Note note = itr.next();
            if (isNoteDeleted(note)) {
                itr.remove();
            }
        }
        return notes;
    }


    // ************* Methods related to Personal Note *************************

    public List<PersonalNote> saveAllNotesOnAddPersonalTask(List<NoteRequest> noteRequestList, PersonalTask taskAdded) {
        List<PersonalNote> noteAdded = new ArrayList<>();
        List<PersonalNote> notesToAdd = new ArrayList<>();

        if (noteRequestList != null && !noteRequestList.isEmpty()) {

            for (NoteRequest n : noteRequestList) {
                PersonalNote note = new PersonalNote();
                note.setNote(n.getNote());
                note.setIsDeleted(Boolean.FALSE);
                note.setAccountId(taskAdded.getFkAccountId().getAccountId());
                note.setPersonalTask(taskAdded);
                notesToAdd.add(note);
            }
            noteAdded = personalNoteRepository.saveAll(notesToAdd);
        }
        return noteAdded;
    }

    public List<PersonalNote> addUpdateNotesOnUpdatePersonalTask(List<NoteRequest> noteRequestList, PersonalTask taskUpdated) {
        List<PersonalNote> updatedNotes = new ArrayList<>();

        for (NoteRequest n : noteRequestList) {

            if (n.getNoteId() == null) {
                PersonalNote note = new PersonalNote();
                note.setNote(n.getNote());
                note.setIsDeleted(Boolean.FALSE);
                note.setAccountId(taskUpdated.getFkAccountId().getAccountId());
                note.setPersonalTask(taskUpdated);
                updatedNotes.add(note);
            } else {
                PersonalNote note = personalNoteRepository.findActiveNoteById(n.getNoteId()).orElseThrow(() -> new EntityNotFoundException("Note not found"));
                if (n.getIsUpdated() != null && n.getIsUpdated()) {
                    note.setNote(n.getNote());
                    updatedNotes.add(note);
                } else if (n.getIsDeleted() != null && n.getIsDeleted()) {
                    note.setIsDeleted(Boolean.TRUE);
                    updatedNotes.add(note);
                }
            }
        }
        return personalNoteRepository.saveAll(updatedNotes);
    }

}
