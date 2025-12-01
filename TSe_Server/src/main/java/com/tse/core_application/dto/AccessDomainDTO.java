package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccessDomainDTO {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Entity Type ID is required")
    private Integer entityTypeId;

    @NotNull(message = "Entity ID is required")
    private Integer entityId;

    @NotNull(message = "Role ID is required")
    private Integer roleId;

    private Integer workflowTypeId;

    @NotNull(message = "Member details are required")
    private MemberDetailsTeam memberDetailsTeam;

}
