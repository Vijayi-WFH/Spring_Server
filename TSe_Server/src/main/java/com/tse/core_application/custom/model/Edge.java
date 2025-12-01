package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Edge {
    private Long predecessorTaskId;
    private Long successorTaskId;
    private Integer relationTypeId;
    private Integer lagTime;
}
