package com.tse.core_application.dto;

import com.tse.core_application.model.RecordedEffortsByDateTime;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class RecordEffortResponse {
    private Long taskId;
    private String taskNumber;
    private Integer taskTypeId;
    private Long teamId;
    private String teamCode;
    private Map<LocalDate, List<RecordedEffortsByDateTime>> recordedEffortsByDateTimeList;
}
