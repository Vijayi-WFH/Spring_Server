package com.tse.core_application.dto;

import com.tse.core_application.model.Country;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteResponse {

    // primary info
    private String inviteId;
    private String primaryEmail;
    private String firstName;
    private String middleName;
    private String lastName;
    private String organizationName;

//     user info in case the user is already registered
    private Boolean isPrimaryEmailPersonal;
    private String alternateEmail;
    private Boolean isAlternateEmailPersonal;
    private String personalEmail;
    private String currentOrgEmail;
    private String givenName;
    private String locale;
    private String city;
    private Integer highestEducation;
    private String secondHighestEducation;
    private Integer gender;
    private Integer ageRange;
    private Country fkCountryId;

    // device info
    private String deviceOs;
    private String deviceOsVersion;
    private String deviceMake;
    private String deviceModel;
}
