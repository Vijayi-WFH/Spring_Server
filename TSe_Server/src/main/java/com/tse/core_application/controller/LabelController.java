package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.label.EntityTypeLabelResponse;
import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.LabelService;
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
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/label")
public class LabelController {
    private static final Logger logger = LogManager.getLogger(LabelController.class.getName());
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private LabelService labelService;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
    }

    @GetMapping(path = "/getLabelForTeam/{teamId}")
    public ResponseEntity<Object> getLabelsByTeamId(@PathVariable Long teamId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        User foundUser = userService.getUserByUserName(jwtUtil.getUsernameFromToken(jwtToken));
        logger.info("Entered" + '"' + " getLabelsByTeamId" + '"' + " method ...");
        List<LabelResponse> labelResponse = new ArrayList<>();
        try {
            labelResponse = labelService.getLabelsByTeamId(teamId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getLabelsByTeamId" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get labels of a user for the username = " + foundUser.getPrimaryEmail() + "for teamId: " + teamId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, labelResponse);
    }

    // expect only one accountId in the header
    @Transactional
    @DeleteMapping(path = "/removeLabelFromTask/{taskId}/{labelId}")
    public ResponseEntity<Object> removeLabelFromTask(@PathVariable Long taskId, @PathVariable Long labelId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        User foundUser = userService.getUserByUserName(jwtUtil.getUsernameFromToken(jwtToken));
        logger.info("Entered" + '"' + " removeLabelFromTask" + '"' + " method ...");

        try {
            boolean isLabelRemoved = labelService.removeLabelFromTask(taskId, labelId, Long.valueOf(accountIds));
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeLabelFromTask" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            if(isLabelRemoved) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Label Removed Successfully From Task");
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.FORBIDDEN, Constants.FormattedResponse.FORBIDDEN, "Remove Label Failed");
            }
        } catch (IllegalArgumentException | ValidationFailedException e) {
            logger.error(request.getRequestURI() + " API: Something went wrong: Not able to delete label for taskId " + taskId + " ,for the user = " + foundUser.getPrimaryEmail() + "for labelId: " + labelId +
                    " , Caught Exception: " + e.getMessage(), e);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.BAD_REQUEST, Constants.FormattedResponse.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error(request.getRequestURI() + " API: Something went wrong: Not able to delete label for taskId " + taskId + " ,for the user = " + foundUser.getPrimaryEmail() + "for labelId: " + labelId +
                    " , Caught Exception: " + e.getMessage(), e);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "Internal Server Error!");

        } finally {
            ThreadContext.clearMap();
        }
    }

    // expect only one accountId in the header
    @Transactional
    @DeleteMapping(path = "/removeLabelFromMeeting/{meetingId}/{labelId}")
    public ResponseEntity<Object> removeLabelFromMeeting(@PathVariable Long meetingId, @PathVariable Long labelId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        User foundUser = userService.getUserByUserName(jwtUtil.getUsernameFromToken(jwtToken));
        logger.info("Entered" + '"' + " removeLabelFromMeeting" + '"' + " method ...");

        try {
            boolean isLabelRemoved = labelService.removeLabelFromMeeting(meetingId, labelId, Long.valueOf(accountIds));
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeLabelFromMeeting" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            if(isLabelRemoved) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Label removed successfully from meeting");
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.FORBIDDEN, Constants.FormattedResponse.FORBIDDEN, "Remove label from meeting failed");
            }
        } catch (IllegalArgumentException | ValidationFailedException e) {
            logger.error(request.getRequestURI() + " API: Something went wrong: Not able to delete label for meetingId " + meetingId + " ,for the user = " + foundUser.getPrimaryEmail() + "for labelId: " + labelId +
                    " , Caught Exception: " + e.getMessage(), e);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.BAD_REQUEST, Constants.FormattedResponse.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error(request.getRequestURI() + " API: Something went wrong: Not able to delete label for meetingId " + meetingId + " ,for the user = " + foundUser.getPrimaryEmail() + "for labelId: " + labelId +
                    " , Caught Exception: " + e.getMessage(), e);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "Internal Server Error!");

        } finally {
            ThreadContext.clearMap();
        }
    }

    // expects only one accountId in the header
    @Transactional
    @DeleteMapping(path = "/removeLabelFromRecurringMeeting/{recurringMeetingId}/{labelId}")
    public ResponseEntity<Object> removeLabelFromRecurringMeeting(@PathVariable Long recurringMeetingId, @PathVariable Long labelId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                         @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        User foundUser = userService.getUserByUserName(jwtUtil.getUsernameFromToken(jwtToken));
        logger.info("Entered" + '"' + " removeLabelFromRecurringMeeting" + '"' + " method ...");

        try {
            boolean isLabelRemoved = labelService.removeLabelFromRecurringMeeting(recurringMeetingId, labelId, Long.valueOf(accountIds));
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " removeLabelFromRecurringMeeting" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            if(isLabelRemoved) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Label removed successfully from the recurring meeting");
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.FORBIDDEN, Constants.FormattedResponse.FORBIDDEN, "Remove label from the recurring meeting failed");
            }
        } catch (IllegalArgumentException | ValidationFailedException e) {
            logger.error(request.getRequestURI() + " API: Something went wrong: Not able to delete label for recurring meetingId " + recurringMeetingId + " ,for the user = " + foundUser.getPrimaryEmail() + "for labelId: " + labelId +
                    " , Caught Exception: " + e.getMessage(), e);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.BAD_REQUEST, Constants.FormattedResponse.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error(request.getRequestURI() + " API: Something went wrong: Not able to delete label for meetingId " + recurringMeetingId + " ,for the user = " + foundUser.getPrimaryEmail() + "for labelId: " + labelId +
                    " , Caught Exception: " + e.getMessage(), e);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "Internal Server Error!");
        } finally {
            ThreadContext.clearMap();
        }
    }

    @GetMapping(path = "/getLabelForEntity/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getLabelsForEntity(@PathVariable Integer entityTypeId, @PathVariable Long entityId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        User foundUser = userService.getUserByUserName(jwtUtil.getUsernameFromToken(jwtToken));
        logger.info("Entered" + '"' + " getLabelsForEntity" + '"' + " method ...");
        List<LabelResponse> labelResponse = new ArrayList<>();
        try {
            labelResponse = labelService.getLabelsForEntity(entityTypeId, entityId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getLabelsForEntity" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get labels of a user for the username = " + foundUser.getPrimaryEmail() + "for entityTypeId: " + entityTypeId + " and entityId: " + entityId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, labelResponse);
    }

    @GetMapping(path = "/getEntityTypeLabels/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getEntityTypeLabels(@PathVariable Integer entityTypeId, @PathVariable Long entityId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        User foundUser = userService.getUserByUserName(jwtUtil.getUsernameFromToken(jwtToken));
        logger.info("Entered" + '"' + " getLabelsForEntity" + '"' + " method ...");
        List<EntityTypeLabelResponse> labelResponse = new ArrayList<>();
        try {
            labelResponse = labelService.getEntityTypeLabels(entityTypeId, entityId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getLabelsForEntity" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get labels of a user for the username = " + foundUser.getPrimaryEmail() + "for entityTypeId: " + entityTypeId + " and entityId: " + entityId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, labelResponse);
    }
}
