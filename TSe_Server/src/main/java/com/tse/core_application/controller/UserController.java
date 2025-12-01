package com.tse.core_application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.Organizations;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.user_access_response.UserAccessResponse;
import com.tse.core_application.exception.*;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.HttpCustomStatus;
import com.tse.core_application.model.User;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.UserAccountService;
import com.tse.core_application.service.Impl.OtpService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tse.core_application.handlers.RequestHeaderHandler;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.tse.core_application.constants.Constants.BEARER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.*;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/user")
public class UserController {

    private static final Logger logger = LogManager.getLogger(TeamController.class.getName());

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    UserAccountService userAccountService;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
    }

    @GetMapping(path = "/getUserDetails/{userName}")
    public ResponseEntity<Object> getUserDetails(@PathVariable(name = "userName") String userName, @RequestHeader(name = "screenName", required = false) String screenName,
                                                 @RequestHeader(name = "timeZone", required = false) String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserDetails" + '"' + " method ...");
        UserDetailsResponse user;
        try {
            user = userService.getUserDetailsByUserName(userName);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserDetails" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserDetails() by username = " +
                    userName + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return userService.getUserDetailsFormattedResponse(user, userName);
        } catch(Exception e) {
            e.printStackTrace();
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserDetails() by username = " +
                        userName + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @GetMapping(path = "/getOrgTeamDropdownStructure/{userName}")
    public ResponseEntity<Object> getOrgTeamDropdownStructure(@PathVariable(name = "userName") String userName, @RequestHeader(name = "accountIds") String accountIds,
                                                              @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                              HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOrgTeamDropdownStructure" + '"' + " method ...");

        boolean isUserNameValidated = userService.validateGetOrgTeamDropdownStructureInputs(userName, jwtToken);
        if(isUserNameValidated) {
            try {
                Organizations orgTeamStructures = userService.getAllOrgStructures(userName);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getOrgTeamDropdownStructure" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, orgTeamStructures);
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgTeamDropdownStructure() by username = " +
                        userName + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidUserNameException());
            logger.error(request.getRequestURI() + " API: " + " ,    " + "userName = " + userName + " not validated.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new InvalidUserNameException();
        }
    }

    @PostMapping(value = "/logout")
    public ResponseEntity<Object> logout(@RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " logout" + '"' + " method ...");

        String tokenValue = null;
        try {
            String authorization = request.getHeader(AUTHORIZATION);
            if (authorization != null && authorization.contains(BEARER)) {
                tokenValue = authorization.replace(BEARER, "").trim();
                String username = jwtUtil.getUsernameFromToken(tokenValue);
                Set<String> blockedTokens = null;
                if(JwtRequestFilter.blockedTokens.containsKey(username)){
                    blockedTokens = JwtRequestFilter.blockedTokens.get(username);

                    if(blockedTokens.contains(tokenValue)){
                        long estimatedTime = System.currentTimeMillis() - startTime;
                        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                        logger.info("Exited" + '"' + " logout" + '"' + " method because completed successfully ...");
                        ThreadContext.clearMap();
                        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "Token already expired !", tokenValue);
                    }

                    blockedTokens.add(tokenValue);
                }
                else{
                    blockedTokens = new HashSet<>();
                    blockedTokens.add(tokenValue);
                }
                JwtRequestFilter.blockedTokens.put(username, blockedTokens);
                redisTemplate.opsForHash().put(Constants.TOKEN_HASH, username, objectMapper.writeValueAsString(blockedTokens));
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " logout" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
            }
        } catch (Exception e) {
            logger.error("Exception Getting in : " + request.getRequestURI(), new Throwable(Arrays.toString(e.getStackTrace())));
            e.printStackTrace();
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "User logout successfully", tokenValue);
    }

        @GetMapping(path = "/getUserProfileDetails/{userId}")
    public ResponseEntity<Object> getUserProfileDetails(@PathVariable(name = "userId") Long userId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                 @RequestHeader(name = "timeZone", required = false) String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered getUserProfileDetails method.");
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        UserProfileDTO userProfileDTO = new UserProfileDTO();
        try {
            if(!Objects.equals(foundUserDbByUsername.getUserId(), userId)) {
                throw new ForbiddenException("Not allowed to get any other user's details");
            }
            userProfileDTO = userService.getUserProfileDetails(userId);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserProfileDetails method for userId = " + userId + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getUserProfileDetails" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        if(userProfileDTO != null) return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userProfileDTO);
        else return ResponseEntity.notFound().build();
    }

    @PutMapping(path = "/editUserProfile")
    public ResponseEntity<Object> editUserProfile(@RequestBody UserProfileDTO userProfileDTO,
                                                  @RequestHeader(name = "screenName", required = false) String screenName,
                                                  @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                  @RequestHeader(name = "accountIds") String accountIds,
                                                  HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered editUserProfile method.");

        boolean isUserProfileSaved = false;
        try {
            if(userProfileDTO.getUserId() == null) {
                throw new IllegalArgumentException("userId can not be null in the editUserProfile request");
            }

            if(!Objects.equals(foundUser.getUserId(), userProfileDTO.getUserId())) {
                throw new ForbiddenException("Not allowed to get any other user's details");
            }
            isUserProfileSaved = userService.editUserProfile(userProfileDTO, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " editUserProfile" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            if(isUserProfileSaved) return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "User Profile Saved Successfully");
            else throw new InternalServerErrorException("User not saved");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute editUserProfile method for userId = " + userProfileDTO.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    /**
     * This api returns user org drop down structure with a boolean representing his higher role access
     */
    @GetMapping(path = "/getUserAllOrgAccess/{userName}")
    public ResponseEntity<Object> getUserAllOrgAccess(@PathVariable(name = "userName") String userName, @RequestHeader(name = "accountIds") String accountIds,
                                                              @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                              HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserAllOrgAccessStructure" + '"' + " method ...");

        List<Long> userAccountIdsFromToken = jwtUtil.getAllAccountIdsFromToken(jwtToken);
        List<Long> userActiveAccountIds = userAccountRepository.findAllAccountIdsByUserIdInAndIsActiveAndIsVerifiedTrue(List.of(foundUser.getUserId()), true);
        if (!new HashSet<>(userAccountIdsFromToken).containsAll(userActiveAccountIds)) {
            return CustomResponseHandler.generateCustomResponseForCustom(HttpCustomStatus.INVALID_TOKEN, Constants.FormattedResponse.INVALID_TOKEN, "Token is invalid or expired");
        }

        boolean isUserNameValidated = userService.validateGetOrgTeamDropdownStructureInputs(userName, jwtToken);
        if(isUserNameValidated) {
            try {
                UserAccessResponse orgTeamStructures = userService.getUserAllOrgScreenAccess(userName, screenName);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getUserAllOrgAccessStructure" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, orgTeamStructures);
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserAllOrgAccessStructure() by username = " +
                        userName + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidUserNameException());
            logger.error(request.getRequestURI() + " API: " + " ,    " + "userName = " + userName + " not validated.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new InvalidUserNameException();
        }
    }

    /**
     *This api returns org team drop down structure for all organizations
     */
    @GetMapping(path = "/getAllOrgTeamDropdownStructure/{userName}")
    public ResponseEntity<Object> getAllOrgTeamDropdownStructure(@PathVariable(name = "userName") String userName, @RequestHeader(name = "accountIds") String accountIds,
                                                              @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                              HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllOrgTeamDropdownStructure" + '"' + " method ...");

        boolean isUserNameValidated = userService.validateGetOrgTeamDropdownStructureInputs(userName, jwtToken);
        if(isUserNameValidated) {
            try {
                Organizations orgTeamStructures = userService.getAllOrgTeamsStructures(userName);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getAllOrgTeamDropdownStructure" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, orgTeamStructures);
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllOrgTeamDropdownStructure() by username = " +
                        userName + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw new InternalServerErrorException(e.getMessage());
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidUserNameException());
            logger.error(request.getRequestURI() + " API: " + " ,    " + "userName = " + userName + " not validated.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new InvalidUserNameException();
        }
    }

    @PostMapping(path = "/sendOtpForManagingUserVerification")
    public ResponseEntity<Object> sendOtpForManagingUserVerification(@RequestBody @Valid Otp req, @RequestHeader(name = "accountIds") String accountIds,
                                          @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                          HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " sendOtpForManagingUserVerification" + '"' + " method ...");
        try {
            return userService.sendManagingUserRequest(req, foundUser, screenName, timeZone);
        } catch(Exception e) {
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                if(e instanceof ServerBusyException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute sendOtpForManagingUserVerification() for the username = " + foundUser.getPrimaryEmail() + " ,    " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }

    @PostMapping(path = "/verifyOtpAndAddManagedUser")
    public ResponseEntity<Object> verifyOtpAndAddManagedUser(@RequestBody @Valid AddUserRequest req, @RequestHeader(name = "accountIds") String accountIds,
                                                                     @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                                     HttpServletRequest request) throws AuthenticationException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " verifyOtpAndAddManagedUser" + '"' + " method ...");
        try {
            userService.verifyOtpAndAddManagedUser(req, foundUser, timeZone);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "All accounts will be logged out");
        } catch(Exception e) {
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                if(e instanceof ServerBusyException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute verifyOtpAndAddManagedUser() for the username = " + foundUser.getPrimaryEmail() + " ,    " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }


    @PostMapping(path = "/sendOtpToVerifyManagedUserRemoval")
    public ResponseEntity<Object> sendOtpToVerifyManagedUserRemoval(@RequestBody @Valid Otp req, @RequestHeader(name = "accountIds") String accountIds,
                                                                     @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                                     HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " sendOtpToVerifyManagedUserRemoval" + '"' + " method ...");
        try {
            return userService.sendRemovingUserRequest(req, foundUser, screenName, timeZone);
        } catch(Exception e) {
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                if(e instanceof ServerBusyException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute sendOtpToVerifyManagedUserRemoval() for the username = " + foundUser.getPrimaryEmail() + " ,    " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }

    @PostMapping(path = "/verifyOtpAndRemoveManagedUser")
    public ResponseEntity<Object> verifyOtpAndRemoveManagedUser(@RequestBody @Valid AddUserRequest req, @RequestHeader(name = "accountIds") String accountIds,
                                                             @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                             HttpServletRequest request) throws AuthenticationException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " verifyOtpAndAddManagedUser" + '"' + " method ...");
        try {
            userService.verifyOtpAndRemoveManagedUser(req, foundUser, timeZone);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "All accounts will be logged out");
        } catch(Exception e) {
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                if(e instanceof ServerBusyException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute verifyOtpAndAddManagedUser() for the username = " + foundUser.getPrimaryEmail() + " ,    " +
                            "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }

    @PostMapping(path = "/editUserName")
    public ResponseEntity<Object> editUserName (@RequestBody @Valid UserNameChangeRequest userNameChangeRequest, @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " editUserName" + '"' + " method ...");

        try {
            Long requesterAccountId = Long.valueOf(accountIds);
            userService.changeUserName (userNameChangeRequest, requesterAccountId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " editUserName" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "User Profile Saved Successfully");
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to edit user name for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

}
