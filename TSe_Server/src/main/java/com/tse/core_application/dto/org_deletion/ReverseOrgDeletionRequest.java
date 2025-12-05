package com.tse.core_application.dto.org_deletion;

import lombok.*;

import javax.validation.constraints.NotNull;

/**
 * Request DTO for reversing organization deletion by Super Admin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReverseOrgDeletionRequest {

    @NotNull(message = "Organization ID is required")
    private Long orgId;
}
