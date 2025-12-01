package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.AllEpicResponse;
import com.tse.core_application.dto.BlockedByRequestDto;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.UserListResponse;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.model.AccessDomain;
import com.tse.core_application.model.Organization;
import com.tse.core_application.model.Team;
import com.tse.core_application.model.User;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.OrganizationDoesNotExistException;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.OrganizationService;
import com.tse.core_application.service.Impl.UserAccountService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/userAccount")
public class UserAccountController {

    private static final Logger logger = LogManager.getLogger(UserAccountController.class.getName());
    @Autowired
    UserAccountService userAccountService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private UserService userService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    //  endPoint to get user's userAccountId by orgName
    @GetMapping(path = "/userAccountId/{orgName}")
    public ResponseEntity<Object> getAccountId(@PathVariable String orgName, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAccountId" + '"' + " method ...");
        Organization organizationDb = null;
        try {
//            organizationDb = organizationRepository.findByOrganizationNameIgnoreCase(orgName);
            organizationDb = organizationService.getOrganizationByOrganizationName(orgName);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAccountId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAccountId() by organization for the username = " +
                    foundUser.getPrimaryEmail() + " ,     " + "orgName = " + orgName + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return userAccountService.getAccountIdFormattedResponse(organizationDb, orgName);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof OrganizationDoesNotExistException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAccountId() by organization for the username = " +
                        foundUser.getPrimaryEmail() + " ,     " + "orgName = " + orgName + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    //  endPoint to get user's email, firstName, lastName, accountId by orgId
    @GetMapping(path = "/userList/{orgId}")
    public ResponseEntity<Object> getUserList(@PathVariable(name = "orgId") Long orgId, @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserList" + '"' + " method ...");
        List<UserListResponse> userListResponses = null;
        try {
            if (organizationRepository.findByOrgId(orgId).getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                userListResponses = Collections.emptyList();
            } else {
                userListResponses = userRepository.getEmailAccountIdFirstMiddleAndLastNameByOrgId(orgId);

                if (userListResponses != null && !userListResponses.isEmpty()) {
                    userListResponses.sort(Comparator
                            .comparing((UserListResponse status) -> {
                                if (status.getIsActive() == null) return 2;
                                return status.getIsActive() ? 0 : 1;
                            })
                            .thenComparing(UserListResponse::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(UserListResponse::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(UserListResponse::getAccountId, Comparator.nullsLast(Long::compareTo))
                    );
                }
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserList" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: No able to execute getUserList() by organization for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "orgId = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userListResponses);
        } catch (Exception e) {
            if (e instanceof NoDataFoundException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: No able to execute getUserList() by organization for the username = " + foundUser.getPrimaryEmail() +
                        " ,     " + "orgId = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    //  endPoint to get a user by accountId
    @GetMapping(path = "/getUserDetailsByAccountId/{accountId}")
    public ResponseEntity<Object> getUserByAccountId(@PathVariable(name = "accountId") Long accountId, @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserByAccountId" + '"' + " method ...");
        EmailFirstLastAccountId emailFirstLastAccountId = null;
        try {
            emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdAndIsActive(accountId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserByAccountId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserByAccountId() by accountId for the username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "accountId = " + accountId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return userAccountService.getUserByAccountIdFormattedResponse(emailFirstLastAccountId);
        } catch (Exception e) {
            if (e instanceof NoDataFoundException) {
                throw e;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Not able to execute getUserByAccount() for the username = " + foundUser.getPrimaryEmail() + " ,     " +
                        "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @GetMapping(path = "/orgMembersExcludedByCurrentTeam/{teamId}")
    public ResponseEntity<Object> getOrgMembersExcludedByCurrentTeam(@PathVariable(name = "teamId") Long teamId, @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOrgMembersExcludedByCurrentTeam" + '"' + " method ...");
        List<EmailFirstLastAccountIdIsActive> userListResponses;
        try {
                userListResponses=userAccountService.excludeTeamMemberFromOrg(teamId);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOrgMembersExcludedByCurrentTeam" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: No able to execute getOrgMembersExcludedByCurrentTeam by team for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "teamId = " + teamId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userListResponses);
        } catch (Exception e) {
            if (e instanceof NoDataFoundException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: No able to execute getOrgMembersExcludedByCurrentTeam for the username = " + foundUser.getPrimaryEmail() +
                        " ,     " + "teamId = " + teamId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @PostMapping(path = "/blockedByMembers")
    public ResponseEntity<Object> blockedByMembers(@RequestBody BlockedByRequestDto blockedByRequestDto,
                                                   @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " blockedByMembers" + '"' + " method ...");
        List<EmailFirstLastAccountIdIsActive> userListResponses;
        try {
            userListResponses = userAccountService.blockedByMembers(blockedByRequestDto, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " blockedByMembers" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, userListResponses);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to blockedByMembers for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
