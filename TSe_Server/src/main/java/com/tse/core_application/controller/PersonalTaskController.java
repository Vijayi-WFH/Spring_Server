package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.DownloadAttachmentResponse;
import com.tse.core_application.dto.personal_task.*;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.PersonalTaskService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.FileUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
@CrossOrigin(value = "*")
@RequestMapping(path = "/personal-task")
public class PersonalTaskController {

    private static final Logger logger = LogManager.getLogger(PersonalTaskController.class.getName());

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private PersonalTaskService personalTaskService;
    @Autowired
    private FileUtils fileUtils;

//     This is used to add a new task. It expects single accountId of the user in Personal Org.
    @Transactional
    @PostMapping(path = "/addTask")
    public ResponseEntity<Object> addPersonalTask(@Valid @RequestBody PersonalTaskAddRequest personalTaskRequest,
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
        logger.info("Entered" + '"' + " addPersonalTask" + '"' + " method ...");
        PersonalTaskResponse response;
        try {
            personalTaskService.validateAndNormalizeAddPersonalTaskRequest(personalTaskRequest, Long.parseLong(accountIds));
            response = personalTaskService.savePersonalTask(personalTaskRequest, Long.parseLong(accountIds), timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "addPersonalTask" + '"' + " method because successfully completed ...");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a personal task for" +
                    " username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }


    // This is used to update a new task. It expects single accountId of the user in Personal Org.
    @Transactional
    @PostMapping(path = "/updateTask")
    public ResponseEntity<Object> updatePersonalTask(@Valid @RequestBody PersonalTaskUpdateRequest personalTaskUpdateRequest,
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
        logger.info("Entered" + '"' + " updatePersonalTask" + '"' + " method ...");
        PersonalTaskResponse response;
        try {
            personalTaskService.validateAndNormalizeUpdatePersonalTaskRequest(personalTaskUpdateRequest, timeZone, Long.parseLong(accountIds));
            response = personalTaskService.updatePersonalTask(personalTaskUpdateRequest, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "updatePersonalTask" + '"' + " method because successfully completed ...");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update the personal task for" +
                    " username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }


    // This is used to fetch task details. It expects single accountId of the user in Personal Org.
    @GetMapping(path = "/getPersonalTask/{personalTaskIdentifier}")
    public ResponseEntity<Object> getPersonalTask(@PathVariable(name = "personalTaskIdentifier") Long personalTaskIdentifier,
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
        logger.info("Entered" + '"' + " getPersonalTask" + '"' + " method ...");
        PersonalTaskResponse response;
        try {
            response = personalTaskService.getPersonalTask(personalTaskIdentifier, timeZone, Long.parseLong(accountIds));
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "getPersonalTask" + '"' + " method because successfully completed ...");
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the personal task for" +
                    " username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }


    // This is used to upload a list of attachments to a task. It expects single accountId of the user in Personal Org in header.
    @Transactional
    @PostMapping(path = "/upload-attachments")
    public ResponseEntity<Object> uploadAttachments(@RequestParam(name = "files") List<MultipartFile> files,
                                                    @RequestParam(name = "personalTaskId") Long personalTaskId,
                                                    @RequestHeader(name = "accountIds") String accountIds,
                                                    @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone,
                                                    HttpServletRequest httpServletRequest) throws IOException {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String foundUserDbUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(foundUserDbUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered uploadAttachments method.");

        HashMap<String, Object> uploadedAttachments = null;
        try {
            Boolean isFileSizeValid = fileUtils.validateFileSizeForOrg(Long.parseLong(accountIds), files);
            if (!isFileSizeValid) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.PAYLOAD_TOO_LARGE, Constants.FormattedResponse.FORBIDDEN, "File exceeds the allowed size for this organization.");
            }
            personalTaskService.validateAccess(personalTaskId, Long.parseLong(accountIds));
            uploadedAttachments = personalTaskService.saveFiles(files, personalTaskId, Long.parseLong(accountIds));
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute uploadAttachments method for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw e;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " uploadAttachments" + '"' + " method for username = " + foundUser.getPrimaryEmail() + " because completed successfully. ");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, uploadedAttachments);
    }


    // This is used to download an attachment using the attachmentId. It expects single accountId of the user in Personal Org in header
    @Transactional
    @GetMapping(path = "/download-attachment")
    public ResponseEntity<Resource> downloadAttachment(@RequestParam(name = "personalTaskId") Long personalTaskId,
                                                       @RequestParam(name = "fileName") String fileName,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       HttpServletRequest httpServletRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String foundUserDbUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(foundUserDbUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered downloadAttachment method.");

        DownloadAttachmentResponse downloadAttachmentResponse = null;
        try {
            personalTaskService.validateAccess(personalTaskId, Long.parseLong(accountIds));
            downloadAttachmentResponse = personalTaskService.getTaskAttachmentByTaskIDAndFileNameAndFileStatus(personalTaskId, fileName, Constants.FileAttachmentStatus.A);
        } catch (Exception exception) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute downloadAttachment method for username = " + foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw exception;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " downloadAttachment" + '"' + " method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
        ThreadContext.clearMap();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadAttachmentResponse.getFileName() + "\"")
                .body(new ByteArrayResource(downloadAttachmentResponse.getFileContent()));

    }


    // This is to delete attachment from the personal task. It expects single accountId of the user in Personal Org in header
    @Transactional
    @DeleteMapping(path = "/delete-attachments")
    public ResponseEntity<Object> deleteAttachments(@RequestParam(name = "personalTaskId") Long personalTaskId,
                                                    @RequestParam(name = "optionIndicator") String optionIndicator,
                                                    @RequestParam(name = "fileName") String fileName,
                                                    @RequestHeader(name = "accountIds") String accountIds,
                                                    @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone,
                                                    HttpServletRequest httpServletRequest) throws IOException {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String foundUserDbUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(foundUserDbUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered deleteAttachments method.");

        String deletedAttachments = null;
        try {
            personalTaskService.validateAccess(personalTaskId, Long.parseLong(accountIds));
            deletedAttachments = personalTaskService.deleteAttachment(personalTaskId, fileName, optionIndicator, Long.parseLong(accountIds));
        } catch (Exception exception) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute deleteAttachments method for username = " + foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw exception;
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " deleteAttachments" + '"' + " method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, deletedAttachments);
    }

    // method creates a duplicate task for the given task number. It expects single accountId of the user in Personal Org in header
    @GetMapping(path = "/createDuplicatePersonalTask/{personalTaskId}")
    public ResponseEntity<Object> createDuplicateTask(@PathVariable("personalTaskId") Long personalTaskId,
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
        logger.info("Entered" + '"' + " createDuplicatePersonalTask" + '"' + " method ...");

        DuplicatePersonalTaskResponse response = null;
        try {
            personalTaskService.validateAccess(personalTaskId, Long.parseLong(accountIds));
            response = personalTaskService.createDuplicatePersonalTask(personalTaskId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " createDuplicatePersonalTask" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create duplicate task for username = "
                    + foundUser.getPrimaryEmail() + " ,    " + "personalTaskId = " + personalTaskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    // method to create recurring tasks based on the selected dates. It expects single accountId of the user in Personal Org in header
    @Transactional
    @PostMapping(path = "/createPersonalRecurringTasks")
    public ResponseEntity<Object> createPersonalRecurringTasks(@Valid @RequestBody RecurrencePersonalTaskDTO recurrenceScheduleDTO,
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
        logger.info("Entered" + '"' + "createPersonalRecurringTasks " + '"' + " method ...");
        try {
            String message = personalTaskService.createPersonalRecurringTasks(recurrenceScheduleDTO, Long.parseLong(accountIds), timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "createPersonalRecurringTasks" + '"' + " method because method successfully completed");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, message);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create personal recurring tasks for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }


    // this method is used to set the workflow task status to delete for a given personal task.
    // Expects a single accountId of the user deleting the task
    @Transactional
    @DeleteMapping(path = "/deletePersonalTask/{taskId}")
    public ResponseEntity<Object> deletePersonalTask(@PathVariable Long taskId,
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
        logger.info("Entering" + '"' + " deletePersonalTask" + '"' + " method ...");

        try {
            personalTaskService.deleteTaskByTaskId(taskId, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exiting" + " " + '"' + " deletePersonalTask" + '"' + " method because task with" + "taskId" + taskId + " has been deleted successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Task Deleted Successfully!");

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof TaskNotFoundException) {
                logger.error(request.getRequestURI() + " API: " + "Task not found for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + taskId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, com.tse.core_application.constants.Constants.FormattedResponse.NOTFOUND, e.getMessage());
            } else if (e instanceof DeleteTaskException) {
                logger.error(request.getRequestURI() + " API: " + "Not allowed to delete task for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + taskId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, com.tse.core_application.constants.Constants.FormattedResponse.VALIDATION_ERROR, e.getMessage());
            } else {
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete a task for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + taskId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }


}
