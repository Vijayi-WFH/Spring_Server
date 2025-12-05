package com.tse.core_application.dto.org_deletion;

import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Request DTO for initiating organization deletion by Org Admin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestOrgDeletionRequest {

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    @Size(max = 2000, message = "Reason cannot exceed 2000 characters")
    private String reason;
}
