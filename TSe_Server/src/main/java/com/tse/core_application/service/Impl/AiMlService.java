package com.tse.core_application.service.Impl;

import com.opencsv.CSVWriter;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.custom.model.RestRespWithData;
import com.tse.core_application.dto.AiMLDtos.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.TaskRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AiMlService {

    private static final Logger logger = LogManager.getLogger(AiMlService.class.getName());

    @Value("${ai.application.root.path}")
    private String aiMLBaseUrl;

    @Value("${comment.api.key}")
    private String aiWorkItemDuplicacyMigrationKey;

    private static final float duplicateFactorScore = 0.9F;

    private final UserAccountRepository userAccountRepository;

    private final TaskRepository taskRepository;

    @Autowired
    public AiMlService(UserAccountRepository userAccountRepository, TaskRepository taskRepository) {
        this.userAccountRepository = userAccountRepository;
        this.taskRepository = taskRepository;
    }

    public RestRespWithData<AiUserApisResponse> registerUserIntoAiService(Long accountId, Integer maxTokens, String timeZone) {

        maxTokens = (maxTokens == null || maxTokens == 0) ? Constants.AiMlConstants.MAX_TOKENS : maxTokens;
        RestTemplate restTemplate = new RestTemplate();

        String url = aiMLBaseUrl + ControllerConstants.AiMLApi.registerUser;
        AiUserRegistrationRequest request = AiUserRegistrationRequest.builder()
                .accountId(accountId.toString())
                .maxTokens(maxTokens)
                .timezone(timeZone)
                .build();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("screenName", "TSE_Server");
        headers.add("accountIds", accountId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<RestRespWithData<AiUserApisResponse>> restResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
            });
            logger.info("userRegistered into the aiServices: " + restResponse.getBody());
            if (restResponse.getBody() != null) {
                if (restResponse.getStatusCodeValue() == 200)
                    userAccountRepository.updateIsRegisteredInAiService(accountId, true);
                return restResponse.getBody();
            }
        } catch (Exception e){
            logger.warn("Caught exception in while registering the user into account Service: " + e.getMessage());
        }
        return new RestRespWithData<>();
    }

    public RestRespWithData<AiUserApisResponse> removeUserFromAiService(Long removedAccountId, String timeZone, String screenName) {

        RestTemplate restTemplate = new RestTemplate();

        String url = aiMLBaseUrl + ControllerConstants.AiMLApi.inactiveUser;
        AiUserRegistrationRequest request = AiUserRegistrationRequest.builder()
                .accountId(removedAccountId.toString())
                .timezone(timeZone)
                .build();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("screenName", screenName);
        headers.add("accountIds", removedAccountId.toString());
        headers.add("timezone", timeZone);
        HttpEntity<Object> requestEntity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<RestRespWithData<AiUserApisResponse>> restResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
            });
            logger.info("userRemoved from the aiServices: " + restResponse.getBody());
            if (restResponse.getBody() != null) {
                if (restResponse.getStatusCodeValue() == 200)
                    userAccountRepository.updateIsRegisteredInAiService(removedAccountId, null);
                return restResponse.getBody();
            }
        } catch (Exception e){
            logger.warn("Caught exception in while deregistering the user into account Service: " + e.getMessage());
        }
        return new RestRespWithData<>();
    }

    public RestRespWithData<AiUserApisResponse> tokenEnquiryInfoByUser(Long accountId, String timeZone, String screenName) {

        if (!userAccountRepository.existsByAccountIdAndIsActive(accountId, true)) {
            throw new ValidationFailedException("No Active Account data found please try with another account.");
        }

        RestTemplate restTemplate = new RestTemplate();

        URI uri = UriComponentsBuilder.fromHttpUrl(aiMLBaseUrl + ControllerConstants.AiMLApi.tokenEnquiry)
                .queryParam("account_id", accountId)
                .build().toUri();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("screenName", screenName);
        headers.add("accountIds", accountId.toString());
        headers.add("timezone", timeZone);
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<RestRespWithData<AiUserApisResponse>> restResponse = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {
        });
        logger.info("token_info from the aiServices: " + restResponse.getBody());
        if (restResponse.getBody() != null) {
            if (restResponse.getBody().getMessage().equals(true)) {
                restResponse.getBody().setStatus(200);
            }
            return restResponse.getBody();
        }
        return new RestRespWithData<>();
    }

    @Transactional
    public byte[] taskMigrationForDuplicateByAi(AiTaskMigrationRequest taskMigrationRequest, String accountIds) {
        if (!aiWorkItemDuplicacyMigrationKey.equals(taskMigrationRequest.getEncryptedKey()) && !accountIds.equals("0")) {
            throw new ValidationFailedException("Please provide a valid encrypted key in request.");
        }
        try (
                Stream<AiWorkItemDescResponse> itemStream = taskRepository.streamAllTask("Deleted", taskMigrationRequest.getStartDateTime(), taskMigrationRequest.getEndDateTime());
                StringWriter stringWriter = new StringWriter();
                CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(
                    new String[]{
                            "taskId",
                            "taskNumber",
                            "taskTitle",
                            "taskDesc",
                            "orgId",
                            "teamId",
                            "projectId",
                            "teamName",
                            "assignedEmail",
                            "taskTypeId",
                            "createDateTime"
                    });
            itemStream.forEach(dto -> {
                        csvWriter.writeNext(
                                new String[]{
                                        String.valueOf(dto.getTaskId()),
                                        dto.getTaskNumber(),
                                        dto.getTaskTitle(),
                                        dto.getTaskDesc(),
                                        dto.getOrgId().toString(),
                                        dto.getTeamId().toString(),
                                        dto.getProjectId().toString(),
                                        dto.getTeamName(),
                                        dto.getAssignedEmail(),
                                        dto.getTaskTypeId().toString(),
                                        dto.getCreateDateTime().toString()
                                });
                    }
            );
            csvWriter.flush();
            return stringWriter.toString().getBytes();
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
            return null;
        }
    }

    public AiDuplicateWorkItemDto isWorkItemCreationIsDuplicate(AiWorkItemDescResponse workItemDesc, Long accountId, String screenName, String timeZone, String jwtToken) {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = UriComponentsBuilder.fromHttpUrl(aiMLBaseUrl + ControllerConstants.AiMLApi.isWorkItemDuplicate).build().toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("screenName", screenName);
        headers.add("accountIds", accountId.toString());
        headers.add("timezone", timeZone);
        headers.setBearerAuth(jwtToken);

        MultiValueMap<String, String> body = getStringStringMultiValueMap(workItemDesc);
        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
        try {
            if(!body.containsKey("isAdd")) {
                ResponseEntity<RestRespWithData<AiDuplicateWorkItemDto>> restResponse = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
                });
                logger.info("IsWorkItemDuplicate from the aiServices: " + restResponse.getBody());
                if (restResponse.getBody() != null) {
                    return filterDuplicateWorkItemBasedOnScore(restResponse.getBody().getData());
                }
            }
            else {
                ResponseEntity<Object> restResponse = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
                });
                logger.info("workItem " + (workItemDesc.getIsAdd() ? "Added " : "Updated ") +  "from the aiServices: " + restResponse.getBody());
                if (restResponse.getBody() != null) {
                    return new AiDuplicateWorkItemDto();
                }
            }

        } catch (Exception e) {
            logger.error("ERROR: AI Api failed, Not able to find the duplicate work item" + e.getMessage() + " " +e.getCause());
        }
        return new AiDuplicateWorkItemDto();
    }

    private static MultiValueMap<String, String> getStringStringMultiValueMap(AiWorkItemDescResponse workItemDesc) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("taskTitle", workItemDesc.getTaskTitle());
        body.add("taskDesc", workItemDesc.getTaskDesc());
        body.add("top_k", "5");
        if(workItemDesc.getIsAdd() != null) {
            body.add("isAdd", workItemDesc.getIsAdd().toString());
        }
        if(workItemDesc.getTaskId() != null) {
            body.add("taskId", workItemDesc.getTaskId().toString());
        }
        if(workItemDesc.getTaskTypeId() != null) {
            body.add("taskTypeId", workItemDesc.getTaskTypeId().toString());
        }
        if(workItemDesc.getTaskNumber() != null) {
            body.add("taskNumber", workItemDesc.getTaskNumber());
        }
        if(workItemDesc.getCreateDateTime() != null) {
            body.add("createdDateTime", workItemDesc.getCreateDateTime().toString());
        }
        if(workItemDesc.getTeamId() != null) {
            body.add("teamId", workItemDesc.getTeamId().toString());
        }
        if(workItemDesc.getOrgId() != null) {
            body.add("orgId", workItemDesc.getOrgId().toString());
        }
        if(workItemDesc.getTeamName() != null) {
            body.add("teamName", workItemDesc.getTeamName());
        }
        if(workItemDesc.getAssignedEmail() != null) {
            body.add("assignedEmail", workItemDesc.getAssignedEmail());
        }
        if(workItemDesc.getProjectId() != null) {
            body.add("projectId", workItemDesc.getProjectId().toString());
        }
        return body;
    }

    private AiDuplicateWorkItemDto filterDuplicateWorkItemBasedOnScore(AiDuplicateWorkItemDto request) {
        List<AiWorkItemDescResponse> response = request.getResults();
        response = response.stream().filter(workItem -> workItem.getScore() > duplicateFactorScore).collect(Collectors.toList());
        request.setResults(response);
        return request;
    }

    // at the end of creation or updating of WorkItem will have to send the work item desc/title to AiService to refresh the Vector Db in real-time.
    public void sendWorkItemDetailOnCreationAndUpdating (Task task, Boolean isForCreation, Long accountId, String screenName, String timeZone, String jwtToken) {
        AiWorkItemDescResponse aiWorkItemDesc = new AiWorkItemDescResponse();
        aiWorkItemDesc.setTaskDesc(task.getTaskDesc());
        aiWorkItemDesc.setTaskTitle(task.getTaskTitle());
        aiWorkItemDesc.setCreateDateTime(LocalDateTime.now());
        aiWorkItemDesc.setOrgId(task.getFkOrgId().getOrgId());
        aiWorkItemDesc.setTaskId(task.getTaskId());
        aiWorkItemDesc.setTaskNumber(task.getTaskNumber());
        aiWorkItemDesc.setTeamId(task.getFkTeamId().getTeamId());
        aiWorkItemDesc.setProjectId(task.getFkProjectId().getProjectId());
        aiWorkItemDesc.setTaskTypeId(task.getTaskTypeId());
        aiWorkItemDesc.setAssignedEmail(task.getFkAccountIdAssigned() != null ? task.getFkAccountIdAssigned().getEmail() : null);
        aiWorkItemDesc.setCreateDateTime(task.getCreatedDateTime());
        aiWorkItemDesc.setIsAdd(isForCreation);

        isWorkItemCreationIsDuplicate(aiWorkItemDesc, accountId, screenName, timeZone, jwtToken);
    }
}
