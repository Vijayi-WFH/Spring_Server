package com.tse.core_application.controller.geo_fencing.punch;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.geo_fence.punch.PunchRequestCreateDto;
import com.tse.core_application.dto.geo_fence.punch.PunchRequestViewDto;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.service.Impl.geo_fencing.preference.GeoFencingAccessService;
import com.tse.core_application.service.Impl.geo_fencing.punch.PunchRequestService;
import com.tse.core_application.utils.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing punch requests.
 */
@CrossOrigin(value = "*")
@RestController
@RequestMapping("/punchRequest")
@Tag(name = "Punch Requests", description = "Endpoints for creating and querying punch requests")
public class PunchRequestController {

    private static final Logger logger = LogManager.getLogger(PunchRequestController.class);
    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final RequestHeaderHandler requestHeaderHandler;
    private final PunchRequestService punchRequestService;
    private final GeoFencingAccessService geoFencingAccessService;

    public PunchRequestController(JWTUtil jwtUtil, UserService userService, RequestHeaderHandler requestHeaderHandler,
                                  PunchRequestService punchRequestService, GeoFencingAccessService geoFencingAccessService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.requestHeaderHandler = requestHeaderHandler;
        this.punchRequestService = punchRequestService;
        this.geoFencingAccessService = geoFencingAccessService;
    }

    @PostMapping("/{orgId}/requestPunchForEntity")
    @Operation(
            summary = "Create a punch request",
            description = "Creates a new punch request targeting a user, team, project, or org. " +
                    "The request remains pending within the specified time window."
    )
    public ResponseEntity<Object> createPunchRequest(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @Valid @RequestBody PunchRequestCreateDto request,

            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest
    ) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " createPunchRequest" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            PunchRequestViewDto result = punchRequestService.createPunchRequest(orgId, request, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " createPunchRequest" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, result);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to create punch request for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/{orgId}/getPendingRequest")
    @Operation(
            summary = "Get pending punch requests for accounts",
            description = "Returns all currently pending punch requests that apply to the specified account IDs. " +
                    "Membership expansion is performed (USER, TEAM, PROJECT, ORG)."
    )
    public ResponseEntity<Object> getPendingRequests(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam("accountId")
            @Parameter(description = "Account IDs to query for (can be repeated)", required = true)
            List<Long> accountIds,

            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIdsHeader,
            HttpServletRequest httpRequest
    ) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIdsHeader).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getPendingRequests" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            List<PunchRequestViewDto> results = punchRequestService.getPendingRequestsForAccounts(orgId, accountIds, accountIdsHeader, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getPendingRequests" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, results);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get pending requests for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/{orgId}/getPunchRequestById")
    @Operation(
            summary = "Get punch request by ID",
            description = "Retrieves a single punch request by its ID within the specified organization."
    )
    public ResponseEntity<Object> getPunchRequestById(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam("id")
            @Parameter(description = "Punch request ID", required = true)
            Long id,

            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIds,
            HttpServletRequest httpRequest
    ) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getPunchRequestById" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            PunchRequestViewDto result = punchRequestService.getPunchRequestById(orgId, id, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getPunchRequestById" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, result);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get punch request by id for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping("/{orgId}/getPendingRequestHistory")
    @Operation(
            summary = "Get pending request history",
            description = "Returns punch requests whose time window overlaps the specified date/time range " +
                    "and that apply to the given account IDs. Includes requests that were pending at some time in the range."
    )
    public ResponseEntity<Object> getPendingRequestHistory(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam(value = "from", required = false)
            @Parameter(description = "Start of time range (ISO 8601 format, defaults to start of today UTC)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(value = "to", required = false)
            @Parameter(description = "End of time range (ISO 8601 format, defaults to from + 1 day)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam("accountId")
            @Parameter(description = "Account IDs to query for (can be repeated)", required = true)
            List<Long> accountIds,

            @RequestHeader(name = "screenName") String screenName,
            @RequestHeader(name = "timeZone") String timeZone,
            @RequestHeader(name = "accountIds") String accountIdsHeader,
            HttpServletRequest httpRequest
    ) {
        long startTime = System.currentTimeMillis();
        String jwtToken = httpRequest.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIdsHeader).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getPendingRequestHistory" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            List<PunchRequestViewDto> results = punchRequestService.getPendingRequestHistory(orgId, from, to, accountIds, accountIdsHeader, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getPendingRequestHistory" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, results);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get pending request history for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
