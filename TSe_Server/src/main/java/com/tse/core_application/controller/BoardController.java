package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.board_view.CurrentlyScheduledSortingFieldsRequest;
import com.tse.core_application.dto.board_view.UpdateBoardRequest;
import com.tse.core_application.dto.board_view.ViewBoardRequest;
import com.tse.core_application.dto.board_view.BoardResponse;
import com.tse.core_application.exception.BoardViewErrorException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.BoardService;
import com.tse.core_application.service.Impl.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.tse.core_application.utils.JWTUtil;


import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/board")
public class BoardController {

    private static final Logger logger = LogManager.getLogger(BoardController.class.getName());

    @Autowired
    private BoardService boardService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // expects single accountId in the header
    @PostMapping(path="/view")
    public  ResponseEntity<Object> getBoardTasks(@Valid @RequestBody ViewBoardRequest viewBoardRequest, @RequestHeader(name = "screenName") String screenName,
                                                 @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " board-view" + '"' + " method ...");

        List<BoardResponse> boardResponseList = new ArrayList<>();
        try {
            boardResponseList = boardService.getBoardTasks(viewBoardRequest, Long.parseLong(accountIds), timeZone);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " board-view" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e){
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "board/view" + "Something went wrong: Not able to get board view tasks " + e.getMessage() + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new BoardViewErrorException(e.getMessage());
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, boardResponseList);
    }

    // expects single accountId in header of the user updating the task
    @PostMapping(path="/update")
    @Transactional
    public  ResponseEntity<Object> updateBoardTasks(@Valid @RequestBody UpdateBoardRequest updateBoardRequest, @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " board-update" + '"' + " method ...");

        List<BoardResponse> boardResponseList = new ArrayList<>();
        try {
            boardResponseList = boardService.updateBoardTasks(updateBoardRequest, Long.parseLong(accountIds), timeZone,accountIds);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " board-update" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e){
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "board/update" + "Something went wrong: Not able to update board view tasks " + e.getMessage() + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new BoardViewErrorException(e.getMessage());
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, boardResponseList);

    }

    // expects user's all accountIds in header
    @GetMapping(path="/active-tasks")
    public  ResponseEntity<Object> getCurentlyScheduledTasks(@RequestBody CurrentlyScheduledSortingFieldsRequest currentlyScheduledSortingFieldsRequest, @RequestHeader(name = "screenName") String screenName,
                                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getCurrentlyScheduledTasks" + '"' + " method ...");
        List<BoardResponse> boardResponseList = new ArrayList<>();
        List<Long> accountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        try {
            boardResponseList = boardService.getCurrentlyScheduledTasks(accountIdsList, timeZone, currentlyScheduledSortingFieldsRequest.getSortingPriorityList());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getCurrentlyScheduledTasks" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e){
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "board/active-tasks" + "Something went wrong: Not able to get currently scheduled tasks " + e.getMessage() + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new BoardViewErrorException(e.getMessage());
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, boardResponseList);
    }

    }
