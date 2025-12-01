package com.tse.core_application.model;

import lombok.*;
import org.springframework.lang.Nullable;
import java.time.LocalDate;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSheetRequest {

    @Nullable
    private Long orgId;

    @Nullable
    private Long buId;

    @Nullable
    private Long projectId;

    @Nullable
    private Long teamId;

    // for task entity, this is entityTypeId
    @Nullable
    private Integer entityTypeId;

    // for task entity, this is taskId
    @Nullable
    private Long entityId;

    @Nullable
    private Long userId;

    @Nullable
    private List<Long> accountIdList;

    @Nullable
    private LocalDate fromDate;

    @Nullable
    private LocalDate toDate;

    @Nullable
    private String timePeriod;

}

