package com.tse.core_application.controller.geo_fencing.attendance;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.geo_fence.attendance.*;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.service.Impl.geo_fencing.attendance.GeoFenceAttendanceDataService;
import com.tse.core_application.service.Impl.geo_fencing.attendance.GeoFenceAttendanceService;
import com.tse.core_application.service.Impl.geo_fencing.preference.GeoFencingAccessService;
import com.tse.core_application.utils.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * Phase 6b: REST controller for attendance operations (CHECK_IN/OUT).
 */
@CrossOrigin(value = "*")
@RestController
@RequestMapping("/punch-event")
@Tag(name = "Attendance", description = "Attendance and punch operations")
public class GeoFenceAttendanceController {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(GeoFenceAttendanceController.class);
    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final RequestHeaderHandler requestHeaderHandler;
    private final GeoFenceAttendanceService geoFenceAttendanceService;
    private final GeoFenceAttendanceDataService geoFenceAttendanceDataService;
    private final GeoFencingAccessService geoFencingAccessService;

    public GeoFenceAttendanceController(JWTUtil jwtUtil, UserService userService, RequestHeaderHandler requestHeaderHandler,
                                        GeoFenceAttendanceService geoFenceAttendanceService, GeoFenceAttendanceDataService geoFenceAttendanceDataService, GeoFencingAccessService geoFencingAccessService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.requestHeaderHandler = requestHeaderHandler;
        this.geoFenceAttendanceService = geoFenceAttendanceService;
        this.geoFenceAttendanceDataService = geoFenceAttendanceDataService;
        this.geoFencingAccessService = geoFencingAccessService;
    }

    /**
     * POST /api/orgs/{orgId}/attendance/punch
     * Process a punch event (CHECK_IN or CHECK_OUT).
     */
    @PostMapping("/{orgId}/punch")
    @Operation(summary = "Process a punch event", description = "Process CHECK_IN or CHECK_OUT event with geofence validation")
    public ResponseEntity<Object> processPunch(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Punch request", required = true)
            @RequestBody PunchCreateRequest request,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " processPunch" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            PunchResponse response = geoFenceAttendanceService.processPunch(orgId, request, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " processPunch" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to process punch for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    /**
     * Process a PUNCHED event (supervisor-triggered punch).
     */
    @PostMapping("/{orgId}/punched")
    @Operation(summary = "Process a PUNCHED event", description = "Fulfill a punch request with PUNCHED event")
    public ResponseEntity<Object> processPunched(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Punched event request", required = true)
            @Valid @RequestBody PunchedEventRequest request,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " processPunched" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            PunchResponse response = geoFenceAttendanceService.processPunchedEvent(
                    orgId,
                    request.getAccountId(),
                    request.getPunchRequestId(),
                    request.getLat(),
                    request.getLon(),
                    request.getAccuracyM(),
                    accountIds,
                    timeZone
            );
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " processPunched" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to process punched event for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    /**
     * Get attendance summary for a specific user and date.
     * Similar to /data API but for single user.
     */
    @PostMapping("/{orgId}/getUserEventDetails")
    @Operation(summary = "Get attendance summary for specific date",
            description = "Get attendance summary for a specific user and date with all events, missing events, and timezone handling")
    public ResponseEntity<Object> getTodaySummary(
            @Parameter(description = "Organization ID")
            @PathVariable("orgId") Long orgId,
            @Parameter(description = "Today attendance request", required = true)
            @Valid @RequestBody TodayAttendanceRequest request,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTodaySummary" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            TodaySummaryResponse response = geoFenceAttendanceService.getTodaySummary(orgId, request, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTodaySummary" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get today summary for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/{orgId}/getGeoFencingAttendanceData")
    @Operation(summary = "Get comprehensive attendance data",
            description = "Get attendance data for date range including summary, detailed rows, grid, and drill-down")
    public ResponseEntity<Object> getAttendanceData(
            @Parameter(description = "Organization ID") @PathVariable Long orgId,
            @Parameter(description = "Attendance data request", required = true)
            @Valid @RequestBody GeoFenceAttendanceDataRequest request,
            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAttendanceData" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            request.setOrgId(orgId);
            AttendanceDataResponse response = geoFenceAttendanceDataService.getAttendanceData(request, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAttendanceData" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get attendance data for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
