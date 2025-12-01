package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Setter
@Getter
public class OrgIdOrgName {
    Long orgId;
    String organizationName;

    public OrgIdOrgName(Long orgId, Object organizationName){
        this.organizationName = (String) organizationName;
        this.orgId = orgId;
    }
}
