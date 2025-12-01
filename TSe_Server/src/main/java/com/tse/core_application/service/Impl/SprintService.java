package com.tse.core_application.service.Impl;

import com.google.firebase.database.utilities.Pair;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.constants.SprintActionFieldsEnum;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.SprintResponseForFilter;
import com.tse.core_application.custom.model.SprintWithTeamCode;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.*;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.helper.ValidationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SprintService {

    private static final Logger logger = LogManager.getLogger(StatsService.class.getName());

    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskHistoryService taskHistoryService;
    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;
    @Autowired
    private TaskServiceImpl taskService;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private MemberDetailsRepository memberDetailsRepository;
    @Autowired
    private UserCapacityMetricsRepository userCapacityMetricsRepository;
    @Autowired
    private SprintCapacityMetricsRepository sprintCapacityMetricsRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private LeaveService leaveService;
    @Autowired
    private CapacityService capacityService;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private FirebaseTokenService firebaseTokenService;
    @Autowired
    private FCMService fcmService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SprintHistoryService sprintHistoryService;

    @Autowired
    private CompletedSprintStatsRepository completedSprintStatsRepository;

    @Autowired
    private DependencyRepository dependencyRepository;

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private DependencyService dependencyService;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    
    @Autowired
    private MeetingRepository meetingRepository;

    private static final ThreadLocal<Boolean> rollbackFlag = ThreadLocal.withInitial(() -> Boolean.FALSE);


    public Sprint createSprint(SprintRequest sprintRequest, String accountIds, String timeZone) throws IllegalAccessException, InvalidKeyException {
        //verifying if user have access to create sprint
        removeLeadingAndTrailingSpacesForSprint(sprintRequest);
        if (!hasModifySprintPermission(accountIds, sprintRequest.getEntityId(), sprintRequest.getEntityTypeId())) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to create a sprint");
        }

        //verifying duration between sprint start date and end date
        if (sprintRequest.getSprintExpEndDate().isBefore(sprintRequest.getSprintExpStartDate())) {
            throw new IllegalStateException("The sprint's expected end date must not be earlier than the expected start date");
        }

        if (sprintRequest.getCapacityAdjustmentDeadline() != null && sprintRequest.getSprintExpEndDate().isBefore(sprintRequest.getCapacityAdjustmentDeadline())) {
            throw new IllegalStateException("The sprint's capacity adjustment deadline must be earlier than the expected end date");
        }

        if (!sprintRepository.findSprintsBetweenDates(sprintRequest.getEntityTypeId(), sprintRequest.getEntityId(), sprintRequest.getSprintExpStartDate(), sprintRequest.getSprintExpEndDate()).isEmpty()) {
            throw new ValidationFailedException("Validation during Sprint Creation: Overlapping sprints for the selected team and date range are not allowed");
        }

        if (sprintRequest.getSprintExpEndDate().isBefore(LocalDateTime.now()) || sprintRequest.getSprintExpStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Invalid Dates : User not allowed to create sprint in past");
        }
        Sprint sprint = new Sprint();
        sprint.setSprintTitle(sprintRequest.getSprintTitle());
        sprint.setSprintObjective(sprintRequest.getSprintObjective());
        sprint.setSprintExpStartDate(sprintRequest.getSprintExpStartDate());
        sprint.setSprintExpEndDate(sprintRequest.getSprintExpEndDate());
        sprint.setSprintStatus(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId());
        sprint.setEntityTypeId(sprintRequest.getEntityTypeId());
        sprint.setEntityId(sprintRequest.getEntityId());
        sprint.setCapacityAdjustmentDeadline(sprintRequest.getCapacityAdjustmentDeadline());

        List<Long> teamAccountIds = accessDomainRepository.findDistinctAccountIdsByEntityTypeAndEntityTypeIdAndIsActive(sprintRequest.getEntityId(), sprintRequest.getEntityTypeId());
        Set<EmailFirstLastAccountId> sprintMembers = new HashSet<>(userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(teamAccountIds));
        sprint.setSprintMembers(sprintMembers);
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount creator = userAccountRepository.findByAccountIdAndIsActive(accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(sprint.getEntityTypeId(), sprint.getEntityId(), headerAccountIds, true), true);
        sprint.setFkAccountIdCreator(creator);
        sprint.setCanModifyEstimates(sprintRequest.getCanModifyEstimates());
        sprint.setCanModifyIndicatorStayActiveInStartedSprint(sprintRequest.getCanModifyIndicatorStayActiveInStartedSprint());

        Sprint savedSprint = sprintRepository.save(sprint);

        List<Sprint> prevSprintList = sprintRepository.findAllPreviousInCompleteSprints(savedSprint.getEntityTypeId(), savedSprint.getEntityId(), savedSprint.getSprintExpStartDate(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId());
        if (!prevSprintList.isEmpty()) {
            Sprint prevSprint = prevSprintList.get(0);
            if (!Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                if (prevSprint.getNextSprintId() != null) {
                    Sprint otherSprint = sprintRepository.findBySprintId(prevSprint.getNextSprintId());
                    otherSprint.setPreviousSprintId(savedSprint.getSprintId());
                    savedSprint.setNextSprintId(otherSprint.getSprintId());
                }
                prevSprint.setNextSprintId(savedSprint.getSprintId());
                savedSprint.setPreviousSprintId(prevSprint.getSprintId());
            }
        }

        Sprint responseSprint = new Sprint();
        BeanUtils.copyProperties(savedSprint, responseSprint);
        convertAllSprintDateToUserTimeZone(responseSprint, timeZone);

        // update UserCapacityMetrics & SprintCapacityMetrics table for capacity planning
        capacityService.calculateAndStoreSprintCapacityOnSprintCreation(responseSprint,sprintRequest.getFetchLoadFactorOfSprint());
        auditService.auditForCreateSprint(accountIds, sprint);

        return responseSprint;
    }

    /**
     * Returns list of all active, delayed, not started and completed
     */
    public AllSprintResponseForGetAllSprint getAllSprint(String accountIds, Integer entityTypeId, Long entityId, String timeZone) throws IllegalAccessException {
        AllSprintResponseForGetAllSprint allSprintResponseForGetAllSprint = new AllSprintResponseForGetAllSprint();
        List<Sprint> dbSprintList = sprintRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId);
        dbSprintList.addAll(sprintRepository.findDeletedSprintByEntityTypeIdAndEntityId(entityTypeId, entityId));
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);

        if (!dbSprintList.isEmpty()) {

            //adding sprints to response
            for (Sprint sprint : dbSprintList) {
                if (!hasSprintViewPermission(accountIdList, sprint, entityId, entityTypeId)) {
                    continue;
                }
                SprintResponseForGetAllSprints sprintResponseForGetAllSprints = new SprintResponseForGetAllSprints();
                convertAllSprintDateToUserTimeZone(sprint, timeZone);
                SprintWithoutSprintMembers sprintWithoutSprintMembers = new SprintWithoutSprintMembers();
                BeanUtils.copyProperties(sprint,sprintWithoutSprintMembers);
                sprintResponseForGetAllSprints.setSprint(sprintWithoutSprintMembers);

                CompletedSprintStats completedSprintStats = null;
                if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                    completedSprintStats = completedSprintStatsRepository.findBySprintId(sprint.getSprintId());
                }
                if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) && completedSprintStats != null) {
                    BeanUtils.copyProperties(completedSprintStats, sprintResponseForGetAllSprints);
                    //Removing parent task from the response count, which gives inconsistent results
                    if(sprintResponseForGetAllSprints.getCompletedTasksList()!=null) {
                        sprintResponseForGetAllSprints.getCompletedTasksList().removeIf(task -> task.getTaskTypeId() != null && task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK));
                        sprintResponseForGetAllSprints.setCompletedTasks(sprintResponseForGetAllSprints.getCompletedTasksList().size());
                    }
                    else
                        sprintResponseForGetAllSprints.setCompletedTasks(0);
                    if(sprintResponseForGetAllSprints.getLateCompletedTasksList()!=null) {
                        sprintResponseForGetAllSprints.getLateCompletedTasksList().removeIf(task -> task.getTaskTypeId() != null && task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK));
                        sprintResponseForGetAllSprints.setLateCompletedTasks(sprintResponseForGetAllSprints.getLateCompletedTasksList().size());
                    }
                    else
                        sprintResponseForGetAllSprints.setLateCompletedTasks(0);
                }
                else if (!Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.DELETED.getSprintStatusId())) {
                    Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(sprint.getEntityTypeId(), sprint.getEntityId(), accountIdList, true);
                    List<Task> sprintTaskList = getSprintTask(sprint, accountId);
                    //filtering sprint tasks by their stat
                    for (Task sprintTask : sprintTaskList) {
                        if (!Objects.equals(sprintTask.getSprintId(), sprint.getSprintId())) {
                            continue;
                        }
                        if (!Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                            if (Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                                sprintResponseForGetAllSprints.incrementDeletedTasks();
                                continue;
                            }
                            if (Objects.equals(StatType.DELAYED, sprintTask.getTaskProgressSystem())) {
                                sprintResponseForGetAllSprints.incrementDelayedTasks();
                            } else if (Objects.equals(StatType.ONTRACK, sprintTask.getTaskProgressSystem())) {
                                sprintResponseForGetAllSprints.incrementOnTrackTasks();
                            } else if (Objects.equals(StatType.COMPLETED, sprintTask.getTaskProgressSystem())) {
                                sprintResponseForGetAllSprints.incrementCompletedTasks();
                            } else if (Objects.equals(StatType.NOTSTARTED, sprintTask.getTaskProgressSystem())) {
                                sprintResponseForGetAllSprints.incrementNotStartedTasks();
                            } else if (Objects.equals(StatType.WATCHLIST, sprintTask.getTaskProgressSystem())) {
                                sprintResponseForGetAllSprints.incrementWatchListTasks();
                            } else if (Objects.equals(StatType.LATE_COMPLETION, sprintTask.getTaskProgressSystem())) {
                                sprintResponseForGetAllSprints.incrementLateCompletedTask();
                            }
                            sprintResponseForGetAllSprints.incrementTotalTasks();
                        }
                        else {
                            List<Task> childTaskList = taskRepository.findByParentTaskId(sprintTask.getTaskId());
                            for (Task childTask : childTaskList) {
                                if (!Objects.equals(childTask.getSprintId(), sprint.getSprintId())) {
                                    continue;
                                }
                                if (Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                                    sprintResponseForGetAllSprints.incrementDeletedTasks();
                                    continue;
                                }
                                if (Objects.equals(StatType.DELAYED, childTask.getTaskProgressSystem())) {
                                    sprintResponseForGetAllSprints.incrementDelayedTasks();
                                } else if (Objects.equals(StatType.ONTRACK, childTask.getTaskProgressSystem())) {
                                    sprintResponseForGetAllSprints.incrementOnTrackTasks();
                                } else if (Objects.equals(StatType.COMPLETED, childTask.getTaskProgressSystem())) {
                                    sprintResponseForGetAllSprints.incrementCompletedTasks();
                                } else if (Objects.equals(StatType.NOTSTARTED, childTask.getTaskProgressSystem())) {
                                    sprintResponseForGetAllSprints.incrementNotStartedTasks();
                                } else if (Objects.equals(StatType.WATCHLIST, childTask.getTaskProgressSystem())) {
                                    sprintResponseForGetAllSprints.incrementWatchListTasks();
                                } else if (Objects.equals(StatType.LATE_COMPLETION, childTask.getTaskProgressSystem())) {
                                    sprintResponseForGetAllSprints.incrementLateCompletedTask();
                                }
                                sprintResponseForGetAllSprints.incrementTotalTasks();
                            }
                        }
                    }
                }

                //filtering sprints on tha basis of their status
                if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                    allSprintResponseForGetAllSprint.getCompletedSprintList().add(sprintResponseForGetAllSprints);
                } else if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                    allSprintResponseForGetAllSprint.getActiveSprintList().add(sprintResponseForGetAllSprints);
                } else if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId())) {
                    allSprintResponseForGetAllSprint.getNotStartedSprintList().add(sprintResponseForGetAllSprints);
                } else if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.DELETED.getSprintStatusId())) {
                    allSprintResponseForGetAllSprint.getDeletedSprintList().add(sprintResponseForGetAllSprints);
                }
            }
        }

        if (allSprintResponseForGetAllSprint.getNotStartedSprintList() != null && !allSprintResponseForGetAllSprint.getNotStartedSprintList().isEmpty()) {
            allSprintResponseForGetAllSprint.getNotStartedSprintList().sort(
                    Comparator.comparing(sprintResponse -> {
                                SprintWithoutSprintMembers sprint = sprintResponse.getSprint();
                                return sprint != null ? sprint.getSprintExpStartDate() : null;
                            },
                            Comparator.nullsLast(LocalDateTime::compareTo)
                    )
            );
        }

        if (allSprintResponseForGetAllSprint.getCompletedSprintList() != null && !allSprintResponseForGetAllSprint.getCompletedSprintList().isEmpty()) {
            allSprintResponseForGetAllSprint.getCompletedSprintList().sort(
                    Comparator.comparing(sprintResponse -> {
                                SprintWithoutSprintMembers sprint = sprintResponse.getSprint();
                                return sprint != null ? sprint.getSprintActEndDate() : null;
                            },
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
            );
        }
        return allSprintResponseForGetAllSprint;
    }

    /**
     * This method returns a response with sprint and all the tasks of that sprint
     */
    public SprintAllTasksResponse getSprint(Long sprintId, String accountIds, String timeZone) throws IllegalAccessException {

        Optional<Sprint> sprintOptional = sprintRepository.findById(sprintId);
        List<Long> accountIdsList = CommonUtils.convertToLongList(accountIds);

        //sprint Validations
        if (sprintOptional.isEmpty()) {
            throw new NoDataFoundException();
        }
        if (!hasSprintViewPermission(accountIdsList, sprintOptional.get(), sprintOptional.get().getEntityId(), sprintOptional.get().getEntityTypeId())) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to view sprints");
        }
        Sprint sprint = sprintOptional.get();
        SprintAllTasksResponse sprintAllTasks = new SprintAllTasksResponse();
        Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(sprint.getEntityTypeId(), sprint.getEntityId(), accountIdsList, true);
        List<Task> sprintTaskList = getSprintTask(sprint, accountId);
        SprintTaskByFilterResponse sprintTaskByFilterResponse = getSprintTaskResponse(sprintTaskList, sprintId, null);

        sprintAllTasks.setSprintStartedTaskList(sprintTaskByFilterResponse.getSprintStartedTaskList());
        sprintAllTasks.setSprintCompletedTaskList(sprintTaskByFilterResponse.getSprintCompletedTaskList());
        sprintAllTasks.setSprintNotStartedTaskList(sprintTaskByFilterResponse.getSprintNotStartedTaskList());
        sprintAllTasks.setTotalCompletedTask(sprintTaskByFilterResponse.getTotalCompletedTask());
        sprintAllTasks.setTotalStartedTask(sprintTaskByFilterResponse.getTotalStartedTask());
        sprintAllTasks.setTotalNotStartedTask(sprintTaskByFilterResponse.getTotalNotStartedTask());
        sprintAllTasks.setToDoWorkItemList(sprint.getToDoWorkItemList());
        sprintAllTasks.setInProgressWorkItemList(sprint.getInProgressWorkItemList());
        sprintAllTasks.setCompletedWorkItemList(sprint.getCompletedWorkItemList());
        if (sprint.getPreviousSprintId() != null) {
            sprintAllTasks.setPreviousSprint(sprintRepository.getSprintTitleAndSprintIdBySprintId(sprint.getPreviousSprintId()));
        }
        if (sprint.getNextSprintId() != null) {
            sprintAllTasks.setNextSprint(sprintRepository.getSprintTitleAndSprintIdBySprintId(sprint.getNextSprintId()));
        }
        convertAllSprintDateToUserTimeZone(sprint, timeZone);
        sprintAllTasks.setSprint(sprint);

        return sprintAllTasks;
    }


    /**
     * This method adds all the tasks provided in the sprint
     */
    public TaskListForBulkResponse addAllTaskToSprint (Long sprintId, List<Long> taskIds, String accountIds) throws IllegalAccessException {
        Optional<Sprint> sprintDb = sprintRepository.findById(sprintId);
        TaskListForBulkResponse taskListForBulkResponse = new TaskListForBulkResponse();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();
        //sprint Validations
        if (sprintDb.isEmpty()) {
            throw new NoDataFoundException();
        }
        Sprint sprint = sprintDb.get();
        if (!hasModifySprintPermission(accountIds, sprint.getEntityId(), sprint.getEntityTypeId())) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to add task in sprint");
        }
        if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
            throw new IllegalStateException("Cannot add Work Item to a completed sprint");
        }

        for (Long taskId : taskIds) {
            Task foundTask = taskRepository.findByTaskId(taskId);
            try {
                if (foundTask == null) {
                    throw new EntityNotFoundException("Work Item not found");
                }
                Task taskRequest = new Task();
                BeanUtils.copyProperties(foundTask, taskRequest);
                addTaskToSprint(sprint, taskRequest, accountIds, taskIds, true);
                successList.add(new TaskForBulkResponse(foundTask.getTaskId(), foundTask.getTaskNumber(), foundTask.getTaskTitle(), foundTask.getFkTeamId().getTeamId(), "Task successfully added to sprint"));
            } catch (Exception e) {
                logger.error("Something went wrong: Not able to add Work Item " + (foundTask != null ? foundTask.getTaskNumber() : null) + " Caught Exception: " + e.getMessage());
                failureList.add(new TaskForBulkResponse(foundTask.getTaskId(), foundTask.getTaskNumber(), foundTask.getTaskTitle(), foundTask.getFkTeamId().getTeamId(), e.getMessage()));

            }
        }

        sprintRepository.save(sprint);
        auditService.auditForSprintTaskMovement(accountIds, sprint.getEntityId());
        taskListForBulkResponse.setSuccessList(successList);
        taskListForBulkResponse.setFailureList(failureList);
        return taskListForBulkResponse;
    }


    /**
     * This method adds task to sprint
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addTaskToSprint(Sprint sprint, Task task, String accountIds, List<Long> taskIds, Boolean isChildTaskIndividual) throws IllegalAccessException {

        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && isChildTaskIndividual) {
            throw new IllegalAccessException("User can't directly add a child task to the sprint. Please include the parent task, and the child task will be added automatically.");
        }
        List<Task> taskList = new ArrayList<>();
        Map<Task, List<String>> taskUpdateMap = new HashMap<>();
        List<Audit> auditCreatedList = new ArrayList<>();
        Map<Task, Task> updatedTaskToTaskCopy = new HashMap<>();
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            List<Task> childTaskList = taskRepository.findByParentTaskId(task.getTaskId());
            if (!childTaskList.isEmpty()) {
                for (Task childTask : childTaskList) {
                    if (!childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)
                            && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)
                            && !taskIds.contains(childTask.getTaskId())) {
                        Task childTaskRequest = new Task();
                        BeanUtils.copyProperties(childTask, childTaskRequest);
                        taskList.add(childTaskRequest);
                    }
                }
            }
        }
        taskList.add(task);
        StatType worstStat = null;
        for (Task foundTask : taskList) {
            //adding sprint id in task
            if (foundTask.getSprintId() != null) {
                if (Objects.equals(foundTask.getSprintId(), sprint.getSprintId()))
                    throw new IllegalAccessException("Work Item is already part of this sprint");
                else
                    throw new IllegalStateException("The Work Item cannot be added because it belongs to another sprint. Please remove the Work Item from the current list and try again.");
            }

            if (foundTask.getFkAccountIdAssigned() != null) {
                Set<EmailFirstLastAccountId> sprintMemberList = sprint.getSprintMembers();
                if (sprintMemberList == null) {
                    sprintMemberList = new HashSet<>();
                }
                List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                        .map(EmailFirstLastAccountId::getAccountId)
                        .collect(Collectors.toList());
                if (!sprintMemberAccountIdList.contains(foundTask.getFkAccountIdAssigned().getAccountId())) {
                    throw new IllegalAccessException("Assigned To user of Work Item is not part of selected sprint");
                }
            }
            else {
                if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                    throw new ValidationFailedException("Assign To in work item is mandatory in started sprint");
                }
            }

            if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty()
                    && ((task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) && task.getTaskActStDate()==null)
                    || task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())))
                throw new IllegalStateException("Work items with dependencies should have dates within sprint dates");
            Task taskCopy = new Task();
            BeanUtils.copyProperties(foundTask, taskCopy);
            Integer noOfAudit = 0;
            List<String> updatedFields = new ArrayList<>();
            if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                if (foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                    throw new IllegalAccessException("The Work Item cannot be added to an active sprint because it is currently on hold or blocked. Please resolve the issue before adding it to the sprint.");
                }
                if (foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                    String message = "Following are missing/incorrect: ";
                    boolean isEstimateNull = false, isPriorityNull = false;
                    if (foundTask.getTaskEstimate() == null) {
                        isEstimateNull = true;
                        message += "Estimate";
                    }
                    if (foundTask.getTaskPriority() == null) {
                        isPriorityNull = true;
                        message += (isEstimateNull ? ", Priority" : "Priority");
                    }
                    if (foundTask.getFkAccountIdAssigned() == null) {
                        message += (isPriorityNull || isEstimateNull ? ", AssignedTo" : "AssignedTo");
                    }
                    if (!Objects.equals(message, "Following are missing/incorrect: ")) {
                        throw new IllegalStateException(message);
                    }

                    if (foundTask.getTaskExpStartDate() == null || foundTask.getTaskExpStartTime() == null || foundTask.getTaskExpEndDate() == null || foundTask.getTaskExpEndTime() == null) {
                        if(foundTask.getTaskTypeId()==Constants.TaskTypes.CHILD_TASK)
                            addSprintDatesToTask(foundTask, sprint, false, task);
                        else
                            addSprintDatesToTask(foundTask, sprint, false, null);
                    }
                    if (foundTask.getFkAccountIdAssigned() == null) {
                        throw new IllegalStateException("Work Item number " + foundTask.getTaskNumber() + "  is currently unassigned. Please assign the Work Item to a team member before adding it to the sprint.");
                    }
                    WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, foundTask.getTaskWorkflowId());
                    foundTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
                    foundTask.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
                    updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                    if (!Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) taskService.computeAndUpdateStatForTask(foundTask, true);
                    if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && taskService.isWorseStat(foundTask.getTaskProgressSystem(), worstStat)) {
                        worstStat = foundTask.getTaskProgressSystem();
                    }
                    if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                        foundTask.setTaskProgressSystem(worstStat);
                        LocalDateTime currentDateTime = LocalDateTime.now();
                        task.setTaskProgressSystemLastUpdated(currentDateTime);
                    }

                }
                sprint.setHoursOfSprint((sprint.getHoursOfSprint() != null ? sprint.getHoursOfSprint() : 0) + (foundTask.getTaskEstimate() != null ? foundTask.getTaskEstimate() : 0));
            }
            String message;
            if(foundTask.getTaskTypeId()==Constants.TaskTypes.CHILD_TASK) {
                message = validateSprintConditionAndModifyTaskProperties(foundTask, sprint, task);
            } else {
                message = validateSprintConditionAndModifyTaskProperties(foundTask, sprint, null);
            }
            taskServiceImpl.validateExpDateTimeWithEstimate (foundTask);
            foundTask.setSprintId(sprint.getSprintId());
            noOfAudit++;
            updatedFields.add(Constants.TaskFields.SPRINT_ID);
            Audit auditCreated = auditService.createAudit(foundTask, noOfAudit, foundTask.getTaskId(), Constants.TaskFields.SPRINT_ID);
            auditCreatedList.add(auditCreated);
            updatedTaskToTaskCopy.put(foundTask, taskCopy);
            taskUpdateMap.put(foundTask, updatedFields);
        }
        auditRepository.saveAll(auditCreatedList);
        updatedTaskToTaskCopy.forEach((updatedTask, copyTask) -> {
            capacityService.updateUserAndSprintCapacityMetricsOnAddTaskToSprint(updatedTask, sprint.getSprintId());
            taskHistoryService.addTaskHistoryOnUserUpdate(copyTask);
        });
        taskRepository.saveAll(updatedTaskToTaskCopy.keySet());
        taskUpdateMap.forEach((taskKey, updatedFeilds) -> {
            taskHistoryMetadataService.addTaskHistoryMetadata(updatedFeilds, taskKey);
        });
    }

    /**
     * remove a task from the sprint
     */
    public void removeTaskFromSprint(Long sprintId, Long taskId, String accountIds, String timeZone, Boolean isChildTaskIndividual, Boolean skipValidations) throws IllegalAccessException {
        Sprint sprintDb = sprintRepository.findById(sprintId).orElseThrow(() -> new EntityNotFoundException("Sprint not found"));

        Task foundTask = taskRepository.findByTaskId(taskId);
        if (foundTask == null) throw new EntityNotFoundException("Work Item not found");

        if (!skipValidations) {
            if (!hasModifySprintPermission(accountIds, sprintDb.getEntityId(), sprintDb.getEntityTypeId())) {
                throw new IllegalAccessException("Unauthorized: User does not have permission to remove Work Item from sprint");
            }
            if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new ValidationFailedException("Cannot remove Work Item from a completed sprint");
            }
            if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                throw new ValidationFailedException("Completed work item can't be removed from sprint");
            }
            if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                throw new ValidationFailedException("Deleted work item can't be removed from sprint");
            }
        }

        if (!Objects.equals(foundTask.getSprintId(), sprintId)) {
            throw new ValidationFailedException("Work Item is not part of the given sprint");
        }

        if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && isChildTaskIndividual) {
            throw new IllegalAccessException("User not allowed to remove child task from spirnt");
        }
        if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            List<Task> childTaskList = taskRepository.findByParentTaskId(foundTask.getTaskId());
            if (!childTaskList.isEmpty()) {
                for (Task childTask : childTaskList) {
                    if (!childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)
                            && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                        removeTaskFromSprint(sprintId, childTask.getTaskId(), accountIds, timeZone, false, true);
                    }
                }
            }
        }
        Task updatedTask = new Task();
        BeanUtils.copyProperties(foundTask, updatedTask);
        updatedTask.setSprintId(null);
        if (foundTask.getTaskEstimate() != null) {
            sprintDb.setHoursOfSprint(sprintDb.getHoursOfSprint() != null ? (sprintDb.getHoursOfSprint() - foundTask.getTaskEstimate()) : 0);
            sprintRepository.save(sprintDb);
        }

        // capacity is being adjusted in the updated task method itself
        taskServiceImpl.updateFieldsInTaskTable(updatedTask, updatedTask.getTaskId(), timeZone,accountIds);
        // capacityService.removeTaskFromSprintCapacityAdjustment(foundTask);
        auditService.auditForSprintTaskMovement(accountIds, sprintDb.getEntityId());
    }

    /**
     * This method updates sprint
     */
    public UpdateSprintResponse updateSprint(SprintRequest sprintRequest, Long sprintId, String accountIds, String timeZone) throws IllegalAccessException, InvalidKeyException, ValidationFailedException {
        Optional<Sprint> sprintDbOptional = sprintRepository.findById(sprintId);

        //sprint Validations
        if (!sprintDbOptional.isPresent()) {
            throw new NoDataFoundException();
        }
        removeLeadingAndTrailingSpacesForSprint(sprintRequest);
        Sprint sprintDb = sprintDbOptional.get();
        Sprint sprintDbCopy = new Sprint();
        BeanUtils.copyProperties(sprintDb, sprintDbCopy);
        if (!hasModifySprintPermission(accountIds, sprintDbOptional.get().getEntityId(), sprintDbOptional.get().getEntityTypeId())) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to update sprint");
        }
        if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.DELETED.getSprintStatusId())) {
            throw new ValidationFailedException("Deleted sprint can't be updated");
        }
        Sprint sprintToSave = new Sprint();
        BeanUtils.copyProperties(sprintDbOptional.get(), sprintToSave);
        List<TaskNumberTaskTitleSprintName> taskToUpdate = validateSprintUpdates(sprintRequest, sprintToSave, accountIds);
        if (!Objects.equals(sprintToSave.getCapacityAdjustmentDeadline(), sprintRequest.getCapacityAdjustmentDeadline())) {
            sprintToSave.setCapacityAdjustmentDeadline(sprintRequest.getCapacityAdjustmentDeadline());
        }
        CommonUtils.copyNonNullProperties(sprintRequest, sprintToSave);

        if(!Objects.equals(sprintRequest.getSprintExpStartDate(), sprintDbCopy.getSprintExpStartDate()) || !Objects.equals(sprintRequest.getSprintExpEndDate(), sprintDbCopy.getSprintExpEndDate())) {

            Sprint oldPrevSprint = sprintRepository.findBySprintId(sprintToSave.getPreviousSprintId());
            Sprint oldNextSprint = sprintRepository.findBySprintId(sprintToSave.getNextSprintId());

            if(oldPrevSprint!=null) {
                oldPrevSprint.setNextSprintId(sprintDb.getNextSprintId());
                sprintRepository.save(oldPrevSprint);
            }
            if(oldNextSprint!=null) {
                oldNextSprint.setPreviousSprintId(sprintDb.getPreviousSprintId());
                sprintRepository.save(oldNextSprint);
            }

            sprintToSave.setPreviousSprintId(null);
            sprintToSave.setNextSprintId(null);

            List<Sprint> sprintList = sprintRepository.findAllPreviousInCompleteSprints(sprintToSave.getEntityTypeId(), sprintToSave.getEntityId(), sprintToSave.getSprintExpStartDate(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId());

            sprintList.removeIf(sprint -> sprint.getSprintId().equals(sprintToSave.getSprintId()));

            if (!sprintList.isEmpty()) {
                Sprint prevSprint = sprintList.get(0);
                if (!Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                    if (prevSprint.getNextSprintId() != null) {
                        Sprint otherSprint = sprintRepository.findBySprintId(prevSprint.getNextSprintId());
                        otherSprint.setPreviousSprintId(sprintToSave.getSprintId());
                        sprintToSave.setNextSprintId(otherSprint.getSprintId());
                    }
                    prevSprint.setNextSprintId(sprintToSave.getSprintId());
                    sprintToSave.setPreviousSprintId(prevSprint.getSprintId());
                }
            }
            else {
                sprintList = sprintRepository.findAllFutureNotStartedSprints(sprintToSave.getEntityTypeId(), sprintToSave.getEntityId(), sprintToSave.getSprintExpStartDate(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId());

                sprintList.removeIf(sprint -> sprint.getSprintId().equals(sprintToSave.getSprintId()));

                if (!sprintList.isEmpty()) {
                    Sprint nextSprint = sprintList.get(0);
                    if (!Objects.equals(nextSprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                        if (nextSprint.getPreviousSprintId() != null) {
                            Sprint otherSprint = sprintRepository.findBySprintId(nextSprint.getPreviousSprintId());
                            sprintToSave.setPreviousSprintId(otherSprint.getSprintId());
                        }
                        nextSprint.setPreviousSprintId(sprintToSave.getSprintId());
                        sprintToSave.setNextSprintId(nextSprint.getSprintId());
                    }
                }
            }
        }
        Sprint savedSprint = sprintRepository.save(sprintToSave);
        sprintHistoryService.addSprintHistory(sprintDbCopy, sprintToSave, accountIds);

        Sprint responseSprint = new Sprint();
        BeanUtils.copyProperties(savedSprint, responseSprint);
        convertAllSprintDateToUserTimeZone(responseSprint, timeZone);
        if (!Objects.equals(sprintRequest.getSprintExpStartDate(), sprintDbCopy.getSprintExpStartDate()) || !Objects.equals(sprintRequest.getSprintExpEndDate(), sprintDbCopy.getSprintExpEndDate()) || sprintRequest.getFetchLoadFactorOfSprint() != null) {
            capacityService.recalculateCapacitiesForSprint(responseSprint, null,sprintRequest.getFetchLoadFactorOfSprint());
        }
        auditService.auditForUpdateSprint(accountIds, sprintDb);
        if(sprintRequest.getAutoUpdateTaskDates())
            return new UpdateSprintResponse(responseSprint,null,taskToUpdate);
        return new UpdateSprintResponse(responseSprint, taskToUpdate,null);
    }

    /**
     * This method validate all the sprint updated fields
     */
    public List<TaskNumberTaskTitleSprintName> validateSprintUpdates(SprintRequest sprintRequest, Sprint sprintDb, String accountIds) throws InvalidKeyException, ValidationFailedException {
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<TaskNumberTaskTitleSprintName> taskToUpdate = new ArrayList<>();
        //Sprint update is not allowed if sprint is completed
        if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) && !accessDomainRepository.findAllRoleIdsByAccountIdsEntityTypeIdAndEntityIdsAndIsActive(accountIdList, sprintRequest.getEntityTypeId(), Collections.singletonList(sprintRequest.getEntityId()), true).contains(RoleEnum.PROJECT_MANAGER_SPRINT)) {
            throw new ForbiddenException("Cannot update sprint. Sprint is already completed");
        }

        //Automatically updating sprint status on receiving sprint actual dates
        if (sprintRequest.getSprintActStartDate() != null && sprintDb.getSprintActStartDate() != null && !Objects.equals(sprintRequest.getSprintActStartDate(), sprintDb.getSprintActStartDate().withSecond(0).withNano(0))) {
            throw new ValidationFailedException("Cannot update actual start date.");
        }
        if (sprintRequest.getSprintActEndDate() != null && sprintDb.getSprintActEndDate() != null && !Objects.equals(sprintRequest.getSprintActEndDate(), sprintDb.getSprintActEndDate().withSecond(0).withNano(0))) {
            throw new ValidationFailedException("Cannot update actual end date.");
        }

        List<Task> sprintTasks = taskRepository.findBySprintId(sprintDb.getSprintId()).stream()
                .filter(task -> !(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) ||
                        task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)))
                .map(originalTask -> {
                    Task copy = new Task();
                    BeanUtils.copyProperties(originalTask, copy);
                    return copy;
                })
                .collect(Collectors.toList());

        //Verifying if updated fields don't contain entity updates
        if (!Objects.equals(sprintRequest.getEntityId(), sprintDb.getEntityId()) || !Objects.equals(sprintRequest.getEntityTypeId(), sprintDb.getEntityTypeId())) {
            throw new ForbiddenException("User is not allowed to update sprint entity");
        }
        //Verifying if update fields don't contain sprintId
        if (!Objects.equals(sprintRequest.getSprintId(), sprintDb.getSprintId())) {
            throw new ForbiddenException("User is not allowed to update sprintId");
        }

        if(Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && !Objects.equals(sprintRequest.getSprintExpStartDate(), sprintDb.getSprintExpStartDate())){
            throw new ValidationFailedException("In a started sprint, Expected Start Date or Time can't be changed.");
        }

        if (Objects.equals(sprintRequest.getSprintStatus(), sprintDb.getSprintStatus()) && sprintRequest.getSprintExpStartDate().isBefore(sprintDb.getSprintExpStartDate()) && sprintRequest.getSprintActStartDate() == null && sprintRequest.getSprintExpStartDate().isBefore(LocalDateTime.now())) {
            throw new ValidationFailedException("The Sprint's expected start date and time cannot be scheduled before its expected start date and time at the time of creation.");
        }
        //In case of the Dependent tasks either Predecessor/Successor expectedDates should be in range of new Sprint Exp dates.
        if ((Objects.equals(sprintRequest.getSprintStatus(), sprintDb.getSprintStatus()) &&
                !Objects.equals(sprintRequest.getSprintExpStartDate(), sprintDb.getSprintExpStartDate())) || !Objects.equals(sprintRequest.getSprintExpEndDate(), sprintDb.getSprintExpEndDate())) {
            List<Long> toBeCheckedTasksPredecessor = new ArrayList<>();
            List<Long> toBeCheckedTasksSuccessor = new ArrayList<>();
            for (Task sprintTask : sprintTasks) {
                if (sprintTask.getDependencyIds() != null && !sprintTask.getDependencyIds().isEmpty()) {
                    if (sprintTask.getTaskExpStartDate().isEqual(sprintDb.getSprintExpStartDate())) {
                        toBeCheckedTasksPredecessor.add(sprintTask.getTaskId());
                    }
                    if (sprintTask.getTaskExpEndDate().isEqual(sprintDb.getSprintExpEndDate())) {
                        toBeCheckedTasksSuccessor.add(sprintTask.getTaskId());
                    }
                    // if sprint dates compressing towards
                    if (sprintRequest.getSprintExpStartDate().isAfter(sprintDb.getSprintExpStartDate())
                            && !sprintTask.getTaskExpEndDate().isAfter(sprintRequest.getSprintExpStartDate())
                        ){
                        throw new ValidationFailedException("Cannot modify Expected Dates. There are Dependent Work Items during this period can interfere the existing Dependency relation.");
                    }
                    if(sprintRequest.getSprintExpEndDate().isBefore(sprintDb.getSprintExpEndDate())
                            && !sprintTask.getTaskExpStartDate().isBefore(sprintRequest.getSprintExpEndDate())){
                        throw new ValidationFailedException("Cannot modify Expected Dates. There are Dependent Work Items during this period can interfere the existing Dependency relation.");
                    }
                }
            }
            // if sprint dates are expanding then checking dependency's predecessor and successor dates are not overlapping to the new Sprint dates.
            LocalDateTime successorTaskTime = null;
            LocalDateTime predecessorTaskTime = null;
            if (!toBeCheckedTasksPredecessor.isEmpty()) {
                    predecessorTaskTime = taskRepository.findDependencyExpEndTimeFromTask(sprintDb.getSprintExpStartDate() ,toBeCheckedTasksPredecessor);
            }
            if (!toBeCheckedTasksSuccessor.isEmpty()) {
                    successorTaskTime = taskRepository.findDependencyExpStartTimeFromTask(sprintDb.getSprintExpEndDate() ,toBeCheckedTasksSuccessor);
            }
            if ((successorTaskTime != null && (successorTaskTime.isEqual(sprintRequest.getSprintExpEndDate()) || successorTaskTime.isBefore(sprintRequest.getSprintExpEndDate()))) ||
                    (predecessorTaskTime != null && (predecessorTaskTime.isEqual(sprintRequest.getSprintExpStartDate()) || predecessorTaskTime.isAfter(sprintRequest.getSprintExpStartDate())))){
                throw new ValidationFailedException("Cannot modify Expected Dates. There are Dependent Work Items during this period can interfere the existing Dependency relation.");
            }
        }

        List<Sprint> overlappingSprints = sprintRepository.findOtherSprintsBetweenDates(sprintRequest.getSprintId(), sprintRequest.getEntityTypeId(), sprintRequest.getEntityId(), sprintRequest.getSprintExpStartDate(), sprintRequest.getSprintExpEndDate());
//        Checking for exactly one overlapping, completed sprint where S1 ends before ExpEndDate
        if (overlappingSprints.size()==1 && Objects.equals(overlappingSprints.get(0).getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) && !overlappingSprints.get(0).getSprintActEndDate().isAfter(overlappingSprints.get(0).getSprintExpEndDate())) {
            if(sprintRequest.getSprintExpStartDate().isAfter(overlappingSprints.get(0).getSprintActEndDate())) {
                Sprint updateOverlappingSprint = new Sprint();
                BeanUtils.copyProperties(overlappingSprints.get(0),updateOverlappingSprint);
                updateOverlappingSprint.setSprintExpEndDate(updateOverlappingSprint.getSprintActEndDate());
                sprintRepository.save(updateOverlappingSprint);
            }
            else {
                throw new ValidationFailedException("Validation during Edit Sprint: Overlapping sprints for the selected team and date range are not allowed");
            }
        }
        else if(!overlappingSprints.isEmpty()) {
            throw new ValidationFailedException("Validation during Edit Sprint: Overlapping sprints for the selected team and date range are not allowed");
        }

        if (sprintRequest.getCanModifyIndicatorStayActiveInStartedSprint() != null && !Objects.equals(sprintRequest.getCanModifyIndicatorStayActiveInStartedSprint(), sprintDb.getCanModifyIndicatorStayActiveInStartedSprint())
                && (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())
                || Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()))) {
            throw new ForbiddenException("User cannot mark estimate indicator active/inactive once sprint is started");
        }

        //If expected dates are changed verifying task dates with sprint dates
        if (!Objects.equals(sprintRequest.getSprintExpStartDate(), sprintDb.getSprintExpStartDate()) || !Objects.equals(sprintRequest.getSprintExpEndDate(), sprintDb.getSprintExpEndDate())) {
            if (sprintRequest.getSprintExpEndDate().isBefore(sprintRequest.getSprintExpStartDate())) {
                throw new IllegalStateException("Sprint expected end date cannot be before sprint expected start date");
            }
            for (Task sprintTask : sprintTasks) {
                if (sprintRequest.getAutoUpdateTaskDates()) {
                    Task sprintTaskCopy = new Task();
                    BeanUtils.copyProperties(sprintTask, sprintTaskCopy);
                    Epic epic = null;
                    if (sprintTask.getFkEpicId() != null) {
                        epic = epicRepository.findByEpicId(sprintTask.getFkEpicId().getEpicId());
                    }
                    boolean taskUpdates = false;
                    List<String> updatedFields = new ArrayList<>();
                    if ((sprintTask.getTaskExpStartDate().isBefore(sprintRequest.getSprintExpStartDate()) || !sprintTask.getTaskExpStartDate().isBefore(sprintRequest.getSprintExpEndDate())) && !sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                        if (epic != null) {
                            if ((epic.getExpEndDateTime() != null && sprintRequest.getSprintExpStartDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprintRequest.getSprintExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                                throw new ValidationFailedException("Work Item's Expected Start Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                            }
                        }
                        sprintTask.setTaskExpStartDate(sprintRequest.getSprintExpStartDate());
                        sprintTask.setTaskExpStartTime(sprintRequest.getSprintExpStartDate().toLocalTime());
                        updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                        updatedFields.add(Constants.TaskFields.EXP_START_TIME);
                        taskUpdates = true;
                    }
                    if (sprintTask.getTaskExpEndDate().isAfter(sprintRequest.getSprintExpEndDate()) || sprintTask.getTaskExpEndDate().isBefore(sprintRequest.getSprintExpStartDate())) {
                        if (epic != null) {
                            if ((epic.getExpEndDateTime() != null && sprintRequest.getSprintExpEndDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprintRequest.getSprintExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                                throw new ValidationFailedException("Work Item's Expected End Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                            }
                        }
                        sprintTask.setTaskExpEndDate(sprintRequest.getSprintExpEndDate());
                        sprintTask.setTaskExpEndTime(sprintRequest.getSprintExpEndDate().toLocalTime());
                        updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                        updatedFields.add(Constants.TaskFields.EXP_END_TIME);
                        taskUpdates = true;
                    }
                    if(taskUpdates && (updatedFields.contains(Constants.TaskFields.EXP_END_DATE) || updatedFields.contains(Constants.TaskFields.EXP_START_DATE))){
                        updateTasksMeetingDateUpdatingSprint(sprintTask, sprintRequest);
                    }

//                    DON'T REMOVE THIS COMMENT

//                    LocalDate taskExpEndDate=sprintTask.getTaskExpEndDate().toLocalDate();
//                    LocalDate sprintRequestExpEndDate=sprintRequest.getSprintExpEndDate().toLocalDate();
//                    LocalDate sprintDbExpEndDate=sprintDb.getSprintExpEndDate().toLocalDate();
//                    LocalTime taskExpEndTime=sprintTask.getTaskExpEndDate().toLocalTime();
//                    LocalTime sprintRequestExpEndTime=sprintRequest.getSprintExpEndDate().toLocalTime();
//                    LocalTime sprintDbExpEndTime=sprintDb.getSprintExpEndDate().toLocalTime();

//
//                    if(sprintRequestExpEndDate.isAfter(sprintDbExpEndDate)&&sprintDbExpEndDate.isEqual(taskExpEndDate)){
//                        if(taskExpEndTime.isAfter(sprintRequestExpEndTime)) {
//                            sprintTask.setTaskExpEndDate(sprintRequest.getSprintExpEndDate());
//                            sprintTask.setTaskExpEndTime(sprintRequest.getSprintExpEndDate().toLocalTime());
//                        }
//                        else {
//                            sprintTask.setTaskExpEndDate(LocalDateTime.of(sprintRequestExpEndDate,taskExpEndTime));
//                        }
//                        updatedFields.add(Constants.TaskFields.EXP_END_DATE);
//                        updatedFields.add(Constants.TaskFields.EXP_END_TIME);
//                        taskUpdates=true;
//                    }

                    // If the work item exp end date is same as sprint exp end date then move the task exp end date same as sprint exp end date
                    if (sprintDb.getSprintExpEndDate().isEqual(sprintTask.getTaskExpEndDate())) {
                        if (epic != null) {
                            if ((epic.getExpEndDateTime() != null && sprintRequest.getSprintExpEndDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprintRequest.getSprintExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                                throw new ValidationFailedException("Work Item's Expected End Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                            }
                        }
                        sprintTask.setTaskExpEndDate(sprintRequest.getSprintExpEndDate());
                        sprintTask.setTaskExpEndTime(sprintRequest.getSprintExpEndDate().toLocalTime());
                        updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                        updatedFields.add(Constants.TaskFields.EXP_END_TIME);
                        taskUpdates = true;
                    }

                    taskServiceImpl.validateExpDateTimeWithEstimate(sprintTask);

                    if (sprintTask.getTaskProgressSystem() != null && (Objects.equals(sprintTask.getTaskProgressSystem(), StatType.DELAYED)
                            || (Objects.equals(sprintTask.getTaskProgressSystem(), StatType.WATCHLIST)
                            && !Objects.equals(sprintTask.getTaskPriority(), Constants.PRIORITY_P0)
                            && !Objects.equals(sprintTask.getTaskPriority(), Constants.PRIORITY_P1)))) {
                        taskService.computeAndUpdateStatForTask(sprintTask, true);
                        taskUpdates = true;
                    }
                    if (!updatedFields.isEmpty()) {
                        // Persist the changes (consider using batch save for performance)
                        taskHistoryService.addTaskHistoryOnSystemUpdate(sprintTaskCopy);
                        taskRepository.save(sprintTask);
                        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, sprintTask);
                    }
                    TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                    task.setTaskNumber(sprintTask.getTaskNumber());
                    task.setTaskId(sprintTask.getTaskId());
                    task.setTaskTitle(sprintTask.getTaskTitle());
                    task.setSprintTitle(sprintDb.getSprintTitle());
                    task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                    task.setTaskTypeId(sprintTask.getTaskTypeId());
                    if (taskUpdates)
                        taskToUpdate.add(task);
                } else {
                    TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                    task.setTaskNumber(sprintTask.getTaskNumber());
                    task.setTaskId(sprintTask.getTaskId());
                    task.setTaskTitle(sprintTask.getTaskTitle());
                    task.setSprintTitle(sprintDb.getSprintTitle());
                    task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                    task.setTaskTypeId(sprintTask.getTaskTypeId());
                    taskToUpdate.add(task);
                }
            }
        }
        return taskToUpdate;
    }

    /**
     * This method verifies if user is allowed to create, update or view sprints
     */
    public Boolean hasModifySprintPermission(String accountIds, Long entityId, Integer entityTypeId) {
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<Integer> authorizeRoleIdList = new ArrayList<>();

        //creating a list of roleIds that are allowed to create sprint
        List<AccountId> authorizeRoleAccountIds = new ArrayList<>();
        authorizeRoleIdList.add(RoleEnum.PROJECT_ADMIN.getRoleId());
        authorizeRoleIdList.add(RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId());


        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            Team team = teamRepository.findByTeamId(entityId);
            if (Objects.equals(team.getFkOrgId().getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
                return false;
            }
            //getting accountIds in case where sprint is created for direct team
            authorizeRoleIdList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
            authorizeRoleIdList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
            authorizeRoleAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityId, authorizeRoleIdList, true);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            Project project = projectRepository.findByProjectId(entityId);
            if (Objects.equals(project.getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
                return false;
            }
            //getting accountIds in case where sprint is created for direct project
            authorizeRoleAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, entityId, authorizeRoleIdList, true);
        }

        Set<Long> authorizeRoleAccountIdsList = new HashSet<>();
        for (AccountId accountId : authorizeRoleAccountIds) {
            authorizeRoleAccountIdsList.add(accountId.getAccountId());
        }

        if (!CommonUtils.containsAny(accountIdList, Arrays.asList(authorizeRoleAccountIdsList.toArray()))) {
            return false;
        }
        return true;
    }

    public String validateSprintConditionAndModifyTaskProperties(Task task, Sprint sprint, Task parentTaskRequest) {
        Task foundTaskDb = taskRepository.findByTaskId(task.getTaskId());

        boolean isTaskInPermissibleState = Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)
                || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) ||
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) ||
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);

        boolean isTaskInActiveSprintState =
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) ||
                        Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);

        // Condition for task already part of another sprint
        boolean wasTaskPartOfAnotherSprint = foundTaskDb != null && foundTaskDb.getSprintId() != null && !foundTaskDb.getSprintId().equals(sprint.getSprintId());

        // Check if the task is in a not started/ backlog  states or if it was in another Sprint earlier, then it can be in started/ blocked/ on hold state
        if (!isTaskInPermissibleState) {
            throw new ValidationFailedException("Cannot add " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " Work Item to sprint. Work Item number : " + task.getTaskNumber());
        }

        // Additional validation for team alignment
        if (Objects.equals(sprint.getEntityTypeId(), Constants.EntityTypes.TEAM) && !Objects.equals(sprint.getEntityId(), task.getFkTeamId().getTeamId())) {
            throw new IllegalStateException("Cannot add Work Item to the sprint. Work Item and sprint belong to different teams");
        }

        // disallow removal of task from completed sprint
        if (foundTaskDb != null && foundTaskDb.getSprintId() != null && task.getSprintId() == null) {
            Sprint sprintFromOriginalTask = sprintRepository.findById(foundTaskDb.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
            if (sprintFromOriginalTask.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new ValidationFailedException("Cannot remove Work Item from a completed sprint");
            }
        }


        // disallow addition of task to a completed sprint
        if (task.getSprintId() != null) {
            Sprint sprintFromOriginalTask = sprintRepository.findById(task.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
            if (foundTaskDb != null && !Objects.equals(sprint.getSprintId(), sprintFromOriginalTask.getSprintId()) && sprintFromOriginalTask.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) && foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                throw new ValidationFailedException("Cannot move Work Item from a completed sprint. The Work Item is currently associated with a completed sprint titled " + sprintFromOriginalTask.getSprintTitle());
            }

            if (Objects.equals(sprint.getSprintId(), sprintFromOriginalTask.getSprintId()) && sprintFromOriginalTask.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new ValidationFailedException("Cannot add Work Item to a completed sprint");
            }
        }

        //
//        if((task.getDependencyIds() != null && !task.getDependencyIds().isEmpty() && parentTaskRequest == null && (sprint.getSprintExpStartDate().isAfter(task.getTaskExpStartDate()) || sprint.getSprintExpEndDate().isBefore(task.getTaskExpEndDate())))
//                ||(task.getDependencyIds() != null && !task.getDependencyIds().isEmpty() && parentTaskRequest != null && (sprint.getSprintExpStartDate().isAfter(parentTaskRequest.getTaskExpStartDate()) || sprint.getSprintExpEndDate().isBefore(parentTaskRequest.getTaskExpEndDate()))))
//            throw new ValidationFailedException("This Task has dependecies and one of its Expected Dates lies out of the range of Sprint Expected Dates.");

        boolean datesAdjusted = addSprintDatesToTask(task, sprint, isTaskInActiveSprintState,parentTaskRequest);

        // Message handling
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Work Item added to sprint ").append(sprint.getSprintTitle()).append(" successfully");
        if (datesAdjusted) stringBuilder.append(", Work Item dates adjusted");

        return stringBuilder.toString();
    }

    public SprintStatusResponse changeSprintStatus(SprintStatusRequest sprintStatusRequest, Long sprintId, String accountIds, String timeZone) throws IllegalAccessException, InvalidKeyException, ValidationFailedException {
        Optional<Sprint> sprintDbOptional = sprintRepository.findById(sprintId);
        if (sprintDbOptional.isEmpty()) {
            throw new ValidationException("Sprint not found");
        }
        Sprint sprintDb = sprintDbOptional.get();
        if (!hasModifySprintPermission(accountIds, sprintDb.getEntityId(), sprintDb.getEntityTypeId())) {
            throw new IllegalAccessException("User is not authorized to change Sprint status");
        }
        if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.DELETED.getSprintStatusId())) {
            throw new ValidationFailedException("Status of deleted sprint can't be updated");
        }
        SprintStatusResponse sprintStatusResponse = new SprintStatusResponse();
        List<TaskNumberTaskTitleSprintName> taskList = new ArrayList<>();
        SprintStatusUpdateObject sprintStatusUpdateObject = new SprintStatusUpdateObject();
        String message = "Sprint status successfully changed to " + sprintStatusRequest.getStatus();
        Boolean isSprintNotMarkedStartedOrCompleted = false;
        if (sprintStatusRequest.getStatus().equalsIgnoreCase(Constants.SprintStatusEnum.STARTED.getSprintStatus())) {
            if (sprintDb.getPreviousSprintId() != null && !sprintStatusRequest.getSkipSprint()) {
                Sprint prevSprint = sprintRepository.findBySprintId(sprintDb.getPreviousSprintId());
                if (!Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                    List<SprintInfo> sprintList = sprintRepository.findAllPreviousInCompleteSprints(sprintDb.getEntityTypeId(), sprintDb.getEntityId(), sprintDb.getSprintExpStartDate(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()).stream().map(sprint -> new SprintInfo(sprint.getSprintTitle(), sprint.getSprintId(), sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate(), sprint.getSprintStatus(), null)).collect(Collectors.toList());
                    sprintStatusResponse.setPastSprintDetails(sprintList);
                    message = "The following sprint are supposed to be Started/Completed before the current sprint, are you sure you want to start the sprint with title " + sprintDb.getSprintTitle();
                    isSprintNotMarkedStartedOrCompleted = true;
                }
            }
            if ((sprintStatusResponse.getPastSprintDetails() == null || sprintStatusResponse.getPastSprintDetails().isEmpty()) || sprintStatusRequest.getSkipSprint()) {
                if (sprintStatusRequest.getSkipSprint()) {
                    List<Sprint> prevSprintList = sprintRepository.findAllPreviousCompleteSprint(sprintDb.getEntityTypeId(), sprintDb.getEntityId(), sprintDb.getSprintExpStartDate(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId());
                    if (prevSprintList!=null && !prevSprintList.isEmpty()) {
                        Sprint prevSprint = prevSprintList.get(0);
                        if (prevSprint != null && prevSprint.getNextSprintId() != null) {
                            Sprint otherSprint = sprintRepository.findBySprintId(prevSprint.getNextSprintId());
                            otherSprint.setPreviousSprintId(null);
                        }
                        if (sprintDb != null && sprintDb.getPreviousSprintId() != null) {
                            Sprint otherSprint = sprintRepository.findBySprintId(sprintDb.getPreviousSprintId());
                            otherSprint.setNextSprintId(null);
                        }
                        prevSprint.setNextSprintId(sprintDb.getSprintId());
                        sprintDb.setPreviousSprintId(prevSprint.getSprintId());
                    }
                }
                sprintStatusUpdateObject = startSprint(sprintDb, sprintStatusRequest, timeZone, accountIds);
                taskList = sprintStatusUpdateObject.getTaskToUpdate();
                if (!taskList.isEmpty()) {
                    message = "Cannot mark the following Work Items as not started, please update them manually";
                }
                if (taskList.isEmpty() && sprintStatusUpdateObject.getAreBlockedOnHoldPresent()) {
                    message = "Sprint status successfully changed to " + sprintStatusRequest.getStatus() + ". Please note that stats of all the blocked and on hold Work Items were changed.";
                }
            }
        }
        else if (sprintStatusRequest.getStatus().equalsIgnoreCase(Constants.SprintStatusEnum.COMPLETED.getSprintStatus())) {
            sprintStatusUpdateObject = endSprint(sprintDb, sprintStatusRequest, accountIds, timeZone);
            taskList = sprintStatusUpdateObject.getTaskToUpdate();
            List<SprintInfo> sprintList = sprintRepository.findAllFutureNotStartedSprints(sprintDb.getEntityTypeId(), sprintDb.getEntityId(), sprintDb.getSprintExpStartDate(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId()).stream().map(sprint -> new SprintInfo(sprint.getSprintTitle(), sprint.getSprintId(), sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate(), sprint.getSprintStatus(), null)).collect(Collectors.toList());
            sprintStatusResponse.setFutureSprintDetails(sprintList);
            if (!sprintStatusUpdateObject.getTaskToUpdate().isEmpty()) {
                message = "Before completing the sprint, please either move the following Work Items to future sprints or mark them as completed.";
                isSprintNotMarkedStartedOrCompleted = true;
            }
            if (sprintStatusUpdateObject.getTaskToUpdate().isEmpty() && sprintStatusUpdateObject.getAreBlockedOnHoldPresent()) {
                message = "Sprint status successfully changed to " + sprintStatusRequest.getStatus() + ". Please note that stats of all the blocked and on hold Work Items were changed before moving the Work Items.";
            }
        }
        else if (sprintStatusRequest.getStatus().equalsIgnoreCase(Constants.SprintStatusEnum.DELETED.getSprintStatus())) {
            sprintStatusUpdateObject = deleteSprint(sprintDb, sprintStatusRequest, accountIds, timeZone);
            taskList = sprintStatusUpdateObject.getTaskToUpdate();
            if (!sprintStatusUpdateObject.getTaskToUpdate().isEmpty()) {
                message = "Before deleting the sprint, please either either remove the following Work item or mark it deleted.";
            }
            if (sprintStatusUpdateObject.getTaskToUpdate().isEmpty()) {
                message = "Sprint status successfully changed to " + sprintStatusRequest.getStatus();
            }
        }
        auditService.auditForSprintStatusUpdate(accountIds, sprintDb, Constants.SprintStatusEnum.getIdByName(sprintStatusRequest.getStatus()));
        sprintStatusResponse.setSprintmessage(message);
        sprintStatusResponse.setTaskList(taskList);
        if(!isSprintNotMarkedStartedOrCompleted)
            notificationService.updateSprintNotification(sprintDb,timeZone, accountIds, Constants.SprintStatusEnum.getIdByName(sprintStatusRequest.getStatus()));
        return sprintStatusResponse;
    }

    private SprintStatusUpdateObject startSprint(Sprint sprintDb, SprintStatusRequest sprintStatusRequest, String timeZone, String accountIds) throws IllegalAccessException, ValidationFailedException, InvalidKeyException {
        List<Task> sprintTasks = taskRepository.findBySprintId(sprintDb.getSprintId()).stream()
                .map(originalTask -> {
                    Task copy = new Task();
                    BeanUtils.copyProperties(originalTask, copy);
                    return copy;
                })
                .collect(Collectors.toList());
        sprintTasks.sort(Comparator.comparing(Task::getTaskTypeId, Comparator.reverseOrder()));
        SprintStatusUpdateObject sprintStatusUpdateObject = new SprintStatusUpdateObject();
        if (!Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && !Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) && sprintStatusRequest.getStatus().equalsIgnoreCase(Constants.SprintStatusEnum.STARTED.getSprintStatus())) {
            //To start a sprint, there should be tasks linked to that sprint
            if (sprintTasks.isEmpty()) {
                throw new ValidationFailedException("Sprint can't be started as there are no work items in the sprint.");
            }

            if (sprintRepository.existsByEntityTypeIdAndEntityIdAndSprintStatusNotIn(Constants.EntityTypes.TEAM, sprintDb.getEntityId(), List.of(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId(), Constants.SprintStatusEnum.DELETED.getSprintStatusId()))) {
                throw new ValidationFailedException("Cannot start the sprint. There is an active sprint present in the team.");
            }

            LocalDateTime sprintActStartDateInServerTime = DateTimeUtils.convertUserDateToServerTimezone(sprintStatusRequest.getStatusUpdateDate(), timeZone);
            if (sprintActStartDateInServerTime.isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("Invalid Sprint Start Date: Please choose sprint actual start date on or before today");
            }

            TeamPreferenceResponse teamPreference = entityPreferenceService.getTeamPreference(sprintDb.getEntityId());
            if(teamPreference!=null){
                sprintStatusRequest.setMinTimeToStart(teamPreference.getBufferTimeToStartSprintEarly());
            }
            if (sprintStatusRequest.getMinTimeToStart() == null)
                sprintStatusRequest.setMinTimeToStart(180); //To-Do: By Default we are adding 3hour as Buffer if we have null value in DB

            Integer hoursOfSprint = 0;
            List<TaskNumberTaskTitleSprintName> taskToUpdate = new ArrayList<>();
            for (Task sprintTask : sprintTasks) {

                if (sprintTask.getTaskExpEndDate() == null || sprintTask.getTaskExpStartDate() == null) {
                    Optional<Task> parentTaskOptional = sprintTasks.stream().filter(task -> task.getTaskId().equals(sprintTask.getParentTaskId())).findFirst();

                    if (sprintTask.getTaskExpEndDate() == null) {
                        if (parentTaskOptional.isPresent() && parentTaskOptional.get().getTaskExpEndDate() != null) {
                            sprintTask.setTaskExpEndDate(parentTaskOptional.get().getTaskExpEndDate());
                            sprintTask.setTaskExpEndTime(parentTaskOptional.get().getTaskExpEndTime());
                        } else {
                            sprintTask.setTaskExpEndDate(sprintDb.getSprintExpEndDate());
                            sprintTask.setTaskExpEndTime(sprintDb.getSprintExpEndDate().toLocalTime());
                        }
                    }

                    if (sprintTask.getTaskExpStartDate() == null) {
                        if (parentTaskOptional.isPresent() && parentTaskOptional.get().getTaskExpStartDate() != null) {
                            sprintTask.setTaskExpStartDate(parentTaskOptional.get().getTaskExpStartDate());
                            sprintTask.setTaskExpStartTime(parentTaskOptional.get().getTaskExpStartTime());
                        } else {
                            sprintTask.setTaskExpStartDate(sprintDb.getSprintExpStartDate());
                            sprintTask.setTaskExpStartTime(sprintDb.getSprintExpStartDate().toLocalTime());
                        }
                    }
                }

                if (Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                    if (sprintTask.getTaskEstimate() != null && !sprintTask.getTaskEstimate().equals(0) && sprintTask.getFkAccountIdAssigned() != null && sprintTask.getTaskPriority() != null && sprintStatusRequest.getAutoUpdate() && sprintTask.getTaskExpStartDate() != null && sprintTask.getTaskExpEndDate() != null) {
                        Task sprintTaskCopy = new Task();
                        BeanUtils.copyProperties(sprintTask, sprintTaskCopy);
                        List<String> updatedFields = new ArrayList<>();
                        WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, sprintTask.getTaskWorkflowId());
                        sprintTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
                        sprintTask.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
                        updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                        taskService.computeAndUpdateStatForTask(sprintTask, true);
                        updatedFields.add(Constants.TaskFields.TASK_STATE);

                        // Persist the changes (consider using batch save for performance)
                        taskHistoryService.addTaskHistoryOnSystemUpdate(sprintTaskCopy);
                        taskRepository.save(sprintTask);
                        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, sprintTask);
                    }
                    else {
                        String message = "Following are missing/incorrect: ";
                        boolean isEstimateNull = false, isPriorityNull = false;
                        if (sprintTask.getTaskEstimate() == null || sprintTask.getTaskEstimate().equals(0)) {
                            isEstimateNull = true;
                            message += "Estimate";
                        }
                        if (sprintTask.getTaskPriority() == null) {
                            isPriorityNull = true;
                            message += (isEstimateNull ? ", Priority" : "Priority");
                        }
                        if (sprintTask.getFkAccountIdAssigned() == null) {
                            message += (isPriorityNull || isEstimateNull ? ", AssignedTo" : "AssignedTo");
                        }
                        TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                        task.setTaskNumber(sprintTask.getTaskNumber());
                        task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                        task.setTaskId(sprintTask.getTaskId());
                        task.setTaskTitle(sprintTask.getTaskTitle());
                        task.setSprintTitle(sprintDb.getSprintTitle());
                        task.setTaskTypeId(sprintTask.getTaskTypeId());
                        task.setMessage(message);
                        taskToUpdate.add(task);
                    }
                }
                else {
                    if (sprintTask.getFkAccountIdAssigned() == null) {
                        String message = "Following are missing/incorrect: AssignedTo";
                        TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                        task.setTaskNumber(sprintTask.getTaskNumber());
                        task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                        task.setTaskId(sprintTask.getTaskId());
                        task.setTaskTitle(sprintTask.getTaskTitle());
                        task.setSprintTitle(sprintDb.getSprintTitle());
                        task.setTaskTypeId(sprintTask.getTaskTypeId());
                        task.setMessage(message);
                        taskToUpdate.add(task);
                    }
                }
                if (Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) || Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE)) {
                    updateBlockOnHoldStatusForTask(sprintTask, sprintStatusUpdateObject);
                }
                if (sprintTask.getTaskEstimate() != null && !Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    hoursOfSprint += sprintTask.getTaskEstimate();
                }
            }
            if (taskToUpdate.isEmpty()) {
                SprintRequest sprintRequest = new SprintRequest();
                BeanUtils.copyProperties(sprintDb, sprintRequest);
                if (sprintActStartDateInServerTime.isBefore(sprintDb.getSprintExpStartDate().minusMinutes(sprintStatusRequest.getMinTimeToStart()))) {
                    sprintRequest.setSprintExpStartDate(sprintActStartDateInServerTime);
                }
                sprintRequest.setSprintStatus(Constants.SprintStatusEnum.STARTED.getSprintStatusId());
                updateSprint(sprintRequest, sprintDb.getSprintId(), accountIds, timeZone);
                sprintDb.setSprintActStartDate(sprintActStartDateInServerTime.withSecond(0).withNano(0));
                sprintDb.setSprintStatus(Constants.SprintStatusEnum.STARTED.getSprintStatusId());
                sprintDb.setHoursOfSprint(hoursOfSprint);
                if (!sprintDb.getCanModifyIndicatorStayActiveInStartedSprint() && sprintDb.getCanModifyEstimates()) {
                    sprintDb.setCanModifyEstimates(false);
                }
                sprintRepository.save(sprintDb);
            }
            sprintStatusUpdateObject.setTaskToUpdate(taskToUpdate);
            return sprintStatusUpdateObject;
        } else {
            throw new IllegalStateException("Invalid sprint status for starting sprint");
        }
    }


    private SprintStatusUpdateObject endSprint(Sprint sprintDb, SprintStatusRequest sprintStatusRequest, String accountIds, String timeZone) throws IllegalAccessException {
        if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && sprintStatusRequest.getStatus().equalsIgnoreCase(Constants.SprintStatusEnum.COMPLETED.getSprintStatus())) {
            SprintStatusUpdateObject sprintStatusUpdateObject = new SprintStatusUpdateObject();
            //validating status update date
            LocalDateTime sprintActEndDateInServerTime = DateTimeUtils.convertUserDateToServerTimezone(sprintStatusRequest.getStatusUpdateDate(), timeZone);
            if (sprintActEndDateInServerTime.isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("Invalid Sprint End Date: Please choose sprint actual end date on or before today");
            }

            if (sprintDb.getSprintActStartDate() == null)
                throw new ValidationException("This sprint has not been started.");

            if (sprintActEndDateInServerTime.withSecond(0).withNano(0).isBefore(sprintDb.getSprintActStartDate())) {
                throw new IllegalStateException("Sprint Actual End Date must not be earlier than the Actual Start Date.");
            }

            if (sprintActEndDateInServerTime.withSecond(0).withNano(0).isBefore(sprintDb.getSprintExpStartDate())) {
                throw new IllegalStateException("Sprint Actual End Date must not be earlier than the Expected Start Date.");
            }

            List<Task> sprintTasks = taskRepository.findBySprintId(sprintDb.getSprintId());

            SprintResponseForGetAllSprints sprintResponseForGetAllSprints = getSprintTaskStats(sprintDb, sprintTasks, timeZone);
            List<TaskNumberTaskTitleSprintName> taskToMove = new ArrayList<>();
            MoveSprintTaskRequest moveSprintTaskRequest = new MoveSprintTaskRequest();
            List<MoveSprintTask> moveSprintTaskList = new ArrayList<>();
            //Checking if all the sprint tasks are completed to end the sprint
            for (Task sprintTask : sprintTasks) {
                if (Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                    continue;
                }
                if (!Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) && !Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    if (sprintStatusRequest.getAutoUpdate() && sprintDb.getNextSprintId() != null) {
                        Optional<Sprint> nextSprintOptional = sprintRepository.findById(sprintDb.getNextSprintId());
                        if (nextSprintOptional.isEmpty() || !Objects.equals(nextSprintOptional.get().getPreviousSprintId(), sprintDb.getSprintId())) {
                            throw new IllegalAccessException("Unable to complete sprint: The next sprint does not exist. Please ensure that the next sprint added is valid before completing the current sprint.");
                        }
                        MoveSprintTask moveSprintTask = new MoveSprintTask(sprintDb.getSprintId(), sprintTask.getTaskId(), nextSprintOptional.get().getSprintId());
                        try {
                            moveSprintTaskRequest.setSprintTaskList(List.of(moveSprintTask));
                            moveSprintTaskRequest.setEntityTypeId(sprintDb.getEntityTypeId());
                            moveSprintTaskRequest.setEntityId(sprintDb.getEntityId());
                            validateSprintConditionAndMoveTaskFromSprint(moveSprintTaskRequest, accountIds, true);
                        } catch (Exception e) {
                            TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                            task.setTaskNumber(sprintTask.getTaskNumber());
                            task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                            task.setTaskId(sprintTask.getTaskId());
                            task.setTaskTitle(sprintTask.getTaskTitle());
                            task.setSprintTitle(sprintDb.getSprintTitle());
                            task.setTaskTypeId(sprintTask.getTaskTypeId());
                            task.setMessage(e.getMessage());
                            taskToMove.add(task);
                        }
                    } else {
                        TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                        task.setTaskNumber(sprintTask.getTaskNumber());
                        task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                        task.setTaskId(sprintTask.getTaskId());
                        task.setTaskTitle(sprintTask.getTaskTitle());
                        task.setSprintTitle(sprintDb.getSprintTitle());
                        task.setTaskTypeId(sprintTask.getTaskTypeId());
                        task.setMessage(sprintStatusRequest.getAutoUpdate() ? "Next sprint don't exists" : "User decided not to automatically move Work Item to next sprint");
                        taskToMove.add(task);
                    }
                }

                if (Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) || Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE)) {
                    Task taskRequest = new Task();
                    BeanUtils.copyProperties(sprintTask, taskRequest);
                    updateBlockOnHoldStatusForTask(taskRequest, sprintStatusUpdateObject);
                }
            }

            //To complete a sprint it is required to add sprint end date
            if (taskToMove.isEmpty()) {
                CompletedSprintStats completedSprintStats = new CompletedSprintStats();
                completedSprintStats.setSprintId(sprintDb.getSprintId());
                BeanUtils.copyProperties(sprintResponseForGetAllSprints, completedSprintStats);
                completedSprintStats.setNotStartedTasksList(sprintResponseForGetAllSprints.getNotStartedTasksList());
                completedSprintStats.setWatchListTasksList(sprintResponseForGetAllSprints.getWatchListTasksList());
                completedSprintStats.setOnTrackTasksList(sprintResponseForGetAllSprints.getOnTrackTasksList());
                completedSprintStats.setDelayedTasksList(sprintResponseForGetAllSprints.getDelayedTasksList());
                completedSprintStats.setCompletedTasksList(sprintResponseForGetAllSprints.getCompletedTasksList());
                completedSprintStats.setLateCompletedTasksList(sprintResponseForGetAllSprints.getLateCompletedTasksList());
                completedSprintStats.setDeletedTasksList(sprintResponseForGetAllSprints.getDeletedTasksList());

                completedSprintStatsRepository.save(completedSprintStats);

                sprintDb.setSprintActEndDate(sprintActEndDateInServerTime.withSecond(0).withNano(0));
                sprintDb.setSprintStatus(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId());
                sprintRepository.save(sprintDb);
            }
            sprintStatusUpdateObject.setTaskToUpdate(taskToMove);
            return sprintStatusUpdateObject;
        } else {
            throw new IllegalStateException("Invalid sprint status for ending sprint");
        }
    }

    /**
     * This method verifies if user is allowed to create, update or view sprints
     */
    private Boolean hasSprintViewPermission(List<Long> accountIds, Sprint sprint, Long entityId, Integer entityTypeId) {
//        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<Integer> authorizeRoleIdList = new ArrayList<>();

        //creating a list of roleIds that are allowed to create sprint
        List<AccountId> authorizeRoleAccountIds = new ArrayList<>();
        authorizeRoleIdList.add(RoleEnum.PROJECT_ADMIN.getRoleId());
        authorizeRoleIdList.add(RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId());


        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            Team team = teamRepository.findByTeamId(entityId);
            if (Objects.equals(team.getFkOrgId().getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
                return false;
            }
            //getting accountIds in case where sprint is created for direct team
            authorizeRoleIdList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
            authorizeRoleIdList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
            authorizeRoleAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityId, authorizeRoleIdList, true);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            Project project = projectRepository.findByProjectId(entityId);
            if (Objects.equals(project.getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
                return false;
            }
            //getting accountIds in case where sprint is created for direct project
            authorizeRoleAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, entityId, authorizeRoleIdList, true);
        }

        Set<Long> authorizeRoleAccountIdsList = authorizeRoleAccountIds.stream().map(AccountId::getAccountId).collect(Collectors.toSet());

        Set<EmailFirstLastAccountId> sprintMembers = sprint.getSprintMembers();
        if (sprintMembers != null && !sprintMembers.isEmpty()) {
            authorizeRoleAccountIdsList.addAll(sprintMembers.stream().map(EmailFirstLastAccountId::getAccountId).collect(Collectors.toSet()));
        }
        if (!CommonUtils.containsAny(accountIds, Arrays.asList(authorizeRoleAccountIdsList.toArray()))) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param moveSprintTaskRequest
     * @param accountIds
     * @param onComplete
     * @return
     * @throws IllegalAccessException
     */
    public String validateSprintConditionAndMoveTaskFromSprint(MoveSprintTaskRequest moveSprintTaskRequest, String
            accountIds, Boolean onComplete) throws IllegalAccessException {
        if (!hasModifySprintPermission(accountIds, moveSprintTaskRequest.getEntityId(), moveSprintTaskRequest.getEntityTypeId())) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to move/remove task from sprint");
        }

        UserAccount userAccount = new UserAccount();
        if(Objects.equals(moveSprintTaskRequest.getEntityTypeId(), Constants.EntityTypes.TEAM))
            userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(jwtRequestFilter.getAccountIdsFromHeader(accountIds), teamRepository.findByTeamId(moveSprintTaskRequest.getEntityId()).getFkOrgId().getOrgId(), true);
        else if(Objects.equals(moveSprintTaskRequest.getEntityTypeId(), Constants.EntityTypes.PROJECT))
            userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(jwtRequestFilter.getAccountIdsFromHeader(accountIds), projectRepository.findByProjectId(moveSprintTaskRequest.getEntityId()).getOrgId(), true);

        HashSet<MoveSprintTask> moveSprintTaskList = new HashSet<>(moveSprintTaskRequest.getSprintTaskList());
        if (moveSprintTaskList.isEmpty()) {
            throw new ValidationFailedException("The provided list of Work Items to move is empty. Please provide valid data to initiate the Work Item movement");
        }
        HashMap<Long, Task> allTaskMap = new HashMap<>();
        HashMap<Task, Sprint> taskDirectlyMoveMap = new HashMap<>();
        HashMap<Long, Sprint> taskDependencyMoveMap = new HashMap<>();
        HashMap<Long, Task> childToParentMap = new HashMap<>();
        HashMap<Long, Sprint> parentToSprintMap = new HashMap<>();
        HashMap<Long, Sprint> sprintMap = new HashMap<>();
        String message = "";

        TaskProcessingContext taskProcessingContext = new TaskProcessingContext();
        ArrayList<Task> taskMoveList = new ArrayList<>();

        Map<Long, List<Long>> sprintIdToMemberAccountIdMap = new HashMap<>();

        for (MoveSprintTask moveSprintTask : moveSprintTaskList) {
            if (!onComplete) {
                if (moveSprintTask.getPreviousSprintId() == null) {
                    throw new ValidationFailedException("Please provide the sprint from which the Work Item is being moved");
                }
                if (Objects.equals(moveSprintTask.getNewSprintId(), moveSprintTask.getPreviousSprintId())) {
                    throw new ValidationFailedException("Cannot move the Work Item to the same sprint. Please select a different sprint");
                }
            }
            Task foundTaskDb = taskRepository.findByTaskId(moveSprintTask.getTaskId());
            if (foundTaskDb == null) {
                throw new TaskNotFoundException();
            }
            Task foundTask = new Task();
            BeanUtils.copyProperties(foundTaskDb, foundTask);

            validateAssignToInStartedSprint (foundTask, moveSprintTask, sprintMap);
            if (foundTask.getFkAccountIdAssigned() != null && moveSprintTask.getNewSprintId() != null) {
                Long newSprintId = moveSprintTask.getNewSprintId();
                List<Long> sprintMemberAccountIdList;

                if (sprintIdToMemberAccountIdMap.containsKey(newSprintId)) {
                    sprintMemberAccountIdList = sprintIdToMemberAccountIdMap.get(newSprintId);
                } else {
                    Optional<Sprint> optionalSprint = sprintRepository.findById(newSprintId);
                    if (optionalSprint.isEmpty()) {
                        throw new ValidationFailedException("New selected sprint for work item " + foundTask.getTaskNumber() + " does not exist");
                    }

                    Set<EmailFirstLastAccountId> sprintMemberList = optionalSprint.get().getSprintMembers();
                    if (sprintMemberList == null) {
                        sprintMemberList = new HashSet<>();
                    }
                    sprintMemberAccountIdList = sprintMemberList.stream()
                            .filter(Objects::nonNull)
                            .map(EmailFirstLastAccountId::getAccountId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    sprintIdToMemberAccountIdMap.put(newSprintId, sprintMemberAccountIdList);
                }

                Long assignedAccountId = foundTask.getFkAccountIdAssigned().getAccountId();
                if (assignedAccountId == null || !sprintMemberAccountIdList.contains(assignedAccountId)) {
                    throw new IllegalAccessException("Assigned To user of Work Item " + foundTask.getTaskNumber() + " is not part of new selected Sprint");
                }
            }

            allTaskMap.put(foundTask.getTaskId(), foundTask);
            createTaskMoveMap(foundTask, moveSprintTask, taskDirectlyMoveMap, taskDependencyMoveMap, childToParentMap, parentToSprintMap, moveSprintTaskList, onComplete, sprintMap, allTaskMap);
        }

        for (Map.Entry<Task, Sprint> move : taskDirectlyMoveMap.entrySet()) {
            Task task = move.getKey();
            Sprint sprint = move.getValue();
            processTaskWithSprint(task, sprint, childToParentMap, message, onComplete, false, null, false, sprintMap, userAccount);
        }

        for (Map.Entry<Long, Sprint> move : taskDependencyMoveMap.entrySet()) {
            Task task = allTaskMap.get(move.getKey());
            taskProcessingContext.getIsVisited().put(task.getTaskId(), false);
            taskProcessingContext.getProcessedTasks().put(task.getTaskId(), -2L);
            taskMoveList.add(task);
            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                taskProcessingContext.getParentExpStartDate().put(task.getTaskId(), task.getTaskExpStartDate());
                taskProcessingContext.getParentExpEndDate().put(task.getTaskId(), task.getTaskExpEndDate());
            }
            else if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                taskProcessingContext.getParentExpStartDate().put(task.getParentTaskId(), move.getValue().getSprintExpEndDate());
                taskProcessingContext.getParentExpEndDate().put(task.getParentTaskId(), move.getValue().getSprintExpStartDate());
            }
        }

        Collections.sort(taskMoveList, Comparator
                .comparing(Task::getTaskExpStartDate));
        //taskMoveList is for the Dependency whether its child or parent type.
        // if A,B is in sprint 1 and C,D is in sprint2 (linear) and we are moving B in sprint 2
        //Contains all non-parent tasks of the request (and parent tasks with children having no dependencies) from sprint1
        for (Task task : taskMoveList) {
            Sprint sprint = taskDependencyMoveMap.get(task.getTaskId());
            processTaskWithSprint(task, sprint, childToParentMap, message, onComplete, true, taskProcessingContext, false, sprintMap, userAccount);
        }

        //This contains all non-parent tasks (and parent tasks with children having no dependencies) from sprint2
        for (Task task : taskProcessingContext.getSprint2TasksToProcess()) {
            Task move = new Task();
            BeanUtils.copyProperties(task, move);
            Sprint prevSprint = sprintMap.get(task.getSprintId());
            if (prevSprint == null) {
                prevSprint = sprintRepository.findBySprintId(task.getSprintId());
                sprintMap.put(task.getSprintId(), prevSprint);
            }
            Sprint sprint = new Sprint();
            BeanUtils.copyProperties(prevSprint, sprint);
            processTaskWithSprint(move, sprint,childToParentMap, message, false, false, taskProcessingContext, true, sprintMap, userAccount);
        }

        for( Task task : taskProcessingContext.getChildTasksWithParentDependencies()) {
            Task move = new Task();
            BeanUtils.copyProperties(task, move);
            Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
            childToParentMap.put(task.getTaskId(), parentTask);
            Sprint sprint = sprintRepository.findBySprintId(parentTask.getSprintId());
            processTaskWithSprint(move, sprint, childToParentMap, message, onComplete, false, taskProcessingContext, false, sprintMap, userAccount);
        }

        //This contains all dependency tasks but we will only process parent tasks
        //We store the last element of the chain for a particular dependency tasks in processedTasks
        for (Map.Entry<Long, Long> move : taskProcessingContext.getProcessedTasks().entrySet()) {
            Task task = taskRepository.findByTaskId(move.getKey());
            if(!Objects.equals(task.getTaskTypeId(),Constants.TaskTypes.PARENT_TASK))
                continue;
            Task moveTask = new Task();
            BeanUtils.copyProperties(task,moveTask);
            Sprint sprint = parentToSprintMap.get(task.getTaskId());
            if(moveTask.getCountChildExternalDependencies()!=0 || moveTask.getCountChildInternalDependencies()!=0)
                processTaskWithSprint(moveTask, sprint, childToParentMap, message, onComplete, true, taskProcessingContext, false, sprintMap, userAccount);
            else
                continue;
        }

        List<Long> taskList = moveSprintTaskRequest.getSprintTaskList().parallelStream().map(MoveSprintTask::getTaskId).collect(Collectors.toList());
        dependencyService.reCalculateLagTimeOnTaskUpdates(taskList);

        sprintRepository.saveAll(sprintMap.values());
        if (moveSprintTaskList.size() < 2) {
            return message;
        } else {
            return "Work Item updated successfully with respect to the sprints";
        }
    }

    public void validateAssignToInStartedSprint(Task foundTask, MoveSprintTask moveSprintTask, HashMap<Long, Sprint> sprintMap) {
        if (foundTask.getFkAccountIdAssigned() == null && moveSprintTask.getNewSprintId() != null) {
            Sprint newSprint = null;
            if (sprintMap.containsKey(moveSprintTask.getNewSprintId())) {
                newSprint = sprintMap.get(moveSprintTask.getNewSprintId());
            }
            else {
                Optional<Sprint> optionalSprint = sprintRepository.findById(moveSprintTask.getNewSprintId());
                if (optionalSprint.isPresent()) {
                    newSprint = optionalSprint.get();
                    sprintMap.put(moveSprintTask.getNewSprintId(), newSprint);
                }
            }
            if (newSprint != null && Objects.equals(newSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                throw new ValidationFailedException("Assign To in work item is mandatory in started sprint");
            }
        }
    }

    /**
     * This api returns list of sprintResponseForFilter on the basis of entity type provided
     */
    public List<SprintResponseForFilter> getAllSprintForEntity(String accountIds, Integer entityTypeId, Long entityId, String timeZone) throws IllegalAccessException {
        List<SprintResponseForFilter> sprintResponseForFilterList = new ArrayList<>();
        List<Long> teamIdList = new ArrayList<>();
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            teamIdList = Collections.singletonList(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            teamIdList = teamRepository.findTeamIdsByProjectId(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            teamIdList = teamRepository.findTeamIdsByOrgId(entityId);
        } else {
            List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
            List<Long> orgIdList = userAccountRepository.getAllOrgIdByAccountIdInAndIsActive(accountIdList, true);
            teamIdList = teamRepository.findByFkOrgIdOrgIdIn(orgIdList).stream().map(Team::getTeamId).collect(Collectors.toList());
        }
        List<Long> unauthorizedTeamIdList = new ArrayList<>();
        for (Long teamId : teamIdList) {
            if (!hasModifySprintPermission(accountIds, teamId, Constants.EntityTypes.TEAM)) {
                unauthorizedTeamIdList.add(teamId);
            }
        }
        List<Long> modifiableTeamIdList = new ArrayList<>(teamIdList);
        if (!unauthorizedTeamIdList.isEmpty()) {
            modifiableTeamIdList.removeAll(unauthorizedTeamIdList);
        }
        sprintResponseForFilterList = sprintRepository.getCustomAllActiveSprintDetailsForEntities(modifiableTeamIdList, Constants.EntityTypes.TEAM);
        for (SprintResponseForFilter sprintResponseForFilter : sprintResponseForFilterList) {
            sprintResponseForFilter.setSprintExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprintResponseForFilter.getSprintExpStartDate(), timeZone));
            sprintResponseForFilter.setSprintExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprintResponseForFilter.getSprintExpEndDate(), timeZone));
        }
        return sprintResponseForFilterList;

    }

    public void convertAllSprintDateToServerTimeZone(SprintRequest sprintRequest, String timeZone) {
        sprintRequest.setSprintExpStartDate(DateTimeUtils.convertUserDateToServerTimezone(sprintRequest.getSprintExpStartDate(), timeZone));
        sprintRequest.setSprintExpEndDate(DateTimeUtils.convertUserDateToServerTimezone(sprintRequest.getSprintExpEndDate(), timeZone));

        if (sprintRequest.getCapacityAdjustmentDeadline() != null) {
            sprintRequest.setCapacityAdjustmentDeadline(DateTimeUtils.convertUserDateToServerTimezone(sprintRequest.getCapacityAdjustmentDeadline(), timeZone));
        }
        if (sprintRequest.getSprintActStartDate() != null) {
            sprintRequest.setSprintActStartDate(DateTimeUtils.convertUserDateToServerTimezone(sprintRequest.getSprintActStartDate(), timeZone));
        }
        if (sprintRequest.getSprintActEndDate() != null) {
            sprintRequest.setSprintActEndDate(DateTimeUtils.convertUserDateToServerTimezone(sprintRequest.getSprintActEndDate(), timeZone));
        }
    }

    public void convertAllSprintDateToUserTimeZone (Sprint sprint, String timeZone) {
        sprint.setSprintExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpStartDate(), timeZone));
        sprint.setSprintExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpEndDate(), timeZone));

        if (sprint.getCapacityAdjustmentDeadline() != null) {
            sprint.setCapacityAdjustmentDeadline(DateTimeUtils.convertServerDateToUserTimezone(sprint.getCapacityAdjustmentDeadline(), timeZone));
        }
        if (sprint.getSprintActStartDate() != null) {
            sprint.setSprintActStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintActStartDate(), timeZone));
        }
        if (sprint.getSprintActEndDate() != null) {
            sprint.setSprintActEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintActEndDate(), timeZone));
        }
    }


    /**
     * This api returns list of sprintResponseForTeamList on the basis of entity type provided
     */
    public List<TeamAndSprintResponse> getAllSprintInEntity(String accountIds, Integer entityTypeId, Long entityId) {

        List<TeamAndSprintResponse> sprintResponseForTeamList = new ArrayList<>();
        List<Team> teamList = new ArrayList<>();
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            teamList = Collections.singletonList(teamRepository.findByTeamId(entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            teamList = teamRepository.findByFkProjectIdProjectId(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            teamList = teamRepository.findByFkOrgIdOrgId(entityId);
        } else {
            List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
            List<Long> orgIdList = userAccountRepository.getAllOrgIdByAccountIdInAndIsActive(accountIdList, true);
            if (orgIdList.contains(Long.valueOf(Constants.OrgIds.PERSONAL))) {
                orgIdList.remove(Long.valueOf(Constants.OrgIds.PERSONAL));
            }
            teamList = teamRepository.findByFkOrgIdOrgIdIn(orgIdList);
        }

        for (Team team : teamList) {
            if (!hasModifySprintPermission(accountIds, team.getTeamId(), Constants.EntityTypes.TEAM) || Objects.equals(team.getFkOrgId().getOrgId(), Long.valueOf(Constants.OrgIds.PERSONAL))) {
                continue;
            }
            TeamAndSprintResponse teamAndSprintResponse = new TeamAndSprintResponse();
            teamAndSprintResponse.setTeamId(team.getTeamId());
            teamAndSprintResponse.setTeamName(team.getTeamName());
            if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG) && !(team.getFkProjectId().getProjectName().contains(Constants.DEFAULT_INDICATOR) && Objects.equals(team.getFkProjectId().getProjectType(), com.tse.core_application.constants.Constants.ProjectType.DEFAULT_PROJECT))) {
                teamAndSprintResponse.setDisplayName(team.getTeamName() + " (" + team.getFkProjectId().getProjectName() + ")");
            } else {
                teamAndSprintResponse.setDisplayName(team.getTeamName());
            }
            teamAndSprintResponse.setProjectName(team.getFkProjectId().getProjectName());
            teamAndSprintResponse.setOrgName(team.getFkOrgId().getOrganizationName());
            teamAndSprintResponse.setSprintList(sprintRepository.getSprintTitleAndSprintIdByEntityTypeIdAndEntityId(team.getTeamId(), Constants.EntityTypes.TEAM));
            sprintResponseForTeamList.add(teamAndSprintResponse);
        }

        return sprintResponseForTeamList;
    }


    /** method calculates the data for the burn down graph within a sprint.*/
    public BurnDownResponse calculateBurnDownData(BurnDownRequest request, List<Long> accountIds) {
        Sprint sprint = sprintRepository.findById(request.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found."));
        Team team = teamRepository.findByTeamId(sprint.getEntityId());

        //verifying if user have access to generate the velocity chart
        if (!hasSprintViewPermission(accountIds, sprint, team.getTeamId(), Constants.EntityTypes.TEAM)) {
            throw new UnauthorizedException("Unauthorized: User does not have permission to view burn down data");
        }

        Pair<List<Integer>, Integer> offDaysAndOfficeMinsPerDay = entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(team.getFkOrgId().getOrgId());

        LocalDate startDate = sprint.getSprintExpStartDate().toLocalDate();
        LocalDate endDate = sprint.getSprintExpEndDate().toLocalDate();

        int maxWorkingMinutesPerDay = offDaysAndOfficeMinsPerDay.getSecond();
        List<TimeSheet> allTimesheetRecords;

        Set<LocalDate> nonWorkingDays = new HashSet<>();
        int totalWorkingDays = 0;
        // Calculate total working days and identify non-working days
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!offDaysAndOfficeMinsPerDay.getFirst().contains(date.getDayOfWeek().getValue())) {
                totalWorkingDays++;
            } else {
                nonWorkingDays.add(date);
            }
        }

        List<UserCapacityMetrics> userCapacityMetricsList = userCapacityMetricsRepository.findBySprintId(sprint.getSprintId());
        int totalTeamMembers = request.getAccountId() == null ? sprint.getSprintMembers().size() : 1;

        int idealMinutesStart = sprint.getSprintMembers() == null ? 0 :
                sprint.getSprintMembers().stream()
                        .map(EmailFirstLastAccountId::getAccountId)
                        .filter(Objects::nonNull)
                        .map(accountId -> userCapacityMetricsList.stream()
                                .filter(ucm -> ucm.getAccountId().equals(accountId) && ucm.getCurrentPlannedCapacity() != null)
                                .map(UserCapacityMetrics::getCurrentPlannedCapacity)
                                .findFirst()
                                .orElse(0))
                        .mapToInt(Integer::intValue)
                        .sum();

        int idealMinutes = idealMinutesStart;

        if (request.getAccountId() == null) {

            idealMinutes = idealMinutesStart;

            allTimesheetRecords = timeSheetRepository.findByTeamIdAndSprintIdAndNewEffortDateBetween(team.getTeamId(), sprint.getSprintId(), startDate, endDate);
        } else {
            idealMinutes = userCapacityMetricsList.stream()
                    .filter(ucm -> ucm.getAccountId().equals(request.getAccountId()))
                    .map(UserCapacityMetrics::getCurrentPlannedCapacity)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);

            allTimesheetRecords = timeSheetRepository.findByTeamIdAndSprintIdAndAccountIdAndNewEffortDateBetween(
                    team.getTeamId(), sprint.getSprintId(), request.getAccountId(), startDate, endDate);
        }

        Map<LocalDate, Integer> dailyRecordedEffort = new HashMap<>();
        Map<LocalDate, Integer> dailyEarnedEffort = new HashMap<>();

        for (TimeSheet record : allTimesheetRecords) {
            LocalDate recordDate = record.getNewEffortDate();
            dailyRecordedEffort.put(recordDate, dailyRecordedEffort.getOrDefault(recordDate, 0) + record.getNewEffort());
            dailyEarnedEffort.put(recordDate, dailyEarnedEffort.getOrDefault(recordDate, 0) + record.getEarnedTime());
        }

        Map<LocalDate, BurnDownDetails> burnDownData = new TreeMap<>();
        int cumulativeRecorded = 0;
        int cumulativeEarned = 0;

        int dailyIdealDecrement = 0;

// track remaining ideal separately so we can force it to 0 on the last working day
        int remainingIdeal = (request.getAccountId() == null) ? idealMinutesStart : idealMinutes;

// count actual working days (excluding non-working days)
        long remainingWorkingDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(d -> !nonWorkingDays.contains(d))
                .count();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!nonWorkingDays.contains(date) && remainingWorkingDays > 0) {
                // recompute decrement each day so that we land exactly at 0 on the last working day
                dailyIdealDecrement = (int) Math.ceil((double) remainingIdeal / remainingWorkingDays);
                remainingIdeal -= dailyIdealDecrement;
                remainingWorkingDays--;
            }

            int dailyRecorded = dailyRecordedEffort.getOrDefault(date, 0);
            int dailyEarned = dailyEarnedEffort.getOrDefault(date, 0);

            cumulativeRecorded += dailyRecorded;
            cumulativeEarned += dailyEarned;

            int remainingRecorded = idealMinutesStart - cumulativeRecorded;
            int remainingEarned = idealMinutesStart - cumulativeEarned;

            burnDownData.put(date, new BurnDownDetails(remainingIdeal, remainingRecorded, remainingEarned));
        }

        return new BurnDownResponse(idealMinutesStart, burnDownData);
    }

    /** method calculates the data for the work items remaining in the given sprint*/
    public WorkItemRemainingResponse generateWorkItemsRemainingData(WorkItemRemainingRequest request, List<Long> accountIds, String timeZone) {
        Sprint sprint = sprintRepository.findById(request.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found."));
        Team team = teamRepository.findByTeamId(sprint.getEntityId());

        //verifying if user have access to generate the velocity chart
        if (!hasSprintViewPermission(accountIds, sprint, team.getTeamId(), Constants.EntityTypes.TEAM)) {
            throw new UnauthorizedException("Unauthorized: User does not have required permission");
        }

        List<Task> sprintTasks = new ArrayList<>();
        if (request.getAccountId() == null) {
            sprintTasks = taskRepository.findBySprintId(request.getSprintId());
        } else {
            sprintTasks = taskRepository.findByFkAccountIdAssignedAccountIdAndSprintId(request.getAccountId(), request.getSprintId());
        }

        List<WorkItemDailySummary> dailySummaries = new ArrayList<>();
        LocalDate startDate = sprint.getSprintExpStartDate().toLocalDate();
        LocalDateTime startDateTimeLocalTimeZone = DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpStartDate(), timeZone);
        LocalDate endDate = sprint.getSprintExpEndDate().toLocalDate();

        // Initial total count of tasks
        int totalTasks = sprintTasks.size();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1), startDateTimeLocalTimeZone = startDateTimeLocalTimeZone.plusDays(1)) {
            // Determine the start and end of the current day in LocalDateTime
            LocalDateTime startOfDay = (date.equals(startDate)) ? sprint.getSprintExpStartDate() : LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = (date.equals(endDate)) ? sprint.getSprintExpEndDate() : LocalDateTime.of(date, LocalTime.MAX);

            int workItemCompleted = 0;
            int workItemNotStarted = 0;

            for (Task task : sprintTasks) {
                // Work Item Completed
                if (task.getTaskActEndDate() != null && !task.getTaskActEndDate().isAfter(endOfDay)) {
                    workItemCompleted++;
                }

                // Work Item Not Started
                if (task.getTaskActStDate() == null || task.getTaskActStDate().isAfter(endOfDay)) {
                    workItemNotStarted++;
                }
            }

            int workItemRemaining = totalTasks - workItemCompleted;
            dailySummaries.add(new WorkItemDailySummary(startDateTimeLocalTimeZone.toLocalDate(), workItemRemaining, workItemNotStarted, workItemCompleted));
        }

        return new WorkItemRemainingResponse(dailySummaries);
    }

    /** method calculates the velocity chart details for the completed sprints within the given date range */
    public VelocityChartResponse generateVelocityChart(VelocityChartRequest request, List<Long> accountIds, String timeZone) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalStateException("Invalid dates in the request");
        }

        LocalDateTime startDateTime = DateTimeUtils.convertUserDateToServerTimezone(request.getStartDate().atTime(0,0,0), timeZone);
        LocalDateTime endDateTime = DateTimeUtils.convertUserDateToServerTimezone(request.getEndDate().atTime(23, 59, 59), timeZone);

        // Retrieve all sprints that might intersect with the given date range
        List<Sprint> allSprints = sprintRepository.findSprintsBetweenDates(Constants.EntityTypes.TEAM, request.getTeamId(), startDateTime, endDateTime);

        List<Sprint> fullyOverlappingSprints = new ArrayList<>();
        List<Sprint> partiallyOverlappingSprints = new ArrayList<>();

        // Identify fully and partially overlapping sprints - we will not consider partial sprints in the velocity calculation
        for (Sprint sprint : allSprints) {

            //verifying if user have access to generate the velocity chart
            if (!hasSprintViewPermission(accountIds, sprint, request.getTeamId(), Constants.EntityTypes.TEAM)) {
                continue;
            }

            if (sprint.getSprintExpStartDate().compareTo(startDateTime) >= 0 && sprint.getSprintExpEndDate().compareTo(endDateTime) <= 0) {
                fullyOverlappingSprints.add(sprint);
            } else {
                partiallyOverlappingSprints.add(sprint);
            }
        }

        String message = "";
        if (!partiallyOverlappingSprints.isEmpty()) {
            String partialSprintDetails = partiallyOverlappingSprints.stream()
                    .map(s -> "Title: " + s.getSprintTitle())
                    .collect(Collectors.joining(", "));

            message = "There are few partial sprints in this time period which we will not consider in this velocity chart: " + partialSprintDetails;
        }

        // Calculate velocity chart details for fully overlapping sprints
        List<VelocityChartDetails> velocityDetails = new ArrayList<>();
        for (Sprint sprint : fullyOverlappingSprints) {
            if (!sprint.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                continue;
            }

            List<Task> tasksInSprint = new ArrayList<>();
            if (request.getAccountId() == null) {
                tasksInSprint = taskRepository.findBySprintId(sprint.getSprintId());
            } else {
                tasksInSprint = taskRepository.findByFkAccountIdAssignedAccountIdAndSprintId(request.getAccountId(), sprint.getSprintId());
            }

            TaskSummary summary = new TaskSummary();
            for (Task task : tasksInSprint) {
                if (!task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {
                    summary.setTotalEstimate(summary.getTotalEstimate() + (task.getTaskEstimate() != null ? task.getTaskEstimate() : 0));
                    summary.setTotalEarnedTime(summary.getTotalEarnedTime() + (task.getEarnedTimeTask() != null ? task.getEarnedTimeTask() : 0));
                    summary.getTaskIds().add(task.getTaskId());
                }
            }

            List<Long> taskIdsFromHistory = new ArrayList<>();
            if (request.getAccountId() == null) {
                taskIdsFromHistory = taskHistoryRepository.findDistinctTaskIdsBySprintId(sprint.getSprintId());
            } else {
                taskIdsFromHistory = taskHistoryRepository.findDistinctTaskIdsBySprintIdAndAccountIdAssigned(sprint.getSprintId(), request.getAccountId());
            }

            // Check if there are any tasks from the task history that were earlier part of the given sprint but was moved later
            List<Long> missingTaskIds = taskIdsFromHistory.stream()
                    .filter(id -> !summary.getTaskIds().contains(id))
                    .collect(Collectors.toList());

            long totalEstimate = summary.getTotalEstimate();
            long totalEarnedTime = summary.getTotalEarnedTime();

            if (!missingTaskIds.isEmpty()) {
                // Adjust totals for the moved tasks
                for (Long taskId : missingTaskIds) {
                    TimeSheetSummary trackingSummary = new TimeSheetSummary();
                    if (request.getAccountId() == null) {
                        trackingSummary = timeSheetRepository.findEffortSummaryByTaskIdInSprint(taskId, sprint.getSprintId());
                    } else {
                        trackingSummary = timeSheetRepository.findEffortSummaryByTaskIdAndAccountIdInSprint(taskId, request.getAccountId(), sprint.getSprintId());
                    }
                    totalEstimate += trackingSummary.getTotalRecordedEffort();
                    totalEarnedTime += trackingSummary.getTotalEarnedTime();
                }
            }

            LocalDateTime sprintExpStartDateUserTimeZone = DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpStartDate(), timeZone);
            LocalDateTime sprintExpEndDateUserTimeZone = DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpEndDate(), timeZone);
            velocityDetails.add(new VelocityChartDetails(sprintExpStartDateUserTimeZone.toLocalDate(), sprintExpEndDateUserTimeZone.toLocalDate(), totalEstimate, totalEarnedTime));
        }

        return new VelocityChartResponse(message, velocityDetails);
    }


    private void updateBlockOnHoldStatusForTask(Task sprintTask, SprintStatusUpdateObject sprintStatusUpdateObject) {
        Task sprintTaskCopy = new Task();
        BeanUtils.copyProperties(sprintTask, sprintTaskCopy);
        List<String> updatedFields = new ArrayList<>();
        rollbackFromOnHoldOrBlocked(sprintTask, updatedFields);
        // Persist the changes (consider using batch save for performance)
        taskHistoryService.addTaskHistoryOnSystemUpdate(sprintTaskCopy);
        taskRepository.save(sprintTask);
        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, sprintTask);
        sprintStatusUpdateObject.setAreBlockedOnHoldPresent(true);
    }

    public SprintTaskByFilterResponse getSprintTaskByFilter (SprintTaskByFilterRequest sprintTaskByFilterRequest, String accountIds, String timeZone) throws IllegalAccessException {
        Sprint sprint = sprintRepository.findBySprintId(sprintTaskByFilterRequest.getSprintId());
        if (sprint == null) {
            throw new EntityNotFoundException("Sprint not found");
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        if (!hasSprintViewPermission(accountIdList, sprint, sprint.getEntityId(), sprint.getEntityTypeId())) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to view sprint Work Items");
        }
        SprintTaskByFilterResponse sprintTaskByFilterResponse = new SprintTaskByFilterResponse();
        Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(sprint.getEntityTypeId(), sprint.getEntityId(), accountIdList, true);
        Boolean hasTeamViewAction = accessDomainRepository.findUserRoleInEntity(Constants.EntityTypes.TEAM, sprint.getEntityId(), accountId, true, Constants.ActionId.TEAM_TASK_VIEW);
        Boolean allowedToViewTask = hasTeamViewAction || Objects.equals(accountId, sprintTaskByFilterRequest.getAccountId());

        List<Task> taskList = new ArrayList<>();
        if (allowedToViewTask) {
            String nativeQuery = getNativeQuery(sprintTaskByFilterRequest);
            Query query = entityManager.createNativeQuery(nativeQuery, Task.class);
            System.out.println(query);
            setQueryParameters(sprintTaskByFilterRequest, query);
            taskList = query.getResultList();
        }
//        List<Task> taskList = taskRepository.findByFkAccountIdAssignedAccountIdAndSprintId(sprintTaskByFilterRequest.getAccountId(), sprintTaskByFilterRequest.getSprintId());

        sprintTaskByFilterResponse = getSprintTaskResponse(taskList, sprintTaskByFilterRequest.getSprintId(), sprintTaskByFilterRequest);
        if (sprintTaskByFilterRequest.getAccountId() != null) {
            //validating if user is project manager sprint or have modify permission
            Boolean managerPermission = hasModifySprintPermission(accountIds, sprint.getEntityId(), sprint.getEntityTypeId());
            Long requestedAccountId = sprintTaskByFilterRequest.getAccountId();
            //if user don't have required permissions than setting requested accountId as of users accountId so it's always user's own capacity
            if (!managerPermission && !accountIdList.contains(requestedAccountId)) {
                requestedAccountId = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(accountIdList, sprint.getFkAccountIdCreator().getOrgId(), true).getAccountId();
            }
            sprintTaskByFilterResponse.setUserCapacityDetails(capacityService.getUserSprintCapacityDetails(requestedAccountId, sprintTaskByFilterRequest.getSprintId(), timeZone).getUserCapacityDetails());
        }
        return sprintTaskByFilterResponse;
    }

    private SprintTaskByFilterResponse getSprintTaskResponse(List<Task> sprintTaskList, Long sprintId, SprintTaskByFilterRequest sprintTaskByFilterRequest) {
        SprintTaskByFilterResponse sprintTaskByFilterResponse = new SprintTaskByFilterResponse();
        List<Task> allChildTaskList = new ArrayList<>();
        List<Long> parentTaskIdList = new ArrayList<>();

        List<SprintTaskResponse> completedTaskList = new ArrayList<>();
        List<SprintTaskResponse> notStartedTaskList = new ArrayList<>();
        List<SprintTaskResponse> startedTaskList = new ArrayList<>();
        //count of deleted tasks will not be included in task lists
        if (!sprintTaskList.isEmpty()) {
            for (Task task : sprintTaskList) {
                if (allChildTaskList.contains(task) || (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && sprintTaskByFilterRequest == null)) {
                    continue;
                }

                Task currentTask = new Task();
                BeanUtils.copyProperties(task, currentTask);
                Boolean addChildren = true;
                if ((Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && sprintTaskByFilterRequest != null)) {
                    currentTask = taskRepository.findByTaskId(task.getParentTaskId());
                }
                if (parentTaskIdList.contains(currentTask.getTaskId())) {
                    continue;
                }

                if ((Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)
                        && sprintTaskByFilterRequest != null && sprintTaskByFilterRequest.getTaskTypeId() != null
                        && Objects.equals(Constants.TaskTypes.PARENT_TASK, sprintTaskByFilterRequest.getTaskTypeId()))) addChildren = false;

                List<SprintTaskResponse> startedChildTaskList = new ArrayList<>();
                List<SprintTaskResponse> completedChildTaskList = new ArrayList<>();
                List<SprintTaskResponse> notStartedChildTaskList = new ArrayList<>();
                SprintTaskResponse sprintTaskResponse = new SprintTaskResponse();
                SprintTaskResponse sprintNotStartedParentTask = new SprintTaskResponse();
                SprintTaskResponse sprintStartedParentTask = new SprintTaskResponse();
                SprintTaskResponse sprintCompletedParentTask = new SprintTaskResponse();
                BeanUtils.copyProperties(currentTask, sprintTaskResponse);
                sprintTaskResponse.setWorkflowTaskStatus(currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                sprintTaskResponse.setAssignedTo(currentTask.getFkAccountIdAssigned() != null ? currentTask.getFkAccountIdAssigned().getEmail() : "");
                sprintTaskResponse.setTeamName(currentTask.getFkTeamId().getTeamName());
                sprintTaskResponse.setTaskId(currentTask.getTaskId());
                sprintTaskResponse.setTeamId(currentTask.getFkTeamId().getTeamId());
                //need to add changes here
                if(Objects.equals(task.getIsStarred(),true)) {
                    UserAccount user=task.getFkAccountIdStarredBy();
                    if(user != null && user.getAccountId() != null)
                    {
                        EmailFirstLastAccountIdIsActive starredByUser=userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                        sprintTaskResponse.setStarredBy(starredByUser);
                    }
                }
                sprintTaskResponse.setIsStarred(currentTask.getIsStarred());
                if (!Objects.equals(currentTask.getSprintId(), sprintId)) {
                    sprintTaskResponse.setPartOfSprint(false);
                }
                sprintTaskResponse.setSprintTitle(sprintRepository.findSprintTitleBySprintId(currentTask.getSprintId()));
                if (!sprintTaskResponse.getPartOfSprint()) {
                    sprintTaskResponse.setSprintMovementTag("Note");
                }
                if (Objects.equals(currentTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    List<Task> childTasks = new ArrayList<>();
                    Boolean showCard = true;
                    if (sprintTaskByFilterRequest != null) {
                        if(task.getFkAccountIdAssigned()!=null)
                            childTasks = taskRepository.findByParentTaskIdAndFkAccountIdAssignedAccountIdAndSprintId(currentTask.getTaskId(), task.getFkAccountIdAssigned().getAccountId(), sprintId, sprintId.toString());
                        else
                            childTasks = taskRepository.findByParentTaskIdAndFkAccountIdAssignedIsNullAccountIdAndSprintId(currentTask.getTaskId(), sprintId, sprintId.toString());
                        String nativeQuery = getNativeQueryForChildTask(sprintTaskByFilterRequest, currentTask.getTaskId());
                        Query query = entityManager.createNativeQuery(nativeQuery, Task.class);
                        setQueryParametersForChildTask(sprintTaskByFilterRequest, currentTask.getTaskId(), query);
                        childTasks = query.getResultList();
                    } else {
                        childTasks = taskRepository.findByParentTaskId(currentTask.getTaskId());
                    }
                    allChildTaskList.addAll(childTasks);
                    parentTaskIdList.add(currentTask.getTaskId());
                    for (Task childTask : childTasks) {
                        SprintTaskResponse sprintChildTaskResponse = new SprintTaskResponse();
                        BeanUtils.copyProperties(childTask, sprintChildTaskResponse);
                        sprintChildTaskResponse.setWorkflowTaskStatus(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                        sprintChildTaskResponse.setAssignedTo(childTask.getFkAccountIdAssigned() != null ? childTask.getFkAccountIdAssigned().getEmail() : "");
                        sprintChildTaskResponse.setTeamName(childTask.getFkTeamId().getTeamName());
                        sprintChildTaskResponse.setTaskId(childTask.getTaskId());
                        sprintChildTaskResponse.setTeamId(childTask.getFkTeamId().getTeamId());
                        sprintChildTaskResponse.setSprintTitle(sprintRepository.findSprintTitleBySprintId(childTask.getSprintId()));
                        if (!Objects.equals(childTask.getSprintId(), sprintId)) {
                            sprintChildTaskResponse.setPartOfSprint(false);
                        }
                        if (sprintTaskByFilterRequest == null || sprintTaskByFilterRequest.getTaskPriority() == null || Objects.equals(task.getTaskPriority(), childTask.getTaskPriority())) {
                            if (childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                                completedChildTaskList.add(sprintChildTaskResponse);
                                if (sprintChildTaskResponse.getPartOfSprint()) {
                                    sprintTaskByFilterResponse.incrementTotalCompeletedTask();
                                }
                            } else if (childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE) || ((childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) || childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) && childTask.getTaskActStDate() != null)) {
                                startedChildTaskList.add(sprintChildTaskResponse);
                                if (sprintChildTaskResponse.getPartOfSprint() && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                                    sprintTaskByFilterResponse.incrementTotalStartedTask();
                                }
                            } else {
                                notStartedChildTaskList.add(sprintChildTaskResponse);
                                if (sprintChildTaskResponse.getPartOfSprint() && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                                    sprintTaskByFilterResponse.incrementTotalNotStartedTask();
                                }
                            }
                        }
                        if (!sprintChildTaskResponse.getPartOfSprint() && Objects.equals(sprintChildTaskResponse.getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                            sprintChildTaskResponse.setSprintMovementTag("Completed " + (sprintChildTaskResponse.getSprintTitle() != null ? "in sprint '" + sprintChildTaskResponse.getSprintTitle() + "'": "before getting added to sprint"));
                        }
                    }
                    if (sprintTaskByFilterRequest != null && !showCardOnScreen(sprintTaskByFilterRequest, currentTask)) {
                        showCard = false;
                    }
                    if (!notStartedChildTaskList.isEmpty() || currentTask.getTaskActStDate() == null) {
                        BeanUtils.copyProperties(sprintTaskResponse, sprintNotStartedParentTask);
                        if (addChildren) sprintNotStartedParentTask.setChildTaskList(notStartedChildTaskList);
                        if (currentTask.getTaskActStDate() != null) {
                            sprintNotStartedParentTask.setShowCard(false);
                        }
                        else {
                            sprintNotStartedParentTask.setShowCard(showCard);
                        }
                        notStartedTaskList.add(sprintNotStartedParentTask);
                    }
                    if (!startedChildTaskList.isEmpty() || (currentTask.getTaskActStDate() != null && currentTask.getTaskActEndDate() == null && showCard)) {
                        BeanUtils.copyProperties(sprintTaskResponse, sprintStartedParentTask);
                        if (addChildren) sprintStartedParentTask.setChildTaskList(startedChildTaskList);
                        if (currentTask.getTaskActStDate() != null && currentTask.getTaskActEndDate() == null) {
                            sprintStartedParentTask.setShowCard(showCard);
                        }
                        else {
                            sprintStartedParentTask.setShowCard(false);
                        }
                        startedTaskList.add(sprintStartedParentTask);
                    }
                    if (!completedChildTaskList.isEmpty() || currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                        BeanUtils.copyProperties(sprintTaskResponse, sprintCompletedParentTask);
                        if (addChildren) sprintCompletedParentTask.setChildTaskList(completedChildTaskList);
                        if (currentTask.getTaskActEndDate() == null) {
                            sprintCompletedParentTask.setShowCard(false);
                        }
                        else {
                            sprintCompletedParentTask.setShowCard(showCard);
                        }
                        completedTaskList.add(sprintCompletedParentTask);
                    }

                }
                if (!Objects.equals(currentTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    if (currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                        completedTaskList.add(sprintTaskResponse);
                        if (sprintTaskResponse.getPartOfSprint()) {
                            sprintTaskByFilterResponse.incrementTotalCompeletedTask();
                        }
                    } else if (currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE) || ((currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) || currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) && currentTask.getTaskActStDate() != null)) {
                        startedTaskList.add(sprintTaskResponse);
                        if (sprintTaskResponse.getPartOfSprint() && !currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                            sprintTaskByFilterResponse.incrementTotalStartedTask();
                        }
                    } else {
                        notStartedTaskList.add(sprintTaskResponse);
                        if (sprintTaskResponse.getPartOfSprint() && !currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                            sprintTaskByFilterResponse.incrementTotalNotStartedTask();
                        }
                    }
                }
            }
        }
        sprintTaskByFilterResponse.setSprintCompletedTaskList(completedTaskList);
        sprintTaskByFilterResponse.setSprintStartedTaskList(startedTaskList);
        sprintTaskByFilterResponse.setSprintNotStartedTaskList(notStartedTaskList);
        return sprintTaskByFilterResponse;
    }

    public String getNativeQuery (SprintTaskByFilterRequest sprintTaskByFilterRequest) {
        String nativeQuery = "SELECT * FROM tse.task WHERE (sprint_id = :sprintId OR :sprintIdString = ANY(STRING_TO_ARRAY(prev_sprints, ','))) ";
        if (sprintTaskByFilterRequest.getAccountId() != null) {
            nativeQuery += "AND account_id_assigned = :accountId ";
        }
        if (sprintTaskByFilterRequest.getTaskTypeId() != null) {
            nativeQuery += "AND task_type_id = :taskTypeId ";
        }
        if (sprintTaskByFilterRequest.getTaskPriority() != null) {
            nativeQuery += "AND task_priority = :taskPriority ";
        }
        if (sprintTaskByFilterRequest.getLabelIds() != null && !sprintTaskByFilterRequest.getLabelIds().isEmpty()) {
            nativeQuery += "AND task_id IN (SELECT task_id FROM tse.task_label WHERE label_id IN (:labelIds)) ";
        }
        if(sprintTaskByFilterRequest.getIsStarred()!= null && sprintTaskByFilterRequest.getIsStarred()){
            nativeQuery += "AND is_starred = true ";
            if (sprintTaskByFilterRequest.getStarredBy() != null && !sprintTaskByFilterRequest.getStarredBy().isEmpty()) {
                nativeQuery += " AND account_id_starred_by IN (:starredBy)";
            }
        }
        return nativeQuery;
    }

    public void setQueryParameters (SprintTaskByFilterRequest sprintTaskByFilterRequest, Query query) {
        query.setParameter("sprintId", sprintTaskByFilterRequest.getSprintId());
        query.setParameter("sprintIdString", sprintTaskByFilterRequest.getSprintId().toString());

        if (sprintTaskByFilterRequest.getAccountId() != null) {
            query.setParameter("accountId", sprintTaskByFilterRequest.getAccountId());
        }
        if (sprintTaskByFilterRequest.getTaskTypeId() != null) {
            query.setParameter("taskTypeId", sprintTaskByFilterRequest.getTaskTypeId());
        }
        if (sprintTaskByFilterRequest.getTaskPriority() != null) {
            query.setParameter("taskPriority", sprintTaskByFilterRequest.getTaskPriority());
        }
        if (sprintTaskByFilterRequest.getLabelIds() != null && !sprintTaskByFilterRequest.getLabelIds().isEmpty()) {
            query.setParameter("labelIds", sprintTaskByFilterRequest.getLabelIds());
        }
        if (Boolean.TRUE.equals(sprintTaskByFilterRequest.getIsStarred())
                && sprintTaskByFilterRequest.getStarredBy() != null
                && !sprintTaskByFilterRequest.getStarredBy().isEmpty()) {
            query.setParameter("starredBy", sprintTaskByFilterRequest.getStarredBy());
        }
    }

    public String getNativeQueryForChildTask (SprintTaskByFilterRequest sprintTaskByFilterRequest, Long parentTaskId) {
        String nativeQuery = "SELECT * FROM tse.task WHERE (parent_task_id = :parentTaskId) ";
        if (sprintTaskByFilterRequest.getAccountId() != null) {
            nativeQuery += "AND account_id_assigned = :accountId ";
        }
        if (sprintTaskByFilterRequest.getTaskTypeId() != null) {
            nativeQuery += "AND task_type_id = :taskTypeId ";
        }
        if (sprintTaskByFilterRequest.getTaskPriority() != null) {
            nativeQuery += "AND task_priority = :taskPriority ";
        }
        if (sprintTaskByFilterRequest.getLabelIds() != null && !sprintTaskByFilterRequest.getLabelIds().isEmpty()) {
            nativeQuery += "AND task_id IN (SELECT task_id FROM tse.task_label WHERE label_id IN (:labelIds)) ";
        }
        if(sprintTaskByFilterRequest.getIsStarred()!= null && sprintTaskByFilterRequest.getIsStarred()){
            nativeQuery += "AND is_starred = true ";
            if (sprintTaskByFilterRequest.getStarredBy() != null && !sprintTaskByFilterRequest.getStarredBy().isEmpty()) {
                nativeQuery += " AND account_id_starred_by IN (:starredBy)";
            }
        }
        return nativeQuery;
    }

    public void setQueryParametersForChildTask (SprintTaskByFilterRequest sprintTaskByFilterRequest, Long parentTaskId, Query query) {
        query.setParameter("parentTaskId", parentTaskId);

        if (sprintTaskByFilterRequest.getAccountId() != null) {
            query.setParameter("accountId", sprintTaskByFilterRequest.getAccountId());
        }
        if (sprintTaskByFilterRequest.getTaskTypeId() != null) {
            query.setParameter("taskTypeId", sprintTaskByFilterRequest.getTaskTypeId());
        }
        if (sprintTaskByFilterRequest.getTaskPriority() != null) {
            query.setParameter("taskPriority", sprintTaskByFilterRequest.getTaskPriority());
        }
        if (sprintTaskByFilterRequest.getLabelIds() != null && !sprintTaskByFilterRequest.getLabelIds().isEmpty()) {
            query.setParameter("labelIds", sprintTaskByFilterRequest.getLabelIds());
        }
        if (Boolean.TRUE.equals(sprintTaskByFilterRequest.getIsStarred())
                && sprintTaskByFilterRequest.getStarredBy() != null
                && !sprintTaskByFilterRequest.getStarredBy().isEmpty()) {
            query.setParameter("starredBy", sprintTaskByFilterRequest.getStarredBy());
        }
    }

    public Boolean addSprintDatesToTask(Task task, Sprint sprint, Boolean isTaskInActiveSprintState, Task parentTask) throws ValidationFailedException {
        Boolean datesAdjusted = false;
        Epic epic = null;
        if (task.getFkEpicId() != null) {
            epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
        }
        if (task.getTaskExpEndDate() == null || task.getTaskExpEndDate().isBefore(sprint.getSprintExpStartDate()) || task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())) {
            if (task.getTaskActStDate() == null) {
                if (parentTask != null && parentTask.getTaskActStDate() == null && parentTask.getTaskExpStartDate() != null && !parentTask.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) && !parentTask.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())) {
                    task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                    task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                } else {
                    if(task.getTaskExpStartDate()!=null && task.getTaskExpStartDate().isAfter(sprint.getSprintExpStartDate()) && task.getTaskExpStartDate().isBefore(sprint.getSprintExpEndDate()) &&
                            (task.getTaskEstimate() != null && (Duration.between(task.getTaskExpStartDate(), sprint.getSprintExpEndDate()).toMinutes() < task.getTaskEstimate()))){
                        task.setTaskExpStartDate(task.getTaskExpStartDate());
                        task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                    } else{
                        task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                        task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
                    }
                }
            }
            if (parentTask != null && parentTask.getTaskExpEndDate() != null && !parentTask.getTaskExpEndDate().isBefore(sprint.getSprintExpStartDate()) && !parentTask.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())) {
                task.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                task.setTaskExpEndTime(task.getTaskExpEndDate().toLocalTime());
            } else {
                task.setTaskExpEndDate(sprint.getSprintExpEndDate());
                task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
            }
            datesAdjusted = true;
        }

        if ((task.getTaskActStDate() == null && (task.getTaskExpStartDate() == null || task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || task.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())))
                || (task.getTaskActStDate() != null && !task.getTaskExpStartDate().isBefore(sprint.getSprintExpEndDate()))) {
            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                if (parentTask.getTaskActStDate() == null) {
                    if (parentTask.getTaskExpStartDate() == null || parentTask.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || parentTask.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())) {
                        addSprintExpDatesToTask(task, sprint);
                    }
                    else {
                        task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                        task.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                    }
                    datesAdjusted = true;
                }
                else if (parentTask.getTaskActStDate().isBefore(sprint.getSprintExpEndDate())) {
                    if (task.getTaskActStDate() == null) {
                        if (sprint.getSprintExpStartDate().isAfter(parentTask.getTaskExpStartDate())) {
                            addSprintExpDatesToTask(task, sprint);
                        }
                        else
                            parentTask.getTaskExpStartDate();
                        task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                        datesAdjusted = true;
                    }
                    else if (task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate())) ;
                    else
                        throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
                }
                else
                    throw new ValidationFailedException("For a Child Task with a Started Parent Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
            }
            else if (task.getTaskActStDate() == null) {
                addSprintExpDatesToTask(task, sprint);
                datesAdjusted = true;
            }
            else if (task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate())) ;
            else
                throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
        }
        task.setIsSprintChanged(true);
        if (epic != null) {
            if ((epic.getExpEndDateTime() != null && task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                throw new ForbiddenException("Work item " + task.getTaskNumber() + " is part of an Epic and it's Expected End Date and Time can't lie in a Sprint and an Epic simultaneously.");
            }
            if ((epic.getExpEndDateTime() != null && task.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                throw new ValidationFailedException("Work item " + task.getTaskNumber() + " is part of an Epic and it's Expected Start Date and Time can't lie in a Sprint and an Epic simultaneously.");
            }
        }
        if (task.getTaskId() != null && datesAdjusted) {
            //need to reschedule the reference meeting to sprint started dates.
            taskServiceImpl.updateMeetingDateOnTaskMovementBetweenSprint(task);
//            throw new ValidationFailedException("Reference meeting date range is outside of Expected date range of work item : " + task.getTaskNumber());
        }
        return datesAdjusted;
    }

    public void addSprintExpDatesToTask(Task task, Sprint sprint) {
        if (task.getTaskExpStartDate() != null && task.getTaskExpEndDate() == null) {
            if (task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) ||
                    (task.getTaskEstimate() != null && (Duration.between(task.getTaskExpStartDate(), sprint.getSprintExpEndDate()).toMinutes() > task.getTaskEstimate()))) {
                task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
            }
            task.setTaskExpEndDate(sprint.getSprintExpEndDate());
            task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
        } else if (task.getTaskExpStartDate() == null && task.getTaskExpEndDate() != null) {
            if (task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate()) ||
                    (task.getTaskEstimate() != null && (Duration.between(sprint.getSprintExpEndDate(), task.getTaskExpEndDate()).toMinutes() > task.getTaskEstimate()))) {
                task.setTaskExpEndDate(sprint.getSprintExpEndDate());
                task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
            }
            task.setTaskExpStartDate(sprint.getSprintExpStartDate());
            task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
        } else if (task.getTaskExpStartDate() != null && task.getTaskExpEndDate() != null) {
        } else {
            task.setTaskExpStartDate(sprint.getSprintExpStartDate());
            task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
            task.setTaskExpEndDate(sprint.getSprintExpEndDate());
            task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
        }
    }

    public SprintTasksWithoutEstimateResponse getSprintTasksWithoutEstimates (SprintTasksWithoutEstimateRequest request, String accountIds, String timeZone) throws IllegalAccessException {
        Sprint sprint = sprintRepository.findBySprintId(request.getSprintId());
        SprintTasksWithoutEstimateResponse response = new SprintTasksWithoutEstimateResponse();
        if (sprint == null) {
            throw new EntityNotFoundException("Sprint not found");
        }
        Boolean userFilter = false;
        if (!hasModifySprintPermission(accountIds, sprint.getEntityId(), sprint.getEntityTypeId()) && !(request.getAccountId() != null && Objects.equals(request.getAccountId(), Long.parseLong(accountIds)))) {
            throw new IllegalAccessException("Unauthorized: User does not have permission to view Work Items without estimates");
        }
        List<Task> taskList = Optional.ofNullable(request.getAccountId())
                .map(accountId -> {
                    if (Objects.equals(accountId, Constants.UNASSIGNED_ACCOUNT_ID)) {
                        return taskRepository.findBySprintIdAndFkAccountIdAssignedAccountIdIsNullAndTaskEstimateIsNull(request.getSprintId());
                    } else {
                        return taskRepository.findByFkAccountIdAssignedAccountIdAndSprintIdAndTaskEstimateIsNull(accountId, request.getSprintId());
                    }
                })
                .orElseGet(() -> taskRepository.findBySprintIdAndTaskEstimateIsNull(request.getSprintId()));

//        List<SprintTaskResponse> sprintTasksWithoudtEstimate = getSprintTaskResponse(taskList, request.getSprintId(), null).getSprintNotStartedTaskList();
        List<SprintTaskResponse> sprintTasksWithoutEstimate = getSprintUnEstimatedTaskResponse (taskList, timeZone);
        response.setTaskList(sprintTasksWithoutEstimate);
        return response;
    }

    List<SprintTaskResponse> getSprintUnEstimatedTaskResponse (List<Task> taskList, String timeZone) {
        List<SprintTaskResponse> sprintTaskResponseList = new ArrayList<>();
        if (taskList != null && !taskList.isEmpty()) {
            for (Task currentTask : taskList) {
                SprintTaskResponse sprintTaskResponse = new SprintTaskResponse();
                BeanUtils.copyProperties(currentTask, sprintTaskResponse);

                convertSprintTaskResponseServerDateToUserTimeZone (sprintTaskResponse, timeZone);

                sprintTaskResponse.setWorkflowTaskStatus(currentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                sprintTaskResponse.setAssignedTo(currentTask.getFkAccountIdAssigned() != null ? currentTask.getFkAccountIdAssigned().getEmail() : "");
                sprintTaskResponse.setTeamName(currentTask.getFkTeamId().getTeamName());
                sprintTaskResponse.setTaskId(currentTask.getTaskId());
                sprintTaskResponse.setTeamId(currentTask.getFkTeamId().getTeamId());
                sprintTaskResponseList.add(sprintTaskResponse);
            }
        }
        return sprintTaskResponseList;
    }

    private void convertSprintTaskResponseServerDateToUserTimeZone (SprintTaskResponse sprintTaskResponse, String timeZone) {
        if (sprintTaskResponse.getTaskExpStartDate() != null) {
            sprintTaskResponse.setTaskExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprintTaskResponse.getTaskExpStartDate(), timeZone));
        }
        if (sprintTaskResponse.getTaskExpEndDate() != null) {
            sprintTaskResponse.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprintTaskResponse.getTaskExpEndDate(), timeZone));
        }
        if (sprintTaskResponse.getTaskActStDate() != null) {
            sprintTaskResponse.setTaskActStDate(DateTimeUtils.convertServerDateToUserTimezone(sprintTaskResponse.getTaskActStDate(), timeZone));
        }
        if (sprintTaskResponse.getTaskActEndDate() != null) {
            sprintTaskResponse.setTaskActEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprintTaskResponse.getTaskActEndDate(), timeZone));
        }
        if (sprintTaskResponse.getCreatedDateTime() != null) {
            sprintTaskResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(sprintTaskResponse.getCreatedDateTime(), timeZone));
        }
        if (sprintTaskResponse.getLastUpdatedDateTime() != null) {
            sprintTaskResponse.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(sprintTaskResponse.getLastUpdatedDateTime(), timeZone));
        }
    }

    private List<Task> getSprintTask (Sprint sprint, Long accountId) {
        List<Task> sprintTaskList = new ArrayList<>();
        Boolean hasTeamViewAction = accessDomainRepository.findUserRoleInEntity(Constants.EntityTypes.TEAM, sprint.getEntityId(), accountId, true, Constants.ActionId.TEAM_TASK_VIEW);
        if (hasTeamViewAction) {
            sprintTaskList = taskRepository.findTasksBySprintIdExcludingChildTasksWithSameSprint(sprint.getSprintId(), sprint.getSprintId().toString());
        } else {
            SprintTaskByFilterRequest sprintTaskByFilterRequest = new SprintTaskByFilterRequest();
            sprintTaskByFilterRequest.setAccountId(accountId);
            sprintTaskByFilterRequest.setSprintId(sprint.getSprintId());
            String nativeQuery = getNativeQuery(sprintTaskByFilterRequest);
            Query query = entityManager.createNativeQuery(nativeQuery, Task.class);
            setQueryParameters(sprintTaskByFilterRequest, query);
            sprintTaskList = query.getResultList();
        }
        return sprintTaskList;
    }

    public SprintInfoWithSprintStatusResponse getAllSprintInfoForEntity(String accountIds, Integer entityTypeId, Long entityId, String timeZone) throws IllegalAccessException {
        SprintInfoWithSprintStatusResponse allSprintResponse = new SprintInfoWithSprintStatusResponse();
        List<Long> teamIdList = new ArrayList<>();
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            teamIdList.addAll(teamRepository.findTeamIdsByOrgId(entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            teamIdList.addAll(teamRepository.findTeamIdsByProjectId(entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            teamIdList.add(entityId);
        } else {
            throw new ValidationException("Entity type provided is not valid");
        }
        List<SprintWithTeamCode> dbSprintList = sprintRepository.findSprintWithTeamCodeByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIdList);
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);

        if (!dbSprintList.isEmpty()) {

            //adding sprints to response
            for (SprintWithTeamCode sprintWithTeamCode : dbSprintList) {
                Sprint sprint = new Sprint();
                BeanUtils.copyProperties(sprintWithTeamCode.getSprint(), sprint);
                if (!hasSprintViewPermission(accountIdList, sprint, entityId, entityTypeId)) {
                    continue;
                }
                SprintInfo sprintResponse = new SprintInfo();
                convertAllSprintDateToUserTimeZone(sprint, timeZone);
                BeanUtils.copyProperties(sprint, sprintResponse);
                sprintResponse.setTeamCode(sprintWithTeamCode.getTeamCode());

                //filtering sprints on tha basis of their status
                if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                    allSprintResponse.getCompletedSprintList().add(sprintResponse);
                } else if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                    allSprintResponse.getActiveSprintList().add(sprintResponse);
                } else {
                    allSprintResponse.getPlannedSprintList().add(sprintResponse);
                }
            }
            if (allSprintResponse.getCompletedSprintList() != null) allSprintResponse.getCompletedSprintList().sort(Comparator.comparing(SprintInfo::getSprintExpEndDate, Comparator.reverseOrder()));
            if (allSprintResponse.getActiveSprintList() != null) allSprintResponse.getActiveSprintList().sort(Comparator.comparing(SprintInfo::getSprintExpStartDate, Comparator.reverseOrder()));
            if (allSprintResponse.getPlannedSprintList() != null) allSprintResponse.getPlannedSprintList().sort(Comparator.comparing(SprintInfo::getSprintExpStartDate, Comparator.reverseOrder()));
        }
        return allSprintResponse;
    }

    public List<SprintInfo> getTitleAndExpDatesForListing(String accountIds, Integer entityTypeId, Long entityId, String timeZone) throws IllegalAccessException {

        List<SprintInfo> sprintResponse = new ArrayList<>();
        List<Team> teamList = new ArrayList<>();
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            Optional<Team> team = Optional.ofNullable(teamRepository.findByTeamId(entityId));
            if (team.isPresent()) {
                teamList.add(team.get());
            }
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            teamList = teamRepository.findByFkProjectIdProjectId(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            teamList = teamRepository.findByFkOrgIdOrgId(entityId);
        } else {
            List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
            List<Long> orgIdList = userAccountRepository.getAllOrgIdByAccountIdInAndIsActive(accountIdList, true);
            if (orgIdList.contains(Long.valueOf(Constants.OrgIds.PERSONAL))) {
                orgIdList.remove(Long.valueOf(Constants.OrgIds.PERSONAL));
            }
            teamList = teamRepository.findByFkOrgIdOrgIdIn(orgIdList);
        }
        for (Team team : teamList) {
            List<Sprint> sprintDbResponse = sprintRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, team.getTeamId());
            sprintDbResponse.addAll(sprintRepository.findDeletedSprintByEntityTypeIdAndEntityId(entityTypeId, entityId));
            for (Sprint sprint : sprintDbResponse) {
                if (!hasSprintViewPermission(CommonUtils.convertToLongList(accountIds), sprint, entityId, entityTypeId)) {
                    continue;
                }
                SprintInfo sprintTemp = new SprintInfo();
                sprintTemp.setSprintId(sprint.getSprintId());
                sprintTemp.setSprintTitle(sprint.getSprintTitle());
                sprintTemp.setSprintExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpStartDate(), timeZone));
                sprintTemp.setSprintExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(sprint.getSprintExpEndDate(), timeZone));
                sprintTemp.setSprintStatus(sprint.getSprintStatus());
                sprintTemp.setTeamCode(team.getTeamCode());
                sprintResponse.add(sprintTemp);
            }
        }
        return sprintResponse;
    }

    public void rollbackFromOnHoldOrBlocked(Task sprintTask, List<String> updatedFields) {
        if (sprintTask.getTaskActStDate() != null) {
            WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE, sprintTask.getTaskWorkflowId());
            sprintTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
            sprintTask.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
            updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
            taskService.computeAndUpdateStatForTask(sprintTask, true);
            updatedFields.add(Constants.TaskFields.TASK_STATE);
        } else {
            WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, sprintTask.getTaskWorkflowId());
            sprintTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
            sprintTask.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
            updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
            taskService.computeAndUpdateStatForTask(sprintTask, true);
            updatedFields.add(Constants.TaskFields.TASK_STATE);
        }
    }


    public boolean isTaskDeletedOrCompleted(Task task) {
        String taskStatus = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
        return taskStatus.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)
                || taskStatus.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE);
    }

    public void adjustSprintHours(Task childTask, Sprint sprint, Sprint currentSprint) {
        if (childTask.getTaskEstimate() != null) {
            List<Sprint> sprintList = new ArrayList<>();

            if (currentSprint != null) {
                currentSprint.setHoursOfSprint(
                        Math.max(0, (currentSprint.getHoursOfSprint() != null ? currentSprint.getHoursOfSprint() : 0) - childTask.getTaskEstimate()));
                sprintList.add(currentSprint);
            }

            if (sprint != null && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                sprint.setHoursOfSprint(
                        (sprint.getHoursOfSprint() != null ? sprint.getHoursOfSprint() : 0) + childTask.getTaskEstimate());
                sprintList.add(sprint);
            }

            if (!sprintList.isEmpty()) {
                sprintRepository.saveAll(sprintList);
            }
        }
    }

    public void handleSprintCapacity(Task childTask, Sprint currentSprint, LocalTime sprintCompleteTimeLimit) {
        if (currentSprint != null && !isSprintCompleted(currentSprint, sprintCompleteTimeLimit)) {
            capacityService.removeTaskFromSprintCapacityAdjustment(childTask);
            capacityService.updateMovedCapacity(childTask);
        } else if (currentSprint != null && Objects.equals(currentSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
            Set<Long> prevSprints = new HashSet<>(childTask.getPrevSprints());
            prevSprints.add(currentSprint.getSprintId());
            childTask.setPrevSprints(new ArrayList<>(prevSprints));
        }
    }

    public boolean isSprintCompleted(Sprint sprint, LocalTime sprintCompleteTimeLimit) {
        LocalDate currentDate = LocalDate.now();
        return currentDate.isAfter(sprint.getSprintExpEndDate().toLocalDate())
                || (currentDate.isEqual(sprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit));
    }

    public void updateTaskSprintsAndWorkflow(Task childTask, Sprint sprint, Task task, Sprint currentSprint, LocalTime sprintCompleteTimeLimit, List<String> updatedFields) {
        if (sprint != null) {
            String validationMessage = validateSprintConditionAndModifyTaskProperties(childTask, sprint, task);
            if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                validateRequiredFields(childTask);
                WorkFlowTaskStatus notStartedStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(
                        Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, childTask.getTaskWorkflowId());
                childTask.setFkWorkflowTaskStatus(notStartedStatus);
                childTask.setTaskState(notStartedStatus.getWorkflowTaskState());
                updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
            }
            childTask.setSprintId(sprint.getSprintId());
            updatedFields.add(Constants.TaskFields.SPRINT_ID);
            capacityService.updateUserAndSprintCapacityMetricsOnAddTaskToSprint(childTask, sprint.getSprintId());
        } else {
            childTask.setSprintId(null);
            updatedFields.add(Constants.TaskFields.SPRINT_ID);
        }
    }

    public void validateRequiredFields(Task task) {
        StringBuilder errorMessage = new StringBuilder("Following fields are missing in Work Item " + task.getTaskNumber() + ":");
        boolean missingFields = false;

        if (task.getTaskPriority() == null) {
            errorMessage.append(" priority");
            missingFields = true;
        }
        if (task.getFkAccountIdAssigned() == null) {
            if (missingFields) errorMessage.append(", ");
            errorMessage.append(" assigned to");
            missingFields = true;
        }
        if (task.getTaskEstimate() == null) {
            if (missingFields) errorMessage.append(", ");
            errorMessage.append(" estimate");
            missingFields = true;
        }

        if (missingFields) {
            throw new IllegalStateException(errorMessage.toString());
        }
    }

    public boolean shouldUpdateTaskProgress(Task task, Sprint sprint) {
        return (sprint != null
                && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()))
                || (task.getTaskProgressSystem() != null
                && (task.getTaskProgressSystem().equals(StatType.DELAYED)
                || (task.getTaskProgressSystem().equals(StatType.WATCHLIST)
                && !Constants.PRIORITY_P0.equals(task.getTaskPriority())
                && !Constants.PRIORITY_P1.equals(task.getTaskPriority()))));
    }

    public RemoveSprintMemberResponse removeMemberFromSprint (RemoveSprintMemberRequest removeSprintMemberRequest, List<Long> accountIdList, Boolean memberRemovedWithinSprint) throws IllegalAccessException {
        RemoveSprintMemberResponse removeSprintMemberResponse = new RemoveSprintMemberResponse();
        List<Integer> authorizedRoleList = new ArrayList<>();
        authorizedRoleList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
        authorizedRoleList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        Sprint sprintDb = sprintRepository.findBySprintId(removeSprintMemberRequest.getSprintId());

        if (sprintDb == null) {
            throw new IllegalAccessException("Sprint not found");
        }
        if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) || Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
            throw new ValidationFailedException("Member can't be removed from started or completed sprint");
        }
        Boolean isSprintStarted = false;
        if (Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
            isSprintStarted = true;
        }
        Set<EmailFirstLastAccountId> sprintMemberList = sprintDb.getSprintMembers();
        if (sprintMemberList == null) {
            sprintMemberList = new HashSet<>();
        }
        List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                .map(EmailFirstLastAccountId::getAccountId)
                .collect(Collectors.toList());
        if (memberRemovedWithinSprint && !CommonUtils.containsAny(accountIdList, sprintMemberAccountIdList)) {
                throw new IllegalAccessException("Unauthorized: You are not part of sprint so you can't remove member from sprint");
        }
        if (memberRemovedWithinSprint && !accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, sprintDb.getEntityId(), accountIdList, true, authorizedRoleList)) {
            throw new IllegalAccessException("Unauthorized: You does not have permission to remove member from sprint");
        }
        if (!sprintMemberAccountIdList.contains(removeSprintMemberRequest.getRemovedMemberAccountId())) {
            if (memberRemovedWithinSprint) {
                throw new IllegalAccessException("Member is already not part of sprint so they can't be removed");
            }
            else {
                return removeSprintMemberResponse;
            }
        }

        List<TaskIdAssignedTo> taskIdAssignedToList = removeSprintMemberRequest.getTaskIdAssignedToList();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();

        if (taskIdAssignedToList != null) {
            for (TaskIdAssignedTo taskIdAssignedTo : taskIdAssignedToList) {
                Task task = taskRepository.findByTaskId(taskIdAssignedTo.getTaskId());
                if (task == null || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                try {
                    UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(taskIdAssignedTo.getAccountIdAssignedTo(), true);
                    if (taskIdAssignedTo.getAccountIdAssignedTo() != null && userAccount != null && !sprintMemberAccountIdList.contains(taskIdAssignedTo.getAccountIdAssignedTo())) {
                        throw new IllegalAccessException(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName() + " is not part of sprint");
                    }
                    if (taskIdAssignedTo.getAccountIdAssignedTo() != null && userAccount == null) {
                        throw new IllegalAccessException("Selected Assign To doesn't exist");
                    }
                    if (Objects.equals(removeSprintMemberRequest.getRemovedMemberAccountId(), taskIdAssignedTo.getAccountIdAssignedTo())) {
                        throw new ValidationFailedException("Work Item should be assign to someone else");
                    }
                    if (isSprintStarted && taskIdAssignedTo.getAccountIdAssignedTo() == null) {
                        throw new ValidationFailedException("In Started Sprint, Assign To field of Work Item can't be null");
                    }
                    changeAssignedToAndRecalculateCapacity(task, taskIdAssignedTo.getAccountIdAssignedTo());
                    successList.add(new TaskForBulkResponse(task.getTaskId(), task.getTaskNumber(), task.getTaskTitle(), task.getFkTeamId().getTeamId(), "Assign to of Work Item is changed successfully"));
                } catch (Exception e) {
                    failureList.add(new TaskForBulkResponse(task.getTaskId(), task.getTaskNumber(), task.getTaskTitle(), task.getFkTeamId().getTeamId(), e.getMessage()));
                }
            }
        }
        removeSprintMemberResponse.setSprintId(sprintDb.getSprintId());
        removeSprintMemberResponse.setSuccessList(successList);
        if (!failureList.isEmpty()) {
            removeSprintMemberResponse.setFailureList(failureList);
            removeSprintMemberResponse.setMessage("Following Work Item is not able to reassigned. Please reassign it manually");
            return removeSprintMemberResponse;
        }
        capacityService.updateCapacityAndRemoveMember(removeSprintMemberRequest.getSprintId(), removeSprintMemberRequest.getRemovedMemberAccountId());
        removeSprintMemberResponse.setMessage("Member is removed successfully");
        sprintMemberList.removeIf(member -> member.getAccountId().equals(removeSprintMemberRequest.getRemovedMemberAccountId()));
        sprintDb.setSprintMembers(sprintMemberList);
        sprintRepository.save(sprintDb);
        return removeSprintMemberResponse;
    }

    // ZZZZZZ 14-04-2025
    public AddedSprintMemberResponse addMemberInSprint (AddedSprintMemberRequest addedSprintMemberRequest, List<Long> accountIdList, Boolean memberAddedWithinSprint, String timeZone) throws IllegalAccessException {
        AddedSprintMemberResponse addedSprintMemberResponse = new AddedSprintMemberResponse();
        List<Integer> authorizedRoleList = new ArrayList<>();
        authorizedRoleList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
        authorizedRoleList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        Sprint sprintDb = sprintRepository.findBySprintId(addedSprintMemberRequest.getSprintId());
        addedSprintMemberResponse.setMessage("Member is added successfully");
        if (sprintDb == null) {
            throw new IllegalAccessException("Sprint not found");
        }
        if (memberAddedWithinSprint && Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) || Objects.equals(sprintDb.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
            throw new ValidationFailedException("Member can not be added in started or completed sprint");
        }

        Set<EmailFirstLastAccountId> sprintMemberList = sprintDb.getSprintMembers();
        if (sprintMemberList == null) {
            sprintMemberList = new HashSet<>();
        }
        List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                .map(EmailFirstLastAccountId::getAccountId)
                .collect(Collectors.toList());
        if (memberAddedWithinSprint && !CommonUtils.containsAny(accountIdList, sprintMemberAccountIdList)) {
            throw new IllegalAccessException("Unauthorized: You are not part of sprint so you can't add member in sprint");
        }
        if (memberAddedWithinSprint && !accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, sprintDb.getEntityId(), accountIdList, true, authorizedRoleList)) {
            throw new IllegalAccessException("Unauthorized: You does not have permission to add member in sprint");
        }

        List<Long> addMemberAccountIdList = addedSprintMemberRequest.getAddedMemberAccountIds();
        List<AccountDetailsForBulkResponse> successList = new ArrayList<>();
        List<AccountDetailsForBulkResponse> failureList = new ArrayList<>();
        List<Long> accountIdListForAddMember = new ArrayList<>();
        for (Long memberAccountId : addMemberAccountIdList) {
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(memberAccountId, true);
            try {
                if (sprintMemberAccountIdList.contains(memberAccountId)) {
                    if (memberAddedWithinSprint) {
                        throw new IllegalAccessException("Member is already part of sprint");
                    }
                    else {
                        continue;
                    }
                }
                if (memberAddedWithinSprint && userAccount == null) {
                    throw new IllegalAccessException("Account Id doesn't exist");
                }
                if (memberAddedWithinSprint && !accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, sprintDb.getEntityId(), memberAccountId, true, Constants.TEAM_NON_ADMIN_ROLE)) {
                    throw new IllegalStateException(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName() + " doesn't present in the team or doesn't have admin role");
                }
                accountIdListForAddMember.add(memberAccountId);
                successList.add(new AccountDetailsForBulkResponse(userAccount.getEmail(), userAccount.getAccountId(), userAccount.getFkUserId().getFirstName(), userAccount.getFkUserId().getLastName(),"Member is added successfully"));
            }
            catch (Exception e) {
                failureList.add(new AccountDetailsForBulkResponse(userAccount != null ? userAccount.getEmail() : null, userAccount != null ? userAccount.getAccountId() : null, userAccount != null ? userAccount.getFkUserId().getFirstName() : null, userAccount != null ? userAccount.getFkUserId().getLastName() : null, e.getMessage()));
            }
        }
        addedSprintMemberResponse.setSprintId(sprintDb.getSprintId());
        addedSprintMemberResponse.setSuccessList(successList);
        if (!failureList.isEmpty()) {
            addedSprintMemberResponse.setFailureList(failureList);
            addedSprintMemberResponse.setMessage("Following member is failed to become sprint member");
        }
        sprintMemberList.addAll(userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(accountIdListForAddMember));
        // ZZZZZZ 14-04-2025
        capacityService.addAndUpdateCapacityOnAddingMember(addedSprintMemberRequest.getSprintId(), accountIdListForAddMember, timeZone);
        sprintDb.setSprintMembers(sprintMemberList);
        sprintRepository.save(sprintDb);
        return addedSprintMemberResponse;
    }

    @Transactional (propagation = Propagation.REQUIRES_NEW)
    public void changeAssignedToAndRecalculateCapacity (Task task, Long accountIdAssigned) {
        if (!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE) && accountIdAssigned == null) {
            throw new ValidationFailedException("Assigned To for " + task.getTaskNumber() + " can't be empty");
        }
        Task updatedTask = new Task();
        BeanUtils.copyProperties(task, updatedTask);
        List<String> updatedFields = new ArrayList<>();
        updatedFields.add("fkAccountIdAssigned");
        if (accountIdAssigned == null) {
            updatedTask.setFkAccountIdAssigned(null);
        }
        else {
            updatedTask.setFkAccountIdAssigned(userAccountRepository.findByAccountId(accountIdAssigned));
        }
        if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            capacityService.handleAccountChange(task, updatedTask);
        }
        taskHistoryService.addTaskHistoryOnUserUpdate(task);
        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, updatedTask);
        taskRepository.save(updatedTask);
    }

    public SprintResponseForGetAllSprints getSprintTaskStats (Sprint sprint, List<Task> sprintTaskList, String timeZone) {
        SprintResponseForGetAllSprints sprintResponseForGetAllSprints = new SprintResponseForGetAllSprints();

        for (Task sprintTask : sprintTaskList) {
            if(!Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {

                ProgressSystemSprintTask progressSystemSprintTask = new ProgressSystemSprintTask();
                progressSystemSprintTask.setTaskId(sprintTask.getTaskId());
                progressSystemSprintTask.setTaskTitle(sprintTask.getTaskTitle());
                progressSystemSprintTask.setEffort(sprintTask.getRecordedEffort());
                progressSystemSprintTask.setEstimate(sprintTask.getTaskEstimate());
                progressSystemSprintTask.setPercentageCompleted(sprintTask.getUserPerceivedPercentageTaskCompleted());
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
                progressSystemSprintTask.setExpEndDate(dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(sprintTask.getTaskExpEndDate(), timeZone)));
                if (sprintTask.getFkAccountIdAssigned() != null) {
                    User user = sprintTask.getFkAccountIdAssigned().getFkUserId();
                    progressSystemSprintTask.setAccountIdAssigned(user.getFirstName() + " " + user.getLastName());
                }
                if (sprintTask.getIsBug()) {
                    progressSystemSprintTask.setIsBug(true);
                }
                progressSystemSprintTask.setTaskTypeId(sprintTask.getTaskTypeId());

                if (!Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    if (Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        sprintResponseForGetAllSprints.incrementDeletedTasks();
                        sprintResponseForGetAllSprints.getDeletedTasksList().add(progressSystemSprintTask);
                        continue;
                    }
                    if (Objects.equals(StatType.DELAYED, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.incrementDelayedTasks();
                        sprintResponseForGetAllSprints.getDelayedTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.ONTRACK, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.incrementOnTrackTasks();
                        sprintResponseForGetAllSprints.getOnTrackTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.COMPLETED, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.incrementCompletedTasks();
                        sprintResponseForGetAllSprints.getCompletedTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.NOTSTARTED, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.incrementNotStartedTasks();
                        sprintResponseForGetAllSprints.getNotStartedTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.WATCHLIST, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.incrementWatchListTasks();
                        sprintResponseForGetAllSprints.getWatchListTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.LATE_COMPLETION, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.incrementLateCompletedTask();
                        sprintResponseForGetAllSprints.getLateCompletedTasksList().add(progressSystemSprintTask);
                    }
                    sprintResponseForGetAllSprints.incrementTotalTasks();
                } else {
                    List<Task> childTaskList = taskRepository.findByParentTaskId(sprintTask.getTaskId());
                    for (Task childTask : childTaskList) {
                        if (!Objects.equals(childTask.getSprintId(), sprint.getSprintId())) {
                            continue;
                        }
                        ProgressSystemSprintTask progressSystemSprintChildTask = new ProgressSystemSprintTask();
                        progressSystemSprintChildTask.setTaskId(childTask.getTaskId());
                        progressSystemSprintChildTask.setTaskTitle(childTask.getTaskTitle());
                        progressSystemSprintChildTask.setEffort(childTask.getRecordedEffort());
                        progressSystemSprintChildTask.setEstimate(childTask.getTaskEstimate());
                        progressSystemSprintChildTask.setPercentageCompleted(childTask.getUserPerceivedPercentageTaskCompleted());
                        progressSystemSprintChildTask.setExpEndDate(dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(childTask.getTaskExpEndDate(), timeZone)));
                        if (childTask.getFkAccountIdAssigned() != null) {
                            User childTaskUser = childTask.getFkAccountIdAssigned().getFkUserId();
                            progressSystemSprintChildTask.setAccountIdAssigned(childTaskUser.getFirstName() + " " + childTaskUser.getLastName());
                        }
                        if (childTask.getIsBug()) {
                            progressSystemSprintChildTask.setIsBug(true);
                        }
                        progressSystemSprintChildTask.setTaskTypeId(childTask.getTaskTypeId());

                        if (Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                            sprintResponseForGetAllSprints.incrementDeletedTasks();
                            sprintResponseForGetAllSprints.getDeletedTasksList().add(progressSystemSprintChildTask);
                            continue;
                        }
                        if (Objects.equals(StatType.DELAYED, childTask.getTaskProgressSystem())) {
                            sprintResponseForGetAllSprints.incrementDelayedTasks();
                            sprintResponseForGetAllSprints.getDelayedTasksList().add(progressSystemSprintChildTask);
                        } else if (Objects.equals(StatType.ONTRACK, childTask.getTaskProgressSystem())) {
                            sprintResponseForGetAllSprints.incrementOnTrackTasks();
                            sprintResponseForGetAllSprints.getOnTrackTasksList().add(progressSystemSprintChildTask);
                        } else if (Objects.equals(StatType.COMPLETED, childTask.getTaskProgressSystem())) {
                            sprintResponseForGetAllSprints.incrementCompletedTasks();
                            sprintResponseForGetAllSprints.getCompletedTasksList().add(progressSystemSprintChildTask);
                        } else if (Objects.equals(StatType.NOTSTARTED, childTask.getTaskProgressSystem())) {
                            sprintResponseForGetAllSprints.incrementNotStartedTasks();
                            sprintResponseForGetAllSprints.getNotStartedTasksList().add(progressSystemSprintChildTask);
                        } else if (Objects.equals(StatType.WATCHLIST, childTask.getTaskProgressSystem())) {
                            sprintResponseForGetAllSprints.incrementWatchListTasks();
                            sprintResponseForGetAllSprints.getWatchListTasksList().add(progressSystemSprintChildTask);
                        } else if (Objects.equals(StatType.LATE_COMPLETION, childTask.getTaskProgressSystem())) {
                            sprintResponseForGetAllSprints.incrementLateCompletedTask();
                            sprintResponseForGetAllSprints.getLateCompletedTasksList().add(progressSystemSprintChildTask);
                        }
                        sprintResponseForGetAllSprints.incrementTotalTasks();
                    }

                    // set parent task detail
                    if (Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        sprintResponseForGetAllSprints.getDeletedTasksList().add(progressSystemSprintTask);
                    }
                    else if (Objects.equals(StatType.DELAYED, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.getDelayedTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.ONTRACK, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.getOnTrackTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.COMPLETED, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.getCompletedTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.NOTSTARTED, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.getNotStartedTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.WATCHLIST, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.getWatchListTasksList().add(progressSystemSprintTask);
                    } else if (Objects.equals(StatType.LATE_COMPLETION, sprintTask.getTaskProgressSystem())) {
                        sprintResponseForGetAllSprints.getLateCompletedTasksList().add(progressSystemSprintTask);
                    }
                }
            }
        }
        return sprintResponseForGetAllSprints;
    }

    public void removeLeadingAndTrailingSpacesForSprint (SprintRequest sprintRequest) {
        if (sprintRequest.getSprintTitle() != null) {
            sprintRequest.setSprintTitle(sprintRequest.getSprintTitle().trim());
        }
        if (sprintRequest.getSprintObjective() != null) {
            sprintRequest.setSprintObjective(sprintRequest.getSprintObjective().trim());
        }
    }

    @Transactional
    public Pair<Integer, Long> getDependencyDownStream(Task task, HashMap<Long, Boolean> isVisited, Long lastVisited, Sprint sprint1, Sprint sprint2, HashMap<Long, Long> processedTasks, List<Task> sprint2TasksToProcess) {
        Task parentTask = new Task();
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK))
            parentTask = taskRepository.findByTaskId(task.getParentTaskId());
        if (parentTask != null && parentTask.getDependencyIds() != null && !parentTask.getDependencyIds().isEmpty()) {
            if(processedTasks.containsKey(task.getTaskId()))
                processedTasks.replace(task.getTaskId(), -1L);
            else
                processedTasks.put(task.getTaskId(), -1L);
            if(processedTasks.containsKey(task.getTaskId()))
                processedTasks.replace(task.getParentTaskId(), -1L);
            else
                processedTasks.put(task.getParentTaskId(), -1L);
        } else if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
            if(!processedTasks.containsKey(parentTask.getTaskId()))
                processedTasks.put(parentTask.getTaskId(), -2L);
            if(!isVisited.containsKey(task.getParentTaskId()))
                isVisited.put(task.getParentTaskId(), false);
        }
        List<Dependency> dependencyList = dependencyRepository.findByDependencyIdInAndIsRemoved(task.getDependencyIds(), false);
        List<Task> predecessorList = new ArrayList<>();
        List<Task> successorList = new ArrayList<>();
        for (Dependency dependency : dependencyList) {
            Task predecessorTask = taskRepository.findByTaskId(dependency.getPredecessorTaskId());
            Task successorTask = taskRepository.findByTaskId(dependency.getSuccessorTaskId());

            /*
            Check if successor is before S2 endDate
            AND
            successor is not part of problemTasks
            */
            if (!Objects.equals(task.getTaskId(), successorTask.getTaskId())) {
                if ((isVisited.containsKey(successorTask.getTaskId()) && isVisited.get(successorTask.getTaskId()))) {
                    successorList.add(successorTask);
                    successorList.add(successorTask);
                    break;
                }
                if (Objects.equals(successorTask.getSprintId(), sprint1.getSprintId())) {
                    if (!Objects.equals(successorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        successorList.add(successorTask);
                        if (!isVisited.containsKey(successorTask.getTaskId()))
                            successorList.add(successorTask);
                    }
                    if (successorList.size() > 2) //ToDo:successor list > 2(to make divergent dependencies upto count 1)
                        break;
                } else if (Objects.equals(successorTask.getSprintId(), sprint2.getSprintId())) {
                    if (!Objects.equals(successorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        successorList.add(successorTask);
                        processedTasks.replace(successorTask.getTaskId(), -2L);
                        sprint2TasksToProcess.add(successorTask);
                    }
                    if (successorList.size() > 2)
                        break;
                } else {
                    if (!Objects.equals(successorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        if (!successorTask.getTaskExpStartDate().isAfter(sprint2.getSprintExpEndDate())) {
                            successorList.add(successorTask);
                            successorList.add(successorTask);
                            break;
                        }
                    }
                    if (successorList.size() > 2)
                        break;
                }
            }
            if (task.getTaskId() != predecessorTask.getTaskId()) {
                if (isVisited.containsKey(predecessorTask.getTaskId()) && isVisited.get(predecessorTask.getTaskId()) && !Objects.equals(predecessorTask.getTaskId(), lastVisited)) {
                    predecessorList.add(predecessorTask);
                    predecessorList.add(predecessorTask);
                    break;
                }
                if (Objects.equals(predecessorTask.getSprintId(), sprint1.getSprintId())) {
                    if (!Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) && !Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE))
                        predecessorList.add(predecessorTask);
                    if (predecessorList.size() > 2)
                        break;
                } else if (Objects.equals(predecessorTask.getSprintId(), sprint2.getSprintId())) {
                    if (!Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) && !Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE))
                        predecessorList.add(predecessorTask);
                    if (predecessorList.size() > 2)
                        break;
                } else {
                    if (!Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) && !Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                        if (!predecessorTask.getTaskExpEndDate().isBefore(sprint2.getSprintExpStartDate())) {
                            predecessorList.add(predecessorTask);
                            predecessorList.add(predecessorTask);
                            break;
                        }
                    }
                    if (predecessorList.size() > 2)
                        break;
                }
            }
        }
        if (successorList.size() > 2 || predecessorList.size() > 2) {
            if (isVisited.containsKey(task.getTaskId()))
                isVisited.replace(task.getTaskId(), true);
            processedTasks.put(task.getTaskId(), -1L);
            return new Pair<Integer, Long>(-1, -1L);
        } else if (successorList.size() > 0) {
            Pair<Integer, Long> a = getDependencyDownStream(successorList.get(0), isVisited, task.getTaskId(), sprint1, sprint2, processedTasks, sprint2TasksToProcess);
//            if(a.getFirst() == -1) {
//                processedTasks.putIfAbsent(task.getTaskId(), -1L);
//                return new Pair<Integer,Long>(-1,-1L);
//            }
            if (processedTasks.containsKey(task.getTaskId()) && processedTasks.get(task.getTaskId()) != -1)
                processedTasks.replace(task.getTaskId(), a.getSecond());
            else if (processedTasks.containsKey(task.getTaskId()))
                a = new Pair<>(a.getFirst(), -1L);
            else
                processedTasks.put(task.getTaskId(), a.getSecond());
            if (isVisited.containsKey(task.getTaskId()))
                isVisited.replace(task.getTaskId(), true);
            return new Pair<Integer, Long>((int) (Math.ceil(task.getTaskEstimate() * (100 - ((task.getUserPerceivedPercentageTaskCompleted() != null) ? task.getUserPerceivedPercentageTaskCompleted() : 0)) / 100.0 + a.getFirst())), a.getSecond());
        }
        if (isVisited.containsKey(task.getTaskId()))
            isVisited.replace(task.getTaskId(), true);
        else
            isVisited.putIfAbsent(task.getTaskId(), true);
        if (!processedTasks.containsKey(task.getTaskId()) || processedTasks.containsKey(task.getTaskId()) && processedTasks.get(task.getTaskId()) != -1) {
            if (processedTasks.containsKey(task.getTaskId()))
                processedTasks.replace(task.getTaskId(), task.getTaskId());
            else
                processedTasks.putIfAbsent(task.getTaskId(), task.getTaskId());
            if(task.getTaskEstimate()==null)
                throw new IllegalStateException("Cannot move " + task.getTaskNumber() + " from sprint. Please provide estimates.");
            return new Pair<Integer, Long>((int) (Math.ceil(task.getTaskEstimate() * (100 - ((task.getUserPerceivedPercentageTaskCompleted() != null) ? task.getUserPerceivedPercentageTaskCompleted() : 0)) / 100.0)), task.getTaskId());
        }
        return new Pair<Integer, Long>((int) Math.ceil(task.getTaskEstimate() * (100 - ((task.getUserPerceivedPercentageTaskCompleted() != null) ? task.getUserPerceivedPercentageTaskCompleted() : 0)) / 100.0), -1L);
    }

    @Transactional
    public String validateSprintConditionAndModifyDependencyTaskProperties(Task task, Sprint sprint, Task parentTaskRequest, HashMap<Long, Boolean> isVisited, HashMap<Long, Long> processedTasks, HashMap<Long, Integer> taskEstimates, HashMap<Long, LocalDateTime> taskEstimatesLastDate, HashMap<Long, LocalDateTime> parentExpStartDate, HashMap<Long, LocalDateTime> parentExpEndDate, List<Task> sprint2TasksToProcess, List<Task> childTasksWithParentDependencies) {

        Task foundTaskDb = taskRepository.findByTaskId(task.getTaskId());

        boolean isTaskInPermissibleState = Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)
                || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) ||
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) ||
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);

        boolean isTaskInActiveSprintState =
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) ||
                        Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);

        // Condition for task already part of another sprint
        boolean wasTaskPartOfAnotherSprint = foundTaskDb != null && foundTaskDb.getSprintId() != null && !foundTaskDb.getSprintId().equals(sprint.getSprintId());

        // Check if the task is in a not started/ backlog  states or if it was in another Sprint earlier, then it can be in started/ blocked/ on hold state
        if (!isTaskInPermissibleState) {
            throw new ValidationFailedException("Cannot add " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " Work Item to sprint. Work Item number : " + task.getTaskNumber());
        }

        // Additional validation for team alignment
        if (Objects.equals(sprint.getEntityTypeId(), Constants.EntityTypes.TEAM) && !Objects.equals(sprint.getEntityId(), task.getFkTeamId().getTeamId())) {
            throw new IllegalStateException("Cannot add Work Item to the sprint. Work Item and sprint belong to different teams");
        }

        // disallow removal of task from completed sprint
        if (foundTaskDb != null && foundTaskDb.getSprintId() != null && task.getSprintId() == null) {
            Sprint sprintFromOriginalTask = sprintRepository.findById(foundTaskDb.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
            if (sprintFromOriginalTask.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new ValidationFailedException("Cannot remove Work Item from a completed sprint");
            }
        }


        // disallow addition of task to a completed sprint
        if (task.getSprintId() != null) {
            Sprint sprintFromOriginalTask = sprintRepository.findById(task.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
            if (foundTaskDb != null && !Objects.equals(sprint.getSprintId(), sprintFromOriginalTask.getSprintId()) && sprintFromOriginalTask.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) && foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                throw new ValidationFailedException("Cannot move task from a completed sprint. The task is currently associated with a completed sprint titled " + sprintFromOriginalTask.getSprintTitle());
            }

            if (Objects.equals(sprint.getSprintId(), sprintFromOriginalTask.getSprintId()) && sprintFromOriginalTask.getSprintStatus().equals(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new ValidationFailedException("Cannot add task to a completed sprint");
            }
        }
        //processedTasks means lastTaskOfChainMap (taskId -> taskId which comes at the end of the chain from key task)
        if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty() && isVisited.containsKey(task.getTaskId()) && !isVisited.get(task.getTaskId())) {
            //First check if predecessors are within sprint and if they are converging towards the current task
            Pair<Integer, Long> pair = getDependencyDownStream(task, isVisited, -1L, sprintRepository.findBySprintId(task.getSprintId()), sprint, processedTasks, sprint2TasksToProcess);
            taskEstimates.put(pair.getSecond(), pair.getFirst());
            if (pair.getFirst() > Duration.between(sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate()).toMinutes())
                throw new ValidationFailedException("Not enough space in sprint.");
            taskEstimatesLastDate.putIfAbsent(pair.getSecond(), sprint.getSprintExpStartDate());
            isVisited.replace(task.getTaskId(), true);
        }

        isTaskInActiveSprintState = Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) ||
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);

        boolean datesAdjusted = false;

        //-1 indicates problematic tasks
        if (processedTasks.containsKey(task.getTaskId()) && processedTasks.get(task.getTaskId()) == -1L) {
            throw new ValidationFailedException("Work Item " + task.getTaskNumber() + " possibly has convergent/divergent dependencies within chain or one of its Expected Dates lies out of the range of Sprint Expected Dates.");
        }
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            if (task.getCountChildExternalDependencies() == 0) {
                Long lastTaskOfChain = processedTasks.get(task.getTaskId());
                //-----------child dependencies------------//
                datesAdjusted = addSprintDatesToTaskForDependencies(task, lastTaskOfChain, sprint, isTaskInActiveSprintState, parentTaskRequest, taskEstimates, taskEstimatesLastDate, parentExpStartDate, parentExpEndDate);
                List<Task> childTasksDb = taskRepository.findByParentTaskId(task.getTaskId());
                for (Task childTask : childTasksDb) {
                    childTasksWithParentDependencies.add(childTask);
                }
            }
            else {
                //Since the child tasks will be processed first, we will have updated values in parentExpDates
                task.setTaskExpStartDate(parentExpStartDate.get(task.getTaskId()));
                task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                task.setTaskExpEndDate(parentExpEndDate.get(task.getTaskId()));
                task.setTaskExpEndTime(task.getTaskExpEndDate().toLocalTime());
            }
        } else {
            Long lastTaskOfChain = processedTasks.get(task.getTaskId());
            //--------------------simple dependencies-----------//
            datesAdjusted = addSprintDatesToTaskForDependencies(task, lastTaskOfChain, sprint, isTaskInActiveSprintState, parentTaskRequest, taskEstimates, taskEstimatesLastDate, parentExpStartDate, parentExpEndDate);
        }

        // Message handling
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Task added to sprint ").append(sprint.getSprintTitle()).append(" successfully");
        if (datesAdjusted) stringBuilder.append(", Task dates adjusted");

        String message = stringBuilder.toString();
        return message;
    }
//---
    @Transactional
    public Boolean addSprintDatesToTaskForDependencies(Task task, Long lastTaskOfChain, Sprint sprint, Boolean isTaskInActiveSprintState, Task parentTask, HashMap<Long, Integer> taskEstimates, HashMap<Long, LocalDateTime> taskEstimatesLastDate, HashMap<Long, LocalDateTime> parentExpStartDate, HashMap<Long, LocalDateTime> parentExpEndDate) {
        Boolean datesAdjusted = false;
        if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
            if (lastTaskOfChain <= 0)
                task.setTaskExpStartDate(sprint.getSprintExpStartDate());
            else
                task.setTaskExpStartDate(taskEstimatesLastDate.get(lastTaskOfChain));
            task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
        }
        if (task.getUserPerceivedPercentageTaskCompleted() == null && lastTaskOfChain>0)
            task.setTaskExpEndDate(taskEstimatesLastDate.get(lastTaskOfChain).plusMinutes((task.getTaskEstimate() - 1) * (Duration.between(sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate()).toMinutes() / taskEstimates.get(lastTaskOfChain))));
        else if (lastTaskOfChain <= 0)
            task.setTaskExpEndDate(sprint.getSprintExpEndDate());
        else {
            double estimatedCompletionTime = task.getTaskEstimate() * (100 - task.getUserPerceivedPercentageTaskCompleted()) / 100.0;

            // Get the total duration of the sprint in minutes as a float
            double sprintDurationMinutes = Duration.between(sprint.getSprintExpStartDate(), sprint.getSprintExpEndDate()).toMinutes();

            // Calculate the minutes proportionate to the last task of the chain
            double proportionateMinutes = (estimatedCompletionTime * (sprintDurationMinutes / taskEstimates.get(lastTaskOfChain))) + 1;

            // Calculate the new end date
            LocalDateTime newEndDate = taskEstimatesLastDate.get(lastTaskOfChain).plusMinutes((long) proportionateMinutes);

            // Set the new end date for the task
            task.setTaskExpEndDate(newEndDate);
        }
        if (Objects.equals(task.getTaskId(), lastTaskOfChain))
            task.setTaskExpEndDate(sprint.getSprintExpEndDate());
        task.setTaskExpEndTime(task.getTaskExpEndDate().toLocalTime());
        taskEstimatesLastDate.replace(lastTaskOfChain, task.getTaskExpEndDate().plusMinutes(1));
        datesAdjusted = true;

        //Assuming forward movement of tasks from S1 to S2
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
            if (parentExpStartDate.containsKey(task.getParentTaskId()) && parentExpStartDate.get(task.getParentTaskId()).isAfter(task.getTaskExpStartDate()))
                parentExpStartDate.replace(task.getParentTaskId(), task.getTaskExpStartDate());
            else if (!parentExpStartDate.containsKey(task.getParentTaskId()))
                parentExpStartDate.put(task.getParentTaskId(), task.getTaskExpStartDate());
            if (parentExpEndDate.containsKey(task.getParentTaskId()) && parentExpEndDate.get(task.getParentTaskId()).isBefore(task.getTaskExpEndDate()))
                parentExpEndDate.replace(task.getParentTaskId(), task.getTaskExpEndDate());
            else if (!parentExpEndDate.containsKey(task.getParentTaskId()))
                parentExpEndDate.put(task.getParentTaskId(), task.getTaskExpEndDate());
        }
        if (isTaskInActiveSprintState) {
            task.setIsSprintChanged(true);
        }
        return datesAdjusted;
    }

    private SprintStatusUpdateObject deleteSprint(Sprint sprintDb, SprintStatusRequest sprintStatusRequest, String accountIds, String timeZone) throws IllegalAccessException {
        List<Task> sprintTaskList = taskRepository.findBySprintId(sprintDb.getSprintId());
//        SprintTaskByFilterResponse sprintTaskByFilterResponse = getSprintTaskResponse(sprintTaskList, sprintDb.getSprintId(), false);
        setWorkItemInSprintBeforeDeletionOfSprint(sprintDb, sprintTaskList, timeZone);
        List<TaskNumberTaskTitleSprintName> taskToMove = new ArrayList<>();
        MoveSprintTaskRequest moveSprintTaskRequest = new MoveSprintTaskRequest();

        SprintStatusUpdateObject sprintStatusUpdateObject = new SprintStatusUpdateObject();

        if (!sprintStatusRequest.getDeleteWorkItem()) {
            for (Task sprintTask : sprintTaskList) {
                if (Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) || Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) || Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                MoveSprintTask moveSprintTask = new MoveSprintTask(sprintDb.getSprintId(), sprintTask.getTaskId(), null);
                try {
                    moveSprintTaskRequest.setSprintTaskList(List.of(moveSprintTask));
                    moveSprintTaskRequest.setEntityTypeId(sprintDb.getEntityTypeId());
                    moveSprintTaskRequest.setEntityId(sprintDb.getEntityId());
                    validateSprintConditionAndMoveTaskFromSprint(moveSprintTaskRequest, accountIds, false);
                } catch (Exception e) {
                    TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                    task.setTaskNumber(sprintTask.getTaskNumber());
                    task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                    task.setTaskId(sprintTask.getTaskId());
                    task.setTaskTitle(sprintTask.getTaskTitle());
                    task.setSprintTitle(sprintDb.getSprintTitle());
                    task.setTaskTypeId(sprintTask.getTaskTypeId());
                    task.setMessage(e.getMessage());
                    taskToMove.add(task);
                }
            }
        }
        else {
            DeleteWorkItemRequest deleteWorkItemRequest = new DeleteWorkItemRequest();
            deleteWorkItemRequest.setDeleteReasonId(Constants.DeleteWorkItemReasonEnum.OTHERS.getTypeId());
            deleteWorkItemRequest.setDeleteReason("Work item deleted along with the deletion of sprint");
            for (Task sprintTask : sprintTaskList) {
                if (Objects.equals(sprintTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) || Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) || Objects.equals(sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                try {
                    taskServiceImpl.deleteTaskByTaskId(sprintTask.getTaskId(), sprintTask, accountIds, timeZone, false, deleteWorkItemRequest);
                } catch (Exception e) {
                    TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                    task.setTaskNumber(sprintTask.getTaskNumber());
                    task.setTeamId(sprintTask.getFkTeamId().getTeamId());
                    task.setTaskId(sprintTask.getTaskId());
                    task.setTaskTitle(sprintTask.getTaskTitle());
                    task.setSprintTitle(sprintDb.getSprintTitle());
                    task.setTaskTypeId(sprintTask.getTaskTypeId());
                    task.setMessage(e.getMessage());
                    taskToMove.add(task);
                }
            }
        }
        if (taskToMove.isEmpty()) {
            Sprint previousSprint = null;
            Sprint nextSprint = null;
            if (sprintDb.getPreviousSprintId() != null) {
                previousSprint = sprintRepository.findBySprintId(sprintDb.getPreviousSprintId());
            }
            if (sprintDb.getNextSprintId() != null) {
                nextSprint = sprintRepository.findBySprintId(sprintDb.getNextSprintId());
            }
            if (previousSprint != null) {
                previousSprint.setNextSprintId(nextSprint != null ? nextSprint.getSprintId() : null);
            }
            if (nextSprint != null) {
                nextSprint.setPreviousSprintId(previousSprint != null ? previousSprint.getSprintId() : null);
            }
            sprintDb.setPreviousSprintId(null);
            sprintDb.setNextSprintId(null);
            sprintDb.setSprintStatus(Constants.SprintStatusEnum.DELETED.getSprintStatusId());
            sprintRepository.save(sprintDb);
        }
        sprintStatusUpdateObject.setTaskToUpdate(taskToMove);
        return sprintStatusUpdateObject;
    }


    public void setWorkItemInSprintBeforeDeletionOfSprint(Sprint sprint, List<Task> sprintTaskList, String timeZone) {
        List<ProgressSystemSprintTask> notStartedWorkItemList= new ArrayList<>();
        List<ProgressSystemSprintTask> startedWorkItemList = new ArrayList<>();
        List<ProgressSystemSprintTask> completedWorkItemList = new ArrayList<>();

        for (Task sprintTask : sprintTaskList) {
            ProgressSystemSprintTask progressSystemSprintTask = new ProgressSystemSprintTask();
            progressSystemSprintTask.setTaskId(sprintTask.getTaskId());
            progressSystemSprintTask.setTaskTitle(sprintTask.getTaskTitle());
            progressSystemSprintTask.setEffort(sprintTask.getRecordedEffort());
            progressSystemSprintTask.setEstimate(sprintTask.getTaskEstimate());
            progressSystemSprintTask.setPercentageCompleted(sprintTask.getUserPerceivedPercentageTaskCompleted());
            if (sprintTask.getTaskExpEndDate() != null) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
                progressSystemSprintTask.setExpEndDate(dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(sprintTask.getTaskExpEndDate(), timeZone)));
            }
            if (sprintTask.getIsBug()) {
                progressSystemSprintTask.setIsBug(true);
            }
            progressSystemSprintTask.setTaskTypeId(sprintTask.getTaskTypeId());
            if (sprintTask.getFkAccountIdAssigned() != null) {
                User user = sprintTask.getFkAccountIdAssigned().getFkUserId();
                progressSystemSprintTask.setAccountIdAssigned(user.getFirstName() + " " + user.getLastName());
            }
            if (sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                completedWorkItemList.add(progressSystemSprintTask);
            } else if (sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE) || ((sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) || sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE) || sprintTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) && sprintTask.getTaskActStDate() != null)) {
                startedWorkItemList.add(progressSystemSprintTask);
            } else {
                notStartedWorkItemList.add(progressSystemSprintTask);
            }
        }
        sprint.setToDoWorkItemList(notStartedWorkItemList);
        sprint.setInProgressWorkItemList(startedWorkItemList);
        sprint.setCompletedWorkItemList(completedWorkItemList);
    }
    //ZZZZZZ 14-04-2025
    public String recalculateCapacityOfSprint(Long sprintId, String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Integer> authorizedRoleList = new ArrayList<>();
        authorizedRoleList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
        authorizedRoleList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        Sprint sprintDb = sprintRepository.findBySprintId(sprintId);
        if (sprintDb == null) {
            throw new IllegalAccessException("Sprint not found");
        }
        Team team = teamRepository.findById(sprintDb.getEntityId()).orElseThrow(() -> new EntityNotFoundException("Team not found"));

        EntityPreference entityPreference = entityPreferenceService.fetchEntityPreference(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId());
        Set<EmailFirstLastAccountId> sprintMemberList = sprintDb.getSprintMembers();
        if (sprintMemberList == null) {
            sprintMemberList = new HashSet<>();
        }
        List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                .map(EmailFirstLastAccountId::getAccountId)
                .collect(Collectors.toList());
        if (!CommonUtils.containsAny(accountIdList, sprintMemberAccountIdList)) {
            throw new IllegalAccessException("Unauthorized: You are not part of sprint so you can't recalculate capacity in sprint");
        }
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, sprintDb.getEntityId(), accountIdList, true, authorizedRoleList)) {
            throw new IllegalAccessException("Unauthorized: You does not have permission to recalculate capacity");
        }
        // ZZZZZZ 14-04-2025
        Sprint sprintCopy = new Sprint();
        BeanUtils.copyProperties(sprintDb, sprintCopy);
        convertAllSprintDateToUserTimeZone(sprintCopy, timeZone);
        capacityService.recalculateCapacitiesForSprint(sprintCopy, entityPreference,null);
        return "Capacity is recalculated successfully";
    }

    public Boolean showCardOnScreen (SprintTaskByFilterRequest sprintTaskByFilterRequest, Task parentTask) {
        if (sprintTaskByFilterRequest.getTaskPriority() != null && !Objects.equals(sprintTaskByFilterRequest.getTaskPriority(), parentTask.getTaskPriority())) {
            return false;
        }
        if (sprintTaskByFilterRequest.getTaskTypeId() != null && !Objects.equals(sprintTaskByFilterRequest.getTaskTypeId(), parentTask.getTaskTypeId())) {
            return false;
        }
        if ((sprintTaskByFilterRequest.getAccountId() != null && parentTask.getFkAccountIdAssigned() == null)
                || (sprintTaskByFilterRequest.getAccountId() != null && parentTask.getFkAccountIdAssigned() != null && !Objects.equals(sprintTaskByFilterRequest.getAccountId(), parentTask.getFkAccountIdAssigned().getAccountId()))) {
            return false;
        }
        if (sprintTaskByFilterRequest.getLabelIds() != null) {
            List<Long> taskLabelIds = parentTask.getLabels()
                    .stream()
                    .map(Label::getLabelId)
                    .collect(Collectors.toList());
            if (taskLabelIds.isEmpty() || sprintTaskByFilterRequest.getLabelIds().stream().noneMatch(taskLabelIds::contains)) {
                return false;
            }
        }
        return true;
    }

    public TaskListForBulkResponse bulkMoveTaskFromSprint(MoveSprintTaskRequest moveSprintTaskRequest, String
            accountIds, Boolean onComplete) throws IllegalAccessException {
        TaskListForBulkResponse taskListForBulkResponse = new TaskListForBulkResponse();
        List<TaskForBulkResponse> failureList = new ArrayList<>();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        for (MoveSprintTask moveSprintTask : moveSprintTaskRequest.getSprintTaskList()) {
            Task moveTask = taskRepository.findByTaskId(moveSprintTask.getTaskId());
            Sprint prevSprint = sprintRepository.findBySprintId(moveTask.getSprintId());
            Sprint nextSprint = sprintRepository.findBySprintId(moveSprintTask.getNewSprintId());
            if(prevSprint == null && nextSprint ==null)
                continue;
            if(nextSprint == null) {
                nextSprint = new Sprint();
                nextSprint.setSprintId(null);
            } else {
                boolean isInvalidSprint = Objects.equals(nextSprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()) ||
                        Objects.equals(nextSprint.getSprintStatus(), Constants.SprintStatusEnum.DELETED.getSprintStatusId());

                boolean isInvalidTask = Objects.equals(moveTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) ||
                        Objects.equals(moveTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) ||
                        Objects.equals(moveTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE);

                if (isInvalidSprint || isInvalidTask) {
                    String errorMessage;

                    if (isInvalidSprint && isInvalidTask) {
                        errorMessage = "Cannot move Child/Completed/Deleted Work Item to a Completed/Deleted Sprint";
                    } else if (isInvalidSprint) {
                        errorMessage = "Cannot add Work Item to a Completed/Deleted Sprint";
                    } else {
                        errorMessage = "Cannot move Child/Completed/Deleted Work Item to another Sprint";
                    }

                    TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                    task.setTaskNumber(moveTask.getTaskNumber());
                    task.setTeamId(moveTask.getFkTeamId().getTeamId());
                    task.setTaskId(moveTask.getTaskId());
                    task.setTaskTitle(moveTask.getTaskTitle());
                    task.setSprintTitle(prevSprint == null ? null : prevSprint.getSprintTitle());
                    task.setTaskTypeId(moveTask.getTaskTypeId());
                    task.setMessage(errorMessage);

                    logger.error("Something went wrong: Not able to add Work Item " + (moveTask != null ? moveTask.getTaskNumber() : null) + " Caught Exception: " + errorMessage);

                    failureList.add(new TaskForBulkResponse(moveTask.getTaskId(), moveTask.getTaskNumber(), moveTask.getTaskTitle(), moveTask.getFkTeamId().getTeamId(), errorMessage));
                    continue;
                }
            }

            try {
                if(prevSprint == null) {
                    TaskListForBulkResponse taskListForBulkResponseFromAddToSprint = addAllTaskToSprint(nextSprint.getSprintId(), List.of(moveTask.getTaskId()), accountIds);
                    if(taskListForBulkResponseFromAddToSprint.getFailureList().size()==0)
                        successList.add(taskListForBulkResponseFromAddToSprint.getSuccessList().get(0));
                    else
                        failureList.add(taskListForBulkResponseFromAddToSprint.getFailureList().get(0));
                }
                else {
                    MoveSprintTaskRequest moveSprintTaskRequestForMoving = new MoveSprintTaskRequest();
                    moveSprintTask.setPreviousSprintId(prevSprint.getSprintId());
                    moveSprintTaskRequestForMoving.setSprintTaskList(List.of(moveSprintTask));
                    moveSprintTaskRequestForMoving.setEntityTypeId(prevSprint.getEntityTypeId());
                    moveSprintTaskRequestForMoving.setEntityId(prevSprint.getEntityId());
                    if (!Objects.equals(prevSprint.getSprintId(), nextSprint.getSprintId()))
                        validateSprintConditionAndMoveTaskFromSprint(moveSprintTaskRequestForMoving, accountIds, false);
                    successList.add(new TaskForBulkResponse(moveTask.getTaskId(), moveTask.getTaskNumber(), moveTask.getTaskTitle(), moveTask.getFkTeamId().getTeamId(), "Task successfully added to sprint"));
                }
            } catch (Exception e) {
                TaskNumberTaskTitleSprintName task = new TaskNumberTaskTitleSprintName();
                task.setTaskNumber(moveTask.getTaskNumber());
                task.setTeamId(moveTask.getFkTeamId().getTeamId());
                task.setTaskId(moveTask.getTaskId());
                task.setTaskTitle(moveTask.getTaskTitle());
                if(prevSprint==null) {
                    if(moveTask.getTaskEstimate()==null)
                    task.setSprintTitle(null);
                }
                else
                    task.setSprintTitle(prevSprint.getSprintTitle());
                task.setTaskTypeId(moveTask.getTaskTypeId());
                task.setMessage(e.getMessage());
                logger.error("Something went wrong: Not able to add Work Item " + (moveTask != null ? moveTask.getTaskNumber() : null) + " Caught Exception: " + e.getMessage());
                failureList.add(new TaskForBulkResponse(moveTask.getTaskId(), moveTask.getTaskNumber(), moveTask.getTaskTitle(), moveTask.getFkTeamId().getTeamId(), e.getMessage()));
            }
        }
        taskListForBulkResponse.setFailureList(failureList);
        taskListForBulkResponse.setSuccessList(successList);
        return taskListForBulkResponse;
    }

    public void createTaskMoveMap (Task foundTask, MoveSprintTask moveSprintTask,
                                   HashMap<Task, Sprint> taskDirectlyMoveMap, HashMap<Long, Sprint> taskDependencyMoveMap,
                                   HashMap<Long, Task> childToParentMap, HashMap<Long, Sprint> parentToSprintMap, HashSet<MoveSprintTask> moveSprintTaskList,
                                   Boolean onComplete, HashMap<Long, Sprint> sprintMap, HashMap<Long, Task> allTaskMap) {

        if (Objects.equals(foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)
                || Objects.equals(foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new IllegalStateException("User not allowed to move " + foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " Work Item from sprint." +
                    " Work Item number : " + foundTask.getTaskNumber());
        }

        List<Task> childTaskList = new ArrayList<>();
        if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            childTaskList.addAll(taskRepository.findByParentTaskId(foundTask.getTaskId()));
        }
        if (Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && !childTaskList.contains(foundTask)
                && !onComplete) {
            throw new IllegalStateException("User not allowed to remove child task from sprint");
        }
        if (moveSprintTask.getNewSprintId() != null) {
            Sprint foundSprint = sprintMap.get(moveSprintTask.getNewSprintId());
            if (foundSprint == null) {
                Optional<Sprint> foundSprintOptional = sprintRepository.findById(moveSprintTask.getNewSprintId());
                if (foundSprintOptional.isEmpty()) {
                    throw new ValidationException("Sprint not found");
                }
                foundSprint = foundSprintOptional.get();
                sprintMap.put(foundSprint.getSprintId(), foundSprint);
            }

            if (!childTaskList.isEmpty()) {
                if (!(foundTask.getDependencyIds() != null && !foundTask.getDependencyIds().isEmpty()
                        && foundTask.getCountChildExternalDependencies() == 0))
                    for (Task childTask : childTaskList) {
                        if (!childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)
                                && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                            MoveSprintTask moveSprintTaskChild = new MoveSprintTask(moveSprintTask.getPreviousSprintId(), childTask.getTaskId(), foundTask.getSprintId());
                            if (!moveSprintTaskList.contains(moveSprintTaskChild)) {
                                Task childTaskCopy = new Task();
                                BeanUtils.copyProperties(childTask, childTaskCopy);
                                if (foundTask.getCountChildExternalDependencies() > 0 || foundTask.getCountChildInternalDependencies() > 0 || (!Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && foundTask.getDependencyIds() != null && !foundTask.getDependencyIds().isEmpty()))
                                    taskDependencyMoveMap.put(childTaskCopy.getTaskId(), foundSprint);
                                else
                                    taskDirectlyMoveMap.put(childTaskCopy, foundSprint);
                            }
                        }
                        allTaskMap.put(childTask.getTaskId(), childTask);
                        childToParentMap.put(childTask.getTaskId(), foundTask);
                    }
            }
            if (foundTask.getCountChildExternalDependencies() > 0 || foundTask.getCountChildInternalDependencies() > 0
                    || (foundTask.getDependencyIds() != null && !foundTask.getDependencyIds().isEmpty())) {
                if (!Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)
                        || foundTask.getDependencyIds() != null && !foundTask.getDependencyIds().isEmpty()) {
                    taskDependencyMoveMap.put(foundTask.getTaskId(), foundSprint);
                    if(Objects.equals(foundTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK))
                        parentToSprintMap.put(foundTask.getTaskId(), foundSprint);
                }
                else
                    parentToSprintMap.put(foundTask.getTaskId(), foundSprint);
            }
            else
                taskDirectlyMoveMap.put(foundTask, foundSprint);
        } else {
            if (!childTaskList.isEmpty()) {
                for (Task childTask : childTaskList) {
                    if (!childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                        MoveSprintTask moveSprintTaskChild = new MoveSprintTask(moveSprintTask.getPreviousSprintId(), childTask.getTaskId(), null);
                        if (!moveSprintTaskList.contains(moveSprintTaskChild)) {
                            Task childTaskCopy = new Task();
                            BeanUtils.copyProperties(childTask, childTaskCopy);
                            taskDirectlyMoveMap.put(childTaskCopy, null);
                        }
                    }
                    allTaskMap.put(childTask.getTaskId(), childTask);
                    childToParentMap.put(childTask.getTaskId(),foundTask);
                }
            }
            taskDirectlyMoveMap.put(foundTask, null);
        }
    }

    private void updateHoursOfSprint (Sprint prevSprint, Sprint sprint, Task task) {
        if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
            prevSprint.setHoursOfSprint(prevSprint.getHoursOfSprint() != null ? (prevSprint.getHoursOfSprint() - task.getTaskEstimate()) : 0);
        }

        if (sprint != null && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && !Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            sprint.setHoursOfSprint((sprint.getHoursOfSprint() != null ? sprint.getHoursOfSprint() : 0) + task.getTaskEstimate());
        }
    }

    private void processTaskWithSprint (Task task, Sprint sprint, HashMap<Long, Task> childToParentMap, String message, Boolean onComplete, Boolean isDependentTask, TaskProcessingContext taskProcessingContext, Boolean isSprint2task, HashMap<Long, Sprint> sprintMap, UserAccount userAccount) {
        Task foundTaskCopy = new Task();
        BeanUtils.copyProperties(task, foundTaskCopy);
        Sprint prevSprint = sprintMap.get(task.getSprintId());
        if (prevSprint == null) {
            prevSprint = sprintRepository.findBySprintId(task.getSprintId());
            sprintMap.put(task.getSprintId(), prevSprint);
        }
        if(isDependentTask && sprint != null && sprint.getSprintExpStartDate().isBefore(prevSprint.getSprintExpStartDate()))
            throw new IllegalStateException("Dependency work items cannot be moved into a sprint that is before the current sprint. Remove the work item from the current sprint and add it to the other sprint.");
        List<String> updatedFields = new ArrayList<>();
        if (task.getTaskEstimate() != null) {
            updateHoursOfSprint(prevSprint, sprint, task);
        }
        LocalTime sprintCompleteTimeLimit = entityPreferenceService.getOfficeEndTime(task.getFkOrgId().getOrgId(), task.getFkTeamId().getTeamId()).minusHours(entityPreferenceService.getCapacityLimit(task.getFkOrgId().getOrgId(), task.getFkTeamId().getTeamId()));
        if (sprint != null) {
            if (isDependentTask) {
                message = validateSprintConditionAndModifyDependencyTaskProperties(task, sprint, childToParentMap.get(task.getTaskId()), taskProcessingContext.getIsVisited(), taskProcessingContext.getProcessedTasks(), taskProcessingContext.getTaskEstimates(), taskProcessingContext.getTaskEstimatesLastDate(), taskProcessingContext.getParentExpStartDate(), taskProcessingContext.getParentExpEndDate(), taskProcessingContext.getSprint2TasksToProcess(), taskProcessingContext.getChildTasksWithParentDependencies());
            } else if (isSprint2task) {
                if (task.getTaskTypeId() == Constants.TaskTypes.PARENT_TASK && (task.getDependencyIds() == null || task.getDependencyIds().isEmpty())) {
                    message = validateSprintConditionAndModifyDependencyTaskProperties(task, sprint, null, taskProcessingContext.getIsVisited(), taskProcessingContext.getProcessedTasks(), taskProcessingContext.getTaskEstimates(), taskProcessingContext.getTaskEstimatesLastDate(), taskProcessingContext.getParentExpStartDate(), taskProcessingContext.getParentExpEndDate(), taskProcessingContext.getSprint2TasksToProcess(), taskProcessingContext.getChildTasksWithParentDependencies());
                } else {
                    if (task.getDependencyIds() == null || task.getDependencyIds().isEmpty())
                        message = validateSprintConditionAndModifyTaskProperties(task, sprint, childToParentMap.get(task.getTaskId()));
                    else {
                        message = validateSprintConditionAndModifyDependencyTaskProperties(task, sprint, childToParentMap.get(task.getTaskId()), taskProcessingContext.getIsVisited(), taskProcessingContext.getProcessedTasks(), taskProcessingContext.getTaskEstimates(), taskProcessingContext.getTaskEstimatesLastDate(), taskProcessingContext.getParentExpStartDate(), taskProcessingContext.getParentExpEndDate(), taskProcessingContext.getSprint2TasksToProcess(), taskProcessingContext.getChildTasksWithParentDependencies());
                    }
                }
            } else {
                if (task.getTaskTypeId() == Constants.TaskTypes.PARENT_TASK)
                    message = validateSprintConditionAndModifyTaskProperties(task, sprint, null);
                else {
                    Task parentTask = childToParentMap.get(task.getTaskId());
                    message = validateSprintConditionAndModifyTaskProperties(task, sprint, parentTask);
                }
            }
            if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK))
                capacityService.updateMovedCapacity(task);
            if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && !((LocalDate.now().isAfter(prevSprint.getSprintExpEndDate().toLocalDate()) || (LocalDate.now().isEqual(prevSprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit))) && Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()))) {
                capacityService.removeTaskFromSprintCapacityAdjustment(task);
            }
            if (onComplete ||
                    (((LocalDate.now().isAfter(prevSprint.getSprintExpEndDate().toLocalDate()) || (LocalDate.now().isEqual(prevSprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit))) && Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())))) {
                Set<Long> prevSprints = new HashSet<>(task.getPrevSprints());
                prevSprints.add(prevSprint.getSprintId());
                List<Long> prevSprintList = new ArrayList<>(prevSprints);
                task.setPrevSprints(prevSprintList);
            }
            if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                handleBacklogTasksForStartedSprint(task, updatedFields);
            }
            task.setSprintId(sprint.getSprintId());
            capacityService.updateUserAndSprintCapacityMetricsOnAddTaskToSprint(task, sprint.getSprintId());
        } else {
            if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK))
                capacityService.updateMovedCapacity(task);
            if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && !((LocalDate.now().isAfter(prevSprint.getSprintExpEndDate().toLocalDate()) || (LocalDate.now().isEqual(prevSprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit))) && Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())))
                capacityService.removeTaskFromSprintCapacityAdjustment(task);
            if (onComplete || (((LocalDate.now().isAfter(prevSprint.getSprintExpEndDate().toLocalDate()) || (LocalDate.now().isEqual(prevSprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit))) && Objects.equals(prevSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())))) {
                Set<Long> prevSprints = new HashSet<>(task.getPrevSprints());
                prevSprints.add(prevSprint.getSprintId());
                List<Long> prevSprintList = new ArrayList<>(prevSprints);
                task.setPrevSprints(prevSprintList);
            }
            task.setSprintId(null);
            message = "Task removed from sprint successfully.";
            task.setIsSprintChanged(true);
        }
        if (task.getTaskProgressSystem() != null && (Objects.equals(task.getTaskProgressSystem(), StatType.DELAYED)
                || (Objects.equals(task.getTaskProgressSystem(), StatType.WATCHLIST)
                && !Objects.equals(task.getTaskPriority(), Constants.PRIORITY_P0)
                && !Objects.equals(task.getTaskPriority(), Constants.PRIORITY_P1)))) {
            taskService.computeAndUpdateStatForTask(task, true);
        }
        Integer noOfAudit = 0;
        noOfAudit++;
        updatedFields.add(Constants.TaskFields.SPRINT_ID);
        Audit auditCreated = auditService.createAudit(task, noOfAudit, task.getTaskId(), Constants.TaskFields.SPRINT_ID);
        auditRepository.save(auditCreated);
        taskHistoryService.addTaskHistoryOnUserUpdate(foundTaskCopy);
        task.setFkAccountIdLastUpdated(userAccount);
        Task savedTask = taskRepository.save(task);
        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, savedTask);
    }

    private void handleBacklogTasksForStartedSprint (Task task, List<String> updatedFields) {
        String errorMessage = "Following fields are missing in task " + task.getTaskNumber();
        boolean firstConcat = true;
        if (task.getTaskPriority() == null) {
            errorMessage += " priority";
            firstConcat = false;

        }
        if (task.getFkAccountIdAssigned() == null) {
            if (!firstConcat) {
                errorMessage += ",";
            }
            errorMessage += " assigned to";
        }
        if (task.getTaskEstimate() == null) {
            if (!firstConcat) {
                errorMessage += ",";
            }
            errorMessage += " estimate";
        }
        if (task.getTaskPriority() == null || task.getFkAccountIdAssigned() == null || task.getTaskEstimate() == null) {
            throw new IllegalStateException(errorMessage);
        }
        WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, task.getTaskWorkflowId());
        task.setFkWorkflowTaskStatus(workFlowTaskStatus);
        task.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
        updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
        taskServiceImpl.computeAndUpdateStatForTask(task, true);
        updatedFields.add(Constants.TaskFields.TASK_STATE);
    }

    @Deprecated
    public Boolean addSprintDatesToTask_Old (Task task, Sprint sprint, Boolean isTaskInActiveSprintState, Task parentTask) throws ValidationFailedException {
        Boolean datesAdjusted = false;
        Epic epic = null;
        if (task.getFkEpicId() != null) {
            epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
        }
        if (isTaskInActiveSprintState) {
            // Validate and adjust task's expected end date
            if (task.getTaskExpEndDate() == null || task.getTaskExpEndDate().isBefore(sprint.getSprintExpStartDate()) || task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())) {
                if (epic != null) {
                    if ((epic.getExpEndDateTime() != null && sprint.getSprintExpEndDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprint.getSprintExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                        throw new ValidationFailedException("Work Item's Expected End Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                    }
                }
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
//                    Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                    if (parentTask.getTaskExpEndDate() == null || parentTask.getTaskExpEndDate().isBefore(sprint.getSprintExpStartDate()) || parentTask.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())) {
                        task.setTaskExpEndDate(sprint.getSprintExpEndDate());
                        task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
                    }
                    else {
                        task.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                        task.setTaskExpEndTime(parentTask.getTaskExpEndTime());
                    }
                    datesAdjusted = true;
                }
                else {
                    task.setTaskExpEndDate(sprint.getSprintExpEndDate());
                    task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
                    datesAdjusted = true;
                }
            }

            if ((!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE.toLowerCase()) && (task.getTaskExpStartDate() == null || task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || task.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())))
                    || (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE.toLowerCase()) && !task.getTaskExpStartDate().isBefore(sprint.getSprintExpEndDate()))) {
                if (epic != null) {
                    if ((epic.getExpEndDateTime() != null && sprint.getSprintExpStartDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprint.getSprintExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                        throw new ValidationFailedException("Work Item's Expected Start Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                    }
                }
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
//                    Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                    if(parentTask.getTaskActStDate()==null) {
                        if (parentTask.getTaskExpStartDate() == null || parentTask.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || parentTask.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())) {
                            task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                            task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
                        } else {
                            task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                            task.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                        }
                        datesAdjusted = true;
                    }
                    else if(parentTask.getTaskActStDate().isBefore(sprint.getSprintExpEndDate())) {
                        if(task.getTaskActStDate()==null) {
                            task.setTaskExpStartDate(sprint.getSprintExpStartDate().isAfter(parentTask.getTaskExpStartDate()) ? sprint.getSprintExpStartDate() : parentTask.getTaskExpStartDate());
                            task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                            datesAdjusted = true;
                        }
                        else if(task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate()));
                        else throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
                    }
                    else throw new ValidationFailedException("For a Child Task with a Started Parent Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
                }
                else if(!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE.toLowerCase())) {
                    task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                    task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
                    datesAdjusted = true;
                }
                else if(task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate()));
                else throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
            }
            task.setIsSprintChanged(true);

        }
        else {
            // Handle tasks in Not Started or Backlog states
            // Adjust start/end date if not within the sprint's dates
            if ((!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE.toLowerCase())
                    && (task.getTaskExpStartDate() == null || task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate())
                    || task.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())))
                    || (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE.toLowerCase())
                    && !task.getTaskExpStartDate().isBefore(sprint.getSprintExpEndDate()))) {
                if (epic != null) {
                    if ((epic.getExpEndDateTime() != null && sprint.getSprintExpStartDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprint.getSprintExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                        throw new ValidationFailedException("Work Item's Expected Start Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                    }
                }
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
//                    Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                    if(parentTask.getTaskActStDate()==null) {
                        if (parentTask.getTaskExpStartDate() == null || parentTask.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || parentTask.getTaskExpStartDate().isAfter(sprint.getSprintExpEndDate())) {
                            task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                            task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
                        } else {
                            task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                            task.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                        }
                        datesAdjusted = true;
                    }
                    else if(parentTask.getTaskActStDate().isBefore(sprint.getSprintExpEndDate())) {
                        if(task.getTaskActStDate() == null) {
                            task.setTaskExpStartDate(sprint.getSprintExpStartDate().isAfter(parentTask.getTaskExpStartDate()) ? sprint.getSprintExpStartDate() : parentTask.getTaskExpStartDate());
                            task.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                            datesAdjusted = true;
                        }
                        else if(task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate()));
                        else throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
                    }
                    else throw new ValidationFailedException("For a Child Task with a Started Parent Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
                }
                else if(task.getTaskActStDate()==null) {
                    task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                    task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
                    datesAdjusted = true;
                }
                else if(task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate()));
                else throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
            }
            else if(task.getTaskActStDate() ==null) {
//                task.setTaskExpStartDate(sprint.getSprintExpStartDate());
//                task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
//                datesAdjusted = true;
            }
            else if(task.getTaskActStDate().isBefore(sprint.getSprintExpEndDate()));
            else throw new ValidationFailedException("For a Started Work Item, the Expected End Date of the sprint should be after the Actual Start Date of the Work Item.");
            if (task.getTaskExpEndDate() == null || task.getTaskExpEndDate().isBefore(sprint.getSprintExpStartDate()) || task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())) {
                if (epic != null) {
                    if ((epic.getExpEndDateTime() != null && sprint.getSprintExpEndDate().isAfter(epic.getExpEndDateTime())) || (epic.getExpStartDateTime() != null && sprint.getSprintExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                        throw new ValidationFailedException("Work Item's Expected End Date Time is not falling within the Sprint's or Epic's Expected Start and End Date Time");
                    }
                }
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
//                    Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                    if (parentTask.getTaskExpEndDate() == null || parentTask.getTaskExpEndDate().isBefore(sprint.getSprintExpStartDate()) || parentTask.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())) {
                        task.setTaskExpEndDate(sprint.getSprintExpEndDate());
                        task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
                    }
                    else {
                        task.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                        task.setTaskExpEndTime(parentTask.getTaskExpEndTime());
                    }
                    datesAdjusted = true;
                }
                else {
                    task.setTaskExpEndDate(sprint.getSprintExpEndDate());
                    task.setTaskExpEndTime(sprint.getSprintExpEndDate().toLocalTime());
                    datesAdjusted = true;
                }
            }
        }
        return datesAdjusted;
    }

    public void removeMemberFromAllSprintInTeam (Long teamId, Long removedMember, List<Long> accountIdOfRequester) throws IllegalAccessException {
        List<Sprint> sprintList = sprintRepository.findByEntityTypeIdAndEntityIdAndSprintStatusIn(Constants.EntityTypes.TEAM, teamId, List.of(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId()));
        if (sprintList != null && !sprintList.isEmpty()) {
            for (Sprint sprint : sprintList) {
                RemoveSprintMemberRequest removeSprintMemberRequest = new RemoveSprintMemberRequest();
                removeSprintMemberRequest.setSprintId(sprint.getSprintId());
                removeSprintMemberRequest.setRemovedMemberAccountId(removedMember);
                removeMemberFromSprint(removeSprintMemberRequest, accountIdOfRequester, false);
            }
        }
    }

    // ZZZZZZ 14-04-2025
    public void addMemberInAllSprintInTeam (Long teamId, Long addedMemberAccountId, List<Long> accountIdOfRequester, String timeZone) throws IllegalAccessException {
        List<Sprint> sprintList = sprintRepository.findByEntityTypeIdAndEntityIdAndSprintStatusIn(Constants.EntityTypes.TEAM, teamId, List.of(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId()));
        if (sprintList != null && !sprintList.isEmpty()) {
            for (Sprint sprint : sprintList) {
                AddedSprintMemberRequest addedSprintMemberRequest = new AddedSprintMemberRequest();
                addedSprintMemberRequest.setSprintId(sprint.getSprintId());
                addedSprintMemberRequest.setAddedMemberAccountIds(List.of(addedMemberAccountId));
                // ZZZZZZ 14-04-2025
                addMemberInSprint(addedSprintMemberRequest, accountIdOfRequester, false, timeZone);
            }
        }
    }

    public SprintCardActionResponse fetchWorkItemFieldsForSprintByRoles(Integer entityTypeId, Long entityId, String accountIds) {

        //we are only implementing this at Team Level only, it can be implemented at Project or Org level in the future.
        Long accountId = Long.parseLong(accountIds);
        Team team = teamRepository.findByTeamId(entityId);
        List<Integer> actionFieldsResponseList = new ArrayList<>();
        List<Integer> actionEssentialFieldsResponseList = new ArrayList<>();
        SprintCardActionResponse response = new SprintCardActionResponse(actionFieldsResponseList, actionEssentialFieldsResponseList);

        if (team == null) {
            throw new ValidationFailedException("Requested Team is not Valid!");
        }
        List<Integer> allowedRolesDb = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(entityTypeId, entityId, accountId, true)
                .parallelStream().map(AccessDomain::getRoleId).collect(Collectors.toList());

        if (allowedRolesDb.isEmpty()) {
            throw new ValidationFailedException("You are not part of a Team!");
        }
        Long projectId = team.getFkProjectId().getProjectId();
        Long orgId = team.getFkOrgId().getOrgId();

        Boolean isProjectManagerSprint = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.PROJECT, projectId, accountId, true, List.of(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId()));
        Boolean isOrgAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.ORG, orgId, accountId, true, List.of(RoleEnum.ORG_ADMIN.getRoleId()));

        if (isProjectManagerSprint) {
            allowedRolesDb.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        }
        if (isOrgAdmin) {
            allowedRolesDb.add(RoleEnum.ORG_ADMIN.getRoleId());
        }

        //gather all the allowed roles for FLAGGING (STARRING) at the Org preference level.
        List<Integer> flagAuthorizedRoles = new ArrayList<>();
        Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (orgPreference.isPresent()) {
            flagAuthorizedRoles = orgPreference.get().getStarringWorkItemRoleIdList();
        }

        for (SprintActionFieldsEnum action : SprintActionFieldsEnum.values()) {
            if (action.getAllowedRoleIds() != null) {
                if (action.equals(SprintActionFieldsEnum.PRIORITY) || action.equals(SprintActionFieldsEnum.ASSIGNED_TO) || action.equals(SprintActionFieldsEnum.ESTIMATE_CHANGES)) {
                    // idea is we are not showing this data at Work Item level but at Team and Based on Role so,
                    // we are using this isSelfAssigned indicator such that when in sprint member selected itself then its true and those fields visible only for self work item.
                    List<Integer> otherAssignedFieldsRoles = new ArrayList<>(action.getAllowedRoleIds());
                    otherAssignedFieldsRoles.removeIf(role -> role < 10);
                    if (allowedRolesDb.parallelStream().anyMatch(otherAssignedFieldsRoles::contains)) {
                        actionFieldsResponseList.add(action.getFieldId());
                    }

                    if (allowedRolesDb.parallelStream().anyMatch(action.getAllowedRoleIds()::contains)) {
                        actionEssentialFieldsResponseList.add(action.getFieldId());
                    }
                } else if (action.getAllowedRoleIds().parallelStream().anyMatch(allowedRolesDb::contains)) {
                    actionFieldsResponseList.add(action.getFieldId());
                }
            } else if (action.equals(SprintActionFieldsEnum.FLAGGING) && flagAuthorizedRoles.parallelStream().anyMatch(allowedRolesDb::contains)) {
                actionFieldsResponseList.add(action.getFieldId());
            }
        }
        return response;
    }

    public void updateTasksMeetingDateUpdatingSprint (Task task, SprintRequest sprintRequest) {
        List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());
        if (meetingList != null && !meetingList.isEmpty()) {
            for (Meeting meeting : meetingList) {
                if (task.getTaskActStDate() != null && meeting.getAttendeeList().parallelStream().anyMatch(attendee -> attendee.getAttendeeDuration() != null)) {
                    continue;
                }
                if(meeting.getStartDateTime().isBefore(sprintRequest.getSprintExpStartDate()) || meeting.getEndDateTime().isAfter(sprintRequest.getSprintExpEndDate())){
                    throw new ValidationFailedException("To update sprint, please remove the reference meeting " + meeting.getMeetingNumber() +
                            " from the work item " + task.getTaskNumber() + " or remove the work item from the sprint, make appropriate changes" +
                            " to the dates of the work item and/or the meeting, and then try to update the sprint.");
                }
            }
        }
    }
}
