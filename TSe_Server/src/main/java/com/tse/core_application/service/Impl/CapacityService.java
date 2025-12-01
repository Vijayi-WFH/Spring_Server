package com.tse.core_application.service.Impl;

import com.google.firebase.database.utilities.Pair;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.SprintResponseForFilter;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.capacity.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tse.core_application.utils.DateTimeUtils.convertServerDateToUserTimezone;

@Service
public class CapacityService {

    private static final Logger logger = LogManager.getLogger(TaskServiceImpl.class.getName());

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private UserCapacityMetricsRepository userCapacityMetricsRepository;
    @Autowired
    private SprintCapacityMetricsRepository sprintCapacityMetricsRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private MemberDetailsRepository memberDetailsRepository;
    @Autowired
    private LeaveService leaveService;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private MeetingService meetingService;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired AuditService auditService;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    /**
     * method to recalculate the capacities when task estimate is changed in a task that is part of a sprint
     */
    public void recalculateCapacitiesForTaskEstimateChange(Task originalTask, Task updatedTask) {
        if ((originalTask.getFkAccountIdAssigned() == null && updatedTask.getFkAccountIdAssigned() == null)
                || Objects.equals(originalTask.getFkAccountIdAssigned().getAccountId(), updatedTask.getFkAccountIdAssigned().getAccountId())) {
            Integer updatedEstimate = updatedTask.getTaskEstimate() != null ? updatedTask.getTaskEstimate() : 0;
            if (!Objects.equals(originalTask.getTaskEstimate(), updatedEstimate)) {
                UserCapacityMetrics userCapacity = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(updatedTask.getFkTeamId().getTeamId(), updatedTask.getSprintId()
                        , updatedTask.getFkAccountIdAssigned() != null ? updatedTask.getFkAccountIdAssigned().getAccountId() : Constants.UNASSIGNED_ACCOUNT_ID);
                if (userCapacity != null) {
                    Integer newAdjustedValue = updatedEstimate - (originalTask.getTaskEstimate() != null ? originalTask.getTaskEstimate() : 0);
                    userCapacity.setCurrentPlannedCapacity(userCapacity.getCurrentPlannedCapacity() + newAdjustedValue);
                    int percentPlannedCapacityUtilization = 0;
                    if (userCapacity.getLoadedCapacity() > 0) {
                        percentPlannedCapacityUtilization = Math.round((float) userCapacity.getCurrentPlannedCapacity() * 100 / userCapacity.getLoadedCapacity());
                    }
                    userCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

                    userCapacityMetricsRepository.save(userCapacity);
                    updateSprintCapacityMetricsBasedOnTaskEstimate(updatedTask.getSprintId(), newAdjustedValue);
                }
            }
        }
    }

    public void adjustCapacitiesForAccountIdChange(Task originalTask, Task updatedTask) {
        // assumption -- sprint & estimate are not getting modified
        if (!Objects.equals(originalTask.getFkAccountIdAssigned().getAccountId(), updatedTask.getFkAccountIdAssigned().getAccountId())) {
            // Calculate the adjusted task estimate
            Integer taskEstimateAdjustment = calculateTaskEstimateAdjustment(updatedTask);

            // Adjust for original accountIdAssigned
            adjustUserCapacityForAccountChange(originalTask.getFkAccountIdAssigned().getAccountId(), originalTask.getSprintId(), -taskEstimateAdjustment, originalTask.getFkTeamId().getTeamId());

            // Adjust for new accountIdAssigned
            adjustUserCapacityForAccountChange(updatedTask.getFkAccountIdAssigned().getAccountId(), updatedTask.getSprintId(), taskEstimateAdjustment, updatedTask.getFkTeamId().getTeamId());
        }
    }

    private void adjustUserCapacityForAccountChange(Long accountId, Long sprintId, Integer taskEstimateAdjustment, Long teamId) {
        UserCapacityMetrics userCapacity = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(teamId, sprintId, accountId);
        if (userCapacity != null) {
            userCapacity.setCurrentPlannedCapacity(userCapacity.getCurrentPlannedCapacity() + taskEstimateAdjustment);
            userCapacity.setLoadedCapacity((int) (userCapacity.getLoadedCapacity() + (taskEstimateAdjustment * userCapacity.getLoadedCapacityRatio())));
            int percentPlannedCapacityUtilization = 0;
            if (userCapacity.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacity.getCurrentPlannedCapacity() * 100 / userCapacity.getLoadedCapacity());
            }
            userCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            userCapacity.setPercentLoadedCapacityUtilization((int) ((double) userCapacity.getLoadedCapacity() / userCapacity.getTotalCapacity() * 100));
            userCapacityMetricsRepository.save(userCapacity);
        }
    }


    private Integer calculateTaskEstimateAdjustment(Task task) {
        Integer taskEstimate = task.getTaskEstimate() != null ? task.getTaskEstimate() : 0;
        Double userPerceivedPercentage = task.getUserPerceivedPercentageTaskCompleted() != null ? (100 - task.getUserPerceivedPercentageTaskCompleted()) / 100.0 : 1.0;
        return (int) Math.round(taskEstimate * userPerceivedPercentage);
    }

    public void transferCapacitiesBetweenSprints(Task originalTask, Task updatedTask) {
        // Check if the task is being removed from a sprint
        if (originalTask.getSprintId() != null && updatedTask.getSprintId() == null) {
            recalculateCapacitiesForTaskRemovalFromSprint(originalTask, updatedTask);
            return;
        }

        // Proceed if there's actual sprint change
        if (!Objects.equals(originalTask.getSprintId(), updatedTask.getSprintId()) && updatedTask.getSprintId() != null) {
            Sprint originalSprint = sprintRepository.findById(originalTask.getSprintId())
                    .orElseThrow(() -> new EntityNotFoundException("Original Sprint not found"));
            Sprint newSprint = sprintRepository.findById(updatedTask.getSprintId())
                    .orElseThrow(() -> new EntityNotFoundException("New Sprint not found"));

            if (!originalSprint.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) &&
                    !newSprint.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {

                // Calculate the adjusted task estimate for the change
                Integer taskEstimateAdjustment = calculateTaskEstimateAdjustment(updatedTask);

                // Adjust capacities for the original and new sprints and involved user accounts
                UserCapacityMetrics usercapacityOriginalSprint = adjustUserCapacityForSprintChange(originalTask.getFkAccountIdAssigned().getAccountId(), originalTask.getSprintId(), -taskEstimateAdjustment, originalTask.getFkTeamId().getTeamId());
                UserCapacityMetrics usercapacityNewSprint = adjustUserCapacityForSprintChange(updatedTask.getFkAccountIdAssigned().getAccountId(), updatedTask.getSprintId(), taskEstimateAdjustment, updatedTask.getFkTeamId().getTeamId());

                if (usercapacityOriginalSprint != null && usercapacityNewSprint != null) {
                    // Adjust SprintCapacityMetrics for both sprints
                    adjustSprintCapacityMetrics(originalTask.getSprintId(), -taskEstimateAdjustment, usercapacityOriginalSprint.getLoadedCapacityRatio());
                    adjustSprintCapacityMetrics(updatedTask.getSprintId(), taskEstimateAdjustment, usercapacityNewSprint.getLoadedCapacityRatio());
                }
            }
        }
    }

    private UserCapacityMetrics adjustUserCapacityForSprintChange(Long accountId, Long sprintId, Integer taskEstimateAdjustment, Long teamId) {
        UserCapacityMetrics userCapacity = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(teamId, sprintId, accountId);
        if (userCapacity != null) {
            userCapacity.setCurrentPlannedCapacity(userCapacity.getCurrentPlannedCapacity() + taskEstimateAdjustment);
            int percentPlannedCapacityUtilization = 0;
            if (userCapacity.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacity.getCurrentPlannedCapacity() * 100 / userCapacity.getLoadedCapacity());
            }
            userCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            return userCapacityMetricsRepository.save(userCapacity);
        }
        return null;
    }

    private void adjustSprintCapacityMetrics(Long sprintId, Integer taskEstimateAdjustment, Double loadedCapacityRatio) {
        SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprintId);
        if (sprintCapacity != null) {
            sprintCapacity.setCurrentPlannedCapacity(sprintCapacity.getCurrentPlannedCapacity() + taskEstimateAdjustment);
            int percentPlannedCapacityUtilization = 0;
            if (sprintCapacity.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) sprintCapacity.getCurrentPlannedCapacity() * 100 / sprintCapacity.getLoadedCapacity());
            }
            sprintCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            sprintCapacityMetricsRepository.save(sprintCapacity);
        }
    }

    /**
     * Adjusts capacity metrics for both user and sprint when a task is removed from a sprint, decreasing planned and
     * loaded capacities based on the task's estimate.
     */

    public void recalculateCapacitiesForTaskRemovalFromSprint(Task originalTask, Task updatedTask) {
        // This method is called when a task is removed from a sprint, when updatedTask's sprintId would be null
        if (originalTask.getSprintId() != null && updatedTask.getSprintId() == null) {
            // Calculate the adjustment value for the task estimate
            Integer taskEstimateAdjustment = calculateTaskEstimateAdjustment(originalTask);

            // Adjust UserCapacityMetrics for the original accountIdAssigned and sprint
            UserCapacityMetrics userCapacityMetrics = adjustUserCapacityForSprintChange(originalTask.getFkAccountIdAssigned().getAccountId(), originalTask.getSprintId(), -taskEstimateAdjustment, originalTask.getFkTeamId().getTeamId());

            // Adjust SprintCapacityMetrics for the original sprint
            if (userCapacityMetrics != null)
                adjustSprintCapacityMetrics(originalTask.getSprintId(), taskEstimateAdjustment, userCapacityMetrics.getLoadedCapacityRatio());
        }
    }

    /**
     * Updates the capacity metrics for a specific user and the associated sprint when a task is added to a sprint, adjusting planned and loaded capacities.
     */
    public void updateUserAndSprintCapacityMetricsOnAddTaskToSprint(Task task, Long sprintId) {
        if (!task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {

            // handling of reference meetings in task
            updateReferenceMeetingInTaskCapacityOnAddTaskToSprint(task, sprintId);

            // if the task is not assigned to anyone - planned capacity will not be incremented
            UserCapacityMetrics userCapacity;
            if (task.getFkAccountIdAssigned() != null) {
                userCapacity = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(task.getFkTeamId().getTeamId(), sprintId, task.getFkAccountIdAssigned().getAccountId());
            } else {
                userCapacity = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(task.getFkTeamId().getTeamId(), sprintId, com.tse.core_application.constants.Constants.UNASSIGNED_TASK_ACCOUNT_ID_CAPACITY);
            }

            if (userCapacity != null) {
                Integer estimate = (task.getTaskEstimate() != null ? calculateTaskEstimateAdjustment(task) : 0);
                if (estimate > 0) {
                    userCapacity.setCurrentPlannedCapacity(userCapacity.getCurrentPlannedCapacity() + estimate);
//                userCapacity.setLoadedCapacity((int) (userCapacity.getLoadedCapacity() + estimate * userCapacity.getLoadedCapacityRatio()));

                    int percentPlannedCapacityUtilization = 0;
                    if (userCapacity.getLoadedCapacity() > 0) {
                        percentPlannedCapacityUtilization = Math.round((float) userCapacity.getCurrentPlannedCapacity() * 100 / userCapacity.getLoadedCapacity());
                    }
                    userCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

                    userCapacityMetricsRepository.save(userCapacity);
                    updateSprintCapacityMetricsBasedOnTaskEstimate(sprintId, estimate);
                }
            }
        }
    }

    public void updateReferenceMeetingCapacityOnAddTaskToSprint(Long sprintId, int meetingDuration, List<Long> accountIdsOfAttendees) {
        List<UserCapacityMetrics> userCapacities = userCapacityMetricsRepository.findBySprintIdAndAccountIdIn(sprintId, accountIdsOfAttendees);
        int totalUserCapacity = 0;
        for (UserCapacityMetrics userCapacity : userCapacities) {
            userCapacity.setCurrentPlannedCapacity(userCapacity.getCurrentPlannedCapacity() + meetingDuration);
            int percentPlannedCapacityUtilization = 0;
            if (userCapacity.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacity.getCurrentPlannedCapacity() * 100 / userCapacity.getLoadedCapacity());
            }
            userCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            totalUserCapacity += meetingDuration;
        }
        userCapacityMetricsRepository.saveAll(userCapacities);
        updateSprintCapacityMetricsBasedOnTaskEstimate(sprintId, totalUserCapacity);
    }

    public void updateReferenceMeetingInTaskCapacityOnAddTaskToSprint(Task task, Long sprintId) {
        if (sprintId != null && task != null) {
            List<Meeting> meetingsInTask = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());

//            if (task.getMeetingList() != null && !task.getMeetingList().isEmpty()) {
            if (!meetingsInTask.isEmpty()) {
                EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkTeamId().getFkOrgId().getOrgId());

                if (!orgPreference.getMeetingEffortPreferenceId().equals(Constants.MeetingPreferenceEnum.NO_EFFORTS.getMeetingPreferenceId())) {
                    // ToDo: Retrieve this from the task later on
//                    List<Meeting> meetingsInTask = meetingRepository.findByReferenceEntityTypeIdAndReferenceEntityNumber(Constants.EntityTypes.TASK, task.getTaskNumber());
                    Integer effortPreferenceId = orgPreference.getMeetingEffortPreferenceId();

                    for (Meeting meeting : meetingsInTask) {
                        List<Long> accountIdsToUpdate = getAccountIdsPerMeetingPreference(meeting, task, sprintId, effortPreferenceId);
                        updateReferenceMeetingCapacityOnAddTaskToSprint(sprintId, meeting.getDuration(), accountIdsToUpdate);
                    }
                }
            }
        }
    }

    public void updateReferenceMeetingCapacity(Task task, Long sprintId, Meeting meeting) {
        if (sprintId != null && task != null) {
            EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkTeamId().getFkOrgId().getOrgId());
            if (!orgPreference.getMeetingEffortPreferenceId().equals(Constants.MeetingPreferenceEnum.NO_EFFORTS.getMeetingPreferenceId())) {
                Integer effortPreferenceId = orgPreference.getMeetingEffortPreferenceId();

                List<Long> accountIdsToUpdate = getAccountIdsPerMeetingPreference(meeting, task, sprintId, effortPreferenceId);
                updateReferenceMeetingCapacityOnAddTaskToSprint(sprintId, meeting.getDuration(), accountIdsToUpdate);
            }
        }
    }

    public void updateReferenceMeetingCapacityOnRemoveTaskFromSprint(Task task, Long sprintId) {
        if (sprintId != null && task != null) {
            List<Meeting> meetingsInTask = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());

//            if (task.getMeetingList() != null && !task.getMeetingList().isEmpty()) {
            if (meetingsInTask != null) {
                EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkTeamId().getFkOrgId().getOrgId());

                if (!orgPreference.getMeetingEffortPreferenceId().equals(Constants.MeetingPreferenceEnum.NO_EFFORTS.getMeetingPreferenceId())) {
                    // ToDo: Retrieve this from the task later on
//                    List<Meeting> meetingsInTask = meetingRepository.findByReferenceEntityTypeIdAndReferenceEntityNumber(Constants.EntityTypes.TASK, task.getTaskNumber());
                    Integer effortPreferenceId = orgPreference.getMeetingEffortPreferenceId();

                    for (Meeting meeting : meetingsInTask) {
                        List<TimeSheet> recordedEffortList = timeSheetRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.MEETING, meeting.getMeetingId());
                        if (recordedEffortList.isEmpty() && meeting.getActualEndDateTime() == null) {
                            List<Long> accountIdsToUpdate = getAccountIdsPerMeetingPreference(meeting, task, sprintId, effortPreferenceId);
                            updateReferenceMeetingCapacityOnAddTaskToSprint(sprintId, -meeting.getDuration(), accountIdsToUpdate);
                        }
                    }
                }
            }
        }
    }

    public List<Long> getAccountIdsPerMeetingPreference(Meeting meeting, Task referenceTask, Long sprintId, Integer effortPreferenceId) {
        List<Long> accountIdsPerMeetingPreference = new ArrayList<>();

        if (sprintId != null && referenceTask != null) {
            List<Attendee> attendees = meeting.getAttendeeList();
            List<Long> accountIdsOfAttendees = attendees.stream().map(Attendee::getAccountId).collect(Collectors.toList());

            if (Objects.equals(effortPreferenceId, Constants.MeetingPreferenceEnum.ALL_MEETING_EFFORTS.getMeetingPreferenceId())) {
                accountIdsPerMeetingPreference.addAll(accountIdsOfAttendees);
            } else if (Objects.equals(effortPreferenceId, Constants.MeetingPreferenceEnum.ONLY_ASSIGNED_TO_EFFORTS.getMeetingPreferenceId())) {
                if (accountIdsOfAttendees.contains(referenceTask.getFkAccountIdAssigned().getAccountId())) {
                    accountIdsPerMeetingPreference.add(referenceTask.getFkAccountIdAssigned().getAccountId());
                }
            } else if (Objects.equals(effortPreferenceId, Constants.MeetingPreferenceEnum.HYBRID_EFFORTS.getMeetingPreferenceId())) {
                List<Long> accountIdsOfStakeHolders = new ArrayList<>();
                if (referenceTask.getFkAccountIdAssigned() != null)
                    accountIdsOfStakeHolders.add(referenceTask.getFkAccountIdAssigned().getAccountId());
                if (referenceTask.getFkAccountIdMentor1() != null)
                    accountIdsOfStakeHolders.add(referenceTask.getFkAccountIdMentor1().getAccountId());
                if (referenceTask.getFkAccountIdMentor2() != null)
                    accountIdsOfStakeHolders.add(referenceTask.getFkAccountIdMentor2().getAccountId());

                // check which all stakeholders are present as attendee in the meeting
                accountIdsPerMeetingPreference = accountIdsOfStakeHolders.stream()
                        .filter(accountIdsOfAttendees::contains)
                        .collect(Collectors.toList());
            }
        }
        return accountIdsPerMeetingPreference;
    }


    /**
     * Updates the overall capacity metrics for a sprint based on task estimates, recalculating planned and loaded capacities.
     */
    public void updateSprintCapacityMetricsBasedOnTaskEstimate(Long sprintId, Integer taskEstimate) {
        if (taskEstimate != null) {
            SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprintId);

            if (sprintCapacity != null) {
                sprintCapacity.setCurrentPlannedCapacity(sprintCapacity.getCurrentPlannedCapacity() + taskEstimate);
                int percentPlannedCapacityUtilization = 0;
                if (sprintCapacity.getLoadedCapacity() > 0) {
                    percentPlannedCapacityUtilization = Math.round((float) sprintCapacity.getCurrentPlannedCapacity() * 100 / sprintCapacity.getLoadedCapacity());
                }
                sprintCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);
//                int newLoadedCapacity = (int) (sprintCapacity.getLoadedCapacity() + taskEstimate * loadedCapacityRatio);
//                sprintCapacity.setLoadedCapacity(newLoadedCapacity);
//                if (sprintCapacity.getTotalCapacity() > 0) {
//                    sprintCapacity.setPercentLoadedCapacityUtilization(Math.round((float) newLoadedCapacity * 100 / sprintCapacity.getTotalCapacity()));
//                }

                sprintCapacityMetricsRepository.save(sprintCapacity);
            }
        }
    }

    /**
     * This method retrieves detailed capacity metrics for a specified sprint, including comprehensive sprint details
     * and individual user capacities.
     */
    public SprintCapacityDetails getSprintCapacityDetails(Long sprintId, String accountIds, String timeZone) {
        SprintCapacityDetails sprintCapacityDetails = new SprintCapacityDetails();
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        //getting if user is manager
        Boolean isManager = sprintService.hasModifySprintPermission(accountIds, sprint.getEntityId(), sprint.getEntityTypeId());
        SprintCapacityMetrics sprintCapacityMetrics = new SprintCapacityMetrics();

        // Todo: check handling of the case when a user is added/ removed from a team in between of a sprint
        List<UserCapacityMetrics> userCapacities = userCapacityMetricsRepository.findBySprintId(sprintId);

        //getting sprint capacity only if user is manager
        List<Long> sprintMemberAccountId = new ArrayList<>();
        List<Long> absentMemberAccountId = new ArrayList<>();
        if (isManager) {
            sprintMemberAccountId.addAll(userCapacities.stream().map(UserCapacityMetrics::getAccountId).collect(Collectors.toList()));
            List<Integer> teamNonAdminRoleList = new ArrayList<>(Constants.TEAM_NON_ADMIN_ROLE);
            absentMemberAccountId.addAll(accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(sprint.getEntityTypeId(), List.of(sprint.getEntityId()), teamNonAdminRoleList, true));
            absentMemberAccountId.removeAll(sprintMemberAccountId);
            sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(sprintId);
            sprintCapacityDetails.setTotalUnassignedTasksWithNoEstimate(taskRepository.countBySprintIdAndFkAccountIdAssignedAccountIdIsNullAndTaskEstimateIsNull(sprintId));
        }

        List<UserCapacityDetail> userCapacityDetails = userCapacities.stream()
                .map(uc -> {
                    //restricting all user capacities only for manager else only user's own capacity will be returned
                    if (isManager || headerAccountIds.contains(uc.getAccountId())) {
                        Pair<String, String> accountNameAndEmail = userAccountService.getAccountNameAndEmailByAccountId(uc.getAccountId());
                        return new UserCapacityDetail(
                                uc.getAccountId(),
                                uc.getAccountId() != 0 ? accountNameAndEmail.getFirst() : "Unassigned",
                                accountNameAndEmail.getSecond(),
                                uc.getTotalCapacity(),
                                uc.getCurrentPlannedCapacity(),
                                uc.getLoadedCapacity(),
                                uc.getPercentPlannedCapacityUtilization(),
                                uc.getPercentLoadedCapacityUtilization(),
                                uc.getLoadedCapacityRatio(),
                                uc.getTotalWorkingDays(),
                                uc.getWorkMinutes(),
                                uc.getBurnedEfforts(),
                                uc.getTotalEarnedEfforts(),
                                uc.getEarnedEffortsTask(),
                                uc.getEarnedEffortsMeeting(),
                                uc.getMinutesBehindSchedule(),
                                true
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted((uc1, uc2) -> {
                    if (uc1.getAccountName().equals("Unassigned")) {
                        return 1; // uc1 comes after uc2 if uc1 is "Unassigned"
                    } else if (uc2.getAccountName().equals("Unassigned")) {
                        return -1; // uc1 comes before uc2 if uc2 is "Unassigned"
                    } else {
                        return uc1.getAccountName().compareTo(uc2.getAccountName()); // sort by account name
                    }
                })
                .collect(Collectors.toList());

        SprintDetails sprintDetails = new SprintDetails();
        BeanUtils.copyProperties(sprint, sprintDetails);
        if (sprint.getEntityTypeId().equals(Constants.EntityTypes.TEAM)) {
            Team team = teamRepository.findByTeamId(sprint.getEntityId());
            sprintDetails.setEntityName(team.getTeamName());
        }

        Boolean misMatchIndicator = false;
        for(UserCapacityDetail eachUserCapacityDetails : userCapacityDetails) {
            if (eachUserCapacityDetails.getAccountId() == 0 || (!userAccountRepository.findByAccountId(eachUserCapacityDetails.getAccountId()).getIsActive() && eachUserCapacityDetails.getCurrentPlannedCapacity() == 0) || (eachUserCapacityDetails.getLoadedCapacityRatio() == 0 && eachUserCapacityDetails.getCurrentPlannedCapacity() == 0)) {
                continue;
            }
            if (eachUserCapacityDetails.getLoadedCapacityRatio() == 0 || eachUserCapacityDetails.getPercentCapacityUtilization() < 97 || eachUserCapacityDetails.getPercentCapacityUtilization() > 100) {
                misMatchIndicator = true;
                break;
            }
        }

        if (isManager && absentMemberAccountId != null && !absentMemberAccountId.isEmpty()) {
            for (Long accountId : absentMemberAccountId) {
                Pair<String, String> accountNameAndEmail = userAccountService.getAccountNameAndEmailByAccountId(accountId);
                UserCapacityDetail userCapacityDetail = new UserCapacityDetail();
                userCapacityDetail.setAccountId(accountId);
                userCapacityDetail.setAccountName(accountNameAndEmail.getFirst());
                userCapacityDetail.setEmail(accountNameAndEmail.getSecond());
                userCapacityDetail.setIsPresent(false);
                userCapacityDetails.add(userCapacityDetail);
            }
        }
        sprintCapacityDetails.setCapacityMismatchIndicator(misMatchIndicator);
        sprintDetails.setSprintActEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprintDetails.getSprintActEndDate(), timeZone));
        sprintDetails.setSprintActStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprintDetails.getSprintActStartDate(), timeZone));
        sprintDetails.setSprintExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprintDetails.getSprintExpEndDate(), timeZone));
        sprintDetails.setSprintExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprintDetails.getSprintExpStartDate(), timeZone));
        sprintDetails.setCapacityAdjustmentDeadline(DateTimeUtils.convertServerDateToUserTimezone(sprintDetails.getCapacityAdjustmentDeadline(), timeZone));
        sprintCapacityDetails.setSprintDetails(sprintDetails);
        if (sprintCapacityMetrics != null) {
            BeanUtils.copyProperties(sprintCapacityMetrics, sprintCapacityDetails);
        }

        sprintCapacityDetails.setUserCapacities(userCapacityDetails);

        if (sprintCapacityDetails != null && sprintCapacityDetails.getUserCapacities() != null) {
            sprintCapacityDetails.getUserCapacities().sort(Comparator
                .comparing((UserCapacityDetail u) -> {
                    Boolean present = u.getIsPresent();
                    if (present == null) return 2;
                    return present ? 0 : 1;
                })
                .thenComparing(UserCapacityDetail::getAccountName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            );
        }

        return sprintCapacityDetails;
    }

    /**
     * This method retrieves capacity metrics and detailed task assignments for a specified user within a given sprint
     */
    public UserSprintCapacityDetails getUserSprintCapacityDetails(Long accountId, Long sprintId, String timeZone) {
        Optional<UserCapacityMetrics> userCapacityOptional = userCapacityMetricsRepository.findBySprintIdAndAccountId(sprintId, accountId);
        List<Task> tasksAssigned = taskRepository.findAllTaskForUserCapacity(sprintId, accountId, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, Constants.TaskTypes.PARENT_TASK);

        UserCapacityMetrics userCapacity;
        UserSprintCapacityDetails userSprintCapacityDetails = new UserSprintCapacityDetails();
        if (userCapacityOptional.isPresent()) {
            userCapacity = userCapacityOptional.get();
            if (Objects.equals(userCapacity.getAccountId(), Constants.UNASSIGNED_ACCOUNT_ID)) {
                tasksAssigned.addAll(taskRepository.findAllTaskForUnassignedCapacity(sprintId, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, Constants.TaskTypes.PARENT_TASK));
            }
            List<CapacityTaskDetails> taskDetails = tasksAssigned.stream().map(task -> {
                CapacityTaskDetails details = new CapacityTaskDetails();
                BeanUtils.copyProperties(task, details);
                details.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                details.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                details.setTeamId(task.getFkTeamId().getTeamId());

                List<Meeting> referenceMeetings = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());
                EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkTeamId().getFkOrgId().getOrgId());
                details.setMeetingEffortPreferenceId(orgPreference.getMeetingEffortPreferenceId());
                //                if (task.getMeetingList() != null) {
                if (!referenceMeetings.isEmpty()) {
//                    List<Meeting> referenceMeetings = meetingRepository.findByMeetingIdsIn(task.getMeetingList());
                    List<ReferenceMeetingDetail> responses = new ArrayList<>();
                    for (Meeting meeting : referenceMeetings) {
                        ReferenceMeetingDetail referenceMeetingDetail = new ReferenceMeetingDetail();
                        BeanUtils.copyProperties(meeting, referenceMeetingDetail);
                        referenceMeetingDetail.setStartDateTime(convertServerDateToUserTimezone(meeting.getStartDateTime(), timeZone));
                        referenceMeetingDetail.setEndDateTime(convertServerDateToUserTimezone(meeting.getEndDateTime(), timeZone));
                        referenceMeetingDetail.setCreatedDateTime(convertServerDateToUserTimezone(meeting.getCreatedDateTime(), timeZone));
                        referenceMeetingDetail.setLastUpdatedDateTime(convertServerDateToUserTimezone(meeting.getLastUpdatedDateTime(), timeZone));
                        referenceMeetingDetail.setMeetingType(meetingService.setMeetingType(meeting.getMeetingTypeIndicator()));
                        responses.add(referenceMeetingDetail);
                    }
                    details.setReferenceMeetingList(responses);
                }

                return details;
            }).collect(Collectors.toList());

            Pair<String, String> userAccountNameAndEmail = userAccountService.getAccountNameAndEmailByAccountId(accountId);
            UserCapacityDetail userCapacityDetail = new UserCapacityDetail(
                    accountId,
                    userAccountNameAndEmail.getFirst(),
                    userAccountNameAndEmail.getSecond(),
                    userCapacity.getTotalCapacity(),
                    userCapacity.getCurrentPlannedCapacity(),
                    userCapacity.getLoadedCapacity(),
                    userCapacity.getPercentPlannedCapacityUtilization(),
                    userCapacity.getPercentLoadedCapacityUtilization(),
                    userCapacity.getLoadedCapacityRatio(),
                    userCapacity.getTotalWorkingDays(),
                    userCapacity.getWorkMinutes(),
                    userCapacity.getBurnedEfforts(),
                    userCapacity.getTotalEarnedEfforts(),
                    userCapacity.getEarnedEffortsTask(),
                    userCapacity.getEarnedEffortsMeeting(),
                    userCapacity.getMinutesBehindSchedule(),
                    true
            );

            userSprintCapacityDetails.setSprintId(sprintId);
            userSprintCapacityDetails.setUserCapacityDetails(userCapacityDetail);
            userSprintCapacityDetails.setTaskDetails(taskDetails);
        }

        return userSprintCapacityDetails;
    }

    /**
     * Processes a bulk update for loaded capacity ratios for users within a specified sprint, recalculating and updating
     * individual user capacities and the overall sprint capacity metrics based on the changes.
     */
    public void updateLoadedCapacityRatios(LoadedCapacityRatioUpdateRequest request) {
        int totalLoadedCapacityChange = 0;
        Optional<Sprint> optionalSprint = sprintRepository.findById(request.getSprintId());
        if (optionalSprint.isEmpty()) {
            throw new ValidationFailedException("Sprint not found");
        }
        if (Objects.equals(optionalSprint.get().getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
            throw new ValidationFailedException("Sprint is already completed");
        }

        for (UserLoadedCapacityUpdate update : request.getUpdates()) {
            UserCapacityMetrics userCapacity = userCapacityMetricsRepository
                    .findBySprintIdAndAccountId(request.getSprintId(), update.getAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("User capacity metrics not found for accountId " + update.getAccountId()));

            // Calculate the change for this user and update
            int oldLoadedCapacity = userCapacity.getLoadedCapacity();
            userCapacity.setLoadedCapacityRatio(update.getNewLoadedCapacityRatio());
            userCapacity.setLoadedCapacity(calculateLoadedCapacity(userCapacity.getTotalCapacity(), update.getNewLoadedCapacityRatio()));
            if (userCapacity.getTotalCapacity() > 0) {
                userCapacity.setPercentLoadedCapacityUtilization((int) Math.round(((float) userCapacity.getLoadedCapacity() * 100) / userCapacity.getTotalCapacity()));
            }

            int percentPlannedCapacityUtilization = 0;
            if (userCapacity.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacity.getCurrentPlannedCapacity() * 100 / userCapacity.getLoadedCapacity());
            }
            userCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            int changeInLoadedCapacity = userCapacity.getLoadedCapacity() - oldLoadedCapacity;

            userCapacityMetricsRepository.save(userCapacity);

            totalLoadedCapacityChange += changeInLoadedCapacity;
        }

        // recalculate and update SprintCapacityMetrics
        if (totalLoadedCapacityChange != 0) {
            updateSprintCapacityMetricsOnLoadedCapacityUpdate(request.getSprintId(), totalLoadedCapacityChange);
        }
    }

    private Integer calculateLoadedCapacity(Double currentPlannedCapacity, Double loadedCapacityRatio) {
        return (int) (currentPlannedCapacity * loadedCapacityRatio);
    }

    private Integer calculatePercentLoadedCapacityUtilization(Integer loadedCapacity, Integer totalCapacity) {
        if (totalCapacity != null && totalCapacity > 0) {
            return Math.round((float) loadedCapacity * 100 / totalCapacity);
        }
        return 0; // Return 0 or another appropriate value if totalCapacity is 0 or null
    }

    /**
     * Updates the loaded capacity and recalculates the percent loaded capacity utilization for a given sprint,
     * applying the specified change in loaded capacity and ensuring values are rounded to the nearest whole number.
     */
    private void updateSprintCapacityMetricsOnLoadedCapacityUpdate(Long sprintId, int totalLoadedCapacityChange) {
        SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprintId);

        sprintCapacity.setLoadedCapacity(sprintCapacity.getLoadedCapacity() + totalLoadedCapacityChange);

        // Recalculate percentLoadedCapacityUtilization based on the updated loaded capacity
        if (sprintCapacity.getTotalCapacity() > 0) {
            sprintCapacity.setPercentLoadedCapacityUtilization(
                    (int) Math.round((float) sprintCapacity.getLoadedCapacity() * 100 / sprintCapacity.getTotalCapacity()));
        }

        int percentPlannedCapacityUtilization = 0;
        if (sprintCapacity.getLoadedCapacity() > 0) {
            percentPlannedCapacityUtilization = Math.round((float) sprintCapacity.getCurrentPlannedCapacity() * 100 / sprintCapacity.getLoadedCapacity());
        }
        sprintCapacity.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

        sprintCapacityMetricsRepository.save(sprintCapacity);
    }

    /**
     * Updates capacity metrics for all team members associated with a newly created sprint.
     * This includes creating UserCapacityMetrics records based on each member's work capacity or a value retrieved based on the orgPreference (or default).
     */
    private List<UserCapacityMetrics> updateTeamMemberCapacitiesOnSprintCreation(Sprint savedSprint, Set<LocalDate> holidaysAndOffDays, LocalTime officeStartTime, LocalTime officeEndTime, Integer workingMinutes,Long fetchLoadFactorOfSprint) {
        Map<Long, Double> capacityMap;
        if (fetchLoadFactorOfSprint != null) {
            Sprint sprintdb = sprintRepository.findBySprintId(fetchLoadFactorOfSprint);

            if (sprintdb == null) {
                throw new ValidationFailedException("Given Sprint for fetch Load Factor does not exist!!");
            }

            if (!Objects.equals(sprintdb.getEntityId(), savedSprint.getEntityId())) {
                throw new ValidationFailedException("Sprint" + sprintdb.getSprintTitle()
                        + " doesn't belong to team " + savedSprint.getSprintTitle());
            }

            capacityMap = userCapacityMetricsRepository.findBySprintId(fetchLoadFactorOfSprint)
                    .stream()
                    .collect(Collectors.toMap(
                            UserCapacityMetrics::getAccountId,
                            UserCapacityMetrics::getLoadedCapacityRatio,
                            (existing, replacement) -> replacement));
        } else capacityMap = new HashMap<>();

        List<UserCapacityMetrics> userCapacityMetricsList = new ArrayList<>();
        if (savedSprint.getEntityTypeId().equals(Constants.EntityTypes.TEAM)) {
            Team team = teamRepository.findById(savedSprint.getEntityId())
                    .orElseThrow(() -> new EntityNotFoundException("Team not found"));

            List<AccountId> memberAccountIds = accessDomainService.getAllActiveDistinctTeamMembers(team.getTeamId());
            List<Long> allActiveMembersInTeam = memberAccountIds.stream()
                    .map(AccountId::getAccountId)
                    .collect(Collectors.toList());

            List<MemberDetails> allMembers = memberDetailsRepository
                    .findByEntityTypeIdAndEntityIdAndAccountIdIn(Constants.EntityTypes.TEAM, team.getTeamId(), allActiveMembersInTeam);

            Map<Long, MemberDetails> memberDetailsMap = allMembers.stream()
                    .collect(Collectors.toMap(MemberDetails::getAccountId, Function.identity(), (existing, replacement) -> existing));

            allActiveMembersInTeam.forEach(accountId -> {
                MemberDetails memberDetails = memberDetailsMap.get(accountId);
                Set<LocalDate> userLeaves = leaveService.getUserApprovedLeaves(accountId, "0");
                double workingDays = calculateWorkingDaysInSprint(savedSprint.getSprintExpStartDate(), savedSprint.getSprintExpEndDate(), holidaysAndOffDays, userLeaves, officeStartTime, officeEndTime, workingMinutes);

                UserCapacityMetrics capacityMetrics = new UserCapacityMetrics();
                capacityMetrics.setOrgId(team.getFkOrgId().getOrgId());
                capacityMetrics.setProjectId(team.getFkProjectId().getProjectId());
                capacityMetrics.setTeamId(team.getTeamId());
                capacityMetrics.setSprintId(savedSprint.getSprintId());
                capacityMetrics.setAccountId(accountId);
                capacityMetrics.setTotalWorkingDays(workingDays);
                capacityMetrics.setLoadedCapacityRatio(
                        capacityMap.getOrDefault(capacityMetrics.getAccountId(), 1.0)
                );
                int workMinutes = memberDetails != null ? memberDetails.getWorkMinutes() :
                        entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(team.getFkOrgId().getOrgId()).getSecond();
                capacityMetrics.setTotalCapacity(roundWorkTime(workingDays, workMinutes));
                capacityMetrics.setLoadedCapacity((int) (capacityMetrics.getTotalCapacity() * capacityMetrics.getLoadedCapacityRatio()));
                if (capacityMetrics.getTotalCapacity() > 0) {
                    capacityMetrics.setPercentLoadedCapacityUtilization((int) Math.round(((float) (capacityMetrics.getLoadedCapacity() * 100) / capacityMetrics.getTotalCapacity())));
                }
                capacityMetrics.setWorkMinutes(workMinutes);
                userCapacityMetricsList.add(capacityMetrics);
            });

            // create one user capacity metrics for the unassigned tasks
            UserCapacityMetrics userCapacityMetrics = new UserCapacityMetrics();
            userCapacityMetrics.setOrgId(team.getFkOrgId().getOrgId());
            userCapacityMetrics.setProjectId(team.getFkProjectId().getProjectId());
            userCapacityMetrics.setTeamId(team.getTeamId());
            userCapacityMetrics.setSprintId(savedSprint.getSprintId());
            userCapacityMetrics.setAccountId(com.tse.core_application.constants.Constants.UNASSIGNED_TASK_ACCOUNT_ID_CAPACITY);
            userCapacityMetricsList.add(userCapacityMetrics);
        }
        return userCapacityMetricsRepository.saveAll(userCapacityMetricsList);
    }

    /**
     * Calculate the total Working Days in a sprint based on sprint start and end date and holidaysOffDays and userLeaves
     */
    private Double calculateWorkingDaysInSprint(LocalDateTime sprintStartDate, LocalDateTime sprintEndDate, Set<LocalDate> holidaysAndOffDays, Set<LocalDate> userLeaves, LocalTime officeStartTime, LocalTime officeEndTime, Integer officeMinutes) {
        //calculating working minutes on start and end day of sprint
        long workingMinutesOnStartAndEndDay = 0L;

        if (!Objects.equals(sprintStartDate.toLocalDate(), sprintEndDate.toLocalDate())) {
            if (!holidaysAndOffDays.contains(sprintStartDate.toLocalDate()) && !userLeaves.contains(sprintStartDate.toLocalDate()) && !sprintStartDate.toLocalTime().isAfter(officeEndTime)) {
                workingMinutesOnStartAndEndDay += updateWorkingMinutesForStartEndDay(sprintStartDate.toLocalTime(), officeEndTime, officeMinutes);
            }
            if (!holidaysAndOffDays.contains(sprintEndDate.toLocalDate()) && !userLeaves.contains(sprintEndDate.toLocalDate()) && !sprintEndDate.toLocalTime().isBefore(officeStartTime)) {
                workingMinutesOnStartAndEndDay += updateWorkingMinutesForStartEndDay(officeStartTime, sprintEndDate.toLocalTime(), officeMinutes);
            }
        } else {
            workingMinutesOnStartAndEndDay += updateWorkingMinutesForStartEndDay(!sprintStartDate.toLocalTime().isBefore(officeStartTime) ? sprintStartDate.toLocalTime() : officeStartTime,
                    !sprintEndDate.toLocalTime().isAfter(officeEndTime) ? sprintEndDate.toLocalTime() : officeEndTime, officeMinutes);
        }

        //starting total days with working minutes on start and end date / office hours
        double totalDays = (double) workingMinutesOnStartAndEndDay / officeMinutes;

        LocalDate date = sprintStartDate.toLocalDate().plusDays(1);
        while (!date.isAfter(sprintEndDate.toLocalDate().minusDays(1))) {
            if (!holidaysAndOffDays.contains(date) && !userLeaves.contains(date)) {
                totalDays++;
            }
            date = date.plusDays(1);
        }
        return totalDays;
    }


    /**
     * Calculates and stores capacity metrics for a sprint based on the total capacity of its team members.
     */
    public void calculateAndStoreSprintCapacityOnSprintCreation(Sprint savedSprint,Long fetchLoadFactorOfSprint) {
        if (savedSprint.getEntityTypeId().equals(Constants.EntityTypes.TEAM)) {
            Team team = teamRepository.findById(savedSprint.getEntityId()).orElseThrow(() -> new EntityNotFoundException("Team not found"));

            EntityPreference entityPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId());
            Set<LocalDate> holidaysAndOffDays = entityPreferenceService.getHolidaysAndOffDaysForEntityPreference(entityPreference, savedSprint.getSprintExpStartDate().toLocalDate(), savedSprint.getSprintExpEndDate().toLocalDate());
            List<UserCapacityMetrics> userCapacities = updateTeamMemberCapacitiesOnSprintCreation(savedSprint, holidaysAndOffDays, entityPreference.getOfficeHrsStartTime(), entityPreference.getOfficeHrsEndTime(), entityPreference.getMinutesToWorkDaily(), fetchLoadFactorOfSprint);

            if (userCapacities.isEmpty()) {
                throw new IllegalStateException("No team members found for calculating sprint capacity.");
            }

            int totalLoadedCapacity = 0;
            Double totalSprintCapacity = 0.0, totalWorkingDays = 0.0;
            for (UserCapacityMetrics userCapacityMetrics : userCapacities) {
                totalSprintCapacity += userCapacityMetrics.getTotalCapacity();
                totalLoadedCapacity += userCapacityMetrics.getLoadedCapacity();
                totalWorkingDays = calculateWorkingDaysInSprint(savedSprint.getSprintExpStartDate(), savedSprint.getSprintExpEndDate(), holidaysAndOffDays, new HashSet<>(), entityPreference.getOfficeHrsStartTime(), entityPreference.getOfficeHrsEndTime(), entityPreference.getMinutesToWorkDaily());
            }
            UserCapacityMetrics sample = userCapacities.get(0);
            SprintCapacityMetrics sprintCapacity = new SprintCapacityMetrics();
            sprintCapacity.setOrgId(sample.getOrgId());
            sprintCapacity.setProjectId(sample.getProjectId());
            sprintCapacity.setTeamId(sample.getTeamId());
            sprintCapacity.setSprintId(savedSprint.getSprintId());
            sprintCapacity.setTotalCapacity(totalSprintCapacity);
            sprintCapacity.setTotalWorkingDays(totalWorkingDays);
            sprintCapacity.setLoadedCapacity(totalLoadedCapacity);
            if (sprintCapacity.getTotalCapacity() > 0) {
                sprintCapacity.setPercentLoadedCapacityUtilization((int) Math.round(((float) sprintCapacity.getLoadedCapacity() * 100) / sprintCapacity.getTotalCapacity()));
            }
            sprintCapacityMetricsRepository.save(sprintCapacity);
        }
    }

    public void handleSprintChange(Task originalTask, Task updatedTask) {
        if (updatedTask.getSprintId() == null) {
            if (ifRemoveCapacityAllowed(originalTask)) {
                removeTaskFromSprintCapacityAdjustment(originalTask);
            }
        } else if (originalTask.getSprintId() != null) {
            if (ifRemoveCapacityAllowed(originalTask)) {
                removeTaskFromSprintCapacityAdjustment(originalTask);
            }
            addTaskToSprintCapacityAdjustment(updatedTask);
        } else {
            addTaskToSprintCapacityAdjustment(updatedTask);
        }
    }

    public void handleAccountChange(Task originalTask, Task updatedTask) {
        removeAccountFromTask(originalTask);
        addAccountToTask(updatedTask);
    }

    public void handleEstimateChange(Task originalTask, Task updatedTask) {
        recalculateCapacitiesForTaskEstimateChange(originalTask, updatedTask);
    }

    public void removeTaskFromSprintCapacityAdjustment(Task task) {
        if (task.getSprintId() == null) {
            throw new ValidationFailedException("Work Item is not a part of any Sprint");
        }

        Long accountId = task.getFkAccountIdAssigned() != null ? task.getFkAccountIdAssigned().getAccountId() : com.tse.core_application.constants.Constants.UNASSIGNED_TASK_ACCOUNT_ID_CAPACITY;
        UserCapacityMetrics userCapacityMetrics = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(
                task.getFkTeamId().getTeamId(), task.getSprintId(), accountId);

        if (userCapacityMetrics != null) {
            Integer adjustedTaskEstimate = calculateTaskEstimateAdjustment(task);
            // Todo: there seems to be a mistake the adjustedEstimate should return the remaining estimate and not the utilized estimate
            userCapacityMetrics.setCurrentPlannedCapacity(userCapacityMetrics.getCurrentPlannedCapacity() - adjustedTaskEstimate);

            int percentPlannedCapacityUtilization = 0;
            if (userCapacityMetrics.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacityMetrics.getCurrentPlannedCapacity() * 100 / userCapacityMetrics.getLoadedCapacity());
            }
            userCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);
            userCapacityMetricsRepository.save(userCapacityMetrics);

            SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(task.getSprintId());
            if (sprintCapacityMetrics != null) {
                sprintCapacityMetrics.setCurrentPlannedCapacity(sprintCapacityMetrics.getCurrentPlannedCapacity() - adjustedTaskEstimate);

                percentPlannedCapacityUtilization = 0;
                if (sprintCapacityMetrics.getLoadedCapacity() > 0) {
                    percentPlannedCapacityUtilization = Math.round((float) sprintCapacityMetrics.getCurrentPlannedCapacity() * 100 / sprintCapacityMetrics.getLoadedCapacity());
                }
                sprintCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);
                sprintCapacityMetricsRepository.save(sprintCapacityMetrics);
            }
        }

        // Reference Meeting Handling
        updateReferenceMeetingCapacityOnRemoveTaskFromSprint(task, task.getSprintId());
    }

    private void addTaskToSprintCapacityAdjustment(Task task) {
        Long accountId = task.getFkAccountIdAssigned() != null ? task.getFkAccountIdAssigned().getAccountId() : 0;
        UserCapacityMetrics userCapacityMetrics = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(
                task.getFkTeamId().getTeamId(), task.getSprintId(), accountId);
        if (userCapacityMetrics != null) {
            int adjustedEstimate = calculateTaskEstimateAdjustment(task);

            userCapacityMetrics.setCurrentPlannedCapacity(userCapacityMetrics.getCurrentPlannedCapacity() + adjustedEstimate);

            int percentPlannedCapacityUtilization = 0;
            if (userCapacityMetrics.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacityMetrics.getCurrentPlannedCapacity() * 100 / userCapacityMetrics.getLoadedCapacity());
            }
            userCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            userCapacityMetricsRepository.save(userCapacityMetrics);

            SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(task.getSprintId());
            if (sprintCapacityMetrics != null) {
                sprintCapacityMetrics.setCurrentPlannedCapacity(sprintCapacityMetrics.getCurrentPlannedCapacity() + adjustedEstimate);

                percentPlannedCapacityUtilization = 0;
                if (sprintCapacityMetrics.getLoadedCapacity() > 0) {
                    percentPlannedCapacityUtilization = Math.round((float) sprintCapacityMetrics.getCurrentPlannedCapacity() * 100 / sprintCapacityMetrics.getLoadedCapacity());
                }
                sprintCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

                sprintCapacityMetricsRepository.save(sprintCapacityMetrics);
            }
        }
    }


    private void removeAccountFromTask(Task task) {
        Long accountId = task.getFkAccountIdAssigned() != null ? task.getFkAccountIdAssigned().getAccountId() : 0;
        UserCapacityMetrics userCapacityMetrics = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(
                task.getFkTeamId().getTeamId(), task.getSprintId(), accountId);
        if (userCapacityMetrics != null) {
            int adjustedEstimate = calculateTaskEstimateAdjustment(task);

            userCapacityMetrics.setCurrentPlannedCapacity(userCapacityMetrics.getCurrentPlannedCapacity() - adjustedEstimate);

            int percentPlannedCapacityUtilization = 0;
            if (userCapacityMetrics.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacityMetrics.getCurrentPlannedCapacity() * 100 / userCapacityMetrics.getLoadedCapacity());
            }
            userCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

            SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(task.getSprintId());
            if (sprintCapacityMetrics != null) {
                sprintCapacityMetrics.setCurrentPlannedCapacity(sprintCapacityMetrics.getCurrentPlannedCapacity() - adjustedEstimate);

                percentPlannedCapacityUtilization = 0;
                if (sprintCapacityMetrics.getLoadedCapacity() > 0) {
                    percentPlannedCapacityUtilization = Math.round((float) sprintCapacityMetrics.getCurrentPlannedCapacity() * 100 / sprintCapacityMetrics.getLoadedCapacity());
                }
                sprintCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

                sprintCapacityMetricsRepository.save(sprintCapacityMetrics);
            }
        }
    }


    private void addAccountToTask(Task task) {
        Long accountId = task.getFkAccountIdAssigned() != null ? task.getFkAccountIdAssigned().getAccountId() : 0;
        UserCapacityMetrics userCapacityMetrics = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(
                task.getFkTeamId().getTeamId(), task.getSprintId(), accountId);
        if (userCapacityMetrics != null) {
            int adjustedEstimate = calculateTaskEstimateAdjustment(task);

            userCapacityMetrics.setCurrentPlannedCapacity(userCapacityMetrics.getCurrentPlannedCapacity() + adjustedEstimate);
            int percentPlannedCapacityUtilization = 0;
            if (userCapacityMetrics.getLoadedCapacity() > 0) {
                percentPlannedCapacityUtilization = Math.round((float) userCapacityMetrics.getCurrentPlannedCapacity() * 100 / userCapacityMetrics.getLoadedCapacity());
            }
            userCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);
            userCapacityMetricsRepository.save(userCapacityMetrics);

            SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(task.getSprintId());
            if (sprintCapacityMetrics != null) {
                sprintCapacityMetrics.setCurrentPlannedCapacity(sprintCapacityMetrics.getCurrentPlannedCapacity() + adjustedEstimate);

                percentPlannedCapacityUtilization = 0;
                if (sprintCapacityMetrics.getLoadedCapacity() > 0) {
                    percentPlannedCapacityUtilization = Math.round((float) sprintCapacityMetrics.getCurrentPlannedCapacity() * 100 / sprintCapacityMetrics.getLoadedCapacity());
                }
                sprintCapacityMetrics.setPercentPlannedCapacityUtilization(percentPlannedCapacityUtilization);

                sprintCapacityMetricsRepository.save(sprintCapacityMetrics);
            }
        }
    }

    /** method to update the total capacity and loaded capacity when dates of a given sprint is modified */
    public void recalculateCapacitiesForSprint(Sprint sprint, EntityPreference entityPreference,Long fetchLoadFactorOfSprint) {
        if (sprint.getEntityTypeId().equals(Constants.EntityTypes.TEAM)) {
            Team team = teamRepository.findById(sprint.getEntityId()).orElseThrow(() -> new EntityNotFoundException("Team not found"));

            List<UserCapacityMetrics> userCapacityMetricsList = userCapacityMetricsRepository.findBySprintId(sprint.getSprintId());
            userCapacityMetricsList = userCapacityMetricsList.stream()
                    .filter(userCapacityMetrics -> userCapacityMetrics.getAccountId() != com.tse.core_application.constants.Constants.UNASSIGNED_TASK_ACCOUNT_ID_CAPACITY)
                    .collect(Collectors.toList());
            if (entityPreference == null) entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId()).get();
            Set<LocalDate> holidaysAndOffDays = entityPreferenceService.getHolidaysAndOffDaysForEntityPreference(entityPreference, sprint.getSprintExpStartDate().toLocalDate(), sprint.getSprintExpEndDate().toLocalDate());

            List<AccountId> memberAccountIds = accessDomainService.getAllActiveDistinctTeamMembers(team.getTeamId());
            List<Long> allActiveMembersInTeam = memberAccountIds.stream()
                    .map(AccountId::getAccountId)
                    .collect(Collectors.toList());
            List<MemberDetails> allMembers = memberDetailsRepository
                    .findByEntityTypeIdAndEntityIdAndAccountIdIn(Constants.EntityTypes.TEAM, team.getTeamId(), allActiveMembersInTeam);

            Map<Long, MemberDetails> memberDetailsMap = allMembers.stream()
                    .collect(Collectors.toMap(MemberDetails::getAccountId, Function.identity(), (existing, replacement) -> existing));
            Map<Long, Double> capacityMap;
            if (fetchLoadFactorOfSprint != null) {
                Sprint sprintdb = sprintRepository.findBySprintId(fetchLoadFactorOfSprint);

                if (sprintdb == null) {
                    throw new ValidationFailedException("Given Sprint for Fetch Load Factor does not exist!!");
                }

                if (!Objects.equals(sprintdb.getEntityId(), sprint.getEntityId())) {
                    throw new ValidationFailedException("Sprint " + sprintdb.getSprintTitle()
                            + " doesn't belong to team " + sprint.getSprintTitle());
                }

                capacityMap = userCapacityMetricsRepository.findBySprintId(fetchLoadFactorOfSprint)
                        .stream()
                        .collect(Collectors.toMap(
                                UserCapacityMetrics::getAccountId,
                                UserCapacityMetrics::getLoadedCapacityRatio,
                                (existing, replacement) -> replacement));
            } else capacityMap = new HashMap<>();
            // Calculate total capacity and loaded capacity for the updated sprint
            Double totalSprintCapacity = 0.0;
            int totalLoadedCapacity = 0;
            for (UserCapacityMetrics userCapacityMetrics : userCapacityMetricsList) {
                MemberDetails memberDetails = memberDetailsMap.get(userCapacityMetrics.getAccountId());
                Set<LocalDate> userLeaves = leaveService.getUserApprovedLeaves(userCapacityMetrics.getAccountId(), "0");
                Double totalWorkingDays = calculateWorkingDaysInSprint(sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate(), holidaysAndOffDays, userLeaves, entityPreference.getOfficeHrsStartTime(), entityPreference.getOfficeHrsEndTime(), entityPreference.getMinutesToWorkDaily());

                int workMinutes = memberDetails != null ? memberDetails.getWorkMinutes()
                        : entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(team.getFkOrgId().getOrgId()).getSecond();

                // Update total capacity based on the total working days and work minutes
                userCapacityMetrics.setWorkMinutes(workMinutes);
                userCapacityMetrics.setTotalWorkingDays(totalWorkingDays);
                Double totalCapacity = roundWorkTime(totalWorkingDays, workMinutes);
                userCapacityMetrics.setTotalCapacity(totalCapacity);
                if (capacityMap.containsKey(userCapacityMetrics.getAccountId())) {
                    userCapacityMetrics.setLoadedCapacityRatio(
                            capacityMap.get(userCapacityMetrics.getAccountId())
                    );
                }

                // Update loaded capacity and loaded capacity percentage based on the loaded capacity ratio
                int loadedCapacity = (int) (totalCapacity * userCapacityMetrics.getLoadedCapacityRatio());
                userCapacityMetrics.setLoadedCapacity(loadedCapacity);
                if (userCapacityMetrics.getTotalCapacity() > 0) {
                    userCapacityMetrics.setPercentLoadedCapacityUtilization((int) Math.round((float) userCapacityMetrics.getLoadedCapacity() * 100 / userCapacityMetrics.getTotalCapacity()));
                }
                if (userCapacityMetrics.getLoadedCapacity() > 0) {
                    userCapacityMetrics.setPercentPlannedCapacityUtilization(Math.round((float) userCapacityMetrics.getCurrentPlannedCapacity() * 100 / userCapacityMetrics.getLoadedCapacity()));
                }

                // Update the user capacity metrics
                userCapacityMetricsRepository.save(userCapacityMetrics);

                // Aggregate total sprint capacity and loaded capacity
                totalSprintCapacity += totalCapacity;
                totalLoadedCapacity += loadedCapacity;
            }

            // Update sprint capacity metrics with the recalculated values
            SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(sprint.getSprintId());
            if (sprintCapacityMetrics != null) {
                Double totalSprintWorkingDays = calculateWorkingDaysInSprint(sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate(), holidaysAndOffDays, new HashSet<>(), entityPreference.getOfficeHrsStartTime(), entityPreference.getOfficeHrsEndTime(), entityPreference.getMinutesToWorkDaily());
                sprintCapacityMetrics.setTotalWorkingDays(totalSprintWorkingDays);
                sprintCapacityMetrics.setTotalCapacity(totalSprintCapacity);
                sprintCapacityMetrics.setLoadedCapacity(totalLoadedCapacity);
                if (sprintCapacityMetrics.getTotalCapacity() > 0) {
                    sprintCapacityMetrics.setPercentLoadedCapacityUtilization((int) Math.round((float) sprintCapacityMetrics.getLoadedCapacity() * 100 / sprintCapacityMetrics.getTotalCapacity()));
                }
                if (sprintCapacityMetrics.getLoadedCapacity() > 0) {
                    sprintCapacityMetrics.setPercentPlannedCapacityUtilization(Math.round((float) sprintCapacityMetrics.getCurrentPlannedCapacity() * 100 / sprintCapacityMetrics.getLoadedCapacity()));
                }
                sprintCapacityMetricsRepository.save(sprintCapacityMetrics);
            }
        }
    }

    /**
     * This api returns a custom response with message for team capacity, this feature is not yet implemented
     * so this api returns active sprint capacity, if there is no active sprint then from previous sprint
     * a sample data will be sent.
     */
    public TeamCapacityResponse getTeamCapacity (Long teamId, String accountIds, String timeZone) {
        TeamCapacityResponse teamCapacityResponse = new TeamCapacityResponse();
        String message = null;
        Long sprintId = null;
        List<UserCapacityDetail> userCapacityDetails = new ArrayList<>();
        Boolean userAuthority = sprintService.hasModifySprintPermission(accountIds, teamId, Constants.EntityTypes.TEAM);
        if (userAuthority) {
            List<SprintResponseForFilter> sprintResponseList = sprintRepository.getCustomAllActiveSprintDetailsForEntities(Collections.singletonList(teamId), Constants.EntityTypes.TEAM);
            //in case of no active sprint a sample data is prepared
            if (sprintResponseList.isEmpty()) {
                message = "While we put the finishing touches on this feature, immerse yourself in our captivating demo below.";
                List<Sprint> sprintList = sprintRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
                if (sprintList.isEmpty()) {
                    message = "Feature coming soon";
                } else {
                    sprintId = sprintList.get(0).getSprintId();
                }
            } else {
                sprintId = sprintResponseList.get(0).getSprintId();
            }
            //getting user capacity for sprint
            userCapacityDetails = sprintId != null ? getSprintCapacityDetails(sprintId, accountIds, timeZone).getUserCapacities() : Collections.emptyList();
        }
        teamCapacityResponse.setMessage(message);
        teamCapacityResponse.setUserCapacityDetails(userCapacityDetails);
        return teamCapacityResponse;
    }

    public void updateEffortsInSprintCapacity(TimeSheet timeSheet, Long sprintId) {

            Optional<UserCapacityMetrics> userCapacityMetricsOptional = userCapacityMetricsRepository.findBySprintIdAndAccountId(sprintId, timeSheet.getAccountId());
            SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(sprintId);
            Integer earnedEfforts = timeSheet.getEarnedTime();
            Integer burnedEfforts = timeSheet.getNewEffort();
            Integer hoursBehindSchedule = burnedEfforts - earnedEfforts;
            if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.MEETING)) {
                sprintCapacityMetrics.setEarnedEffortsMeeting((sprintCapacityMetrics.getEarnedEffortsMeeting() != null ? sprintCapacityMetrics.getEarnedEffortsMeeting() : 0) + earnedEfforts);
            }
            if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.TASK)) {
                sprintCapacityMetrics.setEarnedEffortsTask((sprintCapacityMetrics.getEarnedEffortsTask() != null ? sprintCapacityMetrics.getEarnedEffortsTask() : 0) + earnedEfforts);
            }
            sprintCapacityMetrics.setTotalEarnedEfforts((sprintCapacityMetrics.getTotalEarnedEfforts() != null ? sprintCapacityMetrics.getTotalEarnedEfforts() : 0) + earnedEfforts);
            sprintCapacityMetrics.setBurnedEfforts((sprintCapacityMetrics.getBurnedEfforts() != null ? sprintCapacityMetrics.getBurnedEfforts() : 0) + burnedEfforts);
            sprintCapacityMetrics.setMinutesBehindSchedule((sprintCapacityMetrics.getMinutesBehindSchedule() != null ? sprintCapacityMetrics.getMinutesBehindSchedule() : 0) + hoursBehindSchedule);
            if (userCapacityMetricsOptional.isPresent()) {
                UserCapacityMetrics userCapacityMetrics = userCapacityMetricsOptional.get();
                if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.MEETING)) {
                    userCapacityMetrics.setEarnedEffortsMeeting((userCapacityMetrics.getEarnedEffortsMeeting() != null ? userCapacityMetrics.getEarnedEffortsMeeting() : 0) + earnedEfforts);
                }
                if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.TASK)) {
                    userCapacityMetrics.setEarnedEffortsTask((userCapacityMetrics.getEarnedEffortsTask() != null ? userCapacityMetrics.getEarnedEffortsTask() : 0) + earnedEfforts);
                }
                userCapacityMetrics.setBurnedEfforts((userCapacityMetrics.getBurnedEfforts() != null ? userCapacityMetrics.getBurnedEfforts() : 0) + burnedEfforts);
                userCapacityMetrics.setTotalEarnedEfforts((userCapacityMetrics.getTotalEarnedEfforts() != null ? userCapacityMetrics.getTotalEarnedEfforts() : 0) + earnedEfforts);
                userCapacityMetrics.setMinutesBehindSchedule((userCapacityMetrics.getMinutesBehindSchedule() != null ? userCapacityMetrics.getMinutesBehindSchedule() : 0) + hoursBehindSchedule);
                userCapacityMetricsRepository.save(userCapacityMetrics);
            }
            sprintCapacityMetricsRepository.save(sprintCapacityMetrics);

    }
    public void updateMovedCapacity(Task task) {
        if (task.getSprintId() == null) {
            throw new ValidationFailedException("Work Item is not a part of any Sprint");
        }

        SprintCapacityMetrics sprintCapacityMetrics = sprintCapacityMetricsRepository.findBySprintId(task.getSprintId());
        if (sprintCapacityMetrics != null) {
            Integer adjustedTaskEstimate = getConsumedTaskEstimate(task);
            sprintCapacityMetrics.setMovedCapacity((sprintCapacityMetrics.getMovedCapacity() != null ? sprintCapacityMetrics.getMovedCapacity() : 0 ) + (task.getTaskEstimate() != null ? task.getTaskEstimate() : 0) -  adjustedTaskEstimate);
            sprintCapacityMetricsRepository.save(sprintCapacityMetrics);
        }

    }

    private Integer getConsumedTaskEstimate(Task task) {
        Integer taskEstimate = task.getTaskEstimate() != null ? task.getTaskEstimate() : 0;
        Double userPerceivedPercentage = task.getUserPerceivedPercentageTaskCompleted() != null ? task.getUserPerceivedPercentageTaskCompleted() / 100.0 : 0;
        return (int) Math.round(taskEstimate * userPerceivedPercentage);
    }

    public void adjustCapacityForMeetingPreferenceChange(Integer entityTypeId, Long entityId, Integer oldMeetingPreferenceId, Integer newMeetingPreferenceId) {
        // currently meeting preference can be defined for organization only
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            List<Long> teamIdList = teamRepository.findTeamIdsByOrgId(entityId);
//            teamIdList = new ArrayList<>();
//            teamIdList.add(337L);
            for (Long teamId : teamIdList) {
                // get all the started and not started sprints for the given team
                List<Sprint> notCompletedSprints = sprintRepository.findByEntityTypeIdAndEntityIdAndSprintStatusIn(Constants.EntityTypes.TEAM, teamId,
                        Arrays.asList(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()));

                for (Sprint sprint : notCompletedSprints) {
                    // get all the tasks in the sprint that are not completed and has a reference meeting
                    List<Task> tasks = taskRepository.findBySprintIdAndWorkflowStatusNotInAndMeetingListNotNullAndNotEmpty(sprint.getSprintId(),
                            List.of(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE));

                    for (Task task : tasks) {
                        List<Meeting> meetings = meetingRepository.findByMeetingIdsIn(task.getMeetingList());
                        for (Meeting meeting : meetings) {
                            adjustCapacitiesForPreferenceChange(sprint.getSprintId(), task, meeting, oldMeetingPreferenceId, newMeetingPreferenceId);
                        }
                    }
                }
            }
        }
    }

    public void adjustCapacitiesForPreferenceChange(Long sprintId, Task task, Meeting meeting, int oldPreferenceId, int newPreferenceId) {
        List<Long> oldAccountIds = getAccountIdsPerMeetingPreference(meeting, task, sprintId, oldPreferenceId);
        List<Long> newAccountIds = getAccountIdsPerMeetingPreference(meeting, task, sprintId, newPreferenceId);

        // Determine the users to decrement and increment capacities
        List<Long> toDecrement = new ArrayList<>(oldAccountIds);
        toDecrement.removeAll(newAccountIds);
        List<Long> toIncrement = new ArrayList<>(newAccountIds);
        toIncrement.removeAll(oldAccountIds);

        // Adjust capacities
        updateReferenceMeetingCapacityOnAddTaskToSprint(sprintId, -meeting.getDuration(), toDecrement);  // Decrement for users removed
        updateReferenceMeetingCapacityOnAddTaskToSprint(sprintId, meeting.getDuration(), toIncrement);  // Increment for new users added
    }

    // ZZZZZZ 13-04-2025
    public void updateSprintAndUserCapacityOnLeaveCancellation (Sprint sprintDb, Long accountId, String timeZone) {

        Sprint sprint = new Sprint();
        BeanUtils.copyProperties(sprintDb, sprint);
        sprintService.convertAllSprintDateToUserTimeZone(sprint, timeZone);
        UserCapacityMetrics userCapacityMetrics = userCapacityMetricsRepository.findByTeamIdAndSprintIdAndAccountId(sprint.getEntityId(), sprint.getSprintId(), accountId);
        SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprint.getSprintId());

        if (userCapacityMetrics != null && sprintCapacity != null) {
            Double totalSprintCapacity = sprintCapacity.getTotalCapacity() - userCapacityMetrics.getTotalCapacity();
            int totalLoadedCapacity = sprintCapacity.getLoadedCapacity() - userCapacityMetrics.getLoadedCapacity();
            EntityPreference entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, userCapacityMetrics.getOrgId()).get();
            Set<LocalDate> holidaysAndOffDays = entityPreferenceService.getHolidaysAndOffDaysForEntityPreference(entityPreference, sprint.getSprintExpStartDate().toLocalDate(), sprint.getSprintExpEndDate().toLocalDate());
//            <LocalDate> userLeaves = leaveService.getAllUserLeaveDays(accountId,"0");
//
//            Double workingDays = calculateWorkingDaysInSprint(sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate(), holidaysAndOffDays, userLeaves, entityPreference.getOfficeHrsStartTime(), entityPreference.getOfficeHrsEndTime(), entityPreference.getMinutesToWorkDaily());
            Map<LocalDate, Float> userLeaveMap = leaveService.getAllUserLeaveDays(accountId,"0");
            Double workingDays = calculateAllWorkingDaysInSprintWithHalfDays(
                    sprint.getSprintExpStartDate(),
                    sprint.getSprintExpEndDate(),
                    holidaysAndOffDays,
                    userLeaveMap,
                    entityPreference.getOfficeHrsStartTime(),
                    entityPreference.getOfficeHrsEndTime(),
                    entityPreference.getMinutesToWorkDaily()
            );
            MemberDetails memberDetails = memberDetailsRepository
                    .findByEntityTypeIdAndEntityIdAndAccountId(Constants.EntityTypes.TEAM, sprint.getEntityId(), accountId);
            userCapacityMetrics.setTotalWorkingDays(workingDays);
            int workMinutes = memberDetails != null ? memberDetails.getWorkMinutes() :
                    entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(userCapacityMetrics.getOrgId()).getSecond();
            userCapacityMetrics.setTotalCapacity(roundWorkTime(workingDays, workMinutes));
            userCapacityMetrics.setLoadedCapacity((int) (userCapacityMetrics.getTotalCapacity() * userCapacityMetrics.getLoadedCapacityRatio()));
            if (userCapacityMetrics.getTotalCapacity() > 0) {
                userCapacityMetrics.setPercentLoadedCapacityUtilization((int) Math.round(((float) userCapacityMetrics.getLoadedCapacity() * 100) / userCapacityMetrics.getTotalCapacity()));
            }
            if (userCapacityMetrics.getLoadedCapacity() > 0) {
                userCapacityMetrics.setPercentPlannedCapacityUtilization(Math.round((float) userCapacityMetrics.getCurrentPlannedCapacity() * 100 / userCapacityMetrics.getLoadedCapacity()));
            }
            userCapacityMetrics.setWorkMinutes(workMinutes);

            totalSprintCapacity += userCapacityMetrics.getTotalCapacity();
            totalLoadedCapacity += userCapacityMetrics.getLoadedCapacity();


            sprintCapacity.setTotalCapacity(totalSprintCapacity);
            sprintCapacity.setLoadedCapacity(totalLoadedCapacity);
            if (sprintCapacity.getTotalCapacity() > 0) {
                sprintCapacity.setPercentLoadedCapacityUtilization((int) Math.round(((float) sprintCapacity.getLoadedCapacity() * 100) / sprintCapacity.getTotalCapacity()));
            }
            if (sprintCapacity.getLoadedCapacity() > 0) {
                sprintCapacity.setPercentPlannedCapacityUtilization(Math.round((float) sprintCapacity.getCurrentPlannedCapacity() * 100 / sprintCapacity.getLoadedCapacity()));
            }
            sprintCapacityMetricsRepository.save(sprintCapacity);
            userCapacityMetricsRepository.save(userCapacityMetrics);
        }
    }

    public Boolean ifRemoveCapacityAllowed (Task task) {
        Sprint sprint = sprintRepository.findBySprintId(task.getSprintId());
        LocalTime sprintCompleteTimeLimit = entityPreferenceService.getOfficeEndTime(task.getFkOrgId().getOrgId(), task.getFkTeamId().getTeamId()).minusHours(entityPreferenceService.getCapacityLimit(task.getFkOrgId().getOrgId(), task.getFkTeamId().getTeamId()));
        return (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && !((LocalDate.now().isAfter(sprint.getSprintExpEndDate().toLocalDate()) || (LocalDate.now().isEqual(sprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit))) && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())));
    }

    public void updateCapacities (EntityPreference entityPreference, Boolean updateCapacitiesForJustNewHoldiays, Set<HolidayRequest> newHolidays, String timeZone) {
        Set<Sprint> sprintList = new HashSet<>();
        List<Integer> sprintStatusList = Constants.ACTIVE_AND_FUTURE_SPRINT_STATUS_LIST;

        if (!updateCapacitiesForJustNewHoldiays) {
            if (Objects.equals(entityPreference.getEntityTypeId(), Constants.EntityTypes.ORG)) {
                sprintList.addAll(sprintRepository.getCustomSprintForOrg(entityPreference.getEntityId(), sprintStatusList));
            } else if (Objects.equals(entityPreference.getEntityTypeId(), Constants.EntityTypes.TEAM)) {
                sprintList.addAll(sprintRepository.findByEntityTypeIdAndEntityIdAndSprintStatusIn(Constants.EntityTypes.TEAM, entityPreference.getEntityId(), sprintStatusList));
            }
        } else {
            for (HolidayRequest holidayRequest : newHolidays) {
                if (Objects.equals(entityPreference.getEntityTypeId(), Constants.EntityTypes.ORG)) {
                    sprintList.addAll(sprintRepository.getCustomSprintsForOrgAndContainsDate(entityPreference.getEntityId(), holidayRequest.getDate(), sprintStatusList));
                } else if (Objects.equals(entityPreference.getEntityTypeId(), Constants.EntityTypes.TEAM)) {
                    sprintList.addAll(sprintRepository.getCustomSprintsForEntitiesAndContainsDate(List.of(entityPreference.getEntityId()), Constants.EntityTypes.TEAM, holidayRequest.getDate(), sprintStatusList));
                }
            }
        }

        for (Sprint sprint : sprintList) {
            try {
                Sprint sprintCopy = new Sprint();
                BeanUtils.copyProperties(sprint, sprintCopy);
                sprintService.convertAllSprintDateToUserTimeZone(sprintCopy, timeZone);
                recalculateCapacitiesForSprint(sprintCopy, entityPreference,null);
            } catch (Exception e) {
                logger.error("Unable to update sprint capacities on entity preference update. Error message : " + e.getMessage());
            }
        }
    }
    public void updateCapacityAndRemoveMember (Long sprintId, Long accountId) {
        SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprintId);
        Optional<UserCapacityMetrics> userCapacityMetrics = userCapacityMetricsRepository.findBySprintIdAndAccountId(sprintId, accountId);
        UserCapacityMetrics userCapacity = userCapacityMetrics.get();
        sprintCapacity.setTotalCapacity(sprintCapacity.getTotalCapacity() - userCapacity.getTotalCapacity());
        sprintCapacity.setLoadedCapacity(sprintCapacity.getLoadedCapacity() - userCapacity.getLoadedCapacity());
        if (sprintCapacity.getTotalCapacity() > 0) {
            sprintCapacity.setPercentLoadedCapacityUtilization((int) Math.round(((float) sprintCapacity.getLoadedCapacity() * 100) / sprintCapacity.getTotalCapacity()));
        }
        if (sprintCapacity.getLoadedCapacity() > 0) {
            sprintCapacity.setPercentPlannedCapacityUtilization(Math.round((float) sprintCapacity.getCurrentPlannedCapacity() * 100 / sprintCapacity.getLoadedCapacity()));
        }
        sprintCapacityMetricsRepository.save(sprintCapacity);
        userCapacityMetricsRepository.removeUserCapacity(sprintId, accountId);
    }

    // ZZZZZZ 14-04-2025
    public void addAndUpdateCapacityOnAddingMember (Long sprintId, List<Long> accountIdListForAddedMember, String timeZone) {
        Sprint sprintDb = sprintRepository.findBySprintId(sprintId);
        Team team = teamRepository.findById(sprintDb.getEntityId()).orElseThrow(() -> new EntityNotFoundException("Team not found"));

        // ZZZZZZ 14-04-2025
        Sprint sprintCopy = new Sprint();
        BeanUtils.copyProperties(sprintDb, sprintCopy);
        sprintService.convertAllSprintDateToUserTimeZone(sprintCopy, timeZone);

        EntityPreference entityPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId());
        Set<LocalDate> holidaysAndOffDays = entityPreferenceService.getOfficeHolidaysAndOffDaysFromEntityPreferenceBetweenGivenDates(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId(), sprintCopy.getSprintExpStartDate().toLocalDate(), sprintCopy.getSprintExpEndDate().toLocalDate());
        List<UserCapacityMetrics> userCapacities = updateTeamMemberCapacitiesOnAddingMemberInSprint(sprintCopy, holidaysAndOffDays, accountIdListForAddedMember, entityPreference);

        int totalSprintCapacity = 0, totalLoadedCapacity = 0;
        for (UserCapacityMetrics userCapacityMetrics : userCapacities) {
            totalSprintCapacity += userCapacityMetrics.getTotalCapacity();
            totalLoadedCapacity += userCapacityMetrics.getLoadedCapacity();
        }

        SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprintId);

        sprintCapacity.setTotalCapacity(sprintCapacity.getTotalCapacity() + totalSprintCapacity);
        sprintCapacity.setLoadedCapacity(sprintCapacity.getLoadedCapacity() + totalLoadedCapacity);
        if (sprintCapacity.getTotalCapacity() > 0) {
            sprintCapacity.setPercentLoadedCapacityUtilization((int) Math.round(((float) sprintCapacity.getLoadedCapacity() * 100) / sprintCapacity.getTotalCapacity()));
        }
        if (sprintCapacity.getLoadedCapacity() > 0) {
            sprintCapacity.setPercentPlannedCapacityUtilization(Math.round((float) sprintCapacity.getCurrentPlannedCapacity() * 100 / sprintCapacity.getLoadedCapacity()));
        }
        sprintCapacityMetricsRepository.save(sprintCapacity);

    }

    private List<UserCapacityMetrics> updateTeamMemberCapacitiesOnAddingMemberInSprint(Sprint sprintDb, Set<LocalDate> holidaysAndOffDays, List<Long> accountIdList, EntityPreference entityPreference) {

        List<UserCapacityMetrics> userCapacityMetricsList = new ArrayList<>();
        Team team = teamRepository.findByTeamId(sprintDb.getEntityId());

        List<MemberDetails> allMembers = memberDetailsRepository
                .findByEntityTypeIdAndEntityIdAndAccountIdIn(Constants.EntityTypes.TEAM, team.getTeamId(), accountIdList);

        Map<Long, MemberDetails> memberDetailsMap = allMembers.stream()
                .collect(Collectors.toMap(MemberDetails::getAccountId, Function.identity(), (existing, replacement) -> existing));

        accountIdList.forEach(accountId -> {
            MemberDetails memberDetails = memberDetailsMap.get(accountId);
            Set<LocalDate> userLeaves = leaveService.getUserApprovedLeaves(accountId, "0");

            double workingDays = calculateWorkingDaysInSprint(sprintDb.getSprintExpStartDate(), sprintDb.getSprintExpEndDate(), holidaysAndOffDays, userLeaves, entityPreference.getOfficeHrsStartTime(), entityPreference.getOfficeHrsEndTime(), entityPreference.getMinutesToWorkDaily());

            UserCapacityMetrics capacityMetrics = new UserCapacityMetrics();
            capacityMetrics.setOrgId(team.getFkOrgId().getOrgId());
            capacityMetrics.setProjectId(team.getFkProjectId().getProjectId());
            capacityMetrics.setTeamId(team.getTeamId());
            capacityMetrics.setSprintId(sprintDb.getSprintId());
            capacityMetrics.setAccountId(accountId);
            capacityMetrics.setTotalWorkingDays(workingDays);
            int workMinutes = memberDetails != null ? memberDetails.getWorkMinutes() :
                    entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(team.getFkOrgId().getOrgId()).getSecond();
            capacityMetrics.setTotalCapacity(roundWorkTime(workingDays, workMinutes));
            capacityMetrics.setLoadedCapacity((int) (capacityMetrics.getTotalCapacity() * capacityMetrics.getLoadedCapacityRatio()));
            if (capacityMetrics.getTotalCapacity() > 0) {
                capacityMetrics.setPercentLoadedCapacityUtilization((int)Math.round(((float) capacityMetrics.getLoadedCapacity() * 100) / capacityMetrics.getTotalCapacity()));
            }
            capacityMetrics.setWorkMinutes(workMinutes);
            userCapacityMetricsList.add(capacityMetrics);
        });

        return userCapacityMetricsRepository.saveAll(userCapacityMetricsList);
    }

    public long updateWorkingMinutesForStartEndDay (LocalTime startTime, LocalTime endTime, Integer officeMinutes) {
        long workingMinutesOnStartAndEndDay = 0L;
        long durationOnStartDay = startTime.until(endTime, ChronoUnit.MINUTES);
        long percentageDuration = durationOnStartDay * 100 / officeMinutes.longValue();
        if (percentageDuration >= 85L) {
            workingMinutesOnStartAndEndDay += officeMinutes.longValue();
        } else if (percentageDuration >= 5L) {
            workingMinutesOnStartAndEndDay += Math.min(durationOnStartDay, officeMinutes.longValue());
        }
        return workingMinutesOnStartAndEndDay;
    }

    public double roundWorkTime(double workingDays, int workMinutesPerDay) {
        // Calculate total minutes
        double totalMinutes = workingDays * workMinutesPerDay;

        // Convert to hours and minutes
        int hours = (int) totalMinutes / 60;
        int minutes = (int) totalMinutes % 60;

        // Round the minutes to nearest 0, 30, or 60
        if (minutes < 15) {
            minutes = 0;
        }else if (minutes < 45) {
            minutes = 30;
        } else {
            minutes = 0;
            hours += 1; // Add an hour when rounding up from 45 or more
        }

        // Return the total rounded minutes
        return ((hours * 60) + minutes);
    }

    public void updateSprintEarnedEffort(Sprint sprint, int previousEarnedTime, int updatedEarnedTime) {
        if (sprint == null) return;
        int newEarnedEfforts = Math.max(0, getNonNullValue(sprint.getEarnedEfforts()) - previousEarnedTime + updatedEarnedTime);
        sprint.setEarnedEfforts(newEarnedEfforts);
    }

    public void updateSprintCapacityMetricsEarnedEffort(SprintCapacityMetrics sprintCapacity, int previousEarnedTime, int updatedEarnedTime, Integer entityTypeId) {
        if (sprintCapacity == null) return;

        int newMinutesBehindSchedule = (sprintCapacity.getMinutesBehindSchedule() != null ? sprintCapacity.getMinutesBehindSchedule() : 0) + previousEarnedTime - updatedEarnedTime;

        sprintCapacity.setTotalEarnedEfforts(Math.max(0, getNonNullValue(sprintCapacity.getTotalEarnedEfforts()) - previousEarnedTime + updatedEarnedTime));
        sprintCapacity.setMinutesBehindSchedule(newMinutesBehindSchedule);

        if (Objects.equals(entityTypeId, Constants.EntityTypes.MEETING)) {
            sprintCapacity.setEarnedEffortsMeeting(Math.max(0, getNonNullValue(sprintCapacity.getEarnedEffortsMeeting()) - previousEarnedTime + updatedEarnedTime));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TASK)) {
            sprintCapacity.setEarnedEffortsTask(Math.max(0, getNonNullValue(sprintCapacity.getEarnedEffortsTask()) - previousEarnedTime + updatedEarnedTime));
        }
    }

    public void updateUserCapacityMetricsEarnedEffort(UserCapacityMetrics userCapacity, int previousEarnedTime, int updatedEarnedTime, Integer entityTypeId) {
        if (userCapacity == null) return;

        int newMinutesBehindSchedule = (userCapacity.getMinutesBehindSchedule() != null ? userCapacity.getMinutesBehindSchedule() : 0) + previousEarnedTime - updatedEarnedTime;

        userCapacity.setTotalEarnedEfforts(Math.max(0, getNonNullValue(userCapacity.getTotalEarnedEfforts()) - previousEarnedTime + updatedEarnedTime));
        userCapacity.setMinutesBehindSchedule(newMinutesBehindSchedule);

        if (Objects.equals(entityTypeId, Constants.EntityTypes.MEETING)) {
            userCapacity.setEarnedEffortsMeeting(Math.max(0, getNonNullValue(userCapacity.getEarnedEffortsMeeting()) - previousEarnedTime + updatedEarnedTime));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TASK)) {
            userCapacity.setEarnedEffortsTask(Math.max(0, getNonNullValue(userCapacity.getEarnedEffortsTask()) - previousEarnedTime + updatedEarnedTime));
        }
    }

    public void updateSprintCapacityMetricsBurnedEffort(SprintCapacityMetrics sprintCapacity, int previousBurnedTime, int updatedBurnedTime, Integer entityTypeId) {
        if (sprintCapacity == null) return;

        int newMinutesBehindSchedule = (sprintCapacity.getMinutesBehindSchedule() != null ? sprintCapacity.getMinutesBehindSchedule() : 0) - previousBurnedTime + updatedBurnedTime;

        sprintCapacity.setBurnedEfforts(Math.max(0, getNonNullValue(sprintCapacity.getBurnedEfforts()) - previousBurnedTime + updatedBurnedTime));
        sprintCapacity.setMinutesBehindSchedule(newMinutesBehindSchedule);

        sprintCapacityMetricsRepository.save(sprintCapacity);
    }

    public void updateUserCapacityMetricsBurnedEffort(UserCapacityMetrics userCapacity, int previousBurnedTime, int updatedBurnedTime, Integer entityTypeId) {
        if (userCapacity == null) return;

        int newMinutesBehindSchedule = (userCapacity.getMinutesBehindSchedule() != null ? userCapacity.getMinutesBehindSchedule() : 0) - previousBurnedTime + updatedBurnedTime;

        userCapacity.setBurnedEfforts(Math.max(0, getNonNullValue(userCapacity.getBurnedEfforts()) - previousBurnedTime + updatedBurnedTime));
        userCapacity.setMinutesBehindSchedule(newMinutesBehindSchedule);

        userCapacityMetricsRepository.save(userCapacity);
    }

    private int getNonNullValue(Integer value) {
        return value != null ? value : 0;
    }

    public void fetchAnotherSprintLoadedCapacityRatios(Long sprintId, Long fetchLoadFactorOfSprint) {
        Map<Long, Double> capacityMap;
        Sprint sprint = sprintRepository.findBySprintId(sprintId);
        if (sprint == null) {
            throw new ValidationFailedException("Sprint Id is not exist!!");
        }
        if (fetchLoadFactorOfSprint != null) {
            Sprint sprintDb = sprintRepository.findBySprintId(fetchLoadFactorOfSprint);
            if (sprintDb == null) {
                throw new ValidationFailedException(" Given Sprint for fetch Load Factor does not exist!!");
            }
            if (!Objects.equals(sprintDb.getEntityId(), sprint.getEntityId())) {
                throw new ValidationFailedException("Sprint " + sprintDb.getSprintTitle()
                        + " doesn't belong to team " + sprint.getSprintTitle());
            }
            capacityMap = userCapacityMetricsRepository.findBySprintId(fetchLoadFactorOfSprint)
                    .stream()
                    .collect(Collectors.toMap(
                            UserCapacityMetrics::getAccountId,
                            UserCapacityMetrics::getLoadedCapacityRatio,
                            (existing, replacement) -> replacement));
        } else {
            capacityMap = new HashMap<>();
        }
        List<UserCapacityMetrics> userCapacityMetricsList = userCapacityMetricsRepository.findBySprintId(sprint.getSprintId());
        LoadedCapacityRatioUpdateRequest updateRequest = new LoadedCapacityRatioUpdateRequest();
        updateRequest.setSprintId(sprint.getSprintId());

        List<UserLoadedCapacityUpdate> updateList = new ArrayList<>();
        for (UserCapacityMetrics userCapacityMetrics : userCapacityMetricsList) {
            if (userCapacityMetrics.getAccountId() != null && userCapacityMetrics.getAccountId() != 0 && capacityMap.containsKey(userCapacityMetrics.getAccountId())) {
                UserLoadedCapacityUpdate userList = new UserLoadedCapacityUpdate();
                userList.setAccountId(userCapacityMetrics.getAccountId());
                userList.setNewLoadedCapacityRatio(capacityMap.get(userCapacityMetrics.getAccountId()));
                updateList.add(userList);
            }
        }
        if (!updateList.isEmpty()) {
            updateRequest.setUpdates(updateList);
            updateLoadedCapacityRatios(updateRequest);
        }
    }

    /**
     * Calculates working days in sprint considering full-day & half-day leaves.
     * userLeavesMap: date -> 1.0 (full leave), 0.5 (half leave), 0.0/no entry (no leave)
     */
    private Double calculateAllWorkingDaysInSprintWithHalfDays(
            LocalDateTime sprintStartDate,
            LocalDateTime sprintEndDate,
            Set<LocalDate> holidaysAndOffDays,
            Map<LocalDate, Float> userLeavesMap,
            LocalTime officeStartTime,
            LocalTime officeEndTime,
            Integer officeMinutes
    ) {
        long workingMinutesOnStartAndEndDay = 0L;
        if (!Objects.equals(sprintStartDate.toLocalDate(), sprintEndDate.toLocalDate())) {
            LocalDate startDate = sprintStartDate.toLocalDate();
            if (!holidaysAndOffDays.contains(startDate) && getLeaveFactor(userLeavesMap, startDate) < 1.0
                    && !sprintStartDate.toLocalTime().isAfter(officeEndTime)) {
                long mins = updateWorkingMinutesForStartEndDay(
                        sprintStartDate.toLocalTime(), officeEndTime, officeMinutes);
                workingMinutesOnStartAndEndDay += (long) (mins * (1 - getLeaveFactor(userLeavesMap, startDate)));
            }
            LocalDate endDate = sprintEndDate.toLocalDate();
            if (!holidaysAndOffDays.contains(endDate) && getLeaveFactor(userLeavesMap, endDate) < 1.0
                    && !sprintEndDate.toLocalTime().isBefore(officeStartTime)) {

                long mins = updateWorkingMinutesForStartEndDay(
                        officeStartTime, sprintEndDate.toLocalTime(), officeMinutes);
                workingMinutesOnStartAndEndDay += (long) (mins * (1 - getLeaveFactor(userLeavesMap, endDate)));
            }
        } else {
            LocalDate sameDay = sprintStartDate.toLocalDate();
            double leaveFactor = getLeaveFactor(userLeavesMap, sameDay);
            long mins = updateWorkingMinutesForStartEndDay(
                    !sprintStartDate.toLocalTime().isBefore(officeStartTime) ? sprintStartDate.toLocalTime() : officeStartTime,
                    !sprintEndDate.toLocalTime().isAfter(officeEndTime) ? sprintEndDate.toLocalTime() : officeEndTime,
                    officeMinutes);

            workingMinutesOnStartAndEndDay += (long) (mins * (1 - leaveFactor));
        }
        double totalDays = (double) workingMinutesOnStartAndEndDay / officeMinutes;
        LocalDate date = sprintStartDate.toLocalDate().plusDays(1);
        while (!date.isAfter(sprintEndDate.toLocalDate().minusDays(1))) {
            if (!holidaysAndOffDays.contains(date)) {
                double leaveFactor = getLeaveFactor(userLeavesMap, date);
                if (leaveFactor == 0.0) {
                    totalDays += 1;     // full working day
                } else if (leaveFactor == 0.5) {
                    totalDays += 0.5;   // half working day
                }
            }
            date = date.plusDays(1);
        }
        return totalDays;
    }

    private double getLeaveFactor(Map<LocalDate, Float> leaveMap, LocalDate date) {
        return leaveMap.getOrDefault(date, 0.0f);
    }

}