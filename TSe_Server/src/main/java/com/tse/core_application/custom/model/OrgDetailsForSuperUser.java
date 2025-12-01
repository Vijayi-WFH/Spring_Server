package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class OrgDetailsForSuperUser {
     Long orgId;
     String organizationName;

     String adminEmail;
     Boolean isDisabled;

     Integer maxBuCount;

     Integer maxProjectCount;

     Integer maxTeamCount;

     Integer maxUserCount;

     Long maxMemoryQuota;

     String ownerEmail;

     Boolean paidSubscription;

     Boolean onTrial;

     String ownerFirstName;

     String ownerLastName;

     Boolean isGeoFencingAllowed ;
     Boolean isGeoFencingActive;

     public OrgDetailsForSuperUser(Long orgId, Object orgName, Boolean isDisabled, Integer maxBuCount,
                                   Integer maxProjectCount, Integer maxTeamCount, Integer maxUserCount,
                                   Long maxMemoryQuota, Object ownerEmail, Boolean paidSubscription,
                                   Boolean onTrial, Object adminEmail, Object ownerFirstName, Object ownerLastName, Boolean isGeoFencingAllowed,
                                   Boolean isGeoFencingActive) {
          this.orgId = orgId;
          this.organizationName = (String) orgName;
          this.isDisabled = isDisabled;
          this.maxBuCount = maxBuCount;
          this.maxMemoryQuota = maxMemoryQuota;
          this.maxProjectCount = maxProjectCount;
          this.ownerEmail = (String) ownerEmail;
          this.maxTeamCount = maxTeamCount;
          this.maxUserCount = maxUserCount;
          this.paidSubscription = paidSubscription;
          this.onTrial = onTrial;
          this.adminEmail = (String) adminEmail;
          this.ownerFirstName = (String) ownerFirstName;
          this.ownerLastName = (String) ownerLastName;
          this.isGeoFencingAllowed = isGeoFencingAllowed;
          this.isGeoFencingActive = isGeoFencingActive;
     }
}
