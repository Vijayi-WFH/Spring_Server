package com.tse.core_application.service.Impl;

import com.tse.core_application.model.Note;
import com.tse.core_application.model.NoteHistory;
import com.tse.core_application.repository.NoteHistoryRepository;
import com.tse.core_application.repository.NoteRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteHistoryService {

    @Autowired
    private NoteService  noteService;

    @Autowired
    private NoteHistoryRepository noteHistoryRepository;

    @Autowired
    private NoteRepository noteRepository;

    public NoteHistory addNoteHistory(Note note) {
        Note noteFoundDb = noteRepository.findByNoteLogId(note.getNoteLogId());
        NoteHistory noteHistory = new NoteHistory();
        BeanUtils.copyProperties(noteFoundDb, noteHistory);
        return noteHistoryRepository.save(noteHistory);
    }



}
