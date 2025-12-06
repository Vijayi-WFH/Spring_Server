package com.tse.core_application.repository;

import com.tse.core_application.model.MeetingAnalysisUploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MeetingAnalysisUploadedFileRepository extends JpaRepository<MeetingAnalysisUploadedFile, Long> {

    Boolean existsByMeetingIdAndModelIdInAndUnderProcessing(Long meetingId, List<Integer> modelIdList, boolean underProcess);

    MeetingAnalysisUploadedFile findByMeetingIdAndModelId(Long meetingId, Integer modelId);

    List<MeetingAnalysisUploadedFile> findByMeetingId(Long meetingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MeetingAnalysisUploadedFile mauf WHERE mauf.meetingId IN :meetingIds")
    void deleteByMeetingIdIn(List<Long> meetingIds);
}
