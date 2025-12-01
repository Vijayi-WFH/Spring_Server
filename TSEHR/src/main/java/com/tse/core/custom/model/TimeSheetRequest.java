package com.tse.core.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import javax.validation.constraints.Null;
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

//        @Nullable
//        private TimePeriod timePeriod;

    @Nullable
    private String timePeriod;

}

