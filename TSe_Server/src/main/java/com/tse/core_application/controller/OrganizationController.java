package com.tse.core_application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.BuIdAndBuName;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.org_response.OrgStructureResponse;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import com.tse.core_application.utils.LongListConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CrossOrigin(value = "*")
@RestController
@Validated
@RequestMapping(path = "/organization")
public class OrganizationController {

    private static final Logger logger = LogManager.getLogger(OrganizationController.class.getName());
    @Autowired
    UserRepository userRepository;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private UserService userService;
    @Autowired
    private OrgRequestsRepository orgRequestsRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private InviteService inviteService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomEnvironmentService customEnvironmentService;

    @Autowired
    private UserFeatureAccessService userFeatureAccessService;

    @Autowired
    private SuperAdminService superAdminService;

    //endpoint to get user's orgId, orgName by user's token
    @GetMapping(path = "/userAllOrganizationList")
    public ResponseEntity<Object> getUserOrganizationList(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserOrganizationList" + '"' + " method ...");
        List<OrgIdOrgName> orgIdOrgNames = null;
        try {
            orgIdOrgNames = organizationService.getOrganizationByToken(tokenUsername);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserOrganizationList" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            if (e instanceof NoDataFoundException) {
                throw e;
            } else {
                e.printStackTrace();
                String allSTackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the user organization list for the username = " + foundUser.getPrimaryEmail() +
                        " ,     " + "Caught Exception: " + e, new Throwable(allSTackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, orgIdOrgNames);
    }

    //  endpoint to get ownerAccountId by user's email
    @GetMapping(path = "/ownerAccountId/{email}/{orgId}")
    public ResponseEntity<Object> getOwnerAccountIdByOrgId(@PathVariable(name = "email") String email, @PathVariable(name = "orgId") Long orgId,
                                                           @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                           @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOwnerAccountIdByOrgId" + '"' + " method ...");
        UserAccount userAccountDb = null;
        try {
            userAccountDb = organizationService.getUserAccountIdFromUserAccount(email, orgId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOwnerAccountIdByOrgId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            if (e instanceof NoDataFoundException) {
                throw e;
            } else {
                if (e instanceof UserDoesNotExistException) {
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the accountId for the email = " + email +
                            " for the username = " + foundUser.getPrimaryEmail() + " in organization = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                    else throw e;
                }
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userAccountDb);
    }

    @GetMapping(path = "/getOrgToEdit")
    public ResponseEntity<Object> getOrgToEdit(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getOrgToEdit" + '"' + " method ...");

        try {
            LongListConverter longListConverter = new LongListConverter();
            List<Long> account = longListConverter.convertToEntityAttribute(accountIds);
            List<OrgIdOrgName> orgIdOrgNames = organizationService.getOrgForOrgAdmin(account);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgToEdit" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, orgIdOrgNames);
        } catch (Exception e) {
            if (e instanceof ValidationFailedException) {
                throw e;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgToEdit for accountIds = " + accountIds + " Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    @GetMapping(path = "/getOrgMembers/{orgId}")
    public ResponseEntity<Object> getOrgMembers(@PathVariable(name = "orgId") Long orgId,
                                               @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                               @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getOrgMembers" + '"' + " method ...");

        try {
            LongListConverter longListConverter = new LongListConverter();
            List<Long> account = longListConverter.convertToEntityAttribute(accountIds);
            OrgStructureResponse orgStructureResponseList = organizationService.getOrgStructureToEdit(orgId,account);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgMembers" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, orgStructureResponseList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof NoDataFoundException){
                logger.error(request.getRequestURI() + " User does not own org with orgId = " + orgId + " Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NO_CONTENT, Constants.FormattedResponse.NO_CONTENT, "user does not own this org");
            }
            else if(e instanceof ValidationFailedException) {
                throw e;
            }
            else{
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgMembers for orgId = " + orgId + " Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    @PostMapping(path = "/removeMemberFromOrg")
    @Transactional
    public ResponseEntity<Object> removeMemberFromOrg(@RequestBody RemoveOrgMemberRequest removeOrgMemberRequest,
                                                @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws JsonProcessingException, IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " removeMemberFromOrg" + '"' + " method ...");

        try {
            List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
            boolean result = organizationService.removeMemberFromOrg(removeOrgMemberRequest, accountIdsList, timeZone, foundUser);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeMemberFromOrg" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            if(!result){
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, Constants.FormattedResponse.FORBIDDEN, "Requested organization does not exists.");
                }
            notificationService.sendRemoveUserNotification(removeOrgMemberRequest, timeZone);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "User removed successfully.");
        } catch (Exception e) {
            userAccountService.markAccountAsActive(removeOrgMemberRequest.getAccountId());
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof UserDoesNotExistException){
                logger.error("The org admin trying to remove user who is not part of the org. OrgId= "+removeOrgMemberRequest.getOrgId()+" AccountId= "+ (removeOrgMemberRequest.getAccountId()) +"Caught Exception: "+e,new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_MODIFIED, Constants.FormattedResponse.NO_CONTENT, "The user does not exists in the org.");
            }
            else if(e instanceof ValidationFailedException) {
                throw e;
            }
            else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute removeMemberFromOrg. Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    @PostMapping(path = "/addNewMemberToOrg")
    @Transactional
    public ResponseEntity<Object> addNewMemberToOrg(@RequestBody NewOrgMemberRequest newOrgMemberRequest,
                                                      @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws JsonProcessingException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " addNewMemberToOrg" + '"' + " method ...");
        try {
            int returnState = organizationService.addNewMemberToOrg(newOrgMemberRequest,Long.valueOf(accountIds),timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addNewMemberToOrg" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            switch (returnState){
                case 0:{
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, Constants.FormattedResponse.FORBIDDEN, "User already exists in the organization.");
                }
                case 1:{
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, Constants.FormattedResponse.FORBIDDEN, "Request already send to this user.");
                }
                case 2:{
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, Constants.FormattedResponse.FORBIDDEN, "Requested organization does not exists.");
                }
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Request send successfully.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof UserDoesNotExistException){
                logger.error("User do not exists for userName= "+newOrgMemberRequest.getUserName()+"Caught Exception: "+e,new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, "The user does not exist. Please make sure username is already registered.");
            }
            else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute addNewMemberToOrg. Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    @PostMapping(path = "/getOrgRequest")
    public ResponseEntity<Object> getOrgRequest(@RequestParam Long userId,
                                                    @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getOrgRequest" + '"' + " method ...");
        try {

            List<OrgRequests> orgRequestsList = organizationService.getOrgRequest(userId,accountIds);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgRequest" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();


            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, orgRequestsList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof UserDoesNotExistException){
                logger.error("User do not exists for userId= "+userId+"Caught Exception: "+e,new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, "The user does not exist.");
            } else if (e instanceof UnauthorizedException) {
                logger.error("You are not authorized to see user requests for userId= "+userId+"Caught Exception: "+e,new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, Constants.FormattedResponse.FORBIDDEN, "You are not authorized to see user requests for userId="+userId);
            } else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgRequest. Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    @PostMapping(path = "/updateOrgRequest/{orgRequestId}/{response}")
    public ResponseEntity<Object> updateOrgRequest(@PathVariable Long orgRequestId,@PathVariable Boolean response,
                                                @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getOrgRequest" + '"' + " method ...");
        try {
            organizationService.updateOrgRequest(orgRequestId,response);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgRequest" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            if(response) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Welcome! You are now a part of "+organizationRepository.findByOrgId(orgRequestsRepository.findByOrgRequestId(orgRequestId).getFromOrgId()).getOrganizationName());
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Thank you! for your response.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof NoDataFoundException){
                logger.error("Org Request do not exists for orgRequestId= "+orgRequestId+"Caught Exception: "+e,new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, "Org Request do not exists.");
            }
            else{
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgRequest. Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    // expects single accountId
    @PostMapping(path = "/inviteUserToOrg")
    public ResponseEntity<Object> createInvite(@Valid @RequestBody InviteRequest inviteRequest, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " createInvite" + '"' + " method ...");
        Invite response = null;
        Boolean isUserPartOfSomeOtherOrg = true;
        Boolean shouldOtpSendToOrgAdmin = null;
        Boolean isUserVerificationRemains = false;
        try {
            if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.ORG, inviteRequest.getEntityTypeId())) {
                Organization organization = organizationRepository.findByOrgId(inviteRequest.getEntityId());
                if (organization != null) {
                    UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(inviteRequest.getPrimaryEmail(), inviteRequest.getEntityId(), true);
                    if (userAccount != null && userAccount.getIsActive() && userAccount.getIsVerified() != null && !userAccount.getIsVerified()) {
                        isUserVerificationRemains = true;
                        emailService.sendVerificationEmail(inviteRequest.getPrimaryEmail(), inviteRequest.getFirstName(), organization.getOrgId(), organization.getOrganizationName());
                    }
                }
            }
            if (!isUserVerificationRemains) {
                List<Long> orgIdList = userAccountRepository.findAllOrgIdByEmailAndIsActive(inviteRequest.getPrimaryEmail(), true);
                if (orgIdList == null || orgIdList.isEmpty()) {
                    isUserPartOfSomeOtherOrg = false;
                }

                shouldOtpSendToOrgAdmin = entityPreferenceRepository.existsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin(com.tse.core_application.model.Constants.EntityTypes.ORG, List.of(inviteRequest.getEntityId()), true);
                response = inviteService.createInvite(inviteRequest, Long.parseLong(accountIds), timeZone);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " createInvite" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to send invite to user requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        if (isUserVerificationRemains) {
            response = new Invite();
            response.setMessage("User is already registered but not verified so verification link has been sent");
            return CustomResponseHandler.generateCustomResponse(HttpStatus.ALREADY_REPORTED, Constants.FormattedResponse.WARNING, response);
        }
        if (shouldOtpSendToOrgAdmin != null && shouldOtpSendToOrgAdmin && isUserPartOfSomeOtherOrg) {
            response.setMessage("This user is part of another organization. Hence, the OTPs generated for this user will never be shared with the Org Admin.");
            return CustomResponseHandler.generateCustomResponse(HttpStatus.ALREADY_REPORTED, Constants.FormattedResponse.WARNING, response);
        }
        response.setMessage("Invite sent successfully");
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    // checks whether the organization already exists in the APP. if the organization already exists, we allow registration via invite only method
    @GetMapping("/exists")
    public ResponseEntity<Object> checkOrganizationExists(@RequestParam(name = "orgName") String orgName, @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        boolean isHeadersValidated = CommonUtils.validateTimeZoneAndScreenNameInHeader(timeZone, screenName);
        if (!isHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error(request.getRequestURI() + "API: Headers are not validated" + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " checkOrganizationExists" + '"' + " method ...");
        boolean exists = false;
        try {
            exists = organizationService.doesOrganizationExist(orgName);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " checkOrganizationExists" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute method checkOrganizationExists requested by user , Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        if (exists) {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.FORBIDDEN, Constants.FormattedResponse.FORBIDDEN, "Organization already exists");
        } else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "New Organization");
        }
    }

    @GetMapping(path = "/getOrgAllBU/{orgId}")
    public ResponseEntity<Object> getOrgAllBU(@PathVariable Long orgId,
                                                   @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                   @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getOrgAllBU" + '"' + " method ...");
        try {
            List<BuIdAndBuName> buList = organizationService.getOrgAllBU(orgId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgAllBU" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, buList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgAllBU. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;

        }
    }

    @PostMapping(path = "/createCustomEnvironment")
    public ResponseEntity<Object> createCustomEnvironment(@Valid @RequestBody EnvironmentRequest environmentRequest,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " createCustomEnvironment" + '"' + " method ...");

        try {
            EnviornmentResponse enviornmentResponse = customEnvironmentService.createEnvironment(environmentRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " createCustomEnvironment" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, enviornmentResponse);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create environment for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/updateCustomEnvironment/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> updateCustomEnvironment(@RequestBody @Valid List<@Valid EnvironmentUpdateRequest> environmentRequestList,
                                                          @PathVariable Integer entityTypeId,
                                                          @PathVariable Long entityId,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());

        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateCustomEnvironment" + '"' + " method ...");

        try {
            List<EnviornmentResponse> enviornmentResponseList = customEnvironmentService.updateEnvironment(entityTypeId, entityId, accountIds, environmentRequestList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "updateCustomEnvironment" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, enviornmentResponseList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create environment for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping(path = "/getCustomEnvironment/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getEnvironment(
            @PathVariable Integer entityTypeId,
            @PathVariable Long entityId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getEnvironment" + '"' + " method ...");

        try {
            List<EnviornmentResponse> enviornmentResponseList = customEnvironmentService.getEnvironment(entityTypeId, entityId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getEnvironment" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, enviornmentResponseList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create environment for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping(path = "/getActiveCustomEnvironment/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getEnvironmentIsActive(
            @PathVariable Integer entityTypeId,
            @PathVariable Long entityId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getActiveCustomEnvironment" + '"' + " method ...");

        try {
            List<EnviornmentResponse> enviornmentResponseList = customEnvironmentService.getEnvironmentIsActive(entityTypeId, entityId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getActiveCustomEnvironment" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, enviornmentResponseList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create environment for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
    @PostMapping(path = "/addFeatureAccess")
    public ResponseEntity<Object> addFeatureAccess(@Valid @RequestBody AddHrRoleRequestDto addHrRoleRequestDto,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addFeatureAccess" + '"' + " method ...");
        try {
            UserFeatureAccessResponseForAllDto response= userFeatureAccessService.addHrRole(addHrRoleRequestDto, timeZone,accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addFeatureAccess" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add FeatureAccess for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
    @PostMapping(path = "/updateFeatureAccess")
    public ResponseEntity<Object> updateFeatureAccess(@Valid @RequestBody UpdateHrActionsDto updateHrActionsDto,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());

        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateFeatureAccess" + '"' + " method ...");

        try {
            UserFeatureAccessResponseForAllDto userFeatureAccessResponseDto = userFeatureAccessService.updateActionForUser(updateHrActionsDto,accountIds,timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "updateFeatureAccess" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, userFeatureAccessResponseDto);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update FeatureAccess for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
    @GetMapping(path = "/getAllFeatureAccess/{orgId}")
    public ResponseEntity<Object> getAllFeatureAccess(
                                                          @PathVariable Long orgId,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllFeaturesAccess" + '"' + " method ...");

        try {
            List<UserFeatureAccessResponseForAllDto> response= userFeatureAccessService.getAllHrRoleActions(orgId,accountIds,timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "getAllFeaturesAccess" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create environment for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
    @PostMapping(path = "/deleteFeatureAccess")
    public ResponseEntity<Object> deleteFeatureAccess(@Valid @RequestBody RemoveHrRoleDto removeHrRoleDto,
                                                      @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());

        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " deleteFeatureAccess" + '"' + " method ...");

        try {
            userFeatureAccessService.removeHrRole(removeHrRoleDto, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "deleteFeatureAccess" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "User Feature Access deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete featureAccess for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
    @GetMapping(path = "/getFeatureAccessActions/{orgId}")
    public ResponseEntity<Object> getFeatureAccessActions(
                                               @PathVariable Long orgId,
                                               @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getFeatureAccessActions" + '"' + " method ...");

        try {
            List<ActionResponseDto> actionList = userFeatureAccessService.getActionsForHrRole(orgId,accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "getFeatureAccessActions" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, actionList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able getFeatureAccessActions for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/activateAccountIdsInOrg")
    public ResponseEntity<Object> activateAccountIdsInOrg(@Valid @RequestBody ReactivateDeactivateUserRequest reactivateUserRequest, @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " activateAccountIdsInOrg" + '"' + " method ...");
        try {
            superAdminService.reactivateAccounts(reactivateUserRequest, accountIds, foundUser, false);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " activateAccountIdsInOrg" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Accounts provided were successfully reactivated.");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to activateAccountIdsInOrg for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/deactivateAccountIdsInOrg")
    public ResponseEntity<Object> deactivateAccountIdsInOrg(@Valid @RequestBody ReactivateDeactivateUserRequest deactivateUserRequest, @RequestHeader(name = "screenName") String screenName,
                                                            @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                            HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " deactivateAccountIdsInOrg" + '"' + " method ...");
        try {
            superAdminService.deactivateAccounts(deactivateUserRequest, accountIds, foundUser, false);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deactivateAccountIdsInOrg" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Accounts provided were successfully deactivated.");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to deactivateAccountIdsInOrg for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

}
