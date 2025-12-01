package com.tse.core_application.service;

import static org.junit.Assert.assertEquals;

import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.exception.ServerBusyException;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.service.Impl.OtpService;
import com.tse.core_application.service.Impl.RegistrationService;
import com.tse.core_application.service.Impl.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import com.tse.core_application.model.User;
import com.tse.core_application.dto.Otp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OtpServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private IEMailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private RegistrationService registrationService;

    @Spy
    @InjectMocks
    private OtpService otpService;

  @Before
    public void setup() {
      ReflectionTestUtils.setField(otpService, "emailSubject", "TSE One Time Password");

    }

    /** Test for Valid User */
    @Test
    public void testSendOtp() {

//        otpService = new OtpService();
        Otp otp = new Otp();
        otp.setDeviceUniqueIdentifier("device123");
        otp.setUsername("user@example.com");

        User user = new User();
        user.setPrimaryEmail("user@example.com");

        when(userService.getUser(anyString())).thenReturn(user);
        otp.setOtp("123456");
        doReturn(otp).when(otpService).putOtp(anyString());

        when(emailService.sendOtp("user@example.com", "123456", "TSE One Time Password", null, false)).thenReturn("Success");
        String result = otpService.sendOtp(otp);
        assertEquals("Success", result);
    }

    /** Test for InValid User */
    @Test
    public void testSendOtp_userNotFound() {
//        when(userService.getUser("test-user")).thenReturn(null);

        Otp otp = new Otp();
        otp.setDeviceUniqueIdentifier("device123");
        otp.setUsername("user@example.com");

        String result = otpService.sendOtp(otp);
        assertEquals("User doesn't exists", result);
    }

    /** Test different combinations for Headers */
    @Test
    public void testValidateAllHeaders() {
        OtpService otpService = new OtpService();

        // Test 1: invalid screenName
        boolean check1 = otpService.validateAllHeaders("Asia/Kolkata", "Test Screen Name");
        assertEquals(false, check1);

        // Test 2: valid input values
        boolean check2 = otpService.validateAllHeaders("Asia/Kolkata", "TestScreenName");
        assertEquals(true, check2);

        // Test 3: invalid time zone
        boolean check3 = otpService.validateAllHeaders("InvalidTimeZone", "TestScreenName");
        assertEquals(false, check3);

        // Test 4: invalid screen name and invalid timezone
        boolean check4 = otpService.validateAllHeaders("InvalidTimeZone", "Test Screen Name");
        assertEquals(false, check4);
    }

    /** Test catch exception if any checks fail in validating all headers */
//    @Test(expected = Exception.class)
//    public void testValidateAllHeaders_invalidHeader() {
//        OtpService otpService = new OtpService();
//
//        when(TimeZone.getAvailableIDs()).thenThrow(new Exception());
//
//        otpService.validateAllHeaders("Asia/Kolkata", "TestScreenName");
//    }

    @Test
    public void testGetFormattedSendOtpResponse_success() {
        Otp request = new Otp();
        request.setUsername("test-user");
        request.setDeviceUniqueIdentifier("device123");

        ResponseEntity<String> otp = new ResponseEntity<>("SUCCESS", HttpStatus.OK);

        ResponseEntity<Object> response = otpService.getFormattedSendOtpResponse(otp, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RestResponseWithData ob = (RestResponseWithData) response.getBody();
        assertEquals("SUCCESS", ob.getData());
    }

    @Test
    public void testGetFormattedSendOtpResponse_userDoesNotExist() {
        Otp request = new Otp();
        request.setUsername("test-user");
        request.setDeviceUniqueIdentifier("device123");
        ResponseEntity<String> otp = new ResponseEntity<>("user doesn't exits", HttpStatus.NOT_FOUND);

        try {
            otpService.getFormattedSendOtpResponse(otp, request);
        } catch (UserDoesNotExistException ex) {
            assertEquals("User Does Not Exist", ex.getMessage());
        }
    }

    @Test
    public void testGetFormattedSendOtpResponse_serverBusy() {
        Otp request = new Otp();
        request.setUsername("test-user");
        ResponseEntity<String> otp = new ResponseEntity<>("server busy", HttpStatus.INTERNAL_SERVER_ERROR);

        try {
            otpService.getFormattedSendOtpResponse(otp, request);
        } catch (ServerBusyException ex) {
            assertEquals("Server Busy", ex.getMessage());
        }
    }
}







