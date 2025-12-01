package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ModelFetchedDto {
    @NotNull
    private Integer modelId;
    @NotNull
    private Boolean isFetched = false;
    @NotNull
    private Boolean isProblem = false;
}
