package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.custom.model.UserIdFirstLastName;
import com.tse.core_application.custom.model.CustomRoleAction;
import com.tse.core_application.exception.InvalidOtpException;
import com.tse.core_application.exception.UnauthorizedLoginException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Audit;
import com.tse.core_application.model.AuthRequest;
import com.tse.core_application.dto.AuthResponse;
import com.tse.core_application.dto.User;
import com.tse.core_application.repository.RoleActionRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.repository.AuditRepository;
import com.tse.core_application.service.ILoginService;
import com.tse.core_application.service.IOtpService;
import com.tse.core_application.utils.JWTUtil;
import com.tse.core_application.utils.PBKDF2Encoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LoginService implements ILoginService {

	private static final Logger logger = LogManager.getLogger(LoginService.class.getName());

	@Autowired
	UserService userService;
	
	@Autowired
	PBKDF2Encoder passwordEncoder;
	
	@Autowired
	IOtpService otpService;
	
	@Autowired
	JWTUtil jwtUtil;

	@Autowired
	private AuditService auditService;

	@Autowired
	private AuditRepository auditRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessDomainService accessDomainService;

	@Autowired
	private RoleActionService roleActionService;

	@Autowired
	private RoleActionRepository roleActionRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	ObjectMapper objectMapper = new ObjectMapper();


	@Override
	public ResponseEntity<AuthResponse> getToken(AuthRequest authRequest, String timeZone) {
		String verifyOtpResp = otpService.verifyOtp(authRequest.getDeviceUniqueIdentifier(), authRequest.getUsername(), authRequest.getOtp());
		if(verifyOtpResp.equalsIgnoreCase(Constants.SUCCESS)){
			User userDetails = userService.findByUsername(authRequest.getUsername(), passwordEncoder.encode(authRequest.getOtp()), timeZone);
			List<Long> accountIdsForUser = new ArrayList<>(userService.getAccountIdsForUser(userDetails));
			if (userDetails != null) {
//				UserIdFirstLastName userIdFirstLastName = userService.getUserIdFirstLastNameByPrimaryEmail(authRequest.getUsername());
				com.tse.core_application.model.User user = userService.getUserByUserName(authRequest.getUsername());
				UserIdFirstLastName userIdFirstLastName = new UserIdFirstLastName(user.getUserId(), user.getFirstName(), user.getLastName());
				boolean isSignUpCompleted = userService.isSignUpComplete(user);
				if (user.getIsUserManaging() != null && user.getIsUserManaging() && user.getUserId() != null) {
					List<Long> userIdsList = userRepository.findAllUserIdByManagingUserId(user.getUserId());
					accountIdsForUser.addAll(userAccountRepository.findAllAccountIdsByUserIdInAndIsActive(userIdsList, true));
				}
				if (accountIdsForUser == null || accountIdsForUser.isEmpty()) {
					throw new ValidationFailedException("You are not part of any organisation or not verified in organisation");
				}
				List<CustomAccessDomain> accessDomainList = accessDomainService.getAllActiveAccessDomainsByAllAccountIds(accountIdsForUser);
//				List<CustomRoleAction> roleActionList = roleActionService.getAllRoleActionsByAccessDomains(accessDomainList);
				List<CustomRoleAction> roleActionList = roleActionRepository.findAllRoleActionCustom();
				ResponseEntity<AuthResponse> response;
				if (isSignUpCompleted) {
					response = ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails, accountIdsForUser), null, accessDomainList, roleActionList, userIdFirstLastName, true));
				} else {
					response = ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails, accountIdsForUser), null, accessDomainList, roleActionList, userIdFirstLastName, false));
				}
				if (response.getBody().getToken() != null) {
					Audit auditAdd = auditRepository.save(auditService.auditForSignUpAndLogin(null, authRequest));
					return response;
				}
			}
		}
		else if(verifyOtpResp.equalsIgnoreCase("Invalid OTP")){
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	public com.tse.core_application.model.User validateAndUpdateLoginUserTimeZone(com.tse.core_application.model.User user, String timeZone) {
		if(Objects.equals(TimeZone.getTimeZone(timeZone).getDisplayName(), TimeZone.getTimeZone(user.getTimeZone()).getDisplayName())){
//		if (timeZone.equals(user.getTimeZone())) {
			return user;
		} else {
			userRepository.updateTimeZoneByUserId(timeZone, user.getUserId());
			Audit insertedAudit = auditService.auditForDifferentTimeZone(user, timeZone);
			return user;
		}
	}

	public ResponseEntity<Object> getFormattedLoginResponse(ResponseEntity<AuthResponse> response, AuthRequest request) {
		ResponseEntity<Object> formattedResponse = null;
		if (response.getBody() != null && response.getBody().getToken() != null) {
			HashMap<String, Object> map = objectMapper.convertValue(response.getBody(), HashMap.class);
			map.remove("error");
			formattedResponse = CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, map);
		} else if (response.getStatusCode().equals(HttpStatus.NOT_ACCEPTABLE)) {
			String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidOtpException());
			logger.error("Login API: Invalid otp for username and otp: " + "username = " + request.getUsername() + " ,   " + "otp = " + request.getOtp(), new Throwable(allStackTraces));
			ThreadContext.clearMap();
			throw new InvalidOtpException();
		} else {
			String allStackTraces = StackTraceHandler.getAllStackTraces(new UnauthorizedLoginException());
			logger.error("Login API: Unauthorized login with username and otp: " + "username = " + request.getUsername() + " ,   " + "otp = " + request.getOtp(), new Throwable(allStackTraces));
			ThreadContext.clearMap();
			throw new UnauthorizedLoginException();
		}
		return formattedResponse;
	}

	/**
	 *
	 * @param username
	 * @param sub
	 * @param timezone
	 * @return ResponseEntity (after audit) that contains the token for the user who logged through Google Mail.
	 */
	@Override
	public ResponseEntity<AuthResponse> getTokenForGoogleSSO(String username,String sub, String timezone) {
			User userDetails = userService.findByUsername(username, passwordEncoder.encode(""), timezone);
			List<Long> accountIdsForUser = userService.getAccountIdsForUser(userDetails);
			if (userDetails != null) {
//				UserIdFirstLastName userIdFirstLastName = userService.getUserIdFirstLastNameByPrimaryEmail(username);
				com.tse.core_application.model.User user = userService.getUserByUserName(username);
				UserIdFirstLastName userIdFirstLastName = new UserIdFirstLastName(user.getUserId(), user.getFirstName(), user.getLastName());
				boolean isSignUpCompleted = userService.isSignUpComplete(user);

				List<CustomAccessDomain> accessDomainList = accessDomainService.getAllActiveAccessDomainsByAllAccountIds(accountIdsForUser);
				List<CustomRoleAction> roleActionList = roleActionService.getAllRoleActionsByAccessDomains(accessDomainList);
				ResponseEntity<AuthResponse> response;
				if (isSignUpCompleted) {
					response = ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails, accountIdsForUser), null, accessDomainList, roleActionList, userIdFirstLastName, true));
				} else {
					response = ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails, accountIdsForUser), null, accessDomainList, roleActionList, userIdFirstLastName, false));
				}
				if (response.getBody().getToken()!=null) {
					AuthRequest authRequest=new AuthRequest();
					authRequest.setUsername(username);
					Audit auditAdd = auditRepository.save(auditService.auditForLoginWithGoogle(username,sub));
					return response;
				}
			}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

}
