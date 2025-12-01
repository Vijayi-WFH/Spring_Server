package com.tse.core_application.service.Impl;

import com.ibm.icu.util.VTimeZone;
import com.tse.core_application.dto.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserFeatureAccessService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    public UserFeatureAccessResponseForAllDto addHrRole(AddHrRoleRequestDto addHrRoleRequestDto, String timeZone, String accountIds) {
        Long orgId = addHrRoleRequestDto.getOrgId();
        validateHrRoleRequest(addHrRoleRequestDto.getOrgId(), accountIds);
        Integer entityTypeId = addHrRoleRequestDto.getEntityTypeId();
        if (addHrRoleRequestDto.getEntityActions() == null || addHrRoleRequestDto.getEntityActions().isEmpty()) {
            throw new ValidationFailedException("Entity Id and Actions cannot be null or empty");
        }

        EntityActionDto firstAction = addHrRoleRequestDto.getEntityActions().get(0);
        if (firstAction.getEntityId() == null || firstAction.getActionList() == null || firstAction.getActionList().isEmpty()) {
            throw new ValidationFailedException("Entity ID or action list is missing");
        }
        Long userAccountId = addHrRoleRequestDto.getUserAccountId();
        UserFeaturesAccess userFeaturesAccess = validateFeatureAccess(orgId, entityTypeId, firstAction.getEntityId(), firstAction.getActionList(), userAccountId);
        if(userFeaturesAccess != null && userFeaturesAccess.getIsDeleted() != true)
        {
            throw new ValidationFailedException("User Feature access already exist");
        }
        if (userFeaturesAccess != null) {
            userFeaturesAccess.setActionIds(firstAction.getActionList());
            userFeaturesAccess.setIsDeleted(false);
        } else {
            userFeaturesAccess = new UserFeaturesAccess();
            userFeaturesAccess.setOrgId(orgId);
            userFeaturesAccess.setEntityTypeId(entityTypeId);
            userFeaturesAccess.setEntityId(firstAction.getEntityId());
            userFeaturesAccess.setUserAccountId(userAccountId);
            userFeaturesAccess.setActionIds(firstAction.getActionList());
            userFeaturesAccess.setIsDeleted(false);
            userFeaturesAccess.setDepartmentTypeId(Constants.DepartmentType.HR_DEPARTMENT);
        }
        UserFeaturesAccess savedAccess = userFeatureAccessRepository.save(userFeaturesAccess);
        return createFeatureAccessResponseForAllDto(savedAccess,timeZone);
    }

    public void validateHrRoleRequest(Long orgId, String accountId) {
        Long accountIdLong;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Organization organization = organizationRepository.findByOrgId(orgId);
        if (organization == null) {
            throw new ValidationFailedException("Organization does not exist");
        }
        UserAccount user=userAccountRepository.findByAccountId(accountIdLong);
        if (!Objects.equals(organization.getOwnerEmail(), user.getEmail())) {
            throw new ValidationFailedException("User does not have access to HR Role");
        }
    }

    public List<UserFeatureAccessResponseForAllDto> getAllHrRoleActions(Long orgId, String accountIds, String timeZone) {
        validateHrRoleRequest(orgId, accountIds);
        List<UserFeatureAccessResponseForAllDto> responseList = new ArrayList<>();
        List<UserFeaturesAccess> userFeaturesAccessList = userFeatureAccessRepository.findByOrgIdAndIsDeleted(orgId, false);
        for (UserFeaturesAccess user : userFeaturesAccessList) {
            responseList.add(createFeatureAccessResponseForAllDto(user, timeZone));
        }
        return responseList;
    }

    public UserFeatureAccessResponseForAllDto updateActionForUser(UpdateHrActionsDto updateDto, String accountIds, String timeZone) {
        if (updateDto.getUserFeatureAccessId() == null) {
            throw new ValidationFailedException("Feature access ID cannot be null.");
        }
        Optional<UserFeaturesAccess> optionalAccess = userFeatureAccessRepository.findById(updateDto.getUserFeatureAccessId());
        if (optionalAccess.isEmpty()) {
            throw new ValidationFailedException("Feature access with ID " + updateDto.getUserFeatureAccessId() + " does not exist.");
        }
        validateHrRoleRequest(optionalAccess.get().getOrgId(), accountIds);
        validateFeatureAction(updateDto.getActionList(),optionalAccess.get().getEntityTypeId(),optionalAccess.get().getEntityId(),optionalAccess.get().getUserAccountId());
        UserFeaturesAccess userAccess = optionalAccess.get();
        userAccess.setActionIds(updateDto.getActionList());
        UserFeaturesAccess saved = userFeatureAccessRepository.save(userAccess);
        return createFeatureAccessResponseForAllDto(saved,timeZone);
    }

    public UserFeaturesAccess validateFeatureAccess(
            Long orgId,
            Integer entityTypeId,
            Long entityId,
            List<Integer> actionIds,
            Long userAccountId) {
        Organization organization = organizationRepository.findByOrgId(orgId);
        if (organization == null) {
            throw new ValidationFailedException("Organization with orgId " + orgId + " does not exist.");
        }
        switch (entityTypeId) {
            case Constants.EntityTypes.ORG:
                if (!Objects.equals(entityId, orgId)) {
                    throw new ValidationFailedException("Entity ID does not match the organization ID.");
                }
                break;
            case Constants.EntityTypes.PROJECT:
                Project project = projectRepository.findByProjectId(entityId);
                if (project == null) {
                    throw new ValidationFailedException("Project with ID " + entityId + " does not exist.");
                }
                break;
            case Constants.EntityTypes.TEAM:
                Team team = teamRepository.findByTeamId(entityId);
                if (team == null) {
                    throw new ValidationFailedException("Team with ID " + entityId + " does not exist.");
                }
                break;
            default:
                throw new ValidationFailedException("Invalid entity type: " + entityTypeId);
        }
        UserAccount userDb = userAccountRepository.findByAccountId(userAccountId);
        if (userDb == null || userDb.getOrgId() == null || !Objects.equals(userDb.getOrgId(), orgId)) {
            throw new ValidationFailedException("User does not belong to the specified organization.");
        }
        if (actionIds == null || actionIds.isEmpty()) {
            throw new ValidationFailedException("ActionIds cannot be null or empty.");
        }
        validateFeatureAction(actionIds,entityTypeId,entityId,userAccountId);
        Optional<UserFeaturesAccess> userFeaturesAccess = userFeatureAccessRepository
                .findByEntityTypeIdAndEntityIdAndUserAccountId(entityTypeId, entityId, userAccountId);

        return userFeaturesAccess.orElse(null);
    }

    public void validateFeatureAction(List<Integer> actionIds, Integer entityTypeId, Long entityId, Long userAccountId) {
        if (actionIds == null || actionIds.isEmpty()) {
            throw new ValidationFailedException("Action list cannot be null or empty.");
        }
        Set<Integer> allowedActions = Set.of(
                Constants.ActionId.MANAGE_LEAVE,
                Constants.ActionId.VIEW_TIMESHEET,
                Constants.ActionId.VIEW_ATTENDENCE,
                Constants.ActionId.MANAGE_GEOFENCE_ADMIN_PANEL,
                Constants.ActionId.VIEW_GEOFENCE_ATTENDENCE
        );

        for (Integer actionId : actionIds) {
            if (!allowedActions.contains(actionId)) {
                throw new ValidationFailedException("Action is not allowed: " + actionId);
            }
            boolean isGeoFenceAction =
                    actionId.equals(Constants.ActionId.MANAGE_GEOFENCE_ADMIN_PANEL) ||
                            actionId.equals(Constants.ActionId.VIEW_GEOFENCE_ATTENDENCE);
            if (isGeoFenceAction && (entityTypeId.equals(Constants.EntityTypes.TEAM) || entityTypeId.equals(Constants.EntityTypes.PROJECT))) {
                throw new ValidationFailedException("GeoFence actions are only allowed at Org level.");
            }
            if (isGeoFenceAction && !entityPreferenceRepository.isGeoFencingEnabledForEntity(Constants.EntityTypes.ORG, entityId)) {
                throw new ValidationFailedException("GeoFence is not Active for the Organization, Please contact super Admin to add GeoFencing for Organization");

            }
        }
        //need to make changes here
        if (actionIds.contains(Constants.ActionId.VIEW_TIMESHEET)) {
            List<Long> teamList = new ArrayList<>();
            switch (entityTypeId) {
                case Constants.EntityTypes.ORG:
                    teamList.addAll(teamRepository.findTeamIdsByOrgId(entityId));
                    break;
                case Constants.EntityTypes.PROJECT:
                    teamList.addAll(teamRepository.findTeamIdByFkProjectIdProjectId(entityId));
                    break;
                case Constants.EntityTypes.TEAM:
                    teamList.add(entityId);
                    break;
                default:
                    throw new ValidationFailedException("Invalid entity type: " + entityTypeId);
            }
            if (!teamList.isEmpty()) {
                addRoleForTeamTimeSheetViewed(teamList, userAccountId);
            }
        }
    }



    public void removeHrRole(RemoveHrRoleDto dto, String accountIds) {
        Optional<UserFeaturesAccess> userFeatureDb = userFeatureAccessRepository.findById(dto.getUserFeatureAccessId());
        if (userFeatureDb.isPresent()) {
            validateHrRoleRequest(userFeatureDb.get().getOrgId(), accountIds);
            UserFeaturesAccess userAccess = userFeatureDb.get();
            userAccess.setIsDeleted(true);
            userFeatureAccessRepository.save(userAccess);
        } else {
            throw new ValidationFailedException("HR role not found for user.");
        }
    }

    public List<ActionResponseDto> getActionsForHrRole(Long orgId, String accountIds) {
        validateHrRoleRequest(orgId, accountIds);
        return createActionResponse(Constants.actionList);
    }

    public UserFeatureAccessResponseDto createFeatureAccessResponse(UserFeaturesAccess userFeaturesAccessDb, String timeZone) {
        UserFeaturesAccess userFeaturesAccess = new UserFeaturesAccess();
        BeanUtils.copyProperties(userFeaturesAccessDb, userFeaturesAccess);
        UserFeatureAccessResponseDto response = new UserFeatureAccessResponseDto();
        response.setUserFeatureAccessId(userFeaturesAccess.getUserFeatureAccessId());
        response.setUserAccountId(userFeaturesAccess.getUserAccountId());
        response.setEntityTypeId(userFeaturesAccess.getEntityTypeId());
        response.setEntityId(userFeaturesAccess.getEntityId());
        response.setOrgId(userFeaturesAccess.getOrgId());
        response.setDepartmentTypeId(userFeaturesAccess.getDepartmentTypeId());
        response.setCreatedDateTime(
                DateTimeUtils.convertServerDateToUserTimezone(userFeaturesAccess.getCreatedDateTime(), timeZone)
        );
        response.setActionList(createActionResponse(userFeaturesAccess.getActionIds()));
        return response;
    }

    public UserFeatureAccessResponseForAllDto createFeatureAccessResponseForAllDto(UserFeaturesAccess userFeaturesAccessDb, String timeZone) {
        UserFeaturesAccess userFeaturesAccess = new UserFeaturesAccess();
        BeanUtils.copyProperties(userFeaturesAccessDb, userFeaturesAccess);
        UserFeatureAccessResponseForAllDto response = new UserFeatureAccessResponseForAllDto();
        response.setUserFeatureAccessId(userFeaturesAccess.getUserFeatureAccessId());
        response.setUserAccountId(userFeaturesAccess.getUserAccountId());
        response.setEntityTypeId(userFeaturesAccess.getEntityTypeId());
        response.setEntityId(userFeaturesAccess.getEntityId());
        response.setEntityName(getEntityName(userFeaturesAccess.getEntityTypeId(),userFeaturesAccess.getEntityId()));
        response.setOrgId(userFeaturesAccess.getOrgId());
        response.setDepartmentTypeId(userFeaturesAccess.getDepartmentTypeId());
        UserAccount user = userAccountRepository.findByAccountId(userFeaturesAccess.getUserAccountId());
        response.setFirstName(user.getFkUserId().getFirstName());
        response.setLastName(user.getFkUserId().getLastName());
        response.setEmail(user.getEmail());
        response.setCreatedDateTime(
                DateTimeUtils.convertServerDateToUserTimezone(userFeaturesAccess.getCreatedDateTime(), timeZone)
        );

        List<Integer> actionList = userFeaturesAccess.getActionIds();
        response.setActionList(createActionResponse(actionList));
        return response;
    }

    public String getEntityName(Integer entityTypeId, Long entityId){
        String entityName;
        switch (entityTypeId) {
            case Constants.EntityTypes.ORG:
                entityName = organizationRepository.findOrganizationNameByOrgId(entityId);
                break;

            case Constants.EntityTypes.PROJECT:
                entityName = projectRepository.findProjectNameByProjectId(entityId);
                break;

            case Constants.EntityTypes.TEAM:
                entityName = teamRepository.findTeamNameByTeamId(entityId);
                break;
            default:
                throw new ValidationFailedException("Invalid entity type: " + entityTypeId);
        }
        return entityName;
    }

    public List<ActionResponseDto> createActionResponse(List<Integer> actionIds) {
        List<ActionResponseDto> actionResponseDtoList = new ArrayList<>();
        for (Integer actionId : actionIds) {
            Action actionDb = actionRepository.findActionByActionId(actionId);
            ActionResponseDto actionResponseDto = new ActionResponseDto();
            actionResponseDto.setActionId(actionDb.getActionId());
            actionResponseDto.setActionDesc(actionDb.getActionDesc());
            actionResponseDto.setActionName(actionDb.getActionName());
            actionResponseDtoList.add(actionResponseDto);
        }
        return actionResponseDtoList;
    }

    public void addRoleForTeamTimeSheetViewed(List<Long> teamList, Long userAccountId) {
        if (teamList == null || teamList.isEmpty()) return;
        List<Integer> roleIdsToCheck = List.of(13, 101, 102);
        int entityTypeTeam = Constants.EntityTypes.TEAM;
        // Find teamIds where access does not exist
        List<Long> insertTeamIds = accessDomainRepository.findTeamIdsWithoutAccessDomainForAccountAndRoles(
                teamList,
                userAccountId,
                entityTypeTeam,
                roleIdsToCheck
        );
        List<AccessDomain> toInsert = new ArrayList<>();
        for (Long teamId : insertTeamIds) {
            AccessDomain ad = new AccessDomain();
            ad.setAccountId(userAccountId);
            ad.setEntityTypeId(entityTypeTeam);
            ad.setEntityId(teamId);
            ad.setRoleId(Constants.RoleTypeForFeatureAccess.TEAM_VIEWER);
            ad.setIsActive(true);
            toInsert.add(ad);
        }
        if (!toInsert.isEmpty()) {
            accessDomainRepository.saveAll(toInsert);
        }
    }

    public Boolean checkHrAccessForGeoFencingAttendence(Long accountIdLong, Long orgId) {
        return userFeatureAccessRepository.existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(Constants.EntityTypes.ORG, orgId, List.of(accountIdLong), Constants.ActionId.VIEW_GEOFENCE_ATTENDENCE);
    }

    public Boolean checkHrAccessForGeoFencingAdminPanel(Long accountIdLong, Long orgId) {
        return userFeatureAccessRepository.existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(Constants.EntityTypes.ORG, orgId, List.of(accountIdLong), Constants.ActionId.MANAGE_GEOFENCE_ADMIN_PANEL);
    }

    public void addRemoveFeatureAccessOfOrg(Long orgId, Long accountId, boolean isDeleted) {
        userFeatureAccessRepository.updateIsDeletedByOrgIdAndAccountId(orgId, accountId);
    }
}
