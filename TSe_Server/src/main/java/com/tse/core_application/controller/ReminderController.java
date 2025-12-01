package com.tse.core_application.controller;

import com.tse.core_application.dto.AllReminderResponse;
import com.tse.core_application.dto.ReminderRequest;
import com.tse.core_application.dto.DateRequest;
import com.tse.core_application.dto.ReminderResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.ReminderService;
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
import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/reminder")
public class ReminderController {

    private static final Logger logger = LogManager.getLogger(ReminderController.class.getName());

    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private ReminderService reminderService;

    @PostMapping(path = "/addReminder")
    @Transactional
    public ResponseEntity<Object> addReminder (@RequestBody @Valid ReminderRequest reminderRequest, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addReminder" + '"' + " method ...");
        ReminderResponse response = new ReminderResponse();
        try {
            response = reminderService.addReminder(reminderRequest, timeZone);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute addReminder for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @PostMapping(path = "/updateReminder/{reminderId}")
    @Transactional
    public ResponseEntity<Object> updateReminder (@PathVariable(name = "reminderId") Long reminderId, @RequestBody @Valid ReminderRequest reminderRequest, @RequestHeader(name = "screenName") String screenName,
                                                  @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                  HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateReminder" + '"' + " method ...");
        ReminderResponse response = new ReminderResponse();
        try {
            response = reminderService.updateReminder(reminderId, reminderRequest, timeZone, accountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute updateReminder for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }


    @GetMapping(path = "/getReminderById/{reminderId}")
    public ResponseEntity<Object> getReminderById (@PathVariable(value = "reminderId") Long reminderId, @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getReminderById" + '"' + " method ...");
        ReminderResponse response = new ReminderResponse();
        try {
            response = reminderService.getReminder(reminderId, accountIds, timeZone);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getReminderById for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }



//    @PostMapping(path = "/deleteReminder/{reminderId}")
//    @Transactional
//    public ResponseEntity<Object> deleteReminder (@PathVariable(value = "reminderId") Long reminderId, @RequestHeader(name = "screenName") String screenName,
//                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
//                                               HttpServletRequest request) throws IllegalAccessException {
//
//        long startTime = System.currentTimeMillis();
//        String jwtToken = request.getHeader("Authorization").substring(7);
//        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
//        User foundUser = userService.getUserByUserName(tokenUsername);
//        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
//        ThreadContext.put("userId", foundUser.getUserId().toString());
//        ThreadContext.put("requestOriginatingPage", screenName);
//        logger.info("Entered" + '"' + " deleteReminder" + '"' + " method ...");
//        String response;
//        try {
//            response = reminderService.deleteReminder(reminderId, accountIds);
//        } catch (Exception e) {
//            e.printStackTrace();
//            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
//            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute deleteReminder for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
//            ThreadContext.clearMap();
//            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
//        }
//        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
//    }

    @GetMapping(path = "/getUserAllReminder")
    @Transactional
    public ResponseEntity<Object> getUserAllReminder (@RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserAllReminder" + '"' + " method ...");
        AllReminderResponse response = new AllReminderResponse();
        try {
            response = reminderService.getUserAllReminders(accountIds, timeZone);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserAllReminder for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }


    @PostMapping(path = "/getRemindersForDate")
    public ResponseEntity<Object> getRemindersForDate (@RequestBody @Valid DateRequest dateRequest, @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getRemindersForDate" + '"' + " method ...");
        List<ReminderResponse> response = new ArrayList<>();
        try {
            response = reminderService.getRemindersForDate(dateRequest, accountIds, timeZone);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getRemindersForDate for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }
}
