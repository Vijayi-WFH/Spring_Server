package com.tse.core_application.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TaskByLabelRequest {

    @NotNull
    private List<String> labels;
    @NotNull
    private Long teamId;
    private Long accountIdAssigned;
}
