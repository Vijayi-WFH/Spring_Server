package com.tse.core_application.service;

import com.tse.core_application.model.AuthRequest;
import com.tse.core_application.dto.AuthResponse;
import org.springframework.http.ResponseEntity;

public interface ILoginService {

	ResponseEntity<AuthResponse> getToken(AuthRequest authRequest, String timeZone);

	ResponseEntity<AuthResponse> getTokenForGoogleSSO(String username,String sub, String timezone);

	ResponseEntity<Object> getFormattedLoginResponse(ResponseEntity<AuthResponse> response, AuthRequest request);
}
