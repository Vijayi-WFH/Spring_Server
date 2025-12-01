package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.AlertRequest;
import com.tse.core_application.dto.AlertResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.AlertService;
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
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/alert")
public class AlertController {

    private static final Logger logger = LogManager.getLogger(AlertController.class.getName());

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private AlertService alertService;

    @PostMapping("/addAlert")
    public ResponseEntity<Object> addAlert (@RequestBody @Valid AlertRequest alertRequest, @RequestHeader(name = "screenName", required = false) String screenName,
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
        logger.info("Entered addAlert method.");
        AlertResponse response = new AlertResponse();
        try {
            response = alertService.addAlert(alertRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addAlert" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute addAlert method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getAlert/{alertId}")
    public ResponseEntity<Object> getAlert (@PathVariable(value = "alertId") Long alertId, @RequestHeader(name = "screenName", required = false) String screenName,
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
        logger.info("Entered getAlert method.");
        AlertResponse response = new AlertResponse() ;
        try {
            response = alertService.getAlert(alertId, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAlert" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAlert method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/markAlertAsViewed/{alertId}")
    public ResponseEntity<Object> markAlertAsViewed (@PathVariable(value = "alertId") Long alertId, @RequestHeader(name = "screenName", required = false) String screenName,
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
        logger.info("Entered markAlertAsViewed method.");
        AlertResponse response = new AlertResponse() ;
        try {
            response = alertService.markAlertAsViewed(alertId, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " markAlertAsViewed" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute markAlertAsViewed method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getUserReceivedAlerts")
    public ResponseEntity<Object> getUserReceivedAlerts (@RequestHeader(name = "screenName", required = false) String screenName,
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
        logger.info("Entered getUserReceivedAlerts method.");
        List<AlertResponse> response = new ArrayList<>();
        try {
            response = alertService.getUserReceivedAlerts(accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserReceivedAlerts" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserReceivedAlerts method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getUserAlertsToView")
    public ResponseEntity<Object> getUserAlertsToView (@RequestHeader(name = "screenName", required = false) String screenName,
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
        logger.info("Entered getUserAlertsToView method.");
        List<AlertResponse> response = new ArrayList<>();
        try {
            response = alertService.getUserAlertsToView(accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserAlertsToView" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserAlertsToView method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping("/getUserSentAlerts")
    public ResponseEntity<Object> getUserSentAlerts (@RequestHeader(name = "screenName", required = false) String screenName,
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
        logger.info("Entered getUserSentAlerts method.");
        List<AlertResponse> response = new ArrayList<>();
        try {
            response = alertService.getUserSentAlerts(accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserSentAlerts" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserSentAlerts method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

}
