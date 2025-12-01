package com.tse.core_application.controller;

import com.tse.core_application.dto.UserPreferenceDTO;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.model.UserPreference;
import com.tse.core_application.service.Impl.UserPreferenceService;
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
import java.lang.reflect.InvocationTargetException;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/userPreference")
public class UserPreferenceController {

    private static final Logger logger = LogManager.getLogger(UserPreferenceController.class.getName());

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private UserService userService;

    @Autowired
    private UserPreferenceService userPreferenceService;

    @PostMapping(path = "/addUserPreference")
    public ResponseEntity<Object> addUserPreference(@RequestBody UserPreference userPreference, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws InvocationTargetException, IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addUserPreference" + '"' + " method ...");
        UserPreference savedUserPreference = null;
        try {
            savedUserPreference = userPreferenceService.addUserPreference(userPreference, foundUser.getUserId(), timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addUserPreference" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add user preference for userId = " + userPreference.getUserId() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, savedUserPreference);
    }

    @GetMapping(path = "/getUserPreference/{userId}")
    public ResponseEntity<Object> getUserPreference(@PathVariable Long userId, @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                    HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addUserPreference" + '"' + " method ...");
        UserPreferenceDTO userPreferenceDTO = new UserPreferenceDTO();
        try {
            userPreferenceDTO = userPreferenceService.getUserPreference(userId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserPreference" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get user preference for userId = " + userId + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, userPreferenceDTO);
    }

}
