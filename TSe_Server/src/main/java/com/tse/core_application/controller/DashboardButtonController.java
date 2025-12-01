package com.tse.core_application.controller;

import com.tse.core_application.custom.model.GetAllUIButtonsResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.DashboardButtonService;
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
@RequestMapping(path = "/dashboardButtons")
public class DashboardButtonController {

    private static final Logger logger = LogManager.getLogger(DashboardButtonController.class.getName());

    @Autowired
    private DashboardButtonService dashboardButtonService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @GetMapping(path = "/getAllUIButtons")
    public ResponseEntity<Object> getAllUIButtons(@RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                  @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllUIButtons" + '"' + " method ...");

        GetAllUIButtonsResponse getAllUIButtonsResponse = null;
        try {
            getAllUIButtonsResponse = dashboardButtonService.getAllButtons();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllUIButtons" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " +  "Something went wrong: Not able to execute getAllUIButtons() for username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return dashboardButtonService.getFormattedResponseOfGetAllUIButtons(getAllUIButtonsResponse);
        } catch(Exception e) {
            if(e instanceof NoDataFoundException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " +  "Something went wrong: Not able to execute getAllUIButtons() for username = " +
                        foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }
}
