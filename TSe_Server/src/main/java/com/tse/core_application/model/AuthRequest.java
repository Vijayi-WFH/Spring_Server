package com.tse.core_application.model;

import com.tse.core_application.constants.ErrorConstant;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class  AuthRequest {
	@NotBlank(message = ErrorConstant.USERNAME_ERROR)
	@Email(message = ErrorConstant.EMAIL_CHECK_ERROR)
	private String username;
	@NotBlank(message = ErrorConstant.OTP_ERROR)
    private String otp;
	@NotBlank(message = ErrorConstant.DEVICE_UNIQUE_ID_ERROR)
    private String deviceUniqueIdentifier;

	private String authToken;
}
