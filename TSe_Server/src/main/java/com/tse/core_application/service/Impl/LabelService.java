package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.label.EntityTypeLabelResponse;
import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LabelService {

    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private RecurringMeetingRepository recurringMeetingRepository;
    @Autowired
    private MeetingService meetingService;
    @Autowired
    private TaskHistoryService taskHistoryService;
    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;
    /**
     * method to get all unique labels for a given teamId
     *
     */
    public List<LabelResponse> getLabelsByTeamId(Long teamId) {
        List<LabelResponse> labelResponseList = labelRepository.findLabelInfoInTeam(List.of(teamId));
        labelResponseList.sort(Comparator.comparing(
                LabelResponse::getTeamCode).thenComparing(
                LabelResponse::getLabelName));
        return labelResponseList;
    }

    /**
     * method to get all unique labels for a given entityTypeId and entityId
     *
     */
    public List<LabelResponse> getLabelsForEntity(Integer entityTypeId, Long entityId) {
        List<Long> teamIds = new ArrayList<>();
        if (entityTypeId.equals(Constants.EntityTypes.ORG)) {
            teamIds.addAll(teamRepository.findTeamIdsByOrgId(entityId));
        } else if (entityTypeId.equals(Constants.EntityTypes.PROJECT)) {
            teamIds.addAll(teamRepository.findTeamIdsByProjectId(entityId));
        } else if (entityTypeId.equals(Constants.EntityTypes.BU)) {
            teamIds.addAll(teamRepository.findTeamIdsByBuId(entityId));
        } else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
            teamIds.add(entityId);
        }
        List<LabelResponse> labelResponseList = labelRepository.findLabelInfoInTeam(teamIds);
        labelResponseList.sort(Comparator.comparing(
                LabelResponse::getTeamCode).thenComparing(
                        LabelResponse::getLabelName));
        return labelResponseList;
    }

    /**
     * remove a label from the task
     * @param taskId
     * @param labelId
     * @param accountId
     * @return
     */
    public boolean removeLabelFromTask(Long taskId, Long labelId, Long accountId) {
        // Find the task and label by their IDs
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Incorrect TaskId. Task not found"));
        Task taskCopy = new Task();
        BeanUtils.copyProperties(task, taskCopy);
        Label label = labelRepository.findById(labelId).orElseThrow(() -> new IllegalArgumentException("Incorrect LabelId. Label not found"));

        // Check if the accountId has the right to edit the task -- assuming the label is a basic update action
        boolean isSelfAssigned = task.getFkAccountIdAssigned() != null && Objects.equals(accountId, task.getFkAccountIdAssigned().getAccountId());
        String requiredPermission = isSelfAssigned ? Constants.UpdateTeam.Task_Basic_Update : Constants.UpdateTeam.All_Task_Basic_Update;

        // Check if update is allowed
        if (!taskService.isUpdateAllowed(requiredPermission, accountId, task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("User is not allowed to update the task");
        }

        if (task.getLabels() != null && task.getLabels().contains(label)) {
            List<String> updatedField = new ArrayList<>();

            List<String> taskLabels = task.getLabels().stream()
                    .map(Label::getLabelName)
                    .collect(Collectors.toList());
            task.setTaskLabels(taskLabels);
            taskHistoryService.addTaskHistoryOnUserUpdate(taskCopy);

            task.getLabels().remove(label);
            UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
            if (userAccount != null) {
                task.setFkAccountIdLastUpdated(userAccount);
            }
            taskRepository.save(task);
            updatedField.add(Constants.TaskFields.LABELS);
            taskHistoryMetadataService.addTaskHistoryMetadata(updatedField, task);
            return true;
        }

        return false;
    }

    /**
     * remove a label from the meeting
     * @param meetingId
     * @param labelId
     * @param accountId
     * @return
     */
    public boolean removeLabelFromMeeting(Long meetingId, Long labelId, Long accountId) {
        // Find the meeting and label by their IDs
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(() -> new IllegalArgumentException("Incorrect MeetingId. Meeting not found"));
        Label label = labelRepository.findById(labelId).orElseThrow(() -> new IllegalArgumentException("Incorrect LabelId. Label not found"));

        // Check if the user has the rights to edit the meeting
        boolean hasEditRights = meetingService.hasEditPermissions(accountId, meeting);
        if (!hasEditRights) {
            throw new ValidationFailedException("User does not have permission to edit this meeting");
        }

        // Check if the meeting has the label and remove it
        if (meeting.getMeetingLabels() != null && meeting.getMeetingLabels().contains(label)) {
            meeting.getMeetingLabels().remove(label);
            meetingRepository.save(meeting);
            return true;
        }

        return false;
    }

    /**
     * remove label from a recurring meeting
     */
    public boolean removeLabelFromRecurringMeeting(Long recurringMeetingId, Long labelId, Long accountId) {
        // Find the recurring meeting and label by their IDs
        RecurringMeeting recurringMeeting = recurringMeetingRepository.findById(recurringMeetingId).orElseThrow(() -> new IllegalArgumentException("Incorrect Recurring MeetingId. Meeting not found"));
        Label label = labelRepository.findById(labelId).orElseThrow(() -> new IllegalArgumentException("Incorrect LabelId. Label not found"));

        // Check if the user has the rights to edit the meeting
        boolean hasEditRights = meetingService.hasEditPermissionsForRecurringMeeting(accountId, recurringMeeting);
        if (!hasEditRights) {
            throw new ValidationFailedException("User does not have permission to edit this recurring meeting");
        }

        // Check if the recurring meeting and the associated meetings has the label and remove it
        if (recurringMeeting.getRecurMeetingLabels() != null && recurringMeeting.getRecurMeetingLabels().contains(label)) {
            recurringMeeting.getRecurMeetingLabels().remove(label);
            recurringMeetingRepository.save(recurringMeeting);
            List<Meeting> meetingsOfRecurMeeting = recurringMeeting.getMeetingList();
            for (Meeting meeting : meetingsOfRecurMeeting) {
                if (meeting.getMeetingLabels() != null && meeting.getMeetingLabels().contains(label)) {
                    meeting.getMeetingLabels().remove(label);
                }
            }
            meetingRepository.saveAll(meetingsOfRecurMeeting);
            return true;
        }

        return false;
    }

    public List<EntityTypeLabelResponse> getEntityTypeLabels(Integer entityTypeId, Long entityId,String accountIds) {
        Long orgId = null;

        if(Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            orgId = entityId;
        }
        else if(Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            orgId = projectRepository.findByProjectId(entityId).getOrgId();
        }
        else if(Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            orgId = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
        }

        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(orgId,true,accountIdList);
        if(userAccountId == null) {
            throw new ValidationFailedException("User is not present in the organisation");
        }
        List<Long>entityIdList=new ArrayList<>();
        entityIdList.add(entityId);
        List<EntityTypeLabelResponse> labelResponseList = labelRepository.findLabelInfoByEntityTypeAndEntityList(entityTypeId,entityIdList);

        return labelResponseList;
    }

}
