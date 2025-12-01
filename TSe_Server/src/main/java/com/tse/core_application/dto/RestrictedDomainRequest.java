package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestrictedDomainRequest {

    private Long restrictedDomainId;

    private String domain;

    private String displayName;

    private Boolean isPersonalAllowed = false;

    private Boolean isOrgRegistrationAllowed = false;
}
