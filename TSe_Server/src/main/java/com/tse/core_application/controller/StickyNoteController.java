package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.StickyNoteAddRequest;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.StickyNoteFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.StickyNote;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.StickyNoteService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/sticky-note")
public class StickyNoteController {

    private static final Logger logger = LogManager.getLogger(StickyNoteController.class);

    private User foundUserDbByUsername = null;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private StickyNoteService stickyNoteService;

    private void createLogThreadContextByUserToken(String token, String accountIds, String screenName) {
        String username = jwtUtil.getUsernameFromToken(token);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);

    }

    @PostMapping(path = "/add")
    public ResponseEntity<Object> addNote(@Valid @RequestBody StickyNoteAddRequest stickyNoteAddRequest,
                                          @RequestHeader(name = "accountIds") String accountIds,
                                          @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone,
                                          HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered addNote method.");

        StickyNote stickyNoteResponse = new StickyNote();
        try {
            StickyNote stickyNote = stickyNoteService.addStickyNote(stickyNoteAddRequest, foundUserDbByUsername.getUserId());
            BeanUtils.copyProperties(stickyNote, stickyNoteResponse);
            stickyNoteService.convertAllDateTimeToLocalTimeZone(stickyNoteResponse, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " addNote" + '"' + " method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, stickyNoteResponse);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof StickyNoteFailedException) {
                throw e;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute addNote method for username = " +
                        foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }


    @GetMapping(path = "/getAllNotesByUserId/{userId}")
    public ResponseEntity<Object> getAllNotesByUserId(@PathVariable(name = "userId") Long userId,
                                                      @RequestHeader(name = "accountIds") String accountIds,
                                                      @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone,
                                                      HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        createLogThreadContextByUserToken(jwtToken, accountIds, screenName);
        logger.info("Entered getAllNotesByUserId method.");
        try {
            List<HashMap<String, Object>> stickyNoteList = stickyNoteService.getAllStickyNoteByUserId(userId, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllNotesByUserId" + '"' + " because method completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, stickyNoteList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllNotesByUserId method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/update")
    public ResponseEntity<Object> updateNote(@Valid @RequestBody StickyNoteAddRequest stickyNoteAddRequest,
                                             @RequestHeader(name = "accountIds") String accountIds,
                                             @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone,
                                             HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered updateNote method.");

        try {
            String message = stickyNoteService.updateStickyNote(stickyNoteAddRequest, foundUserDbByUsername.getUserId());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " updateNote" + '"' + " method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof StickyNoteFailedException) {
                throw e;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute updateNote method for username = " +
                        foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
            }
        }
    }

    // expects all accountIds of the user in the header
    @PutMapping(path = "/pin/{noteId}")
    public ResponseEntity<Object> pinNote(@PathVariable Long noteId,
                                          @RequestHeader(name = "accountIds") String accountIds,
                                          @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone,
                                          HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered pinNote method.");
        try {
            stickyNoteService.validateAccess(foundUserDbByUsername.getUserId(), noteId, accountIds);
            stickyNoteService.pinNote(foundUserDbByUsername.getUserId(), noteId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API: Exited pinNote method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Note pinned successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute addNote method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    // expects all accountIds of the user in the header
    @PutMapping(path = "/unpin/{noteId}")
    public ResponseEntity<Object> unpinNote(@PathVariable Long noteId,
                                            @RequestHeader(name = "accountIds") String accountIds,
                                            @RequestHeader(name = "screenName") String screenName,
                                            @RequestHeader(name = "timeZone") String timeZone,
                                            HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered unpinNote  method.");
        try {
            stickyNoteService.validateAccess(foundUserDbByUsername.getUserId(), noteId, accountIds);
            stickyNoteService.unpinNote(foundUserDbByUsername.getUserId(), noteId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API: Exited unpinNote method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Note unpinned successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute unpinNote method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    // expects all accountIds of the user in the header
    @PutMapping(path = "/markImportant/{noteId}")
    public ResponseEntity<Object> markNoteAsImportant(@PathVariable Long noteId,
                                                      @RequestHeader(name = "accountIds") String accountIds,
                                                      @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone,
                                                      HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered markNoteAsImportant method.");
        try {
            stickyNoteService.validateAccess(foundUserDbByUsername.getUserId(), noteId, accountIds);
            stickyNoteService.markNoteAsImportant(foundUserDbByUsername.getUserId(), noteId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API: Exited markNoteAsImportant method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Note marked as important successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute markNoteAsImportant method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    // expects all accountIds of the user in the header
    @PutMapping(path = "/unmarkImportant/{noteId}")
    public ResponseEntity<Object> unmarkNoteAsImportant(@PathVariable Long noteId,
                                                        @RequestHeader(name = "accountIds") String accountIds,
                                                        @RequestHeader(name = "screenName") String screenName,
                                                        @RequestHeader(name = "timeZone") String timeZone,
                                                        HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered unmarkNoteAsImportant method.");
        try {
            stickyNoteService.validateAccess(foundUserDbByUsername.getUserId(), noteId, accountIds);
            stickyNoteService.unmarkNoteAsImportant(foundUserDbByUsername.getUserId(), noteId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API: Exited unmarkNoteAsImportant method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Note unmarked as important successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute unmarkNoteAsImportant method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PutMapping(path = "/pinToDashboard/{noteId}")
    public ResponseEntity<Object> pinNoteToDashboard(@PathVariable Long noteId,
                                          @RequestHeader(name = "accountIds") String accountIds,
                                          @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone,
                                          HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered pinNoteToDashboard method.");
        try {
            stickyNoteService.validateAccess(foundUserDbByUsername.getUserId(), noteId, accountIds);
            stickyNoteService.pinNoteToDashboard(foundUserDbByUsername.getUserId(), noteId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API: Exited pinNoteToDashboard method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Note pinned successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute pinNoteToDashboard method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    // expects all accountIds of the user in the header
    @PutMapping(path = "/unpinFromDashboard/{noteId}")
    public ResponseEntity<Object> unpinNoteFromDahboard(@PathVariable Long noteId,
                                            @RequestHeader(name = "accountIds") String accountIds,
                                            @RequestHeader(name = "screenName") String screenName,
                                            @RequestHeader(name = "timeZone") String timeZone,
                                            HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered unpinNoteFromDahboard  method.");
        try {
            stickyNoteService.validateAccess(foundUserDbByUsername.getUserId(), noteId, accountIds);
            stickyNoteService.unpinNoteFromDashboard(foundUserDbByUsername.getUserId(), noteId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info(httpServletRequest.getRequestURI() + " API: Exited unpinNoteFromDahboard method for username = " + foundUserDbByUsername.getPrimaryEmail() + " because completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Note unpinned successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute unpinNoteFromDahboard method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping(path = "/getUserDashboardNote")
    public ResponseEntity<Object> getUserNoteForDashboard(@RequestHeader(name = "accountIds") String accountIds,
                                                      @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone,
                                                      HttpServletRequest httpServletRequest) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getUserNoteForDashboard method.");
        try {
            StickyNote stickyNote = stickyNoteService.getUserStickyNoteForDashboard(foundUserDbByUsername.getUserId());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserNoteForDashboard" + '"' + " because method completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, stickyNote);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUserNoteForDashboard method for username = " +
                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
