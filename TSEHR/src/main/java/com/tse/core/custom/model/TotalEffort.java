package com.tse.core.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

