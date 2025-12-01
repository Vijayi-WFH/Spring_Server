package com.tse.core_application.controller;

import com.tse.core_application.custom.model.RestRespWithData;
import com.tse.core_application.dto.AiMLDtos.AiTaskMigrationRequest;
import com.tse.core_application.dto.AiMLDtos.AiUserApisResponse;
import com.tse.core_application.dto.AiMLDtos.AiWorkItemDescResponse;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.service.Impl.AiMlService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/ai")
public class AiMlController {

    private static final Logger logger = LogManager.getLogger(AiMlController.class.getName());

    private final AiMlService aiMlService;

    @Autowired
    public AiMlController(AiMlService aiMlService) {
        this.aiMlService = aiMlService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> registerUserInAiService(@RequestParam("maxTokens") Integer maxTokens,
                                                          @RequestParam("requestAccountIds") String requestAccountIds,
                                                          @RequestHeader("accountIds") String accountIds, @RequestHeader String timeZone,
                                                          @RequestHeader String screenName, HttpServletRequest request) {

        ThreadContext.put("accountId", accountIds);
        ThreadContext.put("timezone", timeZone);
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered \"registerUserIntoAiService()\" method ...");

        List<Long> accountIdLong = Arrays.stream(requestAccountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<RestRespWithData<AiUserApisResponse>> responseList = new ArrayList<>();
        for (Long accountId : accountIdLong) {
            RestRespWithData<AiUserApisResponse> response = aiMlService.registerUserIntoAiService(accountId, maxTokens, timeZone);
            responseList.add(response);
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, responseList);
    }

    @PostMapping("/remove")
    public ResponseEntity<Object> removeUserFromAiService(@RequestParam("removedAccountId") Long removedAccountId,
                                                          @RequestHeader("accountIds") String accountIds,
                                                          @RequestHeader String timeZone, @RequestHeader String screenName) {

        ThreadContext.put("accountId", accountIds);
        ThreadContext.put("timezone", timeZone);
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered \"removeUserFromAiService()\" method ...");

        RestRespWithData<AiUserApisResponse> response = aiMlService.removeUserFromAiService(removedAccountId, timeZone, screenName);

        return CustomResponseHandler.generateCustomResponse(HttpStatus.valueOf(response.getStatus()), response.getMessage(), response.getData());
    }

    @PostMapping("/tokenInfo")
    public ResponseEntity<Object> tokenEnquiryInfoByUser(@RequestParam("enquiredAccountId") Long enquiredAccountId,
                                                         @RequestHeader("accountIds") String accountIds,
                                                         @RequestHeader String timeZone, @RequestHeader String screenName) {

        ThreadContext.put("accountId", accountIds);
        ThreadContext.put("timezone", timeZone);
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered \"tokenEnquiryInfoByUser()\" method ...");

        RestRespWithData<AiUserApisResponse> response = aiMlService.tokenEnquiryInfoByUser(enquiredAccountId, timeZone, screenName);

        return CustomResponseHandler.generateCustomResponse(HttpStatus.valueOf(response.getStatus()), response.getMessage(), response.getData());
    }

    @PostMapping(path = "/scheduleTaskDataMigration")
    public ResponseEntity<ByteArrayResource> scheduleTaskDataMigration (@RequestBody AiTaskMigrationRequest taskMigrationRequest,
                                                                        @RequestHeader(name = "screenName") String screenName,
                                                                        @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                        HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " /scheduleTaskDataMigration" + '"' + " method ...");
        try {
            byte[] csvData = aiMlService.taskMigrationForDuplicateByAi(taskMigrationRequest, accountIds);
            ByteArrayResource resource = new ByteArrayResource(csvData);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " /scheduleTaskDataMigration" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=task-migration_data.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(csvData.length)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: /task/scheduleTaskDataMigration Something went wrong: Not able to export CSV data to AI service for migration,  Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

}
