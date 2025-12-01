package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VelocityChartResponse {
    private String message;
    private List<VelocityChartDetails> velocityChartDetails;


}
