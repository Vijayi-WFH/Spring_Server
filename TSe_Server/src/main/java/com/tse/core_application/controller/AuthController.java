package com.tse.core_application.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.SignUpCompletionDetail;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.AuthRequest;
import com.tse.core_application.dto.AuthResponse;
import com.tse.core_application.dto.Otp;
import com.tse.core_application.dto.RegistrationRequest;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.*;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import static org.springframework.web.servlet.support.RequestContextUtils.getTimeZone;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/auth")
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class.getName());
    @Autowired
    IOtpService tseServices;

    @Autowired
    IEMailService eMailService;

    @Autowired
    ILoginService tokenService;

    @Autowired
    UserService userService;

    @Autowired
    IRegistrationService registrationService;

    @Autowired
    private OpenFireService openFireService;

    @Autowired
    IOtpService otpService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private BlockedRegistrationRepository blockedRegistrationRepository;

    @Value("${allow.personal.registration}")
    private Boolean allowPersonalRegistration;

    @Value("${allow.organization.registration}")
    private Boolean allowOrganizationRegistration;

    @Value("${spring.security.oauth2.client.registration.google.clientId}")
    private String CLIENT_ID;

    @Value("${enable.openfire}")
    private Boolean enableOpenfire;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private ExceptionalRegistrationRepository exceptionalRegistrationRepository;

    @Autowired
    private OrganizationService organizationService;

    @PostMapping(path = "/login")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @Transactional
    public ResponseEntity<Object> login(@RequestBody @Valid AuthRequest req, @RequestHeader(name = "screenName", required = false) String screenName,
                                        @RequestHeader(name = "timeZone", required = false) String timeZone) {
        long startTime = System.currentTimeMillis();
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);

        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("Login API: Headers are not validated for username = " + req.getUsername() + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }

        //     ------TASK-2365 -->  SET EMAIL in REQUEST TO LOWERCASE -------------------------
        req.setUsername(req.getUsername().toLowerCase());

        ResponseEntity<AuthResponse> response;
        try {
            boolean isUserExists = userRepository.existsByPrimaryEmail(req.getUsername());
            userAccountService.validateIfUserDeactivated(req.getUsername());
            if (isUserExists) {
                List<UserAccount> userAccountsDb = userAccountRepository.findByEmailAndIsActive(req.getUsername(), true);
                if (userAccountsDb.isEmpty()) {
                    throw new ValidationFailedException("Account is deactivated. Please contact the System Administrator.");
                }
                ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                ThreadContext.put("accountId", String.valueOf(0));
            }
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered" + '"' + " login" + '"' + " method ...");
            //verification via Google token (for android)
            if (req.getAuthToken() != null && !req.getAuthToken().isEmpty()) {
                    HttpTransport transport = new NetHttpTransport();
                    JsonFactory jsonFactory = new JacksonFactory();
                    GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                            .setAudience(Collections.singletonList(CLIENT_ID))
                            .build();
                    GoogleIdToken idToken = null;
                    try{
                        idToken = verifier.verify(req.getAuthToken());
                    }catch (Exception e){
                        logger.error("Unable to get idToken from GoogleIdTokenVerifier. Caught Exception: "+e);
                        throw new InvalidAuthentication();
                    }
                    if ((idToken != null) && req.getUsername().equalsIgnoreCase(idToken.getPayload().get("email").toString())) {
                        if (isUserExists){
                            String username = req.getUsername();

                            boolean isUserValidated = (Boolean) idToken.getPayload().get("email_verified");
                            if (isUserValidated) {
                                String sub = idToken.getPayload().get("sub").toString();
                                response = tokenService.getTokenForGoogleSSO(username,sub, timeZone);
                                if (response.hasBody() && response.getBody().getToken() != null) {
                                    List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(username);
                                    ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                                    ThreadContext.put("accountId", String.valueOf(0));
                                }
                                long estimatedTime = System.currentTimeMillis() - startTime;
                                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                                logger.info("Exited" + '"' + " login" + '"' + " method (for authToken)because successfully completed ...");
                                ThreadContext.clearMap();
                            }
                            else {
                                response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                            }
                        }
                        else {
                            throw new UserNotRegisteredException();
                        }
                    } else {
                        throw new InvalidAuthentication();
                    }
            }
            // verification via otp
            else {
                response = tokenService.getToken(req, timeZone);

                if (response.hasBody() && response.getBody().getToken() != null) {
                    List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(req.getUsername());
                    ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                    ThreadContext.put("accountId", String.valueOf(0));
                }

                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " login" + '"' + " method (for otp)because successfully completed ...");
                ThreadContext.clearMap();
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof InvalidAuthentication){
                logger.error("Invalid Authentication for username= "+req.getUsername()+" . Caught Exception: "+e, new Throwable(allStackTraces));
                throw new InvalidAuthentication();
            } else if (e instanceof UserNotRegisteredException) {
                logger.error("User "+req.getUsername()+" is not registered. "+"Caught Exception: "+e, new Throwable(allStackTraces));
                throw new UserNotRegisteredException();
            } else {
                e.printStackTrace();
                logger.error("Login API: Something went wrong for username = " + req.getUsername() + ",   " + "Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        try {
            return loginService.getFormattedLoginResponse(response, req);
        } catch(Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof InvalidOtpException){
                logger.error("Invalid otp for username= "+req.getUsername()+" . Caught Exception: "+e, new Throwable(allStackTraces));
                throw new InvalidOtpException();
            }else {
                e.printStackTrace();
                logger.error("Not able to execute login() for the username = " + req.getUsername() + " ,    " +
                        "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }


    @PostMapping(path = "/generateotp")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<Object> generateOtp(@RequestBody @Valid RegistrationRequest req, @RequestHeader(name = "screenName", required = false) String screenName,
                                              @RequestHeader(name = "timeZone", required = false) String timeZone) throws IOException {
        long startTime = System.currentTimeMillis();
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);
        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("Generate otp API: Headers are not validated for primary email = " + req.getPrimaryEmail() + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }

        //     ------TASK-2365 -->  SET EMAIL in REQUEST TO LOWERCASE -------------------------
        req.setPrimaryEmail(req.getPrimaryEmail().toLowerCase());

        registrationService.validateUserEmail(req.getPrimaryEmail(), req);
        if (req.getAlternateEmail() != null) registrationService.validateUserEmail(req.getAlternateEmail(), req);

        if (req.getOrganizationName() != null && blockedRegistrationRepository.existsByEmailAndOrganizationNameAndIsDeleted(req.getPrimaryEmail(), req.getOrganizationName(), false)) {
            throw new IllegalStateException("Username have been blocked by the system admin. Please contact system administrator at support@vijayi-wfh.com.");
        }

        if (req.getIsPrimaryEmailPersonal() && !allowPersonalRegistration && !exceptionalRegistrationRepository.existsByEmailAndIsDeleted(req.getPrimaryEmail(), false)) {
            throw new IllegalStateException("Registration for personal users have been blocked. Please contact system administrator at support@vijayi-wfh.com.");
        }

        if (req.getOrganizationName() != null && !req.getIsPrimaryEmailPersonal()
                && !allowOrganizationRegistration && !organizationService.doesOrganizationExist(req.getOrganizationName())
                && !exceptionalRegistrationRepository.existsByEmailAndIsDeleted(req.getPrimaryEmail(), false)) {
            throw new IllegalStateException("Registration of new organization has been blocked. Please contact system administrator at support@vijayi-wfh.com.");
        }
        ResponseEntity<String> otp;
        try {
            boolean isUserExists = userRepository.existsByPrimaryEmail(req.getPrimaryEmail());

            if (isUserExists) {
                List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(req.getPrimaryEmail());
                ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                ThreadContext.put("accountId", String.valueOf(0));
            }

            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered" + '"' + " generateOtp" + '"' + " method ...");
            otp = new ResponseEntity<String>(registrationService.generateAndSendOtp(req), HttpStatus.OK);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " generateOtp" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Generate otp API: Something went wrong for username = " + req.getPrimaryEmail() + ",   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return registrationService.getFormattedGenerateOtpResponse(otp, req);
        } catch(Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof UserAlreadyExistException) {
                logger.error("User already exists with the username = " + req.getPrimaryEmail() + " ,     " +
                        "Caught Exception: " + e, new Throwable(allStackTraces));
                throw new UserAlreadyExistException();
            } else {
                if(e instanceof ServerBusyException) {
                    logger.error("Server occupied. Please try " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    throw new ServerBusyException();
                } else {
                    e.printStackTrace();
                    logger.error("Not able to execute generateOtp() for the username = " + req.getPrimaryEmail() + " ,     " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }


    @Transactional
    @PostMapping(path = "/signup")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<Object> signUp(@RequestBody @Valid RegistrationRequest req, @RequestHeader(name = "screenName", required = false) String screenName,
                                         @RequestHeader(name = "timeZone", required = false) String timeZone) throws IOException {
        long startTime = System.currentTimeMillis();
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);

        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("Sign up API: Headers are not validated for primary email = " + req.getPrimaryEmail() + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }

        //     ------TASK-2365 -->  SET EMAIL in REQUEST TO LOWERCASE -------------------------
        req.setPrimaryEmail(req.getPrimaryEmail().toLowerCase());
        String orgToFind = req.getOrganizationName().trim().replaceAll("\\s+", " ");
        if (orgToFind.length() < 2 || orgToFind.length() > 100) {
            throw new ValidationFailedException("Organization name must be between 2 to 100 characters long");
        }
        req.setOrganizationName(orgToFind);
        User validChatUser = new User();
        ResponseEntity<AuthResponse> response = new ResponseEntity<>(new AuthResponse(), HttpStatus.OK);
        try {
            boolean isUserExists = userRepository.existsByPrimaryEmail(req.getPrimaryEmail());

            if (isUserExists) {
                List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(req.getPrimaryEmail());
                ThreadContext.put("userId", String.valueOf(userAccountsDb.get(0).getFkUserId().getUserId().toString()));
                ThreadContext.put("accountId", String.valueOf(0));
            }

            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered" + '"' + " signUp" + '"' + " method ...");
            response = registrationService.doOtpVerificationAndUserRegistration(req, timeZone);

            if (response.getBody().getToken() != null) {
                List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(req.getPrimaryEmail());
                validChatUser = userAccountsDb.get(0).getFkUserId();
                if (enableOpenfire) {
                    try {
                        openFireService.createChatUser(validChatUser);
                        openFireService.addRosterEntry(req.getOrganizationName(), req.getPrimaryEmail());
                    } catch (Exception e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("Sign up API: Something went wrong for primary email = " + req.getPrimaryEmail() + ",    " + "Caught OpenfireException: " + e, new Throwable(allStackTraces));
//                  throw new OpenfireException(" Openfire Error");
                    }
                }

                ThreadContext.put("userId", String.valueOf(userAccountsDb.get(0).getFkUserId().getUserId().toString()));
                ThreadContext.put("accountId", String.valueOf(0));
            }


            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", estimatedTime + "");
            logger.info("Exited" + '"' + " signUp" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        }  catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Sign up API: Something went wrong for primary email = " + req.getPrimaryEmail() + ",    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

        try {
            return registrationService.getFormattedSignUpResponse(response, req);
        } catch (Exception e) {
            if(e instanceof InvalidOtpException) {
                throw e;
            } else {
                if(e instanceof UserAlreadyExistException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute signUp() for the username = " + req.getPrimaryEmail() + " ,     " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }


    @PostMapping(path = "/sendotp")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<Object> sendOtp(@RequestBody @Valid Otp req, @RequestHeader(name = "screenName", required = false) String screenName,
                                          @RequestHeader(name = "timeZone", required = false) String timeZone) {
        long startTime = System.currentTimeMillis();
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);

        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("Headers are not validated for username = " + req.getUsername() + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }

        //     ------TASK-2365 -->  SET EMAIL in REQUEST TO LOWERCASE -------------------------
        req.setUsername(req.getUsername().toLowerCase());

        ResponseEntity<String> otp;
        try {
            boolean isUserExists = userRepository.existsByPrimaryEmail(req.getUsername());
            userAccountService.validateIfUserDeactivated(req.getUsername());
            if (isUserExists) {
                List<UserAccount> userAccountsDb = userAccountRepository.findByEmailAndIsActive(req.getUsername(), true);
                if (userAccountsDb.isEmpty()) {
                    throw new ValidationFailedException("Account is deactivated. Please contact the System Administrator.");
                }
                ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                ThreadContext.put("accountId", String.valueOf(0));
            }
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered" + '"' + " sendOtp" + '"' + " method ...");
            otp = new ResponseEntity<String>(otpService.sendOtp(req), HttpStatus.OK);

            if (otp.getBody().equalsIgnoreCase(Constants.SUCCESS)) {
                List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(req.getUsername());
                ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                ThreadContext.put("accountId", String.valueOf(0));
            }

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " sendOtp" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong with sendOtp API for username = " + req.getUsername() + ",    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return otpService.getFormattedSendOtpResponse(otp, req);
        } catch(Exception e) {
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                if(e instanceof ServerBusyException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute sendOtp() for the username = " + req.getUsername() + " ,    " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }

    // API to call google authentication in web browser through webapp
    @GetMapping(path = "/googlesso")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @Transactional
    public ResponseEntity<Object> googleAuth(HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        DefaultOAuth2User user= (DefaultOAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username=(String)user.getAttributes().get("email");
        boolean isUserExists = userRepository.existsByPrimaryEmail(username);
        if (isUserExists) {
            List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(username);
            ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
            ThreadContext.put("accountId", String.valueOf(0));
        }
        logger.info("Entered" + '"' + " Google SSO login" + '"' + " method ...");
        boolean isUserValidated = (Boolean) user.getAttributes().get("email_verified");
        TimeZone zone = getTimeZone(request);
        String timezone =(zone!=null)?zone.getID():TimeZone.getDefault().getID();
        try {
            ResponseEntity<AuthResponse> response;
            AuthRequest authRequest = new AuthRequest();
            authRequest.setUsername(username);
            if (isUserExists){
                if (isUserValidated) {
                    String sub = user.getAttributes().get("sub").toString();
                    response = tokenService.getTokenForGoogleSSO(username, sub, timezone);
                    if (response.hasBody() && response.getBody().getToken() != null) {
                        List<UserAccount> userAccountsDb = userAccountRepository.findByEmail(username);
                        ThreadContext.put("userId", userAccountsDb.get(0).getFkUserId().getUserId().toString());
                        ThreadContext.put("accountId", String.valueOf(0));
                    }
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + " Google SSO login" + '"' + " method because successfully completed ...");
                    ThreadContext.clearMap();
                } else {
                    response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            }
            else{
                throw new UserNotRegisteredException();
            }
            return loginService.getFormattedLoginResponse(response, authRequest);
        } catch(Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof UnauthorizedLoginException) {
                logger.error("Unauthorized login attempt by username= "+username+" . Caught Exception: "+e, new Throwable(allStackTraces));
                throw e;
            } else if (e instanceof UserNotRegisteredException) {
                logger.error("User "+username+" is not registered. "+"Caught Exception: "+e, new Throwable(allStackTraces));
                throw e;
            } else {
                e.printStackTrace();
                logger.error("Not able to execute Google SSO login() for the username = " + username + " ,    " +
                        "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @GetMapping(path = "/validateToken")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<Object> validateToken(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "token") String token) {
        long startTime = System.currentTimeMillis();
        boolean isTokenValid = true;
        try {
            ThreadContext.put("requestOriginatingPage", screenName);
            ThreadContext.put("accountId", String.valueOf(0));
            logger.info("Entered" + '"' + " validateToken" + '"' + " method ...");
            if (!jwtUtil.validateToken(token)) {
                isTokenValid = false;
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " validateToken" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
            if (!isTokenValid) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, com.tse.core_application.constants.Constants.FormattedResponse.UNAUTHORIZED, "Invalid or expired token");
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Token is valid");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error executing method: validateToken for token: " + token + " Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/completeRegistration")
    @Transactional
    public ResponseEntity<Object> completeRegistration(@RequestBody @Valid SignUpCompletionDetail signUpCompletionDetail,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " completeRegistration" + '"' + " method ...");
        String response;
        try {
            response = registrationService.completeRegistration(signUpCompletionDetail, foundUserDbByUsername);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add message in the entity" +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " addMessage" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/validateTokenAccount")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<Object> validateTokenAccount(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "token") String token, @RequestHeader(name = "accountIds") List<Long> accountIds) {
        long startTime = System.currentTimeMillis();
        boolean isTokenValid = true;
        boolean areAccountIdsValid = true;
        try {
            ThreadContext.put("requestOriginatingPage", screenName);
            ThreadContext.put("accountId", accountIds.get(0).toString());
            logger.info("Entered" + '"' + " validateTokenAccount" + '"' + " method ...");
            if (!jwtUtil.validateToken(token)) {
                isTokenValid = false;
            }
            else {
                List<Long> accountIdsFromToken = jwtUtil.getAllAccountIdsFromToken(token);
                areAccountIdsValid = accountIdsFromToken.containsAll(accountIds);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " validateTokenAccount" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
            if (!(isTokenValid && areAccountIdsValid)) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, com.tse.core_application.constants.Constants.FormattedResponse.UNAUTHORIZED, "Invalid or expired token");
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Token is valid");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error executing method: validateToken for token: " + token + " Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }


}
