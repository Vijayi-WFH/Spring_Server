package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.OrgDetailsForSuperUser;
import com.tse.core_application.dto.RestrictedDomainRequest;
import com.tse.core_application.dto.report.ApplicationReport;
import com.tse.core_application.dto.report.OrganizationReportResponse;
import com.tse.core_application.dto.report.UserOrganizationsReport;
import com.tse.core_application.dto.super_admin.*;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.GetUserForSuperAdminRequest;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.service.Impl.BlockedRegistrationService;
import com.tse.core_application.service.Impl.ExceptionalRegistrationService;
import com.tse.core_application.service.Impl.SuperAdminService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/admin")
public class SuperAdminController {

    private static final Logger logger = LogManager.getLogger(SuperAdminController.class.getName());

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private BlockedRegistrationService blockedRegistrationService;

    @Autowired
    private ExceptionalRegistrationService exceptionalRegistrationService;

    @PostMapping("/addExceptionalUser")
    public ResponseEntity<Object> addExceptionalUser (@RequestBody @Valid ExceptionalRegistrationRequest exceptionalRegistrationRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                      @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered addExceptionalUser method.");
        try {
            ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationService.addExceptionalUser(exceptionalRegistrationRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addExceptionalUser" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, exceptionalRegistration);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute addExceptionalUser method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/removeExceptionalUser/{exceptionalRegistrationId}")
    public ResponseEntity<Object> removeExceptionalUser (@PathVariable Long exceptionalRegistrationId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                         @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                         @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered removeExceptionalUser method.");
        try {
            ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationService.removeExceptionalUser(exceptionalRegistrationId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeExceptionalUser" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, exceptionalRegistration);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute removeExceptionalUser method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getAllExceptionalRegistration")
    public ResponseEntity<Object> getAllExceptionalRegistration (@RequestHeader(name = "screenName", required = false) String screenName,
                                                                 @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                                 @RequestHeader(name = "accountIds") String accountIds,
                                                                 HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllExceptionalRegistration method.");
        try {
            List<ExceptionalRegistration> exceptionalRegistrationList = exceptionalRegistrationService.getAllExceptionalRegistrationList(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllExceptionalRegistration" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, exceptionalRegistrationList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllExceptionalRegistration method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getAllActiveExceptionalRegistration")
    public ResponseEntity<Object> getAllActiveExceptionalRegistration (@RequestHeader(name = "screenName", required = false) String screenName,
                                                                       @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                                       HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllActiveExceptionalRegistration method.");
        try {
            List<ExceptionalRegistration> exceptionalRegistrationList = exceptionalRegistrationService.getAllActiveExceptionalRegistrationList(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllActiveExceptionalRegistration" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, exceptionalRegistrationList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllActiveExceptionalRegistration method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/updateExceptionalUser/{exceptionalRegistrationId}")
    public ResponseEntity<Object> updateExceptionalUser (@PathVariable Long exceptionalRegistrationId, @RequestBody @Valid ExceptionalRegistrationRequest exceptionalRegistrationRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                         @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                         @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered updateExceptionalUser method.");
        try {
            ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationService.updateExceptionalUser(exceptionalRegistrationId, exceptionalRegistrationRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateExceptionalUser" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, exceptionalRegistration);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute updateExceptionalUser method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/reAddExceptionalUser/{exceptionalRegistrationId}")
    public ResponseEntity<Object> reAddExceptionalUser (@PathVariable Long exceptionalRegistrationId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                        @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                        @RequestHeader(name = "accountIds") String accountIds,
                                                        HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered reAddExceptionalUser method.");
        try {
            ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationService.reAddExceptionalUser(exceptionalRegistrationId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " reAddExceptionalUser" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, exceptionalRegistration);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute reAddExceptionalUser method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/deactivateAccounts")
    @Transactional
    public ResponseEntity<Object> deactivateAccounts (@RequestBody @Valid ReactivateDeactivateUserRequest deactivateUserRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                      @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered deactivateAccounts method.");
        try {
            superAdminService.deactivateAccounts(deactivateUserRequest, accountIds, foundUser,true);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deactivateAccounts" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Accounts provided were successfully deactivated.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute deactivateAccounts method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/reactivateAccounts")
    public ResponseEntity<Object> activateAccounts (@RequestBody @Valid ReactivateDeactivateUserRequest reactivateUserRequest,@RequestHeader(name = "screenName", required = false) String screenName,
                                                      @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered activateAccounts method.");
        try {
            superAdminService.reactivateAccounts(reactivateUserRequest, accountIds, foundUser, true);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllUsersForAdmin" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Accounts provided were successfully reactivated.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllUsersForAdmin method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/getAllUsersForAdmin")
    public ResponseEntity<Object> getAllUsersForAdmin (@RequestBody @Valid GetUserForSuperAdminRequest userRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                       @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllUsersForAdmin method.");
        try {
            List<UserDetailsForSuperAdmin> response = superAdminService.getUsersForSuperAdmin(userRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllUsersForAdmin" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllUsersForAdmin method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/deactivateOrganization/{orgId}")
    public ResponseEntity<Object> deactivateOrganization (@PathVariable Long orgId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                          @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered deactivateOrganization method.");
        try {
            superAdminService.deactivateOrganization(orgId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deactivateOrganization" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Organization deactivated successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute deactivateOrganization method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/reactivateOrganization/{orgId}")
    public ResponseEntity<Object> reactivateOrganization (@PathVariable Long orgId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                          @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered reactivateOrganization method.");
        try {
            superAdminService.reactivateOrganization(orgId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " reactivateOrganization" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Organization reactivated successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute reactivateOrganization method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getAllOrgDetails")
    public ResponseEntity<Object> getAllOrgDetails (@RequestHeader(name = "screenName", required = false) String screenName,
                                                          @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllOrgDetails method.");
        try {
            List<OrgDetailsForSuperUser> orgList = superAdminService.getAllOrgDetails(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllOrgDetails" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, orgList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllOrgDetails method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getDefaultEntitiesCount")
    public ResponseEntity<Object> getDefaultEntitiesCount (@RequestHeader(name = "screenName", required = false) String screenName,
                                                            @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                            @RequestHeader(name = "accountIds") String accountIds,
                                                            HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getDefaultEntitiesCount method.");
        try {
            DefaultEntitiesCountResponse defaultEntitiesCountResponse = superAdminService.getDefaultEntitiesCount(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getDefaultEntitiesCount" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, defaultEntitiesCountResponse);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getDefaultEntitiesCount method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/updateLimitsInOrganization/{orgId}")
    public ResponseEntity<Object> updateLimitsInOrganization (@RequestBody UpdateLimitsInOrgRequest updateLimitsInOrgRequest, @PathVariable Long orgId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                              @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                              @RequestHeader(name = "accountIds") String accountIds,
                                                              HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered updateLimitsInOrganization method.");
        try {
            OrgDetailsForSuperUser response = superAdminService.updateLimitsInOrg(orgId, updateLimitsInOrgRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateLimitsInOrganization" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute updateLimitsInOrganization method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getOrgReportByEmail/{email}")
    public ResponseEntity<Object> getOrgReportByEmail (@PathVariable String email, @RequestHeader(name = "screenName", required = false) String screenName,
                                                              @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                              @RequestHeader(name = "accountIds") String accountIds,
                                                              HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getOrgReportByEmail method.");
        try {
            List<UserOrganizationsReport> response = superAdminService.getUserOrganizationReport(email.toLowerCase().trim(), accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgReportByEmail" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgReportByEmail method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getOrgReportByOrgName/{orgName}")
    public ResponseEntity<Object> getOrgReportByOrgName (@PathVariable String orgName, @RequestHeader(name = "screenName", required = false) String screenName,
                                                       @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getOrgReportByOrgName method.");
        try {
            OrganizationReportResponse response = superAdminService.getOrganizationReportResponse(orgName, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgReportByOrgName" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgReportByOrgName method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getApplicationReport")
    public ResponseEntity<Object> getApplicationReport (@RequestHeader(name = "screenName", required = false) String screenName,
                                                         @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                         @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getApplicationReport method.");
        try {
            ApplicationReport response = superAdminService.getApplicationReport(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getApplicationReport" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getApplicationReport method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/addBlockedUser")
    public ResponseEntity<Object> addBlockedUser (@RequestBody @Valid BlockedRegistrationRequest blockedRegistrationRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                  @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                  @RequestHeader(name = "accountIds") String accountIds,
                                                  HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered addBlockedUser method.");
        try {
            BlockedRegistration blockedRegistration = blockedRegistrationService.addBlockedUser(blockedRegistrationRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addBlockedUser" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, blockedRegistration);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute addBlockedUser method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/removeBlockedUser/{blockedRegistrationId}")
    public ResponseEntity<Object> removeBlockedUser (@PathVariable Long blockedRegistrationId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                     @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                     @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered removeBlockedUser method.");
        try {
            BlockedRegistration blockedRegistration = blockedRegistrationService.removeBlockedUser(blockedRegistrationId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeBlockedUser" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, blockedRegistration);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute removeBlockedUser method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getAllBlockedRegistration")
    public ResponseEntity<Object> getAllBlockedRegistration (@RequestHeader(name = "screenName", required = false) String screenName,
                                                             @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                             @RequestHeader(name = "accountIds") String accountIds,
                                                             HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllBlockedRegistration method.");
        try {
            List<BlockedRegistration> blockedRegistrationList = blockedRegistrationService.getAllBlockedRegistrationList(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllBlockedRegistration" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, blockedRegistrationList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllBlockedRegistration method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getAllActiveBlockedRegistration")
    public ResponseEntity<Object> getAllActiveBlockedRegistration (@RequestHeader(name = "screenName", required = false) String screenName,
                                                                   @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                                   @RequestHeader(name = "accountIds") String accountIds,
                                                                   HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllActiveBlockedRegistration method.");
        try {
            List<BlockedRegistration> blockedRegistrationList = blockedRegistrationService.getAllActiveBlockedRegistrationList(accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllActiveBlockedRegistration" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, blockedRegistrationList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllActiveBlockedRegistration method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/getAllRestrictedDomains")
    public ResponseEntity<Object> getAllRestrictedDomains (@RequestHeader(name = "screenName", required = false) String screenName,
                                                                   @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                                   @RequestHeader(name = "accountIds") String accountIds,
                                                                   HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAllRestrictedDomains method.");
        try {
            List<RestrictedDomains> restrictedDomainsList = superAdminService.getAllRestricedDomains(accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllRestrictedDomains" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, restrictedDomainsList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllRestrictedDomains method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/addRestrictedDomain")
    public ResponseEntity<Object> addRestrictedDomain (@RequestBody @Valid RestrictedDomainRequest restrictedDomainRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                       @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered addRestrictedDomain method.");
        try {
            RestrictedDomains restrictedDomains = superAdminService.addRestrictedDomain(restrictedDomainRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addRestrictedDomain" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, restrictedDomains);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute addRestrictedDomain method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/updateRestrictedDomain")
    public ResponseEntity<Object> updateRestrictedDomain (@RequestBody @Valid RestrictedDomainRequest restrictedDomainRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                                   @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                                   @RequestHeader(name = "accountIds") String accountIds,
                                                                   HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered updateRestrictedDomain method.");
        try {
            RestrictedDomains restrictedDomains = superAdminService.updateRestrictedDomain(restrictedDomainRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateRestrictedDomain" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, restrictedDomains);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute updateRestrictedDomain method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @DeleteMapping("/deleteRestrictedDomain/{restrictedDomainId}")
    public ResponseEntity<Object> deleteRestrictedDomain (@PathVariable(name = "restrictedDomainId") Long restrictedDomainId, @RequestHeader(name = "screenName", required = false) String screenName,
                                                          @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered deleteRestrictedDomain method.");
        try {
            String response = superAdminService.deleteRestrictedDomain(accountIds, restrictedDomainId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deleteRestrictedDomain" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute deleteRestrictedDomain method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
