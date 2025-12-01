package com.tse.core_application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tse.core_application.dto.AuthResponse;
import com.tse.core_application.dto.RegistrationRequest;
import com.tse.core_application.dto.SignUpCompletionDetail;
import com.tse.core_application.model.User;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface IRegistrationService {

	String generateAndSendOtp(RegistrationRequest request) throws JsonProcessingException;

	ResponseEntity<AuthResponse> doOtpVerificationAndUserRegistration(RegistrationRequest request, String timeZone) throws IOException;

	ResponseEntity<Object> getFormattedGenerateOtpResponse(ResponseEntity<String> otp, RegistrationRequest request);

	ResponseEntity<Object> getFormattedSignUpResponse(ResponseEntity<AuthResponse> response, RegistrationRequest request);

	void addImage (RegistrationRequest request) throws IOException;

	String completeRegistration(SignUpCompletionDetail request, User user);

	void validateUserEmail (String email, RegistrationRequest registrationRequest);

	}
