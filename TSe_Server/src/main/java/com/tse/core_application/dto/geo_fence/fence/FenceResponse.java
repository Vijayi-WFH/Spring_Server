package com.tse.core_application.dto.geo_fence.fence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import com.tse.core_application.utils.DateTimeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FenceResponse {

    private Long id;
    private Long orgId;
    private String name;
    private GeoFence.LocationKind locationKind;
    private String siteCode;
    private String tz;
    private Double centerLat;
    private Double centerLng;
    private Integer radiusM;
    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdDatetime;
    private Long updatedBy;
    private LocalDateTime updatedDatetime;

    public static FenceResponse fromEntity(GeoFence fence) {
        return fromEntity(fence, null);
    }

    public static FenceResponse fromEntity(GeoFence fence, String timeZone) {
        FenceResponse response = new FenceResponse();
        response.setId(fence.getId());
        response.setOrgId(fence.getOrgId());
        response.setName(fence.getName());
        response.setLocationKind(fence.getLocationKind());
        response.setSiteCode(fence.getSiteCode());
        response.setTz(fence.getTz());
        response.setCenterLat(fence.getCenterLat());
        response.setCenterLng(fence.getCenterLng());
        response.setRadiusM(fence.getRadiusM());
        response.setIsActive(fence.getIsActive());
        response.setCreatedBy(fence.getCreatedBy());
        // Convert timestamps from server timezone to user timezone
        if (timeZone != null) {
            response.setCreatedDatetime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(fence.getCreatedDatetime(), timeZone));
            response.setUpdatedDatetime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(fence.getUpdatedDatetime(), timeZone));
        } else {
            response.setCreatedDatetime(fence.getCreatedDatetime());
            response.setUpdatedDatetime(fence.getUpdatedDatetime());
        }
        response.setUpdatedBy(fence.getUpdatedBy());
        return response;
    }
}
