package com.tse.core_application.dto.project;

import lombok.Data;

@Data
public class AccessDomainRequest {
    private Long accountId;
    private Integer roleId;
    private Boolean toDelete = false;
}
