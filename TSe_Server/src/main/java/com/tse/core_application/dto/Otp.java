package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Otp {
	
	@NotNull(message = ErrorConstant.DEVICE_UNIQUE_ID_ERROR)
	private String deviceUniqueIdentifier;

	private String otp;

	@NotNull(message = ErrorConstant.USERNAME_ERROR)
	private String username;

	private Boolean isOtpGenerated;

}
