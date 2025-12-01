package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
public class PriorityIdDescDisplayAs {

    private Integer priorityId;
    private String priorityDesc;
    private String priorityDisplayAs;
}
