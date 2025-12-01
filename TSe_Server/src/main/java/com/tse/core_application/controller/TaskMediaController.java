package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.model.User;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.TaskMedia;
import com.tse.core_application.repository.TaskRepository;
import com.tse.core_application.service.Impl.*;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/taskMedia")
public class TaskMediaController {

    private static final Logger logger = LogManager.getLogger(TaskMediaController.class.getName());

    @Autowired
    private TaskMediaService taskMediaService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    NotificationService notificationService;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskServiceImpl taskServiceImpl;

    @PostMapping(path = "/uploadFile/{taskId}")
    public ResponseEntity<Object> uploadFile(@RequestParam(name = "file") MultipartFile file, @PathVariable(name = "taskId") Long taskId,
                                             @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        Long accountIdFromHeader = requestHeaderHandler.getAccountIdFromRequestHeader(accountIds);
        ThreadContext.put("accountId", accountIdFromHeader.toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " uploadFile" + '"' + " method ...");
        Task task = new Task();
        try {
            TaskMedia taskMedia = taskMediaService.storeFile(file, taskId, Long.valueOf(accountIds), timeZone);
            String message = "File Uploaded Successfully";
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " uploadFile" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            try {
                task = taskRepository.findByTaskId(taskId);
                if(task != null) {
                    List<HashMap<String, String>> payload = notificationService.recordVoiceStatusNotification(task, accountIdFromHeader);
                    if (payload != null && !payload.isEmpty()) taskServiceImpl.sendPushNotification(payload);
                }
            }
            catch(Exception e){
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Unable to create notification for record voice status in task " + e, new Throwable(allStackTraces));
            }
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);
        } catch(Exception e) {
            e.printStackTrace();
            if (e instanceof ValidationFailedException) {
                throw e;
            } else {
                if (e instanceof FileNameException) {
                    throw e;
                } else {
                    if (e instanceof FileStorageException) {
                        throw e;
                    } else {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to upload the file for the username = " + foundUser.getPrimaryEmail() +
                                " for a taskNumber = " + task.getTaskId() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                    }
                }
            }
        }
    }

    @GetMapping(path = "/downloadFile/{taskId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable(name = "taskId") Long taskId, @RequestHeader(name = "accountIds") String accountIds,
                                                 @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                 HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " downloadFile" + '"' + " method ...");
        Task foundTaskDb = null;
        try {
            foundTaskDb = taskRepository.findByTaskId(taskId);
            if (!taskMediaService.validateMultipleHeaderAccountIdsForTask(accountIds, foundTaskDb)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Header AccountIds are not validated"));
                logger.error("Header accountId is not validated for the task. " + " ,    " + "accountId = " + accountIds + " ,    " + "taskNumber = " +
                        foundTaskDb.getTaskNumber(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Header AccountIds are not validated");
            }
            TaskMedia taskMediaFoundDb = taskMediaService.getFile(taskId);
            if (taskMediaFoundDb == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(null);
//                String allStackTraces = StackTraceHandler.getAllStackTraces(new NoTaskMediaFoundException());
//                logger.error("No media found for the username = " + foundUser.getPrimaryEmail() + " for the taskNumber = " + taskNumber, new Throwable(allStackTraces));
//                ThreadContext.clearMap();
//                throw new NoTaskMediaFoundException();
            } else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " downloadFile" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(taskMediaFoundDb.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + taskMediaFoundDb.getFileName() + "\"")
                        .body(new ByteArrayResource(taskMediaFoundDb.getMedia()));
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if(e instanceof ValidationFailedException) {
                throw e;
            } else {
                if(e instanceof NoTaskMediaFoundException) {
                    throw e;
                } else {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to download the file for the username = " + foundUser.getPrimaryEmail() +
                            " for a taskNumber = " + (foundTaskDb.getTaskNumber() != null ? foundTaskDb.getTaskNumber() : "undefined") + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
    }
}
