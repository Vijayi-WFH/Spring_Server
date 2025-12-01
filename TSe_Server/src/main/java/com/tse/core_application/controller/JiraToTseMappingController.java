package com.tse.core_application.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.InvalidRequestHeaderException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.JiraToTseMappingService;
import com.tse.core_application.service.Impl.OtpService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/jira")
public class JiraToTseMappingController {

    private static final Logger logger = LogManager.getLogger(JiraToTseMappingController.class.getName());

    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private JiraToTseMappingService jiraToTseMappingService;
    @Autowired
    private OtpService otpService;

    @PostMapping ("/getUserFromJiraFile")
    public ResponseEntity<Object> getUserFromJiraFile (@RequestParam(name = "file") MultipartFile jiraFile, @RequestParam(name = "jiraUserFile", required = false) MultipartFile jiraUserFile, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserFromJiraFile" + '"' + " method ...");
        try {
            List<JiraTasks> jiraTaskList = jiraToTseMappingService.parseJiraCsv(jiraFile);
            List<JiraUsers> jiraUsersFileList = new ArrayList<>();
            if (jiraUserFile != null && !jiraUserFile.isEmpty()) {
                jiraUsersFileList = jiraToTseMappingService.parseJiraUserCsv(jiraUserFile);
            }
            List<JiraUsers> jiraUsersList = jiraToTseMappingService.getJiraUsersFromFile(jiraTaskList, jiraUsersFileList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserFromJiraFile" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraUsersList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get user from jira file for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/getJiraTaskIdAndTitle")
    public ResponseEntity<Object> getJiraTaskIdAndTitle (@RequestParam String getJiraTaskIdAndTitleRequestJson, @RequestParam(name = "file") MultipartFile jiraFile, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getJiraTaskIdAndTitle" + '"' + " method ...");
        try {
            GetJiraTaskIdAndTitleRequest jiraTaskIdAndTitleRequest = new ObjectMapper().readValue(getJiraTaskIdAndTitleRequestJson, GetJiraTaskIdAndTitleRequest.class);
            List<JiraTasks> jiraTaskList = jiraToTseMappingService.parseJiraCsv(jiraFile);
            List<JiraTaskToCreate> jiraTaskToCreateList = jiraToTseMappingService.getAllJiraTaskWithIdAndTitle(jiraTaskIdAndTitleRequest, jiraTaskList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getJiraTaskIdAndTitle" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraTaskToCreateList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get Jira task id and title for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping (value = "/addJiraTask")
    public ResponseEntity<Object> addJiraTask(
            @RequestParam String addJiraTaskRequestJson,
            @RequestParam(value = "file", required = true) MultipartFile jiraFile,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addJiraTask" + '"' + " method ...");
        try {
            AddJiraTaskRequest addJiraTaskRequest = new ObjectMapper().readValue(addJiraTaskRequestJson, AddJiraTaskRequest.class);
            jiraToTseMappingService.validateUserIsAdminOrNot(accountIds, addJiraTaskRequest.getTeamId());
            List<JiraTasks> jiraTaskList = jiraToTseMappingService.parseJiraCsv(jiraFile);
            AddJiraTaskResponse response = jiraToTseMappingService.addAllJiraTask(addJiraTaskRequest, jiraTaskList, accountIds, timeZone);
            System.out.println("No of task in success list" + response.getSuccessList().size());
            System.out.println("No of task in failure list" + response.getFailureList().size());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addJiraTask" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add jira task for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/getJiraCustomWorkflowStatus")
    public ResponseEntity<Object> getJiraCustomWorkflowStatus (@RequestParam(name = "file") MultipartFile jiraFile, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getJiraCustomWorkflowStatus" + '"' + " method ...");
        try {
            List<JiraTasks> jiraTaskList = jiraToTseMappingService.parseJiraCsv(jiraFile);
            WorkflowTypeStatusOfOurAppAndJira jiraCustomWorkflowStatus = jiraToTseMappingService.getJiraCustomWorkflowStatus(jiraTaskList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getJiraCustomWorkflowStatus" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraCustomWorkflowStatus);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get jira custom status file for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/getCustomJiraIssueType")
    public ResponseEntity<Object> getCustomJiraIssueType (@RequestParam(name = "file") MultipartFile jiraFile, @RequestHeader(name = "screenName") String screenName,
                                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                               HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getCustomJiraIssueType" + '"' + " method ...");
        try {
            List<JiraTasks> jiraTaskList = jiraToTseMappingService.parseJiraCsv(jiraFile);
            JiraIssueTypeResponse jiraIssueTypeResponse = jiraToTseMappingService.getCustomJiraIssueType(jiraTaskList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getCustomJiraIssueType" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraIssueTypeResponse);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get jira custom issue type for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/importUserFromJiraUserFile")
    public ResponseEntity<Object> importUserFromJiraUserFile (@RequestParam(name = "jiraUserFile") MultipartFile jiraUserFile, @RequestParam(name = "teamId") Long teamId, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " importUserFromJiraUserFile" + '"' + " method ...");
        try {
            jiraToTseMappingService.validateUserIsAdminOrNot(accountIds, teamId);
            List<JiraUsers> jirFileaUserList = jiraToTseMappingService.parseJiraUserCsv(jiraUserFile);
            importedJiraUser importedJiraUser = jiraToTseMappingService.importUserFromJiraFile (jirFileaUserList, teamId, foundUser, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " importUserFromJiraUserFile" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, importedJiraUser);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get user from jira user file for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/verifyJiraUserAndSendOtp")
    @Transactional
    public ResponseEntity<Object> verifyJiraUserAndSendOtp (@RequestBody @Valid VerifyJiraUserRequest verifyJiraUserRequest, @RequestHeader(name = "screenName") String screenName,
                                                            @RequestHeader(name = "timeZone") String timeZone) throws Exception {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("requestOriginatingPage", screenName);
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);
        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("verifyJiraUserAndSendOtp API: Headers are not validated for username = " + verifyJiraUserRequest.getPrimaryEmail() + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }
        logger.info("Entered" + '"' + " verifyJiraUserAndSendOtp" + '"' + " method ...");
        try {
            String response = jiraToTseMappingService.verifyAndSendOtp(verifyJiraUserRequest, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " verifyJiraUserAndSendOtp" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error( "Something went wrong: Not able to verify jira user and sent otp for username = " + verifyJiraUserRequest.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/verifyOtpAndActivateUser")
    @Transactional
    public ResponseEntity<Object> verifyOtpAndActivateUser (@RequestBody VerifyJiraUserRequest verifyJiraUserRequest, @RequestHeader(name = "screenName") String screenName,
                                                            @RequestHeader(name = "timeZone") String timeZone) throws Exception {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("requestOriginatingPage", screenName);
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);
        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("verifyOtpAndActivateUser API: Headers are not validated for username = " + verifyJiraUserRequest.getPrimaryEmail() + ": timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }
        logger.info("Entered" + '"' + " verifyAndActivateUserAccount" + '"' + " method ...");
        try {
            jiraToTseMappingService.verifyAndActivateUserAccount(verifyJiraUserRequest, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " verifyAndActivateUserAccount" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "User is verified and account is successfully activated");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong: Not able to verify otp and activate user for username = " + verifyJiraUserRequest.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/token/getJiraProjects")
    public ResponseEntity<Object> getJiraProjects (@RequestBody @Valid JiraConnectionRequest getJiraProjectsRequest, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getJiraProjects" + '"' + " method ...");
        try {
            List<JiraProjectResponse> jiraProjectResponseList = jiraToTseMappingService.fetchAllJiraProjects(getJiraProjectsRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getJiraProjects" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraProjectResponseList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get Jira project for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/token/getUserFromJira")
    public ResponseEntity<Object> getUserFromJira (@RequestBody @Valid GetJiraUsersDetailsRequest getJiraUsersDetailsRequest, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserFromJira" + '"' + " method ...");
        try {
            List<JiraUserResponse> jiraProjectResponseList = jiraToTseMappingService.getAllRealUsersFromJira(getJiraUsersDetailsRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserFromJira" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraProjectResponseList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get user from jira for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/token/getCustomJiraIssueType")
    public ResponseEntity<Object> getCustomJiraIssueTypeUsingJiraToken (@RequestBody @Valid GetJiraUsersDetailsRequest getJiraUsersDetailsRequest, @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getCustomJiraIssueTypeUsingJiraToken" + '"' + " method ...");
        try {
            JiraIssueTypeResponse jiraIssueTypeResponse = jiraToTseMappingService.getCustomJiraIssueTypesFromJira (getJiraUsersDetailsRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getCustomJiraIssueTypeUsingJiraToken" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraIssueTypeResponse);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get jira custom issue type using jira token for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/token/getJiraCustomWorkflowStatus")
    public ResponseEntity<Object> getJiraCustomWorkflowStatusUsingJiraToken (@RequestBody @Valid GetJiraUsersDetailsRequest getJiraUsersDetailsRequest, @RequestHeader(name = "screenName") String screenName,
                                                                        @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                        HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getJiraCustomWorkflowStatusUsingJiraToken" + '"' + " method ...");
        try {
            WorkflowTypeStatusOfOurAppAndJira jiraIssueTypeResponse = jiraToTseMappingService.getJiraCustomWorkflowStatusFromToken (getJiraUsersDetailsRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getJiraCustomWorkflowStatusUsingJiraToken" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraIssueTypeResponse);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get jira custom issue type using jira token for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/token/importUserFromJira")
    public ResponseEntity<Object> importUserFromJiraToken(
            @RequestBody @Valid ImportJiraUsersTokenRequest importJiraUsersTokenRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws Exception {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " importUserFromJiraToken" + '"' + " method ...");

        try {
            jiraToTseMappingService.validateUserIsAdminOrNot(accountIds, importJiraUsersTokenRequest.getTeamId());

            List<JiraUsers> jiraUserList = jiraToTseMappingService.fetchAllUsersFromJiraProjectUsingToken(importJiraUsersTokenRequest.getGetJiraUsersDetailsRequest());

            importedJiraUser importedJiraUser = jiraToTseMappingService.importUserFromJiraFile(jiraUserList, importJiraUsersTokenRequest.getTeamId(), foundUser, timeZone);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " importUserFromJiraToken" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, importedJiraUser);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able import user using jira token for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/token/importJiraUsersManually")
    public ResponseEntity<Object> importJiraUsersManually(
            @RequestBody @Valid ImportJiraUsersManualRequest importJiraUsersManualRequest,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) throws Exception {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " importJiraUsersManually" + '"' + " method ...");

        try {
            jiraToTseMappingService.validateUserIsAdminOrNot(accountIds, importJiraUsersManualRequest.getTeamId());

            importedJiraUser importedJiraUser = jiraToTseMappingService.importUserFromJiraFile(importJiraUsersManualRequest.getJiraUsers(), importJiraUsersManualRequest.getTeamId(), foundUser, timeZone);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " importJiraUsersManually" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, importedJiraUser);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able import user using jira user list for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/token/getJiraTaskIdAndTitle")
    public ResponseEntity<Object> getJiraTaskIdAndTitleUsingJiraToken (@RequestBody @Valid GetJiraTaskIdAndTitleUsingTokenRequest getJiraTaskIdAndTitleUsingTokenRequest, @RequestHeader(name = "screenName") String screenName,
                                                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                             HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getJiraTaskIdAndTitleUsingJiraToken" + '"' + " method ...");
        try {
            List<JiraTaskToCreate> jiraIssueTypeResponse = jiraToTseMappingService.getAllJiraTaskWithIdAndTitle (getJiraTaskIdAndTitleUsingTokenRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getJiraTaskIdAndTitleUsingJiraToken" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, jiraIssueTypeResponse);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get jira issue id an title using jira token for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping ("/token/importJiraTask")
    public ResponseEntity<Object> importJiraTaskFromToken (@RequestBody @Valid AddJiraTaskRequest addJiraTaskRequest, @RequestHeader(name = "screenName") String screenName,
                                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                       HttpServletRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " importJiraTaskFromToken" + '"' + " method ...");
        try {
            if (addJiraTaskRequest.getJiraToken() == null || addJiraTaskRequest.getJiraToken().isEmpty() || addJiraTaskRequest.getEmailToFetchAttachment() == null || addJiraTaskRequest.getEmailToFetchAttachment().isEmpty() ||
                    addJiraTaskRequest.getProjectId() == null || addJiraTaskRequest.getProjectId().isEmpty() || addJiraTaskRequest.getSiteUrl() == null || addJiraTaskRequest.getSiteUrl().isEmpty()) {
                throw new ValidationFailedException("Any of the following field should not be missed or empty : jira siteUrl, jira email, jira token, jira project id");
            }
            List<JiraTasks> jiraTaskList = jiraToTseMappingService.fetchAllJiraTasksUsingToken(addJiraTaskRequest.getSiteUrl(), addJiraTaskRequest.getEmailToFetchAttachment(), addJiraTaskRequest.getJiraToken(), addJiraTaskRequest.getProjectId());
            AddJiraTaskResponse response = jiraToTseMappingService.addAllJiraTask(addJiraTaskRequest, jiraTaskList, accountIds, timeZone);

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " importJiraTaskFromToken" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to import jira task using jira token for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
