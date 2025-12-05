package com.tse.core_application.dto.org_deletion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgDeletionResponse {

    private Long orgId;
    private String organizationName;
    private String status;
    private String message;
    private Timestamp scheduledDeletionDate;
    private Integer gracePeriodDays;
}
