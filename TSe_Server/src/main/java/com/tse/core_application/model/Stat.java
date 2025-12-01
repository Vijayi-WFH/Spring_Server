package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Stat {

    private String status;
    private Integer displayOrder;
    private Integer percent;
    private Integer count;
}
