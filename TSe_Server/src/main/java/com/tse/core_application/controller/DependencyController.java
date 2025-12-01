package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.DependencyService;
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

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/dependency")
public class DependencyController {

    private static final Logger logger = LogManager.getLogger(DependencyController.class.getName());
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private DependencyService dependencyService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
    }

    // pass the dependencyId that has to be removed and the taskId from which update action is being performed -- validate that the account has the permission to update this task
    // expects single accountId in the header of the user who is deleting the dependency
    @Transactional
    @DeleteMapping(path = "/removeDependency/{dependencyId}/{taskId}")
    public ResponseEntity<Object> removeDependency(@PathVariable Long dependencyId, @PathVariable Long taskId, @RequestHeader(name = "timeZone") String timeZone,
                                                         @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " removeDependency" + '"' + " method ...");
        try {
            dependencyService.removeDependency(dependencyId, taskId, Long.valueOf(accountIds));
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeDependency" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to remove dependency Id = " + dependencyId + " ,for taskId = " + taskId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Dependency Removed Successfully");
    }

}
