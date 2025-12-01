package com.tse.core.dto.supplements;

import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.tse.core.constants.ErrorConstant;

import com.tse.core.model.supplements.Country;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = ErrorConstant.PRIMARY_EMAIL_ERROR)
    @Email(message = ErrorConstant.EMAIL_CHECK_ERROR)
    private String primaryEmail;

    @NotNull(message = ErrorConstant.IS_PRIMARY_EMAIL_ERROR)
    private Boolean isPrimaryEmailPersonal;

    private String alternateEmail;

    private Boolean isAlternateEmailPersonal;

    private String givenName;

    @NotBlank(message = ErrorConstant.FIRST_NAME_ERROR)
    private String firstName;

    @NotBlank(message = ErrorConstant.LAST_NAME_ERROR)
    private String lastName;

    private String middleName;

    private String locale;

    private String city;

    @Valid
    @NotNull(message = ErrorConstant.Country_Error)
    private Country country;

    @NotNull(message = ErrorConstant.HIGHEST_EDUCATION_ERROR)
    private Integer highestEducation;

    private String secondHighestEducation;

    @NotNull(message = ErrorConstant.GENDER_ERROR)
    private Integer gender;

    @NotNull(message = ErrorConstant.AGE_RANGE_ERROR)
    private Integer ageRange;

    @NotBlank(message = ErrorConstant.DEVICE_OS_ERROR)
    private String deviceOs;

    @NotBlank(message = ErrorConstant.DEVICE_OS_VER_ERROR)
    private String deviceOsVersion;

    @NotBlank(message = ErrorConstant.DEVICE_MAKE_ERROR)
    private String deviceMake;

    @NotBlank(message = ErrorConstant.DEVICE_MODEL_ERROR)
    private String deviceModel;

    @NotBlank(message = ErrorConstant.DEVICE_UNIQUE_ID_ERROR)
    private String deviceUniqueIdentifier;

    @NotBlank(message = ErrorConstant.ORG_NAME_ERROR)
    @Size(min = 2, max = 25, message = ErrorConstant.MIN_MAX_ORG_NAME)
    @Column(name = "organization_name", length = 25)
    private String organizationName;

    private String otp;
}
