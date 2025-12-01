package com.tse.core_application.custom.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EntityOrderResponse {
    private Long entityId;
    private Integer entityTypeId;
    private String entityName;
    private String message;
    private LocalDateTime createdDateTime;
    private String lastMessageFromEmail;
    private String lastMessageFromFullName;
}
