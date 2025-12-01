package com.tse.core_application.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeoFenceOrgIdOrgName {
    Long orgId;
    String organizationName;
    Boolean isGeoFenceAdminPanel;
    Boolean isGeoFenceAttendence;
}
