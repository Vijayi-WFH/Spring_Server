package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.SprintRequest;
import com.tse.core_application.dto.TeamCapacityResponse;
import com.tse.core_application.dto.capacity.LoadedCapacityRatioUpdateRequest;
import com.tse.core_application.dto.capacity.SprintCapacityDetails;
import com.tse.core_application.dto.capacity.UserSprintCapacityDetails;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.CapacityService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/capacity")
public class CapacityController {

    private static final Logger logger = LogManager.getLogger(CapacityController.class.getName());
    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private CapacityService capacityService;

    private void createThreadContextLogContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
    }


    @GetMapping(path = "/getSprintCapacityDetails/{sprintId}")
    public ResponseEntity<Object> getSprintCapacityDetails(@PathVariable(name = "sprintId") Long sprintId,
                                             @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone,
                                             @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createThreadContextLogContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " getSprintCapacityDetails" + '"' + " method ...");
        SprintCapacityDetails sprintCapacityDetails;
        try {
            sprintCapacityDetails = capacityService.getSprintCapacityDetails(sprintId, accountIds, timeZone);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(String.format("%s API: Something went wrong: Not able to get capacity for the sprint id: %d ,     Caught Exception: %s", request.getRequestURI(), sprintId, e), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getSprintCapacityDetails" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, sprintCapacityDetails);
    }

    @GetMapping(path = "/getUserSprintCapacityDetails/{accountId}/{sprintId}")
    public ResponseEntity<Object> getAccountCapacityDetails(@PathVariable(name = "sprintId") Long sprintId,
                                                       @PathVariable(name = "accountId") Long accountId,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createThreadContextLogContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " getUserSprintCapacityDetails" + '"' + " method ...");
        UserSprintCapacityDetails userSprintCapacityDetails;
        try {
            userSprintCapacityDetails = capacityService.getUserSprintCapacityDetails(accountId, sprintId, timeZone);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get capacity for the sprint id: " + sprintId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getUserSprintCapacityDetails" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userSprintCapacityDetails);
    }

    @PostMapping("/updateLoadedCapacityRatios")
    @Transactional
    public ResponseEntity<Object> updateLoadedCapacityRatios(@RequestBody LoadedCapacityRatioUpdateRequest request,
                                                             @RequestHeader(name = "screenName") String screenName,
                                                             @RequestHeader(name = "timeZone") String timeZone,
                                                             @RequestHeader(name = "accountIds") String accountIds,
                                                             HttpServletRequest servletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = servletRequest.getHeader("Authorization").substring(7);
        createThreadContextLogContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered updateLoadedCapacityRatios method ...");

        try {
            capacityService.updateLoadedCapacityRatios(request);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(servletRequest.getRequestURI() + " API: Something went wrong: Not able to update loaded capacity ratios for sprint id: " + request.getSprintId() +
                    ", Caught Exception: ", e);
            ThreadContext.clearMap();
            if (e.getMessage() == null) {
                throw new InternalServerErrorException("Internal Server Error!");
            } else {
                throw e;
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited updateLoadedCapacityRatios method because it completed successfully ...");
        ThreadContext.clearMap();

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Loaded capacity ratios updated successfully.");
    }

    @GetMapping("/getTeamCapacity/{teamId}")
    @Transactional
    public ResponseEntity<Object> getTeamCapacity(@PathVariable Long teamId,
                                                             @RequestHeader(name = "screenName") String screenName,
                                                             @RequestHeader(name = "timeZone") String timeZone,
                                                             @RequestHeader(name = "accountIds") String accountIds,
                                                             HttpServletRequest servletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = servletRequest.getHeader("Authorization").substring(7);
        createThreadContextLogContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered updateLoadedCapacityRatios method ...");
        TeamCapacityResponse response = new TeamCapacityResponse();
        try {
            response = capacityService.getTeamCapacity(teamId, accountIds, timeZone);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(servletRequest.getRequestURI() + " API: Something went wrong: Not able to capacity ratios for team id: " + teamId +
                    ", Caught Exception: ", e);
            ThreadContext.clearMap();
            if (e.getMessage() == null) {
                throw new InternalServerErrorException("Internal Server Error!");
            } else {
                throw e;
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited getTeamCapacity method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @PostMapping("/fetchAnotherSprintLoadedCapacityRatios/{sprintId}/{fetchLoadFactorOfSprint}")
    @Transactional
    public ResponseEntity<Object> fetchAnotherSprintLoadedCapacityRatios(@PathVariable Long sprintId,
                                                                         @PathVariable Long fetchLoadFactorOfSprint,
                                                                         @RequestHeader(name = "screenName") String screenName,
                                                                         @RequestHeader(name = "timeZone") String timeZone,
                                                                         @RequestHeader(name = "accountIds") String accountIds,
                                                                         HttpServletRequest servletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = servletRequest.getHeader("Authorization").substring(7);
        createThreadContextLogContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered updateLoadedCapacityRatios method ...");

        try {
            capacityService.fetchAnotherSprintLoadedCapacityRatios(sprintId,fetchLoadFactorOfSprint);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(servletRequest.getRequestURI() + " API: Something went wrong: Not able to update loaded capacity ratios for sprint id: " + sprintId +
                    ", Caught Exception: ", e);
            ThreadContext.clearMap();
            if (e.getMessage() == null) {
                throw new InternalServerErrorException("Internal Server Error!");
            } else {
                throw e;
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited updateLoadedCapacityRatios method because it completed successfully ...");
        ThreadContext.clearMap();

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS,"Load Factor of user are Updated Successfully!!!");
    }

}
