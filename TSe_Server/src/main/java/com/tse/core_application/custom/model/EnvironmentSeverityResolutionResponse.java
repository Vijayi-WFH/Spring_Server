package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EnvironmentSeverityResolutionResponse {

    private List<EnvironmentIdDescDisplayAs> environment;
    private List<SeverityIdDescDisplayAs> severity;
    private List<ResolutionIdDescDisplayAs> resolution;
}
