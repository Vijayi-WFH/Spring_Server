package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.TaskMaster;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.StatsService;
import com.tse.core_application.service.Impl.TaskService;
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
@RequestMapping(path = "/stats")
public class StatsController {

    private static final Logger logger = LogManager.getLogger(StatsController.class.getName());
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private StatsService statsService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private UserService userService;

    @PostMapping(path = "/getStatsV3")
    public ResponseEntity<Object> getStatsV3(@Valid @RequestBody StatsRequest statsRequest, @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber, @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getStatsV3" + '"' + " method ...");
        Boolean isMyTaskAccountIdsValidated = statsService.validateMyTaskAccountIds(statsRequest, jwtToken);
        if (isMyTaskAccountIdsValidated) {
            Object object = null;
            try {
                if (statsRequest.getSprintId() != null && (statsRequest.getFromDate() != null || statsRequest.getToDate() != null || statsRequest.getNoOfDays() != null)) {
                    throw new ValidationFailedException("Date(s) and/or Date Range can't be chosen with a sprint as the spring itself has a date range.");
                }
                if(statsRequest.getStatName() != null) {
                    List<TaskMaster> taskMasterList = new ArrayList<>();
                    taskMasterList = taskService.getTaskDetailsForStatus(statsRequest, timeZone, accountIds);
                    Integer taskListSize = taskMasterList.size();
                    taskMasterList = taskService.sortTaskMaster(statsRequest, taskMasterList);
                    if (statsRequest.getHasPagination()) {
                        taskMasterList = taskService.getTasksWithPagination(taskMasterList, pageNumber, pageSize);
                    }
                    object = taskService.getTaskByFilterResponse(statsRequest, taskMasterList,pageNumber,pageSize,taskListSize);
                } else {
                    object = statsService.computeStatsForGetStatsV3(statsRequest, timeZone, accountIds);
                }
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getStatsV3" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
            } catch (Exception e) {
                e.printStackTrace();
                if(e instanceof InvalidStatsRequestFilterException) {
                    throw e;
                } else {
                    if(!(e instanceof ExceptionByTask)) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getStatsV3() for the username = " + foundUser.getPrimaryEmail() +
                                " ,      " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                    }
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, object);
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new AuthenticationFailedException());
            logger.error(request.getRequestURI() + " API: " + "AccountId not validated in stats request for my task for the username = " + foundUser.getPrimaryEmail(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new AuthenticationFailedException();
        }
    }


    @PostMapping(path = "/getUserProgress")
    public ResponseEntity<Object> getUserProgress(@RequestBody @Valid UserProgressRequest userProgressRequest, @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber, @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserProgress" + '"' + " method ...");
        UserProgressResponse userProgressResponse = new UserProgressResponse();
        try {
            userProgressResponse = statsService.getUserProgress(accountIds, userProgressRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserProgress" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof InvalidStatsRequestFilterException) {
                throw e;
            } else {
                if (!(e instanceof ExceptionByTask)) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserProgress() for the username = " + foundUser.getPrimaryEmail() +
                            " ,      " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                }
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userProgressResponse);
    }
    @PostMapping(path = "/getTodayFocus")
    public ResponseEntity<Object> getTodayFocus(@RequestBody @Valid TodayFocusRequest todayFocusRequest, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTodayFocus" + '"' + " method ...");
        TodayFocusResponse todayFocusResponse = new TodayFocusResponse();
        try {
            todayFocusResponse = statsService.getTodayFocus(accountIds, todayFocusRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTodayFocus" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof InvalidStatsRequestFilterException) {
                throw e;
            } else {
                if (!(e instanceof ExceptionByTask)) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getTodayFocus() for the username = " + foundUser.getPrimaryEmail() +
                            " ,      " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                }
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, todayFocusResponse);
    }

}
