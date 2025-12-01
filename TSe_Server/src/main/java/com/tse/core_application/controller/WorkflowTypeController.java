package com.tse.core_application.controller;

import com.tse.core_application.custom.model.WorkflowTypeStatusPriorityResponse;
import com.tse.core_application.custom.model.WorkflowTypeStatusPriorityResponseForEpic;
import com.tse.core_application.model.User;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.service.Impl.WorkflowTypeService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/workflowType")
public class WorkflowTypeController {

    private static final Logger logger = LogManager.getLogger(WorkflowTypeController.class.getName());

    @Autowired
    private WorkflowTypeService workflowTypeService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @GetMapping(path = "/getAllWorkflowsAndPriorities")
    public ResponseEntity<Object> getAllWorkflowsAndPriorities(@RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllWorkflowsAndPriorities" + '"' + " method ...");

        WorkflowTypeStatusPriorityResponse allWorkflowsAndPriorities = null;
        try {
            allWorkflowsAndPriorities = workflowTypeService.getAllWorkflowTypeStatusPriority();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllWorkflowsAndPriorities" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllWorkflowsAndPriorities() for the username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return workflowTypeService.getFormattedResponseOfGetAllWorkflowsAndPriorities(allWorkflowsAndPriorities);
    }

    @GetMapping(path = "/getAllWorkflowsAndPrioritiesForEpic")
    public ResponseEntity<Object> getAllWorkflowsAndPrioritiesForEpic(@RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                               @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllWorkflowsAndPrioritiesForEpic" + '"' + " method ...");

        WorkflowTypeStatusPriorityResponseForEpic allWorkflowsAndPriorities = null;
        try {
            allWorkflowsAndPriorities = workflowTypeService.getAllWorkflowTypeStatusPriorityEpic();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllWorkflowsAndPrioritiesForEpic" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllWorkflowsAndPrioritiesForEpic() for the username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return workflowTypeService.getFormattedResponseOfGetAllWorkflowsAndPrioritiesEpic(allWorkflowsAndPriorities);
    }

}
