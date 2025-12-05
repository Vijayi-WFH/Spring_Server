package com.tse.core_application.dto.org_deletion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestOrgDeletionRequest {

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    @Size(max = 500, message = "Deletion reason must not exceed 500 characters")
    private String reason;
}
