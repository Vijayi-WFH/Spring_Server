package com.tse.core_application.controller;


import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.leave.DoctorCertificateMetaData;
import com.tse.core_application.dto.leave.Request.*;
import com.tse.core_application.dto.leave.Response.*;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.LeaveApplicationValidationException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.DataFormatException;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/leave")
public class LeaveController {

    private static final Logger logger = LogManager.getLogger(LeaveController.class.getName());

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private TimeSheetService timeSheetService;


    @PostMapping("/addLeavePolicy")
    @Transactional
    public ResponseEntity<Object> addLeavePolicy(@RequestBody @Valid LeavePolicyRequest leavePolicyRequest,
                                                 @RequestHeader(name = "screenName") String screenName,
                                                 @RequestHeader(name = "timeZone") String timeZone,
                                                 @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request){
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered addLeavePolicy method ...");

            ResponseEntity<String> response = null;
            response= leaveService.addLeavePolicy(leavePolicyRequest, userId, timeZone, accountIds);
            logger.info("Exiting addLeavePolicy method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof HttpClientErrorException.NotAcceptable){
                logger.error("Unable to add leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            }
            else if(e instanceof HttpClientErrorException.Unauthorized){
                logger.error("Unauthorized attempt to add leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            }
            else {
                logger.error("Unable to add leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @GetMapping("/getUserLeavePolicy/{accountId}")
    public ResponseEntity<Object> getUserLeavePolicy(
                                                    @PathVariable Long accountId,
                                                    @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds,
                                                    HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getUserLeavePolicy method ...");

            ResponseEntity<List<LeavePolicyResponse>> response = null;
            response=leaveService.getUserLeavePolicy(accountId, accountIds, timeZone, userId);
            logger.info("Exiting getUserLeavePolicy method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get leave policy assigned to user. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to get leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @GetMapping("/getOrgLeavePolicy/{orgId}")
    public ResponseEntity<Object> getOrgLeavePolicy(
            @PathVariable Long orgId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getOrgLeavePolicy method ...");

            ResponseEntity<List<LeavePolicyResponse>> response = null;
            response =leaveService.getOrgLeavePolicy(orgId, timeZone, accountIds, userId);

            logger.info("Exiting getOrgLeavePolicy method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get leave policies of Organization. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to get leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }


    @PostMapping("/updateLeavePolicy/{leavePolicyId}")
    @Transactional
    public ResponseEntity<Object> updateLeavePolicy(@RequestBody @Valid LeavePolicyRequest leavePolicyRequest,
                                                 @PathVariable Long leavePolicyId,
                                                 @RequestHeader(name = "screenName") String screenName,
                                                 @RequestHeader(name = "timeZone") String timeZone,
                                                 @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request){
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered updateLeavePolicy method ...");

            ResponseEntity<String> response = null;
            response = leaveService.updateLeavePolicy(leavePolicyId, leavePolicyRequest, userId, timeZone, accountIds);
            logger.info("Exiting updateLeavePolicy method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Not acceptable condition. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get leaves in process. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to update leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(0).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/assignLeavePolicyToUser")
    @Transactional
    public ResponseEntity<Object> assignLeavePolicyToUser(@RequestBody @Valid AssignLeavePolicyRequest assignLeavePolicyRequest,
                                                    @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds,
                                                    HttpServletRequest request){
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered assignLeavePolicyToUser method ...");

            ResponseEntity<String> response = null;
            response = leaveService.assignLeavePolicyToUser(assignLeavePolicyRequest, timeZone, accountIds, userId);

            logger.info("Exiting assignLeavePolicyToUser method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to assign leave policy to user. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to update leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/reassignLeavePolicyToUser")
    @Transactional
    public ResponseEntity<Object> reassignLeavePolicyToUser(@RequestBody @Valid AssignLeavePolicyRequest assignLeavePolicyRequest,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request){
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered reassignLeavePolicyToUser method ...");

            ResponseEntity<String> response = null;
            response =leaveService.reassignLeavePolicyToUser(assignLeavePolicyRequest, timeZone, accountIds, userId);

            logger.info("Exiting reassignLeavePolicyToUser method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to reassign leave policy to user. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to reassign leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/createLeave")
    @Transactional
    public ResponseEntity<Object> createLeave(@RequestPart @Valid LeaveApplicationRequest leaveRequest,
                                              @RequestPart (name = "doctorCertificate", required = false) MultipartFile doctorCertificate,
                                              @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone,
                                              @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request) throws DataFormatException, HttpMediaTypeNotAcceptableException, IOException {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered createLeave method ...");

            ResponseEntity<LeaveApplicationResponse> response = null;
            response = leaveService.createLeave(leaveRequest, doctorCertificate, userId, timeZone, accountIds);
            List<HashMap<String, String>> leaveNotificationPayload = notificationService.notifyForCreateOrUpdateLeaveApplication(Objects.requireNonNull(response.getBody()), true, timeZone);
            taskServiceImpl.sendPushNotification(leaveNotificationPayload);
            logger.info("Exiting createLeave method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof DateTimeException) {
                logger.error("Unable to create leave. Caught Exception: " + e, new Throwable(allStackTraces));
                throw new DateTimeException("Date Time is not in correct format.");
            } else if (e instanceof LeaveApplicationValidationException) {
                logger.error("Unable to create leave. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof IllegalStateException) {
                logger.error("Unable to create leave. Caught Exception: " + e, new Throwable(allStackTraces));
                throw e;
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to create leave. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to create leave. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpMediaTypeNotAcceptableException) {
                logger.error("File type not supported. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to create leave. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }


    @PostMapping("/updateLeave")
    @Transactional
    public ResponseEntity<Object> updateLeave(@RequestPart @Valid LeaveApplicationRequest leaveRequest,
                                              @RequestPart (name = "doctorCertificate", required = false) MultipartFile doctorCertificate,
                                              @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone,
                                              @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request){
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered updateLeave method ...");

            ResponseEntity<LeaveApplicationResponse> response = null;
            response =leaveService.updateLeave(leaveRequest, doctorCertificate, userId, timeZone, accountIds);

            List<HashMap<String, String>> leaveNotificationPayload = notificationService.notifyForCreateOrUpdateLeaveApplication(Objects.requireNonNull(response.getBody()), false, timeZone);
            taskServiceImpl.sendPushNotification(leaveNotificationPayload);

            logger.info("Exiting updateLeave method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof DateTimeException) {
                logger.error("Date Time Exception occurred. Unable to update leave. Caught Exception: " + e, new Throwable(allStackTraces));
                throw new DateTimeException("Date Time is not in correct format.");
            }else if (e instanceof LeaveApplicationValidationException) {
                logger.error("Unable to update leave. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to update leave. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Not acceptable condition occurred. Unable to update leave. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpMediaTypeNotAcceptableException) {
                logger.error("File type not supported. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to update leave. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }


    @GetMapping("/getLeaveApplication/{leaveApplicationId}")
    public ResponseEntity<Object> getLeaveApplication(
            @PathVariable Long leaveApplicationId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeaveApplication method ...");

            ResponseEntity<LeaveApplicationResponse> response = null;
            response =leaveService.getLeaveApplication(leaveApplicationId, userId, timeZone, accountIds);
            logger.info("Exiting getLeaveApplication method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get leave application. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.NotFound) {
                logger.error("This leaveApplicationId does not exists. Please check again!. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.NOTFOUND);
            } else {
                logger.error("Unable to get leave application. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getLeaveHistory")
    public ResponseEntity<Object> getLeaveHistory(
            @RequestBody @Valid LeaveHistoryRequest leaveHistoryRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeaveHistory method ...");

            ResponseEntity<List<LeaveApplicationResponse>> response = null;
            response =leaveService.getLeaveHistory(leaveHistoryRequest, userId, timeZone, accountIds);
            logger.info("Exiting getLeaveHistory method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get leave history for user. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get leave history. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to get leave history. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @GetMapping("/getDoctorCertificate/{applicationId}")
    public ResponseEntity<Object> getDoctorCertificate(
            @PathVariable Long applicationId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getDoctorCertificate method ...");

            ResponseEntity<DoctorCertificateMetaData> response = null;
            response = leaveService.getDoctorCertificate(applicationId, userId, timeZone, accountIds);
            logger.info("Exiting getDoctorCertificate method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            if(response.getBody()==null){
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NO_CONTENT, Constants.FormattedResponse.NO_CONTENT,null);
            }
            DoctorCertificateMetaData doctorCertificateMetaData = response.getBody();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(doctorCertificateMetaData.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, doctorCertificateMetaData.getFileName())
                    .body(new ByteArrayResource(doctorCertificateMetaData.getDoctorCertificate()));
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("This applicationId " + applicationId + " does not exists. Please check again! " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to get Doctor Certificate for account Id " + applicationId + " . Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @GetMapping("/applicationStatus/{accountId}")
    public ResponseEntity<Object> applicationStatus(
            @PathVariable Long accountId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered applicationStatus method ...");

            ResponseEntity<List<LeaveApplicationResponse>> response = null;
            response = leaveService.applicationStatus(accountId, userId, timeZone, accountIds);
            logger.info("Exiting applicationStatus method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get application status. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get application status. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to get application status. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getLeavesToProcess")
    public ResponseEntity<Object> getLeavesToProcess(
            @RequestBody LeaveWithFilterRequest leaveWithFilterRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeavesToProcess method ...");

            ResponseEntity<AllLeavesByFilterResponse> response = null;
            response = leaveService.getLeavesToProcess(leaveWithFilterRequest, userId, timeZone, accountIds);
            logger.info("Exiting getLeavesToProcess method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get leaves to process for the user. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get leaves to process. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else if (e instanceof IllegalStateException) {
                throw e;
            } else {
                logger.error("Unable to get leaves to process. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/cancelLeaveApplication")
    @Transactional
    public ResponseEntity<Object> cancelLeaveApplication(
            @RequestBody @Valid CancelLeaveRequest cancelLeaveRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered cancelLeaveApplication method ...");

            ResponseEntity<Object> response = null;
            response = leaveService.cancelLeaveApplication(cancelLeaveRequest, userId, timeZone, accountIds);
            logger.info("Exiting cancelLeaveApplication method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return response;
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("This applicationId " + cancelLeaveRequest.getLeaveApplicationId() + " does not exists. Please check again! " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Forbidden) {
                logger.error("Operation not allowed for applicationId " + cancelLeaveRequest.getLeaveApplicationId() + " . Please check again! " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.FORBIDDEN, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to cancel leave application with application Id " + cancelLeaveRequest.getLeaveApplicationId() + " . Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/changeLeaveStatus")
    @Transactional
    public ResponseEntity<Object> changeLeaveStatus(
            @RequestBody @Valid ChangeLeaveStatusRequest changeLeaveStatusRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered changeLeaveStatus method ...");

            ResponseEntity<LeaveApplicationNotificationRequest> response = null;
            response =leaveService.changeLeaveStatus(changeLeaveStatusRequest, userId, timeZone, accountIds);

            //for notification
            LeaveApplicationNotificationRequest leaveApplicationNotificationRequest = response.getBody();
            if(leaveApplicationNotificationRequest.getSendNotification()) {
                //create meeting invite payload
                List<HashMap<String, String>> leaveNotificationPayload = notificationService.notifyForLeaveApplication(changeLeaveStatusRequest,leaveApplicationNotificationRequest,timeZone);
                //  pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(leaveNotificationPayload);
            }
            if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), com.tse.core_application.model.Constants.NOTIFY_FOR_LEAVE_CANCELLED)) {
                if (Boolean.TRUE.equals(leaveApplicationNotificationRequest.getIsSprintCapacityAdjustment())) {
                    leaveService.updateSprintCapacitiesOnLeaveStatusChange(leaveApplicationNotificationRequest,true, timeZone);
                }
                else {
                    leaveService.updateSprintCapacitiesOnLeaveStatusChange(leaveApplicationNotificationRequest, changeLeaveStatusRequest.getUpdateCapacity(), timeZone);
                }
            }
            else if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), com.tse.core_application.model.Constants.NOTIFY_FOR_APPROVED)) {
                if (Boolean.TRUE.equals(leaveApplicationNotificationRequest.getIsSprintCapacityAdjustment())) {
                    leaveService.updateSprintCapacitiesOnLeaveStatusChange(leaveApplicationNotificationRequest,true, timeZone);
                }
                else {
                    leaveService.updateSprintCapacitiesOnLeaveStatusChange(leaveApplicationNotificationRequest, changeLeaveStatusRequest.getUpdateCapacity(), timeZone);
                }
            }
            logger.info("Exiting changeLeaveStatus method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), "Leave status for application Id "+changeLeaveStatusRequest.getApplicationId()+" has been changed.");
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Account Id " + changeLeaveStatusRequest.getAccountId() + " is not allowed to change application status of applicationId " + changeLeaveStatusRequest.getApplicationId() + " Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Forbidden) {
                logger.error("Operation not allowed for applicationId " + changeLeaveStatusRequest.getApplicationId() + " . Please check again! " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.FORBIDDEN, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to change leave status for applicationId " + changeLeaveStatusRequest.getApplicationId() + ". Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getTeamLeaveHistory")
    public ResponseEntity<Object> getTeamLeaveHistory(
            @RequestBody @Valid TeamLeaveHistoryRequest teamLeaveHistoryRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getTeamLeaveHistory method ...");

            ResponseEntity<List<LeaveApplicationResponse>> response = null;
            response = leaveService.getTeamLeaveHistory(teamLeaveHistoryRequest, userId, timeZone, accountIds);
            logger.info("Exiting getTeamLeaveHistory method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to getTeamLeave history for provided accountId: "+teamLeaveHistoryRequest.getAccountId()+" and teamId: "+teamLeaveHistoryRequest.getTeamId()+" . Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @PostMapping("/getLeavesRemaining")
    public ResponseEntity<Object> getLeavesRemaining(
            @RequestBody LeaveRemainingRequest leaveRemainingHistoryRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeavesRemaining method ...");

            ResponseEntity<List<LeaveRemainingResponse>> response = null;
            response = leaveService.getLeavesRemaining(leaveRemainingHistoryRequest, userId, timeZone, accountIds);
            logger.info("Exiting getLeavesRemaining method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Validation failed for requested accountId: " + leaveRemainingHistoryRequest.getAccountId() + ". Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get leave remaining. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to get leaves remaining. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getApprovedLeaves/{accountId}")
    public ResponseEntity<Object> getApprovedLeaves(
            @PathVariable Long accountId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getApprovedLeaves method ...");

            ResponseEntity<List<LeaveApplicationResponse>> response = null;
            response = leaveService.getApprovedLeaves(accountId, accountIds);
            logger.info("Exiting getApprovedLeaves method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Validation failed for requested accountId: " + accountId + ". Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get approved leaves. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to get approved leaves. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getTeamMembersOnLeave")
    public ResponseEntity<Object> getTeamMembersOnLeave(
            @RequestBody @Valid TeamMemberOnLeaveRequest teamMemberOnLeaveRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getTeamMembersOnLeave method ...");

            ResponseEntity<List<LeaveApplicationResponse>> response = null;
            response =leaveService.getTeamMembersOnLeave(teamMemberOnLeaveRequest, userId, timeZone, accountIds);
            logger.info("Exiting getTeamMembersOnLeave method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get team members on leave for provided teamId: "+teamMemberOnLeaveRequest.getTeamId()+" . Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @PostMapping("/assignLeavePolicyToAllUser")
    @Transactional
    public ResponseEntity<Object> assignLeavePolicyToAllUser(@RequestBody @Valid AssignLeavePolicyInBulkRequest assignLeavePolicyRequest,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request){
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered assignLeavePolicyToAllUser method ...");

            ResponseEntity<String> response = null;
            response = leaveService.assignLeavePolicyToAllUser(assignLeavePolicyRequest, timeZone, accountIds, userId);

            logger.info("Exiting assignLeavePolicyToAllUser method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to assign leave policy to the provided users. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else {
                logger.error("Unable to assign leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getPeopleOnLeave/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getPeopleOnLeave(
            @PathVariable Integer entityTypeId,
            @PathVariable Long entityId,
            @RequestBody @Valid DateRequest todayDate,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getPeopleOnLeave method ...");

            ResponseEntity<List<PeopleOnLeaveResponse>> response = null;
            response =leaveService.getPeopleOnLeave(entityTypeId, entityId, todayDate, userId, timeZone, accountIds);
            logger.info("Exiting getPeopleOnLeave method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get members on leave. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    /**
     * This api gets leave reports for provided user and accepts single accountId in header
     */
    @PostMapping("/getEntityLeaveReport")
    public ResponseEntity<Object> getEntityLeaveReport(
            @RequestBody @Valid EntityLeaveReportRequest entityLeaveReportRequest,
            @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getEntityLeaveReport method ...");

            ResponseEntity<Object> response = null;
            response =leaveService.getEntityLeaveReport(entityLeaveReportRequest, pageNumber, pageSize, timeZone, accountIds);
            logger.info("Exiting getEntityLeaveReport method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave report. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @GetMapping("/getLeaveApproversAndNotifier/{orgId}")
    public ResponseEntity<Object> getLeaveApproversAndNotifier(
            @PathVariable Long orgId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeaveApproversAndNotifier method ...");

            UserListForLeave response = null;
            response =leaveService.getLeaveApproversAndNotifier(orgId, accountIds);
            logger.info("Exiting getLeaveApproversAndNotifier method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave approvers and notifier. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @GetMapping("/getAllUsersPolicyReport/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getAllUsersPolicyReport(
            @PathVariable Integer entityTypeId,
            @PathVariable Long entityId,
            @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException {

        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getAllUsersPolicyReport method ...");

            ResponseEntity<Object> response = null;
            response = leaveService.getUserPolicyReport(entityTypeId, entityId, pageNumber, pageSize, timeZone, accountIds);
            logger.info("Exiting getAllUsersPolicyReport method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get user policy report. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if (e instanceof IllegalAccessException) throw e;
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @PostMapping("/updateUserPolicy")
    public ResponseEntity<Object> updateUserPolicy(
            @RequestBody @Valid UpdateLeavePolicyForUsersRequest usersRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException {

        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered updateUserPolicy method ...");

            ResponseEntity<Object> response = null;
            response = leaveService.updateUserPolicy(usersRequest, timeZone, accountIds);
            logger.info("Exiting updateUserPolicy method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to update user policy. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if (e instanceof IllegalAccessException) throw e;
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @GetMapping("/isUserPartOfActiveSprint/{applicationId}")
    public ResponseEntity<Object> isUserPartOfActiveSprint(
            @PathVariable Long applicationId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException {

        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered isUserPartOfActiveSprint method ...");

            PartOfActiveSprintResponse response = leaveService.isUserPartOfActiveSprint(applicationId);
            logger.info("Exiting isUserPartOfActiveSprint method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to check active sprint for user. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if (e instanceof IllegalAccessException) throw e;
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @GetMapping("/sendAlertForLeaveApproval/{applicationId}")
    public ResponseEntity<Object> sendAlertForLeaveApproval(
            @PathVariable Long applicationId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException {

        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered sendAlertForLeaveApproval method ...");

            String response = leaveService.sendAlertForLeaveApproval(applicationId, Long.valueOf(accountIds), timeZone,accountIds);
            logger.info("Exiting sendAlertForLeaveApproval method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to send alert. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if (e instanceof IllegalStateException) throw e;
            else if (e instanceof EntityNotFoundException) throw e;
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @PostMapping("/getAllUserLeaveApplicationByFilter")
    public ResponseEntity<Object> getAllUserLeaveApplicationByFilter(
            @RequestBody LeaveWithFilterRequest leaveWithFilterRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getAllUserLeaveApplicationByFilter method ...");

            ResponseEntity<AllLeavesByFilterResponse> response = null;
            response =leaveService.getAllUserLeaveApplicationByFilter(leaveWithFilterRequest, userId, timeZone, accountIds);
            logger.info("Exiting getAllUserLeaveApplicationByFilter method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            }  else if (e instanceof IllegalStateException) {
                throw e;
            }  else if (e instanceof EntityNotFoundException) {
                throw e;
            } else {
                logger.error("Unable to get leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getMyLeaves")
    public ResponseEntity<Object> getMyLeaves(
            @RequestBody LeaveWithFilterRequest leaveWithFilterRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getMyLeaves method ...");

            ResponseEntity<AllLeavesByFilterResponse> response = null;
            response =leaveService.getMyLeaves(leaveWithFilterRequest, userId, timeZone, accountIds);
            logger.info("Exiting getMyLeaves method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), response.getBody());
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get user's leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get user's leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            } else {
                logger.error("Unable to get user's leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getUpcomingLeaves")
    public ResponseEntity<Object> getUpcomingLeaves(
            @RequestBody UpcomingLeaveRequest upcomingLeaveRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getUpcomingLeaves method ...");

            UpcomingLeaveResponse response = null;
            response =leaveService.getUpcomingLeaves(upcomingLeaveRequest, userId, timeZone, accountIds);
            logger.info("Exiting getUpcomingLeaves method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get user's upcoming leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                logger.error("Unauthorized attempt to get user's upcoming leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.FORBIDDEN);
            }  else if (e instanceof EntityNotFoundException) {
                throw e;
            } else {
                logger.error("Unable to get user's upcoming leave applications. Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
            }
        }
    }

    @PostMapping("/getEntityMembersAvailability")
    public ResponseEntity<Object> getEntityMembersAvailability(
            @RequestBody @Valid EntityMemberRequest entityMemberRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws IllegalAccessException {

        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getEntityMembersAvailability method ...");

            EntityMembersAvailabilityResponse response = null;
            List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
            response = leaveService.getEntityMembersAvailability(entityMemberRequest.getEntityTypeId(), entityMemberRequest.getEntityId(), entityMemberRequest.getTodaysDate().toLocalDate(), accountIdList);
            logger.info("Exiting getEntityMembersAvailability method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get entity member availability. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else if (e instanceof IllegalAccessException) throw e;
            else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
            else return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage().substring(6).replace("[", "").replace("]",""), Constants.FormattedResponse.SERVER_ERROR);
        }
    }

    @GetMapping("/getLeaveTypes/{orgId}")
    public ResponseEntity<Object> getLeaveTypes(
            @PathVariable Long orgId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeaveTypes method ...");

            List<LeaveTypesResponse> response = null;
            response =leaveService.getLeaveTypeReponse(orgId);
            logger.info("Exiting getLeaveTypes method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave types for user. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }


    @GetMapping("/getLeaveTabsAccess")
    public ResponseEntity<Object> getLeaveTabsAccess(
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
        try{
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            String userId = foundUser.getUserId().toString();
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getLeaveTabsAccess method ...");

            List<LeaveTabsAccessResponse> response = null;
            response =leaveService.getLeaveTabsAccess(accountIds);
            logger.info("Exiting getLeaveTabsAccess method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave tabs access for user. Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/getLeaveAttachment/{leaveApplicationId}")
    public ResponseEntity<Resource> getLeaveAttachment(@PathVariable(name = "leaveApplicationId") Long leaveApplicationId, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getLeaveAttachment" + '"' + " method ...");

        try {
            LeaveAttachmentResponse attachmentResponse = leaveService.getLeaveAttachment(leaveApplicationId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getLeaveAttachment" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentResponse.getDoctorCertificateFileName() + "\"")
                    .body(new ByteArrayResource(attachmentResponse.getDoctorCertificate()));
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get Leave Application for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/getUserLeaveDetails")
    public ResponseEntity<Object> getUserLeaveDetails(
            @RequestBody @Valid LeaveApplicationDetailsRequest leaveApplicationDetailsRequest,
            @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {
            long startTime = System.currentTimeMillis();
            String jwtToken = request.getHeader("Authorization").substring(7);
            String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
            User foundUser = userService.getUserByUserName(tokenUsername);
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
            ThreadContext.put("userId", foundUser.getUserId().toString());
            ThreadContext.put("requestOriginatingPage", screenName);
            logger.info("Entered getUserLeaveDetails method ...");

            GetUsersLeaveDetailsResponse response = leaveService.getLeaveDetails(leaveApplicationDetailsRequest, pageNumber, pageSize, timeZone, accountIds);
            logger.info("Exiting getUserLeaveDetails method ...");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }
}



