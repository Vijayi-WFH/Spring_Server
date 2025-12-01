package com.tse.core_application.controller.geo_fencing.userfence;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.geo_fence.userfence.UserFencesResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.service.Impl.geo_fencing.preference.GeoFencingAccessService;
import com.tse.core_application.service.Impl.geo_fencing.userfence.UserFenceService;
import com.tse.core_application.utils.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * REST controller for retrieving effective fences available to users.
 */
@CrossOrigin(value = "*")
@RestController
@RequestMapping("/fenceAssignment")
@Tag(name = "User Fences", description = "Endpoints for retrieving effective fences available to users")
public class UserFenceController {

    private static final Logger logger = LogManager.getLogger(UserFenceController.class);
    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final RequestHeaderHandler requestHeaderHandler;
    private final UserFenceService userFenceService;
    private final GeoFencingAccessService geoFencingAccessService;

    public UserFenceController(JWTUtil jwtUtil, UserService userService, RequestHeaderHandler requestHeaderHandler,
                               UserFenceService userFenceService, GeoFencingAccessService geoFencingAccessService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.requestHeaderHandler = requestHeaderHandler;
        this.userFenceService = userFenceService;
        this.geoFencingAccessService = geoFencingAccessService;
    }

    @GetMapping("/{orgId}/getUserFences")
    @Operation(
            summary = "Get effective fences for a user in an org",
            description = "Returns the union of all fences available to the user via USER, TEAM, PROJECT, and ORG assignments, " +
                    "de-duplicated with source attribution and a computed default fence."
    )
    public ResponseEntity<Object> getUserFences(
            @PathVariable("orgId")
            @Parameter(description = "Organization ID", required = true)
            Long orgId,

            @RequestParam("accountId")
            @Parameter(description = "User account ID", required = true)
            Long accountId,

            @RequestParam(value = "includeInactive", required = false, defaultValue = "false")
            @Parameter(description = "Whether to include inactive fences (default: false)")
            Boolean includeInactive,

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
        logger.info("Entered" + '"' + " getUserFences" + '"' + " method ...");

        try {
            // Validate geo-fencing access for the organization
            geoFencingAccessService.validateGeoFencingAccess(orgId);

            UserFencesResponse response = userFenceService.getUserFences(
                    orgId,
                    accountId,
                    includeInactive != null ? includeInactive : false
            );
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUserFences" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(httpRequest.getRequestURI() + " API: " + "Something went wrong: Not able to get user fences for username = "
                    + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
