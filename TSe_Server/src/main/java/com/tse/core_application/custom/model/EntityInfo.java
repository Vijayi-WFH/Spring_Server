package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class EntityInfo {
    Long entityId;
    Integer entityTypeId;
    String entityName;

    public EntityInfo(Long entityId, Integer entityTypeId, Object entityName) {
        this.entityId = entityId;
        this.entityTypeId = entityTypeId;
        this.entityName = (String) entityName;
    }
}
