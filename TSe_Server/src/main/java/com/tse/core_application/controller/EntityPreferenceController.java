package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.leave.Response.LeaveTypesResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.EntityPreferenceService;
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
import javax.validation.Valid;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/entity-preferences")
public class EntityPreferenceController {

    private static final Logger logger = LogManager.getLogger(EntityPreferenceController.class.getName());

    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private EntityPreferenceService entityPreferenceService;

    private String createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        return username;
    }

    // expects single accountId in the header
    @Transactional
    @PostMapping(path = "/upsert/orgPreference")
    public ResponseEntity<Object> saveOrUpdateOrgPreference(@RequestBody @Valid EntityPreferenceRequest entityPreferenceRequest, @RequestHeader(name = "screenName") String screenName,
                                                            @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                            HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " saveOrUpdateOrgPreference" + '"' + " method ...");
        EntityPreferenceResponse response;
        String organizationName = "undefined";
        try {
            organizationName = entityPreferenceService.validateAndGetOrgNameFromEntityPreferenceRequest(entityPreferenceRequest, accountIds);
            response = entityPreferenceService.saveOrUpdateEntityPreference(entityPreferenceRequest, timeZone, accountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to save or update org preference for organization: "
                    + organizationName +" ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " saveOrUpdateOrgPreference" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getOrgPreference/{orgId}")
    public ResponseEntity<Object> getOrgPreference(@PathVariable Long orgId, @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                    HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOrgPreference" + '"' + " method ...");
        EntityPreferenceResponse response;
        try {
            response = entityPreferenceService.getOrgPreference(orgId, accountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOrgPreference for username "
                    + username +" ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getOrgPreference" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getPreferenceForQuickCreateCreateUpdate/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getPreferenceForQuickCreateCreateUpdate(@PathVariable Integer entityTypeId, @PathVariable Long entityId, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getPreferenceForQuickCreateCreateUpdate" + '"' + " method ...");
        CreateUpdateTaskPreferenceResponse response;
        try {
            response = entityPreferenceService.getCreateAndUpdateTaskPreference(entityTypeId, entityId, accountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getPreferenceForQuickCreateCreateUpdate for username "
                    + username +" ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getPreferenceForQuickCreateCreateUpdate" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getHolidaysForOrg/{orgId}")
    public ResponseEntity<Object> getHolidaysForOrg(@PathVariable Long orgId, @RequestHeader(name = "screenName") String screenName,
                                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                          HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " getHolidaysForOrg" + '"' + " method ...");
        HolidaysResponse response;
        try {
            response = entityPreferenceService.getHolidaysForOrg(orgId, accountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getHolidaysForOrg for username "
                    + username +" ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getHolidaysForOrg" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getLeaveTypeAlias/{orgId}")
    public ResponseEntity<Object> getLeaveTypeAlias(@PathVariable Long orgId, @RequestHeader(name = "screenName") String screenName,
                                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                          HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getLeaveTypeAlias" + '"' + " method ...");
        List<LeaveTypesResponse> response;
        try {
            response = entityPreferenceService.getLeaveTypeAlias(orgId);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getLeaveTypeAlias for username "
                    + username +" ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getLeaveTypeAlias" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getTeamPreference/{teamId}")
    public ResponseEntity<Object> getTeamPreference(@PathVariable("teamId") Long teamId, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request){
        String jwtToken = request.getHeader("Authorization").substring(7);
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

        TeamPreferenceResponse response = entityPreferenceService.getTeamPreference(teamId);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @PostMapping(path = "/saveTeamPreference")
    public ResponseEntity<Object> saveTeamPreference(@RequestBody TeamPreferenceRequest preferenceRequest,
                                                     @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) {

        String jwtToken = request.getHeader("Authorization").substring(7);
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

        String response = entityPreferenceService.saveTeamPreference(preferenceRequest, Long.valueOf(accountIds));
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);

    }
}
