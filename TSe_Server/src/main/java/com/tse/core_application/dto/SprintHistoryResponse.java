package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;

@Getter
@Setter
public class SprintHistoryResponse {
    private Long sprintId;
    private String modifiedBy;
    private LocalDateTime modifiedOn;
    private HashMap<String, String> fieldName;
    private HashMap<String, Object> oldValue;
    private HashMap<String, Object> newValue;
    private HashMap<String, Object> message;
    private Long version;
}
