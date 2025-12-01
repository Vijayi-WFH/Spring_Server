package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.DownloadAttachmentResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.EntityPreferenceService;
import com.tse.core_application.service.Impl.TaskAttachmentService;
import com.tse.core_application.service.Impl.UserAccountService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.ExceptionUtil;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/task-attachment")
public class TaskAttachmentController {

    private static final Logger logger = LogManager.getLogger(TaskAttachmentController.class.getName());

    private User foundUserDbByUsername = null;

    @Autowired
    private TaskAttachmentService taskAttachmentService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityPreferenceService entityPreferenceService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    /**
     * This is the method which fills the logger context with the accountId, screenName and userId.
     *
     * @param token      The token of the user whose log context has to be created.
     * @param accountIds The list of all the account Ids whose log context has to be created.
     * @param screenName The name of the screen which has invoked the corresponding API.
     */
    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

    }

    /**
     * This is the API which upload the attachments for the given task. This API can upload the single as well as the
     * multiple attachments at a time depending upon the request.
     *
     * @param files              The list of all the files which has to be uploaded.
     * @param taskId             The taskId for which the files have to be uploaded.
     * @param uploaderAccountId  The accountId who has uploaded.
     * @param accountIds         The list of all the accountIds.
     * @param screenName         The name of the screen.
     * @param timeZone           The timeZone.
     * @param httpServletRequest The HttpServletRequest.
     * @return The API standard response.
     */
    @Transactional
    @PostMapping(path = "/upload-attachments")
    public ResponseEntity<Object> uploadAttachments(@RequestParam(name = "files") List<MultipartFile> files, @RequestParam(name = "taskId") String taskId,
                                                    @RequestParam(name = "uploaderAccountId") String uploaderAccountId, @RequestHeader(name = "accountIds") String accountIds,
                                                    @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    HttpServletRequest httpServletRequest) throws IOException {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered uploadAttachments method.");

        HashMap<String, Object> uploadedAttachments = null;
        try {
            Boolean isFileSizeValid = fileUtils.validateFileSizeForOrg(Long.parseLong(uploaderAccountId), files);
            if(!isFileSizeValid) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.PAYLOAD_TOO_LARGE,"Attachment size is greater than the maximum allowed in Organization Preference.",Constants.FormattedResponse.FORBIDDEN);
            }
            uploadedAttachments = taskAttachmentService.saveFiles(files, Long.parseLong(taskId), Long.parseLong(uploaderAccountId));
        } catch (Exception exception) {
            exception.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute uploadAttachments method for username = " + foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (exception instanceof ValidationFailedException) throw exception;
            new ExceptionUtil().onException(exception);
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " uploadAttachments" + '"' + " method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, uploadedAttachments);
    }

    /**
     * This is the API which downloads the requested filename for the requested task.
     *
     * @param taskId             The taskId of the task for which the file has to be downloaded.
     * @param fileName           The name of the file to be downloaded.
     * @param accountIds         The list of all the accountIds.
     * @param screenName         The screenName.
     * @param timeZone           The timeZone.
     * @param httpServletRequest The HttpServletRequest.
     * @return The API standard response.
     */
    @GetMapping(path = "/download-attachment")
    public ResponseEntity<Resource> downloadAttachment(@RequestParam(name = "taskId") Long taskId, @RequestParam(name = "fileName") String fileName,
                                                       @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest httpServletRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered downloadAttachment method.");

        DownloadAttachmentResponse downloadAttachmentResponse = null;
        try {
            downloadAttachmentResponse = taskAttachmentService.getTaskAttachmentByTaskIDAndFileNameAndFileStatus(taskId, fileName, Constants.FileAttachmentStatus.A);
        } catch (Exception exception) {
            exception.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute downloadAttachment method for username = " + foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            new ExceptionUtil().onException(exception);
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " downloadAttachment" + '"' + " method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
        ThreadContext.clearMap();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadAttachmentResponse.getFileName() + "\"")
                .body(new ByteArrayResource(downloadAttachmentResponse.getFileContent()));

    }

    /**
     * This is the API which will delete the given attachment for the given task. This API can delete the single as well as
     * the multiple attachments for the given task as per the request.
     *
     * @param taskId             The taskId of the task for which the file has to be deleted.
     * @param removerAccountId   The remover accountId.
     * @param optionIndicator    The option indicator which indicates single or multiple
     * @param fileName           The file name which has to be deleted.
     * @param accountIds         The list of all the accountIds.
     * @param screenName         The screenName.
     * @param timeZone           The timeZone.
     * @param httpServletRequest The HttpServletRequest.
     * @return The API standard response.
     */
    @Transactional
    @DeleteMapping(path = "/delete-attachments")
    public ResponseEntity<Object> deleteAttachments(@RequestParam(name = "taskId") Long taskId, @RequestParam(name = "removerAccountId") Long removerAccountId,
                                                    @RequestParam(name = "optionIndicator") String optionIndicator, @RequestParam(name = "fileName") String fileName,
                                                    @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest httpServletRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered deleteAttachments method.");

        String deletedAttachments = null;
        try {
            deletedAttachments = taskAttachmentService.deleteAttachmentsByTaskIdAndFileNameAndOptionIndicatorAndRemoverAccountId(taskId, fileName, optionIndicator, removerAccountId);
        } catch (Exception exception) {
            exception.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute deleteAttachments method for username = " + foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            new ExceptionUtil().onException(exception);
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " deleteAttachments" + '"' + " method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, deletedAttachments);
    }
}
