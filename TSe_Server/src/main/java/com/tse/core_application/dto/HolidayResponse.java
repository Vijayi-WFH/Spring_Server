package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HolidayResponse {
    private Long holidayId;
    private LocalDate date;
    private boolean isRecurring;
    private String description;
}
