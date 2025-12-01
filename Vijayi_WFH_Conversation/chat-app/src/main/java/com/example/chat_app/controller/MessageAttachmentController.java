package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.DeleteMessageAttachmentRequest;
import com.example.chat_app.dto.DownloadAttachmentResponse;
import com.example.chat_app.exception.IllegalStateException;
import com.example.chat_app.exception.InternalServerErrorException;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.jwtUtils.JWTUtil;
import com.example.chat_app.repository.GroupUserRepository;
import com.example.chat_app.repository.MessageAttachmentRepository;
import com.example.chat_app.repository.MessageRepository;
import com.example.chat_app.repository.UserRepository;
import com.example.chat_app.service.MessageAttachmentService;
import com.example.chat_app.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/api/attachments")
public class MessageAttachmentController {

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private MessageAttachmentService messageAttachmentService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupUserRepository groupUserRepository;

    /**
     * This is the API which upload the attachments for the given task. This API can upload the single as well as the
     * multiple attachments at a time depending upon the request.
     *
     * @param files              The list of all the files which has to be uploaded.
     * @param messageString      The message for which the files have to be uploaded.
     * @param uploaderAccountId  The accountId who has uploaded.
     * @param accountIds         The list of all the accountIds.
     * @param screenName         The name of the screen.
     * @param timeZone           The timeZone.
     * @param request            The HttpServletRequest.
     * @return                   The API standard response.
     */
    @PostMapping(path = "/upload-attachments")
    public ResponseEntity<Object> uploadAttachments(@RequestPart(name = "files") List<MultipartFile> files,
                                                    @RequestPart(name = "uploaderAccountId") String uploaderAccountId,
                                                    @RequestPart(name = "message") String messageString,
                                                    @RequestHeader List<Long> accountIds,
                                                    @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    HttpServletRequest request) throws Exception {

        HashMap<String, Object> uploadedAttachments = null;
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            Boolean isFileSizeValid = fileUtils.validateFileSizeForOrg(Long.parseLong(uploaderAccountId), files);
            if(!isFileSizeValid) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.PAYLOAD_TOO_LARGE, Constants.FormattedResponse.FORBIDDEN, "File exceeds the allowed max size for this organization.");
            }

            uploadedAttachments = messageAttachmentService.saveFiles(files, messageString, accountIds);

        } catch (Exception exception) {
            if (exception.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else throw exception;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, uploadedAttachments);
    }

    /**
     * This is the API which downloads the requested filename for the requested task.
     *
     * @param messageId             The taskId of the task for which the file has to be downloaded.
     * @param fileName           The name of the file to be downloaded.
     * @param accountIds         The list of all the accountIds.
     * @param screenName         The screenName.
     * @param timeZone           The timeZone.
     * @param request The HttpServletRequest.
     * @return The API standard response.
     */
    @GetMapping(path = "/download-attachment")
    public ResponseEntity<Resource> downloadAttachment(@RequestParam(name = "messageId") Long messageId, @RequestParam(name = "fileName") String fileName,
                                                       @RequestParam(name = "messageAttachmentId") Long messageAttachmentId, @RequestHeader List<Long> accountIds,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) throws Exception {

        DownloadAttachmentResponse downloadAttachmentResponse = null;
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            downloadAttachmentResponse = messageAttachmentService.getTaskAttachmentByTaskIDAndFileNameAndFileStatus(messageId, fileName, messageAttachmentId, accountIds);
        } catch (Exception exception) {
            if (exception.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
            else throw exception;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadAttachmentResponse.getFileName() + "\"")
                .body(new ByteArrayResource(downloadAttachmentResponse.getFileContent()));

    }

    /**
     * This is the API which will delete the given attachment for the given task. This API can delete the single as well as
     * the multiple attachments for the given task as per the request.
     *
     * @param messageAttachmentRequest   DeleteAttachmentRequest Object
     * @param accountIds         The list of all the accountIds.
     * @param screenName         The screenName.
     * @param timeZone           The timeZone.
     * @param request The HttpServletRequest.
     * @return The API standard response.
     */
    @PostMapping(path = "/delete-attachments")
    public ResponseEntity<Object> deleteAttachments(@RequestBody DeleteMessageAttachmentRequest messageAttachmentRequest,
                                                    @RequestHeader List<Long> accountIds, @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) throws Exception {

        String deletedAttachments = null;
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

        deletedAttachments = messageAttachmentService.deleteAttachmentsByAttachmentId(messageAttachmentRequest);
        if(deletedAttachments.equals("fail")){
            throw new IllegalStateException("Something went Wrong please try later!!");
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, deletedAttachments);
    }
}
