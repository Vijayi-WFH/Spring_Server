package com.tse.core_application.dto;

import com.tse.core_application.custom.model.OrgIdOrgName;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.List;

public class GeoFenceActiveDetailsResponseDto {
        Long orgId;
        String organizationName;
        Boolean geofenceAttendenceActive;
        Boolean geofenceAdminActive;

        public GeoFenceActiveDetailsResponseDto(Long orgId, Object organizationName, Boolean geofenceAttendenceActive,Boolean geofenceAdminActive){
            this.organizationName = (String) organizationName;
            this.orgId = orgId;
            this.geofenceAttendenceActive = geofenceAttendenceActive;
            this.geofenceAdminActive = geofenceAdminActive;
        }
    }

