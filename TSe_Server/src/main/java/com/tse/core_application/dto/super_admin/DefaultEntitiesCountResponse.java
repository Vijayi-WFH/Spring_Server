package com.tse.core_application.dto.super_admin;

import lombok.Data;

@Data
public class DefaultEntitiesCountResponse {

    private Integer maxOrgCount;

    private Integer maxBuCount;

    private Integer maxProjectCount;

    private Integer maxTeamCount;

    private Integer maxUserCount;

    private Long maxMemoryQuota;

}
