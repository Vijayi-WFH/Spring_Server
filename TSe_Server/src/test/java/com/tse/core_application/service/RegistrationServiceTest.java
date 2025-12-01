package com.tse.core_application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.dto.AuthResponse;
import com.tse.core_application.dto.Otp;
import com.tse.core_application.dto.RegistrationRequest;
import com.tse.core_application.exception.InvalidOtpException;
import com.tse.core_application.exception.ServerBusyException;
import com.tse.core_application.exception.UserAlreadyExistException;
import com.tse.core_application.model.Audit;
import com.tse.core_application.repository.AuditRepository;
import com.tse.core_application.service.Impl.AuditService;
import com.tse.core_application.service.Impl.RegistrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class RegistrationServiceTest {

    @Mock
    private IOtpService otpService;

    @Mock
    private IEMailService emailService;

    @Mock
    private AuditService auditService;

    @Mock
    private AuditRepository auditRepository;

    private AuthResponse authResponse;

    private RegistrationRequest request;

    @Spy
    @InjectMocks
    private RegistrationService registrationService;



    @Before
    public void setup() {
        ReflectionTestUtils.setField(registrationService, "emailSubject", "TSE One Time Password");

        authResponse = new AuthResponse();
        request = new RegistrationRequest();
        request.setPrimaryEmail("user@example.com");
        request.setDeviceUniqueIdentifier("device1234");
        request.setOtp("123456");

    }

    /** Test generateAndSendOtp when user already exists*/
    @Test
    public void generateAndSendOtp_userAlreadyExists() throws JsonProcessingException {
//        RegistrationRequest request = mock(RegistrationRequest.class);
        doReturn(true).when(registrationService).isUserExistsInUserAccountByEmailAndOrg(request);

        String result = registrationService.generateAndSendOtp(request);
        assertEquals("User already exists",result);
    }

    /** Test generateAndSendOtp for new user*/
    @Test
    public void generateAndSendOtp_newUser() throws JsonProcessingException {

        Otp otp = new Otp();
        otp.setOtp("123456");

        doReturn(false).when(registrationService).isUserExistsInUserAccountByEmailAndOrg(request);
        when(otpService.putOtp(anyString())).thenReturn(otp);
        when(emailService.sendOtp("user@example.com", "123456", "TSE One Time Password", null, true)).thenReturn("Success");

        String result = registrationService.generateAndSendOtp(request);
        assertEquals("Success", result);
    }

    /** Test for user that already exists @throws IOException*/
    @Test
    public void doOtpVerificationAndUserRegistration_validOtpUserExists() throws IOException {
//        RegistrationRequest request = new RegistrationRequest();
//        request.setPrimaryEmail("user@example.com");
//        request.setDeviceUniqueIdentifier("device1234");
//        request.setOtp("123456");

        when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn("Success");

        doReturn(true).when(registrationService).isUserExistsInUserAccountByEmailAndOrg(request);

        AuthResponse authResponse = new AuthResponse(null, "User Already Exists", null, null, null, null);
        ResponseEntity<AuthResponse> test = new ResponseEntity(authResponse, HttpStatus.OK);

        ResponseEntity<AuthResponse> result = registrationService.doOtpVerificationAndUserRegistration(request, "Asia/Kolkata");
        assertEquals(result, test);
    }

    /** Test for valid Otp and New User */
    @Test
    public void doOtpVerificationAndUserRegistration_validOtpNewUser() throws IOException {

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt123");

        ResponseEntity<AuthResponse> inserted = new ResponseEntity<>(authResponse, HttpStatus.OK);
        Audit audit = mock(Audit.class);

        when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn("Success");
        doReturn(false).when(registrationService).isUserExistsInUserAccountByEmailAndOrg(request);
        doReturn(inserted).when(registrationService).insertDataIntoDB("Success", request, "Asia/Kolkata");
        when(auditService.auditForSignUpAndLogin(request,null)).thenReturn(audit);

        registrationService.doOtpVerificationAndUserRegistration(request, "Asia/Kolkata");
        verify(auditRepository, times(1)).save(audit);
    }

    /** Test for invalid Otp */
    @Test
    public void doOtpVerificationAndUserRegistration_invalidOtp() throws IOException {

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt123");

        Audit audit = mock(Audit.class);
        ResponseEntity<AuthResponse> inserted = new ResponseEntity<>(authResponse, HttpStatus.OK);

        when(otpService.verifyOtp(anyString(), anyString(), anyString())).thenReturn(null);
        doReturn(inserted).when(registrationService).insertDataIntoDB(null, request, "Asia/Kolkata");
        when(auditService.auditForSignUpAndLogin(request,null)).thenReturn(audit);

        ResponseEntity<AuthResponse> result = registrationService.doOtpVerificationAndUserRegistration(request, "Asia/Kolkata");

        verify(auditRepository, times(1)).save(audit);
        assertEquals(inserted, result);


    }

    /** Formatted GenerateOtp Response Test when otp generation is successful*/
    @Test
    public void getFormattedGenerateOtpResponse_success() {

        ResponseEntity<String> otp = new ResponseEntity<>("Success", HttpStatus.OK);
        ResponseEntity<Object> formattedResponse = registrationService.getFormattedGenerateOtpResponse(otp, request);
        RestResponseWithData restResponseWithData = (RestResponseWithData) formattedResponse.getBody();
        String result = (String) restResponseWithData.getData();
        assertEquals("Success", result);
    }

    /** Formatted GenerateOtp Response Test when otp generation is unsuccessful -- user already exists*/
    @Test(expected = UserAlreadyExistException.class)
    public void getFormattedGenerateOtpResponse_userAlreadyExists() {
        ResponseEntity<String> otp = new ResponseEntity<>("user already exist", HttpStatus.OK);
        ResponseEntity<Object> formattedResponse = registrationService.getFormattedGenerateOtpResponse(otp, request);
    }

/** Formatted GenerateOtp Response Test when otp generation is unsuccessful -- other error */
    @Test(expected = ServerBusyException.class)
    public void getFormattedGenerateOtpResponse_serverError() {
        ResponseEntity<String> otp = new ResponseEntity<>("unsuccessful", HttpStatus.OK);
        ResponseEntity<Object> formattedResponse = registrationService.getFormattedGenerateOtpResponse(otp, request);
    }

    /** Test when token is present */
    @Test
    public void getFormattedSignUpResponse_tokenNotNull() {
        authResponse.setToken("jwt1234");
        ResponseEntity<AuthResponse> response = new ResponseEntity<>(authResponse, HttpStatus.OK);

        HashMap<String, Object> map = new HashMap<>();
        map.put("token", "jwt1234");
        map.put("accessDomains", null);
        map.put("roleActions", null);
        map.put("user", null);

        ResponseEntity<Object> formattedResponse = registrationService.getFormattedSignUpResponse(response, request);
        RestResponseWithData restResponseWithData = (RestResponseWithData) formattedResponse.getBody();
        HashMap<String, Object> result = (HashMap<String, Object>) restResponseWithData.getData();
        assertEquals(map, result);
        assertEquals(Integer.valueOf(200), restResponseWithData.getStatus());
    }

    /** Test when token is null -- user already exists*/
    @Test(expected = UserAlreadyExistException.class)
    public void getFormattedSignUpResponse_tokenNull(){

        authResponse.setToken(null);
        authResponse.setError("User Already Exists");
        ResponseEntity<AuthResponse> response = new ResponseEntity<>(authResponse, HttpStatus.OK);
        registrationService.getFormattedSignUpResponse(response, request);
    }

    /** Test when token is null -- invalid otp*/
    @Test(expected = InvalidOtpException.class)
    public void getFormattedSignUpResponse_tokenNullInvalidOtp(){

        authResponse.setToken(null);
        authResponse.setError("Invalid Otp");
        ResponseEntity<AuthResponse> response = new ResponseEntity<>(authResponse, HttpStatus.OK);
        registrationService.getFormattedSignUpResponse(response, request);
    }



}

