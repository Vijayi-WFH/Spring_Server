package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.dto.TimeSheetResponse;
import com.tse.core_application.dto.TimeSheetResponseWithHourDistribution;
import com.tse.core_application.dto.TimesheetExportToCSVRequest;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.TimeSheetRequest;
import com.tse.core_application.model.User;
import com.tse.core_application.service.Impl.TimeSheetService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.ValidationException;
import java.util.List;
import java.util.Objects;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/timesheet")
public class TimeSheetController {

    private static final Logger logger = LogManager.getLogger(TimeSheetController.class.getName());

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private TimeSheetService timeSheetService;

    @Value("${tseHr.application.root.path}")
    private String tseHrBaseUrl;

    @PostMapping(path = "/getTimeSheet")
    public ResponseEntity<Object> getTimeSheet(@RequestBody TimeSheetRequest tsRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                               @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws ValidationException, IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        String userId = foundUser.getUserId().toString();

        // entry log
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTimeSheet" + '"' + " method ...");


        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);

        HttpEntity<TimeSheetRequest> requestEntity = new HttpEntity<>(tsRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl+ControllerConstants.TseHr.getTimeSheetUrl;

        ResponseEntity<List<TimeSheetResponse>> timeSheetResponse = null;

        try {
            timeSheetService.validateTimeSheetDateRequest(tsRequest);
            timeSheetResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<TimeSheetResponse>>() {
            });
            timeSheetService.filterTimeSheetForRoles(tsRequest, timeSheetResponse.getBody(), accountIds);
            TimeSheetResponseWithHourDistribution response = timeSheetService.findAndSetHourDistribution(Objects.requireNonNull(timeSheetResponse.getBody()));
            // exit log
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTimeSheet" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e){

            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.NotAcceptable) {
                logger.error("Unable to get user's timesheet. Caught Exception: " + e, new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage() != null ? e.getMessage().substring(6).replace("[", "").replace("]","") : e.getMessage(), Constants.FormattedResponse.VALIDATION_ERROR);
            } else if (e instanceof HttpMessageNotReadableException) {
                throw new com.tse.core_application.exception.HttpMessageNotReadableException("Request's data type is not readable thus not able to process.");
            } else {
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!");
                else if(e.getMessage().contains("Connection refused")) throw new InternalServerErrorException("Currently service is not available. Please try again later.");
                else throw e;
            }
        }
    }

    @PostMapping("/exportToCSV")
    public ResponseEntity<Object> crossOrgGithubLink(@Valid @RequestBody TimesheetExportToCSVRequest timesheetExportToCSVRequest, @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) throws Exception {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " crossOrgGithubLinkRequest" + '"' + " method ...");

        try {
            byte[] csvData = timeSheetService.exportTimesheetToCSV(timesheetExportToCSVRequest, accountIds, foundUser.getUserId().toString(), timeZone);

            ByteArrayResource resource = new ByteArrayResource(csvData);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=timesheet_export.csv");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " crossOrgGithubLinkRequest" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(csvData.length)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to connect github to another org for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
