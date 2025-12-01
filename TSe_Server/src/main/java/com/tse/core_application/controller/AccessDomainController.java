package com.tse.core_application.controller;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.RemoveTeamMemberRequest;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.AccessDomainService;
import com.tse.core_application.service.Impl.AuditService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/accessDomain")
public class AccessDomainController {

    private static final Logger logger = LogManager.getLogger(AccessDomainController.class.getName());
    @Autowired
    UserRepository userRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private AuditService auditService;
    @Autowired
    private UserService userService;

    //  endpoint to save list of accessDomains -- expects a single accountId in the header
    @Transactional
    @PostMapping(path = "/createAndEditAccessDomain/{teamId}")
    public ResponseEntity<Object> addAccessDomain(@PathVariable(name = "teamId") Long teamId, @RequestBody List<AccessDomain> accessDomains,
                                @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addAccessDomain" + '"' + " method ...");
        try {
            accessDomainService.ifOrgPersonalAndTeamDefault(accountIds, teamId);
            List<AccessDomain> filteredAccessDomains = accessDomainService.filterAccessDomain(accessDomains);
            accessDomainService.addAndEditAccessDomains(teamId, filteredAccessDomains, timeZone, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addAccessDomain" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add in access domain table for username = " + foundUser.getPrimaryEmail() +
                    " for teamId = " + teamId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Access domains saved successfully");
    }

    //  endPoint to delete the added teamMembers
    @PostMapping(path = "/removeTeamMember")
    @Transactional
    public void deleteTeamMembersByEmail(@RequestBody RemoveTeamMemberRequest removeTeamMemberRequest, @RequestHeader(name = "screenName") String screenName,
                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                         HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " deleteTeamMembersByEmail" + '"' + " method ...");
        try {
            List<Long> accountIdsOfUser = jwtUtil.getAllAccountIdsFromToken(jwtToken);
            accessDomainService.deleteAddedTeamMembers(removeTeamMemberRequest, timeZone, accountIdsOfUser);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deleteTeamMembersByEmail" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete team member for username = " + foundUser.getPrimaryEmail() +
                    " ,    " + "Team Member Details: username = " + removeTeamMemberRequest.getEmail() + " ,  " + "role = " + removeTeamMemberRequest.getRoleName() + " ,   " + "teamId = " + removeTeamMemberRequest.getTeamId() + " ,    " +
                    "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    //  endPoint to get added team members along with their roles
    @GetMapping(path = "/getTeamMembers/{teamId}")
    public ResponseEntity<Object> getTeamMembersByTokenAndTeamId(@PathVariable(name = "teamId") Long teamId, @RequestHeader(name = "screenName") String screenName,
                                                                 @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                  HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTeamMembersByTokenAndTeamId" + '"' + " method ...");
        List<HashMap<String, Object>> emailFirstLastNameList = null;
        try {
            emailFirstLastNameList = accessDomainService.getEmailFirstNameLastNameRoleList(teamId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTeamMembersByTokenAndTeamId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof NoDataFoundException) {
                logger.error("No Data found for"+request.getRequestURI()+ "Caught Exception: " + e, new Throwable(allStackTraces));
                throw new NoDataFoundException();
            } else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the added team members list for username = " + foundUser.getPrimaryEmail() +
                        " for teamId = " + teamId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "success", emailFirstLastNameList);
    }

    @GetMapping(path = "/getEntityMembers/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getEntityMembers(@PathVariable(name = "entityTypeId") Integer entityTypeId, @PathVariable(name = "entityId") Long entityId, @RequestHeader(name = "screenName") String screenName,
                                                                 @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                 HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getEntityMembers" + '"' + " method ...");
        List<EmailFirstLastAccountId> responseMembersList = new ArrayList<>();
        try {
            responseMembersList = accessDomainService.getEntityMembers(entityTypeId, entityId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getEntityMembers" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof NoDataFoundException) {
                logger.error("No Data found for"+request.getRequestURI()+ "Caught Exception: " + e, new Throwable(allStackTraces));
                throw new NoDataFoundException();
            } else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the members list for username = " + foundUser.getPrimaryEmail() +
                        " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "success", responseMembersList);
    }

    @GetMapping(path = "/getAllEntityMembers/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getAllEntityMembers(@PathVariable(name = "entityTypeId") Integer entityTypeId, @PathVariable(name = "entityId") Long entityId, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllEntityMembers" + '"' + " method ...");
        List<EmailFirstLastAccountIdIsActive> responseMembersList = new ArrayList<>();
        try {
            responseMembersList = accessDomainService.getAllEntityMembers(entityTypeId, entityId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllEntityMembers" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof NoDataFoundException) {
                logger.error("No Data found for"+request.getRequestURI()+ "Caught Exception: " + e, new Throwable(allStackTraces));
                throw new NoDataFoundException();
            } else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the members list for username = " + foundUser.getPrimaryEmail() +
                        " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "success", responseMembersList);
    }
}
