package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.UserPreferenceDTO;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.NotificationService;
import com.tse.core_application.utils.CommonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Service
public class UserPreferenceService {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private NotificationCategoryRepository notificationCategoryRepository;
    @Autowired
    private NotificationService notificationService;

    /**
     * This method is used to add/update a new preference. The request should only contain the fields that needs to be updated. If the listOfCategoriesId is not getting updated then that should be null in the request
     */
    // add validations for org team
    public UserPreference addUserPreference(UserPreference userPreference, Long userId, String timeZone) throws InvocationTargetException, IllegalAccessException {

        // validation: if any of orgId, teamId, or projectId is specified, all three must be present
        if ((userPreference.getOrgId() != null || userPreference.getTeamId() != null || userPreference.getProjectId() != null)
                && (userPreference.getOrgId() == null || userPreference.getTeamId() == null || userPreference.getProjectId() == null)) {
            throw new IllegalArgumentException("If any of orgId, teamId, or projectId is specified, all three must be present.");
        }

        // validation: if orgId or teamId is provided, check the user is part of that org and team is part of that org
        if (userPreference.getOrgId() != null) {
            if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(userId, userPreference.getOrgId(), true) ||
                    !teamRepository.existsByFkOrgIdOrgIdAndTeamIdAndFkProjectIdProjectIdAndIsDisabled(userPreference.getOrgId(), userPreference.getTeamId(), userPreference.getProjectId(), false)) {
                throw new IllegalArgumentException("User, Team, and Project should be part of the specified organization.");
            }
        }
        Integer preference = userPreference.getUserPreferredReminderNotification();
        if (preference != null) {
            if (preference == 0) {
                userPreference.setUserPreferredReminderNotification(null);
            } else if (preference > com.tse.core_application.constants.Constants.NotificationRemainder.MAX_NOTIFICATION_REMANIDER_DURATION
                    || preference < com.tse.core_application.constants.Constants.NotificationRemainder.MIN_NOTIFICATION_REMANIDER_DURATION) {
                throw new ValidationFailedException(
                        "Reminder notification duration cannot be negative or exceed 1 day"
                );
            }
        }

        Optional<UserPreference> userPreferenceFromDb = userPreferenceRepository.findById(userId);
        UserPreference userPreferenceToSave;

        if (userPreferenceFromDb.isPresent()) {
            userPreferenceToSave = userPreferenceFromDb.get();

            List<Integer> userNotificationCategoryIds = userPreference.getNotificationCategoryIds();
            List<Integer> updatedUserNotificationCategoryIds = new ArrayList<>();
            if (userNotificationCategoryIds != null) {
                Set<Integer> userNotificationCategorySet = new HashSet<>(userNotificationCategoryIds);
                List<Integer> systemLevelCategoryId = notificationCategoryRepository.findSystemLevelCategoryIds();
                if (!userNotificationCategorySet.containsAll(systemLevelCategoryId)) {
                    userNotificationCategorySet.addAll(systemLevelCategoryId);
                }
                updatedUserNotificationCategoryIds.addAll(userNotificationCategorySet);
            }
            if (!updatedUserNotificationCategoryIds.isEmpty()) {
                userPreference.setNotificationCategoryIds(updatedUserNotificationCategoryIds);
            }
            CommonUtils.copyNonNullProperties(userPreference, userPreferenceToSave);
        } else {
            // If the record doesn't exist, create a new one
            userPreferenceToSave = new UserPreference();
            BeanUtils.copyProperties(userPreference, userPreferenceToSave);
        }
        userPreferenceToSave.setUserId(userId);
        UserPreference updatedUserPreference = userPreferenceRepository.save(userPreferenceToSave);
        if (userPreference.getOrgId() != null && userPreference.getTeamId() != null) {
            notificationService.sendUpdateUserPreferenceNotification(userPreference, userId, timeZone);
        }

        return updatedUserPreference;
    }

    /**
     * This method is used to get existing user preference
     */
    public UserPreferenceDTO getUserPreference(Long userId) {
        UserPreferenceDTO userPreferenceDTO = new UserPreferenceDTO();
        Optional<UserPreference> userPreferenceOptional = userPreferenceRepository.findById(userId);
        userPreferenceOptional.ifPresent(userPreference -> BeanUtils.copyProperties(userPreference, userPreferenceDTO));
        return userPreferenceDTO;
    }

    public void editUserPreferenceOnUserRemovalFromEntity(Integer entityTypeId, Long entityId, Long removedUserAccountId) {
        UserAccount userAccountOfRemovedUser = userAccountRepository.findByAccountId(removedUserAccountId);
        UserPreference userPreferenceOfRemovedUser = userPreferenceRepository.findByUserId(userAccountOfRemovedUser.getFkUserId().getUserId());
        if (userPreferenceOfRemovedUser != null) {
            if (entityTypeId.equals(Constants.EntityTypes.ORG)  && Objects.equals(userPreferenceOfRemovedUser.getOrgId(), entityId)) {
                // removal from Org
                if (userAccountOfRemovedUser.getOrgId() != null && userAccountOfRemovedUser.getOrgId().equals(entityId)) {
                    List<UserAccount> userAccounts = userAccountRepository.findByFkUserIdUserIdAndIsActive(userAccountOfRemovedUser.getFkUserId().getUserId(), true);
                    userAccounts.remove(userAccountOfRemovedUser);
                    if (!userAccounts.isEmpty()) {
                        userPreferenceOfRemovedUser.setOrgId(userAccounts.get(0).getOrgId());
                        UserAccount otherUserAccountOfRemovedUser = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(userAccounts.get(0).getOrgId(), userAccountOfRemovedUser.getFkUserId().getUserId(), true);
                        List<Team> teams = teamRepository.findByFkOrgIdOrgId(userAccounts.get(0).getOrgId());
                        for (Team team : teams) {
                            List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, team.getTeamId(), otherUserAccountOfRemovedUser.getAccountId(), true);
                            if (accessDomains != null && !accessDomains.isEmpty()) {
                                userPreferenceOfRemovedUser.setTeamId(team.getTeamId());
                                userPreferenceOfRemovedUser.setProjectId(team.getFkProjectId().getProjectId());
                                break;
                            }
                        }
                    } else {
                        // user not part of any other org
                        userPreferenceOfRemovedUser.setOrgId(null);
                        userPreferenceOfRemovedUser.setTeamId(null);
                        userPreferenceOfRemovedUser.setProjectId(null);
                    }
                }
            }
            else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
                // removal from Team
                if (userPreferenceOfRemovedUser.getTeamId() != null && userPreferenceOfRemovedUser.getTeamId().equals(entityId)) {
                    Team team = teamRepository.findByTeamId(entityId);
                    List<Team> teams = teamRepository.findByFkOrgIdOrgId(team.getFkOrgId().getOrgId());
                    teams.remove(team);
                    if (!teams.isEmpty()) {
                        for (Team t : teams) {
                            List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, t.getTeamId(), removedUserAccountId, true);
                            if (accessDomains != null && !accessDomains.isEmpty()) {
                                // recheck this
                                userPreferenceOfRemovedUser.setTeamId(t.getTeamId());
                                userPreferenceOfRemovedUser.setProjectId(t.getFkProjectId().getProjectId());

                                break;
                            }
                        }
                    } else {
                        // user not part of any other team in that org
                        userPreferenceOfRemovedUser.setTeamId(null);
                    }
                }
            }
            userAccountRepository.save(userAccountOfRemovedUser);
        }
    }

}
