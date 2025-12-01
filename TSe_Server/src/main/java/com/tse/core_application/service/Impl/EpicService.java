package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.controller.StatsController;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EpicService {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private WorkFlowEpicStatusRepository workFlowEpicStatusRepository;
    @Autowired
    private EpicRepository epicRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private EpicTaskRepository epicTaskRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskHistoryService taskHistoryService;
    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;
    @Autowired
    private EpicSequenceRepository epicSequenceRepository;
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;
    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private AuditService auditService;

    private static final Logger logger = LogManager.getLogger(StatsController.class.getName());

    public void convertAllEpicDateToServerTimeZone (EpicRequest epicRequest, String timeZone) {

        if (epicRequest.getExpEndDateTime() != null) {
            epicRequest.setExpEndDateTime(DateTimeUtils.convertUserDateToServerTimezone(epicRequest.getExpEndDateTime(), timeZone));
        }
        if(epicRequest.getExpStartDateTime() != null) {
            epicRequest.setExpStartDateTime(DateTimeUtils.convertUserDateToServerTimezone(epicRequest.getExpStartDateTime(), timeZone));
        }
        if(epicRequest.getActStartDateTime() != null) {
            epicRequest.setActStartDateTime(DateTimeUtils.convertUserDateToServerTimezone(epicRequest.getActStartDateTime(), timeZone));
        }
        if(epicRequest.getActEndDateTime() != null) {
            epicRequest.setActEndDateTime(DateTimeUtils.convertUserDateToServerTimezone(epicRequest.getActEndDateTime(), timeZone));
        }
        if(epicRequest.getDueDateTime() != null) {
            epicRequest.setDueDateTime((DateTimeUtils.convertUserDateToServerTimezone(epicRequest.getDueDateTime(), timeZone)));
        }
    }

    public void convertAllEpicDateToUserTimeZone(EpicResponse epic, String timeZone) {
        if (epic.getExpStartDateTime() != null) {
            epic.setExpStartDateTime(DateTimeUtils.convertServerDateToUserTimezone(epic.getExpStartDateTime(), timeZone));
        }
        if(epic.getExpEndDateTime() != null) {
            epic.setExpEndDateTime(DateTimeUtils.convertServerDateToUserTimezone(epic.getExpEndDateTime(), timeZone));
        }
        if(epic.getActStartDateTime() != null) {
            epic.setActStartDateTime(DateTimeUtils.convertServerDateToUserTimezone(epic.getActStartDateTime(), timeZone));
        }
        if(epic.getActEndDateTime() != null) {
            epic.setActEndDateTime(DateTimeUtils.convertServerDateToUserTimezone(epic.getActEndDateTime(), timeZone));
        }
        if(epic.getDueDateTime() != null) {
            epic.setDueDateTime((DateTimeUtils.convertServerDateToUserTimezone(epic.getDueDateTime(), timeZone)));
        }
    }

    public void convertEpicDetailResponseDateTimeToUserTimeZone (EpicDetailsResponse epicDetailsResponse, String timeZone) {
        if (epicDetailsResponse.getExpStartDateTime() != null) {
            epicDetailsResponse.setExpStartDateTime(DateTimeUtils.convertServerDateToUserTimezone(epicDetailsResponse.getExpStartDateTime(), timeZone));
        }
        if(epicDetailsResponse.getExpEndDateTime() != null) {
            epicDetailsResponse.setExpEndDateTime(DateTimeUtils.convertServerDateToUserTimezone(epicDetailsResponse.getExpEndDateTime(), timeZone));
        }
        if(epicDetailsResponse.getActStartDateTime() != null) {
            epicDetailsResponse.setActStartDateTime(DateTimeUtils.convertServerDateToUserTimezone(epicDetailsResponse.getActStartDateTime(), timeZone));
        }
        if(epicDetailsResponse.getActEndDateTime() != null) {
            epicDetailsResponse.setActEndDateTime(DateTimeUtils.convertServerDateToUserTimezone(epicDetailsResponse.getActEndDateTime(), timeZone));
        }
        if(epicDetailsResponse.getDueDateTime() != null) {
            epicDetailsResponse.setDueDateTime((DateTimeUtils.convertServerDateToUserTimezone(epicDetailsResponse.getDueDateTime(), timeZone)));
        }
    }

    public void hasPermissionToModifyEpicForTeamList(List<Long> teamIdList, Long userAccountId) {

        // Team id(s) should be of same project listed (project id is given)
        List<Integer> authorizeRoleIdList = Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION;

        List<Team> teamList = teamRepository.findByTeamIdIn(teamIdList);

        // Creator should have role (10, 11, 12, 14 or 15) in each team selected
        for(Team team : teamList) {
            if(!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, team.getTeamId(), userAccountId, true, authorizeRoleIdList)) {
                throw new ValidationFailedException("User doesn't have necessary role in all the team");
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextEpicIdentifier(Long projectId) {
        EpicSequence sequence = epicSequenceRepository.findByProjectIdForUpdate(projectId);

        if (sequence == null) {
            sequence = new EpicSequence(projectId, 0L);
        }
        Long nextTaskIdentifier = sequence.getLastEpicIdentifier() + 1;
        sequence.setLastEpicIdentifier(nextTaskIdentifier);
        epicSequenceRepository.save(sequence);
        return nextTaskIdentifier;
    }

    public Boolean validateUserRole(List<Long> teamIdList, Long accountId) {
        List<Integer> authorizeRoleIdList = Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION;

        Boolean isValid = false;
        for(Long teamId : teamIdList) {
            if(accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, teamId, accountId, true, authorizeRoleIdList)) {
                isValid = true;
                break;
            }
        }
        return isValid;
    }

    public void validateTeamList(List<Long> teamIdList, Long projectId) {
        // Team id list should not be null
        if(teamIdList.isEmpty()) {
            throw new ValidationFailedException("At least one team should be in epic");
        }
        for(Long teamId : teamIdList) {
            if(!Objects.equals(teamRepository.findFkProjectIdProjectIdByTeamId(teamId), projectId)) {
                throw new ValidationFailedException("Selected team is not part of project");
            }
        }
    }

    public List<EpicTaskResponse> getEpicTaskResponse(List<Task> taskList) {
        List<EpicTaskResponse> epicTaskResponseList = new ArrayList<>();
        for(Task task : taskList) {
            EpicTaskResponse epicTaskResponse = new EpicTaskResponse();
            BeanUtils.copyProperties(task, epicTaskResponse);
            epicTaskResponse.setTeamId(task.getFkTeamId().getTeamId());
            epicTaskResponse.setTeamName(task.getFkTeamId().getTeamName());
            epicTaskResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            epicTaskResponseList.add(epicTaskResponse);
        }
        return epicTaskResponseList;
    }

    public List<EpicChildTaskResponse> getEpicChildTaskResponse(Task task) {
        List<EpicChildTaskResponse> epicChildTaskResponse = new ArrayList<>();
        List<Task> epicChildTaskList = taskRepository.findByParentTaskId(task.getTaskId());
        for(Task childTask : epicChildTaskList) {
            if(childTask.getFkEpicId() != null) {
                EpicChildTaskResponse epicChildTask = new EpicChildTaskResponse();
                BeanUtils.copyProperties(childTask, epicChildTask);
                epicChildTask.setTeamId(childTask.getFkTeamId().getTeamId());
                epicChildTask.setWorkflowTaskStatus(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                if (epicChildTask.getAddedInEpicAfterCompletion() != null && epicChildTask.getAddedInEpicAfterCompletion()) {
                    epicChildTask.setMessage("Added in epic after completion");
                }
                if (childTask.getFkAccountIdAssigned() != null) {
                    epicChildTask.setAssignedTo(childTask.getFkAccountIdAssigned().getAccountId());
                }
                epicChildTaskResponse.add(epicChildTask);
            }
        }
        return epicChildTaskResponse;
    }

    public EpicTaskByFilterResponse getEpicTaskResponseList(List<Task> taskList) {
        EpicTaskByFilterResponse epicTaskByFilterResponse = new EpicTaskByFilterResponse();
        List<EpicTaskWithStatus> epicTaskWithStatusList = new ArrayList<>();
        EpicTaskWithStatus backlogEpicTaskList = new EpicTaskWithStatus();
        EpicTaskWithStatus notStartedEpicTaskList = new EpicTaskWithStatus();
        EpicTaskWithStatus startedEpicTaskList = new EpicTaskWithStatus();
        EpicTaskWithStatus completedEpicTaskList = new EpicTaskWithStatus();

        backlogEpicTaskList.setStatus("Backlog");
        notStartedEpicTaskList.setStatus("Not Started");
        startedEpicTaskList.setStatus("Started");
        completedEpicTaskList.setStatus("Completed");
        for (Task task : taskList) {
            EpicTaskResponse epicTaskResponse = new EpicTaskResponse();
            BeanUtils.copyProperties(task, epicTaskResponse);
            epicTaskResponse.setTeamId(task.getFkTeamId().getTeamId());
            epicTaskResponse.setTeamName(task.getFkTeamId().getTeamName());
            epicTaskResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            if (epicTaskResponse.getAddedInEpicAfterCompletion() != null && epicTaskResponse.getAddedInEpicAfterCompletion()) {
                epicTaskResponse.setMessage("Added in epic after completion");
            }
            if (task.getFkAccountIdAssigned() != null) {
                epicTaskResponse.setAssignedTo(task.getFkAccountIdAssigned().getAccountId());
            }
            if(Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                List<EpicChildTaskResponse> epicChildTaskResponse = getEpicChildTaskResponse(task);
                epicTaskResponse.setChildTaskResponses(epicChildTaskResponse);
            }

            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                backlogEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                backlogEpicTaskList.incrementTaskCount();
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
                notStartedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                notStartedEpicTaskList.incrementTaskCount();
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                startedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                startedEpicTaskList.incrementTaskCount();
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                completedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                completedEpicTaskList.incrementTaskCount();
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                if (task.getTaskActStDate() == null) {
                    notStartedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                    notStartedEpicTaskList.incrementTaskCount();
                }
                else {
                    startedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                    startedEpicTaskList.incrementTaskCount();
                }
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                if (Objects.equals(task.getStatusAtTimeOfDeletion(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                    backlogEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                }
                else if (Objects.equals(task.getStatusAtTimeOfDeletion(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
                    notStartedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                }
                else if (Objects.equals(task.getStatusAtTimeOfDeletion(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                    startedEpicTaskList.getEpicTaskResponseList().add(epicTaskResponse);
                }
            }
        }
        epicTaskWithStatusList.add(backlogEpicTaskList);
        epicTaskWithStatusList.add(notStartedEpicTaskList);
        epicTaskWithStatusList.add(startedEpicTaskList);
        epicTaskWithStatusList.add(completedEpicTaskList);
        epicTaskByFilterResponse.setEpicTaskWithStatusList(epicTaskWithStatusList);
        return epicTaskByFilterResponse;
    }

    @Transactional
    public void addTaskToEpic(Epic epic, Task task, Long accountId, Boolean isChildTaskIndividual) throws IllegalAccessException {

        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && isChildTaskIndividual) {
            throw new ValidationFailedException("User can not directly add a child task to the Epic");
        }
        if(isChildTaskIndividual && !validateUserRole(List.of(task.getFkTeamId().getTeamId()), accountId)) {
            throw new ValidationFailedException("User doesn't have permission to add this Work Item in epic");
        }
        if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new ValidationFailedException("Deleted Work Item can't be added to Epic");
        }
        List<Long> teamIds = epic.getTeamIdList();
        if(!teamIds.contains(task.getFkTeamId().getTeamId())) {
            throw new IllegalAccessException("Work Item is not part of any team of epic");
        }

        if(isChildTaskIndividual && task.getFkEpicId() != null && task.getFkEpicId().getEpicId() != null) {
            if(Objects.equals(epic.getEpicId(), task.getFkEpicId().getEpicId())) {
                throw new IllegalAccessException("Work Item is already part of this epic");
            }
            else {
                throw new IllegalStateException("This Work Item is part of another epic");
            }
        }
        List<Task> taskList = new ArrayList<>();
        Map<Task, List<String>> taskUpdateMap = new HashMap<>();
        Map<Task, Task> updatedTaskToTaskCopy = new HashMap<>();
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            List<Task> childTaskList = taskRepository.findByParentTaskId(task.getTaskId());
            if (!childTaskList.isEmpty()) {
                for (Task childTask : childTaskList) {
                    if ((!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) &&
                            !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) ||
                            (Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) && childTask.getFkEpicId() == null)) {
                        Task childTaskRequest = new Task();
                        BeanUtils.copyProperties(childTask, childTaskRequest);
                        taskList.add(childTaskRequest);
                    }
                }
            }
        }
        taskList.add(task);

        for(Task foundTask : taskList) {
            List<String> updatedFields = new ArrayList<>();
            Task taskCopy = new Task();
            BeanUtils.copyProperties(foundTask, taskCopy);
            Task taskDb = taskRepository.findByTaskId(foundTask.getTaskId());
            if (taskDb != null && taskDb.getFkEpicId() == null && Objects.equals(taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                foundTask.setAddedInEpicAfterCompletion(true);
                validateCompletedWorkItemDateWithEpic (foundTask, epic);
            }
            // Add date validation
            if (epic.getExpEndDateTime() != null) {
                if (foundTask.getSprintId() != null) {
                    if (foundTask.getTaskExpEndDate().isAfter(epic.getExpEndDateTime()) || (epic.getExpStartDateTime() != null && foundTask.getTaskExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                        throw new ValidationFailedException("Work Item is part of sprint and it's expected date doesn't matched with epic's date");
                    }
                }
                if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) &&
                        (foundTask.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && foundTask.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || foundTask.getTaskExpEndDate().isAfter(epic.getExpEndDateTime()))) {
                    Task parentTask = taskRepository.findByTaskId(foundTask.getParentTaskId());
                    if(parentTask.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && parentTask.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || parentTask.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                        foundTask.setTaskExpEndDate(epic.getExpEndDateTime());
                        foundTask.setTaskExpEndTime(epic.getExpEndDateTime().toLocalTime());
                    }
                    else {
                        foundTask.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                        foundTask.setTaskExpEndTime(parentTask.getTaskExpEndDate().toLocalTime());
                    }
                    updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                }
                else if (foundTask.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && foundTask.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || foundTask.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                    foundTask.setTaskExpEndDate(epic.getExpEndDateTime());
                    foundTask.setTaskExpEndTime(epic.getExpEndDateTime().toLocalTime());
                    updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                }

            }
            if (epic.getExpStartDateTime() != null) {
                if (foundTask.getSprintId() != null) {
                    if (task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                        throw new ValidationFailedException("Work Item is part of sprint and it's expected date doesn't matched with epic's date");
                    }
                }
                if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) &&
                        (foundTask.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && foundTask.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || foundTask.getTaskExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                    Task parentTask = taskRepository.findByTaskId(foundTask.getParentTaskId());
                    if(parentTask.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && parentTask.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || parentTask.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                        foundTask.setTaskExpStartDate(epic.getExpStartDateTime());
                        foundTask.setTaskExpStartTime(epic.getExpStartDateTime().toLocalTime());
                    }
                    else {
                        foundTask.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                        foundTask.setTaskExpStartTime(parentTask.getTaskExpStartDate().toLocalTime());
                    }
                    updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                }
                else if (foundTask.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && foundTask.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || foundTask.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                    foundTask.setTaskExpStartDate(epic.getExpStartDateTime());
                    foundTask.setTaskExpStartTime(epic.getExpStartDateTime().toLocalTime());
                    updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                }
            }
            taskServiceImpl.validateExpDateTimeWithEstimate (foundTask);
            if (foundTask.getTaskEstimate() != null && !Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                List<Integer> statusList = new ArrayList<>();
                statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
                statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
                statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
                if (statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                    epic.setOriginalEstimate((epic.getOriginalEstimate() == null ? foundTask.getTaskEstimate() : epic.getOriginalEstimate() + foundTask.getTaskEstimate()));
                }
                epic.setRunningEstimate((epic.getRunningEstimate() == null ? foundTask.getTaskEstimate() : epic.getRunningEstimate() + foundTask.getTaskEstimate()));
            }
            foundTask.setFkEpicId(epic);
            updatedFields.add(Constants.TaskFields.EPIC_ID);
            if (accountId != null) {
                UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
                foundTask.setFkAccountIdLastUpdated(userAccount);
            }
            updatedTaskToTaskCopy.put(foundTask, taskCopy);
            taskUpdateMap.put(foundTask, updatedFields);
        }
        updatedTaskToTaskCopy.forEach((updatedTask, copyTask) -> {
            taskHistoryService.addTaskHistoryOnUserUpdate(copyTask);
        });
        taskRepository.saveAll(updatedTaskToTaskCopy.keySet());
        taskUpdateMap.forEach((taskKey, updatedFields) -> {
            taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, taskKey);
            if (epicTaskRepository.existsByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), taskKey.getTaskId())) {
                EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), taskKey.getTaskId());
                epicTask.setIsDeleted(false);
                epicTaskRepository.save(epicTask);
            } else {
                EpicTask epicTask = new EpicTask();
                epicTask.setFkEpicId(epic);
                epicTask.setFkTaskId(taskKey);
                epicTask.setIsDeleted(false);
                epicTaskRepository.save(epicTask);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeTaskFromEpic(Epic epic, Task task, Long accountId, Boolean isChildTaskIndividual) throws IllegalAccessException {

        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && isChildTaskIndividual) {
            throw new ValidationFailedException("User can not directly remove a child task from the Epic");
        }
        if(task.getFkEpicId() == null || !Objects.equals(task.getFkEpicId().getEpicId(), epic.getEpicId())) {
            throw new IllegalAccessException("Work Item is not present in epic");
        }
        if(isChildTaskIndividual && !validateUserRole(List.of(task.getFkTeamId().getTeamId()), accountId)) {
            throw new ValidationFailedException("User doesn't have permission to remove this Work Item from epic");
        }

        if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new ValidationFailedException("Deleted Work Item can't be removed from Epic");
        }
        if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
            throw new ValidationFailedException("Completed Work Item can't be removed from Epic");
        }
        List<Task> taskList = new ArrayList<>();
        Map<Task, List<String>> taskUpdateMap = new HashMap<>();
        Map<Task, Task> updatedTaskToTaskCopy = new HashMap<>();
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            List<Task> childTaskList = taskRepository.findByParentTaskId(task.getTaskId());
            if (!childTaskList.isEmpty()) {
                for (Task childTask : childTaskList) {
                    if (!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) &&
                            !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        Task childTaskRequest = new Task();
                        BeanUtils.copyProperties(childTask, childTaskRequest);
                        taskList.add(childTaskRequest);
                    }
                }
            }
        }
        taskList.add(task);

        for(Task foundTask : taskList) {
            List<String> updatedFields = new ArrayList<>();
            Task taskCopy = new Task();
            BeanUtils.copyProperties(foundTask, taskCopy);

            if (foundTask.getTaskEstimate() != null && !Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                List<Integer> statusList = new ArrayList<>();
                statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
                statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
                statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
                if (statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                    epic.setOriginalEstimate(epic.getOriginalEstimate() - foundTask.getTaskEstimate());
                }
                epic.setRunningEstimate(epic.getRunningEstimate() - foundTask.getTaskEstimate());
            }
            foundTask.setFkEpicId(null);
            updatedFields.add(Constants.TaskFields.EPIC_ID);
            if (accountId != null) {
                UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
                foundTask.setFkAccountIdLastUpdated(userAccount);
            }
            updatedTaskToTaskCopy.put(foundTask, taskCopy);
            taskUpdateMap.put(foundTask, updatedFields);
        }
        updatedTaskToTaskCopy.forEach((updatedTask, copyTask) -> {
            taskHistoryService.addTaskHistoryOnUserUpdate(copyTask);
        });
        taskRepository.saveAll(updatedTaskToTaskCopy.keySet());
        taskUpdateMap.forEach((taskKey, updatedFields) -> {
            taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, taskKey);
            EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), taskKey.getTaskId());
            epicTask.setIsDeleted(true);
            epicTaskRepository.save(epicTask);
        });
    }

    public void validateCompletedWorkItemDateWithEpic (Task foundTask, Epic epic) {
        if (epic.getExpEndDateTime() != null && foundTask.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
            throw new ValidationFailedException("Work Item exp end date time should not be after epic exp end date time");
        }
        else if (epic.getExpStartDateTime() != null && !foundTask.getTaskExpEndDate().isAfter(epic.getExpStartDateTime())) {
            throw new ValidationFailedException("Work Item exp end date time should be after epic start date time");
        }
    }

    public void validationForCompletedEpic(List<Task> taskList, EpicRequest epicRequest) {
        if(taskList == null || taskList.isEmpty()) {
            throw new ValidationFailedException("There should be at least one Work Item in epic to mark it complete");
        }
        if(epicRequest.getActStartDateTime() == null || epicRequest.getActEndDateTime() == null) {
            throw new ValidationFailedException("Actual start and end date can't be null to complete epic");
        }
        if(!epicRequest.getActStartDateTime().isBefore(epicRequest.getActEndDateTime())) {
            throw new ValidationFailedException("Actual end date and time should be after actual start date and time");
        }
        if(epicRequest.getActEndDateTime().isAfter(LocalDateTime.now())) {
            throw new ValidationFailedException("Actual end date and time of epic can't be after current date and time");
        }
        for(Task task : taskList) {
            if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                continue;
            }
            if(!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                throw new ValidationFailedException("All Work Item should be completed in complete epic");
            }
        }
    }

    public void validateEpicNotInBacklog(EpicRequest epicRequest) {
        if(epicRequest.getExpStartDateTime() == null || epicRequest.getExpEndDateTime() == null) {
            throw new ValidationFailedException("Epic expected started and end date should be present");
        }
        if(!epicRequest.getExpStartDateTime().isBefore(epicRequest.getExpEndDateTime())) {
            throw new ValidationFailedException("Expected start date and time should be before expected end date and time");
        }
        if(epicRequest.getEstimate() == null) {
            throw new ValidationFailedException("Epic estimate can't be null");
        }
        if(epicRequest.getEpicPriority() == null) {
            throw new ValidationFailedException("Epic priority can't be null");
        }
        if(epicRequest.getAssignTo() == null) {
            throw new ValidationFailedException("Epic should assigned to someone");
        }
        if(epicRequest.getEpicOwner() == null) {
            throw new ValidationFailedException("Owner field can't be null");
        }
        if(epicRequest.getColor() == null) {
            throw new ValidationFailedException("Epic color can't be null");
        }
    }

    public UpdateEpicResponse validateEpicTaskDate(EpicRequest epicRequest, Epic epicDb) {
        UpdateEpicResponse updateEpicResponse = new UpdateEpicResponse();
        if(epicRequest.getExpStartDateTime() != null || epicRequest.getExpEndDateTime() != null){
            if(!Objects.equals(epicDb.getExpStartDateTime(), epicRequest.getExpStartDateTime()) || !Objects.equals(epicDb.getExpEndDateTime(), epicRequest.getExpEndDateTime())) {
                List<TaskForBulkResponse> taskForBulkResponses = new ArrayList<>();
                List<Task> taskResponse = new ArrayList<>();
                List<Task> taskList = epicTaskRepository.findFkTaskIdByFkEpicIdEpicIdAndIsDeleted(epicRequest.getEpicId(), false);
                for(Task task : taskList) {
                    if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        continue;
                    }
                    Boolean isValid = true;
                    if(epicRequest.getExpStartDateTime() != null) {
                        if((task.getTaskExpStartDate() != null && task.getTaskExpStartDate().isBefore(epicRequest.getExpStartDateTime())) || (task.getTaskExpEndDate() != null && task.getTaskExpEndDate().isBefore(epicRequest.getExpStartDateTime()))) {
                            isValid = false;
                        }
                    }
                    if(isValid && epicRequest.getExpEndDateTime() != null) {
                        if((task.getTaskExpStartDate() != null && task.getTaskExpStartDate().isAfter(epicRequest.getExpEndDateTime())) || (task.getTaskExpEndDate() != null && task.getTaskExpEndDate().isAfter(epicRequest.getExpEndDateTime()))) {
                            isValid = false;
                        }
                    }
                    if(!isValid) {
                        taskResponse.add(task);
                    }
                }
                if(!taskResponse.isEmpty()) {
                    for (Task task : taskResponse) {
                        TaskForBulkResponse taskDateResponse = new TaskForBulkResponse();
                        BeanUtils.copyProperties(task, taskDateResponse);
                        taskDateResponse.setTeamId(task.getFkTeamId().getTeamId());
                        taskForBulkResponses.add(taskDateResponse);
                    }
                    updateEpicResponse.setEpicResponse(null);
                    updateEpicResponse.setTaskForBulkResponses(taskForBulkResponses);
                    updateEpicResponse.setMessage("Work Item expected date is out of epic expected date");
                }
            }
        }
        return updateEpicResponse;
    }

    public Boolean validateExpectedDate(EpicRequest epicRequest) {
        if(epicRequest.getExpStartDateTime() != null && epicRequest.getExpEndDateTime() != null) {
            if(!epicRequest.getExpStartDateTime().isBefore(epicRequest.getExpEndDateTime())) return false;
        }
        return true;
    }

    public void validateDueDateWithExpectedDate(EpicRequest epicRequest) {
        if(epicRequest.getExpStartDateTime() != null && epicRequest.getDueDateTime() != null && !epicRequest.getExpStartDateTime().isBefore(epicRequest.getDueDateTime())) {
            throw new ValidationFailedException("Expected start date and time should be before Due date and time");
        }
        if(epicRequest.getExpEndDateTime() != null && epicRequest.getDueDateTime() != null && epicRequest.getExpEndDateTime().isAfter(epicRequest.getDueDateTime())) {
            throw new ValidationFailedException("Expected end date and time should be before or equal to Due date and time");
        }
    }

    public void validationForInProgressAndBlocked(EpicRequest epicRequest) {
        List<Integer> statusIdList = new ArrayList<>();
        statusIdList.add(Constants.EpicStatusEnum.STATUS_BLOCKED.getWorkflowEpicStatusId());
        statusIdList.add(Constants.EpicStatusEnum.STATUS_IN_PROGRESS.getWorkflowEpicStatusId());
        if(statusIdList.contains(epicRequest.getWorkflowEpicStatusId()) && epicRequest.getActStartDateTime() == null) {
            throw new ValidationFailedException("Please fill the actual start date field");
        }
    }

    public void updateTaskExpectedDate(EpicRequest epicRequest, List<Task> taskList) {
        for (Task task : taskList) {
            boolean isUpdated = false;
            List<String> updatedFields = new ArrayList<>();
            Task taskCopy = new Task();
            BeanUtils.copyProperties(task, taskCopy);

            if (task.getTaskExpEndDate() == null && epicRequest.getExpEndDateTime() != null) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                    Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                    if (parentTask.getTaskExpEndDate() != null) {
                        task.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                        task.setTaskExpEndTime(parentTask.getTaskExpEndTime());
                    } else {
                        task.setTaskExpEndDate(epicRequest.getExpEndDateTime());
                        task.setTaskExpEndTime(epicRequest.getExpEndDateTime().toLocalTime());
                    }
                } else {
                    task.setTaskExpEndDate(epicRequest.getExpEndDateTime());
                    task.setTaskExpEndTime(epicRequest.getExpEndDateTime().toLocalTime());
                }
                updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                isUpdated = true;
            }

            if (task.getTaskExpStartDate() == null && epicRequest.getExpStartDateTime() != null) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                    Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                    if (parentTask.getTaskExpStartDate() != null) {
                        task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                        task.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                    } else {
                        task.setTaskExpStartDate(epicRequest.getExpStartDateTime());
                        task.setTaskExpStartTime(epicRequest.getExpStartDateTime().toLocalTime());
                    }
                } else {
                    task.setTaskExpStartDate(epicRequest.getExpStartDateTime());
                    task.setTaskExpStartTime(epicRequest.getExpStartDateTime().toLocalTime());
                }
                updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                isUpdated = true;
            }

            if (isUpdated) {
                Task savedTask = taskRepository.save(task);
                taskHistoryService.addTaskHistoryOnSystemUpdate(taskCopy);
                taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, savedTask);
            }
        }
    }

    public void validateEpicForNotInBacklogAndNotInReview(EpicRequest epicRequest) {
        Epic epicDb = epicRepository.findByEpicId(epicRequest.getEpicId());

        if(epicRequest.getAddTeamList() != null && !epicRequest.getAddTeamList().isEmpty()) {
            throw new ValidationFailedException("Team can be added only in Backlog or In Review status");
        }
        if(epicRequest.getRemoveTeamList() != null && !epicRequest.getRemoveTeamList().isEmpty()) {
            throw new ValidationFailedException("Team can be removed only in Backlog or In Review status");
        }
    }

    public List<TaskForBulkResponse> validateAndAddAndRemoveTeamFromEpic(EpicRequest epicRequest, Epic epicDb, Long userAccountId, String accountIds) {
        // Validate and add team in epic
        List<Long> teamIdList = epicDb.getTeamIdList();
        if(epicRequest.getAddTeamList() != null && !epicRequest.getAddTeamList().isEmpty()) {
            validateTeamList(epicRequest.getAddTeamList(), epicRequest.getProjectId());
            hasPermissionToModifyEpicForTeamList(epicDb.getTeamIdList(), userAccountId);
            hasPermissionToModifyEpicForTeamList(epicRequest.getAddTeamList(), userAccountId);
            List<Long> addedTeamIdList = epicRequest.getAddTeamList();
            for(Long addedTeamId : addedTeamIdList) {
                if(!teamIdList.contains(addedTeamId)) {
                    teamIdList.add(addedTeamId);
                }
            }
        }

        // Validate and remove team from epic
        List<TaskForBulkResponse> taskForBulkResponsesList = new ArrayList<>();
        if(epicRequest.getRemoveTeamList() != null && !epicRequest.getRemoveTeamList().isEmpty()) {
            if(epicRequest.getAddTeamList() == null || epicRequest.getAddTeamList().isEmpty()) {
                hasPermissionToModifyEpicForTeamList(epicDb.getTeamIdList(), userAccountId);
            }
            List<Long> removeTeamIdList = epicRequest.getRemoveTeamList();
            for(Long removeTeamId : removeTeamIdList) {
                if(teamIdList.contains(removeTeamId)) {
                    List<Task> taskList = epicTaskRepository.findFkTaskIdByFkEpicIdEpicIdAndFkTeamIdTeamIdInAndIsDeleted(epicDb.getEpicId(), List.of(removeTeamId), false);
                    List<Long> taskIdList = taskList.stream()
                            .map(Task::getTaskId)
                            .collect(Collectors.toList());
                    if(!taskIdList.isEmpty()) {
                        TaskListForBulkResponse taskListForBulkResponse = removeAllTaskFromEpic(epicDb.getEpicId(), taskIdList, accountIds);
                        taskForBulkResponsesList.addAll(taskListForBulkResponse.getSuccessList());
                    }
                    teamIdList.remove(removeTeamId);
                }
            }
        }
        epicDb.setTeamIdList(teamIdList);
        return taskForBulkResponsesList;
    }

    public List<Long> getTeamOfUserFromEpicTeamList(List<Long> teamIdList, Long accountId) {
        List<Long> teamIdListOfUser = new ArrayList<>();
        for (Long teamId : teamIdList) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdNotIn(Constants.EntityTypes.TEAM, teamId, accountId, true, List.of(RoleEnum.TASK_BASIC_USER.getRoleId()))) {
                teamIdListOfUser.add(teamId);
            }
        }
        return teamIdListOfUser;
    }

    public List<Long> getTeamToCreateEpic(List<Long> teamIdList, Long accountId) {
        List<Integer> authorizeRoleIdList = Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION;

        List<Long> authorizedTeamList = new ArrayList<>();
        for(Long teamId : teamIdList) {
            if(accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, teamId, accountId, true, authorizeRoleIdList)) {
                authorizedTeamList.add(teamId);
            }
        }
        return authorizedTeamList;
    }

    List<TeamResponse> setTeamResponse(List<Long> teamIdList) {
        List<TeamResponse> teamResponseList = new ArrayList<>();
        List<Team> teamList = teamRepository.findByTeamIdIn(teamIdList);
        for (Team team : teamList) {
            TeamResponse teamResponse = new TeamResponse();
            BeanUtils.copyProperties(team, teamResponse);
            teamResponseList.add(teamResponse);
        }
        return teamResponseList;
    }

    public List<EpicDetailsResponse> sortEpicByPriorityAndStatus(List<EpicDetailsResponse> epicDetailsResponseList) {
        synchronized (epicDetailsResponseList) {
            Thread sortThread = new Thread(() -> {
                epicDetailsResponseList.sort(Comparator.comparing((EpicDetailsResponse epic) -> {
                    if (epic.getPriority() != null) {
                        switch (epic.getPriority()) {
                            case "P0":
                                return 1;
                            case "P1":
                                return 2;
                            case "P2":
                                return 3;
                            case "P3":
                                return 4;
                            case "P4":
                                return 5;
                        }
                    }
                    return 6;
                }).thenComparing((EpicDetailsResponse epic) -> {
                    if (epic.getFkworkFlowEpicStatus() != null) {
                        String workflowStatusType = epic.getFkworkFlowEpicStatus().getWorkflowEpicStatus();
                        switch (workflowStatusType) {
                            case Constants.WorkflowEpicStatusConstants.STATUS_IN_PROGRESS:
                                return 1;
                            case Constants.WorkflowEpicStatusConstants.STATUS_REVIEWED:
                                return 2;
                            case Constants.WorkflowEpicStatusConstants.STATUS_IN_REVIEW:
                                return 3;
                            case Constants.WorkflowEpicStatusConstants.STATUS_BACKLOG:
                                return 4;
                            case Constants.WorkflowEpicStatusConstants.STATUS_ON_HOLD:
                                return 5;
                            case Constants.WorkflowEpicStatusConstants.STATUS_BLOCKED:
                                return 6;
                            case Constants.WorkflowEpicStatusConstants.STATUS_COMPLETED:
                                return 7;
                            case Constants.WorkflowEpicStatusConstants.STATUS_DELETED:
                                return 8;
                        }
                    }
                    return 9;
                }));
            });
            sortThread.start();

            try {
                sortThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return epicDetailsResponseList;
    }

    public EpicResponse createEpic(EpicRequest epicRequest, String accountIds) throws IllegalAccessException, InvalidKeyException {

        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(accountIdList, epicRequest.getOrgId(), true);

        removeLeadingAndTrailingSpacesForEpic(epicRequest);
        if(userAccount == null) {
            throw new ValidationFailedException("User is not present in the organisation");
        }
        if(!Objects.equals(epicRequest.getEntityTypeId(), Constants.EntityTypes.TEAM) && !Objects.equals(epicRequest.getEntityTypeId(), Constants.EntityTypes.PROJECT)) {
            throw new ValidationFailedException("Epic can be created only for either at project level or team level");
        }
        if((epicRequest.getAddTeamList() == null || epicRequest.getAddTeamList().isEmpty())) {
            throw new ValidationFailedException("Atleast one team should be in epic");
        }
        if(Objects.equals(epicRequest.getEntityTypeId(), Constants.EntityTypes.TEAM) && epicRequest.getAddTeamList().size() != 1) {
            throw new ValidationFailedException("At team level only one team can be added");
        }
        validateTeamList(epicRequest.getAddTeamList(), epicRequest.getProjectId());
        hasPermissionToModifyEpicForTeamList(epicRequest.getAddTeamList(), userAccount.getAccountId());
        if (epicRequest.getExpEndDateTime() != null && epicRequest.getExpStartDateTime() != null) {
            if(!epicRequest.getExpStartDateTime().isBefore(epicRequest.getExpEndDateTime())) {
                throw new ValidationFailedException("Expected Start Date and Time should be before Expected End Date and Time");
            }
        }
        validateDueDateWithExpectedDate(epicRequest);
        if(epicRequest.getAssignTo() != null) {
            if(!validateUserRole(epicRequest.getAddTeamList(), epicRequest.getAssignTo())) {
                throw new IllegalStateException("Assigned user should have essential role in at least one team");
            }
        }
        if(epicRequest.getEpicOwner() != null) {
            if(!validateUserRole(epicRequest.getAddTeamList(), epicRequest.getEpicOwner())) {
                throw new IllegalStateException("Owner should have essential role in at least one team");
            }
        }

        if (epicRequest.getEpicTitle() == null) {
            throw new ValidationFailedException("Epic Title can't be null");
        }
        if (epicRequest.getEpicDesc() == null) {
            throw new ValidationFailedException("Epic Description can't be null");
        }
        if(epicRequest.getEpicPriority() != null) {
            String priority = epicRequest.getEpicPriority().substring(0, 2);
            List<String> priorityList = Constants.PRIORITY_LIST;
            if (!priorityList.contains(priority)) {
                throw new IllegalAccessException("Priority is not valid");
            }
        }

        Epic epic = new Epic();
        epic.setEpicTitle(epicRequest.getEpicTitle());
        epic.setEpicDesc(epicRequest.getEpicDesc());
        Long epicIdentifier = getNextEpicIdentifier(epicRequest.getProjectId());
        epic.setEpicNumber("E-" + epicIdentifier);
        if(epicRequest.getExpStartDateTime() != null) {
            epic.setExpStartDateTime(epicRequest.getExpStartDateTime());
        }
        if(epicRequest.getExpEndDateTime() != null) {
            epic.setExpEndDateTime(epicRequest.getExpEndDateTime());
        }
        if(epicRequest.getDueDateTime() != null) {
            epic.setDueDateTime(epicRequest.getDueDateTime());
        }
        epic.setEntityTypeId(epicRequest.getEntityTypeId());
        if(Objects.equals(epicRequest.getEntityTypeId(), Constants.EntityTypes.TEAM)) {
            epic.setEntityId(epicRequest.getAddTeamList().get(0));
        }
        else if(Objects.equals(epicRequest.getEntityTypeId(), Constants.EntityTypes.PROJECT)) {
            epic.setEntityId(epicRequest.getProjectId());
        }
        epic.setFkOrgId(organizationRepository.findByOrgId(epicRequest.getOrgId()));
        epic.setFkProjectId(projectRepository.findByProjectId(epicRequest.getProjectId()));
        epic.setFkAccountIdAssigned(userAccountRepository.findByAccountId(epicRequest.getAssignTo()));
        epic.setFkEpicOwner(userAccountRepository.findByAccountId(epicRequest.getEpicOwner()));
        epic.setTeamIdList(epicRequest.getAddTeamList());

        if(epicRequest.getEstimate() != null){
            epic.setEstimate(epicRequest.getEstimate());
        }
        if(epicRequest.getEpicPriority() != null) {
            String priority = epicRequest.getEpicPriority().substring(0, 2);
            epic.setEpicPriority(priority);
        }
        if(epicRequest.getColor() != null) {
            epic.setColor(epicRequest.getColor());
        }
        epic.setFkWorkflowEpicStatus(workFlowEpicStatusRepository.findByWorkflowEpicStatus(Constants.WorkflowEpicStatusConstants.STATUS_BACKLOG));
        epic.setValueArea(epicRequest.getValueArea());
        epic.setFunctionalArea(epicRequest.getFunctionalArea());
        epic.setQuarterlyPlan(epicRequest.getQuarterlyPlan());
        epic.setYearlyPlan(epicRequest.getYearlyPlan());
        epic.setRelease(epicRequest.getRelease());

        Epic savedEpic = epicRepository.save(epic);

        EpicResponse epicResponse = new EpicResponse();
        BeanUtils.copyProperties(savedEpic, epicResponse);
        epicResponse.setOrgId(savedEpic.getFkOrgId().getOrgId());
        epicResponse.setProjectId(savedEpic.getFkProjectId().getProjectId());
        if (savedEpic.getFkAccountIdAssigned() != null) {
            epicResponse.setAccountIdAssigned(savedEpic.getFkAccountIdAssigned().getAccountId());
        }
        if(savedEpic.getFkEpicOwner() != null) {
            epicResponse.setAccountIdOwner(savedEpic.getFkEpicOwner().getAccountId());
        }
        List<TeamResponse> teamResponseList = setTeamResponse(savedEpic.getTeamIdList());
        epicResponse.setTeamList(teamResponseList);

        auditService.auditForEpic(userAccount, savedEpic, false);
        return epicResponse;
    }

    public AllEpicResponse getAllEpic(Integer entityTypeId, Long entityId, String accountIds, String timeZone) throws IllegalAccessException {
        Long orgId = null;
        if(Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            orgId = projectRepository.findByProjectId(entityId).getOrgId();
        }
        else if(Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            orgId = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(orgId, true, accountIdList);
        if(userAccountId == null) {
            throw new ValidationFailedException("User is not present in the organisation");
        }
        List<Epic> epicList = new ArrayList<>();
        if(Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            epicList = epicRepository.findByFkProjectIdProjectId(entityId);
        }
        else if(Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdNotIn(Constants.EntityTypes.TEAM, entityId, userAccountId, true, List.of(RoleEnum.TASK_BASIC_USER.getRoleId()))) {
                epicList = epicRepository.findByTeamId(entityId.toString());
            }
            else {
                throw new ValidationFailedException("User does not have permission to view the epic of selected team");
            }
        }

        AllEpicResponse allEpicResponse = new AllEpicResponse();
        List<EpicDetailsResponse> epicDetailsResponseList = new ArrayList<>();
        for(Epic epic : epicList) {
            List<Long> userTeamIdList = getTeamOfUserFromEpicTeamList(epic.getTeamIdList(), userAccountId);
            if (userTeamIdList != null && !userTeamIdList.isEmpty()) {
                EpicDetailsResponse epicDetailsResponse = new EpicDetailsResponse();
                epicDetailsResponse.setEpicId(epic.getEpicId());
                epicDetailsResponse.setEpicNumber(epic.getEpicNumber());
                epicDetailsResponse.setEpicTitle(epic.getEpicTitle());
                epicDetailsResponse.setFkworkFlowEpicStatus(epic.getFkWorkflowEpicStatus());
                epicDetailsResponse.setPriority(epic.getEpicPriority());
                if (epic.getFkAccountIdAssigned() != null && epic.getFkAccountIdAssigned().getAccountId() != null) {
                    EmailFirstLastAccountIdIsActive emailFirstLastAccountIdIsActive = getUserAccountDetails(epic);
                    epicDetailsResponse.setEmailFirstLastAccountIdIsActive(emailFirstLastAccountIdIsActive);
                }
                epicDetailsResponse.setColor(epic.getColor());
                epicDetailsResponse.setExpStartDateTime(epic.getExpStartDateTime());
                epicDetailsResponse.setExpEndDateTime(epic.getExpEndDateTime());
                epicDetailsResponse.setActStartDateTime(epic.getActStartDateTime());
                epicDetailsResponse.setActEndDateTime(epic.getActEndDateTime());
                epicDetailsResponse.setDueDateTime(epic.getDueDateTime());
                epicDetailsResponse.setLoggedEffort(epic.getLoggedEfforts());
                epicDetailsResponse.setEarnedEffort(epic.getEarnedEfforts());
                epicDetailsResponse.setEstimate(epic.getEstimate());
                epicDetailsResponse.setOriginalEstimate(epic.getOriginalEstimate());
                epicDetailsResponse.setRunningEstimate(epic.getRunningEstimate());
                convertEpicDetailResponseDateTimeToUserTimeZone(epicDetailsResponse, timeZone);
                if (Objects.equals(Constants.EntityTypes.PROJECT, epic.getEntityTypeId())) {
                    epicDetailsResponse.setEntityName(epic.getFkProjectId().getProjectName());
                }
                else if (Objects.equals(Constants.EntityTypes.TEAM, epic.getEntityTypeId())) {
                    epicDetailsResponse.setEntityName(teamRepository.findTeamNameByTeamId(epic.getEntityId()));
                }

                List<Task> taskList = epicTaskRepository.findFkTaskIdByFkEpicIdEpicIdAndFkTeamIdTeamIdInAndIsDeletedWithoutChildTask(epic.getEpicId(), userTeamIdList, false);
                getAndSetAllStatusTaskCount (epicDetailsResponse, taskList);
                epicDetailsResponse.setNumberOfTask(taskList.size());
                epicDetailsResponseList.add(epicDetailsResponse);
            }
        }
        epicDetailsResponseList = sortEpicByPriorityAndStatus(epicDetailsResponseList);
        allEpicResponse.setEpicDetailsResponseList(epicDetailsResponseList);
        return allEpicResponse;
    }

    private EmailFirstLastAccountIdIsActive getUserAccountDetails(Epic epic) {
        UserAccount assignedUserAccount = epic.getFkAccountIdAssigned();
        EmailFirstLastAccountIdIsActive emailFirstLastAccountIdIsActive = new EmailFirstLastAccountIdIsActive();
        emailFirstLastAccountIdIsActive.setAccountId(assignedUserAccount.getAccountId());
        emailFirstLastAccountIdIsActive.setEmail(assignedUserAccount.getEmail());
        emailFirstLastAccountIdIsActive.setIsActive(assignedUserAccount.getIsActive());
        emailFirstLastAccountIdIsActive.setFirstName(assignedUserAccount.getFkUserId().getFirstName());
        emailFirstLastAccountIdIsActive.setLastName(assignedUserAccount.getFkUserId().getLastName());
        return emailFirstLastAccountIdIsActive;
    }

    private void getAndSetAllStatusTaskCount(EpicDetailsResponse epicDetailsResponse, List<Task> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            epicDetailsResponse.setNumberOfBacklogTask(0);
            epicDetailsResponse.setNumberOfNotStartedTask(0);
            epicDetailsResponse.setNumberOfStartedTask(0);
            epicDetailsResponse.setNumberOfCompletedTask(0);
            epicDetailsResponse.setNumberOfDeletedTask(0);
            return;
        }

        AtomicInteger backlogEpicTaskCount = new AtomicInteger(0);
        AtomicInteger totalNotStartedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger notStartedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger notStartedBlockedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger totalStartedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger startedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger startedOnHoldEpicTaskCount = new AtomicInteger(0);
        AtomicInteger startedBlockedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger completedEpicTaskCount = new AtomicInteger(0);
        AtomicInteger deletedEpicTaskCount = new AtomicInteger(0);

        taskList.parallelStream().forEach(task -> {
            if (task == null || task.getFkWorkflowTaskStatus() == null || task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() == null) {
                return;
            }

            String status = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus();

            if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                backlogEpicTaskCount.incrementAndGet();
            }
            else if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
                totalNotStartedEpicTaskCount.incrementAndGet();
                notStartedEpicTaskCount.incrementAndGet();
            }
            else if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                totalStartedEpicTaskCount.incrementAndGet();
                startedEpicTaskCount.incrementAndGet();
            }
            else if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                completedEpicTaskCount.incrementAndGet();
            }
            else if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                if (task.getTaskActStDate() == null) {
                    totalNotStartedEpicTaskCount.incrementAndGet();
                    notStartedBlockedEpicTaskCount.incrementAndGet();
                } else {
                    totalStartedEpicTaskCount.incrementAndGet();
                    if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE)) {
                        startedOnHoldEpicTaskCount.incrementAndGet();
                    }
                    else {
                        startedBlockedEpicTaskCount.incrementAndGet();
                    }
                }
            }
            else if (Objects.equals(status, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                deletedEpicTaskCount.incrementAndGet();
            }
        });

        epicDetailsResponse.setNumberOfBacklogTask(backlogEpicTaskCount.get());
        epicDetailsResponse.setNumberOfTotalNotStartedTask(totalNotStartedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfNotStartedTask(notStartedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfNotStartedBlockedTask(notStartedBlockedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfTotalStartedTask(totalStartedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfStartedTask(startedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfStartedBlockedTask(startedBlockedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfStartedOnHoldTask(startedOnHoldEpicTaskCount.get());
        epicDetailsResponse.setNumberOfCompletedTask(completedEpicTaskCount.get());
        epicDetailsResponse.setNumberOfDeletedTask(deletedEpicTaskCount.get());
    }

    public EpicDetailResponse getEpic(Long epicId, String accountIds, String timeZone) throws IllegalAccessException {
        Optional<Epic> optionalEpic = epicRepository.findById(epicId);

        if (optionalEpic.isEmpty()) {
            throw new EntityNotFoundException("Epic not found");
        }

        Epic epic = optionalEpic.get();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epic.getFkOrgId().getOrgId(), true, accountIdList);

        if (userAccountId == null) {
            throw new ValidationFailedException("User is not present in the organisation");
        }

        List<Long> userTeamIdList = getTeamOfUserFromEpicTeamList(epic.getTeamIdList(), userAccountId);
        if (userTeamIdList == null || userTeamIdList.isEmpty()) {
            throw new ValidationFailedException("User does not have permission to view the epic");
        }
        EpicDetailResponse getEpicResponse = new EpicDetailResponse();
        List<Task> taskList = epicTaskRepository.findFkTaskIdByFkEpicIdEpicIdAndFkTeamIdTeamIdInAndIsDeletedWithoutChildTask(epicId, userTeamIdList, false);
        EpicTaskByFilterResponse epicTaskByFilterResponse = getEpicTaskResponseList(taskList);
        EpicResponse epicResponse = new EpicResponse();
        BeanUtils.copyProperties(epic, epicResponse);
        epicResponse.setOrgId(epic.getFkOrgId().getOrgId());
        epicResponse.setProjectId(epic.getFkProjectId().getProjectId());
        if (epic.getFkAccountIdAssigned() != null) {
            epicResponse.setAccountIdAssigned(epic.getFkAccountIdAssigned().getAccountId());
        }
        if (epic.getFkEpicOwner() != null) {
        epicResponse.setAccountIdOwner(epic.getFkEpicOwner().getAccountId());
        }
        List<TeamResponse> teamResponseList = setTeamResponse(epic.getTeamIdList());
        epicResponse.setTeamList(teamResponseList);
        if (Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            WorkItemOfDeletedEpic workItemOfDeletedEpic = new WorkItemOfDeletedEpic();
            workItemOfDeletedEpic.setBaclogWorkItemList(epic.getBacklogWorkItemList());
            workItemOfDeletedEpic.setNotStartedWorkItemList(epic.getNotStartedWorkItemList());
            workItemOfDeletedEpic.setStartedWorkItemList(epic.getStartedWorkItemList());
            workItemOfDeletedEpic.setCompletedWorkItemList(epic.getCompletedWorkItemList());
            getEpicResponse.setWorkItemOfDeletedEpic(workItemOfDeletedEpic);
        }
        convertAllEpicDateToUserTimeZone(epicResponse, timeZone);
        getEpicResponse.setEpicResponse(epicResponse);
        getEpicResponse.setEpicTaskWithStatusList(epicTaskByFilterResponse.getEpicTaskWithStatusList());

        return getEpicResponse;
    }

    public TaskListForBulkResponse addAllTaskInEpic(Long epicId, List<Long> taskIds, String accountIds) {
        Optional<Epic> epicDb = epicRepository.findById(epicId);
        if(epicDb.isEmpty()) {
            throw new ValidationFailedException("Epic is not valid");
        }
        Epic epic = epicDb.get();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epic.getFkOrgId().getOrgId(), true, accountIdList);
        if(userAccountId == null) {
            throw new ValidationFailedException("User is not part of the organisation");
        }

        if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatus(), Constants.WorkflowEpicStatusConstants.STATUS_COMPLETED)) {
            throw new IllegalStateException("Work Item can't be added in completed epic");
        }
        if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatus(), Constants.WorkflowEpicStatusConstants.STATUS_DELETED)) {
            throw new IllegalStateException("Work Item can't be added in deleted epic");
        }
        TaskListForBulkResponse taskListForBulkResponse = new TaskListForBulkResponse();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();

        for (Long taskId : taskIds) {
            Task foundTask = taskRepository.findByTaskId(taskId);
            try {
                if (foundTask == null) {
                    throw new EntityNotFoundException("Work Item not found");
                }
                Task foundTaskCopy = new Task();
                BeanUtils.copyProperties(foundTask, foundTaskCopy);
                addTaskToEpic(epic, foundTaskCopy, userAccountId, true);
                successList.add(new TaskForBulkResponse(foundTask.getTaskId(), foundTask.getTaskNumber(), foundTask.getTaskTitle(), foundTask.getFkTeamId().getTeamId(), "Work Item successfully added to epic"));
            } catch (Exception e) {
                failureList.add(new TaskForBulkResponse(foundTask != null ? foundTask.getTaskId() : taskId, foundTask != null ? foundTask.getTaskNumber() : "Work Item not found", foundTask != null ? foundTask.getTaskTitle() : "Work Item not found", foundTask != null ? foundTask.getFkTeamId().getTeamId() : null, e.getMessage()));
            }
        }

        epicRepository.save(epic);
        taskListForBulkResponse.setSuccessList(successList);
        taskListForBulkResponse.setFailureList(failureList);
        return taskListForBulkResponse;
    }

    public TaskListForBulkResponse removeAllTaskFromEpic(Long epicId, List<Long> taskIds, String accountIds) {
        Optional<Epic> epicDb = epicRepository.findById(epicId);
        if(epicDb.isEmpty()) {
            throw new NoDataFoundException();
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Epic epic = epicDb.get();

        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epic.getFkOrgId().getOrgId(), true, accountIdList);
        if(userAccountId == null) {
            throw new ValidationFailedException("User is not part of the organisation");
        }
        if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
            throw new IllegalStateException("Work Item can't be removed from completed epic");
        }
        if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatus(), Constants.WorkflowEpicStatusConstants.STATUS_DELETED)) {
            throw new IllegalStateException("Work Item can't be removed from deleted epic");
        }
        TaskListForBulkResponse taskListForBulkResponse = new TaskListForBulkResponse();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();

        for (Long taskId : taskIds) {
            Task foundTask = taskRepository.findByTaskId(taskId);
            try {
                if (foundTask == null) {
                    throw new EntityNotFoundException("Work Item not found");
                }
                Task foundTaskCopy = new Task();
                BeanUtils.copyProperties(foundTask, foundTaskCopy);
                removeTaskFromEpic(epic, foundTaskCopy, userAccountId, true);
                successList.add(new TaskForBulkResponse(foundTask.getTaskId(), foundTask.getTaskNumber(), foundTask.getTaskTitle(), foundTask.getFkTeamId().getTeamId(),"Work Item successfully removed from epic"));
            } catch (Exception e) {
                failureList.add(new TaskForBulkResponse(foundTask.getTaskId(), foundTask.getTaskNumber(), foundTask.getTaskTitle(), foundTask != null ? foundTask.getFkTeamId().getTeamId() : null,  e.getMessage()));
            }
        }

        epicRepository.save(epic);
        taskListForBulkResponse.setSuccessList(successList);
        taskListForBulkResponse.setFailureList(failureList);
        return taskListForBulkResponse;
    }

    public UpdateEpicResponse updateEpic(EpicRequest epicRequest, String accountIds, String timeZone) throws IllegalAccessException, InvalidKeyException {
        UpdateEpicResponse updateEpicResponse = new UpdateEpicResponse();
        Optional<Epic> optionalEpic = epicRepository.findById(epicRequest.getEpicId());
        if(optionalEpic.isEmpty()) {
            throw new ValidationFailedException("Epic is not valid");
        }
        Epic epicDb = optionalEpic.get();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(accountIdList, epicRequest.getOrgId(), true);
        removeLeadingAndTrailingSpacesForEpic (epicRequest);
        if(userAccount == null) {
            throw new ValidationFailedException("User is not part of the organisation");
        }
        if(!validateUserRole(epicDb.getTeamIdList(), userAccount.getAccountId())) {
            throw new IllegalStateException("User does not have permission to update epic");
        }
        List<Task> taskList = epicTaskRepository.findFkTaskIdByFkEpicIdEpicIdAndIsDeleted(epicDb.getEpicId(), false).stream()
                .map(originalTask -> {
                    Task copy = new Task();
                    BeanUtils.copyProperties(originalTask, copy);
                    return copy;
                })
                .collect(Collectors.toList());
        WorkFlowEpicStatus workFlowEpicStatus = new WorkFlowEpicStatus();
        if(epicRequest.getWorkflowEpicStatusId() != null) {
            workFlowEpicStatus = workFlowEpicStatusRepository.findByWorkflowEpicStatusId(epicRequest.getWorkflowEpicStatusId());
        }
        if(epicRequest.getWorkflowEpicStatusId() == null || workFlowEpicStatus == null) {
            throw new ValidationFailedException("Please enter the epic status");
        }
        if(!Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId()) &&
                Objects.equals(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
            if (Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
                throw new ValidationFailedException("Completed Epic can't be updated");
            }
            else {
                throw new ValidationFailedException("Status of Completed Epic can't be changed to " + workFlowEpicStatus.getWorkflowEpicStatus());
            }
        }
        if(Objects.equals(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            throw new ValidationFailedException("Deleted Epic can't be updated");
        }
        if(!Objects.equals(epicDb.getFkOrgId().getOrgId(), epicRequest.getOrgId())) {
            throw new ValidationFailedException("Epic's organisation can't be changed");
        }
        if(!Objects.equals(epicDb.getFkProjectId().getProjectId(), epicRequest.getProjectId())) {
            throw new ValidationFailedException("Epic's project can't be changed");
        }
        if(!Objects.equals(epicDb.getEntityTypeId(), epicRequest.getEntityTypeId())) {
            throw new ValidationFailedException("Epic's level can't be changed");
        }
        if(epicRequest.getEpicPriority() != null) {
            String priority = epicRequest.getEpicPriority().substring(0, 2);
            List<String> priorityList = Constants.PRIORITY_LIST;
            if (!priorityList.contains(priority)) {
                throw new IllegalAccessException("Priority is not valid");
            }
        }
        if(!validateExpectedDate(epicRequest)) {
            throw new ValidationFailedException("Expected start date and time should be before expected end date and time");
        }
        validateDueDateWithExpectedDate(epicRequest);
        if(epicRequest.getActStartDateTime() != null && epicRequest.getActStartDateTime().isAfter(LocalDateTime.now())) {
            throw new ValidationFailedException("Actual start date time of epic can't be greater than current date and time");
        }
        if(epicRequest.getActStartDateTime() != null && epicDb.getActStartDateTime() != null && !Objects.equals(epicRequest.getActStartDateTime(), epicDb.getActStartDateTime())) {
            throw new ValidationFailedException("Actual start date and time can not be changed");
        }
        if(!Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId()) &&
                !Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            validateEpicNotInBacklog(epicRequest);
        }
        if(!Objects.equals(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId()) &&
                !Objects.equals(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId())) {

            validateEpicForNotInBacklogAndNotInReview(epicRequest);
            if (!Objects.equals(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId())) {
                if (!Objects.equals(epicDb.getEstimate(), epicRequest.getEstimate())) {
                    throw new ValidationFailedException("Estimate can be changed only in Backlog, In Review and Reviewed state");
                }
                if (!Objects.equals(epicDb.getExpStartDateTime(), epicRequest.getExpStartDateTime())) {
                    throw new ValidationFailedException("Expected start date and time can't be changed");
                }
            }
        }
        if(Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
            validationForCompletedEpic(taskList, epicRequest);
        }

        // If epic expected date is changed then validate it with task expected date
        UpdateEpicResponse dateEpicTaskResponse = validateEpicTaskDate(epicRequest, epicDb);
        if(dateEpicTaskResponse.getTaskForBulkResponses() != null) {
            return dateEpicTaskResponse;
        }

        List<Integer> statusIdList = new ArrayList<>();
        statusIdList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
        statusIdList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
        statusIdList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
        if(!statusIdList.contains(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId()) && statusIdList.contains(epicRequest.getWorkflowEpicStatusId())) {
            throw new ValidationFailedException("Started Epic can't be marked as Backlog or Review or In Review");
        }
        if(epicRequest.getActStartDateTime() != null && statusIdList.contains(epicRequest.getWorkflowEpicStatusId())) {
            throw new ValidationFailedException("Please update the status to In progress");
        }
        if(epicRequest.getActStartDateTime() == null && !statusIdList.contains(epicRequest.getWorkflowEpicStatusId()) && !Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            throw new ValidationFailedException("Actual start date and time can't be null");
        }
        if(epicRequest.getActEndDateTime() != null && !Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId()) && !Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            throw new ValidationFailedException("Workflow status should be marked as completed");
        }
        if(epicRequest.getAssignTo() != null && (epicDb.getFkAccountIdAssigned() == null || !Objects.equals(epicDb.getFkAccountIdAssigned().getAccountId(), epicRequest.getAssignTo()))) {
            if(!validateUserRole(epicDb.getTeamIdList(), epicRequest.getAssignTo())) {
                throw new IllegalStateException("Assigned user should have essential role in at least one team");
            }
        }
        if(epicRequest.getEpicOwner() != null && (epicDb.getFkEpicOwner() == null || !Objects.equals(epicDb.getFkEpicOwner().getAccountId(), epicRequest.getEpicOwner()))) {
            if(!validateUserRole(epicDb.getTeamIdList(), epicRequest.getEpicOwner())) {
                throw new IllegalStateException("Owner should have essential role in at least one team");
            }
        }
        if (Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            if(epicRequest.getAddTeamList() != null && !epicRequest.getAddTeamList().isEmpty()) {
                throw new ValidationFailedException("Team can't be added during epic deletion");
            }
            if(epicRequest.getRemoveTeamList() != null && !epicRequest.getRemoveTeamList().isEmpty()) {
                throw new ValidationFailedException("Team can't be removed during epic deletion");
            }
        }
        List<TaskForBulkResponse> taskForBulkResponsesList = validateAndAddAndRemoveTeamFromEpic(epicRequest, epicDb, userAccount.getAccountId(), accountIds);

        // Add expected date in task
        if (!Objects.equals(epicRequest.getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
            updateTaskExpectedDate(epicRequest, taskList);
        }
        else {
            hasPermissionToModifyEpicForTeamList(epicDb.getTeamIdList(), userAccount.getAccountId());
            UpdateEpicResponse updateDeletedEpicResponse = deleteEpic(epicRequest, epicDb, taskList, accountIds, timeZone);
            if (updateDeletedEpicResponse.getFailureList() != null && !updateDeletedEpicResponse.getFailureList().isEmpty()) {
                updateDeletedEpicResponse.setMessage("Following Work Items is not able to deleted/removed");
                return updateDeletedEpicResponse;
            }
            updateEpicResponse.setSuccessList(updateDeletedEpicResponse.getSuccessList());
            updateEpicResponse.setFailureList(updateDeletedEpicResponse.getFailureList());
        }
        if(epicDb.getFkAccountIdAssigned() == null || !Objects.equals(epicDb.getFkAccountIdAssigned().getAccountId(), epicRequest.getAssignTo())) {
            epicDb.setFkAccountIdAssigned(userAccountRepository.findByAccountId(epicRequest.getAssignTo()));
        }
        if(epicDb.getFkEpicOwner() == null || !Objects.equals(epicDb.getFkEpicOwner().getAccountId(), epicRequest.getEpicOwner())) {
            epicDb.setFkEpicOwner(userAccountRepository.findByAccountId(epicRequest.getEpicOwner()));
        }

        epicDb.setFkWorkflowEpicStatus(workFlowEpicStatus);
        BeanUtils.copyProperties(epicRequest, epicDb);
        if(epicRequest.getEpicPriority() != null) {
            String priority = epicRequest.getEpicPriority().substring(0, 2);
            epicDb.setEpicPriority(priority);
        }
        epicRepository.save(epicDb);
        auditService.auditForEpic(userAccount, epicDb, true);

        EpicResponse epicResponse = new EpicResponse();
        BeanUtils.copyProperties(epicDb, epicResponse);
        epicResponse.setOrgId(epicDb.getFkOrgId().getOrgId());
        epicResponse.setProjectId(epicDb.getFkProjectId().getProjectId());
        if(epicDb.getFkAccountIdAssigned() != null) {
            epicResponse.setAccountIdAssigned(epicDb.getFkAccountIdAssigned().getAccountId());
        }
        if(epicDb.getFkEpicOwner() != null) {
            epicResponse.setAccountIdOwner(epicDb.getFkEpicOwner().getAccountId());
        }
        List<TeamResponse> teamResponseList = setTeamResponse(epicDb.getTeamIdList());
        epicResponse.setTeamList(teamResponseList);

        updateEpicResponse.setEpicResponse(epicResponse);
        if(taskForBulkResponsesList != null && !taskForBulkResponsesList.isEmpty()) {
            updateEpicResponse.setTaskForBulkResponses(taskForBulkResponsesList);
        }
        updateEpicResponse.setMessage("Epic is successfully updated");

        return updateEpicResponse;
    }

    public List<TeamResponse> getTeamForEpic(Long projectId, String accountIds) {
        Long orgId = projectRepository.findByProjectId(projectId).getOrgId();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(orgId, true, accountIdList);

        List<Long> teamIdsOfProject = teamRepository.findTeamIdByFkProjectIdProjectId(projectId);
        List<Long> teamIdList = getTeamToCreateEpic(teamIdsOfProject, userAccountId);
        List<TeamResponse> teamResponseList = setTeamResponse(teamIdList);
        return teamResponseList;
    }

    public AllEpicResponse getEpicForListing(Long teamId, String accountIds, String timezone) {
        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(teamId);
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(orgId, true, accountIdList);
        if(userAccountId == null) {
            throw new ValidationFailedException("User is not present in the organisation");
        }
        List<Epic> epicList = new ArrayList<>();
        List<Integer> authorizeRoleIdList = Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION;
        if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, teamId, userAccountId, true, authorizeRoleIdList)) {
            epicList = epicRepository.findByTeamId(teamId.toString());
        }

        AllEpicResponse allEpicResponse = new AllEpicResponse();
        List<EpicDetailsResponse> epicDetailsResponseList = new ArrayList<>();
        for(Epic epic : epicList) {
            if(!Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId()) && !Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
                EpicDetailsResponse epicDetailsResponse = new EpicDetailsResponse();
                epicDetailsResponse.setEpicId(epic.getEpicId());
                epicDetailsResponse.setEpicNumber(epic.getEpicNumber());
                epicDetailsResponse.setEpicTitle(epic.getEpicTitle());
                epicDetailsResponse.setFkworkFlowEpicStatus(epic.getFkWorkflowEpicStatus());
                epicDetailsResponse.setPriority(epic.getEpicPriority());
                epicDetailsResponse.setColor(epic.getColor());
                if (Objects.equals(Constants.EntityTypes.PROJECT, epic.getEntityTypeId())) {
                    epicDetailsResponse.setEntityName(epic.getFkProjectId().getProjectName());
                } else if (Objects.equals(Constants.EntityTypes.TEAM, epic.getEntityTypeId())) {
                    epicDetailsResponse.setEntityName(teamRepository.findTeamNameByTeamId(epic.getEntityId()));
                }
                epicDetailsResponseList.add(epicDetailsResponse);
            }
        }
        allEpicResponse.setEpicDetailsResponseList(epicDetailsResponseList);
        return allEpicResponse;
    }

    public AllEpicResponse getEpicForListingInFilter(Integer entityTypeId, Long entityId, String accountIds) {
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
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(orgId, true, accountIdList);
        if(userAccountId == null) {
            throw new ValidationFailedException("User is not present in the organisation");
        }

        List<Epic> epicList = new ArrayList<>();

        if(Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            epicList = epicRepository.findByfkOrgIdOrgId(entityId);
        }
        else if(Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            epicList = epicRepository.findByFkProjectIdProjectId(entityId);
        }
        else if(Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdNotIn(Constants.EntityTypes.TEAM, entityId, userAccountId, true, List.of(RoleEnum.TASK_BASIC_USER.getRoleId()))) {
                epicList = epicRepository.findByTeamId(entityId.toString());
            }
        }

        AllEpicResponse allEpicResponse = new AllEpicResponse();
        List<EpicDetailsResponse> epicDetailsResponseList = new ArrayList<>();
        for(Epic epic : epicList) {
            List<Long> userTeamIdList = getTeamOfUserFromEpicTeamList(epic.getTeamIdList(), userAccountId);
            if (userTeamIdList != null && !userTeamIdList.isEmpty()) {
                EpicDetailsResponse epicDetailsResponse = new EpicDetailsResponse();
                epicDetailsResponse.setEpicId(epic.getEpicId());
                epicDetailsResponse.setEpicNumber(epic.getEpicNumber());
                epicDetailsResponse.setEpicTitle(epic.getEpicTitle());
                if (Objects.equals(Constants.EntityTypes.PROJECT, epic.getEntityTypeId())) {
                    epicDetailsResponse.setEntityName(epic.getFkProjectId().getProjectName());
                }
                else if (Objects.equals(Constants.EntityTypes.TEAM, epic.getEntityTypeId())) {
                    epicDetailsResponse.setEntityName(teamRepository.findTeamNameByTeamId(epic.getEntityId()));
                }
                epicDetailsResponseList.add(epicDetailsResponse);
            }
        }
        allEpicResponse.setEpicDetailsResponseList(epicDetailsResponseList);
        return allEpicResponse;
    }

    public Set<EmailFirstLastAccountId> getMemberForEpic (Long projectId, List<Long> teamIdList, String accountIds) throws IllegalAccessException {
        if (teamIdList.isEmpty()) {
            throw new IllegalAccessException("At least one team should be present");
        }
        else {
            for (Long teamId : teamIdList) {
                Team team = teamRepository.findByTeamId(teamId);
                if (team == null) {
                    throw new ValidationFailedException("Team is not valid");
                }
                if (!Objects.equals(projectId, team.getFkProjectId().getProjectId())) {
                    throw new IllegalAccessException("Selected team is not part of project");
                }
            }
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Long> teamIdListOfProject = teamRepository.findTeamIdByFkProjectIdProjectId(projectId);
        List<Integer> authorizeRoleIdListForTeam = Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION;
        List<Integer> authorizeRoleIdListForProject = Constants.ROLE_IDS_FOR_UPDATE_EPIC_PROJECT_ACTION;

        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamIdListOfProject, accountIdList, authorizeRoleIdListForProject, true) &&
            !accessDomainRepository.existsByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamIdList, accountIdList, authorizeRoleIdListForTeam, true)) {
            throw new ValidationFailedException("Unauthorized : User doesn't have role to get member for epic");
        }

        List<Long> accountIdListOfMember = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamIdList, authorizeRoleIdListForTeam, true);
        return userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(accountIdListOfMember)
                .stream()
                .sorted(Comparator
                        .comparing(EmailFirstLastAccountId::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(EmailFirstLastAccountId::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(EmailFirstLastAccountId::getAccountId, Comparator.nullsLast(Long::compareTo))
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void removeLeadingAndTrailingSpacesForEpic (EpicRequest epicRequest) {
        if (epicRequest.getEpicTitle() != null) {
            epicRequest.setEpicTitle(epicRequest.getEpicTitle().trim());
        }
        if (epicRequest.getEpicDesc() != null) {
            epicRequest.setEpicDesc(epicRequest.getEpicDesc().trim());
        }
        if (epicRequest.getValueArea() != null) {
            epicRequest.setValueArea(epicRequest.getValueArea().trim());
        }
        if (epicRequest.getFunctionalArea() != null) {
            epicRequest.setFunctionalArea(epicRequest.getFunctionalArea().trim());
        }
        if (epicRequest.getQuarterlyPlan() != null) {
            epicRequest.setQuarterlyPlan(epicRequest.getQuarterlyPlan().trim());
        }
        if (epicRequest.getYearlyPlan() != null) {
            epicRequest.setYearlyPlan(epicRequest.getYearlyPlan().trim());
        }
        if (epicRequest.getEpicNote() != null) {
            epicRequest.setEpicNote(epicRequest.getEpicNote().trim());
        }
    }

    public UpdateEpicResponse deleteEpic(EpicRequest epicRequest, Epic epicDb, List<Task> taskList, String accountIds, String timeZone) {
        UpdateEpicResponse updateEpicResponse = new UpdateEpicResponse();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();
        setWorkItemInEpicBeforeDeletionOfEpic(epicDb, taskList, timeZone);
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epicDb.getFkOrgId().getOrgId(), true, accountIdList);
        if (epicRequest.getDeleteWorkItem()) {
            DeleteWorkItemRequest deleteWorkItemRequest = new DeleteWorkItemRequest();
            deleteWorkItemRequest.setDeleteReasonId(Constants.DeleteWorkItemReasonEnum.OTHERS.getTypeId());
            deleteWorkItemRequest.setDeleteReason("Work item deleted along with the deletion of epic");
            for (Task task : taskList) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                try {
                    Task taskDb = taskRepository.findByTaskId(task.getTaskId());
                    taskServiceImpl.deleteTaskByTaskId(task.getTaskId(), taskDb, userAccountId.toString(), timeZone, false, deleteWorkItemRequest);
                    successList.add(new TaskForBulkResponse(task.getTaskId(), task.getTaskNumber(), task.getTaskTitle(), task.getFkTeamId().getTeamId(), "Work Item is successfully deleted"));
                } catch (Exception e) {
                    logger.error("Something went wrong: Not able to delete Work Item " + task.getTaskNumber() + " Caught Exception: " + e.getMessage());
                    failureList.add(new TaskForBulkResponse(task.getTaskId(), task.getTaskNumber(), task.getTaskTitle(), task.getFkTeamId().getTeamId(), e.getMessage()));
                }
            }
        }
        else {
            for (Task task : taskList) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                try {
                    removeTaskFromEpic(epicDb, task, userAccountId, true);
                    successList.add(new TaskForBulkResponse(task.getTaskId(), task.getTaskNumber(), task.getTaskTitle(), task.getFkTeamId().getTeamId(), "Work Item is successfully deleted"));
                } catch (Exception e) {
                    logger.error("Something went wrong: Not able to remove Work Item " + task.getTaskNumber() + " Caught Exception: " + e.getMessage());
                    failureList.add(new TaskForBulkResponse(task.getTaskId(), task.getTaskNumber(), task.getTaskTitle(), task.getFkTeamId().getTeamId(), e.getMessage()));
                }
            }
        }
        updateEpicResponse.setSuccessList(successList);
        updateEpicResponse.setFailureList(failureList);
        return updateEpicResponse;
    }

    public void setWorkItemInEpicBeforeDeletionOfEpic (Epic epicDb, List<Task> taskList, String timeZone) {
        List<ProgressSystemSprintTask> baclogWorkItemList= new ArrayList<>();
        List<ProgressSystemSprintTask> notStartedWorkItemList= new ArrayList<>();
        List<ProgressSystemSprintTask> startedWorkItemList = new ArrayList<>();
        List<ProgressSystemSprintTask> completedWorkItemList = new ArrayList<>();
        for (Task task : taskList) {
            ProgressSystemSprintTask progressSystemTask = new ProgressSystemSprintTask();
            progressSystemTask.setTaskId(task.getTaskId());
            progressSystemTask.setTaskTitle(task.getTaskTitle());
            progressSystemTask.setEffort(task.getRecordedEffort());
            progressSystemTask.setEstimate(task.getTaskEstimate());
            progressSystemTask.setPercentageCompleted(task.getUserPerceivedPercentageTaskCompleted());
            if (task.getTaskExpEndDate() != null) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
                progressSystemTask.setExpEndDate(dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(), timeZone)));
            }
            if (task.getIsBug()) {
                progressSystemTask.setIsBug(true);
            }
            progressSystemTask.setTaskTypeId(task.getTaskTypeId());
            if (task.getFkAccountIdAssigned() != null) {
                User user = task.getFkAccountIdAssigned().getFkUserId();
                progressSystemTask.setAccountIdAssigned(user.getFirstName() + " " + user.getLastName());
            }
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                baclogWorkItemList.add(progressSystemTask);
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
                notStartedWorkItemList.add(progressSystemTask);
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                startedWorkItemList.add(progressSystemTask);
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                completedWorkItemList.add(progressSystemTask);
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                if (task.getTaskActStDate() == null) {
                    notStartedWorkItemList.add(progressSystemTask);
                }
                else {
                    startedWorkItemList.add(progressSystemTask);
                }
            }
            else if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                if (Objects.equals(task.getStatusAtTimeOfDeletion(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                    baclogWorkItemList.add(progressSystemTask);
                }
                else if (Objects.equals(task.getStatusAtTimeOfDeletion(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
                    notStartedWorkItemList.add(progressSystemTask);
                }
                else if (Objects.equals(task.getStatusAtTimeOfDeletion(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                    startedWorkItemList.add(progressSystemTask);
                }
            }
        }
        epicDb.setBacklogWorkItemList(baclogWorkItemList);
        epicDb.setNotStartedWorkItemList(notStartedWorkItemList);
        epicDb.setStartedWorkItemList(startedWorkItemList);
        epicDb.setCompletedWorkItemList(completedWorkItemList);

    }
}
