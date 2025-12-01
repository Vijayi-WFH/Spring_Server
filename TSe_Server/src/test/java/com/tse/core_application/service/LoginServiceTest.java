package com.tse.core_application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.custom.model.CustomRoleAction;
import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.custom.model.UserIdFirstLastName;
import com.tse.core_application.dto.AuthResponse;
import com.tse.core_application.dto.User;
import com.tse.core_application.exception.UnauthorizedLoginException;
import com.tse.core_application.model.AuthRequest;
import com.tse.core_application.repository.AuditRepository;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.JWTUtil;
import com.tse.core_application.utils.PBKDF2Encoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoginServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PBKDF2Encoder passwordEncoder;

    @Mock
    private IOtpService otpService;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private AuditService auditService;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessDomainService accessDomainService;

    @Mock
    private RoleActionService roleActionService;

    @InjectMocks
    private LoginService loginService;

    @Mock
    private com.tse.core_application.model.User user;

    ObjectMapper objectMapper = new ObjectMapper();


    /** Verify response when otp fails */
    @Test
    public void testGetToken_verifyOtpFails() {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setDeviceUniqueIdentifier("device123");
        authRequest.setUsername("user@example.com");
        authRequest.setOtp("1234");

        when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn("FAILED");

        ResponseEntity<AuthResponse> response = loginService.getToken(authRequest, "UTC");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // Verify response when otp is correct
//    @Test
//    public void testGetToken_userNotFound() {
//        AuthRequest authRequest = new AuthRequest();
//        authRequest.setDeviceUniqueIdentifier("device123");
//        authRequest.setUsername("user@example.com");
//        authRequest.setOtp("1234");
//
//        lenient().when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn(Constants.SUCCESS);
//        lenient().when(userService.findByUsername(anyString(), anyString(), anyString())).thenReturn(null);
//
//        ResponseEntity<AuthResponse> response = loginService.getToken(authRequest, "UTC");
//
//        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
//    }

    /** Verify token returned is not null */
    @Test
    public void testGetToken_verifyJwtTokenNotNull() {

        AuthRequest authRequest = new AuthRequest();
        authRequest.setDeviceUniqueIdentifier("dummydevice123");
        authRequest.setUsername("user@example.com");
        authRequest.setOtp("123456");

        when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn(Constants.SUCCESS);
        when(passwordEncoder.encode(anyString())).thenReturn("DummyString");
        when(userService.findByUsername(anyString(), anyString(), anyString())).thenReturn(new User());
        when(userService.getAccountIdsForUser(any(User.class))).thenReturn(List.of(1L, 2L));
        when(userService.getUserIdFirstLastNameByPrimaryEmail(anyString())).thenReturn(new UserIdFirstLastName());
        when(accessDomainService.getAllActiveAccessDomainsByAllAccountIds(anyList())).thenReturn(List.of(new CustomAccessDomain()));
        when(roleActionService.getAllRoleActionsByAccessDomains(anyList())).thenReturn(List.of(new CustomRoleAction()));
        when(jwtUtil.generateToken(any(User.class), anyList())).thenReturn("jwtutil123");

        ResponseEntity<AuthResponse> response = loginService.getToken(authRequest, "UTC");
        assertNotNull(response.getBody().getToken());
    }


    /** Timezone matches user's timezone */
    @Test
    public void testValidateAndUpdateLoginUserTimeZone_timeZoneMatches() {
        user = new com.tse.core_application.model.User();
        user.setUserId(1L);
        user.setTimeZone("Asia/Kolkata");

        com.tse.core_application.model.User check = loginService.validateAndUpdateLoginUserTimeZone(user, "Asia/Kolkata");
        assertEquals(user, check);
    }


    /** Timezone doesn't match user's timezone */
    @Test
    public void testValidateAndUpdateLoginUserTimeZone_timeZoneNotMatches() {

        user = new com.tse.core_application.model.User();
        user.setUserId(1L);
        user.setTimeZone("Dubai");

//        when(userRepository.updateTimeZoneByUserId("Dubai", 1L)).thenReturn(1);
//        when(auditService.auditForDifferentTimeZone(user, "Dubai")).thenReturn(new Audit());

        com.tse.core_application.model.User check = loginService.validateAndUpdateLoginUserTimeZone(user, "Asia/Kolkata");
        assertEquals(user, check);
    }

    /** Test: Response Body is not null AND Response Token is not null  */
    @Test
    public void testGetFormattedLoginResponse_responseNotNull(){
        AuthRequest newAuthRequest = new AuthRequest();
        newAuthRequest.setDeviceUniqueIdentifier("device1234");
        newAuthRequest.setUsername("user@example.com");
        newAuthRequest.setOtp("123456");

        AuthResponse newAuthResponse = new AuthResponse();
        newAuthResponse.setToken("jwt123");
        newAuthResponse.setError("Error");

        ResponseEntity<AuthResponse> response = new ResponseEntity<AuthResponse>(newAuthResponse, HttpStatus.OK);
        ResponseEntity<Object> result = loginService.getFormattedLoginResponse(response, newAuthRequest);
        Object body = result.getBody();
        RestResponseWithData responseData = (RestResponseWithData)body;

        HashMap<String, Object> map = objectMapper.convertValue(responseData.getData(), HashMap.class);

        assertEquals("jwt123", map.get("token"));
        assertNull(map.get("error"));
        assertEquals(Integer.valueOf(200), responseData.getStatus());
        assertEquals("success", responseData.getMessage());
    }

    /**Test: Response Body is Null */
    @Test(expected = UnauthorizedLoginException.class)
    public void testGetFormattedLoginResponse_responseNull(){

        AuthRequest newAuthRequest = new AuthRequest();
        newAuthRequest.setDeviceUniqueIdentifier("device1234");
        newAuthRequest.setUsername("user@example.com");
        newAuthRequest.setOtp("123456");

        ResponseEntity<AuthResponse> response = new ResponseEntity<AuthResponse>((AuthResponse) null, HttpStatus.OK);
        ResponseEntity<Object> result = loginService.getFormattedLoginResponse(response, newAuthRequest);
    }

}







