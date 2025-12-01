package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BurnDownDetails {
    private int idealHours;
    private int remainingRecorded;
    private int remainingEarned;
}
