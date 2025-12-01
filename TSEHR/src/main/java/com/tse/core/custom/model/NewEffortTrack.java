package com.tse.core.custom.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEffortTrack {
    @Nullable
    private Integer newEffort;
    @Nullable
    private LocalDate newEffortDate;
}
