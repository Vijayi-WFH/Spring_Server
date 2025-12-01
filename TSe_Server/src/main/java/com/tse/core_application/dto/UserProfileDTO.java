package com.tse.core_application.dto;

import com.tse.core_application.model.Country;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;

@Data
public class UserProfileDTO {
    private Long userId;
    private String primaryEmail;
    private Boolean isPrimaryEmailPersonal;
    private String alternateEmail;
    private Boolean isAlternateEmailPersonal;
    private String personalEmail;
    private String currentOrgEmail;
    private String firstName;
    private String lastName;
    private String middleName;
    private String givenName;
    private String locale;
    private String city;
    private Integer highestEducation;
    private String secondHighestEducation;
    private Integer gender;
    private Integer ageRange;
    private String timeZone;
    private Long countryId;
    private String countryName;
    private Timestamp createdDateTime;
    private Timestamp lastUpdatedDateTime;
    private String imageData;
}
