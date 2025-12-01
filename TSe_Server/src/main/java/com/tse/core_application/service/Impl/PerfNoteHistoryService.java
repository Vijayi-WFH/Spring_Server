package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.performance_notes.PerfNoteHistoryResponse;
import com.tse.core_application.model.performance_notes.PerfNoteHistory;
import com.tse.core_application.repository.PerfNoteHistoryRepository;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PerfNoteHistoryService {
    private static final Logger logger = LogManager.getLogger(PerfNoteHistoryService.class.getName());

    @Autowired
    private PerfNoteHistoryRepository perfNoteHistoryRepository;

    public List<PerfNoteHistoryResponse> getPerfNoteHistory (Long perfNoteId, String timeZone) {
        List<PerfNoteHistory> historyList = perfNoteHistoryRepository.findAllByPerfNoteId(perfNoteId);
        List<PerfNoteHistoryResponse> perfNoteHistoryResponseList = new ArrayList<>();
        for (PerfNoteHistory history : historyList) {
            perfNoteHistoryResponseList.add(generatePerfNoteHistoryResponse(history, timeZone));
        }
        perfNoteHistoryResponseList.sort(Comparator.comparing(PerfNoteHistoryResponse::getVersion, Comparator.reverseOrder()));
        return perfNoteHistoryResponseList;
    }

    private PerfNoteHistoryResponse generatePerfNoteHistoryResponse (PerfNoteHistory perfNote, String timeZone) {
        PerfNoteHistoryResponse perfNoteHistoryResponse = new PerfNoteHistoryResponse();
        BeanUtils.copyProperties(perfNote, perfNoteHistoryResponse);
        perfNoteHistoryResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(perfNote.getCreatedDateTime(), timeZone));

        if (perfNote.getFkModifiedByAccountId() != null) {
            perfNoteHistoryResponse.setModifiedByAccount(new EmailFirstLastAccountId(perfNote.getFkModifiedByAccountId().getEmail(),
                    perfNote.getFkModifiedByAccountId().getAccountId(), perfNote.getFkModifiedByAccountId().getFkUserId().getFirstName(),
                    perfNote.getFkModifiedByAccountId().getFkUserId().getLastName()));
        }
        return perfNoteHistoryResponse;
    }
}
