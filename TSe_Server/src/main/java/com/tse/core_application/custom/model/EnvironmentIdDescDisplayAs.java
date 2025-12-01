package com.tse.core_application.custom.model;

import lombok.*;

@Value
@AllArgsConstructor
@Getter
@Setter
public class EnvironmentIdDescDisplayAs {

    Integer environmentId;
    String environmentDescription;
    String environmentDisplayName;
    Boolean isActive;
}
