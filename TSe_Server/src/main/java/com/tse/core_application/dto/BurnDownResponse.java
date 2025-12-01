package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BurnDownResponse {
    private int maxMins;
    private Map<LocalDate, BurnDownDetails> burnDownDetails;

}
