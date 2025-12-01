package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.TaskHistoryMappingKeyColumnsDesc;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.TaskHistoryColumnsMappingService;
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
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/task-history-mapping")
public class TaskHistoryColumnsMappingController {

    private static final Logger logger = LogManager.getLogger(TaskHistoryColumnsMappingController.class);

    private User foundUserDbByUsername = null;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private TaskHistoryColumnsMappingService taskHistoryColumnsMappingService;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

    }

    @GetMapping(path = "/getAllFieldsAndMappings")
    public ResponseEntity<Object> getAllTaskHistoryFieldsAndMappings(@RequestHeader(name = "accountIds") String accountIds,
                                                                     @RequestHeader(name = "screenName") String screenName,
                                                                     @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest httpServletRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered getAllTaskHistoryFieldsAndMappings method.");

        try {
            List<TaskHistoryMappingKeyColumnsDesc> taskHistoryMappingKeyColumnsDesc = taskHistoryColumnsMappingService.getAllActiveTaskHistoryMappingFieldsAndKeys();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " getAllTaskHistoryFieldsAndMappings" + '"' + " method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, taskHistoryMappingKeyColumnsDesc);
        } catch (Exception exception) {
            exception.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllTaskHistoryFieldsAndMappings method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (exception.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw exception;
        }
    }

}
