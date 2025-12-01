package com.tse.core_application.dto.performance_notes;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PerfNoteFilters {
    private String taskNumber;
    private Long assignedTo;
    private Long postedBy;
    private LocalDate fromDate;
    private LocalDate toDate;
}
