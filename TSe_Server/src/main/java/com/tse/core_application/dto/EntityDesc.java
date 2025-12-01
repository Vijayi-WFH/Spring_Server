package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class EntityDesc {
    private Long entityId;
    private Integer entityTypeId;
    private String entityName;
    private Long accountId;

    public EntityDesc(Long entityId, Integer entityTypeId, Object entityName, Long accountId) {
        this.entityId = entityId;
        this.entityTypeId = entityTypeId;
        this.entityName = (String) entityName;
        this.accountId = accountId;
    }

    public EntityDesc(Long entityId, Integer entityTypeId, Long accountId) {
        this.entityId = entityId;
        this.entityTypeId = entityTypeId;
        this.accountId = accountId;
    }
}
