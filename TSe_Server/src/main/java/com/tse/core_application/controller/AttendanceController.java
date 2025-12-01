package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.AttendanceService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/attendance")
public class AttendanceController {

    private static final Logger logger = LogManager.getLogger(AccessDomainController.class.getName());

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private JWTUtil jwtUtil;

    @PostMapping("/getAllUserAttendance")
    public ResponseEntity<Object> getAttendanceData (@RequestBody @Valid AttendanceRequestDTO attendanceRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                     @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                     @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered getAttendanceData method.");
        try {
            AttendanceResponseDTO attendanceData = attendanceService.getAttendanceData(attendanceRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAttendanceData" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, attendanceData);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAttendanceData method for userId = " + foundUser.getUserId() + " ,     " + "Caught Exception: " + e.getMessage(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping("/exportToCSV")
    public ResponseEntity<ByteArrayResource> exportToCsv(@RequestBody AttendanceRequestDTO attendanceRequest, @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IOException, IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " exportToCSV" + '"' + " method ...");


        try {
            AttendanceResponseDTO attendanceData = attendanceService.getAttendanceData(attendanceRequest, accountIds);
            byte[] csvData = attendanceService.convertToCsv(attendanceData);

            ByteArrayResource resource = new ByteArrayResource(csvData);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(csvData.length)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to generate CSV file " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }

    }

    @PostMapping("/getMemberOnLeaveForAttendance")
    public ResponseEntity<Object> getMemberOnLeaveForAttendance (@RequestBody LeaveAttendanceRequest leaveAttendanceRequest, @RequestHeader(name = "screenName") String screenName,
                                                                 @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                 HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getMemberOnLeaveForAttendance" + '"' + " method ...");
        try {
            List<LeaveAttendanceResponse> leaveAttendanceResponse = attendanceService.getMemberOnLeaveForAttendance(leaveAttendanceRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getMemberOnLeaveForAttendance" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, leaveAttendanceResponse);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get member on leave for attendance for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/v2/getAllUserAttendance")
    public ResponseEntity<Object> getAttendanceDataV2 (@RequestBody @Valid AttendanceRequestDTO attendanceRequest, @RequestHeader(name = "screenName", required = false) String screenName,
                                                     @RequestHeader(name = "timeZone", required = false) String timeZone,
                                                     @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws IllegalAccessException {
        logger.info("Entered getAttendanceData method.");
        AttendanceResponseV2Dto attendanceData = attendanceService.getAttendanceDataV2(attendanceRequest, accountIds);
        logger.info("Exited" + '"' + " getAttendanceData" + '"' + " method because it completed successfully ...");
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, attendanceData);
    }
    /*
        this is version 2 of the existing getMemberOnLeaveForAttendance this also fetches Off_days,
         Holidays along with Leaves.
         So its response Dto is different.
    */
    @PostMapping("/v2/getMemberOnLeaveForAttendance")
    public ResponseEntity<Object> getMemberOnLeaveForAttendanceV2(@RequestBody LeaveAttendanceRequest leaveAttendanceRequest, @RequestHeader(name = "screenName") String screenName,
                                                                  @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                  HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        logger.info("Entered" + '"' + " getMemberOnLeaveForAttendance" + '"' + " method ...with ScreenName: "+ screenName);
        List<OffDaysResponse> leaveAttendanceResponse = attendanceService.getOffDaysForAttendance(leaveAttendanceRequest, accountIds, timeZone);
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Exited" + '"' + " getMemberOnLeaveForAttendance" + '"' + " method because completed successfully ... timeTaken: "+ estimatedTime);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, leaveAttendanceResponse);
    }
}
