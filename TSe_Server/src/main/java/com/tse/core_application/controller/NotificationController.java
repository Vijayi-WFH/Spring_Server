package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.NotificationService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/notification")
public class NotificationController {
    private static final Logger logger = LogManager.getLogger(NotificationController.class.getName());

    @Autowired
    NotificationService notificationService;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @PostMapping("/getAllNotifications")
    public ResponseEntity<Object> getNotification(@Valid @RequestBody NotificationRequest notificationRequest,
                                                  @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request){
        try {
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            if (!notificationRequest.getAccountIdsList().isEmpty() && notificationService.checkAccountIds(notificationRequest.getAccountIdsList(), jwtToken)) {
                logger.info("Entered getNotification method ");
                List<NotificationResponse> notificationList = notificationService.getNotificationForAUser(notificationRequest, timeZone);
                logger.info("Exiting getNotification method ");
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, notificationList);
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, Constants.FormattedResponse.VALIDATION_ERROR, "Account ids are invalid");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to getAllNotification. Caught Exception: " + e, new Throwable(allStackTraces));
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "Please try again!");
        }
    }

    @Transactional
    @PostMapping("/markNotificationRead/{notificationId}")
    public ResponseEntity<Object> markNotificationRead(@Valid @PathVariable Long notificationId,
            @RequestParam Long accountId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered markNotificationRead method ");
        if(!notificationService.checkAccountIds(List.of(accountId), jwtToken)){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, Constants.FormattedResponse.VALIDATION_ERROR, "Account id is invalid");
        }
        boolean checked= notificationService.markNotificationCheckedForAUser(notificationId,accountId);
        logger.info("Exiting markNotificationRead method ");
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        ThreadContext.clearMap();
        if(checked){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, true);
        }
        else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, false);
        }
    }

    @Transactional
    @PostMapping("/markAllNotificationRead")
    public ResponseEntity<Object> markAllNotificationRead(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered markAllNotificationRead method ");
        List<Long> accountIdList= new ArrayList<>();
        List.of(accountIds.split(",")).forEach(account -> accountIdList.add(Long.valueOf(account)));
        if(!notificationService.checkAccountIds(accountIdList, jwtToken)){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, Constants.FormattedResponse.VALIDATION_ERROR, "Account id is invalid");
        }
        boolean checked= notificationService.markAllNotificationCheckedForAUser(accountIdList);
        logger.info("Exiting markAllNotificationRead method ");
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        ThreadContext.clearMap();
        if(checked){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, true);
        }
        else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, false);
        }
    }

    @Transactional
    @PostMapping("/clearNotification")
    public ResponseEntity<Object> clearNotification(@Valid @RequestBody ClearNotificationRequest clearNotificationRequest,
                                                    @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                    HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered clearNotification method ");
        if(!notificationService.checkAccountIds(clearNotificationRequest.getAccountIds(), jwtToken)){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, Constants.FormattedResponse.VALIDATION_ERROR, "Account id is invalid");
        }
        boolean cleared;
        if(clearNotificationRequest.getNotificationIdList()!=null && !clearNotificationRequest.getNotificationIdList().isEmpty()){
            cleared= notificationService.clearNotificationForAUser(clearNotificationRequest.getNotificationIdList(),clearNotificationRequest.getAccountIds());
        }
        else{
            cleared= notificationService.clearAllNotificationForAUser(clearNotificationRequest.getAccountIds());
        }

        logger.info("Exiting clearNotification method ");
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        ThreadContext.clearMap();
        if(cleared){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, true);
        }
        else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, false);
        }
    }

    @PostMapping("/getAllTaskUpdationDetails")
    public ResponseEntity<Object> getAllTaskUpdationDetails (@Valid @RequestBody TaskUpdationDetailsRequest taskUpdationDetailsRequest,
                                                              @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllTaskUpdationDetails" + '"' + " method ...");
        try {
            Set<TaskUpdationDetailsResponseDto> response=notificationService.getAllTaskUpdationDetails(taskUpdationDetailsRequest,accountIds,timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllTaskUpdationDetails" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to getAllTaskUpdationDetails = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }


}
