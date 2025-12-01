package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.FirebaseTokenDTO;
import com.tse.core_application.dto.conversations.ChatPayloadDto;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.FirebaseToken;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.FirebaseTokenService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.FCMNotificationUtil;
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
import java.util.HashMap;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/firebase-token")
public class FirebaseTokenController {

    private static final Logger logger = LogManager.getLogger(FirebaseTokenController.class);

    private User foundUserDbByUsername = null;

    @Autowired
    private FirebaseTokenService firebaseTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private FCMNotificationUtil fcmNotificationUtil;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName, String userId) {
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userId);
        ThreadContext.put("requestOriginatingPage", screenName);

    }

    @PostMapping(path = "/insert-token")
    public ResponseEntity<Object> insertFirebaseToken(@Valid @RequestBody FirebaseTokenDTO firebaseTokenDTO, @RequestHeader(name = "accountIds") String accountIds,
                                                      @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                      HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        Long userId = userService.getUserByUserName(username).getUserId();
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName, userId.toString());
        logger.info("Entered insertFirebaseToken method.");
        try {
            firebaseTokenService.convertTokenTimestampInToServerTimezone(firebaseTokenDTO, timeZone);
            FirebaseToken firebaseTokenInserted = firebaseTokenService.addFirebaseToken(jwtToken, firebaseTokenDTO);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " insertFirebaseToken" + '"' + " method for username = " +
                    username + " because completed successfully. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, true);
        } catch(Exception e) {
            e.printStackTrace();
            if(e instanceof UserDoesNotExistException) {
                throw e;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute insertFirebaseToken method for username = " +
                        username + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw new InternalServerErrorException(e.getMessage());
            }
        }
    }

    @GetMapping(path = "/validate-token/{deviceType}/{deviceId}")
    public ResponseEntity<Object> validateToken(@PathVariable(name = "deviceType") String deviceType, @PathVariable(name = "deviceId") String deviceId,
                                                @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        Long userId = userService.getUserByUserName(username).getUserId();
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName, userId.toString());
        logger.info("Entered validateToken method.");
        try {
            boolean isTokenExists = firebaseTokenService.validateFirebaseToken(userId, deviceType);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " validateToken" + '"' + " method for username = " +
                    username +  " because completed successfully. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, isTokenExists);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute validateToken method for username = " +
                    username + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw new InternalServerErrorException(e.getMessage());
        }
    }

    @GetMapping(path = "/get-token/{deviceType}/{deviceId}")
    public ResponseEntity<Object> getToken(@PathVariable(name = "deviceType") String deviceType, @PathVariable(name = "deviceId") String deviceId,
                                           @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                           @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        Long userId = userService.getUserByUserName(username).getUserId();
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName, userId.toString());
        logger.info("Entered getToken method.");
        try {
            String tokenFoundDb = firebaseTokenService.getFirebaseToken(userId, deviceType);
            if(tokenFoundDb==null){
                logger.info("No token found for userId = " + userId + "deviceType = " + deviceType + " and deviceId = " + deviceId);
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "No token found for given deviceType and deviceId");
            } else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " getToken" + '"' + " method for username = " +
                        username + " because completed successfully. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, tokenFoundDb);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API:  " + "Something went wrong: Not able to execute getToken method for username = " +
                    username + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw new InternalServerErrorException(e.getMessage());
        }
    }

    @PostMapping(path = "/conversation-notification")
    public ResponseEntity<Object> pushConversationNotification(@RequestBody ChatPayloadDto payloadlist,
                                                               @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                               @RequestHeader(name = "Authorization") String authorization, @RequestHeader(name = "timeZone") String timeZone,
                                                               @RequestHeader(name = "userId") String userId,
                                                               HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = authorization.substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName, userId);
        boolean result = fcmNotificationUtil.sendConversationPushNotification(payloadlist.getPayloadList(), timeZone);
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, result ? "Successfully Sent to all" : "Failed to sent Notification!");
    }
}
