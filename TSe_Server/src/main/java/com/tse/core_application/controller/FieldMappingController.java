//package com.tse.core_application.controller;
//
//import com.tse.core_application.constants.Constants;
//import com.tse.core_application.handlers.CustomResponseHandler;
//import com.tse.core_application.handlers.RequestHeaderHandler;
//import com.tse.core_application.handlers.StackTraceHandler;
//import com.tse.core_application.model.TaskHistoryColumnsMapping;
//import com.tse.core_application.model.User;
//import com.tse.core_application.service.FieldMappingService;
//import com.tse.core_application.service.Impl.UserService;
//import com.tse.core_application.utils.ExceptionUtil;
//import com.tse.core_application.utils.JWTUtil;
//import org.apache.log4j.Logger;
//import org.apache.log4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.ArrayList;
//import java.util.List;
//
//@RestController
//@RequestMapping(path = "/field-mapping")
//public class FieldMappingController {
//
//    private static final Logger logger = Logger.getLogger(FieldMappingController.class.getName());
//
//    private User foundUserDbByUsername = null;
//
//    @Autowired
//    private JWTUtil jwtUtil;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private RequestHeaderHandler requestHeaderHandler;
//
//    @Autowired
//    private FieldMappingService fieldMappingService;
//
//    private void createMDCLogContextByUserToken(String token, String accountIds, String screenName) {
//        String username = jwtUtil.getUsernameFromToken(token);
//        foundUserDbByUsername = userService.getUserByUserName(username);
//        MDC.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds));
//        MDC.put("userId", foundUserDbByUsername.getUserId());
//        MDC.put("requestOriginatingPage", screenName);
//
//    }
//
//    @GetMapping(path = "/getAllFieldsAndMappings")
//    public ResponseEntity<Object> getAllFieldsAndMappings(@RequestHeader(name = "accountIds") String accountIds,
//                                                          @RequestHeader(name = "screenName") String screenName,
//                                                          @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest httpServletRequest) {
//        long sprintStartTime = System.currentTimeMillis();
//        String jwtToken = httpServletRequest.getHeader("Authorization").substring(7);
//        createMDCLogContextByUserToken(jwtToken, accountIds, screenName);
//        logger.info("Entered getAllFieldsAndMappings method.");
//
//        List<TaskHistoryColumnsMapping> allFieldsMappings = new ArrayList<>();
//
//        try {
//            allFieldsMappings = fieldMappingService.getAllFieldMappings();
//        } catch (Exception exception) {
//            exception.printStackTrace();
//            String allStackTraces = StackTraceHandler.getAllStackTraces(exception);
//            logger.error(httpServletRequest.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllFieldsAndMappings method for username = " +
//                    foundUserDbByUsername.getPrimaryEmail() + " ,     " + "Caught Exception: " + exception, new Throwable(allStackTraces));
//            MDC.clear();
//            new ExceptionUtil().onException(exception);
//        }
//        long estimatedTime = System.currentTimeMillis() - sprintStartTime;
//        MDC.put("systemResponseTime", estimatedTime);
//        logger.info(httpServletRequest.getRequestURI() + " API:  " + "Exited" + '"' + " getAllFieldsAndMappings" + '"' + " method for username = " +
//                foundUserDbByUsername.getPrimaryEmail() + " because completed successfully. ");
//        MDC.clear();
//        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, allFieldsMappings);
//    }
//
//}
