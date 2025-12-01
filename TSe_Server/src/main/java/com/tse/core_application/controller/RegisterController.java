package com.tse.core_application.controller;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.tse.core_application.custom.model.OrgId;
import com.tse.core_application.dto.DefaultImageResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.InvalidRequestHeaderException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.OtpService;
import com.tse.core_application.service.Impl.RegistrationService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tse.core_application.service.Impl.RegisterService;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/register")
public class RegisterController {

    private static final Logger logger = LogManager.getLogger(RegisterController.class.getName());

    @Autowired
    private RegisterService registerservice;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    UserAccountRepository userAccountRepository;

    @GetMapping(path = "/getAllOptionsForRegistration")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<Object> getRegistrationOptions(@RequestHeader(name = "timeZone", required = false) String timeZone,
                                                         @RequestHeader(name = "screenName", required = false) String screenName) {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getRegistrationOptions" + '"' + " method ...");
        boolean isAllHeadersValidated = otpService.validateAllHeaders(timeZone, screenName);
        if (!isAllHeadersValidated) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidRequestHeaderException());
            logger.error("getAllOptionsForRegistration API:  Headers are not validated: timeZone = " + timeZone + " ,  " + "screenName = " + screenName, new Throwable(allStackTraces));
            throw new InvalidRequestHeaderException();
        }
        HashMap<String, Object> allRecords;
        try {
            allRecords = registerservice.getAllRecordsFromThreeTables();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getRegistrationOptions" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("getAllOptionsForRegistration API: Something went wrong. " + " ,   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return registerservice.getFormattedRegistrationOptionsResponse(allRecords);
    }

    @GetMapping(path = "/getDefaultUserImage")
    public ResponseEntity<Object> getDefaultUserImage(@RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                      @RequestHeader(name = "screenName", required = false) String screenName, HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getDefaultUserImage" + '"' + " method ...");

        DefaultImageResponse response = new DefaultImageResponse();
        try {
            response = registerservice.getUserDefaultImage(foundUser.getFirstName(), foundUser.getLastName());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getDefaultUserImage" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("getDefaultUserImage API: Something went wrong. " + " ,   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getUserImageByUserId/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getUserImageByUserId(@PathVariable(name = "id") Long id, @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                      @RequestHeader(name = "screenName", required = false) String screenName, HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserImageByUserId" + '"' + " method ...");

        byte[] imageBytes =null;
        try {
            User user = userRepository.findByUserId(id);
            Boolean showDefaultImage = validateUserAndFindAccessOfImage (user, foundUser);
            if (user.getImageData() != null && !showDefaultImage) {
                imageBytes = registerservice.getUserImageInByte(user.getImageData());
            }
            else {
                imageBytes = registerservice.getDefaultImageOfUserInByte(user.getFirstName(), user.getLastName());
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserImageByUserId" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            if(e instanceof ValidationFailedException || e instanceof AccessDeniedException){
                throw e;
            }
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("getUserImageByUserId API: Something went wrong. " + " ,   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if(e instanceof IOException){
                throw e;
            }
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @GetMapping(path = "/getUserImageByAccountId/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getUserImageByAccountId(@PathVariable(name = "id") Long id, @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                       @RequestHeader(name = "screenName", required = false) String screenName, HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserImageByAccountId" + '"' + " method ...");

        byte[] imageBytes = null;
        try {
            User user = userRepository.findByUserId(userAccountRepository.findUserIdByAccountId(id));
            Boolean showDefaultImage = validateUserAndFindAccessOfImage (user, foundUser);
            if (user.getImageData() != null && !showDefaultImage) {
                imageBytes = registerservice.getUserImageInByte(user.getImageData());
            }
            else {
                imageBytes = registerservice.getDefaultImageOfUserInByte(user.getFirstName(), user.getLastName());
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserImageByAccountId" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            if(e instanceof ValidationFailedException || e instanceof AccessDeniedException){
                throw e;
            }
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("getUserImageByAccountId API: Something went wrong. " + " ,   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if(e instanceof IOException){
                throw e;
            }
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @GetMapping(path = "/getUserImageByEmail/{email}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getUserImageByEmail(@PathVariable(name = "email") String email, @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                          @RequestHeader(name = "screenName", required = false) String screenName, HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUserImageByEmail" + '"' + " method ...");

        byte[] imageBytes = null;
        try {
            User user = userRepository.findByPrimaryEmail(email);
            Boolean showDefaultImage = validateUserAndFindAccessOfImage (user, foundUser);
            if (user.getImageData() != null && !showDefaultImage) {
                imageBytes = registerservice.getUserImageInByte(user.getImageData());
            }
            else {
                imageBytes = registerservice.getDefaultImageOfUserInByte(user.getFirstName(), user.getLastName());
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserImageByEmail" + '"' + " method because successfully completed ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            if(e instanceof ValidationFailedException || e instanceof AccessDeniedException){
                throw e;
            }
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("getUserImageByEmail API: Something went wrong. " + " ,   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if(e instanceof IOException){
                throw e;
            }
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    public Boolean validateUserAndFindAccessOfImage (User user, User foundUser) throws AccessDeniedException {
        if (user == null) {
            throw new ValidationFailedException("User does not exist");
        }
        List<Long> orgIdListOfRequester = userAccountRepository.findOrgIdByFkUserIdUserIdAndIsActive(foundUser.getUserId(), true).stream().map(OrgId::getOrgId).collect(Collectors.toList());
        List<Long> orgIdListOfUser = userAccountRepository.findOrgIdByFkUserIdUserIdAndIsActive(user.getUserId(), true).stream().map(OrgId::getOrgId).collect(Collectors.toList());
        if (!CommonUtils.containsAny(orgIdListOfRequester, orgIdListOfUser)) {
            // Show default image
            return true;
        }
        return false;
    }
}
