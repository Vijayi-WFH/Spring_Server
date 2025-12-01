package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.OrganizationInviteResponse;
import com.tse.core_application.dto.InvitesResponse;
import com.tse.core_application.dto.TeamInviteResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.InvalidInviteException;
import com.tse.core_application.exception.InvalidRequestHeaderException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Invite;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.EntityPreferenceRepository;
import com.tse.core_application.repository.InviteRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.InviteService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin(value = "*")
@RequestMapping(path = "/userInvite")
public class InviteController {

    private static final Logger logger = LogManager.getLogger(InviteController.class.getName());

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private InviteService inviteService;
    @Autowired
    private InviteRepository inviteRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
    }

    @GetMapping(path = "/getInviteDetails/{inviteId}")
    public ResponseEntity<Object> getInviteDetails(@PathVariable(name = "inviteId") String inviteId,
                                                   @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        boolean isHeadersValidated = CommonUtils.validateTimeZoneAndScreenNameInHeader(timeZone, screenName);
        if (!isHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("getInviteDetails API: Headers are not validated" + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getInviteDetails" + '"' + " method ...");
        OrganizationInviteResponse response;
        try {
            response = inviteService.getInviteDetails(inviteId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", estimatedTime + "");
            logger.info("Exited" + '"' + " getInviteDetails" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getInviteDetails" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    // expects one accountId
    @PutMapping(path = "/revoke/{inviteId}")
    public ResponseEntity<Object> revokeInvite(@PathVariable(name = "inviteId") String inviteId,
                                               @RequestHeader (name = "accountIds") String accountIds,
                                               @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " revokeInvite" + '"' + " method ...");
        try {
            inviteService.revokeInvite(inviteId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " revokeInvite" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute revokeInvite" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Invite Revoked Successfully");
    }

    @PostMapping(path = "/resend/{inviteId}")
    public ResponseEntity<Object> resendInvite(@PathVariable(name = "inviteId") String inviteId,
                                               @RequestHeader (name = "accountIds") String accountIds,
                                               @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " resendInvite" + '"' + " method ...");
        Invite invite = null;
        try {
            invite = inviteRepository.findByInviteIdAndIsRevoked(inviteId, false).orElseThrow(() -> new InvalidInviteException("Invalid invite id"));
            inviteService.resendInvite(invite, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " resendInvite" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute resendInvite" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        if (Objects.equals(invite.getEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.ORG)) {
            Boolean isUserPartOfSomeOtherOrg = true;
            List<Long> orgIdList = userAccountRepository.findAllOrgIdByEmailAndIsActive(invite.getPrimaryEmail(), true);
            if (orgIdList == null || orgIdList.isEmpty()) {
                isUserPartOfSomeOtherOrg = false;
            }
            Boolean shouldOtpSendToOrgAdmin = entityPreferenceRepository.existsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin (com.tse.core_application.model.Constants.EntityTypes.ORG, List.of(invite.getEntityId()), true);
            if (shouldOtpSendToOrgAdmin != null && shouldOtpSendToOrgAdmin && isUserPartOfSomeOtherOrg) {
                String message = "This user is part of another organization. Hence, the OTPs generated for this user will never be shared with the Org Admin.";
                return CustomResponseHandler.generateCustomResponse(HttpStatus.ALREADY_REPORTED, Constants.FormattedResponse.WARNING, message);
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Invite Resend Successfully");
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Invite Resend Successfully");
    }

    // expects one accountId
    @PutMapping(path = "/editValidity/{inviteId}")
    public ResponseEntity<Object> editValidity(@PathVariable(name = "inviteId") String inviteId,
                                               @RequestParam(name = "newDuration") int newDuration,
                                               @RequestHeader (name = "accountIds") String accountIds,
                                               @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " editValidity" + '"' + " method ...");
        Invite response;
        try {
            response = inviteService.editInviteValidity(inviteId, newDuration, timeZone, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", estimatedTime + "");
            logger.info("Exited" + '"' + " editValidity" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute editValidity" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    // expects single accountId in the header of the org admin
    @GetMapping("/invitees/{orgId}")
    public ResponseEntity<Object> getOrganizationInvites(@PathVariable(name = "orgId") Long orgId,
                                                         @RequestHeader (name = "accountIds") String accountIds,
                                                         @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOrganizationInvites" + '"' + " method ...");
        InvitesResponse response;
        try {
            response = inviteService.getOrganizationInvites(orgId, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", estimatedTime + "");
            logger.info("Exited" + '"' + " getOrganizationInvites" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrganizationInvites" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getInviteDetailsForTeam/{inviteId}")
    public ResponseEntity<Object> getInviteDetailsForTeam(@PathVariable(name = "inviteId") String inviteId,
                                                   @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "accountIds") String accountIds,
                                                   @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getInviteDetailsForTeam" + '"' + " method ...");
        boolean isHeadersValidated = CommonUtils.validateTimeZoneAndScreenNameInHeader(timeZone, screenName);
        if (!isHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("getInviteDetailsForTeam API: Headers are not validated" + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getInviteDetailsForTeam" + '"' + " method ...");
        TeamInviteResponse response;
        try {
            response = inviteService.getTeamInviteDetails(inviteId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", estimatedTime + "");
            logger.info("Exited" + '"' + " getInviteDetailsForTeam" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getInviteDetailsForTeam" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping("/teamInvitees/{teamId}")
    public ResponseEntity<Object> getTeamInvites(@PathVariable(name = "teamId") Long teamId,
                                                         @RequestHeader (name = "accountIds") String accountIds,
                                                         @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTeamInvites" + '"' + " method ...");
        InvitesResponse response;
        try {
            response = inviteService.getTeamInvites(teamId, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", estimatedTime + "");
            logger.info("Exited" + '"' + " getTeamInvites" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getTeamInvites" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    }
