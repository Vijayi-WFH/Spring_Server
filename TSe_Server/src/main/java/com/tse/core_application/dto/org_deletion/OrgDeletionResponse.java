package com.tse.core_application.dto.org_deletion;

import lombok.*;

import java.time.LocalDate;

/**
 * Response DTO for organization deletion operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgDeletionResponse {

    private Long orgId;
    private String orgName;
    private String status;
    private String message;
    private LocalDate deletionScheduledDate;
    private Integer usersDeactivated;
}
