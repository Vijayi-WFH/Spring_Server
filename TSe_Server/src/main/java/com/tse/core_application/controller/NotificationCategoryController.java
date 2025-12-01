package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.NotificationCategory;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.NotificationCategoryService;
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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/notification/category")
@CrossOrigin(value = "*")
public class NotificationCategoryController {

    private static final Logger logger = LogManager.getLogger(NotificationCategoryController.class.getName());

    @Autowired
    private NotificationCategoryService service;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserService userService;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

    }

    @GetMapping("/getAllCategories")
    public ResponseEntity<Object> getAllCategories(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                                       @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " getAllCategories" + '"' + " method ...");
        List<NotificationCategory> allCategories = new ArrayList<>();
        try {
            allCategories = service.getAllNotificationCategories();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllCategories method" + " Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " getAllCategories" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, allCategories);
    }
}
