package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.Otp;
import com.tse.core_application.exception.ServerBusyException;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.IEMailService;
import com.tse.core_application.service.IOtpService;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Pattern;

@Service
public class OtpService implements IOtpService {

	private static final Logger logger = LogManager.getLogger(OtpService.class.getName());

	@Autowired
    private RedisTemplate<String, String> redisTemplate;

	@Autowired
    IEMailService emailService;

	@Value("${redis.expire.time}")
	Integer redisExpireTime;

	@Autowired
	UserService userService;

	@Autowired
	private RegistrationService registrationService;

	@Value("${email.subject}")
	private String emailSubject;

	@Value("${demo.otp}")
	private String demoOtp;

	@Value("${demo.username}")
	private String demoUsername;

	@Override
	public String getOtp(String deviceId) {
		ValueOperations<String, String> operations = redisTemplate.opsForValue();
        String city = operations.get(deviceId);
        return city;
	}

	@Override
	public Otp putOtp(String deviceId) {
		ValueOperations<String, String> operations = redisTemplate.opsForValue();
		String generatedOtp = generateOtp();
		operations.set(deviceId, generatedOtp);
	    Boolean isExpireSet = redisTemplate.expire(deviceId, Duration.ofMinutes(redisExpireTime));
		Otp otp = new Otp();
		if(isExpireSet){
					otp.setDeviceUniqueIdentifier(deviceId);
					otp.setOtp(generatedOtp);
					otp.setIsOtpGenerated(isExpireSet);
				}
		return otp;
	}

	@Override
	public String sendOtp(Otp request) {
		String otpKey = CommonUtils.getRedisKeyForOtp(request.getUsername(),request.getDeviceUniqueIdentifier());
		User user = userService.getUser(request.getUsername());
        if(user!=null){
			Otp otp = putOtp(otpKey);
			String sendOtpResp = emailService.sendOtp(request.getUsername(), otp.getOtp(), emailSubject, null, false);
			return sendOtpResp;
        }
        else{
        	return "User doesn't exists";
		}
	}

	private String generateOtp() {
		String otp= new DecimalFormat("000000").format(new Random().nextInt(999999));
		return otp;
	}

	@Override
	public String verifyOtp(String deviceUniqueId,String username, String otpPassword) {
		if (Objects.equals(demoUsername, username) && Objects.equals(demoOtp, otpPassword)) {
			return Constants.SUCCESS;
		}
		ValueOperations<String, String> operations = redisTemplate.opsForValue();
		String otpKey = CommonUtils.getRedisKeyForOtp(username, deviceUniqueId);
		String otp = operations.get(otpKey);
		if (otp != null) {
		if (otp.equals(otpPassword)) {
				return Constants.SUCCESS;
			} else {
				return "Invalid OTP";
			}
		}
		else{
			return "OTP not set for this user and device, or it is expired";
		}
	}

	public ResponseEntity<Object> getFormattedSendOtpResponse(ResponseEntity<String> otp, Otp request) {
		ResponseEntity<Object> formattedResponse = null;
		if (otp.getBody().equalsIgnoreCase(Constants.SUCCESS)) {
			formattedResponse = CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "success", otp.getBody());
		} else {
			String sendOtpMessage = "user doesn't exists";
			if (otp.getBody() != null && otp.getBody().toLowerCase().equals(sendOtpMessage)) {
				String allStackTraces = StackTraceHandler.getAllStackTraces(new UserDoesNotExistException());
				logger.error("User does not exist with username = " + request.getUsername(), new Throwable(allStackTraces));
				throw new UserDoesNotExistException();
			} else {
				String allStackTraces = StackTraceHandler.getAllStackTraces(new ServerBusyException());
				logger.error("Send otp API: Server is Busy for username = " + request.getUsername(), new Throwable(allStackTraces));
				throw new ServerBusyException();
			}
		}
		return formattedResponse;
	}

	public boolean validateAllHeaders(String timeZone, String screenName) {
		boolean isHeaderScreenNameValidated = true;
		boolean isHeaderTimeZoneValidated = false;
		Pattern patternScreenName = Pattern.compile("-?\\d+(\\.\\d+)?");
		try {
			if (screenName == null || screenName.isEmpty() || screenName.contains(" ") ||
					patternScreenName.matcher(screenName).matches() || !Pattern.matches("[a-zA-Z]+", screenName)) {
				isHeaderScreenNameValidated = false;
			}

			String[] validIDs = TimeZone.getAvailableIDs();
			for (String validId : validIDs) {
				if (timeZone != null && !(timeZone.isEmpty()) && !(timeZone.contains(" ")) && validId.equals(timeZone)) {
					isHeaderTimeZoneValidated = true;
					break;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			String allStackTrace = StackTraceHandler.getAllStackTraces(e);
			logger.error("Something went wrong while validating the headers: timeZone and screenName " + ",   " + "Caught Exception: " + e, new Throwable(allStackTrace));
		}
			return (isHeaderScreenNameValidated && isHeaderTimeZoneValidated);
		}


}
