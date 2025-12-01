package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;

@Getter
@Setter
public class TaskHistory_FieldMapping_Response {

    private String taskNumber;
    private String modifiedBy;
    private LocalDateTime modifiedOn;
    private HashMap<String, String> fieldName;
    private HashMap<String, Object> oldValue;
    private HashMap<String, Object> newValue;
    private HashMap<String, Object> message;
    private Long version;
}
