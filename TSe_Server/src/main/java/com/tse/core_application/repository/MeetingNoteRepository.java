package com.tse.core_application.repository;

import com.tse.core_application.model.MeetingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingNoteRepository extends JpaRepository<MeetingNote, Long> {
    MeetingNote findByMeetingNoteId(Long meetingNoteId);

    @Query(value = "SELECT * FROM tse.meeting_note WHERE meeting_id = :meetingId AND is_deleted = :isDeleted",nativeQuery = true)
    List<MeetingNote> findByFkMeetingIdMeetingIdAndIsDeleted(Long meetingId, Boolean isDeleted);
}
