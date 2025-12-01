package com.tse.core_application.custom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CustomAccessDomain {

    private Long accountId;
    private Integer entityTypeId;
    private Long entityId;
    private Integer roleId;
    private Integer workflowTypeId;
}
