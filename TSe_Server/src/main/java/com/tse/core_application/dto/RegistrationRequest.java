package com.tse.core_application.dto;

import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.tse.core_application.constants.ErrorConstant;

import com.tse.core_application.model.Country;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

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

	@Email(message = ErrorConstant.EMAIL_CHECK_ERROR)
	private String alternateEmail;

	private Boolean isAlternateEmailPersonal;

	private String givenName;

	@NotBlank(message = ErrorConstant.FIRST_NAME_ERROR)
	private String firstName;

	@NotBlank(message = ErrorConstant.LAST_NAME_ERROR)
	private String lastName;

	private String middleName;

//	private String locale; -- to be used in future

//	private String city; -- moved to SignUpCompletionDetail

	@Valid
	@NotNull(message = ErrorConstant.Country_Error)
	private Country country;

//	@NotNull(message = ErrorConstant.HIGHEST_EDUCATION_ERROR)
//	private Integer highestEducation; -- moved to SignUpCompletionDetail

//	private String secondHighestEducation; -- not required right now

//	@NotNull(message = ErrorConstant.GENDER_ERROR)
//	private Integer gender; -- moved to SignUpCompletionDetail

//	@NotNull(message = ErrorConstant.AGE_RANGE_ERROR)
//	private Integer ageRange; -- moved to SignUpCompletionDetail

//	@NotBlank(message = ErrorConstant.DEVICE_OS_ERROR)
	private String deviceOs;

//	@NotBlank(message = ErrorConstant.DEVICE_OS_VER_ERROR)
	private String deviceOsVersion;

//	@NotBlank(message = ErrorConstant.DEVICE_MAKE_ERROR)
	private String deviceMake;

//	@NotBlank(message = ErrorConstant.DEVICE_MODEL_ERROR)
	private String deviceModel;

	@NotBlank(message = ErrorConstant.DEVICE_UNIQUE_ID_ERROR)
	private String deviceUniqueIdentifier;

	@NotBlank(message = ErrorConstant.ORG_NAME_ERROR)
	@Size(min = 2, max = 100, message = ErrorConstant.MIN_MAX_ORG_NAME)
	private String organizationName;

	private String otp;

	@Nullable
	private String inviteId;

	@Nullable
	private String imageData;
}
