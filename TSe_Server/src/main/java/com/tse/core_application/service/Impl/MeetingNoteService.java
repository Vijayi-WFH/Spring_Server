package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.MeetingNoteDto;
import com.tse.core_application.model.Meeting;
import com.tse.core_application.model.MeetingNote;
import com.tse.core_application.repository.MeetingNoteRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class MeetingNoteService {
    @Autowired
    private MeetingNoteRepository meetingNoteRepository;

    public void addOrUpdateMeetingNotes (List<MeetingNoteDto> meetingNoteDtoList, Meeting meeting, Long accountId) {
        if (meetingNoteDtoList != null && !meetingNoteDtoList.isEmpty()) {
            List<MeetingNote> meetingNoteList = new ArrayList<>();
            for (MeetingNoteDto meetingNoteDto : meetingNoteDtoList) {
                if (meetingNoteDto.getMeetingNoteId() != null) {
                    MeetingNote meetingNoteDb = meetingNoteRepository.findByMeetingNoteId(meetingNoteDto.getMeetingNoteId());
                    if (meetingNoteDb == null || meetingNoteDb.getIsDeleted() || (meetingNoteDb.getMeeting() != null && !Objects.equals(meetingNoteDb.getMeeting().getMeetingId(), meeting.getMeetingId()))) {
                        continue;
                    }
                    MeetingNote meetingNote = new MeetingNote();
                    BeanUtils.copyProperties(meetingNoteDb, meetingNote);
                    meetingNote.setMeetingNote(sanitizeMeetingNote(meetingNoteDto.getMeetingNote()));
                    meetingNote.setModifiedByAccountId(accountId);
                    meetingNote.setIsImportant(meetingNoteDto.getIsImportant());
                    meetingNote.setIsDeleted(meetingNoteDto.getIsDeleted());
                    meetingNote.setMeeting(meeting);
                    meetingNoteList.add(meetingNote);
                }
                else {
                    MeetingNote meetingNote = new MeetingNote();
                    meetingNote.setMeetingNote(sanitizeMeetingNote(meetingNoteDto.getMeetingNote()));
                    meetingNote.setPostedByAccountId(accountId);
                    meetingNote.setIsImportant(meetingNoteDto.getIsImportant());
                    meetingNote.setMeeting(meeting);
                    meetingNoteList.add(meetingNote);
                }
            }
            if (!meetingNoteList.isEmpty()) {
                meetingNoteRepository.saveAll(meetingNoteList);
            }
        }
    }

    private String sanitizeMeetingNote (String meetingNote) {
        // Start with the basic safe list and add custom tags and attributes
        Safelist customSafeList = Safelist.basic();
        customSafeList.addTags("s", "del")
                .addAttributes("span", "style", "data-email", "class");
        return Jsoup.clean(meetingNote, customSafeList);
    }
}
