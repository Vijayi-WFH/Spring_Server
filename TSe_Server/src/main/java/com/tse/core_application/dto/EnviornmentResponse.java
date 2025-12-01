package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnviornmentResponse {

    private Integer customEnvironmentId;
    private String environmentDisplayName;
    private String environmentDescription;
    private Integer entityTypeId;
    private Long entityId;
    private Boolean isActive;
    private Timestamp createdDateTime;
    private Timestamp UpdatedDateTime;

}
