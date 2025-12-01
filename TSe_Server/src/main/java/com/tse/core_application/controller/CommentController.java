package com.tse.core_application.controller;

import java.security.InvalidKeyException;
import java.util.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.CommentResponse;
import com.tse.core_application.custom.model.CommentTaskIdTaskTitleCommentFrom;
import com.tse.core_application.dto.CommentTagRequest;
import com.tse.core_application.exception.*;
import com.tse.core_application.model.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.FileUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.tse.core_application.service.Impl.CommentService;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/comment")
public class CommentController {

    private static final Logger logger = LogManager.getLogger(CommentController.class.getName());
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentService commentService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private UserService userService;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        User foundUserDbByUsername = null;
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

    }

    // to list user's all task all comments
    @GetMapping(path = "/getUserAllTaskComments")
    public ResponseEntity<Object> getUserAllTaskComments(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                         @RequestHeader(name = "accountIds") String accountIds, @RequestParam(name = "activeDays", defaultValue = "20") int activeDays, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);

        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserAllTaskComments" + '"' + " method ...");


        HashMap<Long, Task> listComments = new HashMap<>();
        HashMap<Long, Task> listNoComments = new HashMap<>();
        List<CommentTaskIdTaskTitleCommentFrom> commentTaskIdTaskTitleCommentFromList = new ArrayList<>();
        try {
            List<Task> tasks = commentService.getAllFilteredTaskForComment(accountIds, activeDays);

            for (Task task : tasks) {
                if (task.getCommentId() == null) {
                    listNoComments.put(task.getTaskId(), task);
                } else {
                    listComments.put(task.getTaskId(), task);
                }
            }

            List<CommentTaskIdTaskTitleCommentFrom> listMapComments = commentService.getUserAllTaskComments(listComments, timeZone);
            List<CommentTaskIdTaskTitleCommentFrom> listMapNoComments = commentService.getUserAllTaskNoComments(listNoComments);

            commentTaskIdTaskTitleCommentFromList.addAll(listMapComments);
            commentTaskIdTaskTitleCommentFromList.addAll(listMapNoComments);

            commentTaskIdTaskTitleCommentFromList.sort(Comparator.comparing(CommentTaskIdTaskTitleCommentFrom::getCreatedDateTime,Comparator.nullsLast(Comparator.reverseOrder())));

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserAllTaskComments" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the comments of a user for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, commentTaskIdTaskTitleCommentFromList);
    }

    // endpoint to list all comments of a task by taskId in pageView
    @GetMapping(path = "/getTaskAllComments/{taskId}")
    public ResponseEntity<Object> getUserTaskAllComments(@PathVariable(name = "taskId") Long taskId,
                                                                 @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                                 @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserTaskAllComments" + '"' + " method ...");
        ArrayList<CommentResponse> createDateTimeComments = null;

        try {
            createDateTimeComments = commentService.getComments(taskId, timeZone, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserTaskAllComments" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            if(e instanceof CommentNotFoundException) {
                throw e;
            } else {
                if(e instanceof TaskNotFoundException) {
                    throw e;
                } else {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    if (e instanceof ForbiddenException) {
                        logger.error(e.getMessage()+" Caught Error: " + e, new Throwable(allStackTraces));
                        throw new ForbiddenException(e.getMessage());
                    }
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the comments of a task for the username = " + foundUser.getPrimaryEmail() +
                            " ,     " + "taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, createDateTimeComments);
    }

    @PostMapping(path = "/addComment")
    @Transactional
    public ResponseEntity<Object> addComment(@RequestParam String commentJson, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                             @RequestHeader(name = "accountIds") String accountIds, @RequestParam(value = "files", required = false) MultipartFile[] files,
                                             HttpServletRequest request) throws JsonProcessingException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addComment" + '"' + " method ...");
        CommentResponse commentResponse = null;
        try {
            Comment comment = null;
            ObjectMapper objectMapper = new ObjectMapper();
            if (commentJson != null && !commentJson.isEmpty()) {
                comment = objectMapper.readValue(commentJson, Comment.class);
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.BAD_REQUEST, Constants.FormattedResponse.BAD_REQUEST, "Comment JSON can not be null or empty");
            }
            if (files != null) {
                Boolean isFileSizeValid = fileUtils.validateFileSizeForOrg(Long.parseLong(accountIds), Arrays.asList(files));
                if (!isFileSizeValid) {
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.PAYLOAD_TOO_LARGE, Constants.FormattedResponse.FORBIDDEN, "File exceeds the allowed size for this organization.");
                }
            }
            if (comment.getTask().getTaskId() != null) {
                commentService.modifyCommentPropertiesAndValidate(comment, files, accountIds);
                commentResponse = commentService.addComment(comment, accountIds, timeZone);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof ForbiddenException) {
                logger.error(e.getMessage() + " Caught Error: " + e, new Throwable(allStackTraces));
                throw new ForbiddenException(e.getMessage());
            }
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add comment for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
        logger.info("Exited" + '"' + " addComment" + '"' + " method because it completed successfully ...");
        ThreadContext.clearMap();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, commentResponse);
    }

    @PostMapping(path = "/getCommentsByTaskId")
    public ResponseEntity<Object> getCommentsByTask(@RequestBody TaskCommentsRequest taskCommentsRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);

        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getCommentsByTask" + '"' + " method ...");

        HashMap<Long, String> response = new HashMap<>();

        try{
            if(!taskRepository.existsByTaskId(taskCommentsRequest.getTaskId())){
                throw new TaskNotFoundException();
            }

            Pageable pageable = PageRequest.of(0, taskCommentsRequest.getCommentsToGet());
            List<Comment> allComments  = commentRepository.getAllCommentsForTasksByLabel(taskCommentsRequest.getTaskId(), taskCommentsRequest.getLabelToSearch(), taskCommentsRequest.getLabelToExclude(),  pageable);


            if(allComments != null && !allComments.isEmpty()) {

                for(Comment comment : allComments) {
                    response.put(comment.getCommentLogId(),comment.getComment());
                }

            }

            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getCommentsByTask" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();

        }
        catch (Exception e){
            if(e instanceof TaskNotFoundException){
                throw e;
            }
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the comments of a task for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "taskId = " + taskCommentsRequest.getTaskId()  + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);


    }

    @PostMapping(path = "/updateCommentsTag")
    public ResponseEntity<Object> updateCommentsTag (@Valid @RequestBody CommentTagRequest commentTagRequest,
                                                     @RequestHeader(name = "commentKey") String commentKey) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        logger.info("Entered" + '"' + " updateCommentsTag" + '"' + " method ...");

        try {
            String response = commentService.updateCommentTag(commentTagRequest, commentKey);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateCommentsTag" + '"' + " method because completed successfully ...");
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(" API: /comment/updateCommentsTag " + "Something went wrong: Not able to update comment tag for comment log id = " + commentTagRequest.getCommentLogId() + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
