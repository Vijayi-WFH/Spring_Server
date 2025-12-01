package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.RoleId;
import com.tse.core_application.dto.EnvironmentRequest;
import com.tse.core_application.dto.EnviornmentResponse;
import com.tse.core_application.dto.EnvironmentUpdateRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.CustomEnvironmentRepository;
import com.tse.core_application.repository.OrganizationRepository;
import com.tse.core_application.repository.UserAccountRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomEnvironmentService {

    @Autowired
    private CustomEnvironmentRepository customEnvironmentRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;


    public EnviornmentResponse createEnvironment(EnvironmentRequest environmentRequest, String accountId) {
        validateCustomEnvironment(environmentRequest.getEntityTypeId(), environmentRequest.getEntityId(), accountId, List.of(RoleEnum.ORG_ADMIN.getRoleId()));
        environmentRequest.setEnvironmentDisplayName(environmentRequest.getEnvironmentDisplayName().trim());
        environmentRequest.setEnvironmentDescription(environmentRequest.getEnvironmentDescription().trim());
        validateName(environmentRequest.getEnvironmentDisplayName(), environmentRequest.getEntityId());
        CustomEnvironment customEnvironmentEntity = new CustomEnvironment();
        BeanUtils.copyProperties(environmentRequest, customEnvironmentEntity);
        CustomEnvironment customDb = customEnvironmentRepository.save(customEnvironmentEntity);
        EnviornmentResponse response = new EnviornmentResponse();
        BeanUtils.copyProperties(customDb, response);
        return response;
    }


    public void validateCustomEnvironment(Integer entityTypeId, Long entityId, String accountId, List<Integer> roles) {
        Long accountIdLong;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        List<Long> accountIds = List.of(accountIdLong);

        // 1. Check entityType, entityId, AccountId belong to Organization
        if (!Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            throw new ValidationFailedException("Entity Type Id does not belong to Organization!");
        }

        boolean hasAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(
                entityTypeId,
                entityId,
                accountIds,
                roles,
                true
        );

        if (!hasAccess) {
            throw new ValidationFailedException("User does not have access of Custom Environment!");
        }
    }

    public void validateName(String environmentDisplayName, Long entityId) {
        String lowerCaseName = environmentDisplayName.toLowerCase();
        CustomEnvironment customDb = customEnvironmentRepository
                .findByLowerEnvironmentDisplayNameAndEntityId(lowerCaseName, entityId);
        if (customDb != null) {
            throw new ValidationFailedException("Environment Display Name must be unique!");
        }
    }

    public List<EnviornmentResponse> updateEnvironment(
            Integer entityTypeId,
            Long entityId,
            String accountId,
            List<EnvironmentUpdateRequest> environmentRequestList) {
        if (entityTypeId == null || entityId == null || accountId == null || environmentRequestList == null) {
            throw new ValidationFailedException("Input parameters cannot be null");
        }
        validateCustomEnvironment(entityTypeId, entityId, accountId, List.of(RoleEnum.ORG_ADMIN.getRoleId()));
        if (environmentRequestList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, Boolean> requestedActiveMap = environmentRequestList.stream()
                .collect(Collectors.toMap(
                        EnvironmentUpdateRequest::getCustomEnvironmentId,
                        EnvironmentUpdateRequest::getIsActive
                ));

        List<CustomEnvironment> existingEnvironmentList = customEnvironmentRepository.findAllByEntityId(entityId);
        boolean atLeastOneActiveAfterUpdate = existingEnvironmentList.stream()
                .anyMatch(env -> {
                    Boolean updatedStatus = requestedActiveMap.get(env.getCustomEnvironmentId());
                    return Boolean.TRUE.equals(updatedStatus != null ? updatedStatus : env.getIsActive());
                });

        if (!atLeastOneActiveAfterUpdate) {
            throw new ValidationFailedException("At least one environment must remain active for the organization.");
        }
        Map<Integer, String> requestIdToNormalizedName = new HashMap<>();
        Set<String> normalizedRequestNames = new HashSet<>();
        for (EnvironmentUpdateRequest req : environmentRequestList) {
            if (req.getCustomEnvironmentId() == null || req.getEnvironmentDisplayName() == null) {
                throw new ValidationFailedException("Environment ID and display name cannot be null");
            }
            req.setEnvironmentDisplayName(req.getEnvironmentDisplayName().trim());
            req.setEnvironmentDescription(req.getEnvironmentDescription().trim());
            String normalizedName = req.getEnvironmentDisplayName().toLowerCase();
            if (!normalizedRequestNames.add(normalizedName)) {
                throw new ValidationFailedException("Duplicate display name in request: " + req.getEnvironmentDisplayName());
            }
            requestIdToNormalizedName.put(req.getCustomEnvironmentId(), normalizedName);
        }
        List<CustomEnvironment> existingEnvironments = customEnvironmentRepository.findAllByEntityId(entityId);
        Set<String> normalizedDbNamesOutsideRequest = existingEnvironments.stream()
                .filter(env -> !requestIdToNormalizedName.containsKey(env.getCustomEnvironmentId()))
                .map(env -> env.getEnvironmentDisplayName().trim().toLowerCase())
                .collect(Collectors.toSet());
        for (Map.Entry<Integer, String> entry : requestIdToNormalizedName.entrySet()) {
            String incomingName = entry.getValue();
            if (normalizedDbNamesOutsideRequest.contains(incomingName)) {
                throw new ValidationFailedException("Display name already exists: " + incomingName);
            }
        }
        List<EnviornmentResponse> responseList = new ArrayList<>();
        for (EnvironmentUpdateRequest envReq : environmentRequestList) {
            CustomEnvironment envDb = customEnvironmentRepository.findById(envReq.getCustomEnvironmentId())
                    .orElseThrow(() -> new ValidationFailedException(
                            "Environment not found for ID: " + envReq.getCustomEnvironmentId()));
            if (!envDb.getEntityId().equals(entityId) || !envDb.getEntityTypeId().equals(entityTypeId)) {
                throw new ValidationFailedException("Environment does not belong to specified entity");
            }
            BeanUtils.copyProperties(envReq, envDb, "entityId", "entityTypeId");
            CustomEnvironment updatedEnv = customEnvironmentRepository.save(envDb);
            EnviornmentResponse envResponse = new EnviornmentResponse();
            BeanUtils.copyProperties(updatedEnv, envResponse);
            responseList.add(envResponse);
        }
        return responseList;
    }

    public List<EnviornmentResponse> getEnvironment(Integer entityTypeId, Long entityId, String accountIds) {
        validateCustomEnvironment(entityTypeId, entityId, accountIds, List.of(RoleEnum.ORG_ADMIN.getRoleId()));
        List<CustomEnvironment> environments = customEnvironmentRepository.findAllByEntityId(entityId);
        List<EnviornmentResponse> responseList = new ArrayList<>();
        for (CustomEnvironment envDb : environments) {
            EnviornmentResponse envResponse = new EnviornmentResponse();
            BeanUtils.copyProperties(envDb, envResponse);
            responseList.add(envResponse);
        }
        return responseList;
    }

    public List<EnviornmentResponse> getEnvironmentIsActive(Integer entityTypeId, Long entityId, String accountIds) {
        Long accountIdLong;
        try {
            accountIdLong = Long.parseLong(accountIds);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        UserAccount user = userAccountRepository.findByAccountId(accountIdLong);
        if (!Objects.equals(entityId, user.getOrgId())) {
            throw new ValidationFailedException("User don't belong to Entity!!!");
        }
        List<CustomEnvironment> environments = customEnvironmentRepository.findAllByEntityIdAndIsActive(entityId, true);
        List<EnviornmentResponse> responseList = new ArrayList<>();
        for (CustomEnvironment envDb : environments) {
            EnviornmentResponse envResponse = new EnviornmentResponse();
            BeanUtils.copyProperties(envDb, envResponse);
            responseList.add(envResponse);
        }
        return responseList;
    }
}

