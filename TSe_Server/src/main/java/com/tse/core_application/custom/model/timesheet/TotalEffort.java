package com.tse.core_application.custom.model.timesheet;

import lombok.*;
import org.springframework.lang.Nullable;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalEffort {
    @Nullable
    private Integer totalEffortMins;
    @Nullable
    private Integer totalEarnedTime;
    @Nullable
    private LocalDate totalEffortDate;
    @Nullable
    private List<EffortOnEntity> effortOnEntityList;
}

