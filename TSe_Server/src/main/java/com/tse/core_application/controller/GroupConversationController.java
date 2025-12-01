package com.tse.core_application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.EntityMessageResponse;
import com.tse.core_application.custom.model.EntityOrderResponse;
import com.tse.core_application.dto.conversations.GroupConversationResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Attachment;
import com.tse.core_application.model.GroupConversation;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.AttachmentService;
import com.tse.core_application.service.Impl.ConversationService;
import com.tse.core_application.service.Impl.GroupConversationService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.FileUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/groupConv")
public class GroupConversationController {

    private static final Logger logger = LogManager.getLogger(GroupConversationController.class.getName());

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private GroupConversationService groupConversationService;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private ConversationService conversationService;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
    }

    /** adds a message in a group entity -- expects a single accountId in the header*/
    @PostMapping(path = "/addMessage")
    @Transactional
    public ResponseEntity<Object> addMessage(@RequestParam String groupConvJSON,
                                             @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone,
                                             @RequestHeader(name = "accountIds") String accountIds,
                                             @RequestParam(value = "files", required = false) MultipartFile[] files,
                                             HttpServletRequest request) throws JsonProcessingException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " addMessage" + '"' + " method ...");
        ObjectMapper objectMapper = new ObjectMapper();
        GroupConversation gcMessage = objectMapper.readValue(groupConvJSON, GroupConversation.class);
        GroupConversationResponse response = null;
        try {
            if(files != null) {
                Boolean isFileSizeValid = fileUtils.validateFileSizeForOrg(Long.parseLong(accountIds), Arrays.asList(files));
                if(!isFileSizeValid) {
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.PAYLOAD_TOO_LARGE, Constants.FormattedResponse.FORBIDDEN, "File exceeds the allowed size for this organization.");
                }
            }
            if (gcMessage.getMessage() != null) {
                gcMessage.setMessage(gcMessage.getMessage().trim().replaceAll("\\s+", " "));
            }
            if(gcMessage.getEntityId() != null && gcMessage.getEntityTypeId() != null) {
                groupConversationService.modifyMessagePropertiesAndValidate(gcMessage, files);
                response = groupConversationService.addGcMessageForEntity(gcMessage, accountIds, timeZone);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add message in the entity" +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " addMessage" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }


    /** gets all messages in a group entity */
    @GetMapping(path = "/getEntityAllMessages/{entityTypeId}/{entityId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Object> getEntityAllMessages(@PathVariable(name = "entityTypeId") Integer entityTypeId,
                                                       @PathVariable(name = "entityId") Long entityId,
                                                       @PathVariable(name = "pageNumber") Integer pageNumber,
                                                       @PathVariable(name = "pageSize") Integer pageSize,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " getEntityAllMessages" + '"' + " method ...");
        List<EntityMessageResponse> entityMessageResponse = null;
        try {
            entityMessageResponse = groupConversationService.getEntityAllMessages(entityTypeId, entityId, pageNumber, pageSize, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getEntityAllMessages" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the messages of a entity = " + entityId + "entityTypeId = " + entityTypeId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, entityMessageResponse);
    }


    /** download attachment in a message based on attachment Id */
    @GetMapping(path = "/getMessageAttachment/{attachmentId}")
    public ResponseEntity<byte[]> downloadMessageAttachment(@PathVariable Long attachmentId,
                                                            @RequestHeader(name = "screenName") String screenName,
                                                            @RequestHeader(name = "timeZone") String timeZone,
                                                            @RequestHeader(name = "accountIds") String accountIds,HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered downloadMessageAttachment method.");
        HttpHeaders headers = new HttpHeaders();
        Attachment attachment = attachmentService.getAttachmentById(attachmentId);
        try {
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }
            headers.setContentType(MediaType.parseMediaType(attachment.getFileType()));
            String filename = attachment.getFileName();
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " downloadMessageAttachment" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute downloadMessageAttachment method for attachmentId = " + attachmentId + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
        }
        return ResponseEntity.ok().headers(headers).body(attachment.getFileContent());

    }

    /**
     * Retrieves entities that the user (identified by the given account IDs) is part of,
     * and returns them in the order of the latest messages. It retrieves the last message and createdDateTime of that message
     * in each entity with and then order all the entities based on createdDateTime
     * @param accountIds
     * @return
     */
    // requires all accountIds in the header
    @GetMapping(path = "/getEntitiesOrderByMessage")
    public ResponseEntity<Object> getEntitiesOrderByMessage(@RequestHeader(name = "screenName") String screenName,
                                                            @RequestHeader(name = "timeZone") String timeZone,
                                                            @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered" + '"' + " getEntitiesOrderByMessage" + '"' + " method ...");
        List<EntityOrderResponse> entityOrderResponses = new ArrayList<>();
        List<Long> accountIdsLong = requestHeaderHandler.convertToLongList(accountIds);
        try {
            entityOrderResponses = groupConversationService.getEntitiesOrderByMessage(accountIdsLong, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getEntitiesOrderByMessage" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get entities order by message for the given accountIds = " + accountIds + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, entityOrderResponses);
    }

    @GetMapping(path = "/getEntityMessagesBetweenGc/{entityTypeId}/{entityId}/{firstGcId}/{secondGcId}")
    public ResponseEntity<Object> getEntityMessagesBetweenGc(
            @PathVariable(name = "entityTypeId") Integer entityTypeId,
            @PathVariable(name = "entityId") Long entityId,
            @PathVariable(name = "firstGcId") Long firstGcId,
            @PathVariable(name = "secondGcId") Long secondGcId,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        List<Long> accountIdsLong = requestHeaderHandler.convertToLongList(accountIds);
        logger.info("Entered getEntityMessagesBetweenGroupConversations method ...");
        List<EntityMessageResponse> entityMessageResponse;
        try {
            entityMessageResponse = groupConversationService.getEntityMessagesBetweenGroupConversations(entityTypeId, entityId, foundUserDbByUsername.getUserId(), accountIdsLong, firstGcId, secondGcId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited getEntityMessagesBetweenGroupConversations method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the messages of a entity = " + entityId + "entityTypeId = " + entityTypeId +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, entityMessageResponse);
    }

    /*
    This method is to migrate/create a new SYSTEM GROUPS of org/project/teams which were non-existent in Conversation table.
    * */
    @PostMapping("/systemGroupMigration")
    public String systemGroupMigrationFromConversation(@RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){
        //fetch the Users and check if any entry of users is missing in conversation if so then hit the api to add the users...into it...
//        List<Long> accountIdsLong = Arrays.stream(accountIds.split(",")).map(Long::parseLong).collect(Collectors.toList());
        if(!accountIds.contains("0")){
            throw new ValidationFailedException("Only System call is valid");
        }
        return conversationService.addNonExistentSystemGroupsIntoConversation(accountIds);
        //fetch all the groups from the Conversation

    }

}
