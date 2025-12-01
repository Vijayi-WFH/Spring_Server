package com.tse.core_application.service;

import com.tse.core_application.dto.Otp;
import org.springframework.http.ResponseEntity;

public interface IOtpService {

	String getOtp(String deviceId);
	
	Otp putOtp(String deviceId);
	
	String verifyOtp(String deviceUniqueId, String userName, String otp);
	
	String sendOtp(Otp request);

	ResponseEntity<Object> getFormattedSendOtpResponse(ResponseEntity<String> otp, Otp request);

	boolean validateAllHeaders(String timeZone, String screenName);
}
