package com.tse.core_application.dto.org_deletion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReverseOrgDeletionRequest {

    @NotNull(message = "Organization ID is required")
    private Long orgId;
}
