package com.tse.core_application.controller;

import com.tse.core_application.dto.SprintHistoryResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.SprintHistoryService;
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

@CrossOrigin (value = "*")
@RestController
@RequestMapping (path = "/sprint-history")
public class SprintHistoryController {
    private static final Logger logger = LogManager.getLogger(SprintHistoryController.class.getName());

    @Autowired
    JwtRequestFilter jwtRequestFilter;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    RequestHeaderHandler requestHeaderHandler;

    @Autowired
    UserService userService;

    @Autowired
    SprintHistoryService sprintHistoryService;

    @GetMapping(path = "/getSprintHistory")
    public ResponseEntity<Object> getSprintHistory(@RequestParam(name = "sprintId") Long sprintId, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getSprintHistory" + '"' + " method ...");

        try {
            List<SprintHistoryResponse> sprintHistoryResponseList = sprintHistoryService.getSprintHistory(sprintId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getSprintHistory" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, sprintHistoryResponseList);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get Sprint History for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
