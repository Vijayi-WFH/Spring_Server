package com.tse.core_application.dto;

import lombok.Data;

@Data
public class NewOrgMemberRequest {
    private Long orgId;
    private String userName;
}
