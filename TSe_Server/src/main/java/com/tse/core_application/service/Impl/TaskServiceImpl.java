package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.pathtemplate.ValidationException;
import com.opencsv.CSVWriter;
import com.tse.core_application.config.DebugConfig;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.RelationDirection;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.custom.model.childbugtask.ChildTask;
import com.tse.core_application.custom.model.childbugtask.LinkedTask;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.AiMLDtos.AiDuplicateWorkItemDto;
import com.tse.core_application.dto.AiMLDtos.AiWorkItemDescResponse;
import com.tse.core_application.dto.dependency.LagTimeResponse;
import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.exception.*;
import com.tse.core_application.exception.NumberFormatException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.specification.OpenTaskSpecification;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.InvalidRelationTypeException;
import javax.naming.TimeLimitExceededException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tse.core_application.utils.CommonUtils.containsAny;

@Service
@Transactional
public class TaskServiceImpl {

    private static final Logger logger = LogManager.getLogger(TaskServiceImpl.class.getName());

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private TimeSheetService timeSheetService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private TimeSheetRepository timeSheetRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkflowTaskStatusService workflowTaskStatusService;

    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;

    @Autowired
    private StatsService statsService;

    @Autowired
    private OfficeHoursService officeHoursService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private FCMService fcmService;
    @Autowired
    private FirebaseTokenService firebaseTokenService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private DeliverablesDeliveredService deliverablesDeliveredService;
    @Autowired
    private TaskHistoryService taskHistoryService;
    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private EntityTypeRepository entityTypeRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    NotificationService notificationService;
    @Autowired
    CustomEnvironmentRepository environmentRepository;
    @Autowired
    SeverityRepository severityRepository;
    @Autowired
    private DependencyRepository dependencyRepository;
    @Autowired
    private DependencyService dependencyService;
    @Autowired
    ResolutionRepository resolutionRepository;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private AttendeeService attendeeService;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CapacityService capacityService;
    @Autowired
    private RecurrenceService recurrenceService;
    @Autowired
    private ActionService actionService;
    @Autowired
    private PersonalTaskService personalTaskService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private TableColumnsTypeService tableColumnsTypeService;
    @Autowired
    private OpenFireService openFireService;
    @Autowired
    private EpicRepository epicRepository;
    @Autowired
    private EpicService epicService;
    @Autowired
    private WorkFlowEpicStatusRepository workFlowEpicStatusRepository;
    @Autowired
    private EpicTaskRepository epicTaskRepository;
    @Autowired
    private SprintCapacityMetricsRepository sprintCapacityMetricsRepository;
    @Autowired
    private UserCapacityMetricsRepository userCapacityMetricsRepository;
    @Autowired
    private ReleaseVersionRepository releaseVersionRepository;

    @Value("${default.effort.edit.time.task}")
    private Integer defaultTaskEffortEditTime;

    @Value("${default.effort.edit.time.meeting}")
    private Integer defaultMeetingEffortEditTime;

    @Value("${personal.org.id}")
    private Long personalOrgId;

    @Value("${tse.search.multiplier}")
    private double similarityThreshold;

    @Value("${enable.openfire}")
    private Boolean enableOpenfire;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CustomEnvironmentRepository customEnvironmentRepository;

    @Autowired
    private AiMlService aiMlService;

    //  ################## FCM Notification Section #######################################
    public List<Long> getUserIdsOfHigherRoleMembersInTeam(Long teamId) {

        List<Integer> roleIds = Constants.HIGHER_ROLE_IDS;
        List<AccountId> accountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, roleIds, true);
        List<Long> accountIdValueList = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toList());

        return userAccountService.getUserIdsFromAccountIds(accountIdValueList);
    }

    public List<Long> getUserIdsOfRoleMembersWithEditEffortAccess(Long teamId) {

        List<Integer> roleIds = Constants.ROLES_WITH_TEAM_EFFORT_EDIT_ACCESS;
        List<AccountId> accountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, roleIds, true);
        List<Long> accountIdValueList = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toList());

        return userAccountService.getUserIdsFromAccountIds(accountIdValueList);
    }

    public List<Long> getAccountIdsOfRoleMembersWithEditEffortAccess(Long teamId) {

        List<Integer> roleIds = Constants.ROLES_WITH_TEAM_EFFORT_EDIT_ACCESS;
        List<AccountId> accountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, roleIds, true);
        List<Long> result=new ArrayList<>();
        if(!accountIds.isEmpty()) {
            for (AccountId accountId : accountIds)
                result.add(accountId.getAccountId());
        }
        return result;
    }

    /**
     * method to send FCM Notification. This is helper method for sendPushNotification method.
     */
    public void sendFcmNotification(Long userId, HashMap<String, String> payload) {
        List<FirebaseToken> firebaseTokenListDb = firebaseTokenService.getFirebaseTokenByUserId(userId);

        if (firebaseTokenListDb != null) {
            for (FirebaseToken firebaseToken : firebaseTokenListDb) {
//                String token = firebaseToken.getToken();
                PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
                pushNotificationRequest.setPayload(payload);
                pushNotificationRequest.setTargetToken(firebaseToken);
                pushNotificationRequest.setDeviceType(firebaseToken.getDeviceType());

                try {
                    String messageSentResponse = fcmService.sendMessageToToken(pushNotificationRequest);
                } catch (Exception e) {
                    throw new FirebaseNotificationException("Error sending fcm notification");
                }
            }
        }
    }

    /**
     * This method sends push notification for addTask/ updateTask to different accounts in the list of payload.
     * It uses sendFCMNotification method to send push notification.
     *
     * @param payloadList
     */
    public void sendPushNotification(List<HashMap<String, String>> payloadList) {
        if (payloadList != null && !payloadList.isEmpty()) {
            for (HashMap<String, String> payload : payloadList) {
                Long accountIdOfUser = Long.parseLong(payload.get("accountId"));
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountIdOfUser, true);
                if (userAccount == null) {
                    continue;
                }
                Long userIdOfUser = userAccount.getFkUserId().getUserId();
                UserPreferenceDTO userPreferenceDTO = userPreferenceService.getUserPreference(userIdOfUser);
                List<Integer> notificationCategoryIds = new ArrayList<>();
                if (userPreferenceDTO != null && userPreferenceDTO.getNotificationCategoryIds() != null && !userPreferenceDTO.getNotificationCategoryIds().isEmpty()) {
                    notificationCategoryIds = userPreferenceDTO.getNotificationCategoryIds();
                }
                Integer categoryIdInPayload = payload.get("categoryId") != null ? Integer.parseInt(payload.get("categoryId")) : 0;
                if (notificationCategoryIds.contains(categoryIdInPayload)) {
                    try {
                        sendFcmNotification(userIdOfUser, payload);
                    } catch (FirebaseNotificationException e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("Something went wrong: Not able to send notification for new Work Item to userId# " + userIdOfUser + e, new Throwable(allStackTraces));
                    }
                }
            }
        }
    }
    //  ################## FCM Notification Section ENDS #######################################


    public Task addTaskInTaskTable(Task task, String timeZone) {
        setBUToTask(task);

        Boolean isSprintAdded = false;
        Boolean isEpicAdded = false;
        Boolean isLabelAdded = false;
        List<String> updateFields = new ArrayList<>();

        // if we are creating a bug task then we have to validate the bug task and do required changes
        if (task.getIsBug() != null && task.getIsBug()) {
            List<Long> linkedTaskIds = validateBugTaskAndGetUpdatedLinkedTaskIds(task);
            task.setBugTaskRelation(linkedTaskIds);
        }

        if (task.getReleaseVersionName() != null) {
            if (task.getLabelsToAdd() != null) {
                task.getLabelsToAdd().add(task.getReleaseVersionName());
            }
            else {
                task.setLabelsToAdd(List.of(task.getReleaseVersionName()));
            }
            createOrUpdateReleaseVersionOfTask (null, task);
        }

        if (task.getLabelsToAdd() != null && !task.getLabelsToAdd().isEmpty()) {
            isLabelAdded = true;
            updateFields.add(Constants.TaskFields.LABELS);
        }

        if (task.getFkEpicId() != null) {
            isEpicAdded = true;
            updateFields.add(Constants.TaskFields.EPIC_ID);
        }
        if (task.getSprintId() != null) {
            isSprintAdded = true;
            updateFields.add(Constants.TaskFields.SPRINT_ID);
            capacityService.updateUserAndSprintCapacityMetricsOnAddTaskToSprint(task, task.getSprintId());
        }

        task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId()));
        addLabelToTask(task);
        Task taskAdded = taskRepository.save(task);

        boolean isDependenciesUpdated = false;
        if (task.getDependentTaskDetailRequestList() != null && !task.getDependentTaskDetailRequestList().isEmpty()) {
            List<Long> dependencyIds = validateAndCreateTaskDependenciesOnAddTask(task);
//            taskHistoryService.addTaskHistoryOnSystemUpdate(taskAdded);
            taskAdded.setDependencyIds(dependencyIds);
//            taskAdded = taskRepository.save(task);
//            taskHistoryMetadataService.addTaskHistoryMetadataBySystemUpdate(List.of(Constants.TaskFields.DEPENDENCY_IDS), taskAdded);
            isDependenciesUpdated = true;
        }

        if (!Constants.WorkFlowStatusIds.BACKLOG.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId()) || isDependenciesUpdated || isLabelAdded || isSprintAdded || isEpicAdded) {
            Task taskCopy = new Task();
            BeanUtils.copyProperties(task, taskCopy);
            taskCopy.setFkEpicId(null);
            taskCopy.setSprintId(null);
            taskHistoryService.addTaskHistoryOnSystemUpdate(taskCopy);

            if (isDependenciesUpdated) {
                updateFields.add(Constants.TaskFields.DEPENDENCY_IDS);
            }

            if (!Constants.WorkFlowStatusIds.BACKLOG.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                statsService.computeAndUpdateStatForAddTask(taskAdded);
                updateFields.add("taskProgressSystem");
                updateFields.add("taskProgressSystemLastUpdated");
            }
//            Task reSavedTask = taskRepository.save(taskAdded);
            Task tempTask = new Task();
            BeanUtils.copyProperties(taskAdded, tempTask);
            tempTask.setVersion(tempTask.getVersion() + 1);
            taskHistoryMetadataService.addTaskHistoryMetadataBySystemUpdate(updateFields, tempTask);
        }

        if (task.getFkEpicId() != null && Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
            modifyEpicPropertiesOnChildTaskCreation(taskAdded);
        }

        // if we are saving a child Task then we have to update the Parent Task as well
        if (taskAdded.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
            modifyParentTaskForNewChildTask(taskAdded);
        }

        if (taskAdded.getIsBug() != null && taskAdded.getIsBug()) {
            modifyBugTaskRelationOfLinkedTasks(taskAdded);
        }

        List<HashMap<String, String>> payload = notificationService.createNewTaskNotification(task);
        sendPushNotification(payload);

        //sending notification for Immediate Attention
        if (!Objects.equals(task.getImmediateAttention(), 0) && task.getImmediateAttentionFrom() != null && task.getImmediateAttentionReason() != null) {
            addImmediateAttentionAlert(task, timeZone);
        }

        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            List<Note> notesAdded = noteService.saveAllNotesOnAddTask(task.getNotes(), taskAdded);
            Integer resultSet = updateNoteIdByTaskId(notesAdded.get(0).getNoteId(), taskAdded.getTaskId());
        }

        int noOfAudit = 1;
        if (task.getFkAccountIdAssigned() != null && task.getFkAccountIdAssigned().getAccountId() != null) {
            noOfAudit = 2;
        }
        if (noOfAudit == 1) {
            Audit auditAdd = auditRepository.save(auditService.createAudit(taskAdded, 1, null, null));
        } else {
            Audit auditAdd1 = auditRepository.save(auditService.createAudit(taskAdded, 1, null, null));
            Audit auditAdd2 = auditRepository.save(auditService.createAudit(taskAdded, 2, null, null));
        }
//        transactionalTestMethod();
        return taskAdded;
    }


    /**
     * process label and add label to task and save labels in the label repository
     */
    public void addLabelToTask(Task task) {
        if (task.getLabelsToAdd() == null || task.getLabelsToAdd().isEmpty()) return;

        List<Label> currentLabels = task.getLabels();
        if (currentLabels == null) {
            currentLabels = new ArrayList<>();
        }

        List<Label> newLabels = new ArrayList<>();
        List<String> labelsInRequest = new ArrayList<>();
        for (String labelName : task.getLabelsToAdd()) {
            final String formattedLabelName = labelName.trim().replaceAll("\\s+", " ");
//            final String formattedLabelName = Arrays.stream(labelName.split("\\b")).map(word -> word.isEmpty() ? word :
//                            Character.toUpperCase(word.charAt(0)) + word.substring(1))
//                    .collect(Collectors.joining(""));

            // Check if the label is already associated with the task
            boolean labelExists = currentLabels.stream()
                    .anyMatch(label -> label.getLabelName().equalsIgnoreCase(formattedLabelName)) || labelsInRequest.stream()
                    .anyMatch(label -> label.equalsIgnoreCase(formattedLabelName));

            labelsInRequest.add(formattedLabelName);

            if (!labelExists) {
                // If not, check if the label exists in the database
                Label label = labelRepository.findByLabelNameIgnoreCaseAndEntityTypeIdAndEntityId(formattedLabelName,Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
                if (label == null) {
                    label = new Label();
                    label.setLabelName(formattedLabelName);
                    label.setEntityId(task.getFkTeamId().getTeamId());
                    label.setEntityTypeId(Constants.EntityTypes.TEAM);
                    label.getTasks().add(task);
                    label = labelRepository.save(label);
                } else {
                    label.getTasks().add(task);
                }
                newLabels.add(label); // Add the label to the new labels list
            }
        }

        // Combine the current labels with the new labels
        currentLabels.addAll(newLabels);
        task.setLabels(currentLabels); // Set the task's labels to the combined list
        // Assuming you have a method to update a task, you would call it here, passing the modified task object
//        taskRepository.save(task);
    }


    /**
     * This method calls multiple functions to set properties to a new task request before persisting the task in the task table
     */
    public void modifyNewTaskProperties(Task task, String timeZone) {
        setDefaultExpTime(task);
        convertTaskAllUserDateAndTimeInToServerTimeZone(task, timeZone);
        setTaskStateByWorkflowTaskStatus(task);
        setDefaultCurrentActivityIndicator(task);
        // Normalise Reported by field
        if (task.getFkAccountIdBugReportedBy() != null) {
            if (task.getFkAccountIdBugReportedBy().getAccountId() == null || !(Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.BUG_TASK))) {
                task.setFkAccountIdBugReportedBy(null);
            }
        }
        if (Objects.equals(task.getIsBallparkEstimate(), Constants.BooleanValues.BOOLEAN_FALSE)) {
            task.setIsBallparkEstimate(null);
        }
        task.setIsRcaDone(null);
        task.setRcaReason(null);
        task.setRcaIntroducedBy(null);
        validateAndNormalizeRca(task);

        modifyTaskPriority(task);
        updateCurrentlyScheduledTaskIndicatorForTask(task);

        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.BUG_TASK)) {
            task.setIsBug(true);
        }
    }

    public void setDefaultExpTime(Task task) {
        if (task.getTaskPriority() != null) {
            String priority = task.getTaskPriority().substring(0, 2);
//            Integer workflowId = task.getTaskWorkflowId();
            if ((task.getTaskExpEndTime() == null && task.getTaskExpEndDate() != null) || (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() == null)) {
                if (priority.equals(Constants.PRIORITY_P0) || priority.equals(Constants.PRIORITY_P1)) {
                    throw new ValidationFailedException("Expected Start/ End Date and Time is mandatory for P0 & P1 priority Work Item");
                }
            }

            if ((task.getTaskExpEndTime() == null && task.getTaskExpEndDate() != null)) {
                LocalDate localDate = task.getTaskExpEndDate().toLocalDate();
                //Todo: this will be derived from team preference in future
                //LocalTime localTime = officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(Constants.OfficeHoursKeys.EOD_TIME, workflowId).getValue();
                LocalTime localTime = LocalTime.of(20, 00, 00);
                task.setTaskExpEndDate(LocalDateTime.of(localDate, localTime));
                task.setTaskExpEndTime(localTime);
            }

            if ((task.getTaskExpStartDate() != null && task.getTaskExpStartTime() == null)) {
                LocalDate localDate = task.getTaskExpStartDate().toLocalDate();
                //Todo: this will be derived from team preference in future
                //LocalTime localTime = officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(Constants.OfficeHoursKeys.OFFICE_START_TIME, workflowId).getValue();
                LocalTime localTime = LocalTime.of(10, 00, 00);
                task.setTaskExpStartDate(LocalDateTime.of(localDate, localTime));
                task.setTaskExpStartTime(localTime);
            }
        }
    }

    /**
     * This method calls multiple functions to set properties to a new child task request before persisting the task in the task table
     */
    public void modifyNewChildTaskProperties(Task task) {
        setTaskStateByWorkflowTaskStatus(task);
        setDefaultCurrentActivityIndicator(task);
        modifyTaskPriority(task);
        updateCurrentlyScheduledTaskIndicatorForTask(task);
    }

    /**
     * This method calls multiple functions to validate properties of a new task request before persisting the task in the task table
     */
    public boolean validateNewTaskProperties(Task task) {
        boolean isTaskValidatedByWorkflowStatus, isTaskValidatedForDateAndTimePairs, isTaskDataValidated, isTaskTypeValidated;
        isTaskDataValidated = validateTaskWorkflowType(task);
        isTaskValidatedByWorkflowStatus = validateTaskByWorkflowStatus(task);
        isTaskValidatedForDateAndTimePairs = validateAllDateAndTimeForPairs(task);
        isTaskTypeValidated = validateTaskTypeIdForAddTask(task);
        validateTaskEntities(task);
        validateImmediateAttention(task);

        return isTaskDataValidated && isTaskValidatedByWorkflowStatus && isTaskValidatedForDateAndTimePairs && isTaskTypeValidated;
    }

    public boolean validateNewChildTaskProperties(Task task) {
        boolean isTaskValidatedByWorkflowStatus, isTaskValidatedForDateAndTimePairs, isTaskDataValidated, isTaskTypeValidated;
        isTaskDataValidated = validateTaskWorkflowType(task);
        isTaskValidatedByWorkflowStatus = validateTaskByWorkflowStatus(task);
        isTaskValidatedForDateAndTimePairs = validateAllDateAndTimeForPairs(task);

        return isTaskDataValidated && isTaskValidatedByWorkflowStatus && isTaskValidatedForDateAndTimePairs;
    }

    public Integer updateNoteIdByTaskId(Long noteId, Long taskId) {
        return taskRepository.setTaskNoteIdByTaskId(noteId, taskId);
    }

    public Integer updateListOfDeliverablesDeliveredIdByTaskId(Long listOfDeliverablesDeliveredId, Long taskId) {
        return taskRepository.setTaskListOfDeliverablesDeliveredIdByTaskId(listOfDeliverablesDeliveredId, taskId);
    }

    @Transactional(readOnly = true)
    public void setBUToTask(Task task) {
        if (task != null) {
            if (task.getFkProjectId() != null && task.getFkProjectId().getProjectId() != null) {
                Project foundProjectDb = projectService.getProjectByProjectId(task.getFkProjectId().getProjectId());
                task.setBuId(foundProjectDb.getBuId());
            } else {
                Team foundTeamDb = teamService.getTeamByTeamId(task.getFkTeamId().getTeamId());
                Project foundProjectDb = projectService.getProjectByProjectId(foundTeamDb.getFkProjectId().getProjectId());
                task.setBuId(foundProjectDb.getBuId());
                task.setFkProjectId(foundProjectDb);
            }
        }
    }

    public List<Task> getAllTasksFromTaskTable() {
        List<Task> task = taskRepository.findAll();
        return task;

    }

    public Task findTaskByTaskId(long taskid) {
        Task task = taskRepository.findByTaskId(taskid);
        return task;
    }

    /**
     * @param taskId
     * @param task
     * @param accountIds
     * @param timeZone
     * @Function: Change the workflowstatus of the task to delete if the task passes all the validations
     * we can delete task in any workflow status except completed/ deleted
     */
    @Transactional
    public String deleteTaskByTaskId(Long taskId, Task task, String accountIds, String timeZone, Boolean onTeamDelete, DeleteWorkItemRequest deleteWorkItemRequest) {
        task.setDeletionReasonId(deleteWorkItemRequest.getDeleteReasonId());
        task.setDeletedReason(deleteWorkItemRequest.getDeleteReason());
        task.setDuplicateWorkItemNumber(deleteWorkItemRequest.getDuplicateWorkItemNumber());
        boolean hasRoleWithDeleteAction = false;
        Long accountId = Long.valueOf(accountIds);
        Set<String> taskNumbersUpdated = new HashSet<>();
        String responseMessage = (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) ? "Parent task " : "Work Item ") + task.getTaskNumber()
                + (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) ? ", along with any not-completed child tasks have been deleted. Completed Child tasks, if present, can't be deleted. "
                : " Deleted Successfully. ");
        if (!onTeamDelete) {
            List<Integer> roleIdList = accessDomainRepository.findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(accountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
            for (Integer role : roleIdList) {
                if (Constants.ROLE_IDS_FOR_DELETE_ACTION.contains(role)) {
                    if (Objects.equals(role, RoleEnum.PERSONAL_USER.getRoleId()) && !Objects.equals(task.getFkAccountIdCreator().getAccountId(), accountId)) {
                        break;
                    }
                    hasRoleWithDeleteAction = true;
                    break;
                }
            }
        }

        if (hasRoleWithDeleteAction || onTeamDelete) {
            //check if the task state is completed or deleted
            if (Constants.WorkFlowStatusIds.COMPLETED.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                logger.error("Trying to delete a COMPLETED Work Item with taskNumber: " + task.getTaskNumber() + " ,task id: " + task.getTaskId());
                throw new DeleteTaskException("Completed Work Item can not be deleted.");
            } else if (Constants.WorkFlowStatusIds.DELETED.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                logger.error("Trying to delete a DELETED Work Item with taskNumber: " + task.getTaskNumber() + " ,task id: " + task.getTaskId());
                throw new DeleteTaskException("Work Item is already marked deleted.");
            }

            boolean isDeleteAllowed = true, areReviewTaskOpenOrDeleted = true;
            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                modifyParentTaskPropertiesForChildTaskDelete(task, accountId, deleteWorkItemRequest.getRemoveFromSprint());
            } else if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                deleteChildTasksOnParentTaskDeleteAndSetDeletedChildTaskIdsInParent(task, accountId, deleteWorkItemRequest.getRemoveFromSprint());
            } else if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.BUG_TASK)) {
                deleteChildTasksOnParentTaskDeleteAndSetDeletedChildTaskIdsInParent(task, accountId, deleteWorkItemRequest.getRemoveFromSprint());
                List<String> associatedTaskNumbers = delinkAssociatedTasks(task, accountId);
                if (!associatedTaskNumbers.isEmpty()) {
                    responseMessage += "Association with work items : " + CommonUtils.convertListToString(associatedTaskNumbers, ", ") + " deleted. ";
                    taskNumbersUpdated.addAll(associatedTaskNumbers);
                }
            }
            if(isActiveReferenceMeetingPresent(task)) {
                throw new ValidationFailedException("Please remove all the non-completed meeting of task to delete work item");
            }
            //TODO: validation for review task
            if (!areReviewTaskOpenOrDeleted) {
                logger.error("Trying to delete a Work Item with taskNumber: " + task.getTaskNumber() + " ,task id: " + task.getTaskId() + " whose reviewTask which is Open or not in Deleted state.");
                throw new DeleteTaskException("There exists reviewTask which is Open or not in Deleted state.");
            }

            //mark task deleted if all validations are true
            if (isDeleteAllowed && areReviewTaskOpenOrDeleted) {
                if (task.getSprintId() != null && !Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    capacityService.removeTaskFromSprintCapacityAdjustment(task);
                }
                removeNotStartedSprintFromWorkItem(task, deleteWorkItemRequest.getRemoveFromSprint());
                task.setFkAccountIdLastUpdated(userAccountRepository.findByAccountId(accountId));
                taskHistoryService.addTaskHistoryOnUserUpdate(task);
                Task taskFromDb = new Task();
                BeanUtils.copyProperties(task, taskFromDb);
                WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, task.getTaskWorkflowId());
                setStatusOnWorkItemDeletion(task);
                task.setFkWorkflowTaskStatus(workFlowTaskStatus);
                task.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
                task.setImmediateAttention(0);  // added in task 3833
                task.setImmediateAttentionFrom(null);
                task.setImmediateAttentionReason(null);
                task.setCurrentlyScheduledTaskIndicator(false);
                task.setCurrentActivityIndicator(0);
                List<String> referenceTaskNumbers = removeTaskReferencesForDeleteTask(task);
                if (!referenceTaskNumbers.isEmpty()) {
                    responseMessage += "Reference with work items : " + CommonUtils.convertListToString(referenceTaskNumbers, ", ") + " deleted. ";
                    taskNumbersUpdated.addAll(referenceTaskNumbers);
                }
                if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty()) {
                    List<String> dependencyTaskNumbers = dependencyService.removeDependenciesOnTaskDeletion(task);
                    if (!dependencyTaskNumbers.isEmpty()) {
                        responseMessage += "Dependency with work items : " + CommonUtils.convertListToString(dependencyTaskNumbers, ", ") + " deleted. ";
                        taskNumbersUpdated.addAll(dependencyTaskNumbers);
                    }
                }
                taskRepository.save(task);
                ArrayList<String> updateFields = new ArrayList<>(List.of("fkWorkflowTaskStatus"));
                try {
                    List<HashMap<String, String>> payload = notificationService.updateTaskNotification(updateFields, task, taskFromDb, timeZone,accountIds);
                    sendPushNotification(payload);
                } catch (Exception e) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Unable to send notification for delete Work Item. Caught Error: " + e, new Throwable(allStackTraces));
                }
                taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, task);
            }
        } else {
            logger.error("User does not have appropriate role. AccountIds: " + accountIds + " , TeamId: " + task.getFkTeamId().getTeamId());
            throw new DeleteTaskException("You don't have action to delete a Work Item.");
        }
        if (!taskNumbersUpdated.isEmpty()) {
            responseMessage += "Work items : " + CommonUtils.convertListToString(new ArrayList<>(taskNumbersUpdated),", ") + " updated.";
        }
        return responseMessage;
    }

    /**
     * @param childTaskToDelete (childTask to be deleted)
     * @param accountIdOfUser   (acId of the user deleting the task)
     * @function system updates the parent task when a child task is deleted
     * we modify the childTaskIds field and recalculates the taskProgressSystem, workflowStatus, recordedEffort, userPerceivedPercentage
     * if it is the last child task, the parent task will be marked as deleted
     */
    // Transactional - propagation required by default
    public void modifyParentTaskPropertiesForChildTaskDelete(Task childTaskToDelete, Long accountIdOfUser, Boolean removeFromSprint) {
        // update the childTaskIds of the parent Task for child task deletion and create user history (not system) -- returns the parent task after modification of childTaskIds
        modifyChildTaskIdsOfParentTask(childTaskToDelete, accountIdOfUser, 1);
        Task updatedParentTaskDB = taskRepository.findByTaskId(childTaskToDelete.getParentTaskId());
        Task updatedParentTask = new Task();
        BeanUtils.copyProperties(updatedParentTaskDB, updatedParentTask);
        Task updatedParentTaskFromDb = new Task();
        BeanUtils.copyProperties(updatedParentTask, updatedParentTaskFromDb);
        List<Long> updatedChildTaskIds = updatedParentTask.getChildTaskIds();
        Set<String> updateFieldsOfParentTask = new HashSet<>();

        // update deleted child TaskIds in the parent task
        List<Long> deletedChildTaskIds;
        if (updatedParentTask.getDeletedChildTaskIds() != null) {
            deletedChildTaskIds = new ArrayList<>(updatedParentTask.getDeletedChildTaskIds());
        } else {
            deletedChildTaskIds = new ArrayList<>();
        }
        deletedChildTaskIds.add(childTaskToDelete.getTaskId());
        updatedParentTask.setDeletedChildTaskIds(deletedChildTaskIds);


        // if it is the last child Task -- delete the parent task on deletion of the sole sub-task
        if (updatedChildTaskIds == null || updatedChildTaskIds.isEmpty()) {
            WorkFlowTaskStatus deleteWorkFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, childTaskToDelete.getTaskWorkflowId());
            setStatusOnWorkItemDeletion(updatedParentTask);
            if (isActiveReferenceMeetingPresent(updatedParentTask)) {
                throw new ValidationFailedException("Please cancel all the non-completed meeting of parent task " + updatedParentTask.getTaskNumber());
            }
            updatedParentTask.setFkWorkflowTaskStatus(deleteWorkFlowTaskStatus);
            updatedParentTask.setTaskState(deleteWorkFlowTaskStatus.getWorkflowTaskState());
            updateFieldsOfParentTask.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
            updatedParentTask.setImmediateAttention(0);
            updatedParentTask.setImmediateAttentionFrom(null);
            updatedParentTask.setImmediateAttentionReason(null);
            if (updatedParentTask.getCurrentlyScheduledTaskIndicator() != null && updatedParentTask.getCurrentlyScheduledTaskIndicator()) {
                updatedParentTask.setCurrentlyScheduledTaskIndicator(false);
                updateFieldsOfParentTask.add(Constants.TaskFields.CURRENTLY_SCHEDULED_TASK_INDICATOR);
            }
            if (updatedParentTask.getCurrentActivityIndicator() != null && updatedParentTask.getCurrentActivityIndicator() == 1) {
                updatedParentTask.setCurrentActivityIndicator(0);
                updateFieldsOfParentTask.add(Constants.TaskFields.CURRENT_ACTIVITY_INDICATOR);
            }
            removeNotStartedSprintFromWorkItem (updatedParentTask, removeFromSprint);
            updatedParentTask.setDeletionReasonId(childTaskToDelete.getDeletionReasonId());
            updatedParentTask.setDeletedReason(childTaskToDelete.getDeletedReason());
            updatedParentTask.setDuplicateWorkItemNumber(childTaskToDelete.getDuplicateWorkItemNumber());
        } else {
            /* case when the sub-task we are deleting is not the last sub-task -- parent task stat will be worst stat of
            all the remaining child task --if after deleting the current child task, all the remaining child tasks
            are in completed state we mark the parent as completed */

            // modify the estimate of the parent task
            if (childTaskToDelete.getRecordedTaskEffort() != null) {
                int adjustedEstimate = (updatedParentTask.getTaskEstimate() != null ? updatedParentTask.getTaskEstimate() : 0) - CommonUtils.calculateTaskEstimateAdjustment(childTaskToDelete);
                updatedParentTask.setTaskEstimate(adjustedEstimate);
                updateFieldsOfParentTask.add(Constants.TaskFields.ESTIMATE);
            } else if (childTaskToDelete.getTaskEstimate() != null) {
                updatedParentTask.setTaskEstimate((updatedParentTask.getTaskEstimate() != null ? updatedParentTask.getTaskEstimate() : 0) - childTaskToDelete.getTaskEstimate());
                updateFieldsOfParentTask.add(Constants.TaskFields.ESTIMATE);
            }

            List<Task> remainingChildTasks = taskRepository.findByTaskIdIn(updatedChildTaskIds);
//            List<Task> remainingChildTasks = allChildTasks.stream().filter(task ->
//                            !Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE))
//                    .collect(Collectors.toList());
            StatType worstStat = remainingChildTasks.get(0).getTaskProgressSystem();
            LocalDateTime worstSystemDerivedEndTs = remainingChildTasks.get(0).getSystemDerivedEndTs();
            boolean areAllRemainingChildTasksCompleted = true;
            for (Task childTask : remainingChildTasks) {
                if (isWorseStat(childTask.getTaskProgressSystem(), worstStat)) {
                    worstStat = childTask.getTaskProgressSystem();
                }

                if (isWorstSystemDerivedEndTs(childTask.getSystemDerivedEndTs(), worstSystemDerivedEndTs)) {
                    worstSystemDerivedEndTs = childTask.getSystemDerivedEndTs();
                }

                if (!childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
                    areAllRemainingChildTasksCompleted = false;
                }
            }

            if (updatedParentTask.getTaskProgressSystem() == null || !updatedParentTask.getTaskProgressSystem().equals(worstStat)) {
                if(updatedParentTask.getTaskActStDate() != null && worstStat.getOrder() == StatType.NOTSTARTED.getOrder())
                    updatedParentTask.setTaskProgressSystem(StatType.ONTRACK);
                else
                    updatedParentTask.setTaskProgressSystem(worstStat);
                LocalDateTime currentDateTime = LocalDateTime.now();
                updatedParentTask.setTaskProgressSystemLastUpdated(currentDateTime);
                updatedParentTask.setSystemDerivedEndTs(worstSystemDerivedEndTs);
                updateFieldsOfParentTask.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM);
                updateFieldsOfParentTask.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM_LAST_UPDATED);
            }

            if (areAllRemainingChildTasksCompleted) {
                WorkFlowTaskStatus workFlowTaskStatusCompleted = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(
                        Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, updatedParentTask.getTaskWorkflowId());
                updatedParentTask.setFkWorkflowTaskStatus(workFlowTaskStatusCompleted);
                updatedParentTask.setTaskActEndDate(LocalDateTime.now());
                updatedParentTask.setTaskActEndTime(LocalTime.now());
                updateFieldsOfParentTask.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                updateFieldsOfParentTask.add(Constants.TaskFields.ACTUAL_END_DATE);
                int parentTaskUserPerceivedPercentageTaskCompleted = calculateParentUserPerceivedPercentageForChildTaskDelete(updatedChildTaskIds, childTaskToDelete);
                updatedParentTask.setUserPerceivedPercentageTaskCompleted(parentTaskUserPerceivedPercentageTaskCompleted);
                updateFieldsOfParentTask.add(("userPerceivedPercentageTaskCompleted"));
            }

        }
        // modify the userPerceivedPercentage of the parent task -- the recorded effort & earnedTime of the parent task won't be modified
        // by childTaskToDelete.getRecordedEffort() != null implies => totalEarnedTimeOfParentTask != null && updatedParentTask.getUserPerceivedPercentageTaskCompleted() != null && childTaskToDelete.getTaskEstimate() != null && childTaskToDelete.getEarnedTimeTask()!=null)
        // assuming that if we are deleting the last child task then the userPerceived percentage of parent task should not be updated o.w it will become 100%
        if (updatedParentTask.getRecordedEffort() != null && !(updatedChildTaskIds == null || updatedChildTaskIds.isEmpty())) {
            int parentTaskUserPerceivedPercentageTaskCompleted = calculateParentUserPerceivedPercentageForChildTaskDelete(updatedChildTaskIds, childTaskToDelete);
            updatedParentTask.setUserPerceivedPercentageTaskCompleted(parentTaskUserPerceivedPercentageTaskCompleted);
            updateFieldsOfParentTask.add(("userPerceivedPercentageTaskCompleted"));
        }

        if (!updateFieldsOfParentTask.isEmpty()) {
            taskHistoryService.addTaskHistoryOnSystemUpdate(updatedParentTaskFromDb);
            taskRepository.save(updatedParentTask);
            taskHistoryMetadataService.addTaskHistoryMetadata(new ArrayList<>(updateFieldsOfParentTask), updatedParentTask);
        }
    }

    private void removeNotStartedSprintFromWorkItem(Task updatedParentTask, Boolean removeFromSprint) {
        if (updatedParentTask.getSprintId() != null) {
            Sprint sprint = sprintRepository.findBySprintId(updatedParentTask.getSprintId());
            if (sprint != null) {
                if (Objects.equals(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId(), sprint.getSprintStatus())) {
                    updatedParentTask.setSprintId(null);
                }
                else if (Objects.equals(Constants.SprintStatusEnum.STARTED.getSprintStatusId(), sprint.getSprintStatus()) && removeFromSprint != null && removeFromSprint) {
                    updatedParentTask.setSprintId(null);
                }
            }
        }
    }

    /** add the deleted child task's id to deletedChildTaskIds list of the parent task*/
    private void modifyDeletedChildTaskIdsOfParentTask(Task childTaskToDelete, Task parentTask) {
        List<Long> deletedChildTaskIds = parentTask.getDeletedChildTaskIds() != null ? new ArrayList<>(parentTask.getDeletedChildTaskIds()) : new ArrayList<>();
        deletedChildTaskIds.add(childTaskToDelete.getTaskId());
        parentTask.setDeletedChildTaskIds(deletedChildTaskIds);
    }

    /** this method calculates the user perceived percentage for the parent task when the child tasks are modified*/
    private int calculateParentUserPerceivedPercentageForChildTaskDelete(List<Long> updatedChildTaskIds, Task childTaskToDelete) {
        int totalEstimate = 0, totalEarnedTime = 0;
        List<Task> allRemainingChildTasks = taskRepository.findByTaskIdIn(updatedChildTaskIds);
        for (Task remainingChildTask : allRemainingChildTasks) {
            totalEstimate += remainingChildTask.getTaskEstimate() != null ? remainingChildTask.getTaskEstimate() : 0;
            totalEarnedTime += remainingChildTask.getEarnedTimeTask() != null ? remainingChildTask.getEarnedTimeTask() : 0;
        }
        totalEstimate += childTaskToDelete.getEarnedTimeTask() != null ? childTaskToDelete.getEarnedTimeTask() : 0;
        totalEarnedTime += childTaskToDelete.getEarnedTimeTask() != null ? childTaskToDelete.getEarnedTimeTask() : 0;
        if (totalEstimate > 0)
            return (int) Math.round((totalEarnedTime / (double) totalEstimate) * 100);

        return 0;
    }

    /**
     * @param childTaskToDelete
     * @param accountIdOfUser
     * @param historyUpdateType -- 0 for system update and 1 for user update
     * @return parent task after modification of childTaskIds
     * @function update the childTaskIds of the parent Task for child task deletion and create user history (not system)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Task modifyChildTaskIdsOfParentTask(Task childTaskToDelete, Long accountIdOfUser, int historyUpdateType) {
        Task parentTask = taskRepository.findByTaskId(childTaskToDelete.getParentTaskId());
        Task parentTaskFromDb = new Task();
        BeanUtils.copyProperties(parentTask, parentTaskFromDb);
        if (historyUpdateType == 1) {
            taskHistoryService.addTaskHistoryOnUserUpdate(parentTaskFromDb);
            parentTask.setFkAccountIdLastUpdated(userAccountRepository.findByAccountId(accountIdOfUser));
        } else if (historyUpdateType == 0) {
            taskHistoryService.addTaskHistoryOnSystemUpdate(parentTaskFromDb);
        }
        List<String> updateFieldsOfParentTask = new ArrayList<>();
        List<Long> updatedChildTaskIds = parentTask.getChildTaskIds();
        if (updatedChildTaskIds != null) updatedChildTaskIds.remove(childTaskToDelete.getTaskId());
        parentTask.setChildTaskIds(updatedChildTaskIds);
        updateFieldsOfParentTask.add(Constants.TaskFields.CHILD_TASK_IDS);
//        saveTask(parentTask);
        taskRepository.save(parentTask);
//        parentTask.setVersion(parentTask.getVersion()+1);
        taskHistoryMetadataService.addTaskHistoryMetadata(updateFieldsOfParentTask, parentTask);
        return parentTask;
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    private Task saveTask(Task task){
//        Task savedTask = taskRepository.save(task);
//        return savedTask;
//    }

    /**
     * @param parentTask
     * @param accountIdOfUser
     * @function marks all sub-tasks workflow status as delete when parent task is deleted
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    private void deleteChildTasksOnParentTaskDeleteAndSetDeletedChildTaskIdsInParent(Task parentTask, Long accountIdOfUser, Boolean removeFromSprint) {
        List<Task> childTasks = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());

        // validation: no childTask should be marked as completed to delete a parent task
//        for (Task childTask : childTasks) {
//            if (childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
//                throw new ValidationFailedException("deletion of parent task is not allowed when a sub-task is marked as completed");
//            }
//        }

        WorkFlowTaskStatus deleteWorkFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, parentTask.getTaskWorkflowId());
        List<Long> deletedChildTaskIds = parentTask.getDeletedChildTaskIds() != null ? new ArrayList<>(parentTask.getDeletedChildTaskIds()) : new ArrayList<>();
        for (Task childTask : childTasks) {
            if (!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                if (isActiveReferenceMeetingPresent(childTask)) {
                    throw new ValidationFailedException("Please cancel all the non-completed meeting of child task " + childTask.getTaskNumber() + " to delete parent task");
                }
            }
        }
        for (Task childTask : childTasks) {
            if (!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                List<String> updatedFieldsOfChildTask = new ArrayList<>();
                Task childTaskFromDb = new Task();
                BeanUtils.copyProperties(childTask, childTaskFromDb);
                if (childTask.getSprintId() != null) {
                    capacityService.removeTaskFromSprintCapacityAdjustment(childTask);
                    removeNotStartedSprintFromWorkItem(childTask, removeFromSprint);
                }
                taskHistoryService.addTaskHistoryOnSystemUpdate(childTaskFromDb);
                setStatusOnWorkItemDeletion(childTask);
                childTask.setFkWorkflowTaskStatus(deleteWorkFlowTaskStatus);
                childTask.setTaskState(deleteWorkFlowTaskStatus.getWorkflowTaskState());
                updatedFieldsOfChildTask.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                childTask.setImmediateAttention(0);
                childTask.setImmediateAttentionFrom(null);
                childTask.setImmediateAttentionReason(null);
                if (childTask.getCurrentlyScheduledTaskIndicator() != null && childTask.getCurrentlyScheduledTaskIndicator()) {
                    childTask.setCurrentlyScheduledTaskIndicator(false);
                    updatedFieldsOfChildTask.add(Constants.TaskFields.CURRENTLY_SCHEDULED_TASK_INDICATOR);
                }
                if (childTask.getCurrentActivityIndicator() != null && childTask.getCurrentActivityIndicator() == 1) {
                    childTask.setCurrentActivityIndicator(0);
                    updatedFieldsOfChildTask.add(Constants.TaskFields.CURRENT_ACTIVITY_INDICATOR);
                }
                childTask.setDeletionReasonId(parentTask.getDeletionReasonId());
                childTask.setDeletedReason(parentTask.getDeletedReason());
                childTask.setDuplicateWorkItemNumber(parentTask.getDuplicateWorkItemNumber());
                deletedChildTaskIds.add(childTask.getTaskId());
                taskRepository.save(childTask);
                taskHistoryMetadataService.addTaskHistoryMetadata(updatedFieldsOfChildTask, childTask);
                modifyChildTaskIdsOfParentTask(childTask, -1L, 0);
            }
            parentTask.setDeletedChildTaskIds(deletedChildTaskIds);
        }
    }

    /**
     * @param bugTask
     * @param accountIdOfUser
     * @function if the bug task is in not started/ backlog workflow states, delink it from the tasks it was associated with
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    private List<String> delinkAssociatedTasks(Task bugTask, Long accountIdOfUser) {
        List<String> taskNumbers = new ArrayList<>();
        if (bugTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) || bugTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
            List<Task> linkedTasks = taskRepository.findByTaskIdIn(bugTask.getBugTaskRelation());
            for (Task linkedTask : linkedTasks) {
                taskNumbers.add(linkedTask.getTaskNumber());
                Set<Long> bugTaskRelationList;
                List<String> updatedFields = new ArrayList<>();
                Task linkedTaskFromDb = new Task();
                BeanUtils.copyProperties(linkedTask, linkedTaskFromDb);
                taskHistoryService.addTaskHistoryOnSystemUpdate(linkedTaskFromDb);
                if (linkedTask.getBugTaskRelation() == null) {
                    bugTaskRelationList = new HashSet<>();
                } else {
                    bugTaskRelationList = new HashSet<>(linkedTask.getBugTaskRelation());
                }
                bugTaskRelationList.remove(bugTask.getTaskId());
                linkedTask.setBugTaskRelation(new ArrayList<>(bugTaskRelationList));
                updatedFields.add(Constants.TaskFields.BUG_TASK_RELATION);
                taskRepository.save(linkedTask);
                taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, linkedTask);
            }
        }
        return taskNumbers;
    }
//   --------------------TASK 2141--- NEW METHOD TO SET TASK STATE STARTS HERE________________________

    /***
     * This method sets the task state in task request according to the workflowTaskStatus.
     */
    public void setTaskStateByWorkflowTaskStatus(Task taskFromRequest) {
        if (taskFromRequest.getTaskState() == null || taskFromRequest.getTaskState().isEmpty()) {

            WorkFlowTaskStatus taskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(taskFromRequest.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), taskFromRequest.getTaskWorkflowId());
            if (taskStatus == null) throw new InternalServerErrorException("WorkFlowTaskStatus does not exists!");
            taskFromRequest.setTaskState(taskStatus.getWorkflowTaskState());
        }
    }

//   ----------------------------------  ENDS HERE ---------------------------------------------------------

    /**
     * @param attentionRequest
     * @return List of set of required fields of tasks which require attention from the user
     */
    public List<AttentionResponse> getAllTaskWithAttention(AttentionRequest attentionRequest) {
        List<Task> taskList = taskRepository.findByImmediateAttentionFromAndTeamIn(attentionRequest.getUserName(), attentionRequest.getTeamList());
        List<AttentionResponse> attentionResponseList = new ArrayList<>();
        for (Task task : taskList) {
            AttentionResponse attentionResponse = new AttentionResponse();
            attentionResponse.setTaskNumber(task.getTaskNumber());
            attentionResponse.setTaskIdentifier(task.getTaskIdentifier());
            attentionResponse.setTeamId(task.getFkTeamId().getTeamId());
            attentionResponse.setTaskTitle(task.getTaskTitle());
            attentionResponse.setTaskDesc(task.getTaskDesc());
            if (task.getTaskActStDate() != null) attentionResponse.setTaskActStDate(task.getTaskActStDate());
            if (task.getTaskActEndDate() != null) attentionResponse.setTaskActEndDate(task.getTaskActEndDate());
            attentionResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            if (task.getTaskPriority() != null) attentionResponse.setTaskPriority(task.getTaskPriority());
            if (task.getFkAccountIdAssigned() != null) {
                attentionResponse.setTaskAssignedTo(task.getFkAccountIdAssigned().getEmail());
            }
            attentionResponseList.add(attentionResponse);
        }
        return attentionResponseList;
    }

    @SuppressWarnings(value = "unused")
    public Task updateFieldsInTaskTable(Task task, Long taskId, String timeZone,String headerAccountIds) throws IllegalAccessException {
        Task taskOptional = taskRepository.findByTaskId(taskId);
        Task taskFromDb = new Task();
        BeanUtils.copyProperties(taskOptional, taskFromDb);
        Task taskSave = null;

        Task parentTaskDbCopy = new Task();
        if (task.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
            Task parentTaskDb = taskRepository.findByTaskId(task.getParentTaskId());
            BeanUtils.copyProperties(parentTaskDb, parentTaskDbCopy);
        }

        boolean isComputeStatAllowedByWorkflowStatus = isComputeStatAllowedByWorkflowStatus(taskOptional, task);

        validateWorkflowStatusWithExistingDependenciesOnUpdateTask(task);
        if (task.getDependentTaskDetailRequestList() != null) {
            validateAndSetTaskDependenciesOnUpdateTask(task);
        }

        Long accountIdPrevAssignee1Db = taskOptional.getAccountIdPrevAssignee1();
        Long accountIdPrevAssignee2Db = taskOptional.getAccountIdPrevAssignee2();
        Long accountIdPrevAssigned1Db = taskOptional.getAccountIdPrevAssigned1();
        Long accountIdPrevAssigned2Db = taskOptional.getAccountIdPrevAssigned2();

        Long accountIdAssigneeDb = null;
        if (taskOptional.getFkAccountIdAssignee() != null) {
            accountIdAssigneeDb = taskOptional.getFkAccountIdAssignee().getAccountId();
        }
        Long accountIdAssignedDb = null;
        if (taskOptional.getFkAccountIdAssigned() != null) {
            accountIdAssignedDb = taskOptional.getFkAccountIdAssigned().getAccountId();
        }

        ArrayList<String> updateFields = taskService.getFieldsToUpdate(task, taskId);
        boolean isStatAffectingFieldsUpdated = false;
        //Todo: to remove isEstimateSystemGenerated later
        List<String> fieldsToRemove = Arrays.asList(
                "comments", "commentId", "fkProjectId", "fkOrgId", "fkAccountId",
                "fkTeamId", "attachments", "listOfDeliverablesDelivered",
                "childTaskList", "linkedTaskList", "parentTaskResponse", "labels", "referenceWorkItemList",
                "labelsToAdd", "dependentTaskDetailResponseList", "addedInEpicAfterCompletion"
        );
        updateFields.removeAll(fieldsToRemove);
        if (!task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {
            isStatAffectingFieldsUpdated = Constants.statAffectingFields.stream().anyMatch(updateFields::contains);
        } else if (updateFields.contains(Constants.TaskFields.WORKFLOW_TASK_STATUS)) {
            isStatAffectingFieldsUpdated = true;
        }

        // updating capacities
        if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            if (updateFields.contains("sprintId") || (task.getSprintId() != null && (updateFields.contains("fkAccountIdAssigned") || updateFields.contains("taskEstimate")))) {
                updateTaskAndCapacities(taskFromDb, task, updateFields);
            }
            //handling sprint for parent tasks
        } else {
            if (updateFields.contains("sprintId")) {
                moveChildTaskFromSprint(task);
            }
        }
        if (task.getFkEpicId() != null || taskFromDb.getFkEpicId() != null) {
            Task taskCopy = new Task();
            BeanUtils.copyProperties(task, taskCopy);
            modifyEpicPropertiesForTask(taskCopy, taskFromDb, timeZone);
            if (taskFromDb.getFkEpicId() == null && Objects.equals(taskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                updateFields.add("addedInEpicAfterCompletion");
                task.setAddedInEpicAfterCompletion(true);
            }
        }
        // if there are new dependencies that have already been validated against the new dates provided -- validating the compatibility of new dates with the old dependencies
        if ((updateFields.contains(Constants.TaskFields.EXP_END_DATE) || updateFields.contains(Constants.TaskFields.EXP_START_DATE)) && (taskFromDb.getDependencyIds() != null && !taskFromDb.getDependencyIds().isEmpty())) {
            validateDependencyDatesConstraintAndRecalculateLagTime(taskFromDb, task.getTaskExpStartDate(), task.getTaskExpEndDate());
        }

        ArrayList<String> fieldsToBroadcastMessage = new ArrayList<>();
        fieldsToBroadcastMessage.add("immediateAttentionFrom");

        if (task.getLabelsToAdd() != null && !task.getLabelsToAdd().isEmpty()) {
            updateFields.add(Constants.TaskFields.LABELS);
        }
        if (taskFromDb.getLabels() != null && !taskFromDb.getLabels().isEmpty()) {
            List<String> taskLabels = taskFromDb.getLabels().stream()
                    .map(Label::getLabelName)
                    .collect(Collectors.toList());
            taskOptional.setTaskLabels(taskLabels);
        }

        updateReferenceWorkItem(task);
        deleteReferenceWorkItem(task);
        if (updateFields.containsAll(fieldsToBroadcastMessage)) {
            if (!Objects.equals(task.getImmediateAttention(), 0) && task.getImmediateAttentionFrom() != null && task.getImmediateAttentionReason() != null) {
                addImmediateAttentionAlert(task, timeZone);
            } else {
                task.setImmediateAttentionFrom(null);
                task.setImmediateAttentionReason(null);
            }
        }
        if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, task.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            setStatusOnWorkItemDeletion(taskOptional);
        }

        // This condition is added because in some task there is some leading or trailing spaces so in updateFields we didn't consider leading or trailing spaces
        if (!updateFields.contains("taskTitle") && !Objects.equals(taskFromDb.getTaskTitle(), task.getTaskTitle())) {
            updateFields.add("taskTitle");
        }
        if (updateFields.contains("releaseVersionName")) {
            createOrUpdateReleaseVersionOfTask(taskFromDb, task);
        }

        if (!updateFields.isEmpty()) {
            TaskHistory taskHistory = taskHistoryService.addTaskHistoryOnUserUpdate(taskOptional);

            for (String s : updateFields) {
                try {
                    Field field = Task.class.getDeclaredField(s);
                    field.setAccessible(true);
                    Object fieldValue = field.get(task);
                    field.set(taskOptional, fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException | SecurityException |
                         NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }

        if (updateFields.contains("currentlyScheduledTaskIndicator") && !task.getCurrentlyScheduledTaskIndicator()) {
            taskOptional.setCurrentActivityIndicator(0);
        }

        taskOptional.setLabels(taskFromDb.getLabels());
        taskOptional.setLabelsToAdd(task.getLabelsToAdd());
        addLabelToTask(taskOptional);
        setTaskStateByWorkflowTaskStatus(taskOptional);
        taskService.setRcaMemberAccountIdListResponse(taskOptional);

        if (taskOptional.getFkAccountIdAssigned() == null) {
            taskOptional.setFkAccountIdAssignee(null);
        } else if (!Objects.equals(accountIdAssignedDb, task.getFkAccountIdAssigned().getAccountId())) {
            taskOptional.setFkAccountIdAssignee(task.getFkAccountIdLastUpdated());
            taskOptional.setAccountIdPrevAssignee1(accountIdAssigneeDb);
            taskOptional.setAccountIdPrevAssignee2(accountIdPrevAssignee1Db);
            taskOptional.setAccountIdPrevAssigned1(accountIdAssignedDb);
            taskOptional.setAccountIdPrevAssigned2(accountIdPrevAssigned1Db);
        }

        // updating notes
        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            List<Note> notesCopy = task.getNotes().stream().map(note -> {
                Note copyNote = new Note();
                BeanUtils.copyProperties(note, copyNote);
                return copyNote;
            }).collect(Collectors.toList());
            List<Note> notesUpdated = noteService.updateAllNotes(notesCopy, task);
            if (notesUpdated != null && !notesUpdated.isEmpty()) {
                Integer resultSet = updateNoteIdByTaskId(notesUpdated.get(0).getNoteId(), task.getTaskId()); // this line is added
            }
        }

        taskSave = taskRepository.save(taskOptional);
        if (isStatAffectingFieldsUpdated || (task.getNextTaskProgressSystemChangeDateTime() != null && LocalDateTime.now().isAfter(task.getNextTaskProgressSystemChangeDateTime()))) {
            computeAndUpdateStatForTask(taskSave, isComputeStatAllowedByWorkflowStatus);
        }

        // updating deliverables delivered
        if (task.getListOfDeliverablesDelivered() != null && !task.getListOfDeliverablesDelivered().isEmpty()) {
            List<DeliverablesDelivered> listOfDeliverablesDeliveredUpdated = deliverablesDeliveredService.updateAllDeliverablesDelivered(task.getListOfDeliverablesDelivered(), task);
            if (listOfDeliverablesDeliveredUpdated != null && !listOfDeliverablesDeliveredUpdated.isEmpty()) {

                Integer resultSet = updateListOfDeliverablesDeliveredIdByTaskId(listOfDeliverablesDeliveredUpdated.get(0).getListOfDeliverablesDeliveredId(), task.getTaskId());
            }
        }

        // in case of task title change -- save the same in the time sheet table
        if (updateFields.contains("taskTitle")) {
            List<TimeSheet> timeSheets = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TASK, taskSave.getTaskId());
            if (timeSheets == null) timeSheets = Collections.emptyList();
            DataEncryptionConverter dataEncryptionConverter = new DataEncryptionConverter();
            String updatedTaskTitle = dataEncryptionConverter.convertToDatabaseColumn(taskSave.getTaskTitle());
            for (TimeSheet timeSheet : timeSheets) {
                timeSheet.setEntityTitle(updatedTaskTitle);
            }
            timeSheetRepository.saveAll(timeSheets);
        }

        // if child task is getting updated -- modify the parent task properties accordingly
        if (taskSave.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
            modifyParentTaskPropertiesForChildTaskUpdate(taskSave, taskFromDb, updateFields, timeZone);
        }

        // set the referenceWorkItemList
//        Task taskCopy = new Task();
//        BeanUtils.copyProperties(task, taskCopy);
//        List<ReferenceWorkItem> referenceWorkItemList = findReferenceWorkItemList(taskCopy);
//        taskSave.setReferenceWorkItemList(referenceWorkItemList);


        // send update task notification
        List<HashMap<String, String>> payload = notificationService.updateTaskNotification(updateFields, task, taskFromDb, timeZone,headerAccountIds);
        sendPushNotification(payload);
        // -------------------------------- ENDS HERE --------------------------------------------------------------
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && updateFields.contains("sprintId")) {
            taskSave.setTaskProgressSystem(task.getTaskProgressSystem());
            taskSave.setTaskProgressSystemLastUpdated(task.getTaskProgressSystemLastUpdated());
        }


        // This check ensures data integrity during the update task.
        // Specifically, we're verifying the completeness of the assignedTo object.
        // In some cases, only the accountId might be provided
        if (taskSave.getFkAccountIdAssigned() != null && taskSave.getFkAccountIdAssigned().getOrgId() == null) {
            UserAccount assignedToUser = userAccountRepository.findByAccountIdAndIsActive(taskSave.getFkAccountIdAssigned().getAccountId(), true);
            if (assignedToUser != null) {
                taskSave.setFkAccountIdAssigned(assignedToUser);
            }
        }

        taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, task);

        return taskSave;
    }

//    private void transactionalTestMethod() {
//
//        throw new RuntimeException("Intentional Exception for transaction test");
//
//    }


    private void updateTaskAndCapacities(Task originalTask, Task updatedTask, List<String> updatedFields) {

//        // if the task is a part of a sprint, or it was part of sprint and now has been removed, or it has been moved from one sprint to another
//        // this condition covers the case where sprintId in the task is now null, but it is not null in the original Task
//        if (updatedTask.getSprintId() != null || !Objects.equals(originalTask.getSprintId(), updatedTask.getSprintId())) {
//            if (updatedTask.getSprintId() != null) {
//                Sprint sprint = sprintRepository.findById(updatedTask.getSprintId()).orElseThrow(() -> new EntityNotFoundException("Sprint not found"));
//                // change the capacities only if sprint has not completed
//                if (!Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
//                    if (updatedFields.contains("sprintId")) {
//                        capacityService.transferCapacitiesBetweenSprints(originalTask, updatedTask);
//                    }
//                    if (updatedFields.contains("fkAccountIdAssigned")) {
//                        capacityService.adjustCapacitiesForAccountIdChange(originalTask, updatedTask);
//                    }
//                    if (updatedFields.contains("taskEstimate")) {
//                        capacityService.recalculateCapacitiesForTaskEstimateChange(originalTask, updatedTask);
//                    }
//                }
//            } else {
//                // sprintId in updated task is null but sprintId in the original task is not null.
//                // Note that such removal is not possible if the sprint has completed. This was checked in the validations
//                capacityService.recalculateCapacitiesForTaskRemovalFromSprint(originalTask, updatedTask);
//            }
//        }
            if (updatedFields.contains("sprintId")) {
                capacityService.handleSprintChange(originalTask, updatedTask);
                return;
            }

            if (updatedFields.contains("fkAccountIdAssigned")) {
                capacityService.handleAccountChange(originalTask, updatedTask);
                return;
            }

            if (updatedFields.contains("taskEstimate")) {
                capacityService.handleEstimateChange(originalTask, updatedTask);
            }
        }


    public void addLabelToUpdateTask(Task task) {
        if (task.getLabelsToAdd() == null || task.getLabelsToAdd().isEmpty()) return;

        // Retrieve the current labels from the task. This list is managed by Hibernate.
        List<Label> currentLabels = task.getLabels();

        for (String labelName : task.getLabelsToAdd()) {
            labelName = labelName.trim().replaceAll("\\s+", " ");
            // Convert to Title case
            labelName = Arrays.stream(labelName.split("\\b")).map(word -> word.isEmpty() ? word :
                            Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(""));

            // Check if the label already exists in the database
            Label label = labelRepository.findByLabelNameIgnoreCaseAndEntityTypeIdAndEntityId(labelName,Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
            if (label == null) {
                // If not, create a new one
                label = new Label();
                label.setLabelName(labelName);
                label.setEntityId(task.getFkTeamId().getTeamId());
                label.setEntityTypeId(Constants.EntityTypes.TEAM);
                label = labelRepository.save(label);
            }

            // Check if the task's current labels do not contain this label
            if (!currentLabels.contains(label)) {
                currentLabels.add(label); // Add label to the current list, not replacing the list
            }
        }

        // No need to call task.setLabels(); the existing list is already updated
    }


    /**
     * This method is updating the parent Task's workflow status and stats depending on update of the child task
     * If the child task is marked as started, we mark the parent task as started and update the stats
     * If all the child tasks are completed, then we mark the parent  task as completed as well
     *
     * @param updatedChildTask
     * @param updateFieldsOfChildTask
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public void modifyParentTaskPropertiesForChildTaskUpdate(Task updatedChildTask, Task childTaskFromDb, List<String> updateFieldsOfChildTask, String timeZone) {
        Task parentTaskDB = taskRepository.findByTaskId(updatedChildTask.getParentTaskId());
        Task parentTask = new Task();
        BeanUtils.copyProperties(parentTaskDB, parentTask);
        List<StatType> allChildTaskStats = taskRepository.getStatsOfTaskIdsIn(parentTask.getChildTaskIds());
        Task parentTaskFromDb = new Task();
        BeanUtils.copyProperties(parentTask, parentTaskFromDb);
        List<String> updateFieldsOfParentTask = new ArrayList<>();

        // modify the workflow status of the parent task
        modifyWorkflowOfParentTaskOnChildTaskUpdate(parentTask, updatedChildTask, updateFieldsOfChildTask, updateFieldsOfParentTask);

        if (updateFieldsOfChildTask.contains(Constants.TaskFields.ESTIMATE)) {
            int differenceOfEstimate = (updatedChildTask.getTaskEstimate() != null ? updatedChildTask.getTaskEstimate() : 0) -
                    (childTaskFromDb.getTaskEstimate() != null ? childTaskFromDb.getTaskEstimate() : 0);
            parentTask.setTaskEstimate((parentTask.getTaskEstimate() != null ? parentTask.getTaskEstimate() : 0) + differenceOfEstimate);
            updateFieldsOfParentTask.add(Constants.TaskFields.ESTIMATE);
        }
        if (!Objects.equals(parentTaskDB.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus())
                && (Objects.equals(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)
                || Objects.equals(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE))) {
            synchronizeChildTaskDetailsWithParent(parentTask, timeZone, childTaskFromDb.getTaskId());
        }
        if (updateFieldsOfChildTask.contains(Constants.TaskFields.RECORDED_EFFORT) || updateFieldsOfChildTask.contains(Constants.TaskFields.USER_PERCEIVED_PERCENTAGE) || updateFieldsOfChildTask.contains(Constants.TaskFields.ESTIMATE)) {
            if (updateFieldsOfChildTask.contains(Constants.TaskFields.RECORDED_EFFORT)) {
                int newEffortOnChild = (updatedChildTask.getRecordedEffort() != null ? updatedChildTask.getRecordedEffort() : 0) - (childTaskFromDb.getRecordedEffort() != null ? childTaskFromDb.getRecordedEffort() : 0);
                int newEarnedTimeOnChild = (updatedChildTask.getEarnedTimeTask() != null ? updatedChildTask.getEarnedTimeTask() : 0) - (childTaskFromDb.getEarnedTimeTask() != null ? childTaskFromDb.getEarnedTimeTask() : 0);
                parentTask.setRecordedEffort(parentTask.getRecordedEffort() != null ? parentTask.getRecordedEffort() + newEffortOnChild : newEffortOnChild);
                parentTask.setTotalEffort(parentTask.getTotalEffort() != null ? parentTask.getTotalEffort() + newEffortOnChild : newEffortOnChild);
                parentTask.setRecordedTaskEffort(parentTask.getRecordedTaskEffort() != null ? parentTask.getRecordedTaskEffort() + newEffortOnChild : newEffortOnChild);
                parentTask.setEarnedTimeTask(parentTask.getEarnedTimeTask() != null ? parentTask.getEarnedTimeTask() + newEarnedTimeOnChild : newEarnedTimeOnChild);

                // Normalize and compare the two values for recordedEffort
                Integer currentEffort = parentTask.getRecordedEffort();
                Integer dbEffort = parentTaskDB.getRecordedEffort();
                if ((currentEffort == null ? 0 : currentEffort) !=
                        (dbEffort == null ? 0 : dbEffort)) {
                    updateFieldsOfParentTask.add(Constants.TaskFields.RECORDED_EFFORT);
                }

            }

            // since parent task's estimate is independent of child task's estimates - we need to fetch all child tasks from the database (even deleted ones) to get the total estimate
            List<Task> allChildTasks = taskRepository.findByParentTaskId(parentTask.getTaskId());
//            allChildTasks.remove(childTaskFromDb);
            allChildTasks = allChildTasks.stream().filter(task -> !task.getTaskId().equals(childTaskFromDb.getTaskId())).collect(Collectors.toList());

            int totalEstimateOfChildTasks = 0, totalEarnedTime = 0;
            for (Task childTask : allChildTasks) {
                if (childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE)) {
                    totalEstimateOfChildTasks += childTask.getEarnedTimeTask() != null ? childTask.getEarnedTimeTask() : 0;
                } else {
                    totalEstimateOfChildTasks += childTask.getTaskEstimate() != null ? childTask.getTaskEstimate() : 0;
                }
                totalEarnedTime += childTask.getEarnedTimeTask() != null ? childTask.getEarnedTimeTask() : 0;
            }
            totalEstimateOfChildTasks += updatedChildTask.getTaskEstimate() != null ? updatedChildTask.getTaskEstimate() : 0;
            totalEarnedTime += updatedChildTask.getEarnedTimeTask() != null ? updatedChildTask.getEarnedTimeTask() : 0;
            int parentTaskUserPerceivedPercentageTaskCompleted = (int) Math.round((totalEarnedTime / (double) totalEstimateOfChildTasks) * 100);

            Integer currentPercentage = parentTask.getUserPerceivedPercentageTaskCompleted();
            Integer dbPercentage = parentTaskDB.getUserPerceivedPercentageTaskCompleted();
            if (!(dbPercentage == null && parentTaskUserPerceivedPercentageTaskCompleted == 0)) {
                parentTask.setUserPerceivedPercentageTaskCompleted(parentTaskUserPerceivedPercentageTaskCompleted);
            }

            if ((currentPercentage == null ? 0 : currentPercentage) !=
                    (dbPercentage == null ? 0 : dbPercentage)) {
                updateFieldsOfParentTask.add(Constants.TaskFields.USER_PERCEIVED_PERCENTAGE);
            }

        }

        /* gets all childTask stats and assigns the worst stat to parent task -- we are not comparing parent task stat with the stat of the current task that is getting updated
         because there can be a scenario that a child task was initially delayed and parent was set to delayed and later child was updated to its task progress system is now not started and there are no child tasks that are delayed */
        StatType worstStat = updatedChildTask.getTaskProgressSystem();
        for (StatType childTaskStat : allChildTaskStats) {
            if (isWorseStat(childTaskStat, worstStat)) {
                worstStat = childTaskStat;
            }
        }
        if (parentTask.getTaskProgressSystem() == null || !parentTask.getTaskProgressSystem().equals(worstStat)) {
            if(parentTask.getTaskActStDate() != null && worstStat.getOrder() == StatType.NOTSTARTED.getOrder())
                parentTask.setTaskProgressSystem(StatType.ONTRACK);
            else
                parentTask.setTaskProgressSystem(worstStat);
            LocalDateTime currentDateTime = LocalDateTime.now();
            parentTask.setTaskProgressSystemLastUpdated(currentDateTime);
            if (parentTask.getSystemDerivedEndTs() == null || (updatedChildTask.getSystemDerivedEndTs() != null && parentTask.getSystemDerivedEndTs().isBefore(updatedChildTask.getSystemDerivedEndTs()))) {
                parentTask.setSystemDerivedEndTs(updatedChildTask.getSystemDerivedEndTs());
            }
            updateFieldsOfParentTask.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM);
            updateFieldsOfParentTask.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM_LAST_UPDATED);
        }

        if (!updateFieldsOfParentTask.isEmpty()) {
            taskHistoryService.addTaskHistoryOnSystemUpdate(parentTaskFromDb);
            taskRepository.save(parentTask);
            taskHistoryMetadataService.addTaskHistoryMetadata(updateFieldsOfParentTask, parentTask);
        }
    }

    private void modifyWorkflowOfParentTaskOnChildTaskUpdate(Task parentTask, Task updatedChildTask, List<String> updateFieldsOfChildTask, List<String> updateFieldsOfParentTask) {
        List<String> childTaskWorkflowStatusToCheck = List.of(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED, Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED, Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD);

        if (updateFieldsOfChildTask.contains(Constants.TaskFields.WORKFLOW_TASK_STATUS)) {

            if (childTaskWorkflowStatusToCheck.contains(updatedChildTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()) || updatedChildTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equals(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
                if (parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
                    if (parentTask.getTaskExpStartDate() == null) {
                        parentTask.setTaskExpStartDate(updatedChildTask.getTaskExpStartDate());
                        parentTask.setTaskExpStartTime(updatedChildTask.getTaskExpStartTime());
                        updateFieldsOfParentTask.add(Constants.TaskFields.EXP_START_DATE);
                    }

                    if (parentTask.getTaskExpEndDate() == null) {
                        parentTask.setTaskExpEndTime(updatedChildTask.getTaskExpEndTime());
                        parentTask.setTaskExpEndDate(updatedChildTask.getTaskExpEndDate());
                        updateFieldsOfParentTask.add(Constants.TaskFields.EXP_END_DATE);
                    }

                    if (updatedChildTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED)) {
                        parentTask.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, updatedChildTask.getTaskWorkflowId()));
                        parentTask.setTaskState(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskState());
                        updateFieldsOfParentTask.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                    }
                }
            }

            if (childTaskWorkflowStatusToCheck.contains(updatedChildTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase())) {
                if (parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) || parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
                    parentTask.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE, updatedChildTask.getTaskWorkflowId()));
                    parentTask.setTaskState(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskState());
                    if (updatedChildTask.getTaskActStDate() != null) {
                        parentTask.setTaskActStDate(updatedChildTask.getTaskActStDate());
                        parentTask.setTaskActStTime(updatedChildTask.getTaskActStTime());
                    } else {
                        parentTask.setTaskActStDate(LocalDateTime.now());
                        parentTask.setTaskActStTime(LocalTime.now());
                    }
                    updateFieldsOfParentTask.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                    updateFieldsOfParentTask.add(Constants.TaskFields.ACTUAL_START_DATE);
                } else if (parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED) && updatedChildTask.getTaskActStDate() != null && parentTask.getTaskActStDate() != null && updatedChildTask.getTaskActStDate().isBefore(parentTask.getTaskActStDate())) {
                    parentTask.setTaskActStDate(updatedChildTask.getTaskActStDate());
                    parentTask.setTaskActStTime(updatedChildTask.getTaskActStTime());
                    updateFieldsOfParentTask.add(Constants.TaskFields.ACTUAL_START_DATE);
                }
            }

            if (updatedChildTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
                List<Task> allChildTasks = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
                boolean isAllChildTaskCompleted = true;
                LocalDateTime lastActualEndDateOfChildTasks = updatedChildTask.getTaskActEndDate();
                for (Task childTask : allChildTasks) {
                    if (!childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
                        isAllChildTaskCompleted = false;
                    }
                    if (childTask.getTaskActEndDate() != null && childTask.getTaskActEndDate().isAfter(lastActualEndDateOfChildTasks)) {
                        lastActualEndDateOfChildTasks = childTask.getTaskActEndDate();
                    }
                }

                if (isAllChildTaskCompleted && !parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
                    if (parentTask.getIsBug() && (parentTask.getStepsTakenToComplete() == null || parentTask.getStepsTakenToComplete().isBlank() || parentTask.getResolutionId() == null)) {
                        throw new MissingDetailsException("Missing details in the parent task: 'Steps taken to complete'  and 'Resolution'");
                    }
                    parentTask.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, updatedChildTask.getTaskWorkflowId()));
                    validateAndNormalizeRca(parentTask);
                    parentTask.setTaskActEndDate(lastActualEndDateOfChildTasks);
                    parentTask.setTaskActEndTime(lastActualEndDateOfChildTasks.toLocalTime());
                    if (!updateFieldsOfParentTask.contains(Constants.TaskFields.WORKFLOW_TASK_STATUS))
                        updateFieldsOfParentTask.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                    updateFieldsOfParentTask.add(Constants.TaskFields.ACTUAL_END_DATE);
                }
            }
        }
    }

    /**
     * Changes to bypass stat calculation for already completed tasks. However, the task which has just completed i.e. because of change of
     * workflowTaskStatus to status COMPLETED for the first time then only the stat calculation will be invoked and the stat will be calculated for that task.
     * The task whose workflowTaskStatus is DELETE then the stats of that task will not be evaluated. The task whose workflowTaskStatus is BACKLOG and
     * its priority is (P0/P1) and its workflowType is other than SPRINT then only its stats will be evaluated. i.e. the stats of the rest of the tasks in
     * BACKLOG will not be evaluated. i.e. backlog tasks with priority (P2/P3/P4/no priority) will not be evaluated.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean isComputeStatAllowedByWorkflowStatus(Task taskToUpdate, Task updatedTask) {
        boolean isComputeStatAllowedByWorkflowStatus = true;
        // some business logic was present
        return isComputeStatAllowedByWorkflowStatus;
    }

    /* This method is used to compute and update the stats of the task which is getting updated in the task table. */
    public Task computeAndUpdateStatForTask(Task task, boolean isComputeStatAllowedByWorkflowStatus) {
        assert task != null;
        // some business logic was present
        return task;
    }

    public boolean isWorseStat(StatType childStat, StatType parentStat) {
        if (childStat != null && parentStat != null) {
            return childStat.getOrder() < parentStat.getOrder();
        }
        return childStat != null && parentStat == null;
    }

    /**
     * @param firstSystemDerivedTime
     * @param secondSystemDerivedTime
     * @return true if firstSystemDerivedTime is after/greater than secondSystemDerivedTime. In case of any value is null, we return true
     */
    public boolean isWorstSystemDerivedEndTs(LocalDateTime firstSystemDerivedTime, LocalDateTime secondSystemDerivedTime) {
        if (firstSystemDerivedTime != null && secondSystemDerivedTime != null) {
            return firstSystemDerivedTime.isAfter(secondSystemDerivedTime);
        }
        return true;
    }

    /* This method will check whether to compute the stat of the task or not. Stat will be calculated only for the tasks
     * which have either taskPriority, taskExpEndDate or taskExpEndTime */
    public boolean isComputeStatAllowedByPriority(Task task) {
        boolean isComputeStatAllowed = false;
        // some business logic was present
        return isComputeStatAllowed;
    }

    /**
     * This method is used to update the taskActEndDate, taskActEndTime and workflowTaskStatus COMPLETED. If at least anyone of the
     * above field is provided and the remaining are not provided then this method will update the taskActEndDate with the
     * current date, taskActEndTime with the current time and workflowTaskStatus of the task as COMPLETED. Also, for taskActEndDate
     * today, the taskActEndTime cannot be more than the current time and for taskActEndDate before today the taskActEndTime would
     * be the office end time of that workflow, if not provided.
     *
     * @param task task object which has to be checked and updated.
     */
    @Transactional(readOnly = true)
    public void checkAndUpdateTaskByWorkflowTaskStatus(Task task) {
        if (task != null) {
            LocalDateTime todayDate = LocalDateTime.now();
            java.time.LocalTime currentTime = DateTimeUtils.getLocalCurrentTime();
            boolean isTaskActualEndDateToday = false;
            boolean isTaskActualEndDateBeforeToday = false;
            boolean isTaskActualEndDateAfterToday = false;
            boolean isActEndDateOrEndTimePresent = isTaskActEndDateOrActEndTimePresent(task);
            boolean isWorkflowTaskStatusCompleted = workflowTaskStatusService.isTaskWorkflowStatusCompleted(task);
            if (isWorkflowTaskStatusCompleted || isActEndDateOrEndTimePresent || task.getFkWorkflowTaskStatus() == null) {
                updateCompletionImpactAndEstimateTimeLogEval(task);
                if (task.getTaskActEndDate() == null) {
                    LocalDateTime currentDate = DateTimeUtils.getLocalCurrentDate();
                    task.setTaskActEndDate(currentDate);

//                    -----------------------------------
                    if (DebugConfig.getInstance().isDebug()) {
                        System.out.println("Work Item actual end date was null and therefore putting sever current date : ");
                        System.out.println("server current date = " + currentDate);
                        System.out.println("Work Item actual end date in Work Item = " + task.getTaskActEndDate());
                    }
//                    ----------------------------------------
                } else {
                    if (task.getTaskActEndDate().toLocalDate().isAfter(todayDate.toLocalDate())) {
                        isTaskActualEndDateAfterToday = true;
                        String validationErrMess = "Work Item Actual End Date can only be today or less than today for workflow Work Item" + " status COMPLETED";
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException(validationErrMess));
                        logger.error("Completed Work Item validation failed: Work Item Actual End Date of a completed Work Item can only be current date or less than the current date but not " + " more than the current date" + " ,    " + "task workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "task actual end date = " + task.getTaskActEndDate() + " ,   " + "current date = " + todayDate.toLocalDate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException(validationErrMess);
                    } else {
                        if (task.getTaskActEndDate().toLocalDate().isBefore(todayDate.toLocalDate())) {
                            isTaskActualEndDateBeforeToday = true;
                        } else {
                            isTaskActualEndDateToday = true;
                        }
                    }
                }
                if (task.getTaskActEndTime() == null) {
                    if (isTaskActualEndDateBeforeToday) {
//                        OfficeHours foundOfficeHours = officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(Constants.OfficeHoursKeys.EOD_TIME, task.getTaskWorkflowId());
//                        LocalTime foundOfficeEndTime = foundOfficeHours.getValue();
                        LocalTime foundOfficeEndTime = entityPreferenceService.getOfficeHrsBasedOnEntityPreference(task).get("officeHrsEndTime");
                        task.setTaskActEndTime(foundOfficeEndTime);

//                        -------------------------------
                        if (DebugConfig.getInstance().isDebug()) {
                            System.out.println("Work Item actual end time is null and Work Item actual end date is before today and therefore putting office end time : ");
                            System.out.println("found office end time from Db = " + foundOfficeEndTime);
                            System.out.println("Work Item actual end time in Work Item = " + task.getTaskActEndTime());
                        }
//                        ------------------------------------
                    } else {
//                        java.time.LocalTime currentTime = DateTimeUtils.getLocalCurrentTime();
                        task.setTaskActEndTime(currentTime);

//                        -------------------------------
                        if (DebugConfig.getInstance().isDebug()) {
                            System.out.println("Work Item actual end time is null and Work Item actual end date is today and therefore putting current time : ");
                            System.out.println("server current time = " + currentTime);
                            System.out.println("Work Item actual end time in Work Item = " + task.getTaskActEndTime());
                        }
//                        ------------------------------------

                    }
                } else {
                    if (isTaskActualEndDateToday) {
//                        if (task.getTaskActEndDate().toLocalTime().isAfter(todayDate.toLocalTime())) {
                        if (task.getTaskActEndTime().isAfter(currentTime)) {
                            String validationErrMess = "Work Item Actual End Time cannot be more than the current time for Work Item actual " + "end date today and workflow task status COMPLETED";

//                            ------------------------------------
                            if (DebugConfig.getInstance().isDebug()) {
                                System.out.println("Work Item actual end date is after current date : " + validationErrMess);
                                System.out.println("Work Item actual end date in Work Item = " + task.getTaskActEndDate().toLocalTime());
                                System.out.println("current time = " + currentTime);
                                System.out.println("Work Item actual end time in Work Item = " + task.getTaskActEndTime());
                            }
//                            --------------------------------------

                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException(validationErrMess));
                            logger.error("Completed Work Item validation failed: Work Item Actual End Time of a completed Work Item cannot be more than the current time for Work Item actual end date as" + " today" + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "task actual end time = " + task.getTaskActEndTime() + " ,   " + "current time = " + currentTime, new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            throw new ValidationFailedException(validationErrMess);
                        }
                    }
                }
                if (task.getFkWorkflowTaskStatus() == null) {
                    workflowTaskStatusService.setTaskWorkflowStatusCompletedByWorkflowType(task);
                }
                if (task.getFkWorkflowTaskStatus() != null && !isWorkflowTaskStatusCompleted) {
                    workflowTaskStatusService.setTaskWorkflowStatusCompletedByWorkflowType(task);
                }
                if (task.getRecordedEffort() == null && task.getNewEffortTracks() == null) {
                    String message = "Recorded effort cannot be empty for Workflow Work Item Status COMPLETED";
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException(message));
                    logger.error("Completed Work Item validation failed: Recorded effort cannot be empty for the completed Work Item" + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "timeLog = " + task.getRecordedEffort(), new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ValidationFailedException(message);
                }
            }
        }
    }

    /* This method will only check whether the task has actual end date or actual end time or not*/
    public boolean isTaskActEndDateOrActEndTimePresent(Task task) {
        boolean isActualEndDateOrTimePresent = false;
        if (task != null) {
            if (task.getTaskActEndDate() != null || task.getTaskActEndTime() != null) {
                isActualEndDateOrTimePresent = true;
            }
        }
        return isActualEndDateOrTimePresent;
    }

    public ArrayList<HashMap<String, Object>> findHistoryFromTaskHistoryTable(long taskid) {
        List<TaskHistory> history = taskHistoryRepository.findByTaskIdOrderByVersionAsc(taskid);

        ArrayList<HashMap<String, Object>> arrayList1 = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> arrayList2 = new ArrayList<HashMap<String, Object>>();

        for (TaskHistory taskhistory : history) {
            HashMap<String, Object> map1 = objectMapper.convertValue(taskhistory, HashMap.class);
            map1.remove("version");
            arrayList1.add(map1);
        }

        Task task = taskRepository.findByTaskId(taskid);
        HashMap<String, Object> map2 = objectMapper.convertValue(task, HashMap.class);
        arrayList1.add(map2);

        for (int i = 0; i < (arrayList1.size() - 1); i++) {

            for (Map.Entry<String, Object> entry1 : arrayList1.get(i).entrySet()) {

                String key = entry1.getKey();
                Object value1 = entry1.getValue();
                Object value2 = arrayList1.get(i + 1).get(key);

                if (value1 == null && value2 == null) {
                    continue;
                } else {
                    if (value1 == null) {
                        HashMap<String, Object> finalMap = new HashMap<String, Object>();
                        finalMap.put("field name", key);
                        finalMap.put("old value", value1);
                        finalMap.put("new value", value2);
                        finalMap.put("lastUpdatedDateTime", arrayList1.get(i).get("lastUpdatedDateTime"));
                        arrayList2.add(finalMap);
                    } else {

                        if (value1.equals(value2)) {
                            continue;
                        } else {

                            HashMap<String, Object> finalMap = new HashMap<String, Object>();
                            finalMap.put("field name", key);
                            finalMap.put("old value", value1);
                            finalMap.put("new value", value2);
                            finalMap.put("lastUpdatedDateTime", arrayList1.get(i).get("lastUpdatedDateTime"));
                            arrayList2.add(finalMap);
                        }
                    }
                }
            }

        }
        return arrayList2;
    }


    //----------- NEW FUNCTIONS added for TASK - 2364 (Add reference entities) STARTS HERE--------------------------------------------

//    public boolean validateTaskReferenceEntityIdByTaskTypeId(Task task) {       // USED IN ADD TASK API CALL
//        boolean isValidated = true;
//        if (task.getTaskTypeId() != null) {
//            if (task.getReferenceEntityId() != null) {
//
//                if (task.getTaskTypeId().equals(Constants.Task_Type.TASK_TYPE_TASK)) {
//                    throw new ValidationFailedException("No reference task can be associated with TaskType 'TASK', either change referenceTaskTypeId or make ReferenceEntityId , null");
//                }
//                boolean exists = taskRepository.existsByTaskId(task.getReferenceEntityId());
//
//                if (!exists) {
//                    throw new ValidationFailedException("Reference Task does not exists, Wrong ReferenceEntityId entered");
//                }
//            } else if (task.getTaskTypeId().equals(Constants.Task_Type.TASK_TYPE_SUB_TASK)) {
//                throw new ValidationFailedException("Reference Entity Id cannot be null for task type 1 (sub task)");
//            }
//        }
//
//        return isValidated;
//    }
//
//    public boolean validateTaskTypeId(Task task, Task taskDb)          // USED IN UPDATE TASK API CALL
//    {
//        boolean isValidated = true;
//        if (taskDb.getReferenceEntityId() != null && !task.getReferenceEntityId().equals(taskDb.getReferenceEntityId())) {
//            throw new ValidationFailedException("Reference Task cannot be changed");
//        }
//        if (task.getTaskTypeId() != null && !task.getTaskTypeId().equals(taskDb.getTaskTypeId())) {
//            throw new ValidationFailedException("task type Id cannot be changed");
//        }
//        if (!Objects.equals(task.getTaskId(), taskDb.getTaskId())) {
//            throw new ValidationFailedException("Task mismatch");
//        }
//        return isValidated;
//    }

    public boolean validateTaskReferenceWorkItem(Task task) {
        boolean isValidated = true;
        List<Long> referenceWorkItemIds = task.getReferenceWorkItemId();

        if (referenceWorkItemIds != null && !referenceWorkItemIds.isEmpty()) {

            Task taskDb = null;
            if(task.getTaskId() != null) {
                taskDb = taskRepository.findByTaskId(task.getTaskId());
            }
            List<Long> existingReferenceWorkItemIds = new ArrayList<>();
            if(taskDb != null && taskDb.getReferenceWorkItemId() != null && !taskDb.getReferenceWorkItemId().isEmpty()) {
                existingReferenceWorkItemIds.addAll(taskDb.getReferenceWorkItemId());
            }

            for (Long referenceWorkItemId : referenceWorkItemIds) {

                boolean exists = taskRepository.existsByTaskId(referenceWorkItemId);
                if (!exists) {
                        throw new ValidationFailedException("Reference Work Item does not exist");
//                    continue;
                }
                if(!Objects.equals(taskRepository.findByTaskId(referenceWorkItemId).getFkTeamId().getTeamId(), task.getFkTeamId().getTeamId())){
                    throw new ValidationFailedException("Team of referenced Work Item and current Work Item is not same");
                }
                if (existingReferenceWorkItemIds != null && !existingReferenceWorkItemIds.isEmpty() && existingReferenceWorkItemIds.contains(referenceWorkItemId)) {
                    continue;
                }
                String referenceWorkItemTaskNumber = taskRepository.findByTaskId(referenceWorkItemId).getTaskNumber();
                if (task.getTaskId() != null && Objects.equals(task.getTaskId(), referenceWorkItemId)) {
                    throw new ValidationFailedException("A work item cannot reference itself. Invalid referenceWorkItemTaskNumber: " + referenceWorkItemTaskNumber);
                }

                Task referencedTask = taskRepository.findById(referenceWorkItemId).orElseThrow(() -> new ValidationFailedException("Referenced Work Item not found. Invalid referenceWorkItemTaskNumber: " + referenceWorkItemTaskNumber));

                if (Objects.equals(referencedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    throw new ValidationFailedException("Deleted Work Item with Work Item number " + referencedTask.getTaskNumber() + " cannot be added as a reference.");
                }

                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    throw new ValidationFailedException("Deleted Work Item cannot added other work item for a reference.");
                }

                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && Objects.equals(referencedTask.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && Objects.equals(referencedTask.getTaskId(), task.getParentTaskId())) {
                    throw new ValidationFailedException("A Child Task cannot reference its Parent Task. Invalid referenceWorkItemTaskNumber: " + referenceWorkItemTaskNumber);
                }

                if (task.getTaskId() != null && Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && Objects.equals(referencedTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && Objects.equals(referencedTask.getParentTaskId(), task.getTaskId())) {
                    throw new ValidationFailedException("A Parent Task cannot reference its Child Task. Invalid referenceWorkItemTaskNumber: " + referenceWorkItemTaskNumber);
                }

                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && Objects.equals(referencedTask.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && Objects.equals(referencedTask.getParentTaskId(), task.getParentTaskId())) {
                    throw new ValidationFailedException("A Child Task cannot reference its sibling Child Task. Invalid referenceWorkItemTaskNumber: " + referenceWorkItemTaskNumber);
                }
            }
        }

        return isValidated;
    }

    public boolean validateBugTaskRelationWithReferenceWorkItemId(Task task) {
        boolean isValidated = true;
        List<Long> bugTaskRelations = new ArrayList<>();
        if (task.getBugTaskRelation() != null && !task.getBugTaskRelation().isEmpty()) bugTaskRelations.addAll(task.getBugTaskRelation());
        List<Long> referenceWorkItemIds = task.getReferenceWorkItemId();
        List<LinkedTask> linkedTaskList = task.getLinkedTaskList();

        if(linkedTaskList != null && !linkedTaskList.isEmpty()) {
            for (LinkedTask linkedTask : linkedTaskList) {
                Long taskIdentifier = getTaskIdentifierFromTaskNumber(linkedTask.getTaskNumber());
                Task linkedTaskDb = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, task.getFkTeamId().getTeamId());
                if(linkedTaskDb == null) {
                    throw new EntityNotFoundException("Work Item in Associate Work Item with Work Item Number " + taskIdentifier + " is not found");
                }
                bugTaskRelations.add(linkedTaskDb.getTaskId());
            }
        }
        if (bugTaskRelations != null && !bugTaskRelations.isEmpty() && referenceWorkItemIds != null && !referenceWorkItemIds.isEmpty()) {

            for (Long bugTaskId : bugTaskRelations) {
                if (referenceWorkItemIds.contains(bugTaskId)) {
                    String taskNumber = taskRepository.findByTaskId(bugTaskId).getTaskNumber();
                    throw new ValidationFailedException("Work Item with Work Item number " + taskNumber + " is present in both Associated Work Item and Reference work item.");
                }
            }
        }

        return isValidated;
    }

    public boolean validateDependencyWithReferenceWorkItemId(Task task) {
        boolean isValidated = true;
//        List<Long> dependencyLists = task.getDependencyIds();
        List<Long> referenceWorkItemIds = task.getReferenceWorkItemId();
        List<Long> dependencyLists = new ArrayList<>();
        List<Long> dependencyTaskPredecessor = new ArrayList<>();
        if(task.getTaskId() != null) {
            dependencyTaskPredecessor.addAll(dependencyRepository.findPredecessorTaskIdBySuccessorTaskIdAndIsRemoved(task.getTaskId(), false));
        }
        if(dependencyTaskPredecessor != null && !dependencyTaskPredecessor.isEmpty()) {
            dependencyLists.addAll(dependencyTaskPredecessor);
        }

        List<Long> dependencyTaskSuccessor = new ArrayList<>();
        if(task.getTaskId() != null) {
            dependencyTaskSuccessor.addAll(dependencyRepository.findSuccessorTaskIdByPredecessorTaskIdAndIsRemoved(task.getTaskId(), false));
        }
        if(dependencyTaskSuccessor != null && !dependencyTaskSuccessor.isEmpty()) {
            dependencyLists.addAll(dependencyTaskSuccessor);
        }
        List<DependentTaskDetail> dependentTaskList = task.getDependentTaskDetailRequestList();

        if(dependentTaskList != null && !dependentTaskList.isEmpty()) {
            for (DependentTaskDetail dependentTaskDetail : dependentTaskList) {
                Task dependentTask = taskRepository.findByFkTeamIdTeamIdAndTaskNumber(task.getFkTeamId().getTeamId(), dependentTaskDetail.getRelatedTaskNumber());
                if(dependentTask == null) {
                    throw new EntityNotFoundException("Work Item in Dependency list with Work Item number " + dependentTaskDetail.getRelatedTaskNumber() + " not found");
                }
                dependencyLists.add(dependentTask.getTaskId());
            }
        }
        if (dependencyLists != null && !dependencyLists.isEmpty() && referenceWorkItemIds != null && !referenceWorkItemIds.isEmpty()) {
            for (Long dependencyList : dependencyLists) {
                if (referenceWorkItemIds.contains(dependencyList)) {
                    String taskNumber = taskRepository.findByTaskId(dependencyList).getTaskNumber();
                    throw new ValidationFailedException("Work Item with Work Item number " + taskNumber + " is present in both DependencyList and Reference work item .");
                }
            }
        }

        return isValidated;
    }

    public List<Long> getUpdatedReferencedTaskIds(Task referenceTask) {

        Task taskDb = taskRepository.findByTaskId(referenceTask.getTaskId());
        List<Long> newReferenceWorkItemIds = referenceTask.getReferenceWorkItemId();
        List<Long> oldReferenceWorkItemIds = taskRepository.findByTaskId(referenceTask.getTaskId()).getReferenceWorkItemId();
        List<Long> updatedReferencedTaskIds = new ArrayList<>();

        // Loop through the new referenceWorkItemIds and add to the updated list if not already present
        for (Long newReferenceWorkItemId : newReferenceWorkItemIds) {
            if (!updatedReferencedTaskIds.contains(newReferenceWorkItemId)) {
                updatedReferencedTaskIds.add(newReferenceWorkItemId);
            }
        }

        return updatedReferencedTaskIds;
    }

    private void modifyReferenceWorkItem(Task referenceTask) {
        List<Task> referencedTasksDb = taskRepository.findByTaskIdIn(referenceTask.getReferenceWorkItemId());
        for (Task referencedTaskDb : referencedTasksDb) {
            Task taskCopy = new Task();
            BeanUtils.copyProperties(referencedTaskDb, taskCopy);
//            taskHistoryService.addTaskHistoryOnSystemUpdate(taskCopy);
            List<Long> referenceWorkItemList;
            if (referencedTaskDb.getReferenceWorkItemId() != null) {
                referenceWorkItemList = new ArrayList<>(referencedTaskDb.getReferenceWorkItemId());
            } else {
                referenceWorkItemList = new ArrayList<>();
            }
            if(!referenceWorkItemList.contains(referenceTask.getTaskId())){
                referenceWorkItemList.add(referenceTask.getTaskId());
                taskHistoryService.addTaskHistoryOnSystemUpdate(taskCopy);
                referencedTaskDb.setReferenceWorkItemId(referenceWorkItemList);
                Task savedLinkedTask = taskRepository.save(referencedTaskDb);
            List<String> updateFields = Collections.singletonList(Constants.TaskFields.REFERENCE_WORK_ITEM_ID);
                taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, savedLinkedTask);
            }
            else {
                referencedTaskDb.setReferenceWorkItemId(referenceWorkItemList);
                Task savedLinkedTask = taskRepository.save(referencedTaskDb);
            }
        }
    }

    public void modifyDeletedReferenceWorkItem(Task referenceTask, List<Long> deletedReferenceWorkItemList) {
        List<Task> referencedTasksDb = taskRepository.findByTaskIdIn(deletedReferenceWorkItemList);
        for (Task referencedTaskDb : referencedTasksDb){
            Task taskCopy = new Task();
            BeanUtils.copyProperties(referencedTaskDb, taskCopy);
            taskHistoryService.addTaskHistoryOnSystemUpdate(taskCopy);
            List<Long> referenceWorkItemList = referencedTaskDb.getReferenceWorkItemId();
            referenceWorkItemList.remove(referenceTask.getTaskId());
            referencedTaskDb.setReferenceWorkItemId(referenceWorkItemList);
            Task savedLinkedTask = taskRepository.save(referencedTaskDb);
            List<String> updateFields = Collections.singletonList(Constants.TaskFields.REFERENCE_WORK_ITEM_ID);
            taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, savedLinkedTask);
        }
    }

    public List<Long> getDeletedReferencedTaskIds(Task referenceTask){
        Task taskDb = taskRepository.findByTaskId(referenceTask.getTaskId());
        List<Long> deletedReferenceWorkItemList = new ArrayList<>();
        List<Long> existingReferenceWorkItemIds = taskDb.getReferenceWorkItemId();
        List<Long> newReferenceWorkItemIds = referenceTask.getReferenceWorkItemId();

        if(existingReferenceWorkItemIds != null && !existingReferenceWorkItemIds.isEmpty()) {
            for (Long existingReferenceWorkItemId : existingReferenceWorkItemIds) {
                if (newReferenceWorkItemIds == null || newReferenceWorkItemIds.isEmpty() || !newReferenceWorkItemIds.contains(existingReferenceWorkItemId)) {
                    deletedReferenceWorkItemList.add(existingReferenceWorkItemId);
                }
            }
        }

        return deletedReferenceWorkItemList;
    }

    public void updateReferenceWorkItem(Task referenceTask) {
        if (referenceTask.getReferenceWorkItemId() != null && !referenceTask.getReferenceWorkItemId().isEmpty()) {
            List<Long> updatedReferencedTaskIdList = getUpdatedReferencedTaskIds(referenceTask);
            referenceTask.setReferenceWorkItemId(updatedReferencedTaskIdList);
            modifyReferenceWorkItem(referenceTask);
        }
    }

    public void deleteReferenceWorkItem(Task referenceTask){
        Task taskDb = taskRepository.findByTaskId(referenceTask.getTaskId());
        if (taskDb.getReferenceWorkItemId() != null && !taskDb.getReferenceWorkItemId().isEmpty()) {
            List<Long> deletedReferenceWorkItemList = getDeletedReferencedTaskIds(referenceTask);
            modifyDeletedReferenceWorkItem(referenceTask, deletedReferenceWorkItemList);
        }
    }

    List<ReferenceWorkItem> findReferenceWorkItemList(Task task){
        List<ReferenceWorkItem> referenceWorkItems = new ArrayList<>();
        Task taskCopy = new Task();
        BeanUtils.copyProperties(task, taskCopy);
        List<Long> referenceWorkItemIds = taskCopy.getReferenceWorkItemId();
        List<Task> referenceTasks = new ArrayList<>();
        if (referenceWorkItemIds != null && !referenceWorkItemIds.isEmpty()) referenceTasks = taskRepository.findByTaskIdIn(referenceWorkItemIds).stream()
                .map(originalTask -> {
                    Task copy = new Task();
                    BeanUtils.copyProperties(originalTask, copy);
                    return copy;
                })
                .collect(Collectors.toList());

        if(referenceTasks != null && !referenceTasks.isEmpty()) {
            for (Task referenceTask : referenceTasks) {
                ReferenceWorkItem referenceWorkItem = new ReferenceWorkItem();
                referenceWorkItem.setTaskId(referenceTask.getTaskId());
                referenceWorkItem.setTaskNumber(referenceTask.getTaskNumber());
                referenceWorkItem.setTaskTitle(referenceTask.getTaskTitle());
                referenceWorkItem.setUserPerceivedPercentageTaskCompleted(referenceTask.getUserPerceivedPercentageTaskCompleted());
                referenceWorkItems.add(referenceWorkItem);
            }
        }
        return referenceWorkItems;
    }

    public void addEpicInTask(Epic epic, Task task) throws IllegalAccessException {

        if(task.getTaskEstimate() != null) {
            List<Integer> statusList = new ArrayList<>();
            statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
            statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
            statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
            if(statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                epic.setOriginalEstimate((epic.getOriginalEstimate() == null ? task.getTaskEstimate() : epic.getOriginalEstimate() + task.getTaskEstimate()));
            }
            epic.setRunningEstimate((epic.getRunningEstimate() == null ? task.getTaskEstimate() : epic.getRunningEstimate() + task.getTaskEstimate()));
        }

        if(epicTaskRepository.existsByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId())) {
            EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId());
            epicTask.setIsDeleted(false);
            epicTaskRepository.save(epicTask);
        }
        else {
            EpicTask epicTask = new EpicTask();
            epicTask.setFkEpicId(epic);
            epicTask.setFkTaskId(task);
            epicTask.setIsDeleted(false);
            epicTaskRepository.save(epicTask);
        }
    }

    public void removeEpicFromTask(Epic epic, Task task) throws IllegalAccessException {

        if(task.getTaskEstimate() != null) {
            List<Integer> statusList = new ArrayList<>();
            statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
            statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
            statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
            if(statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                epic.setOriginalEstimate(epic.getOriginalEstimate() - task.getTaskEstimate());
            }
            epic.setRunningEstimate(epic.getRunningEstimate() - task.getTaskEstimate());
        }
        task.setFkEpicId(null);
        EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId());
        epicTask.setIsDeleted(true);
        epicTaskRepository.save(epicTask);
    }

    public void addParentTaskInEpic (Epic epic, Task task, List<Task> childTaskList) throws IllegalAccessException {
        for (Task childTask : childTaskList) {
            if ((!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) &&
                    !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) ||
                    (Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) && childTask.getFkEpicId() == null)) {
                Task childTaskRequest = new Task();
                BeanUtils.copyProperties(childTask, childTaskRequest);
                epicService.addTaskToEpic(epic, childTaskRequest, null, false);
            }
        }
        if(epicTaskRepository.existsByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId())) {
            EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId());
            epicTask.setIsDeleted(false);
            epicTaskRepository.save(epicTask);
        }
        else {
            EpicTask epicTask = new EpicTask();
            epicTask.setFkEpicId(epic);
            epicTask.setFkTaskId(task);
            epicTask.setIsDeleted(false);
            epicTaskRepository.save(epicTask);
        }
    }

    public void removeParentTaskFromEpic (Epic epic, Task task, List<Task> childTaskList) throws IllegalAccessException {
        for (Task childTask : childTaskList) {
            if (!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) &&
                    !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                Task childTaskRequest = new Task();
                BeanUtils.copyProperties(childTask, childTaskRequest);
                epicService.removeTaskFromEpic(epic, childTaskRequest, null, false);
            }
        }
        EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId());
        epicTask.setIsDeleted(true);
        epicTaskRepository.save(epicTask);
    }

    public void moveParentTaskInEpic(Epic epic, Epic epicDb, Task task, Task taskFromDb, List<Task> childTaskList) {
        Map<Task, List<String>> taskUpdateMap = new HashMap<>();
        Map<Task, Task> updatedTaskToTaskCopy = new HashMap<>();
        for (Task childTask : childTaskList) {
            if (!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) &&
                    !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                Task childTaskCopy = new Task();
                BeanUtils.copyProperties(childTask, childTaskCopy);
                List<String> updatedFields = new ArrayList<>();
                if (epic.getExpEndDateTime() != null) {
                    if (childTask.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && childTask.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || childTask.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                        if (task.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                            childTask.setTaskExpEndDate(epic.getExpEndDateTime());
                            childTask.setTaskExpEndTime(epic.getExpEndDateTime().toLocalTime());
                        } else {
                            childTask.setTaskExpEndDate(task.getTaskExpEndDate());
                            childTask.setTaskExpEndTime(task.getTaskExpEndDate().toLocalTime());
                        }
                        updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                    }
                }
                if (epic.getExpStartDateTime() != null) {
                    if (childTask.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && childTask.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || childTask.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                        if(task.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && task.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                            childTask.setTaskExpStartDate(epic.getExpStartDateTime());
                            childTask.setTaskExpStartTime(epic.getExpStartDateTime().toLocalTime());
                        }
                        else {
                            childTask.setTaskExpStartDate(task.getTaskExpStartDate());
                            childTask.setTaskExpStartTime(task.getTaskExpStartDate().toLocalTime());
                        }
                        updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                    }
                }
                if (childTask.getTaskEstimate() != null) {
                    List<Integer> statusList = new ArrayList<>();
                    statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
                    statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
                    statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
                    if (statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                        epic.setOriginalEstimate((epic.getOriginalEstimate() == null ? childTask.getTaskEstimate() : epic.getOriginalEstimate() + childTask.getTaskEstimate()));
                    }
                    epic.setRunningEstimate((epic.getRunningEstimate() == null ? childTask.getTaskEstimate() : epic.getRunningEstimate() + childTask.getTaskEstimate()));

                    if (statusList.contains(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                        epicDb.setOriginalEstimate(epicDb.getOriginalEstimate() - childTask.getTaskEstimate());
                    }
                    epicDb.setRunningEstimate(epicDb.getRunningEstimate() - childTask.getTaskEstimate());
                }
                childTask.setFkEpicId(epic);
                updatedFields.add(Constants.TaskFields.EPIC_ID);
                updatedTaskToTaskCopy.put(childTask, childTaskCopy);
                taskUpdateMap.put(childTask, updatedFields);
            }
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

            EpicTask epicDbTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epicDb.getEpicId(), taskKey.getTaskId());
            epicDbTask.setIsDeleted(true);
            epicTaskRepository.save(epicDbTask);
        });

        if(epicTaskRepository.existsByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId())) {
            EpicTask epicTask = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epic.getEpicId(), task.getTaskId());
            epicTask.setIsDeleted(false);
            epicTaskRepository.save(epicTask);
        }
        else {
            EpicTask epicTask = new EpicTask();
            epicTask.setFkEpicId(epic);
            epicTask.setFkTaskId(task);
            epicTask.setIsDeleted(false);
            epicTaskRepository.save(epicTask);
        }

        EpicTask epicTaskDb = epicTaskRepository.findByFkEpicIdEpicIdAndFkTaskIdTaskId(epicDb.getEpicId(), taskFromDb.getTaskId());
        epicTaskDb.setIsDeleted(true);
        epicTaskRepository.save(epicTaskDb);
    }

    public void modifyEpicPropertiesForTask(Task task, Task taskFromDb, String timeZone) throws IllegalAccessException {

        Epic epic = null;
        Epic epicDb = null;
        if(task.getFkEpicId() != null) {
            epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
        }
        if(taskFromDb.getFkEpicId() != null) {
            epicDb = epicRepository.findByEpicId(taskFromDb.getFkEpicId().getEpicId());
        }

        if ((epic != null && epicDb == null) || (epic == null && epicDb != null) || (epic != null && !Objects.equals(epic.getEpicId(), epicDb.getEpicId()))) {
            List<Task> childTaskList = new ArrayList<>();
            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                childTaskList = taskRepository.findByParentTaskId(task.getTaskId());
            }
            Task taskRequest = new Task();
            BeanUtils.copyProperties(task, taskRequest);
            Task taskFromDbRequest = new Task();
            BeanUtils.copyProperties(taskFromDb, taskFromDbRequest);
            if (epic != null && epicDb == null) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    addParentTaskInEpic (epic, taskRequest, childTaskList);
                }
                else {
                    addEpicInTask(epic, taskRequest);
                }
            }
            else if (epic == null && epicDb != null) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    removeParentTaskFromEpic (epicDb, taskFromDbRequest, childTaskList);
                }
                else {
                    removeEpicFromTask(epicDb, taskFromDbRequest);
                }
            }
            else {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    moveParentTaskInEpic(epic, epicDb, taskRequest, taskFromDbRequest, childTaskList);
                }
                else {
                    addEpicInTask(epic, taskRequest);
                    removeEpicFromTask(epicDb, taskFromDbRequest);
                }
            }
        }
        else if(task.getFkEpicId() != null && !Objects.equals(task.getTaskEstimate(), taskFromDb.getTaskEstimate())) {
            List<Integer> statusList = new ArrayList<>();
            statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
            statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
            statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
            if(statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                epic.setOriginalEstimate((epic.getOriginalEstimate() == null ? task.getTaskEstimate() : epic.getOriginalEstimate() + (task.getTaskEstimate() == null ? 0 : task.getTaskEstimate()) - (taskFromDb.getTaskEstimate() == null ? 0 : taskFromDb.getTaskEstimate())));
            }
            epic.setRunningEstimate((epic.getRunningEstimate() == null ? task.getTaskEstimate() : epic.getRunningEstimate() + (task.getTaskEstimate() == null ? 0 : task.getTaskEstimate()) - (taskFromDb.getTaskEstimate() == null ? 0 : taskFromDb.getTaskEstimate())));
        }
        if(epic != null && epicDb != null && Objects.equals(epic.getEpicId(), epicDb.getEpicId()) && Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId()) && (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE))) {
            epic.setFkWorkflowEpicStatus(workFlowEpicStatusRepository.findByWorkflowEpicStatus(Constants.WorkflowEpicStatusConstants.STATUS_IN_PROGRESS));
            epic.setActStartDateTime(task.getTaskActStDate());
            List<AccountId> accountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, epic.getTeamIdList(), Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION, true);
            List<Long> authorizedAccountIds = accountIdList.stream().map(AccountId::getAccountId).collect(Collectors.toList());

            List<HashMap<String, String>> payload = notificationService.sendNotificationForEpicStarted(epic, authorizedAccountIds, timeZone);
            sendPushNotification(payload);
        }
        if(epic != null) {
            epicRepository.save(epic);
        }
        if(epicDb != null) {
            epicRepository.save(epicDb);
        }
    }

    public boolean validateBugTaskRelationWithDependencyWorkItem(Task task) {
        boolean isValidated = true;
        List<Long> bugTaskRelations = new ArrayList<>();
        if (task.getBugTaskRelation() != null) bugTaskRelations.addAll(task.getBugTaskRelation());
        List<LinkedTask> linkedTaskList = task.getLinkedTaskList();

        if(linkedTaskList != null && !linkedTaskList.isEmpty()) {
            for (LinkedTask linkedTask : linkedTaskList) {
                Long taskIdentifier = getTaskIdentifierFromTaskNumber(linkedTask.getTaskNumber());
                Task linkedTaskDb = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, task.getFkTeamId().getTeamId());
                if(linkedTaskDb == null) {
                    throw new EntityNotFoundException("Work Item in Associate Work Item with Work Item Number " + taskIdentifier + " is not found");
                }
                bugTaskRelations.add(linkedTaskDb.getTaskId());
            }
        }

        List<Long> dependencyLists = new ArrayList<>();
        List<Long> dependencyTaskPredecessor = dependencyRepository.findPredecessorTaskIdBySuccessorTaskIdAndIsRemoved(task.getTaskId(), false);
        if(dependencyTaskPredecessor != null && !dependencyTaskPredecessor.isEmpty()) {
            dependencyLists.addAll(dependencyTaskPredecessor);
        }
        List<Long> dependencyTaskSuccessor = dependencyRepository.findSuccessorTaskIdByPredecessorTaskIdAndIsRemoved(task.getTaskId(), false);
        if(dependencyTaskSuccessor != null && !dependencyTaskSuccessor.isEmpty()) {
            dependencyLists.addAll(dependencyTaskSuccessor);
        }
        List<DependentTaskDetail> dependentTaskList = task.getDependentTaskDetailRequestList();

        if(dependentTaskList != null && !dependentTaskList.isEmpty()) {
            for (DependentTaskDetail dependentTaskDetail : dependentTaskList) {
                Task dependentTask = taskRepository.findByFkTeamIdTeamIdAndTaskNumber(task.getFkTeamId().getTeamId(), dependentTaskDetail.getRelatedTaskNumber());
                if(dependentTask == null) {
                    throw new EntityNotFoundException("Work Item in dependency list with Work Item number " + dependentTaskDetail.getRelatedTaskNumber() + " not found");
                }
                dependencyLists.add(dependentTask.getTaskId());
            }
        }
        if (bugTaskRelations != null && !bugTaskRelations.isEmpty() && dependencyLists != null && !dependencyLists.isEmpty()) {

            for (Long bugTaskId : bugTaskRelations) {
                if (dependencyLists.contains(bugTaskId)) {
                    String taskNumber = taskRepository.findByTaskId(bugTaskId).getTaskNumber();
                    throw new ValidationFailedException("Work Item with Work Item number " + taskNumber + " is present in both Dependency list and Associated Work Item.");
                }
            }
        }

        return isValidated;
    }

    // ---------------------------------  ENDS HERE  ----------------------------------------------------------------------------------

    /**
     * This method will validate the task for the estimate. If the workflowStatus of the task is "NOT STARTED" or "STARTED" and taskEstimate
     * is not present i.e. taskEstimate is null then this method will derive an estimate for the task and will also raise an exception TaskEstimateException.
     *
     * @param task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskEstimateByWorkflowTaskStatus(Task task) {
        boolean isTaskEstimateValidatedByWorkflowTaskStatus = true;
        if (task != null) {
            if (task.getFkWorkflowTaskStatus() != null) {
                WorkFlowTaskStatus foundWorkflowTaskStatusDb = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
                ArrayList<String> toValidateWorkflowStatusForTaskEstimate = new ArrayList<>();
                toValidateWorkflowStatusForTaskEstimate.add(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
                toValidateWorkflowStatusForTaskEstimate.add(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase());
                if (toValidateWorkflowStatusForTaskEstimate.contains(foundWorkflowTaskStatusDb.getWorkflowTaskStatus().toLowerCase())) {
                    if (task.getIsEstimateSystemGenerated() == null || task.getIsEstimateSystemGenerated() != 1) {
                        if (task.getTaskEstimate() == null || task.getTaskEstimate() == 0) {
                            if (task.getTaskEstimate()!=null && task.getTaskEstimate() == 0)
                                throw new ForbiddenException("Work Item estimate cannot be 0.");
                            if (task.getTaskPriority() != null) {
                                Long derivedEstimateInMillis = statsService.deriveTimeEstimateForTask(task);
                                Integer derivedEstimateInMinutes = Math.toIntExact(derivedEstimateInMillis / (60 * 1000));
                                task.setTaskEstimate(derivedEstimateInMinutes);
                                task.setIsEstimateSystemGenerated(1);
                                String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskEstimateException(derivedEstimateInMinutes));
                                logger.error("Work Item Estimate is mandatory for started and not started Work Item: taskNumber = " + task.getTaskNumber() + " whose workflow status is " + task.getFkWorkflowTaskStatus().getWorkflowTaskState() + " does not have task estimate. Hence, system generated estimate = " + derivedEstimateInMinutes + " is taken.", new Throwable(allStackTraces));
                                ThreadContext.clearMap();
                                throw new TaskEstimateException(derivedEstimateInMinutes);
                            } else {
                                isTaskEstimateValidatedByWorkflowTaskStatus = false;
                            }
                        }
                    }
                }
            }
        }
        return isTaskEstimateValidatedByWorkflowTaskStatus;
    }

    /**
     * This method will call the validation methods based on its workflow status to validate the task with all the validations that are applicable
     * to that status. Ex. If the workflow status of the task is not started then it will call the method which validates the not started task.
     *
     * @param task object which has to be validated based on its workflow status.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatus(Task task) {
        boolean isTaskValidatedByWorkflowStatus = false;
        if (task != null && task.getFkWorkflowTaskStatus() != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if (foundWorkflowTaskStatus != null) {
                switch (foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase()) {
                    case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
                        boolean isTaskValidatedForWorkflowStatusNotStarted = validateTaskForWorkflowStatusNotStarted(task);
                        if (isTaskValidatedForWorkflowStatusNotStarted) {
                            isTaskValidatedByWorkflowStatus = true;
                        }
                        break;

                    case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
                        boolean isTaskValidatedByWorkflowStatusBacklog = validateTaskForWorkflowStatusBacklog(task);
                        if (isTaskValidatedByWorkflowStatusBacklog) {
                            isTaskValidatedByWorkflowStatus = true;
                        }
                        break;

                    default:
                        break;
                }
            }
        }
        return isTaskValidatedByWorkflowStatus;
    }

    /**
     * This method will validate only the task which is in backlog with all the validations which are applicable.
     * Ex. Suppose the workflow status of the task is backlog then it will call all the methods which are validating
     * the backlog tasks.
     *
     * @param task object which has to be validated based on its workflow status.
     * @return boolean values.
     */
    public boolean validateTaskForWorkflowStatusBacklog(Task task) {
        boolean isPriorityValidated = false, isTaskValidatedForWorkflowStatusBacklog = false;
        if(task.getTaskEstimate()!=null &&task.getTaskEstimate().equals(0))
            task.setTaskEstimate(null);
        if (task != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if (foundWorkflowTaskStatus != null) {
                if (Objects.equals(foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase())) {
                    isPriorityValidated = validatePriorityInBacklogTask(task);
                    if(Objects.equals(task.getImmediateAttention(), Constants.BooleanValues.BOOLEAN_TRUE)) {
                        throw new ValidationFailedException("Backlog Work Item cannot be created with immediate attention");
                    }
                }
            }
        }
        if (isPriorityValidated) {
            isTaskValidatedForWorkflowStatusBacklog = true;
        }
        return isTaskValidatedForWorkflowStatusBacklog;
    }


    /**
     * This method is used to validate only the tasks which are in backlog. The backlog tasks should
     * neither have the task priorities "P0" nor "P1".
     *
     * @param task object.
     * @return boolean value.
     */
    public boolean validatePriorityInBacklogTask(Task task) {
        boolean isPriorityValidated = false;
        List<String> invalidPriorities = List.of(Constants.PRIORITY_P0, Constants.PRIORITY_P1);
        if (task.getTaskPriority() != null && invalidPriorities.contains(task.getTaskPriority())) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if (foundWorkflowTaskStatus != null && !Objects.equals(task.getTaskWorkflowId(), Constants.TaskWorkFlowIds.SPRINT)) {
                if (Objects.equals(foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase())) {
                    throw new ForbiddenException("'P0' and 'P1' priority are not allowed for 'Backlog' Work Item");
                }
            }
        } else {
            isPriorityValidated = true;
        }
        return isPriorityValidated;
    }


    /**
     * This method will validate only the task which is not started with all the validations which are applicable.
     * Ex. Suppose the workflow status of the task is not started then it will call all the methods which are validating
     * the not started task.
     *
     * @param task object which has to be validated based on its workflow status.
     * @return boolean values.
     */
    public boolean validateTaskForWorkflowStatusNotStarted(Task task) {
        boolean isTaskValidatedForWorkflowStatusNotStarted = false, isTaskDateTimePriorEstimateValidated = false, isEstimateValidated = false;
        if (task != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if (foundWorkflowTaskStatus != null) {
                if (Objects.equals(foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase())) {
                    isEstimateValidated = validateTaskEstimateByWorkflowTaskStatus(task);
                    isTaskDateTimePriorEstimateValidated = validateDateTimePriorityEstimateForNotStartedTask(task);
                }
            }
        }
        if (isTaskDateTimePriorEstimateValidated && isEstimateValidated) {
            isTaskValidatedForWorkflowStatusNotStarted = true;
        }
        return isTaskValidatedForWorkflowStatusNotStarted;
    }

    /**
     * This method will only validate the "Not Started" task. If the workflow task status of the task is Not Started then the
     * task should have task expected start date, task expected start time  task expected end date, task expected end time,
     * task priority and task estimate. This will also log the error in the error log table.
     *
     * @param task which has to be validated based on its workflow status.
     * @return boolean values.
     */
    public boolean validateDateTimePriorityEstimateForNotStartedTask(Task task) {
        boolean isTaskValidated = true;
        if (task != null) {
            WorkFlowTaskStatus foundWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            if (foundWorkflowTaskStatus != null) {
                if (Objects.equals(foundWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase())) {
                    if (task.getTaskExpStartDate() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected Start Date can not be null for Not Started Work Item"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected Start Date can not be null for Not Started Work Item" + " ,     " + "taskNumber = " + task.getTaskNumber() + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,   " + "task expected start date = " + task.getTaskExpStartDate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected Start Date can not be null for Not Started Work Item");
                    }
                    if (task.getTaskExpStartTime() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected Start Time can not be null for Not Started Work Item"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected Start Time can not be null for Not Started Work Item" + " ,     " + "taskNumber = " + task.getTaskNumber() + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "Work Item expected start time = " + task.getTaskExpStartTime(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected Start Time can not be null for Not Started Work Item");
                    }
                    if (task.getTaskExpEndDate() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected End Date can not be null for Not Started Work Item"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected End Date can not be null for Not Started Work Item" + " ,     " + "taskNumber = " + task.getTaskNumber() + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "Work Item expected end date = " + task.getTaskExpEndDate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected End Date can not be null for Not Started Work Item");
                    }
                    if (task.getTaskExpEndTime() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected End Time can not be null for Not Started Work Item"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected End Time can not be null for Not Started Work Item" + " ,     " + "taskNumber = " + task.getTaskNumber() + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "Work Item expected end time = " + task.getTaskExpEndTime(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected End Time can not be null for Not Started Work Item");
                    }
                    if (task.getTaskPriority() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Priority can not be null for Not Started Work Item"));
                        logger.error("Not Started Work Item validation failed: Work Item Priority can not be null for Not Started Work Item" + " ,     " + "taskNumber = " + task.getTaskNumber() + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "Work Item Priority = " + task.getTaskPriority(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Priority can not be null for Not Started Work Item");
                    }
                    if (task.getTaskEstimate() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Estimate can not be null for Not Started Work Item"));
                        logger.error("Not Started Work Item validation failed: Work Item Estimate can not be null for Not Started Work Item" + " ,     " + "taskNumber = " + task.getTaskNumber() + " ,    " + "Work Item workflow status = " + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " ,    " + "Work Item estimate = " + task.getTaskEstimate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Estimate can not be null for Not Started Work Item");
                    }
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the task which is Not Started. If the workflow task status of the task has been updated
     * to "Not-Started" then the task should have task expected start date, task expected start time, task expected end date,
     * task expected end time, task priority and task estimate. This will also log error in the error log table and will throw ValidationFailedException.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateDateTimePriorityEstimateForNotStartedTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = true;
        if (taskToUpdate != null && updatedTask != null) {
            if (taskToUpdate.getFkWorkflowTaskStatus() != null && updatedTask.getFkWorkflowTaskStatus() != null) {
                WorkFlowTaskStatus foundWorkflowTaskStatusDbOldTask = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
                WorkFlowTaskStatus foundWorkflowTaskStatusDbNewTask = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());

                if (Objects.equals(foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase())) {
                    if (updatedTask.getTaskExpStartDate() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected Start Date can not be null, if workflow status changed to Not-Started"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected Start Date can not be null, if workflow status changed to Not-Started" + " ,    " + "Work Item old workflow status = " + foundWorkflowTaskStatusDbOldTask.getWorkflowTaskStatus() + " ,   " + "Work Item new workflow status = " + foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus() + " ,   " + "Work Item expected start date = " + updatedTask.getTaskExpStartDate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected Start Date can not be null, if workflow status changed to Not-Started");
                    }
                    if (updatedTask.getTaskExpStartTime() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected Start Time can not be null, if workflow status changed to Not-Started"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected Start Time can not be null, if workflow status changed to Not-Started" + " ,    " + "Work Item old workflow status = " + foundWorkflowTaskStatusDbOldTask.getWorkflowTaskStatus() + " ,   " + "Work Item new workflow status = " + foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus() + " ,   " + "Work Item expected start time = " + updatedTask.getTaskExpStartTime(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected Start Time can not be null, if workflow status changed to Not-Started");
                    }
                    if (updatedTask.getTaskExpEndDate() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected End Date can not be null, if workflow status changed to Not-Started"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected End Date can not be null, if workflow status changed to Not-Started" + " ,    " + "Work Item old workflow status = " + foundWorkflowTaskStatusDbOldTask.getWorkflowTaskStatus() + " ,   " + "Work Item new workflow status = " + foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus() + " ,   " + "Work Item expected end date = " + updatedTask.getTaskExpEndDate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected End Date can not be null, if workflow status changed to Not-Started");
                    }
                    if (updatedTask.getTaskExpEndTime() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Expected End Time can not be null, if workflow status changed to Not-Started"));
                        logger.error("Not Started Work Item validation failed: Work Item Expected End Time can not be null, if workflow status changed to Not-Started" + " ,    " + "Work Item old workflow status = " + foundWorkflowTaskStatusDbOldTask.getWorkflowTaskStatus() + " ,   " + "Work Item new workflow status = " + foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus() + " ,   " + "Work Item expected end time = " + updatedTask.getTaskExpEndTime(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Expected End time can not be null, if workflow status changed to Not-Started");
                    }
                    if (updatedTask.getTaskPriority() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Priority can not be null, if workflow status changed to Not-Started"));
                        logger.error("Not Started Work Item validation failed: Work Item Priority can not be null, if workflow status changed to Not-Started" + " ,    " + "Work Item old workflow status = " + foundWorkflowTaskStatusDbOldTask.getWorkflowTaskStatus() + " ,   " + "Work Item new workflow status = " + foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus() + " ,   " + "Work Item priority = " + updatedTask.getTaskPriority(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Priority can not be null, if workflow status changed to Not-Started");
                    }
                    if (updatedTask.getTaskEstimate() == null) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Estimate can not be null, if workflow status changed to Not-Started"));
                        logger.error("Not Started Work Item validation failed: Work Item Estimate can not be null, if workflow status changed to Not-Started" + " ,    " + "Work Item old workflow status = " + foundWorkflowTaskStatusDbOldTask.getWorkflowTaskStatus() + " ,   " + "Work Item new workflow status = " + foundWorkflowTaskStatusDbNewTask.getWorkflowTaskStatus() + " ,   " + "Work Item estimate = " + updatedTask.getTaskEstimate(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Work Item Estimate can not be null, if workflow status changed to Not-Started");
                    }
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method will validate the task for the estimate. If the workflow status of the task is getting updated
     * to the workflowStatus of the task "NOT STARTED", "STARTED" or "COMPLETED" and taskEstimate is not present then this method will validate.
     * i.e. taskEstimate is null then this method will derive an estimate for the task and will also raise an exception TaskEstimateException.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    @Transactional(readOnly = true)
    public boolean validateTaskEstimateByWorkflowTaskStatus(Task taskToUpdate, Task updatedTask) {
        boolean isTaskEstimateValidatedByWorkflowTaskStatus = true;
        if (taskToUpdate != null && updatedTask != null) {
            if (taskToUpdate.getFkWorkflowTaskStatus() != null && updatedTask.getFkWorkflowTaskStatus() != null) {
                WorkFlowTaskStatus foundWorkflowTaskStatusDbUpdatedTask = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());

                ArrayList<String> toValidateWorkflowStatusForTaskEstimate = new ArrayList<>();
                toValidateWorkflowStatusForTaskEstimate.add(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
                toValidateWorkflowStatusForTaskEstimate.add(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase());
                toValidateWorkflowStatusForTaskEstimate.add(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED.toLowerCase());

                if (toValidateWorkflowStatusForTaskEstimate.contains(foundWorkflowTaskStatusDbUpdatedTask.getWorkflowTaskStatus().toLowerCase())) {
                    if (updatedTask.getIsEstimateSystemGenerated() == null || updatedTask.getIsEstimateSystemGenerated() != 1) {
                        if (updatedTask.getTaskEstimate() == null) {
                            if (updatedTask.getTaskPriority() != null) {
                                Long derivedEstimateInMillis = statsService.deriveTimeEstimateForTask(updatedTask);
                                Integer derivedEstimateInMinutes = Math.toIntExact(derivedEstimateInMillis / (60 * 1000));
                                updatedTask.setTaskEstimate(derivedEstimateInMinutes);
                                updatedTask.setIsEstimateSystemGenerated(1);
                                throw new TaskEstimateException(derivedEstimateInMinutes);
                            } else {
                                isTaskEstimateValidatedByWorkflowTaskStatus = false;
                            }
                        }
                    }
                }
            }
        }
        return isTaskEstimateValidatedByWorkflowTaskStatus;
    }

    /**
     * This method is used to validate the task workflow type.
     * The workflowIds = 1,2 and 3 are allowed. These Ids are as per workflowType table.
     *
     * @param task object which has to be validated.
     * @return boolean values.
     */
    public Boolean validateTaskWorkflowType(Task task) {
        Boolean isTaskWorkflowTypeValidated = true;
        List<Integer> workflowIds = new ArrayList<>();
//        workflowIds.add(1); this is a temporary fix
        workflowIds.add(3);
        workflowIds.add(6);

        Integer taskWorkflowId = task.getTaskWorkflowId();
        if (!workflowIds.contains(taskWorkflowId)) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new WorkflowTypeDoesNotExistException());
            logger.error("Task Workflow type failed: task workflowId = " + task.getTaskWorkflowId() + " is not allowed for a task.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new WorkflowTypeDoesNotExistException();
        }
        return isTaskWorkflowTypeValidated;
    }

    /**
     * This method is used to validate the task for all its date and time fields. All the date and time fields of a task should always exist
     * in pairs.
     *
     * @param task object whose date and time has to be validated for pairs.
     * @return boolean values.
     */
    public boolean validateAllDateAndTimeForPairs(Task task) {
        boolean isTaskValidatedForDateAndTimePairs = true;
        if (task != null) {
            if ((task.getTaskActStDate() != null && task.getTaskActStTime() == null) || (task.getTaskActStDate() == null && task.getTaskActStTime() != null)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Actual Start Date", "Actual Start Time"));
                logger.error("Date and Time should always be in pairs: " + "Work Item actual start date = " + task.getTaskActStDate() + " and Work Item actual start time = " + task.getTaskActStTime() + " are not in pair.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new DateAndTimePairFailedException("Actual Start Date", "Actual Start Time");
            }
            if ((task.getTaskActEndDate() != null && task.getTaskActEndTime() == null) || (task.getTaskActEndDate() == null && task.getTaskActEndTime() != null)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Actual End Date", "Actual End Time"));
                logger.error("Date and Time should always be in pairs: " + "Work Item actual end date = " + task.getTaskActEndDate() + " and Work Item actual end time = " + task.getTaskActEndTime() + " are not in pair.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new DateAndTimePairFailedException("Actual End Date", "Actual End Time");
            }
            if ((task.getTaskExpStartDate() != null && task.getTaskExpStartTime() == null) || (task.getTaskExpStartDate() == null && task.getTaskExpStartTime() != null)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Expected Start Date", "Expected Start Time"));
                logger.error("Date and Time should always be in pairs: " + "Work Item expected start date = " + task.getTaskExpStartDate() + " and Work Item expected start time = " + task.getTaskExpStartTime() + " are not in pair.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new DateAndTimePairFailedException("Expected Start Date", "Expected Start Time");
            }
            if ((task.getTaskExpEndDate() != null && task.getTaskExpEndTime() == null) || (task.getTaskExpEndDate() == null && task.getTaskExpEndTime() != null)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Expected End Date", "Expected End Time"));
                logger.error("Date and Time should always be in pairs: " + "Work Item expected end date = " + task.getTaskExpEndDate() + " and Work Item expected end time = " + task.getTaskExpEndTime() + " are not in pair.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new DateAndTimePairFailedException("Expected End Date", "Expected End Time");
            }
            if ((task.getTaskCompletionDate() != null && task.getTaskCompletionTime() == null) || (task.getTaskCompletionDate() == null && task.getTaskCompletionTime() != null)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Completion Date", "Completion Time"));
                logger.error("Date and Time should always be in pairs: " + "Work Item completion date = " + task.getTaskCompletionDate() + " and Work Item completion time = " + task.getTaskCompletionTime() + " are not in pair.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new DateAndTimePairFailedException("Completion Date", "Completion Time");
            }

            // added new validations for exp dates and actual dates in task 3281
            if ((task.getTaskExpStartDate() != null && task.getTaskExpEndDate() != null)) {
                if (task.getTaskExpStartDate().isAfter(task.getTaskExpEndDate()) || task.getTaskExpStartDate().isEqual(task.getTaskExpEndDate())) {
                    throw new ValidationFailedException("Work Item expected start date time cannot be greater than or equal to expected end date time");
                }
            }

            if ((task.getTaskActStDate() != null && task.getTaskActEndDate() != null)) {
                if (task.getTaskActStDate().isAfter(task.getTaskActEndDate()) || task.getTaskActStDate().isEqual(task.getTaskActEndDate())) {
                    throw new ValidationFailedException("Work Item actual start date time cannot be greater than or equal to  expected end date time");
                }
            }

        } else {
            isTaskValidatedForDateAndTimePairs = false;
        }
        return isTaskValidatedForDateAndTimePairs;
    }

    /**
     * This method is used to update the taskActStDate, taskActStTime, taskExpStartDate and taskExpStartTime if it is not provided.
     * These fields will only be updated, if the workflowTaskStatus of the task is getting updated to status "started".
     *
     * @param updatedTask  task object after update.
     * @param taskToUpdate task object before update.
     * @return boolean values.
     */
    public boolean validateExpActStartDateTimeForWorkflowStatusStarted(Task updatedTask, Task taskToUpdate) {
        boolean isWorkflowTaskStatusValidated = true;
        WorkFlowTaskStatus updatedWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());

        if (updatedWorkflowTaskStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED)) {
            LocalDateTime currentDate = DateTimeUtils.getLocalCurrentDate();
            java.time.LocalTime currentTime = DateTimeUtils.getLocalCurrentTime();
            if (updatedTask.getTaskActStDate() == null && updatedTask.getTaskActStTime() == null) {
                updatedTask.setTaskActStDate(currentDate);
                updatedTask.setTaskActStTime(currentTime);
            } else {
                if (!(updatedTask.getTaskActStDate() != null && updatedTask.getTaskActStTime() != null)) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Actual Start Date", "Actual Start Time"));
                    logger.error("Date and Time should always be in pairs: " + "Work Item actual start date = " + updatedTask.getTaskActStDate() + " and Work Item actual start time = " + updatedTask.getTaskActStTime() + " are not in pair.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new DateAndTimePairFailedException("Actual Start Date", "Actual Start Time");
                }
            }
            if (updatedTask.getTaskExpStartDate() == null && updatedTask.getTaskExpStartTime() == null) {
                updatedTask.setTaskExpStartDate(currentDate);
                updatedTask.setTaskExpStartTime(currentTime);
            } else {
                if (!(updatedTask.getTaskExpStartDate() != null && updatedTask.getTaskExpStartTime() != null)) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new DateAndTimePairFailedException("Actual Start Date", "Actual Start Time"));
                    logger.error("Date and Time should always be in pairs: " + "Work Item expected start date = " + updatedTask.getTaskExpStartDate() + " and Work Item expected start time = " + updatedTask.getTaskExpStartTime() + " are not in pair.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new DateAndTimePairFailedException("Expected Start Date", "Expected Start Time");
                }
            }
        }
        return isWorkflowTaskStatusValidated;
    }

    /**
     * This method will validate the task by its workflow status. i.e. if the workflow status of the task is STARTED then it will
     * validate the task for all the validations that are required or applicable for the STARTED task.
     * Similarly, for other status also. Ex. If the workflow status of the task is started then it will call the method which validates
     * the task with all the validations that are applicable on the started task.
     *
     * @param taskToUpdate the task object before update.
     * @param updatedTask  the task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatus(Task taskToUpdate, Task updatedTask, String headerAccountIds) {
        boolean isTaskValidatedByWorkflowStatus = true;
        if (taskToUpdate != null && updatedTask != null) {
            String workflowTaskStatus = updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();

            //Removing values needed when task is blocked from task when workflow status is changed
            if (Objects.equals(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) && !Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                updatedTask.setNextReminderDateTime(null);
                updatedTask.setBlockedReasonTypeId(null);
                updatedTask.setReminderInterval(null);
                updatedTask.setBlockedReason(null);
                updatedTask.setFkAccountIdBlockedBy(null);
            }
            switch (workflowTaskStatus) {
                case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
                    boolean isTaskValidatedForWorkflowBacklog = validateTaskByWorkflowStatusBacklog(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowBacklog) {
                        isTaskValidatedByWorkflowStatus = false;
                    }
                    break;

                case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
                    boolean isTaskValidatedForWorkflowNotStarted = validateTaskByWorkflowStatusNotStarted(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowNotStarted) {
                        isTaskValidatedByWorkflowStatus = false;
                    }
                    break;

                case Constants.WorkFlowTaskStatusConstants.STATUS_STARTED:
                    boolean isTaskValidatedForWorkflowStarted = validateTaskByWorkflowStatusStarted(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowStarted) {
                        isTaskValidatedByWorkflowStatus = false;
                    }
                    break;

                case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED:
                    boolean isTaskValidatedForWorkflowCompleted = validateTaskByWorkflowStatusCompleted(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowCompleted) {
                        isTaskValidatedByWorkflowStatus = false;
                    }
                    break;

                case Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED:
                    boolean isTaskValidatedForWorkflowBlocked = validateTaskByWorkflowStatusBlocked(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowBlocked) {
                        isTaskValidatedByWorkflowStatus = false;
                    } else {
                        if (headerAccountIds != null && !headerAccountIds.isEmpty()) {
                            List<Long> blockedByAccountId = Arrays.stream(headerAccountIds.split(","))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .map(Long::valueOf)
                                    .collect(Collectors.toList());
                            List<UserAccount> blockedByAccounts = userAccountRepository.findAllById(blockedByAccountId);
                            updatedTask.setFkAccountIdBlockedBy(blockedByAccounts.get(0));
                        } else {
                            updatedTask.setFkAccountIdBlockedBy(null);
                        }
                    }
                    break;

                case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD:
                    boolean isTaskValidatedForWorkflowOnHold = validateTaskByWorkflowStatusOnHold(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowOnHold) {
                        isTaskValidatedByWorkflowStatus = false;
                    }
                    break;

                case Constants.WorkFlowTaskStatusConstants.STATUS_DELETE:
                    boolean isTaskValidatedForWorkflowDelete = validateTaskByWorkflowStatusDelete(taskToUpdate, updatedTask);
                    if (!isTaskValidatedForWorkflowDelete) {
                        isTaskValidatedByWorkflowStatus = false;
                    }
                    break;

                default:
                    isTaskValidatedByWorkflowStatus = false;
                    break;
            }
        }
        return isTaskValidatedByWorkflowStatus;
    }

    /**
     * This method will validate the task for its workflow status. i.e. if the workflow status of the task is dependent on some other fields
     * (other than workflowTaskStatus field) of the task. i.e. because of validations on some other fields of the task, the workflow status of
     * the task might be changed then such kind of validations will be done here. Ex. if task actual start date and task actual start time
     * are present then the workflow status of the task has to be STARTED for the first update. Here, workflow task status of the task is
     * dependent on the task actual start date and task actual start time.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskForWorkflowStatus(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = true;
        boolean isTaskActualStartDateTimeValidated = checkActStartDateTimeToStartTask(taskToUpdate, updatedTask);
        if (!isTaskActualStartDateTimeValidated) {
            isTaskValidated = false;
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the tasks which are completed. The method will validate the task whose workflow
     * status is COMPLETED with all the validations that are required or applicable to the tasks which are completed.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusCompleted(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED.toLowerCase())) {
                checkAndUpdateTaskByWorkflowTaskStatus(updatedTask);
                boolean isExpEndDateTimeValidated = validateTaskExpEndDateTime(updatedTask);
                boolean isDefaultPriorityChecked = isDefaultPriorityAssignedToCompletedTask(taskToUpdate, updatedTask);
                boolean isTaskEstimateValidated = validateTaskEstimateByWorkflowTaskStatus(taskToUpdate, updatedTask);
                boolean isTaskAssigned = isTaskAssigned(updatedTask);
                boolean isTaskExpActStartDateTimeValidated = validateExpActStartDateTimeForCompletedTask(taskToUpdate, updatedTask);
                if (isExpEndDateTimeValidated && isDefaultPriorityChecked && isTaskAssigned && isTaskExpActStartDateTimeValidated && isTaskEstimateValidated) {
                    isTaskValidated = true;
                }
                if (updatedTask.getUserPerceivedPercentageTaskCompleted() != null && updatedTask.getUserPerceivedPercentageTaskCompleted() != 100) {
                    throw new ValidationFailedException("Percentage Work Item completed should be 100 for a completed Work Item");
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to check the priority of only completed tasks.
     * i.e. this method will assign the default priority(P3) to the completed tasks,
     * if there is no priority to the completed tasks.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean isDefaultPriorityAssignedToCompletedTask(Task taskToUpdate, Task updatedTask) {
        boolean isDefaultPriorityAssigned = true;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED.toLowerCase())) {
                if (updatedTask.getTaskPriority() == null) {
                    updatedTask.setTaskPriority(Constants.PRIORITY_P3);
                }
            } else {
                isDefaultPriorityAssigned = false;
            }
        } else {
            isDefaultPriorityAssigned = false;
        }
        return isDefaultPriorityAssigned;
    }

    /**
     * This method is used to validate only the backlog tasks. i.e. this method will validate the backlog tasks with all the
     * validations that are applicable on the backlog tasks by calling the respective validations methods.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusBacklog(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidatedForWorkflowBacklog = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase())) {
                boolean isActStartDateTimeValidated = validateTaskActStartDateTimeForWorkflowStatusBacklog(updatedTask);
                boolean isPreviousStatusValidated = validatePreviousWorkflowStatusForBacklogTask(taskToUpdate, updatedTask);
                boolean isPriorityValidated = validatePriorityInBacklogTask(updatedTask);
                if (isActStartDateTimeValidated && isPreviousStatusValidated && isPriorityValidated) {
                    isTaskValidatedForWorkflowBacklog = true;
                }
            }
        }
        return isTaskValidatedForWorkflowBacklog;
    }

    /**
     * This method is used to validate only the blocked tasks. i.e. this method will validate the blocked tasks with all the
     * validations that are applicable on the blocked tasks by calling the respective validations methods.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusBlocked(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidatedForWorkflowBlocked = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED.toLowerCase())) {
                if (updatedTask.getBlockedReasonTypeId() == null) {
                    throw new ValidationException("Please provide the proper reason type to block a Work Item");
                }
                if(updatedTask.getBlockedReason()==null)
                {
                    throw new ValidationFailedException("Please Provide Proper Reason to block work Item");
                }
                if (Objects.equals(updatedTask.getBlockedReasonTypeId(), Constants.BlockedType.INTERNAL_TO_ORG)) {
                    if (updatedTask.getFkAccountIdRespondent() == null) {
                        throw new ValidationException("Please provide organization member information to block this Work Item");
                    }
                    UserAccount user = updatedTask.getFkAccountIdRespondent();
                    List<EmailFirstLastAccountIdIsActive> users =
                            userAccountService.excludeTeamMemberFromOrg(taskToUpdate.getFkTeamId().getTeamId());
                    boolean isValidUser = users.stream()
                            .anyMatch(u -> u.getAccountId().equals(user.getAccountId()) && Boolean.TRUE.equals(u.getIsActive()));
                    if (!isValidUser) {
                        throw new ValidationFailedException("For block this work item user is either not active in the organization or already part of the team.");
                    }
                    if (updatedTask.getReminderInterval() != null) {
                        updatedTask.setNextReminderDateTime(LocalDateTime.now().plusDays(updatedTask.getReminderInterval()));
                    }
                } else if (Objects.equals(updatedTask.getBlockedReasonTypeId(), Constants.BlockedType.INTERNAL_TEAM_MEMBER)) {
                    if (updatedTask.getFkAccountIdRespondent() == null) {
                        throw new ValidationException("Please provide team member information to block this Work Item");
                    }
                    UserAccount user = updatedTask.getFkAccountIdRespondent();
                    List<AccessDomain> activeTeamMembers = accessDomainRepository
                            .findByEntityTypeIdAndEntityIdAndIsActive(
                                    Constants.EntityTypes.TEAM,
                                    taskToUpdate.getFkTeamId().getTeamId(),
                                    true
                            );
                    boolean isUserActiveInTeam = activeTeamMembers.stream()
                            .anyMatch(member -> member.getAccountId().equals(user.getAccountId()));
                    if (!isUserActiveInTeam) {
                        throw new ValidationFailedException("For block this work item user is not an active member of this team.");
                    }
                    if (updatedTask.getReminderInterval() != null) {
                        updatedTask.setNextReminderDateTime(LocalDateTime.now().plusDays(updatedTask.getReminderInterval()));
                    }
                } else if (Objects.equals(updatedTask.getBlockedReasonTypeId(), Constants.BlockedType.OTHER_REASON_ID)) {
                    if (updatedTask.getReminderInterval() != null) {
                        updatedTask.setNextReminderDateTime(LocalDateTime.now().plusDays(updatedTask.getReminderInterval()));
                    }
                } else if (Objects.equals(updatedTask.getBlockedReasonTypeId(), Constants.BlockedType.EXTERNAL_SOURCE)) {
                    if (updatedTask.getReminderInterval() != null) {
                        updatedTask.setNextReminderDateTime(LocalDateTime.now().plusDays(updatedTask.getReminderInterval()));
                    }
                } else {
                    throw new ValidationException("Please provide proper reason before marking this Work Item as blocked");
                }

                boolean isPriorityValidated = validatePriorityForBlockedTask(updatedTask);
                boolean isExpEndDateTimeValidated = validateTaskExpEndDateTime(updatedTask);
//                boolean isActStDateTimeValidated = validateTaskActStartDateTimeForWorkflowStatusBlocked(updatedTask); && isActStDateTimeValidated
                boolean isPreviousWorkflowStatusValidated = validatePreviousWorkflowStatusForBlockedTask(taskToUpdate, updatedTask);
                if (isPriorityValidated && isExpEndDateTimeValidated && isPreviousWorkflowStatusValidated) {
                    isTaskValidatedForWorkflowBlocked = true;
                }
            }
        }
        return isTaskValidatedForWorkflowBlocked;
    }

    /**
     * This method is used to validate only the tasks which are on hold with all the validations. i.e. this method will validate
     * the on hold tasks with all the validations that are applicable on the tasks which are on hold by calling the respective validations methods.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusOnHold(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidatedForWorkflowOnHold = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD.toLowerCase())) {
                if (updatedTask.getTaskActStDate() == null) {
                    throw new ValidationFailedException("Actual start date time can't be null for On-Hold status");
                }
                boolean isPriorityValidated = validatePriorityForOnHoldTask(updatedTask);
                boolean isExpEndDateTimeValidated = validateTaskExpEndDateTime(updatedTask);
                boolean isPreviousWorkflowStatusValidated = validatePreviousWorkflowStatusForOnHoldTask(taskToUpdate, updatedTask);
                if (isPriorityValidated && isExpEndDateTimeValidated && isPreviousWorkflowStatusValidated) {
                    isTaskValidatedForWorkflowOnHold = true;
                }
            }
        }
        return isTaskValidatedForWorkflowOnHold;
    }

    /**
     * This method id used to validate only delete task. i.e. this method will validate delete task with all the
     * validations that are applicable on delete task by calling the respective validations methods.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusDelete(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidatedForWorkflowDelete = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE.toLowerCase())) {
                boolean isExpEndDateTimeValidated = validateTaskExpEndDateTime(updatedTask);
                boolean isActStartDateTimeValidated = validateTaskActStartDateTimeForWorkflowStatusDelete(updatedTask);
                if (isExpEndDateTimeValidated && isActStartDateTimeValidated) {
                    isTaskValidatedForWorkflowDelete = true;
                }
            }
        }
        return isTaskValidatedForWorkflowDelete;
    }

    /**
     * This method is used to validate the task which has started. The method will validate the task whose workflow status is
     * STARTED with all the validations that are required or applicable to the task which are started. Ex. if the workflow status
     * of the task is started then it will validate the task with all the validations that are required on started tasks by
     * calling the respective methods.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusStarted(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase())) {
                boolean isTaskEstimateValidated = validateTaskEstimateByWorkflowTaskStatus(taskToUpdate, updatedTask);
                boolean isPriorityValidated = validatePriorityForStartedTask(updatedTask);
                boolean isExpActStartDateTimeValidated = validateExpActStartDateTimeForWorkflowStatusStarted(updatedTask, taskToUpdate);
                boolean isTaskActualStartDateTimeValidated = validateActualStarDateTimeForStartedTask(taskToUpdate, updatedTask);
                boolean isTaskAssigned = isTaskAssigned(updatedTask);
                boolean isExpEndDateTimeValidated = validateExpEndDateTimeForStartedTask(taskToUpdate, updatedTask);
                if (isExpActStartDateTimeValidated && isPriorityValidated && isTaskEstimateValidated && isTaskActualStartDateTimeValidated && isTaskAssigned && isExpEndDateTimeValidated) {
                    isTaskValidated = true;
                }
                if (updatedTask.getTaskActEndDate() != null || updatedTask.getTaskActEndTime() != null) {
                    return false;
                }
                if (updatedTask.getUserPerceivedPercentageTaskCompleted() != null && updatedTask.getUserPerceivedPercentageTaskCompleted() == 100) {
                    throw new ValidationFailedException("Percentage Work Item completed cannot be 100 for a started Work Item");
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate only the started task. If the task is started then its task expected end date and
     * task expected end time are mandatory.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateExpEndDateTimeForStartedTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase())) {
                if (updatedTask.getTaskExpEndDate() != null && updatedTask.getTaskExpEndTime() != null) {
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the task expected end date and task expected end time.
     * i.e. task expected end date and expected end time should be present.
     *
     * @param task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskExpEndDateTime(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
                isTaskValidated = true;
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the previous workflow status of only blocked task.
     * i.e. if the updated task or the current task workflow status is blocked then this current
     * task should not have been updated from the backlog task. i.e. a backlog task is not allowed
     * to get updated to blocked task.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validatePreviousWorkflowStatusForBlockedTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskPreviousStatusValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED.toLowerCase())) {
                ArrayList<String> previousStatusNotAllowedList = new ArrayList<>();
                previousStatusNotAllowedList.add(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
                if (!(previousStatusNotAllowedList.contains(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()))) {
                    isTaskPreviousStatusValidated = true;
                }
            }
        }
        return isTaskPreviousStatusValidated;
    }

    /**
     * This method will validate the task actual start date and task actual start time of a blocked task. i.e. the
     * task actual start date and time are mandatory for blocked tasks. If they are not present then return false, else, if they are
     * present then check the task expected start date and task expected start time. If they are not present then
     * put the task actual start date as task expected start date and task actual start time as expected start time.
     *
     * @param task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED.toLowerCase())) {
                if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
                    if (!(task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null)) {
                        task.setTaskExpStartDate(task.getTaskActStDate());
                        task.setTaskExpStartTime(task.getTaskActStTime());
                    }
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method will validate the task actual start date and task actual start time of on hold task. i.e. the
     * task actual start date and time are mandatory for on hold tasks. If they are not present then return false, else, if they are
     * present then check the task expected start date and task expected start time. If they are not present then
     * put the task actual start date as task expected start date and task actual start time as expected start time.
     *
     * @param task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskActStartDateTimeForWorkflowStatusOnHold(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD.toLowerCase())) {
                if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
                    if (!(task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null)) {
                        task.setTaskExpStartDate(task.getTaskActStDate());
                        task.setTaskExpStartTime(task.getTaskActStTime());
                    }
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the previous workflow status of only on hold task.
     * i.e. if the updated task or the current task workflow status is on hold then this current
     * task should not have been updated from the backlog task. i.e. a backlog task is not allowed
     * to get updated to on hold task.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validatePreviousWorkflowStatusForOnHoldTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskPreviousStatusValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD.toLowerCase())) {
                ArrayList<String> previousStatusNotAllowedList = new ArrayList<>();
                previousStatusNotAllowedList.add(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
                previousStatusNotAllowedList.add(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE.toLowerCase());
                if (!(Objects.equals(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()))) {
                    if (!(previousStatusNotAllowedList.contains(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()))) {
                        isTaskPreviousStatusValidated = true;
                    }
                } else {
                    isTaskPreviousStatusValidated = true;
                }
            }
        }
        return isTaskPreviousStatusValidated;
    }

    /**
     * This method is used to validate the task actual start date and task actual start time of a backlog tasks.
     * i.e. the task actual start date and task actual start time should not be present for the backlog tasks.
     *
     * @param task task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskActStartDateTimeForWorkflowStatusBacklog(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase())) {
                if (task.getTaskActStDate() == null && task.getTaskActStTime() == null) {
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the previous workflow status of only backlog task.
     * i.e. if the updated task or the current task workflow status is backlog then this current
     * task should not have been updated from the not started task. i.e. a not started task is not allowed
     * to get updated to backlog task.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validatePreviousWorkflowStatusForBacklogTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskPreviousStatusValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase())) {
                ArrayList<String> previousStatusNotAllowedList = new ArrayList<>();
                previousStatusNotAllowedList.add(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
                if (!(Objects.equals(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()))) {
                    if (!(previousStatusNotAllowedList.contains(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()))) {
                        isTaskPreviousStatusValidated = true;
                    }
                } else {
                    isTaskPreviousStatusValidated = true;
                }
            }
        }
        return isTaskPreviousStatusValidated;
    }

    /**
     * This method is used to validate the task actual start date and task actual start time of not started tasks.
     * i.e. the task actual start date and task actual start time should not be present for the not started tasks.
     *
     * @param task task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskActStartDateTimeForWorkflowStatusNotStarted(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase())) {
                if (task.getTaskActStDate() == null && task.getTaskActStTime() == null) {
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the task actual start date and task actual start time of deleted tasks.
     * i.e. the task actual start date and task actual start time should not be present for the deleted tasks.
     *
     * @param task task object which has to be validated.
     * @return boolean values.
     */
    public boolean validateTaskActStartDateTimeForWorkflowStatusDelete(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE.toLowerCase())) {
                if (task.getTaskActStDate() == null && task.getTaskActStTime() == null) {
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }


    /**
     * This method will validate only the not started tasks. i.e. if the workflow status of the task is not started then it will
     * validate the task for all the validations that are applicable on the not started tasks by calling the corresponding methods.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateTaskByWorkflowStatusNotStarted(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase())) {
                boolean isTaskEstimateValidated = validateTaskEstimateByWorkflowTaskStatus(taskToUpdate, updatedTask);
                boolean isDateTimePriorityEstimateValidated = validateDateTimePriorityEstimateForNotStartedTask(taskToUpdate, updatedTask);
                boolean isPreviousWorkflowStatusValidated = validatePreviousWorkflowStatusOfNotStartedTask(taskToUpdate, updatedTask);
                boolean isTaskActStartDateTimeValidated = validateTaskActStartDateTimeForWorkflowStatusNotStarted(updatedTask);
                if (isDateTimePriorityEstimateValidated && isPreviousWorkflowStatusValidated && isTaskActStartDateTimeValidated && isTaskEstimateValidated) {
                    isTaskValidated = true;
                }
            }
        }
        return isTaskValidated;
    }

    /**
     * This method will validate only the not started tasks. i.e. if the task present workflow status is not started then its previous
     * workflow status should not be started. i.e. the task which is started cannot be changed to not started again.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validatePreviousWorkflowStatusOfNotStartedTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        boolean isWorkflowStatusValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase())) {
                if (!Objects.equals(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase())) {
                    isWorkflowStatusValidated = true;
                }
            }
        }
        if (isWorkflowStatusValidated) {
            isTaskValidated = true;
        }
        return isTaskValidated;
    }

    /**
     * This method is used to check whether the given task is assigned or not.
     *
     * @param task object which has to be checked.
     * @return boolean values.
     */
    public boolean isTaskAssigned(Task task) {
        boolean isTaskAssigned = false;
        if (task != null) {
            if (task.getFkAccountIdAssigned() != null) {
                if (task.getFkAccountIdAssigned().getAccountId() != null) {
                    isTaskAssigned = true;
                }
            }
        }
        return isTaskAssigned;
    }

    /**
     * This method will validate the task expected start date, task expected start time, task actual start date and task actual start time
     * for the completed task. If the workflow status of the task is getting updated to COMPLETED from workflow status BACKLOG, NOT STARTED
     * and task expected start date time, task actual start date time are null then this method will throw an exception for task actual start date time.
     * Else if task expected start date time are present and task actual start date time are not present then it will throw an exception for
     * task actual start date time but in case of vice-versa, this method will put the given actual start date time as its expected start date time.
     * Else no changes in start date time, if both start date time are given respectively.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateExpActStartDateTimeForCompletedTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = true;
        if (taskToUpdate != null && updatedTask != null) {
            WorkFlowTaskStatus workflowTaskStatusToUpdate = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            WorkFlowTaskStatus updatedWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
            ArrayList<String> workflowStatusList = new ArrayList<>();
            workflowStatusList.add(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
            workflowStatusList.add(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());

            if (Objects.equals(updatedWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED.toLowerCase())) {
                if (workflowStatusList.contains(workflowTaskStatusToUpdate.getWorkflowTaskStatus().toLowerCase())) {
                    if (updatedTask.getTaskExpStartDate() != null && updatedTask.getTaskExpStartTime() != null) {
                        if (!(updatedTask.getTaskActStDate() != null && updatedTask.getTaskActStTime() != null)) {
                            throw new ValidationFailedException("Completed Work Item Validation Failed: Work Item actual start date or Work Item actual start time cannot be null");
                        }
                    } else {
                        if (!(updatedTask.getTaskActStDate() != null && updatedTask.getTaskActStTime() != null)) {
                            throw new ValidationFailedException("Completed Work Item Validation Failed: Work Item actual start date or Work Item actual start time cannot be null");
                        } else {
                            updatedTask.setTaskExpStartDate(updatedTask.getTaskActStDate());
                            updatedTask.setTaskExpStartTime(updatedTask.getTaskActStTime());
                        }
                    }
                }
            }
        } else {
            isTaskValidated = false;
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the task actual start date and task actual start time for the task which has started. This method
     * will either change the workflow status of the task to STARTED which has actual start date and actual start time, or it will put the
     * current date as actual start date and current time as actual start time (if not present) for the workflow status STARTED.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean validateActualStarDateTimeForStartedTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            WorkFlowTaskStatus updatedWorkflowTaskStatus = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());

            if (updatedTask.getTaskActStDate() != null && updatedTask.getTaskActStTime() != null) {
                if (!Objects.equals(updatedWorkflowTaskStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase())) {
                    Integer taskWorkflowId = updatedTask.getTaskWorkflowId();
                    List<WorkFlowTaskStatus> allWorkflowTaskStatusFound = workflowTaskStatusService.getAllWorkflowStatusByWorkflowTypeId(taskWorkflowId);
                    for (WorkFlowTaskStatus workflowStatus : allWorkflowTaskStatusFound) {
                        if (Objects.equals(workflowStatus.getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase())) {
                            updatedTask.setFkWorkflowTaskStatus(workflowStatus);
                            isTaskValidated = true;
                            break;
                        }
                    }
                } else {
                    isTaskValidated = true;
                }
            } else {
                if (Objects.equals(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase())) {
                    LocalDateTime currentDate = DateTimeUtils.getLocalCurrentDate();
                    java.time.LocalTime currentTime = DateTimeUtils.getLocalCurrentTime();
                    updatedTask.setTaskActStDate(currentDate);
                    updatedTask.setTaskActStTime(currentTime);
                }
                isTaskValidated = true;
            }
        }
        return isTaskValidated;
    }

    /**
     * This method will only validate the task actual start date and task actual start time of a task to
     * be started. i.e. if the task actual start date and task actual start time are present then the
     * task should not be Backlog/Not Started.
     *
     * @param taskToUpdate task object before update.
     * @param updatedTask  task object after update.
     * @return boolean values.
     */
    public boolean checkActStartDateTimeToStartTask(Task taskToUpdate, Task updatedTask) {
        boolean isTaskValidated = false;
        if (taskToUpdate != null && updatedTask != null) {
            if (updatedTask.getTaskActStDate() != null && updatedTask.getTaskActStTime() != null) {
                ArrayList<String> workflowStatusShouldNotBe = new ArrayList<>();
                workflowStatusShouldNotBe.add(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
                workflowStatusShouldNotBe.add(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
                if (!(workflowStatusShouldNotBe.contains(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase()))) {
                    isTaskValidated = true;
                }
                else {
                    throw new ValidationFailedException("In Backlog or Not-Started status Actual start date time should be null");
                }
            } else {
                isTaskValidated = true;
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the priority of the task which has started. The priority of the task is
     * mandatory for the tasks which are started. Hence, it will log and throw the validationFailedException.
     *
     * @param task object whose is started.
     * @return boolean values.
     */
    public boolean validatePriorityForStartedTask(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (task.getTaskPriority() != null) {
                isTaskValidated = true;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Task Priority is mandatory for STARTED Work Item."));
                logger.error("Work Item Priority is mandatory for STARTED Work Item.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Work Item Priority is mandatory for STARTED Work Item.");
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the priority of the task which is blocked. The priority of the task is
     * mandatory for the tasks which are blocked. Hence, it will log and throw the validationFailedException.
     *
     * @param task object whose is blocked.
     * @return boolean values.
     */
    public boolean validatePriorityForBlockedTask(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (task.getTaskPriority() != null) {
                isTaskValidated = true;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Task Priority is mandatory for BLOCKED Work Item."));
                logger.error("Work Item Priority is mandatory for BLOCKED Work Item.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Work Item Priority is mandatory for BLOCKED Work Item.");
            }
        }
        return isTaskValidated;
    }

    /**
     * This method is used to validate the priority of the task which is on hold. The priority of the task is
     * mandatory for the tasks which are on hold. Hence, it will log and throw the validationFailedException.
     *
     * @param task object whose is on hold.
     * @return boolean values.
     */
    public boolean validatePriorityForOnHoldTask(Task task) {
        boolean isTaskValidated = false;
        if (task != null) {
            if (task.getTaskPriority() != null) {
                isTaskValidated = true;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Work Item Priority is mandatory for ON HOLD Work Item."));
                logger.error("Work Item Priority is mandatory for ON HOLD Work Item.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Work Item Priority is mandatory for ON HOLD Work Item.");
            }
        }
        return isTaskValidated;
    }

    //    consider the first two word of task priority as its priority
    public void modifyTaskPriority(Task task) {
        if (task.getTaskPriority() != null) {
            String taskPriority = task.getTaskPriority();
            String modifiedTaskPriority = taskPriority.substring(0, 2);
            task.setTaskPriority(modifiedTaskPriority);
//            boolean isPriorityValidated = validatePriorityP4(task);
        }
    }

    public boolean validatePriorityP4(Task task) {
        boolean isPriorityValidated = true;
        if (task != null) {
            if (task.getTaskPriority() != null) {
                if (Objects.equals(task.getTaskPriority().toLowerCase(), Constants.PRIORITY_P4.toLowerCase())) {
                    throw new ValidationFailedException("Priority : " + task.getTaskPriority() + " is not allowed");
                }
            }
        } else {
            isPriorityValidated = false;
        }
        return isPriorityValidated;
    }

    //    set the Current Activity Indicator to No
    public void setDefaultCurrentActivityIndicator(Task task) {
        if (task != null) {
            if (task.getCurrentActivityIndicator() != null && task.getCurrentActivityIndicator() != 0) {
                task.setCurrentActivityIndicator(0);
            }
        }
    }

    /*this method will convert all the local date and local time of a task into the corresponding date and time according to the
    server timeZone. E.g. If TaskExpEndDate and taskExpEndTime is in IST and server is in Germany then the taskExpEndDate
    and taskExpEndTime will be converted into Germany timezone.*/
    @Transactional(readOnly = true)
    public void convertTaskAllUserDateAndTimeInToServerTimeZone(Task task, String localTimeZone) {
        if (task != null) {
            if (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskExpStartDate(), localTimeZone);
                task.setTaskExpStartDate(convertedDate);
                task.setTaskExpStartTime(convertedDate.toLocalTime());
            }
            if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskExpEndDate(), localTimeZone);
                task.setTaskExpEndDate(convertedDate);
                task.setTaskExpEndTime(convertedDate.toLocalTime());
            }
            if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskActStDate(), localTimeZone);
                task.setTaskActStDate(convertedDate);
                task.setTaskActStTime(convertedDate.toLocalTime());
            }
            if (task.getTaskActEndDate() != null && task.getTaskActEndTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskActEndDate(), localTimeZone);
                task.setTaskActEndDate(convertedDate);
                task.setTaskActEndTime(convertedDate.toLocalTime());

//                -------------------------------------
                if (DebugConfig.getInstance().isDebug()) {
                    System.out.println("converted date to server timezone from local timezone  - task actual end date = " + convertedDate);
                    System.out.println("taskActualEndDate in Work Item = " + task.getTaskActEndDate());
                    System.out.println("taskActualEndTime in Work Item = " + task.getTaskActEndTime());
                }
//                -----------------------------------------------

            }
            if (task.getTaskCompletionDate() != null && task.getTaskCompletionTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskCompletionDate(), localTimeZone);
                task.setTaskCompletionDate(convertedDate);
                task.setTaskCompletionTime(convertedDate.toLocalTime());

            }

            if (task.getCreatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getCreatedDateTime(), localTimeZone);
                task.setCreatedDateTime(convertedDate);


            }

            if (task.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getLastUpdatedDateTime(), localTimeZone);
                task.setLastUpdatedDateTime(convertedDate);

            }
            if (task.getSystemDerivedEndTs() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getSystemDerivedEndTs(), localTimeZone);
                task.setSystemDerivedEndTs(convertedDate);
            }

            if (task.getNotes() != null) {
                convertNotesUserDateAndTimeToServerTimeZone(task, localTimeZone);
            }

            if (task.getListOfDeliverablesDelivered() != null) {
                convertDeliverablesUserDateAndTimeToServerTimeZone(task, localTimeZone);
            }
        }
    }

    private void convertNotesUserDateAndTimeToServerTimeZone(Task task, String localTimeZone) {
        for (int note = 0; note < task.getNotes().size(); note++) {
            if (task.getNotes().get(note).getCreatedDateTime() != null) {
                LocalDateTime convertedCreatedDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getNotes().get(note).getCreatedDateTime(), localTimeZone);
                task.getNotes().get(note).setCreatedDateTime(convertedCreatedDateTime);
            }
            if (task.getNotes().get(note).getLastUpdatedDateTime() != null) {
                LocalDateTime convertedLastUpdatedDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getNotes().get(note).getLastUpdatedDateTime(), localTimeZone);
                task.getNotes().get(note).setLastUpdatedDateTime(convertedLastUpdatedDateTime);
            }
        }
    }

    private void convertDeliverablesUserDateAndTimeToServerTimeZone(Task task, String localTimeZone) {
        for (int deliverable = 0; deliverable < task.getListOfDeliverablesDelivered().size(); deliverable++) {
            if (task.getListOfDeliverablesDelivered().get(deliverable).getCreatedDateTime() != null) {
                LocalDateTime convertedCreatedDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getListOfDeliverablesDelivered().get(deliverable).getCreatedDateTime(), localTimeZone);
                task.getListOfDeliverablesDelivered().get(deliverable).setCreatedDateTime(convertedCreatedDateTime);
            }
            if (task.getListOfDeliverablesDelivered().get(deliverable).getLastUpdatedDateTime() != null) {
                LocalDateTime convertedLastUpdatedDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getListOfDeliverablesDelivered().get(deliverable).getLastUpdatedDateTime(), localTimeZone);
                task.getListOfDeliverablesDelivered().get(deliverable).setLastUpdatedDateTime(convertedLastUpdatedDateTime);
            }
        }
    }

    /*this method will convert all the server date and server time of a task into the corresponding date and time according to the
    local timeZone. E.g. If TaskExpEndDate and taskExpEndTime is in Germany timeZone(server) and local timeZone is in IST then the taskExpEndDate
    and taskExpEndTime will be converted into IST timezone.*/
    @Transactional(readOnly = true)
    public void convertTaskAllServerDateAndTimeInToUserTimeZone(Task task, String localTimeZone) {
        if (task != null) {
            if (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpStartDate(), localTimeZone);
                task.setTaskExpStartDate(convertedDate);
                task.setTaskExpStartTime(convertedDate.toLocalTime());
            }
            if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(), localTimeZone);
                task.setTaskExpEndDate(convertedDate);
                task.setTaskExpEndTime(convertedDate.toLocalTime());
            }
            if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActStDate(), localTimeZone);
                task.setTaskActStDate(convertedDate);
                task.setTaskActStTime(convertedDate.toLocalTime());
            }
            if (task.getTaskActEndDate() != null && task.getTaskActEndTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActEndDate(), localTimeZone);
                task.setTaskActEndDate(convertedDate);
                task.setTaskActEndTime(convertedDate.toLocalTime());

//                -------------------------------------
                if (DebugConfig.getInstance().isDebug()) {
                    System.out.println("converted date to local timezone from server timezone  - Work Item actual end date = " + convertedDate);
                    System.out.println("taskActualEndDate in Work Item = " + task.getTaskActEndDate());
                    System.out.println("taskActualEndTime in Work Item = " + task.getTaskActEndTime());
                }
//                -----------------------------------------------

            }
            if (task.getTaskCompletionDate() != null && task.getTaskCompletionTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskCompletionDate(), localTimeZone);
                task.setTaskCompletionDate(convertedDate);
                task.setTaskCompletionTime(convertedDate.toLocalTime());
            }

            if (task.getCreatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getCreatedDateTime(), localTimeZone);
                task.setCreatedDateTime(convertedDate);
                //--------------------------------------
                if (DebugConfig.getInstance().isDebug()) {
                    System.out.println("Converted creation date to local dateTime" + convertedDate);
                    System.out.println("actual date time in Work Item" + task.getCreatedDateTime());
                }
                //---------------------------------------
            }

            if (task.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getLastUpdatedDateTime(), localTimeZone);
                task.setLastUpdatedDateTime(convertedDate);
                //            ----------------------------------------------------
                if (DebugConfig.getInstance().isDebug()) {
                    System.out.println("Converted creation date to local dateTime" + convertedDate);
                    System.out.println("actual date time in Work Item" + task.getLastUpdatedDateTime());
                }
                //           -------------------------------------------------------
            }

            if (task.getSystemDerivedEndTs() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getSystemDerivedEndTs(), localTimeZone);
                task.setSystemDerivedEndTs(convertedDate);
            }

            if (task.getNotes() != null) {
                List<Note> notes = task.getNotes().stream().map(note -> {
                    Note noteCopy = new Note();
                    BeanUtils.copyProperties(note, noteCopy);
                    return noteCopy;
                }).collect(Collectors.toList());
                List<Note> updatedNotes = convertNotesServerDateAndTimeToUserTimeZone(notes, localTimeZone);
                task.setNotes(updatedNotes);
            }

            if(task.getListOfDeliverablesDelivered() != null) {
                List<DeliverablesDelivered> deliverables = task.getListOfDeliverablesDelivered().stream().map(d -> {
                    DeliverablesDelivered deliver = new DeliverablesDelivered();
                    BeanUtils.copyProperties(d, deliver);
                    return deliver;
                }).sorted(Comparator.comparing(DeliverablesDelivered::getCreatedDateTime)).collect(Collectors.toList());
                List<DeliverablesDelivered> deliverablesList = convertDeliverablesServerDateAndTimeToUserTimeZone(deliverables, localTimeZone);
                task.setListOfDeliverablesDelivered(deliverablesList);
            }
            if (task.getFkAccountIdRespondent() != null && task.getFkAccountIdRespondent().getAccountId() != null) {
                UserAccount user = userAccountRepository.findByAccountId(task.getFkAccountIdRespondent().getAccountId());
                task.setFkAccountIdRespondent(user);
            }

        }
    }

    private List<Note> convertNotesServerDateAndTimeToUserTimeZone(List<Note> notes, String localTimeZone) {
        for (Note note : notes) {
            if (note.getCreatedDateTime() != null) {
                LocalDateTime convertedCreatedDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(note.getCreatedDateTime(), localTimeZone);
                note.setCreatedDateTime(convertedCreatedDateTime);
            }
            if (note.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedLastUpdatedDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(note.getLastUpdatedDateTime(), localTimeZone);
                note.setLastUpdatedDateTime(convertedLastUpdatedDateTime);
            }
        }
        return notes;
    }

    private List<DeliverablesDelivered> convertDeliverablesServerDateAndTimeToUserTimeZone(List<DeliverablesDelivered> deliverablesList, String localTimeZone) {
        for (DeliverablesDelivered deliverables : deliverablesList) {
            if (deliverables.getCreatedDateTime() != null) {
                LocalDateTime convertedCreatedDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(deliverables.getCreatedDateTime(), localTimeZone);
                deliverables.setCreatedDateTime(convertedCreatedDateTime);
            }
            if (deliverables.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedLastUpdatedDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(deliverables.getLastUpdatedDateTime(), localTimeZone);
                deliverables.setLastUpdatedDateTime(convertedLastUpdatedDateTime);
            }
        }
        return deliverablesList;
    }

    public Integer updateTaskAttachmentsByTaskId(String attachments, Long taskId) {
        return taskRepository.setTaskAttachmentsByTaskId(attachments, taskId);
    }

    ;

    /* This method will update the estimateTimeLogEvaluation and taskCompletionImpact fields of the task. These two fields of the task will only be updated, if
     * the workflowTaskStatus of the task is COMPLETED. i.e. task is COMPLETED. */
    public void updateCompletionImpactAndEstimateTimeLogEval(Task task) {
        if (task != null && task.getRecordedEffort() != null && task.getTaskEstimate() != null) {
            if (task.getRecordedEffort() < 0 || task.getTaskEstimate() < 0) {
                throw new ValidationFailedException("Recorded Effort & Estimate cannot be less than 0");
            } else {
                task.setEstimateTimeLogEvaluation("PERFECT");
                task.setTaskCompletionImpact("NONE");
                int timeLog_Estimate_Diff = task.getRecordedEffort() - task.getTaskEstimate();
                int timeLog_Estimate_Diff_Fraction = timeLog_Estimate_Diff / task.getTaskEstimate();
                if (task.getTaskEstimate() > 0 && task.getTaskEstimate() < 120) {    /* 120 is in minutes*/
                    if (timeLog_Estimate_Diff > 0) {
                        if (timeLog_Estimate_Diff_Fraction > 2) {
                            task.setEstimateTimeLogEvaluation("HORRIFIC");
                            task.setTaskCompletionImpact("EXTREMELY HIGH");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.8) {  // greater than 0.8 and less than equal to 2
                            task.setEstimateTimeLogEvaluation("Very BAD");
                            task.setTaskCompletionImpact("HIGH");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.5) { // greater than 0.5 and less than equal to 0.8
                            task.setEstimateTimeLogEvaluation("BAD");
                            task.setTaskCompletionImpact("MEDIUM");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.2 && timeLog_Estimate_Diff_Fraction <= 0.5) {
                            task.setEstimateTimeLogEvaluation("SLIGHTLY BAD");
                            task.setTaskCompletionImpact("LOW");
                        }
                    } else { // if timeLog_Estimate_Diff <= 0
                        if (timeLog_Estimate_Diff_Fraction < -0.1 && timeLog_Estimate_Diff_Fraction >= -0.25) {
                            task.setEstimateTimeLogEvaluation("NICE PERFORMANCE");
                            task.setTaskCompletionImpact("SLIGHTLY POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.25 && timeLog_Estimate_Diff_Fraction >= -0.4) {
                            task.setEstimateTimeLogEvaluation("GOOD PERFORMANCE");
                            task.setTaskCompletionImpact("POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.4 && timeLog_Estimate_Diff_Fraction >= -0.65) {
                            task.setEstimateTimeLogEvaluation("HIGH ESTIMATE OR GREAT PERFORMANCE");
                            task.setTaskCompletionImpact("HIGHLY POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.65) {
                            task.setEstimateTimeLogEvaluation("HIGH ESTIMATE");
                            task.setTaskCompletionImpact("EXTREMELY POSITIVE");
                        }
                    }
                } else if (task.getTaskEstimate() >= 120 && task.getTaskEstimate() < 300) {
                    if (timeLog_Estimate_Diff > 0) {
                        if (timeLog_Estimate_Diff_Fraction > 1.25) {
                            task.setEstimateTimeLogEvaluation("HORRIFIC");
                            task.setTaskCompletionImpact("EXTREMELY HIGH");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.7 && timeLog_Estimate_Diff_Fraction <= 1.25) {
                            task.setEstimateTimeLogEvaluation("VERY BAD");
                            task.setTaskCompletionImpact("HIGH");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.5 && timeLog_Estimate_Diff_Fraction <= 0.7) {
                            task.setEstimateTimeLogEvaluation("BAD");
                            task.setTaskCompletionImpact("MEDIUM");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.2 && timeLog_Estimate_Diff_Fraction <= 0.5) {
                            task.setEstimateTimeLogEvaluation("SLIGHTLY BAD");
                            task.setTaskCompletionImpact("LOW");
                        }
                    } else {  //timeLog_Estimate_Diff <= 0
                        if (timeLog_Estimate_Diff_Fraction < -0.08 && timeLog_Estimate_Diff_Fraction >= -0.18) {
                            task.setEstimateTimeLogEvaluation("NICE PERFORMANCE");
                            task.setTaskCompletionImpact("SLIGHTLY POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.18 && timeLog_Estimate_Diff_Fraction >= -0.3) {
                            task.setEstimateTimeLogEvaluation("GOOD PERFORMANCE");
                            task.setTaskCompletionImpact("POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.3 && timeLog_Estimate_Diff_Fraction >= -0.55) {
                            task.setEstimateTimeLogEvaluation("HIGH ESTIMATE OR GREAT PERFORMANCE");
                            task.setTaskCompletionImpact("HIGHLY POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.55) {
                            task.setEstimateTimeLogEvaluation("HIGH ESTIMATE");
                            task.setTaskCompletionImpact("EXTREMELY POSITIVE");
                        }
                    }
                } else { // task estimate > 300
                    if (timeLog_Estimate_Diff > 0) {
                        if (timeLog_Estimate_Diff_Fraction > 0.9) {
                            task.setEstimateTimeLogEvaluation("HORRIFIC");
                            task.setTaskCompletionImpact("EXTREMELY HIGH");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.6 && timeLog_Estimate_Diff_Fraction <= 0.9) {
                            task.setEstimateTimeLogEvaluation("Very BAD");
                            task.setTaskCompletionImpact("HIGH");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.4 && timeLog_Estimate_Diff_Fraction <= 0.6) {
                            task.setEstimateTimeLogEvaluation("BAD");
                            task.setTaskCompletionImpact("MEDIUM");
                        } else if (timeLog_Estimate_Diff_Fraction > 0.1 && timeLog_Estimate_Diff_Fraction <= 0.4) {
                            task.setEstimateTimeLogEvaluation("SLIGHTLY BAD");
                            task.setTaskCompletionImpact("LOW");
                        }
                    } else { // timeLog_Estimate_Diff <= 0
                        if (timeLog_Estimate_Diff_Fraction < -0.05 && timeLog_Estimate_Diff_Fraction >= -0.15) {
                            task.setEstimateTimeLogEvaluation("NICE PERFORMANCE");
                            task.setTaskCompletionImpact("SLIGHTLY POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.15 && timeLog_Estimate_Diff_Fraction >= -0.25) {
                            task.setEstimateTimeLogEvaluation("GOOD PERFORMANCE");
                            task.setTaskCompletionImpact("POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.25 && timeLog_Estimate_Diff_Fraction >= -0.5) {
                            task.setEstimateTimeLogEvaluation("HIGH ESTIMATE OR GREAT PERFORMANCE");
                            task.setTaskCompletionImpact("HIGHLY POSITIVE");
                        } else if (timeLog_Estimate_Diff_Fraction < -0.5) {
                            task.setEstimateTimeLogEvaluation("HIGH ESTIMATE");
                            task.setTaskCompletionImpact("EXTREMELY POSITIVE");
                        }
                    }
                }

                // calculations based on priority
                if (task.getTaskPriority() != null) {
                    if (Objects.equals(TaskPriority.valueOf(task.getTaskPriority()), TaskPriority.P0) || Objects.equals(TaskPriority.valueOf(task.getTaskPriority()), TaskPriority.P1)) {
                        String completionImpact = task.getTaskCompletionImpact();
                        switch (completionImpact.toLowerCase()) {
                            case "high":
                                task.setTaskCompletionImpact("EXTREMELY HIGH");
                                break;
                            case "medium":
                                task.setTaskCompletionImpact("HIGH");
                                break;
                            case "low":
                                task.setTaskCompletionImpact("MEDIUM");
                                break;
                            case "highly positive":
                                task.setTaskCompletionImpact("EXTREMELY POSITIVE");
                                break;
                            case "positive":
                                task.setTaskCompletionImpact("HIGHLY POSITIVE");
                                break;
                            case "slightly positive":
                                task.setTaskCompletionImpact("POSITIVE");
                                break;
                            default:
                                if (timeLog_Estimate_Diff_Fraction > -0.02 && timeLog_Estimate_Diff_Fraction < 0) {
                                    task.setEstimateTimeLogEvaluation("NICE");
                                } else if (timeLog_Estimate_Diff_Fraction >= 0 && timeLog_Estimate_Diff_Fraction < 0.06) {
                                    task.setEstimateTimeLogEvaluation("LOW");
                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * This function is used to set the reference entities in time tracking by getting that reference task fields from task table from database.
     */
//    public void setReferenceEntitiesByTask(Task task, TimeSheet timeSheet) {
//        timeSheet.setReferenceEntityId(task.getReferenceEntityId());
//
//        if (task.getReferenceEntityId() != null) {
//            Task taskFromDB = taskRepository.findByTaskId(task.getReferenceEntityId());
//            timeSheet.setReferenceEntityNum(taskFromDB.getTaskNumber());
//            timeSheet.setReferenceTaskTypeId(taskFromDB.getTaskTypeId());
//            timeSheet.setReferenceEntityTitle(taskFromDB.getTaskTitle());
//            timeSheet.setReferenceEntityTypeId(Constants.EntityTypes.TASK);
//        }
//
//    }

    /**
     * This method updates recorded effort field (task) with the sum of new efforts provided and save each new effort in TimeSheet table
     */
    public void updateTimeSheetAndRecordedEffort(Task task, String localTimeZone) {
        User user = null;
        TimeSheet timeSheet = null;
        UserAccount userAccount = null;
        Integer sum = 0;
        List<NewEffortTrack> newEffortTracks = task.getNewEffortTracks();
        String taskWorkflowStatus = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
        String taskPrevWorkflowStatus = taskRepository.findByTaskId(task.getTaskId()).getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
        List<String> workflowStatusNotAllowed = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED, Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE, Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD);
        DataEncryptionConverter dataEncryptionConverter = new DataEncryptionConverter();
        List<RoleId> roleIdsOfUserUpdatingTask = accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(task.getFkAccountIdLastUpdated().getAccountId(), task.getFkTeamId().getTeamId(), true);
        boolean isUserHigherRoleInTeam = false;

        // if no new efforts are provided, or the task is a parent task with child tasks - we won't update anything and just return
        if (newEffortTracks == null || newEffortTracks.isEmpty()) {
            return;
        }

        if (task.getSprintId() != null) {
            Optional<Sprint> sprintDb = sprintRepository.findById(task.getSprintId());
            if (sprintDb.isPresent() && !Objects.equals(sprintDb.get().getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                throw new ValidationFailedException("Effort addition is restricted for tasks in non-started sprints. Please ensure the sprint is in the 'STARTED' state before adding efforts to the task.");
            }
        }

        if (task.getFkEpicId() != null && task.getFkEpicId().getEpicId() != null) {
            Epic epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
            if (epic != null && epic.getFkWorkflowEpicStatus() != null && !Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_IN_PROGRESS.getWorkflowEpicStatusId())) {
                throw new ValidationFailedException("You can only add effort to a work item if its associated epic is in the In-Progress state.");
            }
        }

        // validation: new effort can't be recorded for parent task
        if (task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {
            throw new ValidationFailedException("new effort can't be recorded for a task with child task/s");
        }

        // validation: check that the user adding the recorded effort is either the higher role user in the team or the account to which task is assigned
        if (!Objects.equals(task.getFkAccountIdAssigned().getAccountId(), task.getFkAccountIdLastUpdated().getAccountId())) {

            for (RoleId roleId : roleIdsOfUserUpdatingTask) {
                Integer role = roleId.getRoleId();
                if (Constants.HIGHER_ROLE_IDS.contains(role)) {
                    isUserHigherRoleInTeam = true;
                    break;
                }
            }

            if (!isUserHigherRoleInTeam) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("error"));
                logger.error("user not authorized to add recorded effort to this Work Item", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("user not authorized to add recorded effort to this Work Item");
            }
        }

        // validation: new effort can't be recorded for task with BACKLOG, NOT STARTED, ON HOLD, DELETED, BLOCKED workflow status
        if (workflowStatusNotAllowed.contains(taskWorkflowStatus)) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("error"));
            logger.error("new effort can not be recorded for Work Item with BACKLOG, NOT STARTED, DELETED, BLOCKED, ON HOLD workflow status", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("new effort can not be recorded for Work Item with BACKLOG, NOT STARTED, DELETED, BLOCKED, ON HOLD  workflow status");
        }

        // validation: new effort can't be recorded for already completed task
        if (taskPrevWorkflowStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("error"));
            logger.error("new effort can not be recorded for already completed Work Item", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("new effort can not be recorded for already completed Work Item");
        }

        // validation: task estimate can't be null if workflowTaskStatus is valid and newEffort is recorded
        if (task.getTaskEstimate() == null || task.getTaskEstimate() == 0) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateTimeSheetAndRecordedEffort"));
            logger.error("task estimate can not be null if workTaskStatus is valid and newEffort is provided", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("Work Item estimate can not be null");
        }

        // validation: recorded effort field should be null when the task is updated for the first time
        if (task.getVersion() == 0L && task.getRecordedEffort() != null) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateTimeSheetAndRecordedEffort"));
            logger.error("recordedEffort field should be null when Work Item is updated for the first time", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("recordedEffort field should be null when Work Item is updated for the first time");
        }

        // validation: accountIdAssigned can not be null if new Effort is added
        if (task.getFkAccountIdAssigned() == null) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateTimeSheetAndRecordedEffort"));
            logger.error("account Id assigned can not be null if new effort is added", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("account Id assigned can not be null if new effort is added");
        }

        Optional<UserAccount> userAccountOptional = Optional.ofNullable(userAccountService.getActiveUserAccountByAccountId(task.getFkAccountIdAssigned().getAccountId()));

        if (userAccountOptional.isPresent()) {
            userAccount = userAccountOptional.get();
            user = userAccount.getFkUserId();
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("user account not found", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        }

        List<TimeSheet> tsRecords = new ArrayList<>();
        for (NewEffortTrack newEffortTrack : newEffortTracks) {

            // validation: newEffort and newEffortDate parameters both should be passed if a new effort is recorded
            if (newEffortTrack.getNewEffort() == null || newEffortTrack.getNewEffortDate() == null) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateTimeSheetAndRecordedEffort"));
                logger.error("either new effort or new effort date is null", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("new effort or new effort date can not be null");
            }

            // validation: newEffortDate should be less or equal to today's date
            LocalDate dateFromRequest = newEffortTrack.getNewEffortDate();
            if (dateFromRequest != null) {
                LocalDateTime clientDate = DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now(), localTimeZone);
                if (dateFromRequest.isAfter(clientDate.toLocalDate())) {

                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateTimeSheetAndRecordedEffort"));
                    logger.error("new effort date can not be greater than today's date ", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ValidationFailedException("new effort date can not be greater than today's date");
                }

                // validation: newEffortDate can't be prior to Task actual start date and later than Task actual end date
                LocalDateTime clientActEndDate = null, clientActStDate = null;
                if (task.getTaskActEndDate() != null) {
                    clientActEndDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActEndDate(), localTimeZone);
                }
                if (task.getTaskActStDate() != null)
                    clientActStDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActStDate(), localTimeZone);
                if ((clientActStDate != null && dateFromRequest.isBefore(clientActStDate.toLocalDate())) || (clientActEndDate != null && dateFromRequest.isAfter(clientActEndDate.toLocalDate()))) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateTimeSheetAndRecordedEffort"));
                    logger.error("new effort date can not be prior to Work Item actual start date or later than Work Item actual end date", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ValidationFailedException("new effort date can not be prior to Work Item actual start date or later than Work Item actual end date");
                }
                if(effortsWithin24(task.getFkAccountIdAssigned().getAccountId(), newEffortTrack)>0) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Effort cannot be more than 24 hours for a single day"));
                        logger.error("Effort cannot be more than 24 hours for a single day ", new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Effort cannot be more than 24 hours for a single day");
                }
            }

            timeSheet = new TimeSheet();
            timeSheet.setNewEffort(newEffortTrack.getNewEffort());
            timeSheet.setNewEffortDate(newEffortTrack.getNewEffortDate());
            timeSheet.setEntityId(task.getTaskId());
            timeSheet.setEntityNumber(task.getTaskNumber());
            timeSheet.setEntityTitle(dataEncryptionConverter.convertToDatabaseColumn(task.getTaskTitle()));
            timeSheet.setEntityTypeId(Constants.EntityTypes.TASK);
            timeSheet.setSprintId(task.getSprintId());
            if (task.getFkEpicId() != null && task.getFkEpicId().getEpicId() != null) {
                timeSheet.setEpicId(task.getFkEpicId().getEpicId());
            }

            //------- CHANGES -> TASK - 2364 ----------------------------------
            timeSheet.setTaskTypeId(task.getTaskTypeId());
//            setReferenceEntitiesByTask(task, timeSheet);
            // ----------------------------------------------------------------
            timeSheet.setBuId(task.getBuId());
            timeSheet.setProjectId(task.getFkProjectId().getProjectId());
            timeSheet.setTeamId(task.getFkTeamId().getTeamId());
            timeSheet.setAccountId(userAccount.getAccountId());
            timeSheet.setOrgId(userAccount.getOrgId());
            timeSheet.setUserId(user.getUserId());

            sum += newEffortTrack.getNewEffort();
            tsRecords.add(timeSheet);
        }

        if (task.getIncreaseInUserPerceivedPercentageTaskCompleted() == null) {
            throw new IllegalStateException("Unable to add efforts: User-perceived percentage completion increase is missing.");
        }
        Integer oldEarnedTimeValue = task.getEarnedTimeTask() != null ? task.getEarnedTimeTask() : 0;
        Integer totalEffort = task.getRecordedEffort() == null ? sum : task.getRecordedEffort() + sum;

        LocalDateTime endDateTime = task.getTaskActEndDate() == null ? LocalDateTime.now() : task.getTaskActEndDate();
        if(task.getTaskActStDate() != null && Duration.between(task.getTaskActStDate(), endDateTime).toMinutes() < totalEffort){
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Effort time is greater than Actual Time frame."));
            logger.error("validation: totalEffortTime(previousEffortTime + New Effort time) <= (Actual end Time - Actual start time)", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("Total logged effort can't exceed the time difference between the actual start date-time and the current time or actual end date-time.");
        }

        // this helper method updates earnedTime in the list of timesheets before we save it and also updates the earnedTimeTask in the Task
        updateEarnedTimeAndUpdateTaskAndTimeSheet(task, tsRecords, sum);
        timeSheetService.saveAllTimeSheetRecords(tsRecords);

        task.setRecordedEffort(totalEffort);
        task.setRecordedTaskEffort(task.getRecordedTaskEffort() == null ? sum : task.getRecordedTaskEffort() + sum);
        task.setTotalEffort(task.getTotalEffort() == null ? sum : task.getTotalEffort() + sum);
    }

    /**
     * This method calculates earned time & increase in user perceived percentage task completed of the user based on increase in userPerceivedPercentageTaskCompleted, Task estimate
     */
    public void updateEarnedTimeAndUpdateTaskAndTimeSheet(Task task, List<TimeSheet> tsRecords, Integer sumOfNewEfforts) {

        // assumption: taskEstimate is uneditable once added in a task (not finalized)
        int newEarnedTime = 0;
        Integer currentTaskEstimateValue = task.getTaskEstimate();
        Integer currentEarnedTimeTask = task.getEarnedTimeTask();
        Integer increaseInTaskCompletion = task.getIncreaseInUserPerceivedPercentageTaskCompleted();
        TaskHistory lastTask = null;
        Integer lastTaskUserPerceivedPercentageCompleted = null;
        Integer currentTaskUserPerceivedPercentageCompleted = task.getUserPerceivedPercentageTaskCompleted();

        Optional<TaskHistory> lastUpdatedTask = Optional.ofNullable(taskHistoryRepository.findByTaskIdAndVersion(task.getTaskId(), task.getVersion()));

        if (lastUpdatedTask.isPresent()) {
            lastTask = lastUpdatedTask.get();
            currentEarnedTimeTask = lastTask.getEarnedTimeTask();
            lastTaskUserPerceivedPercentageCompleted = lastTask.getUserPerceivedPercentageTaskCompleted();
        }

        if (currentEarnedTimeTask == null || increaseInTaskCompletion == null || lastTaskUserPerceivedPercentageCompleted == null || currentTaskUserPerceivedPercentageCompleted == null) {

            if (currentEarnedTimeTask == null) currentEarnedTimeTask = 0;
            if (increaseInTaskCompletion == null) increaseInTaskCompletion = 0;
            if (lastTaskUserPerceivedPercentageCompleted == null) lastTaskUserPerceivedPercentageCompleted = 0;
            if (currentTaskUserPerceivedPercentageCompleted == null) currentTaskUserPerceivedPercentageCompleted = 0;
        }

        // validation: task estimate can not be null
        if (currentTaskEstimateValue == null || currentTaskEstimateValue == 0) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateEarnedTimeAndUpdateTaskAndTimeSheet"));
            logger.error("Work Item estimate can not be null if workTaskStatus is valid and newEffort is provided", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("Work Item estimate can not be null/ zero");
        }

        // validation: user perceived percentage task completed can't be less than previously updated
        if (lastTask != null && currentTaskUserPerceivedPercentageCompleted < lastTaskUserPerceivedPercentageCompleted) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in updateEarnedTimeAndUpdateTaskAndTimeSheet"));
            logger.error("User perceived percentage Work Item completed can not be less than previously updated", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("User perceived percentage Work Item completed can not be less than previously updated");
        }

        if (task.getUserPerceivedPercentageTaskCompleted() == null) {

            //calculate difference of estimated time and current EarnedTimeTask
            int difference = currentTaskEstimateValue - currentEarnedTimeTask;

            if (difference >= sumOfNewEfforts) {
                task.setEarnedTimeTask(currentEarnedTimeTask + sumOfNewEfforts);
                newEarnedTime = sumOfNewEfforts;
            } else {
                task.setEarnedTimeTask(currentEarnedTimeTask + difference);
                newEarnedTime = difference;
            }
        } else if (task.getUserPerceivedPercentageTaskCompleted() != null) {


            // no increase in User Perceived Percentage then no new earned Time
            if (increaseInTaskCompletion == 0) {
                return;
            }
            if (increaseInTaskCompletion > 100 || increaseInTaskCompletion < 0) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Invalid values of increase in user perceived Work Item completion percentage"));
                logger.error("Invalid values of increase in user perceived Work Item completion percentage", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Invalid values of increase in user perceived Work Item completion percentage");
            }
            newEarnedTime = Math.round((increaseInTaskCompletion.floatValue() * currentTaskEstimateValue.floatValue()) / 100);
            task.setEarnedTimeTask(currentEarnedTimeTask + newEarnedTime);
        }

        Integer totalEarnedTime = 0;
        for (TimeSheet timesheet : tsRecords) {

            int roundedValue = Math.round(((float) newEarnedTime * timesheet.getNewEffort().floatValue()) / sumOfNewEfforts.floatValue());
            timesheet.setEarnedTime(roundedValue);

            if (increaseInTaskCompletion != 0) {
                roundedValue = Math.round((increaseInTaskCompletion.floatValue() * timesheet.getNewEffort().floatValue()) / sumOfNewEfforts.floatValue());
                timesheet.setIncreaseInUserPerceivedPercentageTaskCompleted(roundedValue);
            }
            if (task.getSprintId() != null) {
                capacityService.updateEffortsInSprintCapacity(timesheet, task.getSprintId());

            }
            totalEarnedTime +=  timesheet.getEarnedTime();
        }
        if (task.getSprintId() != null) {
            Sprint sprint = sprintRepository.findBySprintId(task.getSprintId());
            if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                sprint.setEarnedEfforts((sprint.getEarnedEfforts() != null ? sprint.getEarnedEfforts() : 0) + totalEarnedTime);
                sprintRepository.save(sprint);
            }
        }

        if (task.getFkEpicId() != null && task.getFkEpicId().getEpicId() != null) {
            Epic epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
            if (epic != null) {
                epic.setEarnedEfforts((epic.getEarnedEfforts() != null ? epic.getEarnedEfforts() : 0) + totalEarnedTime);
                epic.setLoggedEfforts((epic.getLoggedEfforts() != null ? epic.getLoggedEfforts() : 0) + sumOfNewEfforts);
                epicRepository.save(epic);
            }
        }

    }

    /**
     * This method sets the currentlyScheduledTaskIndicator as true for tasks with priority P0, P1 with
     * workflowTaskStatus other than Completed/ Deleted. Additionally this method sets the indicator as false for
     * completed/ deleted tasks of any priority.
     */
    public void updateCurrentlyScheduledTaskIndicatorForTask(Task task) {
        List<String> validPriorities = List.of(Constants.Priorities.PRIORITY_P0, Constants.Priorities.PRIORITY_P1);
        WorkFlowTaskStatus workflowTaskStatusToUpdate = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());

        String taskWorkFlowStatus = workflowTaskStatusToUpdate.getWorkflowTaskStatus().toLowerCase();
        if (task.getTaskPriority() != null && validPriorities.contains(task.getTaskPriority())) {
            if (Objects.equals(taskWorkFlowStatus, Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) ||
                    Objects.equals(taskWorkFlowStatus, Constants.WorkFlowTaskStatusConstants.STATUS_STARTED))
                task.setCurrentlyScheduledTaskIndicator(true);
        }
        List<String> workFlowsToCheck = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE,
                Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD, Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG);
        if (workFlowsToCheck.contains(taskWorkFlowStatus)) {
            task.setCurrentlyScheduledTaskIndicator(false);
        }
    }

    public void updateImmediateAttentionIndicatorForTask(Task task) {

        if (task.getImmediateAttention() == null) {
            task.setImmediateAttention(0);
        }
        if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(),Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE) && Objects.equals(task.getImmediateAttention(),Constants.BooleanValues.BOOLEAN_TRUE)) {
            throw new ValidationFailedException("Backlogs Work Items cannot have immediate attention");
        }
        WorkFlowTaskStatus workflowTaskStatusToUpdate = workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());
        String taskWorkFlowStatus = workflowTaskStatusToUpdate.getWorkflowTaskStatus().toLowerCase();

        List<String> workFlowsToCheck = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE);
        if (workFlowsToCheck.contains(taskWorkFlowStatus)) {
            task.setImmediateAttention(0);
            task.setImmediateAttentionFrom(null);
            task.setImmediateAttentionReason(null);
        }
    }

//    public void validatePriorityInBacklogTask(Task task) {
//        List<String> invalidPriorities = List.of(Constants.Priorities.PRIORITY_P0, Constants.Priorities.PRIORITY_P1);
//        if (task.getTaskPriority() != null && invalidPriorities.contains(task.getTaskPriority())) {
//            if(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase("Backlog")){
//                throw new ForbiddenException();
//            }
//        }
//    }

    /**
     * For a taskNumber, this method will return the date wise total Effort & total earned time on that task
     *
     * @param taskId
     * @return List<TotalEffortByDate>
     */
    public List<TotalEffortByDate> getTaskRecordedEffort(Long taskId) {
        List<TimeSheet> timeSheets = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TASK, taskId);

        Map<LocalDate, TotalEffortByDate> mapDateToTotalEffort = new TreeMap<>();

        for (TimeSheet timeSheet : timeSheets) {
            LocalDate newEffortDate = timeSheet.getNewEffortDate();
            Integer newEffort = timeSheet.getNewEffort();
            Integer earnedTime = timeSheet.getEarnedTime();
            if (earnedTime == null) {
                earnedTime = 0;
            }

            if (mapDateToTotalEffort.containsKey(newEffortDate)) {
                TotalEffortByDate totalEffortByDate = mapDateToTotalEffort.get(newEffortDate);

                int currentEffortTotal = totalEffortByDate.getTotalEffortMins() == null ? 0 : mapDateToTotalEffort.get(newEffortDate).getTotalEffortMins();
                int currentEarnedTime = totalEffortByDate.getTotalEarnedTime() == null ? 0 : mapDateToTotalEffort.get(newEffortDate).getTotalEarnedTime();
                totalEffortByDate.setTotalEffortMins(currentEffortTotal + newEffort);
                totalEffortByDate.setTotalEarnedTime(currentEarnedTime + earnedTime);
            } else {
                mapDateToTotalEffort.put(newEffortDate, new TotalEffortByDate(newEffort, earnedTime, newEffortDate));
            }
        }

        return new ArrayList<>(mapDateToTotalEffort.values());
    }

    public HashMap<String, List<TotalEffortByDate>> getRecordedEffortByTaskNumberAndTotalEffort(Long taskId) {

        Task task = taskRepository.findByTaskId(taskId);
        HashMap<String, List<TotalEffortByDate>> mapTaskNumberToTotalEffort = new HashMap<>();
        if (task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {
            List<Long> childTaskIds = task.getChildTaskIds();
            List<Task> allChildTasks = taskRepository.findByParentTaskId(task.getTaskId());
            for (Task childTask : allChildTasks) {
                mapTaskNumberToTotalEffort.put(childTask.getTaskNumber(), getTaskRecordedEffort(childTask.getTaskId()));
            }
        } else {
            mapTaskNumberToTotalEffort.put(task.getTaskNumber(), getTaskRecordedEffort(task.getTaskId()));
        }
        return mapTaskNumberToTotalEffort;
    }

    /**
     * @Function: Populating Task model from quickCreateTaskRequest
     */
    public Task populateTask(QuickCreateTaskRequest quickCreateTaskRequest, String accountIds) {
        Task task = new Task();
        task.setTaskTypeId(Constants.TaskTypes.TASK);
        task.setTaskTitle(quickCreateTaskRequest.getTaskTitle());
        //if desc is null or empty populate title as desc
        task.setTaskDesc((quickCreateTaskRequest.getTaskDesc() != null && !quickCreateTaskRequest.getTaskDesc().isEmpty()) ? quickCreateTaskRequest.getTaskDesc() : quickCreateTaskRequest.getTaskTitle());
        if (quickCreateTaskRequest.getTaskPriority() != null)
            task.setTaskPriority(quickCreateTaskRequest.getTaskPriority().substring(0, 2));
        task.setTaskWorkflowId(quickCreateTaskRequest.getTaskWorkFlowId());
        //checking for workflow ids
        if (!Constants.DEFAULT_WORKFLOW.contains(task.getTaskWorkflowId())) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new WorkflowTypeDoesNotExistException());
            logger.error("Work Item Workflow type failed: Work Item workflowId = " + task.getTaskWorkflowId() + " is not allowed for a Work Item.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new WorkflowTypeDoesNotExistException();
        }
        task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatusId(quickCreateTaskRequest.getTaskWorkFlowStatus()));
        if (quickCreateTaskRequest.getExpStartDateTime() != null) {
            task.setTaskExpStartDate(quickCreateTaskRequest.getExpStartDateTime());
            if (quickCreateTaskRequest.getExpStartTime() != null)
                task.setTaskExpStartTime(quickCreateTaskRequest.getExpStartDateTime().toLocalTime());
            else task.setTaskExpStartTime(null);
        }
        if (quickCreateTaskRequest.getExpEndDateTime() != null) {
            task.setTaskExpEndDate(quickCreateTaskRequest.getExpEndDateTime());
            if (quickCreateTaskRequest.getExpEndTime() != null)
                task.setTaskExpEndTime(quickCreateTaskRequest.getExpEndDateTime().toLocalTime());
            else task.setTaskExpEndTime(null);
        }
        task.setFkAccountId(userAccountRepository.findByAccountId(Long.valueOf(accountIds)));
        if (quickCreateTaskRequest.getAssignTo() != null) {
            task.setFkAccountIdAssigned(userAccountRepository.findByAccountId(quickCreateTaskRequest.getAssignTo()));
            task.setFkAccountIdAssignee(task.getFkAccountId());
        }
        if (quickCreateTaskRequest.getLabelsToAdd() != null) {
            task.setLabelsToAdd(quickCreateTaskRequest.getLabelsToAdd());
        }
        if(quickCreateTaskRequest.getEstimate()!=null && quickCreateTaskRequest.getEstimate().equals(0))
            task.setTaskEstimate(null);
        else
            task.setTaskEstimate(quickCreateTaskRequest.getEstimate());
        task.setCurrentActivityIndicator(0);
        task.setSprintId(quickCreateTaskRequest.getSprintId());
        task.setFkTeamId(teamRepository.findByTeamId(quickCreateTaskRequest.getTeamId()));
        task.setBuId(task.getFkTeamId().getFkProjectId().getBuId());
        task.setFkProjectId(task.getFkTeamId().getFkProjectId());
        task.setFkOrgId(task.getFkTeamId().getFkOrgId());
        task.setFkAccountIdCreator(task.getFkAccountId());
        task.setFkAccountIdLastUpdated(task.getFkAccountId());
        if (quickCreateTaskRequest.getDependentTaskDetailRequestList() != null) {
            task.setDependentTaskDetailRequestList(quickCreateTaskRequest.getDependentTaskDetailRequestList());
        }
        if(quickCreateTaskRequest.getReferenceWorkItemId() != null && !quickCreateTaskRequest.getReferenceWorkItemId().isEmpty()) {
            task.setReferenceWorkItemId(quickCreateTaskRequest.getReferenceWorkItemId());
        }
        if(quickCreateTaskRequest.getEpicId() != null) {
            task.setFkEpicId(epicRepository.findByEpicId(quickCreateTaskRequest.getEpicId()));
        }
        if (quickCreateTaskRequest.getReleaseVersionName() != null) {
            task.setReleaseVersionName(quickCreateTaskRequest.getReleaseVersionName());
        }
        if (quickCreateTaskRequest.getIsStarred() != null && quickCreateTaskRequest.getIsStarred()) {
            task.setIsStarred(true);
            task.setFkAccountIdStarredBy(task.getFkAccountId());
        }
        return task;
    }

    // ------------------------------ Methods for New Task Types - ChildTask ---------------------------------------------------------//

    /**
     * we can only create normal Task or Bug Task using addTask API. This method verifies that the taskTypeId is not null
     * and task type is of either normal task or bug task and there is no other fields present related to the other task type Ids example childTaskIds, meeting list
     *
     * @param task
     */
    public boolean validateTaskTypeIdForAddTask(Task task) {
        if (task.getTaskTypeId() == null || task.getParentTaskId() != null || task.getParentTaskTypeId() != null || task.getChildTaskList() != null ||
                (task.getTaskTypeId() != Constants.TaskTypes.TASK && task.getTaskTypeId() != Constants.TaskTypes.BUG_TASK) || (task.getMeetingList() != null && !task.getMeetingList().isEmpty())) {
            throw new ValidationFailedException("Validation Error: Work Item type is invalid or Child Task / Meeting list is invalid");
        }
        if(Objects.equals(task.getTaskTypeId(),Constants.TaskTypes.BUG_TASK)){
            validateCustomEnvironmentForBug(task.getFkTeamId().getTeamId(),task.getEnvironmentId());
        }
        return true;
    }

    /**
     * creates a new Task object from the child Task request after validating the parent task details
     *
     * @param childTaskRequest
     * @param accountIds
     */
    public Task createNewChildTask(Task parentTask, ChildTaskRequest childTaskRequest, String accountIds) {

        validateParentTask(parentTask, childTaskRequest);
        return populateChildTaskFromParentTask(parentTask, childTaskRequest, accountIds);
    }

    /**
     * validates parent task properties for creating a new child task
     *
     * @param parentTask
     * @param childTaskRequest
     */
    public void validateParentTask(Task parentTask, ChildTaskRequest childTaskRequest) {

        // task type id validation
        if (parentTask.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK) || (parentTask.getChildTaskIds() != null && !parentTask.getChildTaskIds().isEmpty() && !parentTask.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK))) {
            throw new IllegalArgumentException("Invalid task type id: " + parentTask.getTaskTypeId());
        }
        if (parentTask.getMeetingList() != null && !parentTask.getMeetingList().isEmpty()) {
            throw new ValidationFailedException("Parent task can't have reference meeting Please remove it to create child task .");
        }

//        // estimate validation
//        if (parentTask.getTaskEstimate() == null) {
//            throw new ValidationFailedException("Estimate is mandatory for parent task. Update estimate in Task before creating child task");
//        }

        // workflow status validation
        String parentWorkFlowStatus = parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
        switch (parentWorkFlowStatus) {
            case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
            case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
                verifyChildTasksFields(parentTask, childTaskRequest);
                break;
            case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED:
            case Constants.WorkFlowTaskStatusConstants.STATUS_DELETE:
            case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD:
            case Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED:
                throw new IllegalArgumentException("Cannot create Child Tasks for parent task with status: " + parentWorkFlowStatus);
            case Constants.WorkFlowTaskStatusConstants.STATUS_STARTED:
                List<Long> childTaskIds = parentTask.getChildTaskIds();
                List<TimeSheet> tsRecords = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TASK, parentTask.getTaskId());

                boolean noRecordedEfforts = tsRecords.isEmpty();
                boolean hasSubTasks = childTaskIds != null && !childTaskIds.isEmpty();

                if (!noRecordedEfforts && !hasSubTasks) {
                    throw new IllegalArgumentException("You can't create a Child Task when the parent task has recorded efforts and there are no existing Child Tasks.");
                }

                verifyChildTasksFields(parentTask, childTaskRequest);
                break;
            default:
                throw new IllegalStateException("Unexpected workflow status: " + parentWorkFlowStatus);
        }
    }

    /**
     * validates if we have all the required fields to create a child task based on the workflow status of the parent task
     *
     * @param parentTask
     * @param childTaskRequest
     */
    public void verifyChildTasksFields(Task parentTask, ChildTaskRequest childTaskRequest) {

        String parentWorkFlowTaskStatus = parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();

        if (parentWorkFlowTaskStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
            if (childTaskRequest.getTaskTitle() == null || childTaskRequest.getTaskDesc() == null) {
                throw new ValidationFailedException("Child tasks must have non-null values for Work Item Title, Work Item Description when the parent task is in the 'Backlog' workflow status.");
            }
        } else if (parentWorkFlowTaskStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) || parentWorkFlowTaskStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED)) {
            if (childTaskRequest.getTaskTitle() == null || childTaskRequest.getTaskDesc() == null || childTaskRequest.getTaskExpEndDate() == null
                    || childTaskRequest.getTaskExpStartDate() == null || childTaskRequest.getTaskEstimate() == null
                    || childTaskRequest.getAccountIdAssigned() == null) {
                throw new ValidationFailedException("Child tasks must have non-null values for Work Item Title, Work Item Description, Work Item Expected End Date, Work Item Expected Start Date, Work Item Estimate, and Assigned Account ID when the parent task is in the 'Not Started' or 'Started' workflow status.");
            }
        }

        if (childTaskRequest.getTaskExpEndDate() != null && childTaskRequest.getTaskExpEndDate().isAfter(parentTask.getTaskExpEndDate())) {
            throw new ValidationFailedException("childTask expected end date can not be after the parent task expected end date");
        }

        if (childTaskRequest.getTaskExpStartDate() != null && childTaskRequest.getTaskExpStartDate().isBefore(parentTask.getTaskExpStartDate())) {
            throw new ValidationFailedException("childTask expected start date can not be before the parent task expected start date");
        }
    }

    /**
     * creates child Task object using the given details of the child task and populating the rest of the details from the parent task
     *
     * @param parentTask
     * @param childTaskRequest
     * @param accountIds
     * @return
     */
    public Task populateChildTaskFromParentTask(Task parentTask, ChildTaskRequest childTaskRequest, String accountIds) {
        Task task = new Task();
        Boolean isChildFirst = false;
        String parentTaskWorkflowStatus = parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
        task.setTaskTypeId(Constants.TaskTypes.CHILD_TASK);
        task.setParentTaskId(childTaskRequest.getParentTaskId());
        if (parentTask.getTaskTypeId().equals(Constants.TaskTypes.TASK) || parentTask.getTaskTypeId().equals(Constants.TaskTypes.BUG_TASK)) {
            isChildFirst = true;
        }
//        else if (parentTask.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK) || parentTask.getTaskTypeId().equals(Constants.TaskTypes.BUG_TASK))
//            task.setParentTaskTypeId(parentTask.getTaskTypeId());

//        if (Objects.equals(parentTask.getTaskTypeId(), Constants.TaskTypes.BUG_TASK) || (parentTask.getIsBug() != null && parentTask.getIsBug())) {
//            task.setIsBug(true);
//            task.setEnvironmentId(parentTask.getEnvironmentId());
//            task.setSeverityId(parentTask.getSeverityId());
//            task.setPlaceOfIdentification(parentTask.getPlaceOfIdentification());
//            task.setCustomerImpact(parentTask.getCustomerImpact());
//        }

        task.setParentTaskTypeId(Constants.TaskTypes.PARENT_TASK); // -- need to remove the parent task type id field from the task table
        task.setTaskTitle(childTaskRequest.getTaskTitle());
        task.setTaskDesc(childTaskRequest.getTaskDesc());
        task.setTaskPriority(parentTask.getTaskPriority());
        task.setCurrentActivityIndicator(0);
        task.setTaskWorkflowId(parentTask.getTaskWorkflowId());
        task.setTaskEstimate(childTaskRequest.getTaskEstimate());
        task.setFkAccountIdAssigned(childTaskRequest.getAccountIdAssigned() != null ? userAccountService.getActiveUserAccountByAccountId(childTaskRequest.getAccountIdAssigned()) : parentTask.getFkAccountIdAssigned());
        if (parentTaskWorkflowStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG) || parentTaskWorkflowStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED)) {
            task.setFkWorkflowTaskStatus(parentTask.getFkWorkflowTaskStatus());
        } else if (parentTaskWorkflowStatus.equals(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED)) {
            task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, parentTask.getTaskWorkflowId()));
        }
        task.setFkTeamId(parentTask.getFkTeamId());
        task.setBuId(parentTask.getBuId());
        task.setFkProjectId(parentTask.getFkProjectId());
        task.setFkOrgId(parentTask.getFkOrgId());

        if (childTaskRequest.getTaskExpStartDate() != null) {
            task.setTaskExpStartDate(childTaskRequest.getTaskExpStartDate());
            task.setTaskExpStartTime(childTaskRequest.getTaskExpStartDate().toLocalTime());
        }
        if (childTaskRequest.getTaskExpEndDate() != null) {
            task.setTaskExpEndDate(childTaskRequest.getTaskExpEndDate());
            task.setTaskExpEndTime(childTaskRequest.getTaskExpEndDate().toLocalTime());
        }
        if (childTaskRequest.getDependentTaskDetailRequestList() != null) {
            task.setDependentTaskDetailRequestList(childTaskRequest.getDependentTaskDetailRequestList());
        }
        if(childTaskRequest.getReferenceWorkItemId() != null && !childTaskRequest.getReferenceWorkItemId().isEmpty()) {
            task.setReferenceWorkItemId(childTaskRequest.getReferenceWorkItemId());
        }
        if (childTaskRequest.getReleaseVersionName() != null) {
            task.setReleaseVersionName(childTaskRequest.getReleaseVersionName());
        }
        if (parentTask.getSprintId() != null) {
            List<Long> authorizedAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, parentTask.getFkTeamId().getTeamId(), List.of(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(), RoleEnum.TEAM_MANAGER_SPRINT.getRoleId()), true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
            if (!authorizedAccountIds.contains(Long.valueOf(accountIds))) {
                throw new IllegalStateException("User not authorized to create a Child Task for Work Item in sprint");
            }
            task.setSprintId(parentTask.getSprintId());
            Sprint sprint = sprintRepository.findBySprintId(parentTask.getSprintId());
            if (task.getTaskExpStartDate() == null || task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || !task.getTaskExpStartDate().isBefore(sprint.getSprintExpEndDate())) {
                if (parentTask.getTaskExpStartDate() != null && !parentTask.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate())) {
                    task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                    task.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                } else {
                    task.setTaskExpStartDate(sprint.getSprintExpStartDate());
                    task.setTaskExpStartTime(sprint.getSprintExpStartDate().toLocalTime());
                }
            }
            if (task.getTaskExpEndDate() == null) {
                task.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                task.setTaskExpEndTime(parentTask.getTaskExpEndTime());
            }
            if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                if (childTaskRequest.getAccountIdAssigned() == null) {
                    throw new ValidationFailedException("Cannot create Work Item: User assignment required.");
                }
                if (childTaskRequest.getTaskEstimate() == null) {
                    throw new ValidationFailedException("Cannot create Work Item: Work Item estimate is required.");
                }
                sprint.setHoursOfSprint((sprint.getHoursOfSprint() != null ? sprint.getHoursOfSprint() : 0) + childTaskRequest.getTaskEstimate());
                if (isChildFirst) {
                    sprint.setHoursOfSprint((sprint.getHoursOfSprint() != null ? sprint.getHoursOfSprint() : 0) - parentTask.getTaskEstimate());
                }
            }
            if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new ValidationFailedException("User not allowed to add Work Item in completed sprint");
            }
            if (isChildFirst) {
                capacityService.removeTaskFromSprintCapacityAdjustment(parentTask);
            }

        }

        if (parentTask.getFkEpicId() != null) {
            Epic epic = epicRepository.findByEpicId(parentTask.getFkEpicId().getEpicId());
            if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatus(), Constants.WorkflowEpicStatusConstants.STATUS_COMPLETED)) {
                throw new IllegalStateException("Work Item can't be added in completed epic");
            }
            if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatus(), Constants.WorkflowEpicStatusConstants.STATUS_DELETED)) {
                throw new IllegalStateException("Work Item can't be added in deleted epic");
            }
            List<Long> authorizedAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, parentTask.getFkTeamId().getTeamId(), Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION, true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
            if (!authorizedAccountIds.contains(Long.valueOf(accountIds))) {
                throw new IllegalStateException("User not authorized to create a Child Task for Work Item in epic");
            }
            task.setFkEpicId(parentTask.getFkEpicId());
            if (task.getTaskExpStartDate() == null) {
                task.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                task.setTaskExpStartTime(parentTask.getTaskExpStartTime());
            }
            if (task.getTaskExpEndDate() == null) {
                task.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                task.setTaskExpEndTime(parentTask.getTaskExpEndTime());
            }
        }

        task.setFkAccountId(userAccountRepository.findByAccountId(Long.valueOf(accountIds)));
        task.setFkAccountIdAssignee(task.getFkAccountId());
        task.setFkAccountIdCreator(task.getFkAccountId());
        task.setFkAccountIdLastUpdated(task.getFkAccountId());
        if (childTaskRequest.getIsStarred() != null && childTaskRequest.getIsStarred()) {
            task.setIsStarred(true);
            task.setFkAccountIdStarredBy(task.getFkAccountId());
        }

        if (childTaskRequest.getLabelsToAdd() != null) {
            task.setLabelsToAdd(childTaskRequest.getLabelsToAdd());
        }
        return task;
    }

    /**
     * checks properties of task type and other validations based on the task type id when updating an existing task
     *
     * @param task                     - The actual task object that is getting updated
     * @param useSystemDerivedForChild - indicates whether we want to use system derived values for updating child task's estimate/ expected dates when the child task is in backlog and parent is moved to not started
     */
    public void validateTaskByTaskTypeForUpdateTask(Task task, Boolean useSystemDerivedForChild) throws IllegalArgumentException {

        Task taskFromDb = taskRepository.findByTaskId(task.getTaskId());
        List<Long> childTaskIdsTask = task.getChildTaskIds();
        List<Long> childTaskIdsTaskFromDb = taskFromDb.getChildTaskIds();
        if (!Objects.equals(taskFromDb.getTaskTypeId(), task.getTaskTypeId()) || (childTaskIdsTaskFromDb == null && childTaskIdsTask != null) ||
                (childTaskIdsTaskFromDb != null && childTaskIdsTask == null) || (childTaskIdsTaskFromDb != null && !Objects.equals(childTaskIdsTask, childTaskIdsTaskFromDb))) {
            throw new IllegalArgumentException("Error: Invalid value for taskTypeId or childTaskIds");
        }
        Integer taskTypeId = task.getTaskTypeId();
        Long parentTaskId = task.getParentTaskId();
        Integer parentTaskTypeId = task.getParentTaskTypeId();
        List<Long> childTaskIds = task.getChildTaskIds();

        if (taskTypeId == null) throw new IllegalArgumentException("Task Type Id can not be null");

        if (parentTaskId != null) {
            Task parentTask = taskRepository.findByTaskId(parentTaskId);
        }

        switch (taskTypeId) {
            // validations for standalone task
            case Constants.TaskTypes.TASK:
                if (parentTaskId != null || parentTaskTypeId != null || (childTaskIds != null && !childTaskIds.isEmpty())) {
                    throw new IllegalArgumentException("Invalid Work Item: parent_task_id, parent_task_type_id, and List<ChildTaskIds> should be null for standalone Work Item");
                }
                break;

            // validations for parent task -- front end & back end has to mark the task type id as 1 when a child task is added to a task
            case Constants.TaskTypes.PARENT_TASK:
                List<Task> childTasksFromDb = taskRepository.findByTaskIdIn(childTaskIds);
                //id validations
                if (childTaskIds == null || childTaskIds.isEmpty() || parentTaskId != null || parentTaskTypeId != null) {
                    throw new IllegalArgumentException("Invalid Work Item: List<ChildTaskIds> should not be null/empty and parent_task_id and parent_task_type_id should be null for parent task");
                }
                // workflow validations
                String workflowStatusOfParentTask = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
                List<String> workflowStatusOfParentToCheck = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD, Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED,
                        Constants.WorkFlowTaskStatusConstants.STATUS_DELETE);
                if (workflowStatusOfParentToCheck.contains(workflowStatusOfParentTask)) {
                    for (Task childTaskFromDb : childTasksFromDb) {
                        if (!childTaskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(workflowStatusOfParentTask)) {
                            throw new ValidationFailedException("Cannot mark the parent task as '" + workflowStatusOfParentTask + "' until all its child tasks are marked as '" + workflowStatusOfParentTask + "'");
                        }
                    }
                }

                // Case: Parent Task workflow status is modified to Not Started/ Started
                if (workflowStatusOfParentTask.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) || workflowStatusOfParentTask.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED)) {
                    validateSubTaskForStatusChange(task, task.getChildTaskIds(), useSystemDerivedForChild);
                }

                // new effort validations
                if (task.getNewEffortTracks() != null && !task.getNewEffortTracks().isEmpty()) {
                    throw new ValidationFailedException("Effort cannot be added to a Work Item that has child tasks");
                }

                // estimate validation
                if (task.getTaskEstimate() == null && !Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                    throw new ValidationFailedException("Estimate is mandatory for parent task");
                }

                // bug parent validation
                if ((task.getIsBug() != null && task.getIsBug() && task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) && (task.getResolutionId() == null || task.getStepsTakenToComplete() == null || (task.getStepsTakenToComplete() != null && task.getStepsTakenToComplete().isEmpty()))) {
                    throw new ValidationFailedException("Resolution and Steps Taken To Complete must be provided when a bug Work Item is marked as complete");
                }
                break;

            // validations for updating child task
            case Constants.TaskTypes.CHILD_TASK:
                Task parentTask = taskRepository.findByTaskId(parentTaskId);
                // id validations
                if (parentTaskId == null || parentTaskTypeId == null || parentTaskTypeId == 1 || (childTaskIds != null && !childTaskIds.isEmpty())) {
                    throw new IllegalArgumentException("Invalid Work Item: parent_task_id and parent_task_type_id should not be null and parent_task_type_id can not be 1. List<childTaskIds> should be null for sub task");
                }
                // workflow validations
                String parentTaskWorkflowStatus = parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
//                if (Objects.equals(parentTaskWorkflowStatus, Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG) && !Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
//                    throw new ValidationFailedException("Error: Can't modify the childTask workflow status if the parent task is in Backlog state. Mark the parent task as Not Started/ Started.");
//                }

                // dates validation -- since dates exists in pairs if exp end date is provided then it is assumed that exp start date should also be there
                if ((task.getTaskExpEndDate() != null && parentTask.getTaskExpStartDate() != null)) {
                    if (task.getTaskExpEndDate().isAfter(parentTask.getTaskExpEndDate()) || task.getTaskExpStartDate().isBefore(parentTask.getTaskExpStartDate())) {
                        throw new ValidationFailedException("The child task's Expected End Date & Time cannot be after the parent task's End Date & Time, and the child task's Expected Start Date & Time cannot be before the parent task's Start Date & Time. ");
                    }
                }

                if (task.getTaskActStDate() != null && (parentTask.getFkAccountIdAssigned() == null || parentTask.getTaskPriority() == null)) {
                    String errorMessage = "The following details might be missing in parent task :";
                    if (parentTask.getTaskPriority() == null) {
                        errorMessage += " priority";
                    }
                    if (parentTask.getFkAccountIdAssigned() == null) {
                        if (!errorMessage.equalsIgnoreCase("The following details might be missing in parent task :")) {
                            errorMessage += ",";
                        }
                        errorMessage += " assigned to";
                    }
                    throw new ValidationFailedException(errorMessage + ".");
                }

                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                    //validating parent task fields
                    Sprint sprint = null;
                    if (parentTask.getSprintId() != null) {
                        sprint = sprintRepository.findBySprintId(parentTask.getSprintId());
                    }
                    if (parentTask.getTaskPriority() == null || (task.getTaskActStDate() != null && parentTask.getFkAccountIdAssigned() == null) || (parentTask.getFkAccountIdAssigned() == null && ((sprint != null && Objects.equals(Constants.SprintStatusEnum.STARTED.getSprintStatusId(), sprint.getSprintStatus())) || parentTask.getTaskActStDate() != null))) {
                        String errorMessage = "The following details might be missing in parent task :";
                        if (parentTask.getTaskPriority() == null) {
                            errorMessage += " priority";
                        }
                        if (parentTask.getFkAccountIdAssigned() == null) {
                            if (!errorMessage.equalsIgnoreCase("The following details might be missing in parent task :")) {
                                errorMessage += ",";
                            }
                            errorMessage += " assigned to";
                        }
                        throw new ValidationFailedException(errorMessage + ".");
                    }
                    List<Long> childTaskIdsToBeValidated= parentTask.getChildTaskIds();
                    childTaskIdsToBeValidated = childTaskIdsToBeValidated.stream().filter(taskId -> !taskId.equals(task.getTaskId())).collect(Collectors.toList());
                    if (parentTask.getTaskExpEndDate() != null && parentTask.getTaskExpStartDate() != null) {
                        validateSubTaskForStatusChange(parentTask, childTaskIdsToBeValidated, useSystemDerivedForChild);
                    } else {
                        validateSubTaskForStatusChange(task, childTaskIdsToBeValidated, useSystemDerivedForChild);
                    }
                }

                break;

            case Constants.TaskTypes.BUG_TASK:
                if ((task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) && (task.getResolutionId() == null || task.getStepsTakenToComplete() == null || (task.getStepsTakenToComplete() != null && task.getStepsTakenToComplete().isEmpty()))) {
                    throw new ValidationFailedException("Resolution and Steps Taken to Resolve must be provided when a bug is marked as completed");
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid Task Type Id: " + taskTypeId);
        }
    }

    /**
     * Synchronizes the details of child tasks with the corresponding parent task when the parent task is modifying the workflow status from backlog to not started/ started and any of the child tasks is still in backlog
     * This method updates the start date, end date, priority, and estimate of any child task if that child task is in backlog, and it is missing any of the before-mentioned fields
     *
     * @param parentTask The parent task whose details are to be used for synchronization.
     */
    public void synchronizeChildTaskDetailsWithParent(Task parentTask, String timeZone, Long childTaskId) {
        // check if the parent's workflow is changed to Not Started/ Started
        Task parentTaskFromDb = taskRepository.findByTaskId(parentTask.getTaskId());
        String parentWorkflowStatus = parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
        if (!Objects.equals(parentTaskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), parentWorkflowStatus) && ((parentWorkflowStatus.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) || parentWorkflowStatus.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE))) && !(parentTaskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE))) {
            List<Task> childTasks = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
            if (childTaskId != null) childTasks = childTasks.stream().filter(task -> !task.getTaskId().equals(childTaskId)).collect(Collectors.toList());
            Integer totalChildEstimate = parentTask.getTaskEstimate();
            if (totalChildEstimate != null) {
                long providedEstimatesSum = childTasks.stream()
                        .filter(t -> t.getTaskEstimate() != null)
                        .mapToLong(Task::getTaskEstimate)
                        .sum();
                long remainingEstimate = totalChildEstimate - providedEstimatesSum;
                long countOfTasksWithoutEstimate = childTasks.stream()
                        .filter(t -> t.getTaskEstimate() == null)
                        .count();

                for (Task childTask : childTasks) {
                    Task childTaskCopy = new Task();
                    BeanUtils.copyProperties(childTask, childTaskCopy);
                    List<String> updatedFields = new ArrayList<>();
                    if (childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
                        if (childTask.getTaskExpStartDate() == null) {
                            childTask.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                            childTask.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                            updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                            updatedFields.add(Constants.TaskFields.EXP_START_TIME);
                        }

                        if (childTask.getTaskExpEndDate() == null) {
                            childTask.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                            childTask.setTaskExpEndTime(parentTask.getTaskExpEndTime());
                            updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                            updatedFields.add(Constants.TaskFields.EXP_END_TIME);
                        }

                        if (childTask.getTaskPriority() == null) {
                            childTask.setTaskPriority(parentTask.getTaskPriority());
                            updatedFields.add(Constants.TaskFields.PRIORITY);
                        }

                        if (childTask.getTaskEstimate() == null && countOfTasksWithoutEstimate > 0) {
                            if (remainingEstimate < 2 * Constants.ESTIMATES.get(TaskPriority.valueOf(parentTask.getTaskPriority()))) {
                                long parentTaskPriorityMillisInMinutes = Constants.ESTIMATES.get(TaskPriority.valueOf(parentTask.getTaskPriority())) / (60 * 1000);
                                childTask.setTaskEstimate(Math.toIntExact(parentTaskPriorityMillisInMinutes));
                            } else {
                                childTask.setTaskEstimate((int) (remainingEstimate / countOfTasksWithoutEstimate));
                            }
                            updatedFields.add(Constants.TaskFields.ESTIMATE);
                        }

                        WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, parentTask.getTaskWorkflowId());
                        childTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
                        childTask.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
                        updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                        computeAndUpdateStatForTask(childTask, true);
                        updatedFields.add(Constants.TaskFields.TASK_STATE);

                        // Persist the changes (consider using batch save for performance)
                        taskHistoryService.addTaskHistoryOnSystemUpdate(childTaskCopy);
                        taskRepository.save(childTask);
                        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, childTask);
                    }
                }
            } else {
                throw new ValidationFailedException("Parent task's estimate is missing");
            }
        } else if (!Objects.equals(parentTaskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), parentWorkflowStatus) && (parentTaskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE))) {
            List<Task> childTasks = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
            for (Task childTask : childTasks) {
                if (Objects.equals(childTask.getBlockedReasonTypeId(), Constants.BlockedMessages.PARENT_TASK_BLOCKED_ID) && Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                    Task childTaskCopy = new Task();
                    BeanUtils.copyProperties(childTask, childTaskCopy);
                    List<String> updatedFields = new ArrayList<>();
                    childTask.setBlockedReason(null);
                    childTask.setBlockedReasonTypeId(null);
                    childTask.setNextReminderDateTime(null);
                    childTask.setFkAccountIdRespondent(null);

                    List<WorkFlowTaskStatus> prevWorkflows = taskHistoryRepository.findWorkflowToRevertFromBlockedByTaskNumber(childTask.getTaskId(), Constants.WorkFlowStatusIds.BLOCKED);

                    childTask.setFkWorkflowTaskStatus(prevWorkflows.get(0));
                    childTask.setTaskState(prevWorkflows.get(0).getWorkflowTaskState());
                    updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                    computeAndUpdateStatForTask(childTask, true);
                    updatedFields.add(Constants.TaskFields.TASK_STATE);
                    updatedFields.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM);
                    updatedFields.add(Constants.TaskFields.BLOCKED_REASON);
                    updatedFields.add(Constants.TaskFields.BLOCKED_REASON_TYPE_ID);
                    if (!Objects.equals(childTask.getFkAccountIdRespondent(), childTaskCopy.getFkAccountIdRespondent())) {
                        updatedFields.add(Constants.TaskFields.ACCOUNT_ID_RESPONDENT);
                    }

                    // Persist the changes (consider using batch save for performance)
                    taskHistoryService.addTaskHistoryOnSystemUpdate(childTaskCopy);
                    taskRepository.save(childTask);
                    taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, childTask);
                }

            }
        }
    }


    /**
     * populates the child task response for the list of tasks [retrieved from the database for the childTaskIds of a Parent/ BugTask]
     */
    public List<ChildTask> createChildTaskResponse(List<Task> childTasks, String timeZone) {
        List<ChildTask> childTaskList = new ArrayList<>();
        for (Task task : childTasks) {
            ChildTask childTask = new ChildTask();
            BeanUtils.copyProperties(task, childTask);
            if (task.getFkAccountIdAssigned() != null) {
                childTask.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
            }
            childTask.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            childTask.setTaskExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpStartDate(), timeZone));
            childTask.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(), timeZone));
            childTask.setSystemDerivedEndTs(DateTimeUtils.convertServerDateToUserTimezone(task.getSystemDerivedEndTs(), timeZone));

            if (task.getLabels() != null && !task.getLabels().isEmpty()) {
                List<LabelResponse> labelResponses = new ArrayList<>();
                for (Label label : task.getLabels()) {
                    LabelResponse labelResponse = new LabelResponse();
                    labelResponse.setLabelId(label.getLabelId());
                    labelResponse.setLabelName(label.getLabelName());
                    labelResponses.add(labelResponse);
                }
                childTask.setLabelDetails(labelResponses);
            }

            if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty()) {
                List<Dependency> dependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(task.getDependencyIds(), false);
                List<DependentTaskDetail> dependentTaskDetails = new ArrayList<>();
                for (Dependency dependency : dependencies) {
                    DependentTaskDetail dependencyTaskDetail = new DependentTaskDetail();
                    if (dependency.getPredecessorTaskId().equals(task.getTaskId())) {
                        dependencyTaskDetail.setRelatedTaskNumber(taskRepository.findByTaskId(dependency.getSuccessorTaskId()).getTaskNumber());
                        dependencyTaskDetail.setRelationDirection(RelationDirection.SUCCESSOR);
                        dependencyTaskDetail.setRelationTypeId(dependency.getRelationTypeId());
                    } else if (dependency.getSuccessorTaskId().equals(task.getTaskId())) {
                        dependencyTaskDetail.setRelatedTaskNumber(taskRepository.findByTaskId(dependency.getPredecessorTaskId()).getTaskNumber());
                        dependencyTaskDetail.setRelationDirection(RelationDirection.PREDECESSOR);
                        dependencyTaskDetail.setRelationTypeId(dependency.getRelationTypeId());
                    }

                    dependentTaskDetails.add(dependencyTaskDetail);
                }
                childTask.setDependentTaskDetails(dependentTaskDetails);
            }

            childTaskList.add(childTask);
        }

        return childTaskList;
    }

    /**
     * creates a linked task response for the get api -- populates linked task response for the given list of tasks
     *
     * @param linkedTasks
     * @return
     */
    public List<LinkedTask> createLinkedTaskResponse(List<Task> linkedTasks, String timeZone) {
        List<LinkedTask> linkedTaskResponseList = new ArrayList<>();

        if (linkedTasks != null && !linkedTasks.isEmpty()) {
            for (Task task : linkedTasks) {
                LinkedTask linkedTask = new LinkedTask();
                BeanUtils.copyProperties(task, linkedTask);
                linkedTask.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(), timeZone));
                linkedTask.setTaskExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpStartDate(), timeZone));
                if (task.getFkAccountIdAssigned() != null) {
                    linkedTask.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
                }
                linkedTask.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                linkedTaskResponseList.add(linkedTask);
            }
        }
        return linkedTaskResponseList;
    }

    /**
     * validates the tasks in linked tasks list in a given bugTask and set the required properties
     *
     * @param bugTask
     */
    public void validateAndSaveLinkedTaskInfo(Task bugTask) {
        if (bugTask.getLinkedTaskList() != null && !bugTask.getLinkedTaskList().isEmpty()) {
            List<Long> updatedLinkedTaskIdList = validateBugTaskAndGetUpdatedLinkedTaskIds(bugTask);
            bugTask.setBugTaskRelation(updatedLinkedTaskIdList);
            modifyBugTaskRelationOfLinkedTasks(bugTask);
        }
    }

    /**
     * This method will modify the parent task when a new child task is added. The taskTypeId, childTaskIds are modified and task history is saved.
     *
     * @param childTask
     */
    private void modifyParentTaskForNewChildTask(Task childTask) {
        Task parentTaskDb = taskRepository.findByTaskId(childTask.getParentTaskId());
        Task parentTask = new Task();
        BeanUtils.copyProperties(parentTaskDb, parentTask);
        if (parentTask == null) {
            throw new ValidationFailedException("Invalid Parent Task: The given parent task doesn't exist");
        }
        Task parentTaskCopy = new Task();
        BeanUtils.copyProperties(parentTask, parentTaskCopy);
        List<String> updatedFields = new ArrayList<>();
        // modify estimate of the parent task
        modifyEstimateOfParentTask(childTask, parentTask, updatedFields);

        List<StatType> allChildTaskStats = taskRepository.getStatsOfTaskIdsIn(parentTask.getChildTaskIds());
        // first child is getting created
        List<Long> childTaskIds;
        if (parentTask.getChildTaskIds() == null || parentTask.getChildTaskIds().isEmpty()) {
            if (parentTask.getTaskTypeId().equals(Constants.TaskTypes.TASK) || parentTask.getTaskTypeId().equals(Constants.TaskTypes.BUG_TASK)) {
                parentTask.setTaskTypeId(Constants.TaskTypes.PARENT_TASK);
                parentTask.setNextTaskProgressSystemChangeDateTime(null);
            }
//            // if it is the first child task then estimate of parent task = child task estimate
//            if (!Objects.equals(parentTask.getTaskEstimate(), childTask.getTaskEstimate())) {
//                parentTask.setTaskEstimate(childTask.getTaskEstimate());
//                updatedFields.add(Constants.TaskFields.ESTIMATE);
//            }

            childTaskIds = new ArrayList<>();
        } else {
            childTaskIds = new ArrayList<>(parentTask.getChildTaskIds());

//            // if it is not the first child task then add child task estimate to the parent task estimate
//            if (childTask.getTaskEstimate() != null) {
//                int updatedParentEstimate = (parentTask.getTaskEstimate() != null ? parentTask.getTaskEstimate() : 0) + childTask.getTaskEstimate();
//                parentTask.setTaskEstimate(updatedParentEstimate);
//                updatedFields.add(Constants.TaskFields.ESTIMATE);
//            }
        }
        childTaskIds.add(childTask.getTaskId());
        parentTask.setChildTaskIds(childTaskIds);
        parentTask.setFkAccountIdLastUpdated(childTask.getFkAccountIdLastUpdated());
        updatedFields.add("childTaskIds");

        if (parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED) &&
                (parentTask.getUserPerceivedPercentageTaskCompleted() != null || parentTask.getRecordedEffort() != null)) {
            // this should get the current task as well from the dB as it was saved earlier
            List<Task> allChildTasks = taskRepository.findByParentTaskId(childTask.getParentTaskId());
            int totalEarnedTime = 0, totalEstimateOfChildTasks = 0;
            for (Task cTask : allChildTasks) {
                if (childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE)) {
                    totalEstimateOfChildTasks += childTask.getEarnedTimeTask() != null ? childTask.getEarnedTimeTask() : 0;
                } else {
                    totalEstimateOfChildTasks += childTask.getTaskEstimate() != null ? childTask.getTaskEstimate() : 0;
                }
                totalEarnedTime += childTask.getEarnedTimeTask() != null ? childTask.getEarnedTimeTask() : 0;
            }
            int parentTaskUserPerceivedPercentageTaskCompleted = (int) Math.round((totalEarnedTime / (double) totalEstimateOfChildTasks) * 100);
            parentTask.setUserPerceivedPercentageTaskCompleted(parentTaskUserPerceivedPercentageTaskCompleted);
            updatedFields.add(Constants.TaskFields.USER_PERCEIVED_PERCENTAGE);
        }

        if (!Objects.equals(parentTask.getTaskProgressSystem(), childTask.getTaskProgressSystem())) {
            StatType worstStat = childTask.getTaskProgressSystem();
            for (StatType childTaskStat : allChildTaskStats) {
                if (isWorseStat(childTaskStat, worstStat)) {
                    worstStat = childTaskStat;
                }
            }
            if (!Objects.equals(parentTask.getTaskProgressSystem(), worstStat)) {
                parentTask.setTaskProgressSystem(worstStat);
                LocalDateTime currentDateTime = LocalDateTime.now();
                parentTask.setTaskProgressSystemLastUpdated(currentDateTime);
                if (parentTask.getSystemDerivedEndTs() == null || (childTask.getSystemDerivedEndTs() != null && parentTask.getSystemDerivedEndTs().isBefore(childTask.getSystemDerivedEndTs()))) {
                    parentTask.setSystemDerivedEndTs(childTask.getSystemDerivedEndTs());
                }
                updatedFields.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM);
            }
        }

        Task savedParentTask = taskRepository.save(parentTask);
        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, savedParentTask);
        taskHistoryService.addTaskHistoryOnUserUpdate(parentTaskCopy);
    }

    private void modifyEstimateOfParentTask(Task childTask, Task parentTask, List<String> updatedFields) {
        if (parentTask.getChildTaskIds() == null || parentTask.getChildTaskIds().isEmpty()) {
            // if it is the first child task then estimate of parent task = child task estimate
            if (!Objects.equals(parentTask.getTaskEstimate(), childTask.getTaskEstimate())) {
                parentTask.setTaskEstimate(childTask.getTaskEstimate());
                updatedFields.add(Constants.TaskFields.ESTIMATE);
            }
        } else {

            // if it is not the first child task then add child task estimate to the parent task estimate
            if (childTask.getTaskEstimate() != null) {
                int updatedParentEstimate = (parentTask.getTaskEstimate() != null ? parentTask.getTaskEstimate() : 0) + childTask.getTaskEstimate();
                parentTask.setTaskEstimate(updatedParentEstimate);
                updatedFields.add(Constants.TaskFields.ESTIMATE);
            }
        }

    }

    /**
     * This method will validate properties when we are creating a new bug task and returns the taskIds for the taskNumbers
     * received in the linkedTaskList in the Task request
     *
     * @param bugTask
     */
    public List<Long> validateBugTaskAndGetUpdatedLinkedTaskIds(Task bugTask) {

        // validation: environment & severity is mandatory when the bug task is created
        if (bugTask.getEnvironmentId() == null || bugTask.getSeverityId() == null) {
            throw new IllegalArgumentException("Environment & Severity is mandatory when a new bug Work Item is created");
        }

        Set<Long> linkedTaskIdList;
        if (bugTask.getBugTaskRelation() != null && !bugTask.getBugTaskRelation().isEmpty()) {
            linkedTaskIdList = new HashSet<>(bugTask.getBugTaskRelation());
        } else {
            linkedTaskIdList = new HashSet<>();
        }

        if (bugTask.getLinkedTaskList() != null && !bugTask.getLinkedTaskList().isEmpty()) {
            List<Long> linkedTaskIdsList = new ArrayList<>();
            for (LinkedTask linkedTask : bugTask.getLinkedTaskList()) {
                if (linkedTask.getTaskNumber() == null) {
                    throw new IllegalArgumentException("Invalid value for linked/ associated Work Item list");
                } else {
                    Long taskIdentifier = getTaskIdentifierFromTaskNumber(linkedTask.getTaskNumber());
                    linkedTaskIdsList.add(taskIdentifier);
                }
            }

//            List<Task> linkedTasksDb = taskRepository.findByTaskIdIn(linkedTaskIdsList);
            List<Task> linkedTasksDb = taskRepository.findByTaskIdentifierInAndFkTeamIdTeamId(linkedTaskIdsList, bugTask.getFkTeamId().getTeamId());
            for (Task linkedTaskDb : linkedTasksDb) {
                Optional<Task> parentOfLinkedTaskDb = Optional.empty();
                if(linkedTaskDb.getParentTaskId() != null) {
                    parentOfLinkedTaskDb = taskRepository.findById(linkedTaskDb.getParentTaskId());
                }
                if(!linkedTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED))
                    throw new ValidationFailedException("Invalid workflow status for the associated/ linked Work Item");
                if (linkedTaskDb.getTaskTypeId() == Constants.TaskTypes.BUG_TASK || linkedTaskDb.getIsBug())
                    throw new ValidationFailedException("Cannot create bug to bug association");
                if(parentOfLinkedTaskDb.isPresent() && parentOfLinkedTaskDb.get().getIsBug())
                    throw new ValidationFailedException("Parent of the associated child task is a bug, and bugs cannot be associated.");
                // when updating task
                if (bugTask.getBugTaskRelation() != null && bugTask.getBugTaskRelation().contains(linkedTaskDb.getTaskId())) {
                    throw new IllegalArgumentException("Given Work Item number is already associated/ linked with this bug Work Item");
                }
                linkedTaskIdList.add(linkedTaskDb.getTaskId());
            }
        }
        return new ArrayList<>(linkedTaskIdList);
    }

    /**
     * the method will modify the bug task relation list of linked tasks when a new bug task is created for that linked task
     *
     * @param bugTask
     */
    private void modifyBugTaskRelationOfLinkedTasks(Task bugTask) {
        List<Task> linkedTasksDb = taskRepository.findByTaskIdIn(bugTask.getBugTaskRelation());
        for (Task linkedTaskDb : linkedTasksDb) {
            taskHistoryService.addTaskHistoryOnSystemUpdate(linkedTaskDb);
            Set<Long> bugTaskRelationList;
            if (linkedTaskDb.getBugTaskRelation() != null) {
                bugTaskRelationList = new HashSet<>(linkedTaskDb.getBugTaskRelation());
            } else {
                bugTaskRelationList = new HashSet<>();
            }
            bugTaskRelationList.add(bugTask.getTaskId());
            linkedTaskDb.setBugTaskRelation(new ArrayList<>(bugTaskRelationList));
            Task savedLinkedTask = taskRepository.save(linkedTaskDb);
            List<String> updateFields = Collections.singletonList("bugTaskRelation");
            taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, savedLinkedTask);
        }
    }

    /**
     * gets values for static tables severity, environment and resolution in custom response format
     *
     * @return
     */
    public EnvironmentSeverityResolutionResponse getAllEnvironmentSeverityResolution(Integer entityTypeId, Long entityId) {
        if (!Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            throw new ValidationFailedException("Entity Type Id does not belong to Organization!");
        }
        if (entityId == null) {
            throw new ValidationFailedException("Entity Id cannot be null!");
        }
        Organization orgDb = organizationRepository.findByOrgId(entityId);
        if (orgDb == null) {
            throw new ValidationFailedException("Entity Id does not exist!");
        }
        List<EnvironmentIdDescDisplayAs> environmentIdDescDisplayAsList = environmentRepository.getEnvironmentIdDescDisplayAs(entityId);
        List<SeverityIdDescDisplayAs> severityIdDescDisplayAsList = severityRepository.getSeverityIdDescDisplayAs();
        List<ResolutionIdDescDisplayAs> resolutionIdDescDisplayAsList = resolutionRepository.getResolutionIdDescDisplayAs();
        return new EnvironmentSeverityResolutionResponse(environmentIdDescDisplayAsList, severityIdDescDisplayAsList, resolutionIdDescDisplayAsList);
    }
// ---------------------------------------------------------------- End -------------------------------------------------------------------------------//

    /**
     * removes the deleted account as a mentor/ observer from the open (not completed) tasks. If the entityTypeId is org, that means user is removed from the org
     * and we search at org level for all the tasks. Similarly, if the entityTypeId is Team, we search at team level for all such tasks
     */
    public void removeDeletedAccountAsMentorObserverAssignedToImmediateAttention(Long removedUserAccountId, Integer entityTypeId, Long entityId, Long adminAccountId, List<TaskIdAssignedTo> taskIdAssignedToList, String timeZone) {

        UserAccount removedUserAccount = userAccountRepository.findByAccountIdAndIsActive(removedUserAccountId, true);
        // get all open tasks of the removed account where the account is a mentor or observer
//        Specification<Task> spec = Specification
//                .where(OpenTaskSpecification.notCompletedOrDeleted())
//                .and(OpenTaskSpecification.isMentorOrObserver(removedUserAccountId))
//                .and(OpenTaskSpecification.matchesEntity(entityTypeId, entityId));

        Specification<Task> spec = Specification
                .where(OpenTaskSpecification.notCompletedOrDeleted())
                .and(OpenTaskSpecification.matchesEntity(entityTypeId, entityId))
                .and(
                        Specification.where(OpenTaskSpecification.isMentorOrObserver(removedUserAccountId))
                                .or(OpenTaskSpecification.hasImmediateAttention(removedUserAccount.getEmail())));

        List<Task> openTasks = taskRepository.findAll(spec);

        UserAccount adminUserAccount = userAccountRepository.findByAccountIdAndIsActive(adminAccountId, true);
        for (Task taskDb : openTasks) {
            Task task = new Task();
            BeanUtils.copyProperties(taskDb, task);
            List<String> updatedFields = new ArrayList<>();

            if (task.getFkAccountIdMentor1() != null && task.getFkAccountIdMentor1().getAccountId().equals(removedUserAccountId)) {
                if (task.getFkAccountIdMentor2() != null && !Objects.equals(task.getFkAccountIdMentor2().getAccountId(), removedUserAccountId)) {
                    task.setFkAccountIdMentor1(task.getFkAccountIdMentor2());
                    task.setFkAccountIdMentor2(null);
                    updatedFields.add(Constants.TaskFields.MENTOR_1);
                    updatedFields.add(Constants.TaskFields.MENTOR_2);
                } else {
                    task.setFkAccountIdMentor1(null);
                    updatedFields.add(Constants.TaskFields.MENTOR_1);
                }
            }

            if (task.getFkAccountIdMentor2() != null && task.getFkAccountIdMentor2().getAccountId().equals(removedUserAccountId)) {
                task.setFkAccountIdMentor2(null);
                updatedFields.add(Constants.TaskFields.MENTOR_2);
            }

            if (task.getFkAccountIdObserver1() != null && task.getFkAccountIdObserver1().getAccountId().equals(removedUserAccountId)) {
                if (task.getFkAccountIdObserver2() != null && !Objects.equals(task.getFkAccountIdObserver2().getAccountId(), removedUserAccountId)) {
                    task.setFkAccountIdObserver1(task.getFkAccountIdObserver2());
                    task.setFkAccountIdObserver2(null);
                    updatedFields.add(Constants.TaskFields.OBSERVER_1);
                    updatedFields.add(Constants.TaskFields.OBSERVER_2);
                } else {
                    task.setFkAccountIdObserver1(null);
                    updatedFields.add(Constants.TaskFields.OBSERVER_1);
                }
            }

            if (task.getFkAccountIdObserver2() != null && task.getFkAccountIdObserver2().getAccountId().equals(removedUserAccountId)) {
                task.setFkAccountIdObserver2(null);
                updatedFields.add(Constants.TaskFields.OBSERVER_2);
            }

            if (task.getImmediateAttention() != null && task.getImmediateAttention() == 1 &&
                    Objects.equals(task.getImmediateAttentionFrom(), removedUserAccount.getEmail())) {

                // role IDs for project managers
                List<Integer> substituteRoleIds = List.of(
                        RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(),
                        RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId()
                );

                // Retrieve access domains for the team
                List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(
                        Constants.EntityTypes.TEAM,
                        task.getFkTeamId().getTeamId(),
                        substituteRoleIds,
                        true
                );

                if (!accessDomains.isEmpty()) {
                    // Find a substitute user account
                    UserAccount substituteUserAccount = accessDomains.stream()
                            .map(accessDomain -> userAccountRepository.findByAccountIdAndIsActive(accessDomain.getAccountId(), true))
                            .filter(userAccount -> !Objects.equals(userAccount, removedUserAccount))
                            .findFirst()
                            .orElse(null);

                    if (substituteUserAccount != null) {
                        // Set immediate attention to the substitute user
                        task.setImmediateAttentionFrom(substituteUserAccount.getEmail());
                        updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION_FROM);

                        // Send notification to the substitute user
                        notificationService.immediateAttentionNotification(task, timeZone);
                    } else {
                        // Reset immediate attention if no substitute user found
                        task.setImmediateAttention(0);
                        task.setImmediateAttentionFrom(null);
                        task.setImmediateAttentionReason(null);
                        updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION);
                        updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION_FROM);
                        updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION_REASON);
                    }
                } else {
                    // Reset immediate attention if no access domains of substitue role Ids found
                    task.setImmediateAttention(0);
                    task.setImmediateAttentionFrom(null);
                    task.setImmediateAttentionReason(null);
                    updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION);
                    updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION_FROM);
                    updatedFields.add(Constants.TaskFields.IMMEDIATE_ATTENTION_REASON);
                }
            }

            task.setFkAccountIdLastUpdated(adminUserAccount);
            updateAndSaveModifiedTaskOnUserDeletionFromOrganization(task, updatedFields, timeZone);
        }

        Map<Long, Sprint> notStartedAndStartedSprintMap = new HashMap<>();
        for (TaskIdAssignedTo obj : taskIdAssignedToList) {
            modifyAssignedTo(obj, adminUserAccount, notStartedAndStartedSprintMap, timeZone);
        }
    }

    private void modifyAssignedTo(TaskIdAssignedTo obj, UserAccount adminUserAccount, Map<Long, Sprint> notStartedAndStartedSprintMap, String timeZone) {
        Long accountIdAssigned = obj.getAccountIdAssignedTo();
        Long taskId = obj.getTaskId();
        Task taskFromDb = taskRepository.findByTaskId(taskId);
        Task taskToModify = new Task();
        BeanUtils.copyProperties(taskFromDb, taskToModify);

        List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(
                Constants.EntityTypes.TEAM, taskToModify.getFkTeamId().getTeamId(), accountIdAssigned, true);
        // check if the accountIdAssigned is part of the team in which the task is created
        // ToDo: we can add a check here that the accountId's only role is not Team admin or back team admin as they can not create task
        if (accessDomains == null || accessDomains.isEmpty()) {
            throw new IllegalArgumentException("New Assigned To of work item " + taskToModify.getTaskNumber() + " is not a part of the team " + taskToModify.getFkTeamId().getTeamName());
        }

        if (taskToModify.getSprintId() != null) {
            notStartedAndStartedSprintMap.computeIfAbsent(taskToModify.getSprintId(),
                    id -> sprintRepository.findById(id).orElse(null));
            Sprint sprint = notStartedAndStartedSprintMap.get(taskToModify.getSprintId());
            if (sprint != null && Objects.equals(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId(), sprint.getSprintStatus())) {
                Set<EmailFirstLastAccountId> sprintMemberList = sprint.getSprintMembers();
                if (sprintMemberList == null) {
                    sprintMemberList = new HashSet<>();
                }
                List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                        .map(EmailFirstLastAccountId::getAccountId)
                        .collect(Collectors.toList());
                if (!sprintMemberAccountIdList.contains(accountIdAssigned)) {
                    throw new ValidationFailedException("New assigned to of work item " + taskToModify.getTaskNumber() + " is not part of it's sprint " + sprint.getSprintTitle());
                }
            }
        }
        UserAccount newAccountIdAssignedUserAccount = new UserAccount();
        newAccountIdAssignedUserAccount.setAccountId(accountIdAssigned);
        taskToModify.setFkAccountIdAssigned(newAccountIdAssignedUserAccount);
        taskToModify.setFkAccountIdAssignee(adminUserAccount);
        List<String> updatedFields = new ArrayList<>(Arrays.asList(Constants.TaskFields.ACCOUNT_ID_ASSIGNED, Constants.TaskFields.ACCOUNT_ID_ASSIGNEE));
        taskToModify.setFkAccountIdLastUpdated(adminUserAccount);
//        updateFieldsInTaskTable(taskToModify, taskToModify.getTaskId(), timeZone);
        updateAndSaveModifiedTaskOnUserDeletionFromOrganization(taskToModify, updatedFields, timeZone);
    }

    private void updateAndSaveModifiedTaskOnUserDeletionFromOrganization(Task taskToModify, List<String> updatedFields, String timeZone) {
        Task taskDb = taskRepository.findByTaskId(taskToModify.getTaskId());
        if (updatedFields.contains(Constants.TaskFields.ACCOUNT_ID_ASSIGNED)) {
            Long accountIdAssigneeDb = null, accountIdAssignedDb = null;
            if (taskDb.getFkAccountIdAssignee() != null) {
                accountIdAssigneeDb = taskDb.getFkAccountIdAssignee().getAccountId();
            }
            if (taskDb.getFkAccountIdAssigned() != null) {
                accountIdAssignedDb = taskDb.getFkAccountIdAssigned().getAccountId();
            }

            if (!Objects.equals(accountIdAssignedDb, taskToModify.getFkAccountIdAssigned().getAccountId())) {
                Long accountIdPrevAssignee1Db = taskDb.getAccountIdPrevAssignee1();
                Long accountIdPrevAssigned1Db = taskDb.getAccountIdPrevAssigned1();
                taskToModify.setAccountIdPrevAssignee1(accountIdAssigneeDb);
                taskToModify.setAccountIdPrevAssignee2(accountIdPrevAssignee1Db);
                taskToModify.setAccountIdPrevAssigned1(accountIdAssignedDb);
                taskToModify.setAccountIdPrevAssigned2(accountIdPrevAssigned1Db);
            }

            if (taskToModify.getSprintId() != null) {
                capacityService.handleAccountChange(taskDb, taskToModify);
            }
        }

        if (!updatedFields.isEmpty()) {
            taskHistoryService.addTaskHistoryOnSystemUpdate(taskDb);
            Task savedTask = taskRepository.save(taskToModify);
            taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, savedTask);
        }
    }

    /**
     * This method returns a Hashmap of team wise details of open tasks (not completed/ deleted) tasks where the accountIdAssigned = given accountid
     *
     * @param request
     * @return
     */
    public HashMap<Long, List<OpenTaskDetails>> getOpenTasksAssignedToUser(OpenTaskAssignedToUserRequest request) {
        Specification<Task> spec = Specification
                .where(OpenTaskSpecification.notCompletedOrDeleted())
                .and(OpenTaskSpecification.assignedTo(request.getAccountIdAssigned()))
                .and(OpenTaskSpecification.matchesEntity(request.getEntityTypeId(), request.getEntityId()));

        List<Task> openTasks = taskRepository.findAll(spec);
        HashMap<Long, List<OpenTaskDetails>> openTaskDetailsByTeam = new HashMap<>();

        for (Task task : openTasks) {
            OpenTaskDetails openTaskDetails = new OpenTaskDetails();
            BeanUtils.copyProperties(task, openTaskDetails);
            Long teamId = task.getFkTeamId().getTeamId();
            openTaskDetails.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
            openTaskDetails.setWorkflowStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            openTaskDetails.setTeamId(teamId);
            openTaskDetails.setOrgId(task.getFkOrgId().getOrgId());
            openTaskDetails.setTeamName(task.getFkTeamId().getTeamName());
            if (task.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
                Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                openTaskDetails.setParentTaskIdentifier(parentTask.getTaskIdentifier());
                openTaskDetails.setParentTaskNumber(parentTask.getTaskNumber());
                openTaskDetails.setParentTaskTitle(parentTask.getTaskTitle());
            }
            openTaskDetailsByTeam.computeIfAbsent(teamId, k -> new ArrayList<>()).add(openTaskDetails);
        }
        return openTaskDetailsByTeam;
    }

    /**
     * This method validated the Dependent Task Detail Request at add Task, if validated add the new dependencies in the dependency table and return the list of all dependencyIds added
     */
    public List<Long> validateAndCreateTaskDependenciesOnAddTask(Task task) {
        List<DependentTaskDetail> dependencies = task.getDependentTaskDetailRequestList();
        Map<String, RelationDirection> dependenciesMap = dependencies.stream().collect(Collectors.
                toMap(DependentTaskDetail::getRelatedTaskNumber, DependentTaskDetail::getRelationDirection));
        List<Long> dependencyIds = new ArrayList<>();

        if (dependencies != null && !dependencies.isEmpty()) {

//            // Validation: Check if the size of the dependency list is max 4
//            if (dependencies.size() > 4) {
//                throw new IllegalArgumentException("A task can have a maximum of 4 dependencies (2 predecessors and 2 successors).");
//            }

            // Validation: Check if predecessor count & successor count of the new task < 2
//            int predecessorCount = 0, successorCount = 0 ;
//            List<Long> taskNumbersFromDependencies = new ArrayList<>();
//            for (DependentTaskDetailRequest dependency : dependencies) {
//                taskNumbersFromDependencies.add(dependency.getRelatedTaskNumber());
//                if (dependency.getRelationDirection() == RelationDirection.PREDECESSOR) {
//                    predecessorCount++;
//                } else {
//                    successorCount++;
//                }
//                if (predecessorCount > 2 || successorCount > 2) {
//                    throw new IllegalArgumentException("A task can have a maximum of 2 predecessors and 2 successors");
//                }
//            }

            List<String> taskNumbersFromDependencies = new ArrayList<>();
            for (DependentTaskDetail dependency : dependencies) {
                taskNumbersFromDependencies.add(dependency.getRelatedTaskNumber());
            }

            // Validation: Check that the related taskNumbers provided actually exists in the task table
            List<Task> tasksFromDB = taskRepository.findByFkTeamIdTeamIdAndTaskNumberIn(task.getFkTeamId().getTeamId(), taskNumbersFromDependencies);
            if (tasksFromDB.size() != taskNumbersFromDependencies.size()) {
                // Some task numbers from the dependencies aren't in the DB.
                throw new IllegalArgumentException("Invalid Work Item numbers provided in dependencies.");
            }
            if(!dependenciesMap.isEmpty()) {
                for (int itr = 0; itr < tasksFromDB.size(); itr++) {
                    Task t = tasksFromDB.get(itr);
                    String workflow = t.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
                    String taskWorkflow = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus();

                    // Validation: Related Task and current task can't be in 'Completed' or 'Deleted' status.
                    if (workflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED) ||
                            workflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE) ||
                            taskWorkflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED) ||
                            taskWorkflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE)) {
                        throw new DependencyValidationException("Dependency can't be created if either of the Work Items is Completed or Deleted Work Item.");
                    }
                    // For Succeeding task Actual start date should not present whether the workflow status is Started/OnHold/Blocked
                    if (dependenciesMap.get(t.getTaskNumber()).equals(RelationDirection.SUCCESSOR) && t.getTaskActStDate() != null) {
                        throw new DependencyValidationException("The combination of the status of the Dependent Work Items is not valid.");
                    }
                    // For Preceding task Actual start date should be present whether the workflow status is Started/OnHold/Blocked
                    if (dependenciesMap.get(t.getTaskNumber()).equals(RelationDirection.PREDECESSOR) && task.getTaskActStDate() != null) {
                        throw new DependencyValidationException("The combination of the status of the Dependent Work Items is not valid.");
                    }
                }
            }
            // Validation: Check the existing predecessor/ successor dependencies of the relatedTaskNumber is less than 2
//            for (DependentTaskDetailRequest dependency : dependencies) {
//                // Get taskId for the related task number
//                Long relatedTaskId = tasksFromDB.stream()
//                        .filter(t -> t.getTaskNumber().equals(dependency.getRelatedTaskNumber()))
//                        .findFirst()
//                        .map(Task::getTaskId)
//                        .orElse(null);
//
//                // Depending on relation direction, check the count in the respective column
//                if (dependency.getRelationDirection() == RelationDirection.PREDECESSOR) {
//                    long existingPredecessorCount = dependencyRepository.countByPredecessorTaskIdAndIsRemoved(relatedTaskId, false);
//                    if (existingPredecessorCount >= 2) {
//                        throw new IllegalArgumentException("2 predecessors already exists for task number: " + dependency.getRelatedTaskNumber());
//                    }
//                } else if (dependency.getRelationDirection() == RelationDirection.SUCCESSOR){
//                    long existingSuccessorCount = dependencyRepository.countBySuccessorTaskIdAndIsRemoved(relatedTaskId, false);
//                    if (existingSuccessorCount >= 2) {
//                        throw new IllegalArgumentException("2 successors already exists for task number: " + dependency.getRelatedTaskNumber());
//                    }
//                }
//            }

            // Convert the list of tasks into a hashmap for easy retrieval.
            Map<String, Task> taskMap = tasksFromDB.stream().collect(Collectors.toMap(Task::getTaskNumber, Function.identity()));

            for (DependentTaskDetail dependency : dependencies) {
                Task relatedTask = taskMap.get(dependency.getRelatedTaskNumber());

                // Check the dependency is not between parent task and child task
                if ((task.getTaskTypeId() == Constants.TaskTypes.PARENT_TASK && relatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) ||
                        (task.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && relatedTask.getTaskTypeId() == Constants.TaskTypes.PARENT_TASK)) {
                    throw new IllegalArgumentException("Dependency between Parent and Child Tasks are not allowed.");
                }

                // Date validation for different dependency relations
                validateDatesForDependencyRelations(task, relatedTask, dependency.getRelationTypeId(), dependency.getRelationDirection());

                //checking for related task cyclic dependency on current task
                validateForParentChildCyclicDependency(task, relatedTask);

                //checking for current task cyclic dependency on related task
                validateForParentChildCyclicDependency(relatedTask, task);


                // Create Dependency object and calculate lag time
                Dependency newDependency = new Dependency();
                newDependency.setRelationTypeId(dependency.getRelationTypeId());

                int lagTime = 0; // in minutes
                if (dependency.getRelationDirection() == RelationDirection.PREDECESSOR) {
                    newDependency.setPredecessorTaskId(relatedTask.getTaskId());
                    newDependency.setSuccessorTaskId(task.getTaskId());
                    lagTime = calculateLagTimeInMinutes(relatedTask.getTaskExpEndDate(), task.getTaskExpStartDate(), task).getLagTime();
                } else if (dependency.getRelationDirection() == RelationDirection.SUCCESSOR) {
                    newDependency.setPredecessorTaskId(task.getTaskId());
                    newDependency.setSuccessorTaskId(relatedTask.getTaskId());
                    lagTime = calculateLagTimeInMinutes(task.getTaskExpEndDate(), relatedTask.getTaskExpStartDate(), task).getLagTime();
                }
                newDependency.setLagTime(lagTime);

                // Cyclic check
                if (dependencyService.checkForCycles(relatedTask, newDependency)) {
                    throw new IllegalArgumentException("Introducing this dependency would create a cycle.");
                }
                Dependency savedDependency = dependencyRepository.save(newDependency);
                if (task.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && relatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && Objects.equals(task.getParentTaskId(), relatedTask.getParentTaskId())) {
                    updateInternalAndExternalDependencyCount(task, true, true);
                }
                else {
                    if (task.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) {
                        updateInternalAndExternalDependencyCount(task, false, true);
                    }
                    if (relatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) {
                        updateInternalAndExternalDependencyCount(relatedTask, false, true);
                    }
                }
                List<Long> modifiedDependencyIdsOfRelatedTask;
                if (relatedTask.getDependencyIds() == null) {
                    modifiedDependencyIdsOfRelatedTask = new ArrayList<>();
                } else {
                    modifiedDependencyIdsOfRelatedTask = new ArrayList<>(relatedTask.getDependencyIds());
                }
                modifiedDependencyIdsOfRelatedTask.add(savedDependency.getDependencyId());
                taskHistoryService.addTaskHistoryOnSystemUpdate(relatedTask);
                relatedTask.setDependencyIds(modifiedDependencyIdsOfRelatedTask);
                taskRepository.save(relatedTask);
                taskHistoryMetadataService.addTaskHistoryMetadata(new ArrayList<>(Collections.singletonList(Constants.TaskFields.DEPENDENCY_IDS)), relatedTask);
                dependencyIds.add(savedDependency.getDependencyId());
            }
        }
        return dependencyIds;
    }

    /** extracts task Identifier from the task Number */
    public Long getTaskIdentifierFromTaskNumber(String taskNumber) {
        try {
            int hyphenIndex = taskNumber.indexOf('-');

            // Extract the substring after the hyphen
            String extractedNumber = taskNumber.substring(hyphenIndex + 1);
            return Long.valueOf(extractedNumber.trim());
        } catch (Exception e) {
            throw new NumberFormatException(e.getLocalizedMessage());
        }
    }

    /**
     * this method validates the new dependencies added in update task and sets the new dependency ids list in the task object
     */
    public void validateAndSetTaskDependenciesOnUpdateTask(Task task) {
        List<Dependency> existingDependencies = new ArrayList<>();
        List<String> taskNumbersFromNewDependencies = new ArrayList<>();
        List<Long> taskIdsFromExistingDependencies = new ArrayList<>();
        Set<Long> allRelatedTaskIds = new HashSet<>();
        int existingPredecessorCount = 0, existingSuccessorCount = 0, newPredecessorCount = 0, newSuccessorCount = 0;

        if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty()) {
            existingDependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(task.getDependencyIds(), false);
        }

        List<DependentTaskDetail> newDependencyRequestList = new ArrayList<>();
        Map<String, RelationDirection> newDependencyRequestListMap = new HashMap<>();
        if (task.getDependentTaskDetailRequestList() != null && !task.getDependentTaskDetailRequestList().isEmpty()) {
            newDependencyRequestList = task.getDependentTaskDetailRequestList();
            newDependencyRequestListMap =  newDependencyRequestList.stream().collect(Collectors
                    .toMap(DependentTaskDetail::getRelatedTaskNumber, DependentTaskDetail::getRelationDirection));
        }

        // Validation: size of the dependency request list
//        if (existingDependencies.size() + newDependencyRequestList.size() > 4) {
//            throw new IllegalArgumentException("A task can have a maximum of 4 dependencies (2 predecessors and 2 successors).");
//        }

        // Validation: Predecessor & Successor Count Validation of the given task
        for (Dependency dep : existingDependencies) {
            if (task.getTaskId().equals(dep.getPredecessorTaskId())) {
//                existingPredecessorCount++;
                taskIdsFromExistingDependencies.add(dep.getSuccessorTaskId());
            } else if (task.getTaskId().equals(dep.getSuccessorTaskId())) {
//                existingSuccessorCount++;
                taskIdsFromExistingDependencies.add(dep.getPredecessorTaskId());
            }
        }

        for (DependentTaskDetail dependentTaskDetail : newDependencyRequestList) {
            taskNumbersFromNewDependencies.add(dependentTaskDetail.getRelatedTaskNumber());
//            if(dependentTaskDetailRequest.getRelationDirection() == RelationDirection.PREDECESSOR) {
//                newSuccessorCount++;
//            } else if(dependentTaskDetailRequest.getRelationDirection() == RelationDirection.SUCCESSOR){
//                newPredecessorCount++;
//            }
        }

//        if (existingPredecessorCount + newPredecessorCount > 2 || existingSuccessorCount + newSuccessorCount > 2) {
//            throw new IllegalArgumentException("A task can have a maximum of 2 predecessors and 2 successors");
//        }

        // Validation: Check that the related taskNumbers provided actually exists in the task table -- this will also ensure that the task numbers in the
        // new dependency request list are all unique
        List<Task> tasksFromDB = taskRepository.findByFkTeamIdTeamIdAndTaskNumberIn(task.getFkTeamId().getTeamId(), taskNumbersFromNewDependencies);
        if (tasksFromDB.size() != taskNumbersFromNewDependencies.size()) {
            // Some task numbers from the dependencies aren't in the DB.
            throw new IllegalArgumentException("Invalid Work Item numbers provided in dependencies.");
        }

        // Validation: task Ids in the new dependency request list is not the same as in the existing dependencies
        List<Long> newTaskIds = tasksFromDB.stream().map(Task::getTaskId).collect(Collectors.toList());
        if (!Collections.disjoint(taskIdsFromExistingDependencies, newTaskIds)) {
            throw new IllegalArgumentException("Some Work Item numbers provided in new dependencies already exist as dependencies for the Work Item.");
        }

        // Validation: Check the existing predecessor/ successor dependencies of the relatedTaskNumber is less than 2
//        for (DependentTaskDetailRequest dependency : newDependencyRequestList) {
//            // Get taskId for the related task number
//            Long relatedTaskId = tasksFromDB.stream()
//                    .filter(t -> t.getTaskNumber().equals(dependency.getRelatedTaskNumber()))
//                    .findFirst()
//                    .map(Task::getTaskId)
//                    .orElse(null);
//
//            // Depending on relation direction, check the count in the respective column
//            if (dependency.getRelationDirection() == RelationDirection.PREDECESSOR) {
//                long existingPredecessorCountOfRelatedTask = dependencyRepository.countByPredecessorTaskIdAndIsRemoved(relatedTaskId, false);
//                if (existingPredecessorCountOfRelatedTask >= 2) {
//                    throw new IllegalArgumentException("2 predecessors already exist for task number: " + dependency.getRelatedTaskNumber());
//                }
//            } else if (dependency.getRelationDirection() == RelationDirection.SUCCESSOR) {
//                long existingSuccessorCountOfRelatedTask = dependencyRepository.countBySuccessorTaskIdAndIsRemoved(relatedTaskId, false);
//                if (existingSuccessorCountOfRelatedTask >= 2) {
//                    throw new IllegalArgumentException("2 successors already exist for task number: " + dependency.getRelatedTaskNumber());
//                }
//            }
//        }
        if(!newDependencyRequestListMap.isEmpty()) {
            for (int itr = 0; itr < tasksFromDB.size(); itr++) {
                Task t = tasksFromDB.get(itr);
                String workflow = t.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
                String taskWorkflow = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus();

                // Validation: Related Task and current task can't be in 'Completed' and 'Deleted' status.
                if (workflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED) ||
                        workflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE) ||
                        taskWorkflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED) ||
                        taskWorkflow.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE)) {
                    throw new DependencyValidationException("Dependency can't be created if either of the Work Items is Completed or Deleted Work Item.");
                }
                // For Succeeding task Actual start date should not be present whether the workflow status is Started/OnHold/Blocked
                if (newDependencyRequestListMap.get(t.getTaskNumber()).equals(RelationDirection.SUCCESSOR) && t.getTaskActStDate() != null) {
                    throw new DependencyValidationException("The combination of the status of the Dependent Work Items is not valid.");
                }
                // For Succeeding task Actual start date should be present whether the workflow status is Started/OnHold/Blocked
                if (newDependencyRequestListMap.get(t.getTaskNumber()).equals(RelationDirection.PREDECESSOR) && task.getTaskActStDate() != null) {
                    throw new DependencyValidationException("The combination of the status of the Dependent Work Items is not valid.");
                }
            }
        }
        // Validation: Checks on each dependency
        List<Long> dependencyIds = new ArrayList<>();
        for (DependentTaskDetail dependency : newDependencyRequestList) {
            Task relatedTask = tasksFromDB.stream()
                    .filter(t -> t.getTaskNumber().equals(dependency.getRelatedTaskNumber()))
                    .findFirst()
                    .orElse(null);

            // Validation: Check the dependency is not between parent task and child task
            if ((task.getTaskTypeId() == Constants.TaskTypes.PARENT_TASK && relatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) ||
                    (task.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && relatedTask.getTaskTypeId() == Constants.TaskTypes.PARENT_TASK)) {
                throw new IllegalArgumentException("Dependency between parent and child tasks is not allowed.");
            }


//             It might increase processing, while adding even a single dependency, as the entire upstream and downstream graph will have to be generated.

            Boolean isProblematic = false;
            if(Objects.equals(dependency.getRelationDirection(),RelationDirection.PREDECESSOR))
                isProblematic = dependencyService.getAllUpstreamAndDownstreamDependencies(relatedTask.getTaskId(),task.getTaskId());
            else
                isProblematic = dependencyService.getAllUpstreamAndDownstreamDependencies(task.getTaskId(),relatedTask.getTaskId());

            if(isProblematic)
                throw new ValidationFailedException("Adding this dependency creates direct or indirect redundant dependencies");

            // Validation: Date validation for different dependency relations
            validateDatesForDependencyRelations(task, relatedTask, dependency.getRelationTypeId(), dependency.getRelationDirection());

            // Create Dependency object and calculate lag time
            Dependency newDependency = new Dependency();
            newDependency.setRelationTypeId(dependency.getRelationTypeId());

            int lagTime;
            if (dependency.getRelationDirection() == RelationDirection.PREDECESSOR) {
                newDependency.setPredecessorTaskId(relatedTask.getTaskId());
                newDependency.setSuccessorTaskId(task.getTaskId());
                lagTime = calculateLagTimeInMinutes(relatedTask.getTaskExpEndDate(), task.getTaskExpStartDate(), task).getLagTime();
            } else {
                newDependency.setPredecessorTaskId(task.getTaskId());
                newDependency.setSuccessorTaskId(relatedTask.getTaskId());
                lagTime = calculateLagTimeInMinutes(task.getTaskExpEndDate(), relatedTask.getTaskExpStartDate(), task).getLagTime();
            }
            newDependency.setLagTime(lagTime);

            // Validation: Cyclic check
            if (dependencyService.checkForCycles(task, newDependency)) {
                throw new IllegalArgumentException("Introducing this dependency would create a cyclic dependency.");
            }

            Dependency savedDependency = dependencyRepository.save(newDependency);
            if (task.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && relatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && Objects.equals(task.getParentTaskId(), relatedTask.getParentTaskId())) {
                updateInternalAndExternalDependencyCount(task, true, true);
            }
            else {
                if (task.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) {
                    updateInternalAndExternalDependencyCount(task, false, true);
                }
                if (relatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) {
                    updateInternalAndExternalDependencyCount(relatedTask, false, true);
                }
            }

            // Update dependencies of the related task
            List<Long> modifiedDependencyIdsOfRelatedTask = relatedTask.getDependencyIds() != null ? new ArrayList<>(relatedTask.getDependencyIds()) : new ArrayList<>();
            modifiedDependencyIdsOfRelatedTask.add(savedDependency.getDependencyId());
            taskHistoryService.addTaskHistoryOnSystemUpdate(relatedTask);
            relatedTask.setDependencyIds(modifiedDependencyIdsOfRelatedTask);
            taskRepository.save(relatedTask);
            taskHistoryMetadataService.addTaskHistoryMetadataBySystemUpdate(new ArrayList<>(Collections.singleton(Constants.TaskFields.DEPENDENCY_IDS)), relatedTask);
            dependencyIds.add(savedDependency.getDependencyId());
        }

        // Set the updated dependencyIds in the task object
        if (task.getDependencyIds() == null || task.getDependencyIds().isEmpty()) {
            task.setDependencyIds(dependencyIds);
        } else {
            List<Long> currentDependencyIdsList = task.getDependencyIds();
            currentDependencyIdsList.addAll(dependencyIds);
            task.setDependencyIds(currentDependencyIdsList);
        }
    }

    public void updateInternalAndExternalDependencyCount (Task task, Boolean isInternal, Boolean isAdded) {
        Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
        List<String> updatedFields = new ArrayList<>();
        taskHistoryService.addTaskHistoryOnSystemUpdate(parentTask);
        if (isInternal) {
            if (isAdded) {
                parentTask.setCountChildInternalDependencies(parentTask.getCountChildInternalDependencies() + 1);
            }
            else {
                parentTask.setCountChildInternalDependencies(parentTask.getCountChildInternalDependencies() > 0 ? parentTask.getCountChildInternalDependencies() - 1 : 0);
            }
            updatedFields.add(Constants.TaskFields.CHILD_TASK_INTERNAL_DEPENDENCIES);
        }
        else {
            if (isAdded) {
                parentTask.setCountChildExternalDependencies(parentTask.getCountChildExternalDependencies() + 1);
            }
            else {
                parentTask.setCountChildExternalDependencies(parentTask.getCountChildExternalDependencies() > 0 ? parentTask.getCountChildExternalDependencies() - 1 : 0);
            }
            updatedFields.add(Constants.TaskFields.CHILD_TASK_EXTERNAL_DEPENDENCIES);
        }
        taskRepository.save(parentTask);
        taskHistoryMetadataService.addTaskHistoryMetadataBySystemUpdate(updatedFields, parentTask);
    }

    private void validateWorkflowStatusWithExistingDependenciesOnUpdateTask(Task taskToUpdate) {
        List<String> workflowStatusToCheck = List.of(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE, Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);
        List<Long> existingDependenciesIds = taskToUpdate.getDependencyIds();
        // if the workflow of the task is getting modified to other than Backlog/ Not Started and the task has dependencies
        if (existingDependenciesIds != null && workflowStatusToCheck.contains(taskToUpdate.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            List<Dependency> ftsDependencies = new ArrayList<>();
            List<Long> relatedTaskIds = new ArrayList<>();
            List<Dependency> existingDependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(existingDependenciesIds, false);

            for (Dependency dep : existingDependencies) {
                // similar to this filter all other relation types in future when other relation types are defined
                if (Objects.equals(dep.getRelationTypeId(), Constants.DependencyRelationType.FS_RELATION_TYPE)) {
                    ftsDependencies.add(dep);
                }

                if (taskToUpdate.getTaskId().equals(dep.getPredecessorTaskId())) {
                    relatedTaskIds.add(dep.getSuccessorTaskId());
                } else if (taskToUpdate.getTaskId().equals(dep.getSuccessorTaskId())) {
                    relatedTaskIds.add(dep.getPredecessorTaskId());
                }
            }

            List<Task> relatedTasksFromDb = taskRepository.findByTaskIdIn(relatedTaskIds);
            Map<Long, Task> relatedTasksMap = relatedTasksFromDb.stream().collect(Collectors.toMap(Task::getTaskId, Function.identity()));

            // condition for FTS relation type
            for (Dependency dep : ftsDependencies) {
                // Validation: All predecessor tasks must be completed if we want to modify the task's workflow status to started/ blocked/ on hold/ completed
                if (Objects.equals(taskToUpdate.getTaskId(), dep.getSuccessorTaskId())) {
                    Task predecessorTask = relatedTasksMap.get(dep.getPredecessorTaskId());
                    if (!Objects.equals(predecessorTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                        throw new ValidationFailedException("Predecessor Work Item must be completed to change the workflow status of the successor Work Item in a FTS relation");
                    }
                }
            }
        }
    }


    /**
     * method to calculate lag time when a new dependency is being added in a task via add/ update task
     */
    public LagTimeResponse calculateLagTimeInMinutes(LocalDateTime expEndDatePredecessor, LocalDateTime expStartDateSuccessor, Task task) {
        if (expEndDatePredecessor == null || expStartDateSuccessor == null) return new LagTimeResponse(0);

        HashMap<String, LocalTime> officeHrs = entityPreferenceService.getOfficeHrsBasedOnEntityPreference(task);
        LocalTime officeStartTime = officeHrs.get("officeHrsStartTime");
        LocalTime officeEndTime = officeHrs.get("officeHrsEndTime");

        // ZZZZ Made the following changes in discussion with Rohit sir on 6 april 2025
//                int breakTimeInDay = (int) (Constants.BREAK_TIME_IN_DAY / 60); // in minutes
        int breakTimeInDay = (int) (entityPreferenceService.getBreakTimeOfOrg(task.getFkOrgId().getOrgId()) / 60); // in minutes
        // ZZZZ Changes end
        int timeInFullDay = (int) Duration.between(officeStartTime, officeEndTime).toMinutes() - breakTimeInDay;
        //ToDo: get this from team preference table later on
        List<String> offDays = entityPreferenceService.getWeeklyNonWorkingDaysBasedOnEntityPreference(task);

        int totalMinutes = 0;
        // exp end date predecessor and exp start date successor is the same date
        if (expEndDatePredecessor.toLocalDate().equals(expStartDateSuccessor.toLocalDate())) {
            totalMinutes = Math.max((int) Duration.between(expEndDatePredecessor.toLocalTime().isAfter(officeStartTime) ? expStartDateSuccessor.toLocalTime() : officeStartTime,
                    expStartDateSuccessor.toLocalTime().isBefore(officeEndTime) ? expStartDateSuccessor.toLocalTime() : officeEndTime)
                    .toMinutes(), 0);
            if(!expEndDatePredecessor.toLocalTime().isAfter(officeStartTime) && !expStartDateSuccessor.toLocalTime().isBefore(officeEndTime)){
                totalMinutes -= breakTimeInDay;
            }
        } else {
            // minutes for the starting date
            totalMinutes += Math.max( (int) Duration.between(expEndDatePredecessor.toLocalTime().isAfter(officeStartTime) ? expStartDateSuccessor.toLocalTime() : officeStartTime,
                    officeEndTime).toMinutes(), 0);
            if(!expEndDatePredecessor.toLocalTime().isAfter(officeStartTime)){
                totalMinutes -= breakTimeInDay;
            }
            // minutes for the ending date
            totalMinutes += Math.max((int) Duration.between(officeStartTime, expStartDateSuccessor.toLocalTime().isBefore(officeEndTime) ? expStartDateSuccessor.toLocalTime() : officeEndTime).toMinutes(), 0);
            if(!expStartDateSuccessor.toLocalTime().isBefore(officeEndTime)){
                totalMinutes -= breakTimeInDay;
            }
            // Calculate the total minutes for the full days in between
            LocalDate nextDay = expEndDatePredecessor.toLocalDate().plusDays(1);
            LocalDate finalDay = expStartDateSuccessor.toLocalDate().minusDays(1);

            while (nextDay.isBefore(finalDay) || nextDay.isEqual(finalDay)) {
                if (!offDays.contains(nextDay.getDayOfWeek().name())) {
                    totalMinutes += timeInFullDay;
                }
                nextDay = nextDay.plusDays(1);
            }
        }

        return new LagTimeResponse(offDays, breakTimeInDay, officeEndTime, officeStartTime, totalMinutes);
    }


    /**
     * validate dates of predecessor and successor tasks based on the relationship type
     */
    private void validateDatesForDependencyRelations(Task task, Task relatedTask, Integer dependencyRelationType, RelationDirection relationDirection) {
        if (dependencyRelationType == Constants.DependencyRelationType.FS_RELATION_TYPE) {
            // assumption when initializing: relatedTask is successor in the dependency relation
            // relationship direction is wrt to related task
            Task predTask = task;
            Task succTask = relatedTask;
            if (relationDirection == RelationDirection.PREDECESSOR) {
                predTask = relatedTask;
                succTask = task;
            }
            if(predTask.getTaskExpStartDate()==null || predTask.getTaskExpEndDate()==null || succTask.getTaskExpStartDate()==null || succTask.getTaskExpEndDate()==null)
                throw new DependencyValidationException("Dependent Work Items should have valid Expected Dates set.");

            if (predTask.getTaskExpEndDate() != null && succTask.getTaskExpStartDate() != null &&
                    succTask.getTaskExpStartDate().isBefore(predTask.getTaskExpEndDate())) {
                throw new DependencyValidationException("For Finish-Start relation, predecessor's end date should be on or before successor's start date.");
            }
        }
        // ToDo Future: write logic for other dependency relation types when they are defined
    }

    /**
     * If ExpStartDate or ExpEndDate is updated in a task then this method will validate that it is compatible with the old dependencies
     * and re-calculate the lag time for the old dependencies
     *
     * @param taskFromDb   existing task retrieved from the database
     * @param expStartDate expStartDate as in the update task request
     * @param expEndDate   expEndDate as in the update task request
     */
    private void validateDependencyDatesConstraintAndRecalculateLagTime(Task taskFromDb, LocalDateTime expStartDate, LocalDateTime expEndDate) {
        List<Long> dependencyIdsFromTask = taskFromDb.getDependencyIds();
        if (dependencyIdsFromTask == null || dependencyIdsFromTask.isEmpty()) {
            return; // No dependencies to validate against simply return
        }

        List<Dependency> allDependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(dependencyIdsFromTask, false);

        // Fetch the related tasks
        List<Long> relatedTaskIds = new ArrayList<>();
        for (Dependency dep : allDependencies) {
            if (taskFromDb.getTaskId().equals(dep.getPredecessorTaskId())) {
                relatedTaskIds.add(dep.getSuccessorTaskId());
            } else {
                relatedTaskIds.add(dep.getPredecessorTaskId());
            }
        }

        List<Task> relatedTasksList = taskRepository.findByTaskIdIn(relatedTaskIds);
        Map<Long, Task> relatedTasksMap = relatedTasksList.stream().collect(Collectors.toMap(Task::getTaskId, task -> task));

        for (Dependency dependency : allDependencies) {
            Task relatedTask = relatedTasksMap.get(
                    taskFromDb.getTaskId().equals(dependency.getPredecessorTaskId()) ?
                            dependency.getSuccessorTaskId() : dependency.getPredecessorTaskId()
            );

            // Add logic for other dependency relations apart from FS in future when they are defined
            if (dependency.getRelationTypeId() == Constants.DependencyRelationType.FS_RELATION_TYPE) {
                Task predTask, succTask;

                if (taskFromDb.getTaskId().equals(dependency.getPredecessorTaskId())) {
                    predTask = taskFromDb;
                    succTask = relatedTask;
                } else {
                    predTask = relatedTask;
                    succTask = taskFromDb;
                }

                int newLagTime = 0;
                // If the task from the database is the predecessor in the relation
                if (predTask.equals(taskFromDb)) {
                    if (expEndDate != null && succTask.getTaskExpStartDate() != null &&
                            succTask.getTaskExpStartDate().isBefore(expEndDate)) {
                        throw new DependencyValidationException("For Finish-Start relation, predecessor's end date should be on or before successor's start date. For modification in expected start/end dates, validate with old dependencies.");
                    }
                    newLagTime = calculateLagTimeInMinutes(expEndDate, succTask.getTaskExpStartDate(), taskFromDb).getLagTime();
                }
                // If the task from the database is the successor in the relation
                else {
                    if (expStartDate != null && predTask.getTaskExpEndDate() != null &&
                            expStartDate.isBefore(predTask.getTaskExpEndDate())) {
                        throw new DependencyValidationException("For Finish-Start relation, successor's start date should be after or on predecessor's end date. For modification in expected start/end dates, validate with old dependencies.");
                    }
                    newLagTime = calculateLagTimeInMinutes(predTask.getTaskExpEndDate(), expStartDate, taskFromDb).getLagTime();
                }

                dependency.setLagTime(newLagTime);
            }
        }

        dependencyRepository.saveAll(allDependencies);
    }

    /**
     * gets tasks by labels and other search parameters
     */
    public List<TaskByLabelResponse> findTasksByLabels(List<String> labelNames, Long teamId, Long accountId) {
        List<Task> tasks = new ArrayList<>();
        List<TaskByLabelResponse> taskByLabelResponseList = new ArrayList<>();
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Incorrect TeamId. Team not found"));
        if (accountId == null) {
            tasks = taskRepository.findDistinctByLabels_LabelNameInAndFkTeamId_TeamId(labelNames, teamId);
        } else {
            tasks = taskRepository.findDistinctByLabels_LabelNameInAndFkTeamId_TeamIdAndFkAccountIdAssigned_AccountId(labelNames, teamId, accountId);
        }

        for (Task task : tasks) {
            TaskByLabelResponse taskByLabelResponse = new TaskByLabelResponse();
            BeanUtils.copyProperties(task, taskByLabelResponse);
            taskByLabelResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            taskByLabelResponse.setTeamName(team.getTeamName());
            if (task.getFkAccountIdAssigned() != null) {
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), true);
                User user = userAccount.getFkUserId();
                taskByLabelResponse.setEmail(userAccount.getEmail());
                String fullName = user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "");
                taskByLabelResponse.setFullName(fullName);
            }
            taskByLabelResponseList.add(taskByLabelResponse);
        }

        return taskByLabelResponseList;
    }

    public void validateAccounts(Task task) {

        if (task.getFkAccountIdAssigned() != null && !accessDomainService.IsActiveAccessDomain(task.getFkAccountIdAssigned().getAccountId(), task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("Assigned to user should be member of the team");
        }

        if (task.getFkAccountIdMentor1() != null && !accessDomainService.IsActiveAccessDomain(task.getFkAccountIdMentor1().getAccountId(), task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("Mentor 1 is not the member of team");
        }

        if (task.getFkAccountIdMentor2() != null && !accessDomainService.IsActiveAccessDomain(task.getFkAccountIdMentor2().getAccountId(), task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("Mentor 2 is not the member of team");
        }

        if (task.getFkAccountIdObserver1() != null && !accessDomainService.IsActiveAccessDomain(task.getFkAccountIdObserver1().getAccountId(), task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("Observer 1 is not the member of team");
        }

        if (task.getFkAccountIdObserver2() != null && !accessDomainService.IsActiveAccessDomain(task.getFkAccountIdObserver2().getAccountId(), task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("Observer 2 is not the member of team");
        }

        if (task.getFkAccountIdBugReportedBy() != null && task.getFkAccountIdBugReportedBy().getAccountId() != null && !accessDomainService.IsActiveAccessDomain(task.getFkAccountIdBugReportedBy().getAccountId(), task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("User reporting the bug is not the member of team");
        }

        if (task.getImmediateAttentionFrom() != null && task.getImmediateAttentionReason() != null) {
            UserAccount immediateAttentionUser = userAccountRepository.findByEmailAndOrgIdAndIsActive(task.getImmediateAttentionFrom(), task.getFkOrgId().getOrgId(), true);
            if (immediateAttentionUser == null) {
                throw new ValidationFailedException("Please enter the valid immediate attention from");
            }
            if (!accessDomainService.IsActiveAccessDomain(immediateAttentionUser.getAccountId(), task.getFkTeamId().getTeamId())) {
                throw new ValidationFailedException("Immediate Attention user should be member of the team");
            }
        }
    }

    /**
     * This methods blocks all the child tasks if parent task is blocked with blocked reason in child task that parent task is blocked
     *
     * @param parentTask
     * @param timeZone
     */
    public void synchronizeChildTaskDetailsWhenParentBlocked(Task parentTask, String timeZone) {
        // check if the parent's workflow is changed to Blocked
        Task parentTaskFromDb = taskRepository.findByTaskId(parentTask.getTaskId());
        String parentWorkflowStatus = parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
        if (!Objects.equals(parentTaskFromDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), parentWorkflowStatus) && (parentWorkflowStatus.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE))) {
            List<Task> childTasks = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
            for (Task childTask : childTasks) {
                //checking if child task is not completed, deleted or blocked
                if (!Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) && !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) && !Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    Task childTaskCopy = new Task();
                    BeanUtils.copyProperties(childTask, childTaskCopy);
                    List<String> updatedFields = new ArrayList<>();

                    if (childTask.getTaskExpStartDate() == null) {
                        childTask.setTaskExpStartDate(parentTask.getTaskExpStartDate());
                        childTask.setTaskExpStartTime(parentTask.getTaskExpStartTime());
                        updatedFields.add(Constants.TaskFields.EXP_START_DATE);
                        updatedFields.add(Constants.TaskFields.EXP_START_TIME);
                    }

                    if (childTask.getTaskActStDate() == null) {
                        childTask.setTaskActStDate(parentTask.getTaskActStDate());
                        childTask.setTaskActStTime(parentTask.getTaskActStTime());
                        updatedFields.add(Constants.TaskFields.ACTUAL_START_DATE);
                    }

                    if (childTask.getTaskExpEndDate() == null) {
                        childTask.setTaskExpEndDate(parentTask.getTaskExpEndDate());
                        childTask.setTaskExpEndTime(parentTask.getTaskExpEndTime());
                        updatedFields.add(Constants.TaskFields.EXP_END_DATE);
                        updatedFields.add(Constants.TaskFields.EXP_END_TIME);
                    }

                    if (childTask.getTaskPriority() == null) {
                        childTask.setTaskPriority(parentTask.getTaskPriority());
                        updatedFields.add(Constants.TaskFields.PRIORITY);
                    }


                    childTask.setBlockedReasonTypeId(Constants.BlockedMessages.PARENT_TASK_BLOCKED_ID);
                    updatedFields.add(Constants.TaskFields.BLOCKED_REASON_TYPE_ID);
                    childTask.setBlockedReason(Constants.BlockedMessages.PARENT_TASK_BLOCKED);
                    updatedFields.add(Constants.TaskFields.BLOCKED_REASON);
                    WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE, childTask.getTaskWorkflowId());
                    childTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
                    childTask.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
                    updatedFields.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
                    computeAndUpdateStatForTask(childTask, true);
                    updatedFields.add(Constants.TaskFields.TASK_STATE);
                    updatedFields.add(Constants.TaskFields.TASK_PROGRESS_SYSTEM);

                    // Persist the changes (consider using batch save for performance)
                    taskHistoryService.addTaskHistoryOnSystemUpdate(childTaskCopy);
                    taskRepository.save(childTask);
                    taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, childTask);
                }
            }
        }
    }

    /**
     * This method maps all the recorded efforts of a task with it's task number
     */
    public List<RecordEffortResponse> getAllEffortsListByTaskNumber(Long taskId, String accountIds) {
        List<Long> accountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        Task task = taskRepository.findByTaskId(taskId);
        List<RecordEffortResponse> recordEffortResponseList = new ArrayList<>();
        List<Task> taskList = new ArrayList<>();
        taskList.add(task);
        //if task type is parent than returning all efforts recorded in child tasks
        if (task.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {
            List<Long> childTaskIds = task.getChildTaskIds();
            taskList.addAll(taskRepository.findByParentTaskId(task.getTaskId()));
        }
        for (Task responseTask : taskList) {
            RecordEffortResponse recordEffortResponse = new RecordEffortResponse();
            recordEffortResponse.setTeamId(responseTask.getFkTeamId().getTeamId());
            recordEffortResponse.setTeamCode(responseTask.getFkTeamId().getTeamCode());
            recordEffortResponse.setTaskNumber(responseTask.getTaskNumber());
            recordEffortResponse.setTaskTypeId(responseTask.getTaskTypeId());
            recordEffortResponse.setTaskId(responseTask.getTaskId());
            Map<LocalDate, List<RecordedEffortsByDateTime>> dateToEffortMap = getRecordedEfforts(responseTask, accountIdsList);
            recordEffortResponse.setRecordedEffortsByDateTimeList(dateToEffortMap);
            recordEffortResponseList.add(recordEffortResponse);
        }
        return recordEffortResponseList;
    }

    /**
     * Retrieves a consolidated map of recorded efforts associated with a specific task, including efforts from both task and linked meetings.
     */
    public Map<LocalDate, List<RecordedEffortsByDateTime>> getRecordedEfforts(Task task, List<Long> accountIds) {

        List<TimeSheet> timeSheets = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TASK, task.getTaskId());
        List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());

        for (Meeting meeting : meetingList) {
            List<TimeSheet> meetingTimesheet = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.MEETING, meeting.getMeetingId());
            timeSheets.addAll(meetingTimesheet);
        }
        Map<LocalDate, List<RecordedEffortsByDateTime>> recordedEffortsList = new TreeMap<>();

        for (TimeSheet timeSheet : timeSheets) {
            RecordedEffortsByDateTime recordedEffortsByDateTime = new RecordedEffortsByDateTime();
            UserAccount user = userAccountRepository.findByAccountId(timeSheet.getAccountId());
            recordedEffortsByDateTime.setAccountId(timeSheet.getAccountId());
            recordedEffortsByDateTime.setFirstName(user.getFkUserId().getFirstName());
            recordedEffortsByDateTime.setLastName(user.getFkUserId().getLastName());
            recordedEffortsByDateTime.setMiddleName(user.getFkUserId().getMiddleName() != null ? user.getFkUserId().getMiddleName() : null);
            recordedEffortsByDateTime.setRecordedEffortDate(timeSheet.getNewEffortDate());
            recordedEffortsByDateTime.setRecordedEffortMins(timeSheet.getNewEffort());
            recordedEffortsByDateTime.setRecordedEarnedTime(timeSheet.getEarnedTime() == null ? 0 : timeSheet.getEarnedTime());
            recordedEffortsByDateTime.setLastUpdatedDateTime(timeSheet.getLastUpdatedDateTime());
            recordedEffortsByDateTime.setTimeTrackingId(timeSheet.getTimeTrackingId());
            recordedEffortsByDateTime.setEntityId(timeSheet.getEntityId());
            recordedEffortsByDateTime.setEntityNumber(timeSheet.getEntityNumber());
            if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.MEETING)) {
                recordedEffortsByDateTime.setEntityTypeId(Constants.EntityTypes.MEETING);
                if (timeSheet.getReferenceEntityNum() != null && Objects.equals(timeSheet.getReferenceEntityNum(), task.getTaskNumber())) {
                    recordedEffortsByDateTime.setIsBilled(Boolean.TRUE);
                }
            } else {
                recordedEffortsByDateTime.setEntityTypeId(Constants.EntityTypes.TASK);
            }
            List<Long> accountsWithEditAccess=getAccountIdsOfRoleMembersWithEditEffortAccess(task.getFkTeamId().getTeamId());
            recordedEffortsByDateTime.setIsEditable((validateEditDuration(timeSheet) && accountIds.contains(timeSheet.getAccountId()) ? Boolean.TRUE : Boolean.FALSE)|containsAny(accountsWithEditAccess,accountIds));
            if (recordedEffortsList.containsKey(timeSheet.getNewEffortDate())) {
                List<RecordedEffortsByDateTime> recordedEffortsByDateTimeList = recordedEffortsList.get(timeSheet.getNewEffortDate());
                recordedEffortsByDateTimeList.add(recordedEffortsByDateTime);
            } else {
                List<RecordedEffortsByDateTime> recordedEffortsByDateTimeList = new ArrayList<>();
                recordedEffortsByDateTimeList.add(recordedEffortsByDateTime);
                recordedEffortsList.put(timeSheet.getNewEffortDate(), recordedEffortsByDateTimeList);
            }
        }

        return recordedEffortsList;
    }

    /**
     * This method validates if user is editing efforts in time limit provided
     */
    public Boolean validateEditDuration(TimeSheet timeSheet) {
        Integer effortEditTimeDuration = getEditTimeDurationForTask(timeSheet.getOrgId(), timeSheet.getTeamId(), timeSheet.getEntityTypeId());
        Duration timeDifference = Duration.between(timeSheet.getCreatedDateTime(), LocalDateTime.now());
        if (timeDifference.toMinutes() <= effortEditTimeDuration.longValue()) {
            return true;
        }
        return false;
    }

    /**
     * This effort returns edit duration according to user preference
     */
    public Integer getEditTimeDurationForTask(Long orgId, Long teamId, Integer entityTypeId) {
        EntityPreference teamPreferenceDb = entityPreferenceService.getEntityPreference(Constants.EntityTypes.TEAM, teamId);
        if (teamPreferenceDb != null && Objects.equals(entityTypeId, Constants.EntityTypes.TASK) && teamPreferenceDb.getTaskEffortEditDuration() != null) {
            return teamPreferenceDb.getTaskEffortEditDuration();
        } else if (teamPreferenceDb != null && Objects.equals(entityTypeId, Constants.EntityTypes.MEETING) && teamPreferenceDb.getMeetingEffortEditDuration() != null) {
            return teamPreferenceDb.getMeetingEffortEditDuration();
        }

        EntityPreference orgPreferenceDb = entityPreferenceService.getEntityPreference(com.tse.core_application.model.Constants.EntityTypes.ORG, orgId);
        if (orgPreferenceDb != null && Objects.equals(entityTypeId, Constants.EntityTypes.TASK) && orgPreferenceDb.getTaskEffortEditDuration() != null) {
            return orgPreferenceDb.getTaskEffortEditDuration();
        } else if (orgPreferenceDb != null && Objects.equals(entityTypeId, Constants.EntityTypes.MEETING) && orgPreferenceDb.getMeetingEffortEditDuration() != null) {
            return orgPreferenceDb.getMeetingEffortEditDuration();
        }

        if (Objects.equals(entityTypeId, Constants.EntityTypes.MEETING)) {
            return defaultMeetingEffortEditTime;
        } else {
            return defaultTaskEffortEditTime;
        }
    }

    /**
     * This method edits recorded effort
     *
     * @return
     * @throws IllegalAccessException
     * @throws TimeLimitExceededException
     */
    @Transactional
    public String editRecordedEffortsAndUpdateTimesheet(Long timeTrackingId, List<Long> accountIds, String localTimeZone, NewEffortTrack newEffortTrack) throws IllegalAccessException, TimeLimitExceededException {
        Integer noOfAudit = 0;
        Optional<TimeSheet> optionalTimeSheet = timeSheetRepository.findById(timeTrackingId);
        Task taskCopy = new Task();
        if (optionalTimeSheet.isPresent()) {
            TimeSheet timeSheet = optionalTimeSheet.get();

            //checking if newEfforts and date are changed or not
            if (Objects.equals(newEffortTrack.getNewEffortDate(), timeSheet.getNewEffortDate()) && Objects.equals(newEffortTrack.getNewEffort(), timeSheet.getNewEffort())) {
                throw new ValidationException("Nothing to edit in new efforts record");
            }
            //Checking if user is editing effort in time limit provided
            //Checking if user has a role (11, 12, 14, 15)
            boolean hasEditAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, timeSheet.getTeamId(), accountIds, Constants.ROLES_WITH_TEAM_EFFORT_EDIT_ACCESS, true);
            if (!validateEditDuration(timeSheet) && !hasEditAccess) {
                throw new TimeLimitExceededException("Time limit exceeded : Cannot edit efforts after " + getEditTimeDurationForTask(timeSheet.getOrgId(), timeSheet.getTeamId(), timeSheet.getEntityTypeId()) + " minutes");
            }

            Integer previousBurnedEffort = timeSheet.getNewEffort();

            //Proceed only if entity type id is task
            if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.TASK)) {
                Task task = taskRepository.findByTaskId(timeSheet.getEntityId());
                BeanUtils.copyProperties(task, taskCopy);
                Integer sum = 0;
                String taskWorkflowStatus = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
                List<String> workflowStatusNotAllowed = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED, Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE, Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD);
                List<String> workflowStatusAllowed= Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED,Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD,Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED,Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED);

                if (userAccountService.getActiveUserAccountByAccountId(timeSheet.getAccountId()) != null) {

                    // validation: new effort can't be recorded for task with BACKLOG, NOT STARTED, ON HOLD, DELETED, BLOCKED workflow status
                    if (!hasEditAccess && workflowStatusNotAllowed.contains(taskWorkflowStatus)) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("error"));
                        logger.error("Effort can not be edited for Work Item with BACKLOG, NOT STARTED, DELETED, BLOCKED, ON HOLD workflow status", new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Effort can not be edited for Work Item with BACKLOG, NOT STARTED, DELETED, BLOCKED, ON HOLD  workflow status");
                    }

                    if(hasEditAccess && !workflowStatusAllowed.contains(taskWorkflowStatus)){
                        String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("error"));
                        logger.error("Effort can not be edited for Work Item that is NOT STARTED workflow status", new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw new ValidationFailedException("Effort can not be edited for Work Item that is NOT STARTED workflow status");
                    }

                    List<String> updatedFields = new ArrayList<>();
                    // validation: newEffortDate should be less or equal to today's date
                    LocalDate dateFromRequest = newEffortTrack.getNewEffortDate();
                    if (dateFromRequest != null) {
                        LocalDateTime clientDate = DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now(), localTimeZone);
                        // validation: newEffortDate should be less or equal to today's date
                        if (dateFromRequest.isAfter(clientDate.toLocalDate())) {

                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in editRecordedEffortsAndUpdateTimesheet"));
                            logger.error("Effort date can not be greater than today's date ", new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            throw new ValidationFailedException("Effort date can not be greater than today's date");
                        }

                        LocalDateTime clientActEndDate = null;
                        if (task.getTaskActEndDate() != null) {
                            clientActEndDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActEndDate(), localTimeZone);
                        }
                        LocalDateTime clientActStDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActStDate(), localTimeZone);
                        // validation: newEffortDate can't be prior to Task actual start date and later than Task actual end date
                        if (dateFromRequest.isBefore(clientActStDate.toLocalDate()) || (clientActEndDate != null && dateFromRequest.isAfter(clientActEndDate.toLocalDate()))) {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("validation error in editRecordedEffortsAndUpdateTimesheet"));
                            logger.error("Effort date can not be prior to Work Item actual start date or later than Work Item actual end date", new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            throw new ValidationFailedException("Effort date can not be prior to Work Item actual start date or later than Work Item actual end date");
                        }
                    }

                    //checking if new effort date is not equal to previous effort date
                    if (!Objects.equals(newEffortTrack.getNewEffortDate(), timeSheet.getNewEffortDate())) {
                        NewEffortTrack newEffortTrackTemp=new NewEffortTrack(newEffortTrack.getNewEffort(), newEffortTrack.getNewEffortDate());
                        if(effortsWithin24(timeSheet.getAccountId(),newEffortTrackTemp)>0) {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Effort cannot be more than 24 hours for a single day"));
                            logger.error("Effort cannot be more than 24 hours for a single day ", new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            throw new ValidationFailedException("Effort cannot be more than 24 hours for a single day");
                        }
                        timeSheet.setNewEffortDate(newEffortTrack.getNewEffortDate());
                    }

                    //checking if new effort is not equal to previous effort
                    if (!Objects.equals(newEffortTrack.getNewEffort(), timeSheet.getNewEffort())) {
                        sum += newEffortTrack.getNewEffort() - ((timeSheet.getNewEffort() != null) ? timeSheet.getNewEffort() : 0);
                        timeSheet.setNewEffort(newEffortTrack.getNewEffort());
                        Integer totalEffort = (task.getRecordedEffort() == null) ? sum : (task.getRecordedEffort() + sum);
                        task.setRecordedEffort(totalEffort);
                        updatedFields.add(Constants.TaskFields.RECORDED_EFFORT);
                        noOfAudit++;
                        task.setRecordedTaskEffort((task.getRecordedTaskEffort() == null) ? sum : (task.getRecordedTaskEffort() + sum));
                        updatedFields.add(Constants.TaskFields.RECORDED_TASK_EFFORT);
                        noOfAudit++;
                        task.setTotalEffort((task.getTotalEffort() == null) ? sum : (task.getTotalEffort() + sum));
                        updatedFields.add(Constants.TaskFields.TOTAL_EFFORT);
                        noOfAudit++;
                        NewEffortTrack newEffortTrackTemp=new NewEffortTrack(sum,newEffortTrack.getNewEffortDate());
                        if(effortsWithin24(timeSheet.getAccountId(),newEffortTrackTemp)>0) {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Effort cannot be more than 24 hours for a single day"));
                            logger.error("Effort cannot be more than 24 hours for a single day ", new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            throw new ValidationFailedException("Effort cannot be more than 24 hours for a single day");
                        }
                        taskRepository.save(task);
                        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                            Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                            Task parentTaskCopy = new Task();
                            BeanUtils.copyProperties(parentTask, parentTaskCopy);
                            parentTask.setRecordedEffort((parentTask.getRecordedEffort() == null) ? sum : (parentTask.getRecordedEffort() + sum));
                            parentTask.setRecordedTaskEffort((parentTask.getRecordedTaskEffort() == null) ? sum : (parentTask.getRecordedTaskEffort() + sum));
                            parentTask.setTotalEffort((parentTask.getTotalEffort() == null) ? sum : (parentTask.getTotalEffort() + sum));
                            taskRepository.save(parentTask);
                            Audit auditCreated = auditService.createAudit(parentTask, noOfAudit, parentTask.getTaskId(), Constants.TaskFields.RECORDED_EFFORT);
                            auditRepository.save(auditCreated);
                            taskHistoryService.addTaskHistoryOnUserUpdate(parentTaskCopy);
                            taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, parentTask);
                        }
                        Audit auditCreated = auditService.createAudit(task, noOfAudit, task.getTaskId(), Constants.TaskFields.RECORDED_EFFORT);
                        auditRepository.save(auditCreated);
                        taskHistoryService.addTaskHistoryOnUserUpdate(taskCopy);
                        taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, task);
                    }
                    updateBurnedEffortInSprint (timeSheet, previousBurnedEffort, timeSheet.getNewEffort());
                    updateBurnedEffortInEpic (timeSheet, previousBurnedEffort, timeSheet.getNewEffort());
                    timeSheetRepository.save(timeSheet);

                    return "Effort recorded successfully.";
                } else {
                    throw new ValidationException("The efforts cannot be edited since the user is inactive : " + task.getTaskNumber());
                }
            } else {
                throw new IllegalAccessException("Effort editing is restricted to Work Items. You are not authorized to edit efforts for other entities.");
            }
        } else {
            throw new NoDataFoundException();
        }
    }

    /**
     * this methods adjusts earned time in task and timesheet records when estimate of a task is modified
     */
    public void modifyEarnedTimeInTaskAndTimeSheetForEstimateChange(Task updatedTask) {
        Task taskFromDb = findTaskByTaskId(updatedTask.getTaskId());
        boolean isEstimateUpdated = false, isUserPerceivedUpdated = false, isNewEffortsUpdated = false;

        if (updatedTask.getTaskEstimate()!=null && updatedTask.getTaskEstimate() == 0)
            updatedTask.setTaskEstimate(null);

        if (taskFromDb.getTaskEstimate() != null && updatedTask.getTaskEstimate() != null && !Objects.equals(taskFromDb.getTaskEstimate(), updatedTask.getTaskEstimate()))
            isEstimateUpdated = true;

        if (updatedTask.getNewEffortTracks() != null && !updatedTask.getNewEffortTracks().isEmpty())
            isNewEffortsUpdated = true;

        if (taskFromDb.getUserPerceivedPercentageTaskCompleted() != null && updatedTask.getUserPerceivedPercentageTaskCompleted() != null && !Objects.equals(taskFromDb.getUserPerceivedPercentageTaskCompleted(), updatedTask.getUserPerceivedPercentageTaskCompleted()))
            isUserPerceivedUpdated = true;

        if (!isEstimateUpdated || updatedTask.getRecordedEffort() == null || updatedTask.getRecordedEffort() == 0) {
            // Either no change in estimate or if estimate is changed but recorded effort is null/ zero
            return;
        }

        // Case where Estimate & UserPerceived is changed but new Effort is not added  -- not applicable as of now
        if (!isNewEffortsUpdated && isUserPerceivedUpdated &&  taskFromDb.getRecordedEffort() != null) {
            Integer newEstimate = updatedTask.getTaskEstimate();
            Integer oldEstimate = taskFromDb.getTaskEstimate();
            int newEarnedTime = Math.round((float) taskFromDb.getEarnedTimeTask() * (newEstimate.floatValue() / oldEstimate.floatValue()));
            updatedTask.setEarnedTimeTask(newEarnedTime);

            // Update timesheet records
            List<TimeSheet> taskTimeSheets = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TASK, updatedTask.getTaskId());
            List<TimeSheet> meetingTimeSheets = timeSheetRepository.findByReferenceEntityTypeIdAndReferenceEntityId(Constants.EntityTypes.TASK, updatedTask.getTaskId());

            Map<Long, Sprint> sprintMap = new HashMap<>();
            Map<Long, SprintCapacityMetrics> sprintCapacityMap = new HashMap<>();
            Map<Long, UserCapacityMetrics> userCapacityMap = new HashMap<>();
            Map<Long, Epic> epicMap = new HashMap<>();

            List<TimeSheet> updatedTimeSheets = Stream.concat(taskTimeSheets.stream(), meetingTimeSheets.stream())
                    .peek(timeSheet -> {
                        int previousTimeSheetEarnedTime = timeSheet.getEarnedTime() != null ? timeSheet.getEarnedTime() : 0;
                        int updatedTimeSheetEarnedTime = Math.round((float) timeSheet.getEarnedTime() * (newEstimate.floatValue() / oldEstimate.floatValue()));
                        int updatedIncreaseInUserPerceivedPercentageCompleted = Math.round((float) timeSheet.getIncreaseInUserPerceivedPercentageTaskCompleted() * (updatedTask.getUserPerceivedPercentageTaskCompleted().floatValue() / taskFromDb.getUserPerceivedPercentageTaskCompleted().floatValue()));
                        updateEarnedEffortInSprint (timeSheet, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime, sprintMap, sprintCapacityMap, userCapacityMap);
                        updateEarnedEffortInEpic (timeSheet, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime, epicMap);
                        timeSheet.setEarnedTime(updatedTimeSheetEarnedTime);
                        timeSheet.setIncreaseInUserPerceivedPercentageTaskCompleted(updatedIncreaseInUserPerceivedPercentageCompleted);
                    })
                    .collect(Collectors.toList());

            timeSheetRepository.saveAll(updatedTimeSheets);

            sprintRepository.saveAll(sprintMap.values());
            sprintCapacityMetricsRepository.saveAll(sprintCapacityMap.values());
            userCapacityMetricsRepository.saveAll(userCapacityMap.values());
            epicRepository.saveAll(epicMap.values());

            return;
        }

        // Case where only estimate is changed and recorded effort is not null
        // Case where estimate is changed and newEffort/ UserPerceived is also changed
        if (taskFromDb.getRecordedEffort() != null) {
            modifyOldTimeSheetRecordOnEstimateUpdate(updatedTask, taskFromDb);
        }
    }

    /** If estimate is updated in the task, this method modifies the earned time for the previous efforts recorded in that task*/
    private void modifyOldTimeSheetRecordOnEstimateUpdate(Task updatedTask, Task taskFromDb) {
        Integer newEstimate = updatedTask.getTaskEstimate();
        Integer oldEstimate = taskFromDb.getTaskEstimate();
        int newEarnedTime = Math.round((float) taskFromDb.getEarnedTimeTask() * (newEstimate.floatValue() / oldEstimate.floatValue()));
        updatedTask.setEarnedTimeTask(newEarnedTime);

        // Update timesheet records
        List<TimeSheet> taskTimeSheets = timeSheetRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TASK, updatedTask.getTaskId());
        List<TimeSheet> meetingTimeSheets = timeSheetRepository.findByReferenceEntityTypeIdAndReferenceEntityId(Constants.EntityTypes.TASK, updatedTask.getTaskId());

        Map<Long, Sprint> sprintMap = new HashMap<>();
        Map<Long, SprintCapacityMetrics> sprintCapacityMap = new HashMap<>();
        Map<Long, UserCapacityMetrics> userCapacityMap = new HashMap<>();
        Map<Long, Epic> epicMap = new HashMap<>();

        List<TimeSheet> updatedTimeSheets = Stream.concat(taskTimeSheets.stream(), meetingTimeSheets.stream())
                .peek(timeSheet -> {
                    int previousTimeSheetEarnedTime = timeSheet.getEarnedTime() != null ? timeSheet.getEarnedTime() : 0;
                    int updatedTimeSheetEarnedTime = Math.round((float) timeSheet.getEarnedTime() * (newEstimate.floatValue() / oldEstimate.floatValue()));
                    updateEarnedEffortInSprint (timeSheet, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime, sprintMap, sprintCapacityMap, userCapacityMap);
                    updateEarnedEffortInEpic (timeSheet, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime, epicMap);
                    timeSheet.setEarnedTime(updatedTimeSheetEarnedTime);
                })
                .collect(Collectors.toList());

        timeSheetRepository.saveAll(updatedTimeSheets);

        sprintRepository.saveAll(sprintMap.values());
        sprintCapacityMetricsRepository.saveAll(sprintCapacityMap.values());
        userCapacityMetricsRepository.saveAll(userCapacityMap.values());
    }

    public void updateEarnedEffortInSprint(TimeSheet timeSheet, Integer previousTimeSheetEarnedTime, Integer updatedTimeSheetEarnedTime, Map<Long, Sprint> sprintMap, Map<Long, SprintCapacityMetrics> sprintCapacityMap, Map<Long, UserCapacityMetrics> userCapacityMap) {
        Long sprintId = timeSheet.getSprintId();
        Long accountId = timeSheet.getAccountId();
        if (sprintId != null) {
            Sprint sprint = sprintMap.computeIfAbsent(sprintId, id -> sprintRepository.findById(id).orElse(null));
            if (sprint != null && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                capacityService.updateSprintEarnedEffort(sprint, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime);
                SprintCapacityMetrics sprintCapacity = sprintCapacityMap.computeIfAbsent(sprintId,
                        id -> sprintCapacityMetricsRepository.findBySprintId(id));
                if (sprintCapacity != null) {
                    capacityService.updateSprintCapacityMetricsEarnedEffort(sprintCapacity, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime, timeSheet.getEntityTypeId());
                }
                if (accountId != null) {
                    UserCapacityMetrics userCapacity = userCapacityMap.computeIfAbsent(accountId,
                            id -> userCapacityMetricsRepository.findBySprintIdAndAccountId(sprintId, id).orElse(null));
                    if (userCapacity != null) {
                        capacityService.updateUserCapacityMetricsEarnedEffort(userCapacity, previousTimeSheetEarnedTime, updatedTimeSheetEarnedTime, timeSheet.getEntityTypeId());
                    }
                }
            }
        }

    }

    public void updateBurnedEffortInSprint(TimeSheet timeSheet, Integer previousTimeSheetBurnedTime, Integer updatedTimeSheetBurnedTime) {
        Long sprintId = timeSheet.getSprintId();
        Long accountId = timeSheet.getAccountId();
        if (sprintId != null) {
            Sprint sprint = sprintRepository.findBySprintId(sprintId);
            if (sprint != null && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                SprintCapacityMetrics sprintCapacity = sprintCapacityMetricsRepository.findBySprintId(sprintId);
                if (sprintCapacity != null) {
                    capacityService.updateSprintCapacityMetricsBurnedEffort(sprintCapacity, previousTimeSheetBurnedTime, updatedTimeSheetBurnedTime, timeSheet.getEntityTypeId());
                }
                if (accountId != null) {
                    Optional<UserCapacityMetrics> optionalUserCapacity = userCapacityMetricsRepository.findBySprintIdAndAccountId(sprintId, accountId);
                    if (optionalUserCapacity.isPresent()) {
                        UserCapacityMetrics userCapacity = optionalUserCapacity.get();
                        capacityService.updateUserCapacityMetricsBurnedEffort(userCapacity, previousTimeSheetBurnedTime, updatedTimeSheetBurnedTime, timeSheet.getEntityTypeId());
                    }
                }
            }
        }

    }

    public void updateEarnedEffortInEpic (TimeSheet timeSheet, Integer previousTimeSheetEarnedTime, Integer updatedTimeSheetEarnedTime, Map<Long, Epic> epicMap) {
        Long epicId = timeSheet.getEpicId();
        if (epicId != null) {
            Epic epic = epicMap.computeIfAbsent(epicId, id -> epicRepository.findById(id).orElse(null));
            if (epic != null && epic.getFkWorkflowEpicStatus() != null && !Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId()) && !Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
                Integer previousEpicEarnedEffort = (epic.getEarnedEfforts() != null ? epic.getEarnedEfforts() : 0);
                Integer newEpicEarnedEffort = Math.max(0, previousEpicEarnedEffort - previousTimeSheetEarnedTime + updatedTimeSheetEarnedTime);
                epic.setEarnedEfforts(newEpicEarnedEffort);
            }
        }
    }

    public void updateBurnedEffortInEpic (TimeSheet timeSheet, Integer previousTimeSheetLoggedTime, Integer updatedTimeSheetLoggedTime) {
        Long epicId = timeSheet.getEpicId();
        if (epicId != null) {
            Epic epic = epicRepository.findById(epicId).orElse(null);
            if (epic != null && epic.getFkWorkflowEpicStatus() != null && !Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId()) && !Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
                Integer previousEpicLoggedEffort = (epic.getLoggedEfforts() != null ? epic.getLoggedEfforts() : 0);
                Integer newEpicLoggedEffort = Math.max(0, previousEpicLoggedEffort - previousTimeSheetLoggedTime + updatedTimeSheetLoggedTime);
                epic.setLoggedEfforts(newEpicLoggedEffort);
                epicRepository.save(epic);
            }
        }
    }

    public void validateTaskWithSprintAndModifyProperties(Task foundTaskDb, Task task, ArrayList<String> updateFieldsByUser, String accountIds) throws IllegalAccessException {
        // validates a task when a sprint id is added/ updated in a task.
        if (!Objects.equals(foundTaskDb.getSprintId(),task.getSprintId()) && Objects.equals(task.getTaskTypeId(),Constants.TaskTypes.CHILD_TASK)) {
            throw new IllegalAccessException("Child task without parent cannot be moved from sprint");
        }
        Optional<Sprint> optionalSprint = null;
        if (task.getSprintId() != null) {
            optionalSprint = sprintRepository.findById(task.getSprintId());
            if (optionalSprint.isEmpty()) {
                throw new IllegalStateException("Sprint not found");
            }
            if (task.getFkAccountIdAssigned() != null) {
                Set<EmailFirstLastAccountId> sprintMemberList = optionalSprint.get().getSprintMembers();
                if (sprintMemberList == null) {
                    sprintMemberList = new HashSet<>();
                }
                List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                        .map(EmailFirstLastAccountId::getAccountId)
                        .collect(Collectors.toList());
                if (!sprintMemberAccountIdList.contains(task.getFkAccountIdAssigned().getAccountId())) {
                    throw new IllegalAccessException("Assigned To user of Work Item is not part of selected sprint");
                }
            }

            if (Objects.equals(optionalSprint.get().getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && task.getFkAccountIdAssigned() == null) {
                throw new ValidationFailedException("Please provide Assign to before adding/updating Work Item in started sprint");
            }
        }
        if (task.getSprintId() != null && !Objects.equals(foundTaskDb.getSprintId(), task.getSprintId())) {
            Boolean isBacklog = false;
            if (Objects.equals(foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), task.getFkWorkflowTaskStatus().getWorkflowTaskStatus()) && Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                isBacklog = true;
            }
            validateTaskAndAddToSprint(task, accountIds);
            if (foundTaskDb.getSprintId() != null) {
                addSprintInPrevSprintList(foundTaskDb, task);
            }
            if (Objects.equals(optionalSprint.get().getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId()) && isBacklog) {
                updateFieldsByUser.add(Constants.TaskFields.WORKFLOW_TASK_STATUS);
            }
            return;
        }

        //validate sprint before starting the task
        if (foundTaskDb.getSprintId() != null) {
            List<Long> authorizedAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), List.of(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(), RoleEnum.TEAM_MANAGER_SPRINT.getRoleId()), true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
            Optional<Sprint> sprintDb = sprintRepository.findById(foundTaskDb.getSprintId());
            List<String> unauthorizedWorkflowStatusForReassignment = new ArrayList<>(List.of(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE));
            //verifying if user reassigning the task in sprint is project manager.
            if (updateFieldsByUser.contains("fkAccountIdAssigned") && !authorizedAccountIds.contains(task.getFkAccountIdLastUpdated().getAccountId()) && !unauthorizedWorkflowStatusForReassignment.contains(foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                throw new IllegalStateException("User do not have the necessary permissions to reassign a Work Item in this sprint. Please contact your manager for assistance.");
            }
            if (sprintDb.isPresent()) {
                Sprint sprint = sprintDb.get();
                for (String field : updateFieldsByUser) {
                    if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId())) {
                        if (Constants.nonEditableFieldsForTaskInNotStartedSprint.containsKey(field)) {
                            throw new IllegalStateException("User not authorized to update field " + Constants.nonEditableFieldsForTaskInNotStartedSprint.get(field) + " for a Work Item in not started sprint");
                        }
                        if (Constants.editableFieldsForTaskInNotStartedSprintForProjectManager.containsKey(field) && !authorizedAccountIds.contains(task.getFkAccountIdLastUpdated().getAccountId())) {
                            throw new IllegalStateException("User not authorized to update field " + Constants.editableFieldsForTaskInNotStartedSprintForProjectManager.get(field) + " for a Work Item in not started sprint, please contact your manager to update the provided field.");
                        }
                    }
                    if (Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())
                            && Constants.editableFieldsForTaskInStartedSprintForProjectManager.containsKey(field)
                            && !authorizedAccountIds.contains(task.getFkAccountIdLastUpdated().getAccountId())) {
                        throw new IllegalStateException("User not authorized to update field " + Constants.editableFieldsForTaskInNotStartedSprintForProjectManager.get(field) + " for a task in started sprint, please contact your manager to update the provided field.");
                    }
                    if (Objects.equals(field, "taskEstimate") && !sprint.getCanModifyEstimates() && !authorizedAccountIds.contains(task.getFkAccountIdLastUpdated().getAccountId())) {
                        throw new IllegalAccessException("Unauthorized access: You do not have permission to update the Work Item estimate in the specified sprint.");
                    }

                    if(Objects.equals(field,"taskExpStartDate")) {
                        if(task.getTaskExpStartDate()==null)
                            throw new IllegalStateException("Expected Start Date of the Work Item should be a valid date within a Sprint.");
                        if(task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate()) || !task.getTaskExpStartDate().isBefore(sprint.getSprintExpEndDate()))
                            throw new IllegalStateException("Expected Dates of the Work Item should not lie outside the Expected Dates of the Sprint.");
                    }

                    if(Objects.equals(field,"taskExpEndDate")) {
                        if(task.getTaskExpEndDate()==null)
                            throw new IllegalStateException("Expected End Date of the Work Item should be a valid date within a Sprint.");
                        if(task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate()) || !task.getTaskExpEndDate().isAfter(sprint.getSprintExpStartDate()))
                            throw new IllegalStateException("Expected Dates of the Work Item should not lie outside the Expected Dates of the Sprint.");
                    }
                    if(Objects.equals(field,"taskActStDate")) {
                        if(task.getTaskActStDate().isBefore(sprint.getSprintExpStartDate()) && (sprint.getSprintActStartDate() != null ? task.getTaskActStDate().isBefore(sprint.getSprintActStartDate()) : true))
                            throw new IllegalAccessException("Actual Start Date of the Work Item should be after the Start Date of the Sprint.");
                    }
                    if(Objects.equals(field,"taskActEndDate")) {
                        if(task.getTaskActEndDate().isBefore(sprint.getSprintExpStartDate()) && (sprint.getSprintActStartDate() != null ? task.getTaskActEndDate().isBefore(sprint.getSprintActStartDate()) : true))
                            throw new IllegalAccessException("Actual End Date of the Work Item should be after the Actual Start Date of the Sprint.");
                    }
                }
            }
        }

        if (foundTaskDb.getSprintId() != null && task.getSprintId() == null) {
            addSprintInPrevSprintList(foundTaskDb, task);
        }
    }

    public void validateTaskWithEpicAndModifyProperties(Task foundTaskDb, Task task, ArrayList<String> updateFieldsByUser, String accountIds) throws IllegalAccessException {
        if(Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && ((foundTaskDb.getFkEpicId() != null && task.getFkEpicId() == null) || (foundTaskDb.getFkEpicId() == null && task.getFkEpicId() != null) ||
                (task.getFkEpicId() != null && !Objects.equals(foundTaskDb.getFkEpicId().getEpicId(), task.getFkEpicId().getEpicId())))) {
            throw new IllegalAccessException("Child task can't be directly added to epic");
        }

        if(task.getFkEpicId() == null && foundTaskDb.getFkEpicId() == null) {
            return;
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);

        Epic epic = null;
        Epic epicDb = null;
        if (task.getFkEpicId() != null) {
            epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
            if (!epic.getTeamIdList().contains(task.getFkTeamId().getTeamId())) {
                throw new IllegalAccessException("Team is not part of selected epic");
            }
        }
        if (foundTaskDb.getFkEpicId() != null) {
            epicDb = epicRepository.findByEpicId(foundTaskDb.getFkEpicId().getEpicId());
        }

        if((task.getFkEpicId() != null && foundTaskDb.getFkEpicId() == null) || (task.getFkEpicId() == null && foundTaskDb.getFkEpicId() != null) ||
                !Objects.equals(task.getFkEpicId().getEpicId(), foundTaskDb.getFkEpicId().getEpicId())) {

            if (task.getFkEpicId() != null) {

                if (epic == null) {
                    throw new IllegalStateException("Epic is not valid");
                }
                Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epic.getFkOrgId().getOrgId(), true, accountIdList);
                if (userAccountId == null) {
                    throw new ValidationFailedException("User is not part of the organisation");
                }
                if (!epicService.validateUserRole(List.of(task.getFkTeamId().getTeamId()), userAccountId)) {
                    throw new ValidationFailedException("User doesn't have permission to add this Work Item in selected epic");
                }
                if (Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
                    throw new IllegalStateException("Work item can't be added in completed epic");
                }
                if (Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
                    throw new IllegalStateException("Work item can't be added in deleted epic");
                }
                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                    epicService.validateCompletedWorkItemDateWithEpic (task, epic);
                }
                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    throw new ValidationFailedException("Deleted work item can't be added in epic");
                }
                if (epic.getExpEndDateTime() != null) {
                    if (task.getSprintId() != null) {
                        if (task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime()) || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                            throw new ValidationFailedException("Work item is part of sprint and it's expected end date & time doesn't matched with epic's date & time");
                        }
                    }
                    if (task.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                        task.setTaskExpEndDate(epic.getExpEndDateTime());
                        task.setTaskExpEndTime(epic.getExpEndDateTime().toLocalTime());
                    }
                }
                if (epic.getExpStartDateTime() != null) {
                    if (task.getSprintId() != null) {
                        if (task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                            throw new ValidationFailedException("Work Item is part of sprint and it's expected start date & time doesn't matched with epic's date & time");
                        }
                    }
                    if (task.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && task.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                        task.setTaskExpStartDate(epic.getExpStartDateTime());
                        task.setTaskExpStartTime(epic.getExpStartDateTime().toLocalTime());
                    }
                }
            }
            if (foundTaskDb.getFkEpicId() != null) {

                Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epicDb.getFkOrgId().getOrgId(), true, accountIdList);
                if (userAccountId == null) {
                    throw new ValidationFailedException("User is not part of the organisation");
                }
                if (!epicService.validateUserRole(List.of(foundTaskDb.getFkTeamId().getTeamId()), userAccountId)) {
                    throw new ValidationFailedException("User doesn't have permission to remove this Work Item from epic");
                }
                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    throw new ValidationFailedException("Deleted work item can't be removed from epic");
                }
                if (Objects.equals(foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                    throw new ValidationFailedException("Completed work item can't be removed or moved in epic");
                }
                if (Objects.equals(epicDb.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId())) {
                    throw new IllegalStateException("Work item can't be removed from deleted epic");
                }
            }
        }
        else {
            if (epic != null && epic.getExpEndDateTime() != null) {
                if (task.getSprintId() != null) {
                    if (task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime()) || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime()))) {
                        throw new ValidationFailedException("Work item is part of sprint and it's expected end date & time doesn't matched with epic's date & time");
                    }
                }
                if (task.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                    task.setTaskExpEndDate(epic.getExpEndDateTime());
                    task.setTaskExpEndTime(epic.getExpEndDateTime().toLocalTime());
                }
            }
            if (epic != null && epic.getExpStartDateTime() != null) {
                if (task.getSprintId() != null) {
                    if (task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                        throw new ValidationFailedException("Work Item is part of sprint and it's expected start date & time doesn't matched with epic's date & time");
                    }
                }
                if (task.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && task.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                    task.setTaskExpStartDate(epic.getExpStartDateTime());
                    task.setTaskExpStartTime(epic.getExpStartDateTime().toLocalTime());
                }
            }
        }
        if (task.getFkEpicId() != null && (
                (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)
                        && !Objects.equals(foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE))
                        ||
                        (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)
                                && !Objects.equals(foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE))
        )) {
            List<Integer> statusIdList = new ArrayList<>();
            statusIdList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
            statusIdList.add(Constants.EpicStatusEnum.STATUS_IN_PROGRESS.getWorkflowEpicStatusId());
            if(!statusIdList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                throw new ValidationFailedException("This Work Item can't be started or completed as the Epic is in " + epic.getFkWorkflowEpicStatus().getWorkflowEpicStatus() + " state");
            }
        }
    }

    public byte[] convertToCsv(List<TaskMaster> taskMasterList) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(
                    new String[]{
                            "Task Number",
                            "Task Title",
                            "Task Description",
                            "Task Workflow Type",
                            "Team Name",
                            "Workflow Task Status Type",
                            "Full Name",
                            "Email",
                            "Task Priority",
                            "WorkItem Type Id",
                            "WorkItem Type",
                            "Created Date Time",
                            "Last Updated Date Time",
                            "Task Expected Start Date",
                            "Task Actual Start Date",
                            "Task Expected End Date",
                            "Task Actual End Date",
                            "New Effort Date",
                            "Task Progress System"
                    }
            );
            // Assuming TaskMaster has appropriate getters for fields
            taskMasterList.forEach(taskMaster ->
                    csvWriter.writeNext(
                            new String[]{
                                    String.valueOf(taskMaster.getTaskNumber()),
                                    taskMaster.getTaskTitle(),
                                    taskMaster.getTaskDesc(),
                                    taskMaster.getTaskWorkflowType(),
                                    taskMaster.getTeamName(),
                                    taskMaster.getWorkflowTaskStatusType(),
                                    taskMaster.getFullName(),
                                    taskMaster.getEmail(),
                                    taskMaster.getTaskPriority(),
                                    String.valueOf(taskMaster.getTaskTypeId()),
                                    String.valueOf(taskMaster.getTaskType()),
                                    String.valueOf(taskMaster.getCreatedDateTime()),
                                    String.valueOf(taskMaster.getLastUpdatedDateTime()),
                                    String.valueOf(taskMaster.getTaskExpStartDate()),
                                    String.valueOf(taskMaster.getTaskActStDate()),
                                    String.valueOf(taskMaster.getTaskExpEndDate()),
                                    String.valueOf(taskMaster.getTaskActEndDate()),
                                    String.valueOf(taskMaster.getNewEffortDate()),
                                    String.valueOf(taskMaster.getTaskProgressSystem())
                            }
                    )
            );

            csvWriter.flush();
            return stringWriter.toString().getBytes();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void validateCompletedTaskUpdate (Task task, List<String> updateFieldsByUser) throws NoSuchFieldException {
        Task foundTask = taskRepository.findById(task.getTaskId()).orElseThrow(TaskNotFoundException::new);
        //validating if user is not reassigning a deleted task
        if (updateFieldsByUser.contains("fkAccountIdAssigned") && Objects.equals(foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new IllegalStateException("User not allowed to reassign a deleted task.");
        }
        if (Objects.equals(foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
            List<String> accountFields = List.of("fkAccountIdAssigned", "fkAccountIdMentor1", "fkAccountIdMentor2", "fkAccountIdObserver1", "fkAccountIdObserver2");
            List<String> otherFkFields = List.of("fkTeamId", "fkOrgId");
            String failedField = null;
            for (String field : updateFieldsByUser) {
                if (accountFields.contains(field)) {
                    switch (field) {
                        case "fkAccountIdAssigned":
                            if(!Objects.equals(task.getFkAccountIdAssigned().getAccountId(), foundTask.getFkAccountIdAssigned().getAccountId())) failedField = field; break;
                        case "fkAccountIdMentor1":
                            if(!Objects.equals(task.getFkAccountIdMentor1().getAccountId(), foundTask.getFkAccountIdMentor1()!=null ? foundTask.getFkAccountIdMentor1().getAccountId():null)) failedField = field; break;
                        case "fkAccountIdMentor2":
                            if(!Objects.equals(task.getFkAccountIdMentor2().getAccountId(), foundTask.getFkAccountIdMentor2()!=null ? foundTask.getFkAccountIdMentor2().getAccountId():null)) failedField = field; break;
                        case "fkAccountIdObserver1":
                            if(!Objects.equals(task.getFkAccountIdObserver1().getAccountId(), foundTask.getFkAccountIdObserver1()!=null?foundTask.getFkAccountIdObserver1().getAccountId():null)) failedField = field; break;
                        case "fkAccountIdObserver2":
                            if(!Objects.equals(task.getFkAccountIdObserver2().getAccountId(), foundTask.getFkAccountIdObserver2()!=null?foundTask.getFkAccountIdObserver2().getAccountId():null)) failedField = field; break;
                    }
                } else if (otherFkFields.contains(field)) {
                    switch (field) {
                        case "fkTeamId":
                            if(!Objects.equals(task.getFkTeamId().getTeamId(), foundTask.getFkTeamId().getTeamId())) failedField = field; break;
                        case "fkOrgId":
                            if(!Objects.equals(task.getFkOrgId().getOrgId(), foundTask.getFkOrgId().getOrgId())) failedField = field; break;
                    }
                } else if (Constants.nonEditableFieldsForCompletedTask.containsKey(field)) {
                    failedField = Constants.nonEditableFieldsForCompletedTask.get(field); break;
                }
            }
            if(failedField != null) throw new ValidationFailedException("Cannot update field " + failedField + " for a completed Work Item. This field is non-editable once the Work Item is marked as completed");
        }
    }


//    List<TaskMaster> searchTasks(Long userId, List<Long> accountIds, String searchTerm, String timeZone) {
//        String nativeQuery = "SELECT * FROM tse.task WHERE (search_vector @@ plainto_tsquery(:searchTerm)) " +
//                "AND SIMILARITY(task_title, :searchTerm) > :searchMultiplier " +
//                "AND org_id = :orgId AND team_id = :teamId " +
//                "ORDER BY SIMILARITY(task_title, :searchTerm) DESC";
//
//        List<TeamOrgBuAndProjectName> TeamOrgBuAndProjectName = teamRepository.getAllMyTeamsForUserId(userId, Constants.EntityTypes.TEAM);
//
//        Query query = entityManager.createNativeQuery(nativeQuery, Task.class);
//        query.setParameter("searchTerm", searchTerm);
//        query.setParameter("teamId", teamId);
//
//        return query.getResultList();
//    }


//    public List<TaskMaster> searchTasksByFTS(Long userId, String searchTerm) {
//        // Construct the base query
//        String nativeQuery = "SELECT * FROM tse.task WHERE (search_vector @@ plainto_tsquery(:searchTerm)) ";
//
//        List<TeamOrgBuAndProjectName> teamOrgAndProjectName = teamRepository.getAllMyTeamsForUserId(userId, Constants.EntityTypes.TEAM);
//        List<Long> teamIds = teamOrgAndProjectName.stream().map(TeamOrgBuAndProjectName::getTeamId).collect(Collectors.toList());
//
////        // Add dynamic filtering for accountIds and teamIds
////        if (accountIds != null && !accountIds.isEmpty()) {
////            String accountIdsString = accountIds.stream()
////                    .map(String::valueOf)
////                    .collect(Collectors.joining(", "));
////            nativeQuery += " AND fk_account_id IN (" + accountIdsString + ") ";
////        }
//
//        if (teamIds != null && !teamIds.isEmpty()) {
//            String teamIdsString = teamIds.stream()
//                    .map(String::valueOf)
//                    .collect(Collectors.joining(", "));
//            nativeQuery += " AND team_id IN (" + teamIdsString + ") ";
//        }
//
//        // Order by FTS rank
//        nativeQuery += " ORDER BY ts_rank(search_vector, plainto_tsquery(:searchTerm)) DESC";
//
//        // Create and set parameters for the query
//        Query query = entityManager.createNativeQuery(nativeQuery, Task.class);
//        query.setParameter("searchTerm", searchTerm);
//
//        List<Task> tasks = query.getResultList();
//        List<TaskMaster> taskMasters = new ArrayList<>();
//        for(Task task: tasks) {
//            TaskMaster taskMaster = new TaskMaster();
//            BeanUtils.copyProperties(task, taskMaster);
//            taskMasters.add(taskMaster);
//        }
//        return taskMasters;
//    }

    /**  * Searches for tasks by Full Text Search (FTS) and similarity, filtering by team IDs associated with the provided account IDs.
     * Tasks are only searched in Teams where the account has team task view action or account doesn't have team task view action but the
     * task is assigned to the user. It prioritizes results based on FTS relevance and then by trigram similarity score for the specified
     * search term. Based on the search task request, it combines all filters to create a native query.
    */
    public List<SearchTaskResponse> searchTasksByFTSAndTrigram(List<Long> accountIds, SearchTaskRequest request) {
        List<Long> allTeamIdsOfUser = accessDomainRepository.findDistinctEntityIdsByActiveAccountIds(Constants.EntityTypes.TEAM, accountIds);
        // gets the team where the user has team task view action
        List<Long> teamTaskViewTeamIds = accessDomainRepository.findTeamIdsByAccountIdsAndActionId(accountIds, Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW);

//        String nativeQuery = "SELECT * FROM tse.task " +
//                "WHERE (search_vector @@ plainto_tsquery(:searchTerm) OR SIMILARITY(task_title, :searchTerm) > 0.2) " +
//                "AND team_id IN (:allTeamIdsLong) ";

        String nativeQuery = "SELECT * FROM tse.task WHERE team_id IN (:allTeamIdsLong)";

        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            nativeQuery += " AND (search_vector @@ plainto_tsquery(:searchTerm) " +
                    "OR SIMILARITY(task_title, :searchTerm) > :similarityThreshold " +
                    "OR SIMILARITY(CAST(task_number AS TEXT), :searchTerm) > :similarityThreshold" +
                    ")";
        }

        nativeQuery = applyFiltersToSearchQuery(request, nativeQuery);
        nativeQuery = applyOrderByCondition(request, nativeQuery);

//                 nativeQuery += "ORDER BY " +
//                "CASE WHEN search_vector @@ plainto_tsquery(:searchTerm) THEN " +
//                "ts_rank(search_vector, plainto_tsquery(:searchTerm)) ELSE 0 END DESC, " +
//                "SIMILARITY(task_title, :searchTerm) DESC";

        Query query = entityManager.createNativeQuery(nativeQuery, Task.class);
        setParametersInSearchQuery(request, query, allTeamIdsOfUser);
        List<Task> tasks = query.getResultList();
        // Remove tasks that do not satisfy the team task view condition
        tasks.removeIf(task -> !teamTaskViewTeamIds.contains(task.getFkTeamId().getTeamId()) &&
                (task.getFkAccountIdAssigned() == null || !accountIds.contains(task.getFkAccountIdAssigned().getAccountId())));
        return createSearchTaskResponse(tasks);
    }

    /** based on the search request, this method appends filter criteria string to the native query*/
    private String applyFiltersToSearchQuery(SearchTaskRequest request, String nativeQuery) {
        List<Integer> workflowStatusIds = new ArrayList<>();

        if (request.getOrgId() != null) {
            nativeQuery = nativeQuery + "AND org_id = :orgId ";
        }
        if (request.getBuId() != null) {
            nativeQuery += "AND bu_id = :buId ";
        }
        if (request.getProjectId() != null) {
            nativeQuery += "AND project_id = :projectId ";
        }
        if (request.getWorkflowStatuses() != null && !request.getWorkflowStatuses().isEmpty()) {
            List<String> workflowStatuses = request.getWorkflowStatuses();
            workflowStatusIds = workFlowTaskStatusRepository.findWorkflowTaskStatusIdByWorkflowTaskStatusIn(workflowStatuses);
            request.setWorkflowStatusIds(workflowStatusIds);
            nativeQuery += "AND workflow_task_status_id IN (:workflowStatusIds) ";
        }
        if (request.getAccountIdAssigned() != null) {
            nativeQuery += "AND account_id_assigned = :accountIdAssigned ";
        }
        if (request.getTaskProgressSystems() != null && !request.getTaskProgressSystems().isEmpty()) {
            nativeQuery += "AND task_progress_system IN (:taskProgressSystems) ";
        }
        if (request.getSprintId() != null) {
            nativeQuery += "AND sprint_id = :sprintId ";
        }
        if (request.getTaskNumbersToSkip() != null && !request.getTaskNumbersToSkip().isEmpty()) {
            nativeQuery += "AND task_number NOT IN (:taskNumbersToSkip) ";
        }
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            nativeQuery += "AND task_id IN (SELECT task_id FROM tse.task_label WHERE label_id IN (:labelIds)) ";
        }
        if (request.getTeamId() != null) {
            nativeQuery += "AND team_id = :teamId ";
        }
        if (request.getCurrentlyScheduledTaskIndicator() != null) {
            nativeQuery += "AND currently_scheduled_task_indicator = :currentlyScheduledTaskIndicator ";
        }
        if (request.getCurrentActivityIndicator() != null) {
            nativeQuery += "AND current_activity_indicator = :currentActivityIndicator ";
        }
        if (request.getEpicId() != null) {
            nativeQuery += "AND epic_id = :epicId ";
        }
        if(request.getIsStarred() != null && request.getIsStarred())
        {
            nativeQuery += " AND is_starred = true";
            if (request.getStarredBy() != null && !request.getStarredBy().isEmpty()) {
                nativeQuery += " AND account_id_starred_by IN (:starredBy)";
            }
        }
        return nativeQuery;
    }

    /** This method sets the required parameters in the native query*/
    private void setParametersInSearchQuery(SearchTaskRequest request, Query query, List<Long> teamIds) {
//        query.setParameter("searchTerm", request.getSearchTerm());
        query.setParameter("allTeamIdsLong", teamIds);

        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            query.setParameter("similarityThreshold", similarityThreshold);
        }

        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            query.setParameter("searchTerm", request.getSearchTerm());
        }

        if (request.getOrgId() != null) {
            query.setParameter("orgId", request.getOrgId());
        }
        if (request.getBuId() != null) {
            query.setParameter("buId", request.getBuId());
        }
        if (request.getProjectId() != null) {
            query.setParameter("projectId", request.getProjectId());
        }
        if (request.getWorkflowStatuses() != null && !request.getWorkflowStatuses().isEmpty()) {
            query.setParameter("workflowStatusIds", request.getWorkflowStatusIds());
        }
        if (request.getAccountIdAssigned() != null) {
            query.setParameter("accountIdAssigned", request.getAccountIdAssigned());
        }
        if (request.getTaskProgressSystems() != null && !request.getTaskProgressSystems().isEmpty()) {
            query.setParameter("taskProgressSystems", request.getTaskProgressSystems());
        }
        if (request.getSprintId() != null) {
            query.setParameter("sprintId", request.getSprintId());
        }
        if (request.getTaskNumbersToSkip() != null && !request.getTaskNumbersToSkip().isEmpty()) {
            query.setParameter("taskNumbersToSkip", request.getTaskNumbersToSkip());
        }
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            query.setParameter("labelIds", request.getLabelIds());
        }
        if (request.getTeamId() != null) {
            query.setParameter("teamId", request.getTeamId());
        }
        if (request.getCurrentlyScheduledTaskIndicator() != null) {
            query.setParameter("currentlyScheduledTaskIndicator", request.getCurrentlyScheduledTaskIndicator());
        }
        if (request.getCurrentActivityIndicator() != null) {
            query.setParameter("currentActivityIndicator", request.getCurrentActivityIndicator());
        }
        if (request.getEpicId() != null) {
            query.setParameter("epicId", request.getEpicId());
        }
        if (Boolean.TRUE.equals(request.getIsStarred())
                && request.getStarredBy() != null
                && !request.getStarredBy().isEmpty()) {
            query.setParameter("starredBy", request.getStarredBy());
        }
    }

    private String applyOrderByCondition(SearchTaskRequest request, String nativeQuery) {
        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            nativeQuery += " ORDER BY " +
                    "CASE WHEN search_vector @@ plainto_tsquery(:searchTerm) THEN " +
                    "ts_rank(search_vector, plainto_tsquery(:searchTerm)) ELSE 0 END DESC, " +
                    "GREATEST(" +
                    "   SIMILARITY(task_title, :searchTerm), " +
                    "   SIMILARITY(CAST(task_number AS TEXT), :searchTerm)" +
                    ") DESC";
        } else {
            nativeQuery += " ORDER BY\n" +
                    "    CASE\n" +
                    "        WHEN task_priority = 'P1' THEN 1\n" +
                    "        WHEN task_priority = 'P2' THEN 2\n" +
                    "        WHEN task_priority = 'P3' THEN 3\n" +
                    "        WHEN task_priority = 'P4' THEN 4\n" +
                    "        WHEN task_priority = 'P5' THEN 5\n" +
                    "        ELSE 6\n" +
                    "    END,\n" +
                    "    created_date_time DESC\n";
        }
        return nativeQuery;
    }

    /** create a search Task Response from a list of tasks */
    private List<SearchTaskResponse> createSearchTaskResponse (List<Task> tasks) {
        List<SearchTaskResponse> responseList = new ArrayList<>();
        for (Task task : tasks) {
            SearchTaskResponse searchTaskResponse = new SearchTaskResponse();
            BeanUtils.copyProperties(task, searchTaskResponse);
            searchTaskResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            searchTaskResponse.setTeamId(task.getFkTeamId().getTeamId());
            searchTaskResponse.setTeamName(task.getFkTeamId().getTeamName());
            //need to add changes here
            if(Objects.equals(task.getIsStarred(),true)) {
                UserAccount user=task.getFkAccountIdStarredBy();
                if(user != null && user.getAccountId() != null)
                {
                    EmailFirstLastAccountIdIsActive starredByUser=userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                    searchTaskResponse.setStarredBy(starredByUser);
                }
            }
            searchTaskResponse.setIsStarred(task.getIsStarred());
            if (task.getFkAccountIdAssigned() != null) {
                String fullName = task.getFkAccountIdAssigned().getFkUserId().getFirstName() + " " + task.getFkAccountIdAssigned().getFkUserId().getLastName();
                String email = task.getFkAccountIdAssigned().getEmail();
                searchTaskResponse.setEmail(email);
                searchTaskResponse.setFullName(fullName);
            }
            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PERSONAL_TASK)) {
                searchTaskResponse.setIsPersonalTask(true);
            }
            responseList.add(searchTaskResponse);
        }
        return responseList;
    }


    public void checkAndSetIsSprintChangedIndicator(Task task) {
        Task foundTaskDb = taskRepository.findByTaskId(task.getTaskId());
        if (!Objects.equals(task.getSprintId(), foundTaskDb.getSprintId()) && foundTaskDb.getSprintId() != null && task.getSprintId() != null) {
            task.setIsSprintChanged(true);
        }
    }

    /**
     * Populating Task model from quickCreateBugRequest
     */
    public Task populateBugTask(QuickCreateBugRequest quickCreateBugRequest, String accountIds) {
        Task task = new Task();
        task.setTaskTypeId(Constants.TaskTypes.BUG_TASK);
        task.setTaskTitle(quickCreateBugRequest.getTaskTitle());
        //if desc is null or empty populate title as desc
        task.setTaskDesc((quickCreateBugRequest.getTaskDesc() != null && !quickCreateBugRequest.getTaskDesc().isEmpty()) ? quickCreateBugRequest.getTaskDesc() : quickCreateBugRequest.getTaskTitle());
        if (quickCreateBugRequest.getTaskPriority() != null)
            task.setTaskPriority(quickCreateBugRequest.getTaskPriority().substring(0, 2));
        task.setTaskWorkflowId(quickCreateBugRequest.getTaskWorkFlowId());
        //checking for workflow ids
        if (!Constants.DEFAULT_WORKFLOW.contains(task.getTaskWorkflowId())) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new WorkflowTypeDoesNotExistException());
            logger.error("Task Workflow type failed: task workflowId = " + task.getTaskWorkflowId() + " is not allowed for a task.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new WorkflowTypeDoesNotExistException();
        }
        task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatusId(quickCreateBugRequest.getTaskWorkFlowStatus()));
        if (quickCreateBugRequest.getExpStartDateTime() != null) {
            task.setTaskExpStartDate(quickCreateBugRequest.getExpStartDateTime());
            if (quickCreateBugRequest.getExpStartTime() != null)
                task.setTaskExpStartTime(quickCreateBugRequest.getExpStartDateTime().toLocalTime());
            else task.setTaskExpStartTime(null);
        }
        if (quickCreateBugRequest.getExpEndDateTime() != null) {
            task.setTaskExpEndDate(quickCreateBugRequest.getExpEndDateTime());
            if (quickCreateBugRequest.getExpEndTime() != null)
                task.setTaskExpEndTime(quickCreateBugRequest.getExpEndDateTime().toLocalTime());
            else task.setTaskExpEndTime(null);
        }
        task.setFkAccountId(userAccountRepository.findByAccountIdAndIsActive(Long.valueOf(accountIds), true));
        if (quickCreateBugRequest.getAssignTo() != null) {
            task.setFkAccountIdAssigned(userAccountRepository.findByAccountIdAndIsActive(quickCreateBugRequest.getAssignTo(), true));
            task.setFkAccountIdAssignee(task.getFkAccountId());
        }
        if (quickCreateBugRequest.getBugReportedBy() != null) {
            task.setFkAccountIdBugReportedBy(userAccountRepository.findByAccountIdAndIsActive(quickCreateBugRequest.getBugReportedBy(), true));
        }
        if (quickCreateBugRequest.getLabelsToAdd() != null) {
            task.setLabelsToAdd(quickCreateBugRequest.getLabelsToAdd());
        }
        task.setTaskEstimate(quickCreateBugRequest.getEstimate());
        task.setCurrentActivityIndicator(0);
        task.setSprintId(quickCreateBugRequest.getSprintId());
        task.setFkTeamId(teamRepository.findByTeamId(quickCreateBugRequest.getTeamId()));
        task.setBuId(task.getFkTeamId().getFkProjectId().getBuId());
        task.setFkProjectId(task.getFkTeamId().getFkProjectId());
        task.setFkOrgId(task.getFkTeamId().getFkOrgId());
        task.setFkAccountIdCreator(task.getFkAccountId());
        task.setFkAccountIdLastUpdated(task.getFkAccountId());
        task.setEnvironmentId(quickCreateBugRequest.getEnvironmentId());
        task.setSeverityId(quickCreateBugRequest.getSeverityId());
        task.setPlaceOfIdentification(quickCreateBugRequest.getPlaceOfIdentification());
        task.setCustomerImpact(quickCreateBugRequest.getCustomerImpact());
        task.setLinkedTaskList(quickCreateBugRequest.getLinkedTaskList());
        if (quickCreateBugRequest.getDependentTaskDetailRequestList() != null) {
            task.setDependentTaskDetailRequestList(quickCreateBugRequest.getDependentTaskDetailRequestList());
        }
        if(quickCreateBugRequest.getReferenceWorkItemId() != null && !quickCreateBugRequest.getReferenceWorkItemId().isEmpty()) {
            task.setReferenceWorkItemId(quickCreateBugRequest.getReferenceWorkItemId());
        }
        task.setFkEpicId(epicRepository.findByEpicId(quickCreateBugRequest.getEpicId()));
        task.setIsBug(true);
        task.setRcaId(quickCreateBugRequest.getRcaId());
        if (quickCreateBugRequest.getReleaseVersionName() != null) {
            task.setReleaseVersionName(quickCreateBugRequest.getReleaseVersionName());
        }
        if (quickCreateBugRequest.getIsStarred() != null && quickCreateBugRequest.getIsStarred()) {
            task.setIsStarred(true);
            task.setFkAccountIdStarredBy(task.getFkAccountId());
        }
        return task;
    }

    public void normalizeTaskEfforts (Task task, Task taskDb) {
        task.setTotalEffort(taskDb.getTotalEffort());
        task.setRecordedTaskEffort(taskDb.getRecordedTaskEffort());
        task.setRecordedEffort(taskDb.getRecordedEffort());
    }

    // methods related to recurring tasks

    /** method to create recurring tasks based on the provided details of the task and recurrence filters in the
     * recurrence schedule request. It returns a list of tasks numbers successfully created.
     */
    public String createRecurringTasks(RecurrenceTaskDTO request, Long accountIdOfUser, String timeZone) {
        if (request.getTaskTitle() != null) {
            request.setTaskTitle(request.getTaskTitle().trim());
        }
        if (request.getTaskDesc() != null) {
            request.setTaskDesc(request.getTaskDesc().trim());
        }
        validateRecurrenceRequest(request, timeZone);
        List<LocalDate[]> generatedExpectedDates = recurrenceService.generateRecurringDates(request.getRecurrenceSchedule(), accountIdOfUser);
        StringBuilder createdTasksString = new StringBuilder("Recurring Work Items created on business days: ");
        String createdTasksOnHolidays = " and work items on non-business days is/are: ";
        List<String> createdTaskOnHolidayList = new LinkedList<>();
        List<String> createdTaskList = new LinkedList<>();
        boolean hasTasksOnNonBusinessDays = false, hasAnyTaskAddedOnHoliday = false;
        Task savedCheckTask;
        HolidayOffDayInfo holidayOffDayInfo = recurrenceService.getHolidaysAndOffDaysBasedOnEntityPreference(accountIdOfUser); // could be accountId of the assignedTo as well

        if (generatedExpectedDates != null && !generatedExpectedDates.isEmpty()) {
            Task checkTask = populateRecurringTask(request, accountIdOfUser, generatedExpectedDates.get(0)[0], generatedExpectedDates.get(0)[1],  timeZone);
            validateRecurringTask(checkTask, timeZone);
            if (!checkTask.getTaskExpStartDate().isBefore(checkTask.getTaskExpEndDate())) {
                throw new ValidationFailedException("Expected End Time should be greater than Expected Start Time");
            }
            validateExpDateTimeWithEstimate (checkTask);
            Task taskUpdated = taskService.initializeTaskNumberSetProperties(checkTask);
            savedCheckTask = addTaskInTaskTable(taskUpdated, timeZone);
            createdTaskList.add(savedCheckTask.getTaskNumber());
            if (!recurrenceService.isBusinessDay(generatedExpectedDates.get(0)[0], holidayOffDayInfo)) {
                createdTaskOnHolidayList.add(savedCheckTask.getTaskNumber());
                hasTasksOnNonBusinessDays = true;
                hasAnyTaskAddedOnHoliday = true;
            }
        } else {
            return "No Dates generated for the given filters";
        }

        int generatedExpectedDatesSize = generatedExpectedDates.size();
        for (int i = 1; i < generatedExpectedDatesSize; i++) {
            LocalDate[] expectedDates = generatedExpectedDates.get(i);
            Task task = populateRecurringTask(request, accountIdOfUser, expectedDates[0], expectedDates[1], timeZone);
            Task taskUpdated = taskService.initializeTaskNumberSetProperties(task);
            Task taskAdd = addRecurringTaskToTaskTable(taskUpdated, timeZone);

            createdTaskList.add(taskAdd.getTaskNumber());
            if (!recurrenceService.isBusinessDay(generatedExpectedDates.get(i)[0], holidayOffDayInfo)) {
                createdTaskOnHolidayList.add(taskAdd.getTaskNumber());
                hasTasksOnNonBusinessDays = true;
            }
        }
        if(createdTaskList.size() < 5){
            createdTaskList.forEach(taskNumber -> createdTasksString.append(taskNumber).append(", "));
            createdTasksString.deleteCharAt(createdTasksString.length() -2);
        } else {
            createdTasksString.append(createdTaskList.get(0)).append(", ").append(createdTaskList.get(1)).append(", ").append(createdTaskList.get(2)).append(" ..... ").append(createdTaskList.get(createdTaskList.size()-1));
        }
        if(hasTasksOnNonBusinessDays) {
            createdTasksString.append(createdTasksOnHolidays);
            if (createdTaskOnHolidayList.size() < 5) {
                createdTaskOnHolidayList.forEach(taskNumber -> createdTasksString.append(taskNumber).append(", "));
                createdTasksString.deleteCharAt(createdTasksString.length() -2);
            } else {
                createdTasksString.append(createdTaskOnHolidayList.get(0)).append(", ").append(createdTaskOnHolidayList.get(1)).append(", ").append(createdTaskOnHolidayList.get(2)).append(" ..... ").append(createdTaskOnHolidayList.get(createdTaskOnHolidayList.size() - 1));
            }
        }

        List<HashMap<String, String>> payload = notificationService.createRecurringTaskNotification(savedCheckTask, createdTasksString.toString());
        sendPushNotification(payload);
        return createdTasksString.toString();
    }


    /** validate check task of a recurring task */
    public void validateRecurringTask(Task task, String timeZone) {
        validateAccounts(task);
        if (!validateTaskByWorkflowStatus(task))
            throw new ValidationFailedException("Invalid Request: Please check workflow condition");
        ArrayList<Integer> roleIds = accessDomainService.getEffectiveRolesByAccountId(task.getFkAccountId().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());

        boolean result;
        if (task.getFkAccountIdAssigned() == null || task.getFkAccountIdAssigned().getAccountId() == null) {
            result = actionService.isInAction(roleIds, Constants.Task_Add);
        } else if (task.getFkAccountIdAssigned().getAccountId().equals(task.getFkAccountId().getAccountId())) {
            result = actionService.isInAction(roleIds, Constants.Self_Created_Self_Assignment);
        } else {
            result = actionService.isInAction(roleIds, Constants.Self_Created_Assignment_Others);
        }

        if (!result) throw new ValidationFailedException("You don't have required role to update the Work Item");
    }

    /** validate the recurrence request conditions */
    private void validateRecurrenceRequest(RecurrenceTaskDTO request, String timeZone) {
        // only allowed values for workflow status is not started and backlog
        if (!Objects.equals(request.getTaskWorkFlowStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)
                && !Objects.equals(request.getTaskWorkFlowStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
            throw new IllegalArgumentException("Only allowed values for workflow status are 'Backlog' and 'Not-Started'");
        }

        // estimate is mandatory for not started
        if (Objects.equals(request.getTaskWorkFlowStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) && request.getTaskEstimate() == null) {
            throw new ValidationFailedException("Estimate must be provided for Work Item in not started workflow status");
        }

        // validate the given workflow Id is valid
        if (!Constants.DEFAULT_WORKFLOW.contains(request.getTaskWorkFlowId())) {
            throw new WorkflowTypeDoesNotExistException();
        }

        // Validate either startDate & endDate are provided or numberOfOccurrences, but not both
        boolean isDateRangeProvided = request.getRecurrenceSchedule().getStartDate() != null && request.getRecurrenceSchedule().getEndDate() != null;
        boolean isNumberOfOccurrencesProvided = request.getRecurrenceSchedule().getNumberOfOccurrences() != null;

        if (isDateRangeProvided == isNumberOfOccurrencesProvided) {
            throw new ValidationFailedException("Either 'start date and end date' or 'number of occurrences' must be provided, but not both");
        }

        // Validate selectedDate or startDate is greater than the current date
        LocalDate relevantDate = request.getRecurrenceSchedule().getSelectedDate() != null ? request.getRecurrenceSchedule().getSelectedDate() : request.getRecurrenceSchedule().getStartDate();
        if (relevantDate != null && !relevantDate.isAfter(DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now().minusDays(1), timeZone).toLocalDate())) {
            throw new ValidationFailedException("Selected date or Start date must be greater than the current date");
        }

        // Additional validation for recurDays based on recurrenceType
        if (request.getRecurrenceSchedule().getRecurDays() != null && !request.getRecurrenceSchedule().getRecurDays().isEmpty()) {
            switch (request.getRecurrenceSchedule().getRecurrenceType()) {
                case WEEKLY:
                    RecurrenceScheduleDTO.validateWeeklyRecurDays(request.getRecurrenceSchedule().getRecurDays());
                    break;
                case MONTHLY:
                    RecurrenceScheduleDTO.validateMonthlyRecurDays(request.getRecurrenceSchedule().getRecurDays());
                    break;
                case YEARLY:
                    RecurrenceScheduleDTO.validateYearlyRecurDays(request.getRecurrenceSchedule().getRecurDays());
                    break;
            }
        }

    }

    /** populate Task Object from the Recurrence Task request */
    public Task populateRecurringTask(RecurrenceTaskDTO recurringTaskRequest, Long accountIdOfUser, LocalDate taskExpStartDate, LocalDate taskExpEndDate, String timeZone) {
        Task task = new Task();
        // ToDo: We have task type id field in the RecurrenceTaskDTO -- later on we will include the logic for the same
        task.setTaskTypeId(Constants.TaskTypes.TASK);

        task.setTaskTitle(recurringTaskRequest.getTaskTitle());
        //if desc is null or empty populate desc same as title
        task.setTaskDesc((recurringTaskRequest.getTaskDesc() != null && !recurringTaskRequest.getTaskDesc().isEmpty()) ? recurringTaskRequest.getTaskDesc() : recurringTaskRequest.getTaskTitle());

        if (recurringTaskRequest.getTaskPriority() != null)
            task.setTaskPriority(recurringTaskRequest.getTaskPriority().substring(0, 2));

        task.setTaskWorkflowId(recurringTaskRequest.getTaskWorkFlowId());
        task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(recurringTaskRequest.getTaskWorkFlowStatus(), recurringTaskRequest.getTaskWorkFlowId()));

        LocalDateTime taskExpStartDateTime = DateTimeUtils.convertUserDateToServerTimezone(
                LocalDateTime.of(taskExpStartDate, recurringTaskRequest.getTaskExpStartTime()), timeZone);
        LocalDateTime taskExpEndDateTime = DateTimeUtils.convertUserDateToServerTimezone(
                LocalDateTime.of(taskExpEndDate, recurringTaskRequest.getTaskExpEndTime()), timeZone);

        task.setTaskExpStartDate(taskExpStartDateTime);
        task.setTaskExpEndDate(taskExpEndDateTime);
        task.setTaskExpStartTime(taskExpStartDateTime.toLocalTime());
        task.setTaskExpEndTime(taskExpEndDateTime.toLocalTime());

        task.setFkAccountId(userAccountRepository.findByAccountId(accountIdOfUser));
        if (recurringTaskRequest.getAssignedToAccountId() != null) {
            task.setFkAccountIdAssigned(userAccountRepository.findByAccountId(recurringTaskRequest.getAssignedToAccountId()));
            task.setFkAccountIdAssignee(task.getFkAccountId());
        }
        if (recurringTaskRequest.getLabelsToAdd() != null) {
            task.setLabelsToAdd(recurringTaskRequest.getLabelsToAdd());
        }
        task.setTaskEstimate(recurringTaskRequest.getTaskEstimate());
        task.setCurrentActivityIndicator(0);
        task.setFkTeamId(teamRepository.findByTeamId(recurringTaskRequest.getTeamId()));
        task.setBuId(task.getFkTeamId().getFkProjectId().getBuId());
        task.setFkProjectId(task.getFkTeamId().getFkProjectId());
        task.setFkOrgId(task.getFkTeamId().getFkOrgId());
        task.setFkAccountIdCreator(task.getFkAccountId());
        task.setFkAccountIdLastUpdated(task.getFkAccountId());
        task.setIsBug(task.getIsBug());
        setTaskStateByWorkflowTaskStatus(task);
        updateCurrentlyScheduledTaskIndicatorForTask(task);
        return task;
    }

    public Task addRecurringTaskToTaskTable(Task task, String timeZone) {
        addLabelToTask(task);
        Task taskAdded = taskRepository.save(task);

        if (!Constants.WorkFlowStatusIds.BACKLOG.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
            List<String> updateFields = new ArrayList<>();
            taskHistoryService.addTaskHistoryOnSystemUpdate(task);
            statsService.computeAndUpdateStatForAddTask(taskAdded);
            updateFields.add("taskProgressSystem");
            updateFields.add("taskProgressSystemLastUpdated");
            Task tempTask = new Task();
            BeanUtils.copyProperties(taskAdded, tempTask);
            tempTask.setVersion(tempTask.getVersion() + 1);
            taskHistoryMetadataService.addTaskHistoryMetadataBySystemUpdate(updateFields, tempTask);
        }

        int noOfAudit = 1;
        if (task.getFkAccountIdAssigned() != null && task.getFkAccountIdAssigned().getAccountId() != null) {
            noOfAudit = 2;
        }
        if (noOfAudit == 1) {
            Audit auditAdd = auditRepository.save(auditService.createAudit(taskAdded, 1, null, null));
        } else {
            Audit auditAdd1 = auditRepository.save(auditService.createAudit(taskAdded, 1, null, null));
            Audit auditAdd2 = auditRepository.save(auditService.createAudit(taskAdded, 2, null, null));
        }
        return taskAdded;
    }

    public void addImmediateAttentionAlert (Task task, String timeZone) {
        AlertRequest alertRequest = new AlertRequest();
        alertRequest.setAlertTitle(task.getTaskNumber() + " need attention from you.");
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(task.getFkAccountIdLastUpdated() != null ? task.getFkAccountIdLastUpdated().getAccountId() : task.getFkAccountId().getAccountId(), true);
        alertRequest.setAlertReason(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName() + " needs your help in" + Constants.taskTypeMessages.get(task.getTaskTypeId()) + " " + task.getTaskNumber());
        User user = userRepository.findByPrimaryEmail(task.getImmediateAttentionFrom());
        List<Long> accountIdList = userAccountRepository.findAccountIdByFkUserIdUserIdAndIsActive(user, true);
        Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM,task.getFkTeamId().getTeamId(),accountIdList, true);
        alertRequest.setAccountIdReceiver(accountId);
        alertRequest.setAssociatedTaskNumber(task.getTaskNumber());
        alertRequest.setAssociatedTaskId(task.getTaskId());
        alertRequest.setAccountIdSender(userAccount.getAccountId());
        alertRequest.setAlertType(Constants.AlertTypeEnum.TASK.getType());
        alertRequest.setTeamId(task.getFkTeamId().getTeamId());
        alertRequest.setOrgId(task.getFkOrgId().getOrgId());
        alertRequest.setProjectId(task.getFkProjectId().getProjectId());
        alertService.addAlert(alertRequest, userAccount.getAccountId().toString(), timeZone);
    }

    public void validateAndNormalizeUpdateTaskRequest(Task task, Task taskDb, String timeZone) throws ValidationFailedException {
        normalizeImmediateAttention(task);
        normalizeTaskEfforts(task, taskDb);
        setDefaultExpTime(task);
        validateAccounts(task);
        convertTaskAllUserDateAndTimeInToServerTimeZone(task, timeZone);
        if (!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(),
                taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                throw new ValidationFailedException("Work flow status of Parent task/Bug can't be changed directly!!");
            }
        }
        if(task.getTaskActStDate()!=null && task.getTaskActStDate().isAfter(LocalDateTime.now()))
            throw new ValidationFailedException("Actual Start Date and Time cannot be after Current Date and Time");

        //Checking to see if child expDates are within parent expDates
        if(Objects.equals(task.getTaskTypeId(),Constants.TaskTypes.PARENT_TASK)) {
            List<Task> childTasks = taskRepository.findByParentTaskId(task.getTaskId());
            List<String> tasksWithExceededDates = new ArrayList<>();
            for( Task childTask: childTasks) {
                if(Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(),Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) || Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(),Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE))
                    continue;
                if ((childTask.getTaskExpStartDate() != null && task.getTaskExpStartDate()!=null && childTask.getTaskExpStartDate().isBefore(task.getTaskExpStartDate()))
                        || (childTask.getTaskExpEndDate() != null && task.getTaskExpEndDate()!=null && childTask.getTaskExpEndDate().isAfter(task.getTaskExpEndDate()))) {
                    tasksWithExceededDates.add(String.valueOf(childTask.getTaskNumber()));
                }
                String errorMessage = "Error: ";
                if (!tasksWithExceededDates.isEmpty()) {
                    String exceededTaskNumbers = String.join(", ", tasksWithExceededDates);
                    errorMessage += "the following sub-tasks [" + exceededTaskNumbers + "] have dates that fall outside the expected range of their parent task.";
                    throw new SubTaskDetailsMissingException(errorMessage);
                }
            }
        }
        // Normalise Reported by field
        if (task.getFkAccountIdBugReportedBy() != null) {
            if (task.getFkAccountIdBugReportedBy().getAccountId() == null || !(Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.BUG_TASK) ||
                    (taskDb.getIsBug() && Objects.equals(taskDb.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)))) {
                task.setFkAccountIdBugReportedBy(null);
            }
        }

        if (task.getReleaseVersionName() != null &&
                !Objects.equals(task.getReleaseVersionName(), taskDb.getReleaseVersionName()) &&
                (task.getLabelsToAdd() == null || !task.getLabelsToAdd().contains(task.getReleaseVersionName()))) {

            List<String> updatedLabels = task.getLabelsToAdd() != null
                    ? new ArrayList<>(task.getLabelsToAdd())
                    : new ArrayList<>();

            updatedLabels.add(task.getReleaseVersionName());

            task.setLabelsToAdd(updatedLabels);
        }

        modifyTaskPriority(task);
        checkAndSetIsSprintChangedIndicator(task);
        updateCurrentlyScheduledTaskIndicatorForTask(task);
        normalizeMentorAndObserver(task);
        normalizeStarringField (task, taskDb);
        validateEstimateForParentTask(task, taskDb);
        validateRcaFieldsNotEditableForCompletedBug(task, taskDb);
        validateAndNormalizeRca (task);
        validateOtherTaskProperties(task, taskDb);
    }

    private void normalizeStarringField(Task task, Task taskDb) {
        task.setIsStarred(taskDb.getIsStarred());
        task.setFkAccountIdStarredBy(taskDb.getFkAccountIdStarredBy());
    }

    private void validateOtherTaskProperties(Task task, Task taskDb) {
        if (!Objects.equals(taskDb.getIsBug(), task.getIsBug())) {
            throw new ValidationFailedException("Invalid Request: Incorrect field modification");
        }
        if (task.getTaskActStDate() != null && (task.getFkAccountIdAssigned() == null || task.getFkAccountIdAssigned().getAccountId() == null)) {
            throw new ValidationFailedException("Assign to can't be null if actual start date time is present");
        }
    }

    private void validateEstimateForParentTask(Task task, Task taskDb) {
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK) && !Objects.equals(taskDb.getTaskEstimate(), task.getTaskEstimate())) {
            throw new ValidationFailedException("Modification of estimate of parent task is not allowed");
        }
    }

    private void normalizeImmediateAttention(Task task) {
        if (task.getImmediateAttention()!=null) {
            if(task.getImmediateAttention() == 0) {
                task.setImmediateAttentionFrom(null);
                task.setImmediateAttentionReason(null);
            } else if (task.getImmediateAttention() == 1 && task.getImmediateAttentionFrom() == null) {
                throw new ValidationFailedException("Immediate attention from must be provided if immediate attention is true");
            } else if (task.getImmediateAttention() == 1 && task.getImmediateAttentionReason() == null) {
                throw new ValidationFailedException("Immediate attention reason must be provided if immediate attention is true");
            }
        }
    }

    private void normalizeMentorAndObserver(Task task) {
        if (task.getFkAccountIdMentor2() != null && task.getFkAccountIdMentor1() == null) {
            task.setFkAccountIdMentor1(task.getFkAccountIdMentor2());
            task.setFkAccountIdMentor2(null);
        }

        if (task.getFkAccountIdObserver2() != null && task.getFkAccountIdObserver1() == null) {
            task.setFkAccountIdObserver1(task.getFkAccountIdObserver2());
            task.setFkAccountIdObserver2(null);
        }

        if (task.getFkAccountIdObserver1() != null && task.getFkAccountIdObserver2() != null && Objects.equals(task.getFkAccountIdObserver1(), task.getFkAccountIdObserver2())) {
            task.setFkAccountIdObserver2(null);
        }

        if (task.getFkAccountIdMentor1() != null && task.getFkAccountIdMentor2() != null && Objects.equals(task.getFkAccountIdMentor1(), task.getFkAccountIdMentor2())) {
            task.setFkAccountIdMentor2(null);
        }
    }

    public Task findTaskByTaskNumberAndTeamId(String taskNumber, Long teamId) {
        Long taskIdentifier = getTaskIdentifierFromTaskNumber(taskNumber);
        return taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, teamId);
    }

    public void validateTaskAndAddToSprint(Task task, String accountIds) throws IllegalAccessException {
        // validates a task when a sprint id is added/ updated in a task.
        if (task.getSprintId() != null) {
            Optional<Sprint> sprint = sprintRepository.findById(task.getSprintId());
            if (sprint.isEmpty()) {
                throw new EntityNotFoundException("Sprint not found");
            }
            if (!sprintService.hasModifySprintPermission(accountIds, sprint.get().getEntityId(), sprint.get().getEntityTypeId())) {
                throw new IllegalAccessException("Unauthorized: User does not have permission to add Work Item in sprint");
            }
            if (task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() == null) {
                WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusIdAndFkWorkFlowTypeWorkflowTypeId(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), task.getTaskWorkflowId());
                if (workFlowTaskStatus != null) {
                    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
                } else {
                    throw new ValidationFailedException("Please provide a valid workflow status");
                }
            }
            if(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)
                    && task.getTaskEstimate()!=null && task.getTaskExpStartDate().isAfter(sprint.get().getSprintExpEndDate().minusMinutes(task.getTaskEstimate()))){
                throw new ValidationFailedException("The work item will not be added to sprint because Expected end date will be changed to Sprint Expected end date which will be less than expected start date of Work Item.");
            }
            if (Objects.equals(sprint.get().getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())) {
                if (task.getTaskEstimate() == null || task.getTaskEstimate().equals(0)) {
                    throw new ValidationFailedException("Please provide Work Item estimate to add Work Item in started sprint");
                }
                if (task.getFkAccountIdAssigned() == null) {
                    throw new ValidationFailedException("Please assign Work Item to someone before adding Work Item in started sprint");
                }
                if (task.getTaskPriority() == null) {
                    throw new ValidationFailedException("Please provide Work Item priority to add Work Item in started sprint");
                }
                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                    WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, task.getTaskWorkflowId());
                    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
                    task.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
                }
            }
            if (Objects.equals(sprint.get().getSprintStatus(), Constants.SprintStatusEnum.COMPLETED.getSprintStatusId())) {
                throw new IllegalStateException("User not allowed to add Work Item in a completed sprint");
            }

            if (Objects.equals(sprint.get().getSprintStatus(), Constants.SprintStatusEnum.DELETED.getSprintStatusId())) {
                throw new IllegalStateException("User not allowed to add Work Item in a deleted sprint");
            }
            if (task.getFkAccountIdAssigned() != null) {
                Set<EmailFirstLastAccountId> sprintMemberList = sprint.get().getSprintMembers();
                if (sprintMemberList == null) {
                    sprintMemberList = new HashSet<>();
                }
                List<Long> sprintMemberAccountIdList = sprintMemberList.stream()
                        .map(EmailFirstLastAccountId::getAccountId)
                        .collect(Collectors.toList());
                if (!sprintMemberAccountIdList.contains(task.getFkAccountIdAssigned().getAccountId())) {
                    throw new IllegalAccessException("Assigned To user of Work Item is not part of selected sprint");
                }
            }
            String message = "";
            HashMap<Long, Boolean> isVisited = new HashMap<>();

            //Task to last task in the chain
            HashMap<Long, Long> processedTasks = new HashMap<>();

            //Last task in the chain to total estimate of that chain
            HashMap<Long, Integer> taskEstimates = new HashMap<>();

            //Last task in the chain to end date of the last processed task of that chain
            HashMap<Long, LocalDateTime> taskEstimatesLastDate = new HashMap<>();

            HashMap<Long, LocalDateTime> parentExpStartDate = new HashMap<>();
            HashMap<Long, LocalDateTime> parentExpEndDate = new HashMap<>();

            List<Task> sprint2TasksToProcess = new ArrayList<>();

            if (task.getDependencyIds() != null && !task.getDependencyIds().isEmpty() && sprint.isPresent()) {
                if(task.getTaskExpStartDate()==null || task.getTaskExpEndDate()==null)
                    throw new ValidationException("Please add Expected Dates to the Work Item");

                if (((task.getTaskExpStartDate().isBefore(sprint.get().getSprintExpStartDate()) && task.getTaskActStDate() == null) || task.getTaskExpEndDate().isAfter(sprint.get().getSprintExpEndDate())))
                    throw new IllegalStateException("Work items with dependencies should have dates within sprint dates while adding to a sprint.");
            }
            message = sprintService.validateSprintConditionAndModifyTaskProperties(task, sprint.get(), null);
        }
    }

    /** creates a drill down for estimates of parent task*/
    public EstimateDrillDownResponse getEstimateDrilldown(Long parentTaskId, String accountIds) {
        Task parentTask = taskRepository.findById(parentTaskId).orElseThrow(EntityNotFoundException::new);

        if(!taskService.isTaskViewAllowed(parentTask, accountIds)) {
            throw new UnauthorizedException("User not authorised");
        }

        if (!parentTask.getTaskTypeId().equals(Constants.TaskTypes.PARENT_TASK)) {
            throw new ValidationFailedException("Task is not a parent task");
        }

        List<Task> childTaskList = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
        List<Task> deletedChildTaskList = taskRepository.findByTaskIdIn(parentTask.getDeletedChildTaskIds());

        Map<String, Integer> currentChildTasks = new HashMap<>();
        Map<String, Map<String, Integer>> deletedChildTasks = new HashMap<>();
        int totalParentTaskEstimate = 0;

        for (Task child : childTaskList) {
            int childEstimate = child.getTaskEstimate() != null ? child.getTaskEstimate() : 0;
            currentChildTasks.put(child.getTaskNumber(), childEstimate);
            totalParentTaskEstimate += childEstimate;
        }

        for (Task deletedChild : deletedChildTaskList) {
            Map<String, Integer> deletedChildDetails = new HashMap<>();
            deletedChildDetails.put("estimateIncludedInParentTask", deletedChild.getEarnedTimeTask() != null ? deletedChild.getEarnedTimeTask() : 0);
            deletedChildDetails.put("userPerceivedPercentageCompleted", deletedChild.getUserPerceivedPercentageTaskCompleted() != null ? deletedChild.getUserPerceivedPercentageTaskCompleted() : 0);
            totalParentTaskEstimate += (deletedChild.getEarnedTimeTask() != null ? deletedChild.getEarnedTimeTask() : 0);

            deletedChildDetails.put("taskEstimate", deletedChild.getTaskEstimate());
            deletedChildTasks.put(deletedChild.getTaskNumber(), deletedChildDetails);
        }

        return new EstimateDrillDownResponse(totalParentTaskEstimate, currentChildTasks, deletedChildTasks);
    }

    //    If a taskId of a given sub-task is provided this api will return the status of all sub tasks of that parent
    public Map<String, String> getAllSubTasksStatus(Long subTaskId, String accountIds) {
        Task subTask = taskRepository.findById(subTaskId).orElseThrow(EntityNotFoundException::new);

        if (!subTask.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
            throw new IllegalArgumentException("Given Work Item is not a child task");
        }

        Task parentTask = taskRepository.findById(subTask.getParentTaskId()).orElseThrow(EntityNotFoundException::new);

        if (!taskService.isTaskViewAllowed(parentTask, accountIds)) {
            throw new UnauthorizedException("User not authorised to view this information");
        }

        List<Task> childTaskList = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
        List<Task> deletedChildTaskList = taskRepository.findByTaskIdIn(parentTask.getDeletedChildTaskIds());

        Map<String, String> responseMap = new HashMap<>();
        for (Task child : childTaskList) {
            responseMap.put(child.getTaskNumber(), child.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        }

        for (Task child : deletedChildTaskList) {
            responseMap.put(child.getTaskNumber(), child.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        }

        return responseMap;
    }


    public void setTimeOfDateTimeInTaskField (UpdateTaskRequest updateTaskRequest) {
        if (updateTaskRequest.getTaskExpStartDate() != null && updateTaskRequest.getTaskExpStartTime() == null) {
            updateTaskRequest.setTaskExpStartTime(updateTaskRequest.getTaskExpStartDate().toLocalTime());
        }
        if (updateTaskRequest.getTaskExpEndDate() != null && updateTaskRequest.getTaskExpEndTime() == null) {
            updateTaskRequest.setTaskExpEndTime(updateTaskRequest.getTaskExpEndDate().toLocalTime());
        }
        if (updateTaskRequest.getTaskActStDate() != null && updateTaskRequest.getTaskActStTime() == null) {
            updateTaskRequest.setTaskActStTime(updateTaskRequest.getTaskActStDate().toLocalTime());
        }
        if (updateTaskRequest.getTaskActEndDate() != null && updateTaskRequest.getTaskActEndTime() == null) {
            updateTaskRequest.setTaskActEndTime(updateTaskRequest.getTaskActEndDate().toLocalTime());
        }
    }

    public void updateTaskFields (UpdateTaskFieldsRequest updateTaskFieldsRequest, String accountIds, String timeZone, String email, String uri, Long userId, String jwtToken) throws NoSuchFieldException, IllegalAccessException {

        List<UpdateTaskRequest> updateTaskRequests = updateTaskFieldsRequest.getUpdateTaskRequestList();
        List<Integer> teamAdminRoleList = new ArrayList<>(Constants.TEAM_ADMIN_ROLE);
        for (UpdateTaskRequest updateTaskRequest : updateTaskRequests) {
            Task foundTask = findTaskByTaskId(updateTaskRequest.getTaskId());
            setTimeOfDateTimeInTaskField (updateTaskRequest);
            Task task = new Task();
            if (foundTask == null) {
                throw new TaskNotFoundException();
            }
            BeanUtils.copyProperties(foundTask, task);
            convertTaskAllServerDateAndTimeInToUserTimeZone(task, timeZone);
            CommonUtils.copyNonNullProperties(updateTaskRequest, task);
            if (updateTaskRequest.getWorkflowTaskStatusId() != null) {
                if (updateTaskRequest.getTaskWorkflowId() == null) {
                    throw new IllegalAccessException("Please provide workflow type to update workflow status for Work Item " + task.getTaskNumber());
                }
                WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusIdAndFkWorkFlowTypeWorkflowTypeId(updateTaskRequest.getWorkflowTaskStatusId(), updateTaskRequest.getTaskWorkflowId());
                if (workFlowTaskStatus == null) {
                    throw new ValidationFailedException("Please provide a valid workflow status for Work Item " + task.getTaskNumber());
                }

                task.setFkWorkflowTaskStatus(workFlowTaskStatus);
            }

            if (task.getIncreaseInUserPerceivedPercentageTaskCompleted() != null && task.getUserPerceivedPercentageTaskCompleted() == null) {
                task.setUserPerceivedPercentageTaskCompleted(task.getIncreaseInUserPerceivedPercentageTaskCompleted() + (foundTask.getUserPerceivedPercentageTaskCompleted() != null ? foundTask.getUserPerceivedPercentageTaskCompleted() : 0));
            }
            UserAccount lastUpdatedAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(task.getFkOrgId().getOrgId(), userId, true);
            if (lastUpdatedAccount == null) throw new EntityNotFoundException("Last updating user details not found for Work Item " + task.getTaskNumber());
            task.setFkAccountIdLastUpdated(lastUpdatedAccount);
            if (updateTaskRequest.getAccountIdAssigned() != null) {
                UserAccount assignedAccount = userAccountRepository.findByAccountIdAndIsActive(updateTaskRequest.getAccountIdAssigned(), true);
                if (assignedAccount == null) throw new EntityNotFoundException("Assigned to user details not found for Work Item " + task.getTaskNumber());
                if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdNotIn(Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), updateTaskRequest.getAccountIdAssigned(), true, teamAdminRoleList)) {
                    throw new ValidationFailedException("Assigned To of work item " + task.getTaskNumber() + " doesn't have any team role");
                }
                task.setFkAccountIdAssigned(assignedAccount);
            }
            if (updateTaskRequest.getAccountIdAssignee() != null) {
                UserAccount assigneeAccount = userAccountRepository.findByAccountIdAndIsActive(updateTaskRequest.getAccountIdAssignee(), true);
                if (assigneeAccount == null) throw new EntityNotFoundException("Assignee user details not found for Work Item " + task.getTaskNumber());
                task.setFkAccountIdAssignee(assigneeAccount);
            }
            if (updateTaskRequest.getRespondentAccountId() != null) {
                UserAccount respondentAccount = userAccountRepository.findByAccountIdAndIsActive(updateTaskRequest.getRespondentAccountId(), true);
                if (respondentAccount == null) throw new EntityNotFoundException("Respondent user details not found for Work Item " + task.getTaskNumber());
                task.setFkAccountIdRespondent(respondentAccount);
            }
            if (foundTask.getTaskActStDate() != null && (task.getTaskActStDate() != null || task.getTaskExpStartDate() != null) && !(Objects.equals(foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE) && Objects.equals(foundTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE))) {
                Task foundTaskCopy = new Task();
                BeanUtils.copyProperties(foundTask, foundTaskCopy);
                convertTaskAllServerDateAndTimeInToUserTimeZone(foundTaskCopy, timeZone);
                task.setTaskActStDate(foundTaskCopy.getTaskActStDate());
                task.setTaskActStTime(foundTaskCopy.getTaskActStTime());
                task.setTaskExpStartDate(foundTaskCopy.getTaskExpStartDate());
                task.setTaskExpStartTime(foundTaskCopy.getTaskExpStartTime());
            }
            updateTask(task, updateTaskRequest.getTaskId(), true, lastUpdatedAccount.getAccountId().toString(), timeZone, email, uri, jwtToken);
        }
    }

    public Task updateTask (Task task, Long taskId, Boolean useSystemDerivedForChild, String accountIds, String timeZone, String email, String uri, String jwtToken) throws NoSuchFieldException, IllegalAccessException {
        long startTime = System.currentTimeMillis();
        Task taskDb = null;
        boolean isTaskValidatedByWorkflowStatus, isTaskValidatedForDateAndTimePairs, isTaskValidated, isTaskDataValidated, isTaskTypeValidated, isTitleOrDescChanged;
        try {
            taskDb = taskRepository.findByTaskId(taskId);
            if (taskDb == null) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskNotFoundException());
                logger.error(uri + " API: " + "Work Item with taskId = " + taskId + "does not exist", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new TaskNotFoundException();
            }
            isTitleOrDescChanged = (task.getTaskTitle().equals(taskDb.getTaskTitle())) || (!task.getTaskDesc().equals(taskDb.getTaskDesc()));
            validateAndNormalizeUpdateTaskRequest(task, taskDb, timeZone);
            modifyEarnedTimeInTaskAndTimeSheetForEstimateChange(task);
            updateImmediateAttentionIndicatorForTask(task);

            if(task.getNewEffortTracks() == null || task.getNewEffortTracks().isEmpty()){
                task.setUserPerceivedPercentageTaskCompleted(taskDb.getUserPerceivedPercentageTaskCompleted());
            }
            List<Integer> workItemStatusIdList = Constants.WORK_ITEM_STATUS_ID_LIST;
            if (task.getFkWorkflowTaskStatus() != null && !workItemStatusIdList.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                throw new IllegalAccessException("Work Item status is not valid");
            }
            validateTaskByTaskTypeForUpdateTask(task, useSystemDerivedForChild);
            // If the task is a parent task and useSystemDerivedForChild is true, synchronize child tasks workflow status with the parent task
            if(Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                synchronizeChildTaskDetailsWithParent(task, timeZone, null);
                synchronizeChildTaskDetailsWhenParentBlocked(task, timeZone);
            }

            // Method for Reference Work Item
            validateTaskReferenceWorkItem(task);
            validateBugTaskRelationWithReferenceWorkItemId(task);
            validateDependencyWithReferenceWorkItemId(task);

            if(task.getIsBug() != null && task.getIsBug()){
                validateAndSaveLinkedTaskInfo(task);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            if (e instanceof TaskEstimateException) {
                throw e;
            } else {
                if (e instanceof ValidationFailedException) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(uri + " API: " + "Something went wrong: Not able to create a Work Item for username = " + email+ "ERROR :" +e.getMessage() + "Caught Exception" + e ,  new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw e;
                } else {
                    if (e instanceof DateAndTimePairFailedException) {
                        throw e;
                    } else {
                        if (e instanceof WorkflowTypeDoesNotExistException) {
                            throw e;
                        } else {
                            if (e instanceof TaskNotFoundException) {
                                throw e;
                            } else {
                                if (e instanceof WorkflowTaskStatusFailedException) {
                                    throw e;
                                } else {
                                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                    logger.error(uri + " API: " + "Something went wrong: Not able to update a Work Item for the username = " + email + " ,    " + "taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                    ThreadContext.clearMap();
                                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                                }
                            }
                        }
                    }
                }
            }
        }

        if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(),Constants.WorkFlowTaskStatusConstants.STATUS_DELETE)) {
            throw new ValidationFailedException("Could not update Deleted Work Item " + task.getTaskNumber() + ".");
        }
        if (Objects.equals(task.getIsBallparkEstimate(), Constants.BooleanValues.BOOLEAN_FALSE)) {
            task.setIsBallparkEstimate(null);
        }

        if (Objects.equals(taskDb.getFkTeamId().getTeamId(), task.getFkTeamId().getTeamId())) {
            boolean isSelfAssigned = false;

            if (taskDb.getFkAccountIdAssigned() != null) {
                if (Objects.equals(task.getFkAccountIdLastUpdated().getAccountId(), taskDb.getFkAccountIdAssigned().getAccountId())) {
                    isSelfAssigned = true;
                }
            }

            ArrayList<String> updateFieldsByUser = taskService.getFieldsToUpdate(task, taskId);
            validateTaskWithSprintAndModifyProperties(taskDb, task, updateFieldsByUser, accountIds);
            validateTaskWithEpicAndModifyProperties(taskDb, task, updateFieldsByUser, accountIds);

            validateExpDateTimeWithEstimate (task);
            //Validation for reference meeting
            if(Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) &&
                !Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                    validationForCompletedChildTask(task);
                }
                else {
                    if(isActiveReferenceMeetingPresent(task)) {
                        boolean isRequiredToSend = task.getMeetingNotificationSentTime() == null || task.getMeetingNotificationSentTime().plusHours(Constants.ReferenceMeetingDialogBox.RESEND_NOTIFICATION_COOLDOWN_TIME).isAfter(LocalDateTime.now());
                        throw new ReferenceMeetingException("Cannot complete the task as there is/are active Reference Meeting/s. Please choose one of the Two options Available", isRequiredToSend);
                    }
                }
            }

            if (!Objects.equals(taskDb.getTaskExpStartDate(), task.getTaskExpStartDate()) || !Objects.equals(taskDb.getTaskExpEndDate(), task.getTaskExpEndDate())) {
                if (task.getSprintId() != null && task.getSprintId().equals(taskDb.getSprintId()) && isMeetingDateRangeIsOutsideOfReferenceTask(task)) {
                    throw new ValidationFailedException("Date range of reference meeting is not in between Expected date range of Work item " + task.getTaskNumber());
                }
            }

            updateFieldsByUser.remove("commentId");
            updateFieldsByUser.remove("fkTeamId");
            updateFieldsByUser.remove("fkOrgId");
            updateFieldsByUser.remove("fkProjectId"); // essential update but will be changed later
            updateFieldsByUser.remove("fkAccountIdCreator");
            updateFieldsByUser.remove("fkAccountIdAssignee");
            updateFieldsByUser.remove("fkAccountIdLastUpdated");
            updateFieldsByUser.remove("fkAccountId"); // accountId to be replaced by accountIdLastUpdated
            updateFieldsByUser.remove("newEffortTracks");
            updateFieldsByUser.remove("attachments");
            updateFieldsByUser.remove("systemDerivedEndTs");
            updateFieldsByUser.remove("childTaskIds");
            updateFieldsByUser.remove("deletedChildTaskIds");
            updateFieldsByUser.remove("childTaskList");
            updateFieldsByUser.remove("parentTaskResponse");
            updateFieldsByUser.remove("linkedTaskList");
            updateFieldsByUser.remove("bugTaskRelation");
            updateFieldsByUser.remove("dependentTaskDetailResponseList");
            updateFieldsByUser.remove("referenceWorkItemList");
            updateFieldsByUser.remove("labels");
            updateFieldsByUser.remove("dependencyIds");
            updateFieldsByUser.remove("estimateTimeLogEvaluation");
            updateFieldsByUser.remove("taskCompletionImpact");
            updateFieldsByUser.remove("isSprintChanged");
            updateFieldsByUser.remove("prevSprints");
            updateFieldsByUser.remove("isBug");
            updateFieldsByUser.remove("meetingEffortPreferenceId");
            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("updateFieldsByUser-----------" + updateFieldsByUser);
            }

            ArrayList<String> finalBasicUpdate = new ArrayList<>();
            ArrayList<String> finalEssentialUpdate = new ArrayList<>();
            Integer noOfAudit = finalEssentialUpdate.size();
            ArrayList<String> finalNotUpdate = new ArrayList<>();

            for (String updateFieldByUser : updateFieldsByUser) {
                if (tableColumnsTypeService.getDbBasicFields().contains(updateFieldByUser)) {
                    finalBasicUpdate.add(updateFieldByUser);
                } else {
                    if (tableColumnsTypeService.getDbEssentialFields().contains(updateFieldByUser)) {
                        finalEssentialUpdate.add(updateFieldByUser);
                    } else {
                        finalNotUpdate.add(updateFieldByUser);
                    }
                }
            }

            List<Integer> roleIdList = accessDomainRepository.findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(task.getFkAccountIdLastUpdated().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
            if (roleIdList == null || roleIdList.isEmpty()) {
                throw new ValidationFailedException("You do not have any non admin role in the team of the task");
            }
            String roleNameOfUpdatedTaskUser = RoleEnum.getRoleNameById(roleIdList.get(0));

            if (finalNotUpdate.isEmpty()) {
                isTaskDataValidated = validateTaskWorkflowType(task);
                updateTimeSheetAndRecordedEffort(task, timeZone);
                isTaskValidatedByWorkflowStatus = validateTaskByWorkflowStatus(taskDb, task, accountIds);
                isTaskValidated = validateTaskForWorkflowStatus(taskDb, task);
                isTaskValidatedForDateAndTimePairs = validateAllDateAndTimeForPairs(task);
                if (!isTaskValidatedByWorkflowStatus) {
                    throw new ValidationFailedException("Workflow status is not valid");
                }
                if (!isTaskDataValidated || !isTaskValidatedForDateAndTimePairs || !isTaskValidated) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException("user not allowed to update the task"));
                    logger.error(uri + " API: " + "Forbidden: user not allowed to update the Work Item" + " ,    " + "username = " + email + " ,    " + "taskId = " + taskId, new Throwable(allStackTraces));
                    ThreadContext.clearMap();

                    throw new ForbiddenException("user not allowed to update the Work Item");
                }
                validateCompletedTaskUpdate(task, updateFieldsByUser);

                if (Objects.equals(taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) && !updateFieldsByUser.isEmpty()) {
                    throw new IllegalStateException("User not allowed to update a deleted Work Item.");
                }
//                updateTimeSheetAndRecordedEffort(task, timeZone);
                if (finalEssentialUpdate.isEmpty()) {

                    if (isSelfAssigned) {
                        boolean updateResult = taskService.isUpdateAllowed(Constants.UpdateTeam.Task_Basic_Update, task.getFkAccountIdLastUpdated().getAccountId(), task.getFkTeamId().getTeamId());

                        if (updateResult) {
                            try {
                                Task updatedTask = updateFieldsInTaskTable(task, taskId,timeZone,accountIds);
                                long estimatedTime = System.currentTimeMillis() - startTime;
                                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                                logger.info("Exited" + '"' + " updateTask" + '"' + " method because completed successfully...");
                                ThreadContext.clearMap();
                                Task taskResponse = new Task();
                                BeanUtils.copyProperties(updatedTask, taskResponse);
                                convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse,timeZone);
                                taskResponse.setComments(Collections.emptyList());
                                if(isTitleOrDescChanged) {
                                    aiMlService.sendWorkItemDetailOnCreationAndUpdating(task, false, Long.valueOf(accountIds), "Updating_Task", timeZone, jwtToken);
                                }
                                return taskResponse;
                            } catch (Exception e) {
                                e.printStackTrace();
                                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                logger.error(uri + " API: " + "Something went wrong: Not able to update Work Item number = " + task.getTaskNumber() + " for the username = " + email + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                ThreadContext.clearMap();
                                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                            }
                        } else {
                            String errorMessage = buildErrorMessageForNotAccessToUpdate (roleNameOfUpdatedTaskUser, taskDb.getTaskNumber(), finalBasicUpdate);
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException(errorMessage));
                            logger.error(uri + " API: " + errorMessage + " ,     " + "username = " + email + " ,   " + "taskId = " + taskId, new Throwable(allStackTraces));
                            ThreadContext.clearMap();

                            throw new ForbiddenException(errorMessage);
                        }
                    } else {
                        boolean updateResult = taskService.isUpdateAllowed(Constants.UpdateTeam.All_Task_Basic_Update, task.getFkAccountIdLastUpdated().getAccountId(), task.getFkTeamId().getTeamId());

                        if (updateResult) {
                            try {
                                Task updatedTask = updateFieldsInTaskTable(task, taskId,timeZone,accountIds);
                                long estimatedTime = System.currentTimeMillis() - startTime;
                                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                                logger.info("Exited" + '"' + " updateTask" + '"' + " method because task has been successfully updated ...");
                                ThreadContext.clearMap();
                                Task taskResponse = new Task();
                                BeanUtils.copyProperties(updatedTask, taskResponse);
                                convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse,timeZone);
                                taskResponse.setComments(Collections.emptyList());
                                if(isTitleOrDescChanged) {
                                    aiMlService.sendWorkItemDetailOnCreationAndUpdating(task, false, Long.valueOf(accountIds), "Updating_Task", timeZone, jwtToken);
                                }
                                return taskResponse;
                            } catch (Exception e) {
                                e.printStackTrace();
                                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                logger.error(uri + " API: " + "Something went wrong: Not able to update Work Item number = " + task.getTaskNumber() + " for the username = " + email + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                ThreadContext.clearMap();
                                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                            }
                        } else {
                            String errorMessage = buildErrorMessageForNotAccessToUpdate (roleNameOfUpdatedTaskUser, taskDb.getTaskNumber(), finalBasicUpdate);
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException(errorMessage));
                            logger.error(uri + " API: " + errorMessage + " ,     " + "username = " + email + " ,   " + "taskId = " + taskId, new Throwable(allStackTraces));
                            ThreadContext.clearMap();

                            throw new ForbiddenException(errorMessage);
                        }
                    }
                } else {
                    if (isSelfAssigned) {
                        boolean updateResult = taskService.isUpdateAllowed(Constants.UpdateTeam.Task_Essential_Update, task.getFkAccountIdLastUpdated().getAccountId(), task.getFkTeamId().getTeamId());
                        if (updateResult) {
                            try {
                                Task updatedTask = updateFieldsInTaskTable(task, taskId,timeZone,accountIds);
                                for (String essentialUpdate : finalEssentialUpdate) {
                                    Audit auditCreated = auditService.createAudit(updatedTask, noOfAudit, taskId, essentialUpdate);
                                    Audit auditAdd = auditRepository.save(auditCreated);
                                }
                                if (enableOpenfire) {
                                    new Thread(() -> {
                                        try {
                                            openFireService.updateChatGroupAndChatRoom(updateFieldsByUser, task);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                            logger.error(uri + " API: " + "Something went wrong: Not able to update chatroom/ chat group for Work Item# " + task.getTaskNumber() + " for the username = " + email + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                        }
                                    }).start();
                                }

                                long estimatedTime = System.currentTimeMillis() - startTime;
                                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                                logger.info("Exiting" + '"' + " updateTask" + '"' + " method because Work Item has been successfully updated ...");
                                ThreadContext.clearMap();
                                Task taskResponse = new Task();
                                BeanUtils.copyProperties(updatedTask, taskResponse);
                                convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse,timeZone);
                                taskResponse.setComments(Collections.emptyList());
                                if(isTitleOrDescChanged) {
                                    aiMlService.sendWorkItemDetailOnCreationAndUpdating(task, false, Long.valueOf(accountIds), "Updating_Task", timeZone, jwtToken);
                                }
                                return taskResponse;
                            } catch (Exception e) {
                                e.printStackTrace();
                                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                logger.error(uri + " API: " + "Something went wrong: Not able to update Work Item number = " + task.getTaskNumber() + " for the username = " + email + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                ThreadContext.clearMap();
                                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                            }
                        } else {
                            String errorMessage = buildErrorMessageForNotAccessToUpdate (roleNameOfUpdatedTaskUser, taskDb.getTaskNumber(), finalEssentialUpdate);
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException(errorMessage));
                            logger.error(uri + " API: " + errorMessage + " ,     " + "username = " + email + " ,   " + "taskId = " + taskId, new Throwable(allStackTraces));
                            ThreadContext.clearMap();

                            throw new ForbiddenException(errorMessage);
                        }
                    } else {
                        boolean updateResult = taskService.isUpdateAllowed(Constants.UpdateTeam.All_Task_Essential_Update, task.getFkAccountIdLastUpdated().getAccountId(), task.getFkTeamId().getTeamId());
                        // Checking update permission for Formal Team Lead Level 1 (role 9) when the task is not self-assigned.
                        boolean isUpdateAllowedForTeamLeadLevel1 = false;
                        if (finalEssentialUpdate.size() == 1 && updateFieldsByUser.contains("fkAccountIdAssigned")) {
                            if (accessDomainRepository.findFirstByEntityTypeIdAndEntityIdAndRoleIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), RoleEnum.FORMAL_TEAM_LEAD_LEVEL_1.getRoleId(), task.getFkAccountIdLastUpdated().getAccountId(), true) != null) {
                                isUpdateAllowedForTeamLeadLevel1 = true;
                            }
                        }
                        if (updateResult || isUpdateAllowedForTeamLeadLevel1) {
                            try {
                                Task updatedTask = updateFieldsInTaskTable(task, taskId, timeZone,accountIds);
                                for (String essentialUpdate : finalEssentialUpdate) {
                                    Audit auditCreated = auditService.createAudit(updatedTask, noOfAudit, taskId, essentialUpdate);
                                    Audit auditAdd = auditRepository.save(auditCreated);
                                }

                                if (enableOpenfire) {
                                    new Thread(() -> {
                                        try {
                                            openFireService.updateChatGroupAndChatRoom(updateFieldsByUser, task);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                            logger.error(uri + " API: " + "Something went wrong: Not able to update chatroom/ chat group for task# " + task.getTaskNumber() + " for the username = " + email + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                        }
                                    }).start();
                                }

                                long estimatedTime = System.currentTimeMillis() - startTime;
                                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                                logger.info("Exiting" + '"' + " updateTask" + '"' + " method because Work Item has been successfully updated ...");
                                ThreadContext.clearMap();
                                Task taskResponse = new Task();
                                BeanUtils.copyProperties(updatedTask, taskResponse);
                                convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse,timeZone);
                                taskResponse.setComments(Collections.emptyList());
                                if(isTitleOrDescChanged) {
                                    aiMlService.sendWorkItemDetailOnCreationAndUpdating(task, false, Long.valueOf(accountIds), "Updating_Task", timeZone, jwtToken);
                                }
                                return taskResponse;
                            } catch (Exception e) {
                                e.printStackTrace();
                                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                logger.error(uri + " API: " + "Something went wrong: Not able to update Work Item number = " + task.getTaskNumber() + " for the username = " + email + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                                ThreadContext.clearMap();
                                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                            }
                        } else {
                            String errorMessage = buildErrorMessageForNotAccessToUpdate (roleNameOfUpdatedTaskUser, taskDb.getTaskNumber(), finalEssentialUpdate);
                            String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException(errorMessage));
                            logger.error(uri + " API: " + errorMessage + " ,     " + "username = " + email + " ,   " + "taskId = " + taskId, new Throwable(allStackTraces));
                            ThreadContext.clearMap();

                            throw new ForbiddenException(errorMessage);
                        }
                    }
                }
            } else {
                String errorMessage = buildErrorMessageForNotAccessToUpdate (roleNameOfUpdatedTaskUser, taskDb.getTaskNumber(), finalNotUpdate);
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException(errorMessage));
                logger.error(uri + " API: " + errorMessage + " ,     " + "username = " + email + " ,   " + "taskId = " + taskId, new Throwable(allStackTraces));
                ThreadContext.clearMap();

                throw new ForbiddenException("This Work Item has been modified by another session/user.  Please reload and retry your update.");
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException("user does not have actions to update Work Item number = " + task.getTaskNumber()));
            logger.error(uri + " API: " + "Forbidden: user does not have actions to update Work Item number = " + task.getTaskNumber() + " ,     " + "username = " + email + " ,   " + "taskId = " + taskId, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ForbiddenException("user does not have actions to update Work Item number = " + task.getTaskNumber());
        }
    }

    public void askForStatus (TaskIdsRequest askForStatusRequest, String accountIds, String timeZone, Long userId) throws JsonProcessingException {
        List<Long> taskIds = askForStatusRequest.getTaskIds();
        List<Task> taskList = taskRepository.findByTaskIdIn(taskIds);
        HashMap<Long, List<Long>> authorizedAccountsMap = new HashMap<>();
        HashMap<Long, List<Integer>> entityPreferenceForAuthorizedRoles = new HashMap<>();
        HashMap<Long, Boolean> entityPreferenceForDelayedTask = new HashMap<>();
        List<String> taskNumberList = new ArrayList<>();
        for (Task task : taskList) {
            Long teamId = task.getFkTeamId().getTeamId();
            List<Long> accountIdList = Arrays.stream(accountIds.split(","))
                    .map(String::trim)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            if(task.getFkAccountIdAssigned()==null)
            {
                continue;
            }
            Long assignedId = task.getFkAccountIdAssigned().getAccountId();
            boolean exists = accountIdList.stream()
                    .anyMatch(id -> Objects.equals(id, assignedId));
            if (exists) {
              continue;
            }
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) || Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                continue;
            }
            UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(task.getFkOrgId().getOrgId(), userId, true);
            if (!entityPreferenceForAuthorizedRoles.containsKey(teamId)) {
                entityPreferenceService.updateEntityPreferenceFields(teamId, entityPreferenceForAuthorizedRoles, entityPreferenceForDelayedTask);
            }
            if (!authorizedAccountsMap.containsKey(teamId)) {
                List<Long> authorizedAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, entityPreferenceForAuthorizedRoles.get(teamId), true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
                authorizedAccountsMap.put(teamId, authorizedAccountIds);
            }
            if (!authorizedAccountsMap.get(teamId).contains(userAccount.getAccountId())) {
                String errorMessage = "User not authorized to ask for progress.";
                if (!taskNumberList.isEmpty()) {
                    errorMessage += " Query sent for Work Items with Work Item number " + String.join(", ", taskNumberList) + ".";
                }
                throw new ValidationFailedException(errorMessage);
            }
            String message = null;
            if (task.getTaskProgressSystem() != null) {
                switch (task.getTaskProgressSystem()) {
                    case DELAYED:
                        message = "Please Update the progress on Work Item " + task.getTaskNumber();
                        if (Boolean.TRUE.equals(entityPreferenceForDelayedTask.get(teamId))) {
                            message += " and provide the reason for the delay?";
                        }
                        break;

                    case NOTSTARTED:
                    case WATCHLIST:
                    case ONTRACK:
                        message = "Can you update the Work Item progress for Work Item number " + task.getTaskNumber();
                        break;

                    case LATE_COMPLETION:
                        message = "Could you please provide the reason of Late Completion for work item number " + task.getTaskNumber();
                        break;
                    default:
                        message = "Unknown status. Please update the Work Item "+task.getTaskNumber();
                }
            }
            if (message != null) {
                Comment comment = new Comment();
                comment.setComment(message);
                comment.setPostedByAccountId(userAccount.getAccountId());
                comment.setTask(task);
                comment.setCommentsTags(new String[]{"REQUEST"});

                commentService.addComment(comment, userAccount.getAccountId().toString(), timeZone);
                notificationService.createNotificationForStatusInquiryInTask(task, comment, timeZone);

                taskNumberList.add(task.getTaskNumber());
            }
        }
        }

    public void updateBugDetails(UpdateBugDetailsRequest updateBugDetailsRequest, String accountIds) {
        Long accountIdOfUser = Long.parseLong(accountIds);
        Task parentTask = taskRepository.findById(updateBugDetailsRequest.getTaskId()).orElseThrow(EntityNotFoundException::new);
        Task parentTaskCopy = new Task();

        List<ResolutionIdDescDisplayAs> allResolutions = resolutionRepository.getResolutionIdDescDisplayAs();

        boolean resolutionExists = allResolutions.stream()
                .anyMatch(resolution -> resolution.getResolutionId().equals(updateBugDetailsRequest.getResolutionId()));

        if (!resolutionExists) {
            throw new IllegalArgumentException("Invalid value for Resolution");
        }
        if (updateBugDetailsRequest.getRcaId() != null && !Constants.RCA_ID_LIST.contains(updateBugDetailsRequest.getRcaId())) {
            throw new ValidationFailedException("Please select a valid RCA reason ID for this bug or its parent bug");
        }

        BeanUtils.copyProperties(parentTask, parentTaskCopy);
        if (updateBugDetailsRequest.getResolutionId() != null) {
            parentTaskCopy.setResolutionId(updateBugDetailsRequest.getResolutionId());
        }
        if (updateBugDetailsRequest.getStepsTakenToComplete() != null) {
            parentTaskCopy.setStepsTakenToComplete(updateBugDetailsRequest.getStepsTakenToComplete());
        }
        if (updateBugDetailsRequest.getIsRcaDone() != null) {
            parentTaskCopy.setIsRcaDone(updateBugDetailsRequest.getIsRcaDone());
        }
        if (updateBugDetailsRequest.getRcaId() != null) {
            parentTaskCopy.setRcaId(updateBugDetailsRequest.getRcaId());
        }
        if (updateBugDetailsRequest.getRcaReason() != null) {
            parentTaskCopy.setRcaReason(updateBugDetailsRequest.getRcaReason());
        }
        if (updateBugDetailsRequest.getRcaIntroducedBy() != null) {
            parentTaskCopy.setRcaIntroducedBy(updateBugDetailsRequest.getRcaIntroducedBy());
        }

        if(updateBugDetailsRequest.getPlaceOfIdentification() != null) {
            PlaceOfIdentification enumValue = PlaceOfIdentification.containsAny(updateBugDetailsRequest.getPlaceOfIdentification());
            if(enumValue != null) {
                parentTaskCopy.setPlaceOfIdentification(enumValue);
            }
            else{
                throw new ValidationFailedException("Provided \"place of identification\" field is invalid");
            }
        }

        if ((parentTaskCopy.getStepsTakenToComplete() != null && parentTaskCopy.getStepsTakenToComplete().isEmpty()) || parentTaskCopy.getStepsTakenToComplete() == null || parentTaskCopy.getResolutionId() == null) {
            throw new IllegalArgumentException("Steps taken to complete or resolution can't be empty in parent task");
        }

        if (Constants.WorkFlowStatusIds.COMPLETED.contains(parentTaskCopy.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
            if (parentTaskCopy.getRcaId() == null) {
                throw new ValidationFailedException("Root cause analysis can't be empty for this bug or its parent bug");
            }
            if (!Objects.equals(parentTaskCopy.getRcaId(), Constants.RCAEnum.RCA_DOES_NOT_REQUIRED.getTypeId()) && (parentTaskCopy.getIsRcaDone() == null) || Boolean.FALSE.equals(parentTaskCopy.getIsRcaDone())) {
                throw new ValidationFailedException("Please mark RCA is done of its parent bug to mark the bug completed");
            }
        }
        if (parentTaskCopy.getRcaId() != null && Objects.equals(parentTaskCopy.getRcaId(), Constants.RCAEnum.RCA_DOES_NOT_REQUIRED.getTypeId())) {
            parentTaskCopy.setIsRcaDone(null);
            parentTaskCopy.setRcaReason(null);
            parentTaskCopy.setRcaIntroducedBy(null);
        }
        if (parentTaskCopy.getRcaIntroducedBy() != null && !parentTaskCopy.getRcaIntroducedBy().isEmpty()) {
            parentTaskCopy.setRcaIntroducedBy(findValidAccountIdList(parentTaskCopy.getRcaIntroducedBy()));
        }
        if (parentTaskCopy.getRcaReason() != null) {
            parentTaskCopy.setRcaReason(parentTaskCopy.getRcaReason().trim());
        }

        //below fields are of type basic update
        List<String> fieldsToUpdate = Arrays.asList(Constants.TaskFields.STEPS_TAKEN_TO_COMPLETE, Constants.TaskFields.RESOLUTION_ID);
        boolean isUpdateAllowed = false;

//        if (parentTask.getFkAccountIdAssigned() != null) {
//            if (Objects.equals(parentTask.getFkAccountIdAssigned().getAccountId(), accountIdOfUser)) {
//                isSelfAssigned = true;
//            }
//        }
        isUpdateAllowed = taskService.isUpdateAllowed(Constants.UpdateTeam.Task_Basic_Update, parentTask.getFkAccountIdLastUpdated().getAccountId(), parentTask.getFkTeamId().getTeamId());


        if (!isUpdateAllowed) {
            throw new UnauthorizedException("You're not authorized to update the parent task");
        }

        taskHistoryService.addTaskHistoryOnUserUpdate(parentTask);
        taskHistoryMetadataService.addTaskHistoryMetadata(fieldsToUpdate, parentTask);
        taskRepository.save(parentTaskCopy);
    }

    public TaskListForBulkResponse deleteTaskInBulk (TaskIdsRequest taskIdsRequest, String accountIds, String timeZone) {
        List<Long> taskIdList = taskIdsRequest.getTaskIds();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        TaskListForBulkResponse taskListForBulkResponse = new TaskListForBulkResponse();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();
        DeleteWorkItemRequest deleteWorkItemRequest = new DeleteWorkItemRequest();
        deleteWorkItemRequest.setDeleteReasonId(Constants.DeleteWorkItemReasonEnum.OTHERS.getTypeId());
        deleteWorkItemRequest.setDeleteReason("Work item deleted in bulk deletion");
        for (Long taskId : taskIdList) {
            Task taskDelete = findTaskByTaskId(taskId);
            //Catching the error for failed tasks and adding them to failure list
            try {
                if (taskDelete == null) {
                    continue;
                }
                UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(accountIdList, taskDelete.getFkOrgId().getOrgId(), true);
                if (userAccount == null) {
                    throw new EntityNotFoundException("User not found");
                }
                String response = deleteTaskByTaskId(taskId, taskDelete,userAccount.getAccountId().toString(),timeZone, false, deleteWorkItemRequest);
                successList.add(new TaskForBulkResponse(taskDelete.getTaskId(), taskDelete.getTaskNumber(), taskDelete.getTaskTitle(), taskDelete.getFkTeamId().getTeamId(), response));
            } catch (Exception e) {
                logger.error("Something went wrong: Not able to delete Work Item " + taskDelete.getTaskNumber() + " Caught Exception: " + e.getMessage());
                failureList.add(new TaskForBulkResponse(taskDelete.getTaskId(), taskDelete.getTaskNumber(), taskDelete.getTaskTitle(), taskDelete.getFkTeamId().getTeamId(), e.getMessage()));
            }
        }
        taskListForBulkResponse.setSuccessList(successList);
        taskListForBulkResponse.setFailureList(failureList);
        return taskListForBulkResponse;
    }

    private void validateTaskEntities (Task task) {
        Team team = teamRepository.findByTeamId(task.getFkTeamId().getTeamId());
        if (task.getFkProjectId() == null || task.getFkProjectId().getProjectId() == null) {
            throw new IllegalStateException("Please provide project to create task");
        }
        if (!Objects.equals(team.getFkProjectId().getProjectId(), task.getFkProjectId().getProjectId())) {
            throw new IllegalStateException("The provided team does not belong to the specified project.");
        }
        if (!Objects.equals(team.getFkProjectId().getOrgId(), task.getFkOrgId().getOrgId())) {
            throw new IllegalStateException("The provided team does not belong to the specified organization.");
        }
    }

    public WorkItem validateTaskForDependency(ValidateTaskRequest validateTaskRequest) throws InvalidRelationTypeException, EntityNotFoundException {
        if (validateTaskRequest.getLinkTo() == null || validateTaskRequest.getLinkTo().replaceAll("\\s", "").isEmpty()) {
            throw new InvalidRequestParamater("Please, Enter a valid Work Item number");
        }
        Task linkTo = taskRepository.findByFkTeamIdTeamIdAndTaskNumber(validateTaskRequest.getTeamId(), validateTaskRequest.getLinkTo());

        if (linkTo == null) {
            throw new EntityNotFoundException("The item adding for dependency does not exist");
        }
        if (Objects.equals(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE.toLowerCase())) {
            throw new DependencyValidationException("Work Item cannot have a dependency to a completed Work Item");
        }
        if (Objects.equals(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE.toLowerCase())) {
            throw new DependencyValidationException("Work Item cannot have dependency on deleted Work Item");
        }

        if (!validateTaskRequest.getIsForCreation()) {
            Task linkFrom = taskRepository.findByTaskId(validateTaskRequest.getLinkFrom());

            if (linkFrom == null) {
                throw new EntityNotFoundException("LinkFrom Work Item does not exist");
            }

            if (Objects.equals(linkFrom.getTaskId(), linkTo.getTaskId())) {
                throw new InvalidRelationTypeException("Work Item cannot have a dependency on itself");
            }

            if (!Objects.equals(linkFrom.getFkTeamId().getTeamId(), linkTo.getFkTeamId().getTeamId())) {
                throw new InvalidRelationTypeException("Work Item do not belong to the same team");
            }

            List<Dependency> dependency = dependencyRepository.findByDependencyIdsAndTaskIds(linkFrom.getDependencyIds(), linkTo.getTaskId(), linkFrom.getTaskId());
            if (dependency != null && !dependency.isEmpty() && ((Objects.equals(dependency.get(0).getPredecessorTaskId(), linkFrom.getTaskId()) && Objects.equals(dependency.get(0).getSuccessorTaskId(), linkTo.getTaskId())) || (Objects.equals(dependency.get(0).getPredecessorTaskId(), linkTo.getTaskId()) && Objects.equals(dependency.get(0).getSuccessorTaskId(), linkFrom.getTaskId())))) {
                throw new InvalidRelationTypeException(linkFrom.getTaskNumber() + " is already dependent to the current Work Item!");
            }

            if (linkFrom.getReferenceWorkItemId() != null && linkFrom.getReferenceWorkItemId().contains(linkTo.getTaskId())) {
                throw new InvalidRelationTypeException(linkTo.getTaskNumber() + " is already referenced to the current Work Item");
            }
            if (linkFrom.getBugTaskRelation() != null && linkFrom.getBugTaskRelation().contains(linkTo.getTaskId())) {
                throw new InvalidRelationTypeException(linkTo.getTaskNumber() + " is already associated with current bug!");
            }
            if (linkFrom.getChildTaskIds() != null && linkFrom.getChildTaskIds().contains(linkTo.getTaskId())) {
                throw new InvalidRelationTypeException("Parent task can't have a dependency on child task and vice-versa!");
            }
            if (linkTo.getChildTaskIds() != null && linkTo.getChildTaskIds().contains(linkFrom.getTaskId())) {
                throw new InvalidRelationTypeException("Parent task can't have a dependency on child task and vice-versa");
            }

            //checking for cyclic dependency for both link to and link from tasks
            validateForParentChildCyclicDependency(linkTo, linkFrom);
            validateForParentChildCyclicDependency(linkTo, linkFrom);
        }

        WorkItem response = new WorkItem();
        response.setTaskId(linkTo.getTaskId());
        response.setTaskEstimate(linkTo.getTaskEstimate());
        response.setTaskNumber(linkTo.getTaskNumber());
        response.setTaskTitle(linkTo.getTaskTitle());
        response.setWorkflowStatus(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        response.setUserPerceivedPercentageTaskCompleted(linkTo.getUserPerceivedPercentageTaskCompleted());
        if (linkTo.getFkAccountIdAssigned() != null) response.setAssignedTo(new EmailFirstLastAccountIdIsActive(linkTo.getFkAccountIdAssigned().getEmail(), linkTo.getFkAccountIdAssigned().getAccountId(), linkTo.getFkAccountIdAssigned().getFkUserId().getFirstName(), linkTo.getFkAccountIdAssigned().getFkUserId().getLastName(), linkTo.getFkAccountIdAssigned().getIsActive()));
        return response;
    }

    public WorkItem validateTaskForReference(ValidateTaskRequest validateTaskRequest) throws InvalidRelationTypeException {
        if (validateTaskRequest.getLinkTo() == null || validateTaskRequest.getLinkTo().replaceAll("\\s", "").isEmpty()) {
            throw new InvalidRequestParamater("Please, Enter a valid Work Item number");
        }
        Task linkTo = taskRepository.findByFkTeamIdTeamIdAndTaskNumber(validateTaskRequest.getTeamId(), validateTaskRequest.getLinkTo());

        if (linkTo == null) {
            throw new EntityNotFoundException("The item to be referenced does not exist");
        }
        if (Objects.equals(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE.toLowerCase())) {
            throw new InvalidRelationTypeException("Work Item cannot have reference on deleted Work Item");
        }
        if (!validateTaskRequest.getIsForCreation()) {
            Task linkFrom = taskRepository.findByTaskId(validateTaskRequest.getLinkFrom());

            if (linkFrom == null) {
                throw new EntityNotFoundException("LinkFrom Work Item does not exist");
            }

            if (Objects.equals(linkFrom.getTaskId(), linkTo.getTaskId())) {
                throw new InvalidRelationTypeException("Work Item cannot have a reference to itself");
            }
            if (!Objects.equals(linkFrom.getFkTeamId().getTeamId(), linkTo.getFkTeamId().getTeamId())) {
                throw new InvalidRelationTypeException("Work Item do not belong to the same team");
            }
            if (linkTo.getDependencyIds() != null && linkTo.getDependencyIds().contains(linkFrom.getTaskId())) {
                throw new InvalidRelationTypeException(linkFrom.getTaskNumber() + " is already dependent to the current Work Item!");
            }
            List<Dependency> dependency = dependencyRepository.findByDependencyIdsAndTaskIds(linkFrom.getDependencyIds(), linkTo.getTaskId(), linkFrom.getTaskId());
            if (dependency != null && !dependency.isEmpty() && ((Objects.equals(dependency.get(0).getPredecessorTaskId(), linkFrom.getTaskId()) && Objects.equals(dependency.get(0).getSuccessorTaskId(), linkTo.getTaskId())) || (Objects.equals(dependency.get(0).getPredecessorTaskId(), linkTo.getTaskId()) && Objects.equals(dependency.get(0).getSuccessorTaskId(), linkFrom.getTaskId())))) {
                throw new InvalidRelationTypeException(linkFrom.getTaskNumber() + " is already dependent to the current Work Item!");
            }
            if (linkFrom.getBugTaskRelation() != null && linkFrom.getBugTaskRelation().contains(linkTo.getTaskId())) {
                throw new InvalidRelationTypeException(linkTo.getTaskNumber() + " is already associated with current bug!");
            }
            if (linkFrom.getChildTaskIds() != null && linkFrom.getChildTaskIds().contains(linkTo.getTaskId())) {
                throw new InvalidRelationTypeException("Parent task can't have a reference on child task and vice-versa");
            }
            if (linkTo.getChildTaskIds() != null && linkTo.getChildTaskIds().contains(linkFrom.getTaskId())) {
                throw new InvalidRelationTypeException("Parent task can't have a reference on child task and vice-versa");
            }
            if (linkTo.getParentTaskId() != null && linkFrom.getParentTaskId() != null && Objects.equals(linkTo.getParentTaskId(), linkFrom.getParentTaskId())) {
                throw new InvalidRelationTypeException("Work Items having same parent cannot have a reference");
            }
            if (linkFrom.getReferenceWorkItemId() != null && linkFrom.getReferenceWorkItemId().contains(linkTo.getTaskId())) {
                throw new InvalidRelationTypeException(linkTo.getTaskNumber() + " is already referenced to the current Work Item");
            }
        }

        WorkItem response = new WorkItem();
        response.setTaskId(linkTo.getTaskId());
        response.setTaskEstimate(linkTo.getTaskEstimate());
        response.setTaskNumber(linkTo.getTaskNumber());
        response.setTaskTitle(linkTo.getTaskTitle());
        response.setWorkflowStatus(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        response.setUserPerceivedPercentageTaskCompleted(linkTo.getUserPerceivedPercentageTaskCompleted());
        if (linkTo.getFkAccountIdAssigned() != null) response.setAssignedTo(new EmailFirstLastAccountIdIsActive(linkTo.getFkAccountIdAssigned().getEmail(), linkTo.getFkAccountIdAssigned().getAccountId(), linkTo.getFkAccountIdAssigned().getFkUserId().getFirstName(), linkTo.getFkAccountIdAssigned().getFkUserId().getLastName(), linkTo.getFkAccountIdAssigned().getIsActive()));
        return response;
    }

    public WorkItem validateTaskForAssociation(ValidateTaskRequest validateTaskRequest) throws InvalidRelationTypeException {
        if (validateTaskRequest.getLinkTo() == null || validateTaskRequest.getLinkTo().replaceAll("\\s", "").isEmpty()) {
            throw new InvalidRequestParamater("Please, Enter a valid Work Item number");
        }
        Task linkFrom = taskRepository.findByTaskId(validateTaskRequest.getLinkFrom());
        Task linkTo = taskRepository.findByFkTeamIdTeamIdAndTaskNumber(validateTaskRequest.getTeamId(), validateTaskRequest.getLinkTo());

        if (linkFrom == null) {
            throw new EntityNotFoundException("LinkFrom Work Item does not exist");
        }
        if (linkTo == null) {
            throw new EntityNotFoundException("The item to be associated does not exist");
        }
        if (Objects.equals(linkFrom.getIsBug(), Boolean.FALSE)) {
            throw new InvalidRelationTypeException("An association can only be initialised by Bug or Parent Bug!");
        }
        if (Objects.equals(linkFrom.getTaskId(), linkTo.getTaskId())) {
            throw new InvalidRelationTypeException("Work Item cannot have a association with itself");
        }
        if (!Objects.equals(linkFrom.getFkTeamId().getTeamId(), linkTo.getFkTeamId().getTeamId())) {
            throw new InvalidRelationTypeException("Work Item do not belong to the same team");
        }
        if (Objects.equals(linkTo.getIsBug(), Boolean.TRUE)) {
            throw new InvalidRelationTypeException("A bug cannot associate with a Bug Work Item!");
        }
        if (!Objects.equals(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE.toLowerCase())) {
            throw new InvalidRelationTypeException("Bug Work Item can only be associated with a completed Work Item");
        }
        if (Objects.equals(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE.toLowerCase())) {
            throw new InvalidRelationTypeException("Work Item cannot have association with deleted Work Item");
        }
        List<Dependency> dependency=dependencyRepository.findByDependencyIdsAndTaskIds(linkFrom.getDependencyIds(),linkTo.getTaskId(),linkFrom.getTaskId());
        if (dependency!=null && !dependency.isEmpty() && ((Objects.equals(dependency.get(0).getPredecessorTaskId(),linkFrom.getTaskId()) && Objects.equals(dependency.get(0).getSuccessorTaskId(),linkTo.getTaskId())) || (Objects.equals(dependency.get(0).getPredecessorTaskId(),linkTo.getTaskId()) && Objects.equals(dependency.get(0).getSuccessorTaskId(),linkFrom.getTaskId())))) {
            throw new InvalidRelationTypeException(linkFrom.getTaskNumber() + " is already dependent to the current Work Item!");
        }
        if (linkFrom.getReferenceWorkItemId()!=null && linkFrom.getReferenceWorkItemId().contains(linkTo.getTaskId())) {
            throw new InvalidRelationTypeException(linkTo.getTaskNumber()+" is already referenced to the current Work Item");
        }
        if (linkFrom.getBugTaskRelation() != null && linkFrom.getBugTaskRelation().contains(linkTo.getTaskId())) {
            throw new InvalidRelationTypeException(linkTo.getTaskNumber() + " is already associated with current bug!");
        }
        WorkItem response = new WorkItem();
        response.setTaskId(linkTo.getTaskId());
        response.setTaskEstimate(linkTo.getTaskEstimate());
        response.setTaskNumber(linkTo.getTaskNumber());
        response.setTaskTitle(linkTo.getTaskTitle());
        response.setWorkflowStatus(linkTo.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        response.setUserPerceivedPercentageTaskCompleted(linkTo.getUserPerceivedPercentageTaskCompleted());
        if (linkTo.getFkAccountIdAssigned() != null) response.setAssignedTo(new EmailFirstLastAccountIdIsActive(linkTo.getFkAccountIdAssigned().getEmail(), linkTo.getFkAccountIdAssigned().getAccountId(), linkTo.getFkAccountIdAssigned().getFkUserId().getFirstName(), linkTo.getFkAccountIdAssigned().getFkUserId().getLastName(), linkTo.getFkAccountIdAssigned().getIsActive()));
        return response;
    }

    private void validateSubTaskForStatusChange (Task task, List<Long> childTaskIdList, Boolean useSystemDerivedForChild) {
        List<Task> childTasks = taskRepository.findByTaskIdIn(childTaskIdList);
        List<String> tasksWithMissingDetails = new ArrayList<>();
        List<String> tasksWithExceededDates = new ArrayList<>();
        for (Task childTask : childTasks) {
            Sprint sprint = null;
            if (childTask.getSprintId() != null) {
                sprint = sprintRepository.findBySprintId(task.getSprintId());
            }
            // Check for missing details (estimate, expected dates, priority)
            if (childTask.getTaskEstimate() == null || childTask.getTaskPriority() == null || (childTask.getFkAccountIdAssigned() == null && ((sprint != null && Objects.equals(Constants.SprintStatusEnum.STARTED.getSprintStatusId(), sprint.getSprintStatus())) || childTask.getTaskActStDate() != null))
                    || ((childTask.getTaskExpEndDate() == null) != (childTask.getTaskExpStartDate() == null))) {
                tasksWithMissingDetails.add(String.valueOf(childTask.getTaskNumber()));
            }
            if ((childTask.getTaskExpStartDate() != null && childTask.getTaskExpStartDate().isBefore(task.getTaskExpStartDate()))
                    || (childTask.getTaskExpEndDate() != null && childTask.getTaskExpEndDate().isAfter(task.getTaskExpEndDate()))) {
                tasksWithExceededDates.add(String.valueOf(childTask.getTaskNumber()));
            }
        }

        if ((!tasksWithMissingDetails.isEmpty() || !tasksWithExceededDates.isEmpty()) && (useSystemDerivedForChild == null || !useSystemDerivedForChild)) {
            String errorMessage = "Error: ";
            if (!tasksWithMissingDetails.isEmpty()) {
                String taskNumbers = String.join(", ", tasksWithMissingDetails);
                errorMessage += "the following sub-tasks [" + taskNumbers + "], are missing one or more required details: estimate, assignee, priority, or expected dates. Please review and update them before trying again.";
            }
            if (!tasksWithExceededDates.isEmpty()) {
                String exceededTaskNumbers = String.join(", ", tasksWithExceededDates);
                if (!errorMessage.equalsIgnoreCase("Error: ")) {
                    errorMessage += " Additionally, ";
                }
                errorMessage += "the following sub-tasks [" + exceededTaskNumbers + "] have dates that fall outside the expected range of their parent task.";
            }
            throw new SubTaskDetailsMissingException(errorMessage);
        }
    }

     public void validateImmediateAttention(Task task) {
         if (task.getImmediateAttention() != null && !Objects.equals(task.getImmediateAttention(), 0)) {
             if (task.getImmediateAttentionFrom() == null) {
                 throw new ValidationFailedException("Please enter Immediate attention from field");
             } else if (task.getImmediateAttentionReason() == null) {
                 throw new ValidationFailedException("Please enter Immediate attention reason field");
             }
             task.setImmediateAttention(1);
         } else {
             task.setImmediateAttention(0);
             task.setImmediateAttentionFrom(null);
             task.setImmediateAttentionReason(null);
         }
     }

    public Integer effortsWithin24 (Long accountId, NewEffortTrack newEffortTrack) {
        List<TimeSheet> oldEfforts = timeSheetRepository.findByAccountIdAndNewEffortDate(accountId, newEffortTrack.getNewEffortDate());
        Integer effortTime=0;
        for(TimeSheet oldEffort:oldEfforts)
            effortTime+=oldEffort.getNewEffort();
        return effortTime+newEffortTrack.getNewEffort()-1440;
    }

    public List<String> removeTaskReferencesForDeleteTask (Task task) {
        List<String> taskNumbers = new ArrayList<>();
        if ( task.getReferenceWorkItemId() != null && !task.getReferenceWorkItemId().isEmpty()) {
            List<Long> referenceTaskIds = task.getReferenceWorkItemId();
            List<Task> referenceTasks = taskRepository.findByTaskIdIn(referenceTaskIds);

            for (Task referenceTask : referenceTasks) {
                Task taskCopy = new Task();
                taskNumbers.add(referenceTask.getTaskNumber());
                List<String> updatedFields = new ArrayList<>();
                BeanUtils.copyProperties(referenceTask, taskCopy);
                if (referenceTask.getReferenceWorkItemId() != null) {
                    referenceTask.getReferenceWorkItemId().remove(task.getTaskId());
                    updatedFields.add(Constants.TaskFields.REFERENCE_WORK_ITEM_ID);
                    taskHistoryService.addTaskHistoryOnSystemUpdate(taskCopy);
                    Task savedTask = taskRepository.save(referenceTask);
                    taskHistoryMetadataService.addTaskHistoryMetadataBySystemUpdate(updatedFields, savedTask);
                }
            }
            task.getReferenceWorkItemId().removeAll(referenceTaskIds);
        }
        return taskNumbers;
    }

    public String getMessageForDeletingChildTask (Long taskId, String accountIds) {
        String message = "Please confirm that you wish to delete this work item." ;
        Task subTask = taskRepository.findById(taskId).orElseThrow(EntityNotFoundException::new);

        if (!subTask.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
            throw new IllegalArgumentException("Given Work Item is not a child task");
        }

        if (Objects.equals(subTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new IllegalStateException("Work Item already deleted.");
        }

        Task parentTask = taskRepository.findById(subTask.getParentTaskId()).orElseThrow(EntityNotFoundException::new);

        if (!taskService.isTaskViewAllowed(parentTask, accountIds)) {
            throw new UnauthorizedException("User not authorised to view this information");
        }

        List<Task> childTaskList = taskRepository.findByTaskIdIn(parentTask.getChildTaskIds());
        childTaskList.addAll(taskRepository.findByTaskIdIn(parentTask.getDeletedChildTaskIds()));

        Integer otherChildTaskCount = childTaskList.size() - 1;
        if (Objects.equals(otherChildTaskCount, 0)) {
            message = "This is the only Child Task of the Parent Task " + parentTask.getTaskNumber() + ". Deleting this would delete the Parent Task as well. Do you wish to continue?";
            return message;
        }

        Integer completedTask = 0;
        Integer deletedTask = 0;

        for (Task child : childTaskList) {
             if (!Objects.equals(child.getTaskNumber(), subTask.getTaskNumber()) && Objects.equals(child.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)) {
                completedTask++;
            } else if (!Objects.equals(child.getTaskNumber(), subTask.getTaskNumber()) && Objects.equals(child.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                 deletedTask++;
             }
        }

        if (!Objects.equals(completedTask, 0) && Objects.equals(completedTask + deletedTask, otherChildTaskCount)) {
            message = "The Parent Task of this Child Task is " + parentTask.getTaskNumber() + ". All the other Child Tasks of this Parent Task are either completed or deleted. Deleting this would mark the Parent Task as completed. Do you wish to continue?";
        }

        if (Objects.equals(deletedTask, otherChildTaskCount)) {
            message = "The Parent Task of this Child Task is " + parentTask.getTaskNumber() + ". All the other Child Tasks of this Parent Task are deleted. Deleting this would delete the Parent Task as well. Do you wish to continue?";
        }

        return message;
    }
    private void addSprintInPrevSprintList (Task foundTaskDb, Task task) {
        Sprint currentSprint = sprintRepository.findBySprintId(foundTaskDb.getSprintId());
        LocalTime sprintCompleteTimeLimit = entityPreferenceService.getOfficeEndTime(foundTaskDb.getFkOrgId().getOrgId(), foundTaskDb.getFkTeamId().getTeamId()).minusHours(entityPreferenceService.getCapacityLimit(foundTaskDb.getFkOrgId().getOrgId(), foundTaskDb.getFkTeamId().getTeamId()));
        if ((((LocalDate.now().isAfter(currentSprint.getSprintExpEndDate().toLocalDate()) || (LocalDate.now().isEqual(currentSprint.getSprintExpEndDate().toLocalDate()) && LocalTime.now().isAfter(sprintCompleteTimeLimit))) && Objects.equals(currentSprint.getSprintStatus(), Constants.SprintStatusEnum.STARTED.getSprintStatusId())))) {
            Set<Long> prevSprints = new HashSet<>(task.getPrevSprints());
            prevSprints.add(currentSprint.getSprintId());
            List<Long> prevSprintList = new ArrayList<>(prevSprints);
            task.setPrevSprints(prevSprintList);
        }
    }

    public void validateCreateTaskWithEpicAndModifyProperties(Task task, String accountIds) {
        Epic epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
        if (epic == null) {
            throw new EntityNotFoundException("Epic is not valid");
        }
        if (!epic.getTeamIdList().contains(task.getFkTeamId().getTeamId())) {
            throw new ValidationFailedException("Team of Work Item or bug is not part of selected epic");
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long userAccountId = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(epic.getFkOrgId().getOrgId(), true, accountIdList);
        List<Integer> authorizeRoleIdList = Constants.ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION;

        if(!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), userAccountId, true, authorizeRoleIdList)) {
            throw new ValidationFailedException("User does not have role to add epic in Work Item or bug");
        }
        if(Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_DELETED.getWorkflowEpicStatusId()) || Objects.equals(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId(), Constants.EpicStatusEnum.STATUS_COMPLETED.getWorkflowEpicStatusId())) {
            throw new ValidationFailedException("Completed or Deleted epic can't be add in Work Item");
        }
        if(epic.getExpEndDateTime() != null) {
            if(task.getSprintId() != null) {
                if(task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime()) || (epic.getExpStartDateTime() != null && task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime()))) {
                    throw new ValidationFailedException("Task is part of sprint and it's expected end date & time doesn't matched with epic's date & time");
                }
            }
            if(task.getTaskExpEndDate() == null || (epic.getExpStartDateTime() != null && task.getTaskExpEndDate().isBefore(epic.getExpStartDateTime())) || task.getTaskExpEndDate().isAfter(epic.getExpEndDateTime())) {
                task.setTaskExpEndDate(epic.getExpEndDateTime());
                task.setTaskExpEndTime(epic.getExpEndDateTime().toLocalTime());
            }
        }
        if(epic.getExpStartDateTime() != null) {
            if(task.getSprintId() != null) {
                if (task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                    throw new ValidationFailedException("Work Item is part of sprint and it's expected start date & time doesn't matched with epic's date & time");
                }
            }
            if(task.getTaskExpStartDate() == null || (epic.getExpEndDateTime() != null && task.getTaskExpStartDate().isAfter(epic.getExpEndDateTime())) || task.getTaskExpStartDate().isBefore(epic.getExpStartDateTime())) {
                task.setTaskExpStartDate(epic.getExpStartDateTime());
                task.setTaskExpStartTime(epic.getExpStartDateTime().toLocalTime());
            }
        }
    }

    public void setEpicInCreateTask(Task task) throws IllegalAccessException {
        Epic epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
        addEpicInTask(epic, task);
        epicRepository.save(epic);
    }

    private void moveChildTaskFromSprint(Task task) {
        List<Task> childTaskList = taskRepository.findByParentTaskId(task.getTaskId()).stream()
                .map(originalTask -> {
                    Task copy = new Task();
                    BeanUtils.copyProperties(originalTask, copy);
                    return copy;
                })
                .collect(Collectors.toList());

        if (childTaskList.isEmpty()) {
            return; // No child tasks to process, exit early.
        }
        StatType worstStat = task.getTaskProgressSystem();
        Sprint sprint = sprintRepository.findBySprintId(task.getSprintId());
        LocalTime sprintCompleteTimeLimit = entityPreferenceService.getOfficeEndTime(task.getFkOrgId().getOrgId(), task.getFkTeamId().getTeamId())
                .minusHours(entityPreferenceService.getCapacityLimit(task.getFkOrgId().getOrgId(), task.getFkTeamId().getTeamId()));

        for (Task childTask : childTaskList) {
            Task childTaskCopy = new Task();
            BeanUtils.copyProperties(childTask, childTaskCopy);
            // Skip tasks with DELETE or COMPLETED status
            if (sprintService.isTaskDeletedOrCompleted(childTask)) {
                continue;
            }
            List<String> updatedFields = new ArrayList<>();
            Sprint currentSprint = sprintRepository.findBySprintId(childTask.getSprintId());
            sprintService.adjustSprintHours(childTask, sprint, currentSprint);
            sprintService.handleSprintCapacity(childTask, currentSprint, sprintCompleteTimeLimit);
            sprintService.updateTaskSprintsAndWorkflow(childTask, sprint, task, currentSprint, sprintCompleteTimeLimit, updatedFields);

            // Update progress system
            if (sprintService.shouldUpdateTaskProgress(childTask, sprint)) {
                computeAndUpdateStatForTask(childTask, true);
                worstStat = isWorseStat(childTask.getTaskProgressSystem(), worstStat) ? childTask.getTaskProgressSystem() : worstStat;
            }

            // Create audit and save changes
            Integer noOfAudit = updatedFields.size();
            Audit audit = auditService.createAudit(task, noOfAudit, task.getTaskId(), Constants.TaskFields.SPRINT_ID);
            auditRepository.save(audit);
            taskRepository.save(childTask);
            taskHistoryService.addTaskHistoryOnUserUpdate(childTaskCopy);
            taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, childTask);
        }

        if (!Objects.equals(worstStat, task.getTaskProgressSystem())) {
            task.setTaskProgressSystem(worstStat);
            task.setTaskProgressSystemLastUpdated(LocalDateTime.now());
        }
    }

    private void validateForParentChildCyclicDependency(Task task, Task relatedTask) {
        //getting the dependency ids for related tasks
        List<Long> dependencyIds = relatedTask.getDependencyIds() != null ? relatedTask.getDependencyIds() : Collections.emptyList();

        //if task is parent checking for dependencies in all child tasks
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
            List<Long> childTaskDependencyIdList = taskRepository.findDependencyIdsByTaskIdIn(task.getChildTaskIds());
            if (CommonUtils.containsAny(dependencyIds, childTaskDependencyIdList)) {
                throw new IllegalStateException("There already exists a dependency with one of the child task of Work Item : " + task.getTaskNumber() + " with Work Item : " + relatedTask.getTaskNumber());
            }
        }
        // if task is child checking for dependencies in parent
        else if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
            List<Long> parentTaskDependencyIdList = taskRepository.findDependencyIdsByTaskIdIn(List.of(task.getParentTaskId()));
            if (CommonUtils.containsAny(dependencyIds, parentTaskDependencyIdList)) {
                throw new IllegalStateException("There already exists a dependency with parent task of Work Item : " + task.getTaskNumber() + " with Work Item : " + relatedTask.getTaskNumber());
            }
        }
    }

    public void modifyEpicPropertiesOnChildTaskCreation(Task task) {
        Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
        Epic epic = epicRepository.findByEpicId(task.getFkEpicId().getEpicId());
        Boolean isFirstChildTask = false;
        if (Objects.equals(parentTask.getTaskTypeId(), Constants.TaskTypes.TASK) || Objects.equals(parentTask.getTaskTypeId(), Constants.TaskTypes.BUG_TASK)) {
            isFirstChildTask = true;
        }
        List<Integer> statusList = new ArrayList<>();
        statusList.add(Constants.EpicStatusEnum.STATUS_BACKLOG.getWorkflowEpicStatusId());
        statusList.add(Constants.EpicStatusEnum.STATUS_IN_REVIEW.getWorkflowEpicStatusId());
        statusList.add(Constants.EpicStatusEnum.STATUS_REVIEWED.getWorkflowEpicStatusId());
        if (isFirstChildTask && parentTask.getTaskEstimate() != null) {
            if (statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                epic.setOriginalEstimate(epic.getOriginalEstimate() - parentTask.getTaskEstimate());
            }
            epic.setRunningEstimate(epic.getRunningEstimate() - parentTask.getTaskEstimate());
        }
        if (task.getTaskEstimate() != null) {
            if (statusList.contains(epic.getFkWorkflowEpicStatus().getWorkflowEpicStatusId())) {
                epic.setOriginalEstimate((epic.getOriginalEstimate() == null ? task.getTaskEstimate() : epic.getOriginalEstimate() + task.getTaskEstimate()));
            }
            epic.setRunningEstimate((epic.getRunningEstimate() == null ? task.getTaskEstimate() : epic.getRunningEstimate() + task.getTaskEstimate()));
        }
        EpicTask epicTask = new EpicTask();
        epicTask.setFkEpicId(epic);
        epicTask.setFkTaskId(task);
        epicTask.setIsDeleted(false);
        epicTaskRepository.save(epicTask);

        epicRepository.save(epic);
    }

    public void addLabelInWorkItem (Task taskOptional) {
        Task taskFromDb = taskRepository.findByTaskId(taskOptional.getTaskId());
        if (taskFromDb != null && taskFromDb.getLabels() != null && !taskFromDb.getLabels().isEmpty()) {
            List<String> taskLabels = taskFromDb.getLabels().stream()
                    .map(Label::getLabelName)
                    .collect(Collectors.toList());
            taskOptional.setTaskLabels(taskLabels);
        }
    }

    private void setStatusOnWorkItemDeletion (Task task) {
        if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE, task.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            task.setStatusAtTimeOfDeletion(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE);
        }
        else if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE, task.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            task.setStatusAtTimeOfDeletion(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE);
        }
        else if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE, task.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            task.setStatusAtTimeOfDeletion(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE);
        }
        else if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE, task.getFkWorkflowTaskStatus().getWorkflowTaskStatus())
                || Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE, task.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
            if (task.getTaskActStDate() != null) {
                task.setStatusAtTimeOfDeletion(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE);
            }
            else {
                task.setStatusAtTimeOfDeletion(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE);
            }
        }
    }

    public List<OpenTaskDetails> getOpenTasksAssignedToUserInSprint(OpenTasksAssignedToUserInSprintRequest openTasksAssignedToUserInSprintRequest) {

        List<Task> openTasks = taskRepository.findByFkAccountIdAssignedAccountIdAndSprintId(openTasksAssignedToUserInSprintRequest.getAccountIdAssigned(), openTasksAssignedToUserInSprintRequest.getSprintId());
        List<OpenTaskDetails> openTaskDetailsInSprint = new ArrayList<>();

        for (Task task : openTasks) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) ||
                    Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                continue;
            }
            OpenTaskDetails openTaskDetails = new OpenTaskDetails();
            BeanUtils.copyProperties(task, openTaskDetails);
            Long teamId = task.getFkTeamId().getTeamId();
            openTaskDetails.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
            openTaskDetails.setWorkflowStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            openTaskDetails.setTeamId(teamId);
            openTaskDetails.setOrgId(task.getFkOrgId().getOrgId());
            openTaskDetails.setTeamName(task.getFkTeamId().getTeamName());
            if (task.getTaskTypeId().equals(Constants.TaskTypes.CHILD_TASK)) {
                Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                openTaskDetails.setParentTaskIdentifier(parentTask.getTaskIdentifier());
                openTaskDetails.setParentTaskNumber(parentTask.getTaskNumber());
                openTaskDetails.setParentTaskTitle(parentTask.getTaskTitle());
            }
            openTaskDetailsInSprint.add(openTaskDetails);
        }
        return openTaskDetailsInSprint;
    }

    public Boolean isActiveReferenceMeetingPresent(Task task) {
        List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());
        if (meetingList != null && !meetingList.isEmpty()) {
            for (Meeting meeting : meetingList) {
                List<Attendee> attendeeList = new ArrayList<>();
                if (meeting.getAttendeeId() != null && meeting.getAttendeeList() != null) {
                    attendeeList = attendeeService.removeDeletedAttendees(meeting.getAttendeeList());
                }
                if (attendeeList != null && !attendeeList.isEmpty() && !isMeetingCompleted(attendeeList)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean isMeetingCompleted (List<Attendee> attendeeList) {
        if (attendeeList != null && !attendeeList.isEmpty()) {
            for (Attendee attendee : attendeeList) {
                if (attendee.getDidYouAttend() != null && Objects.equals(attendee.getDidYouAttend(), com.tse.core_application.model.Constants.BooleanValues.BOOLEAN_TRUE)
                    && attendee.getAttendeeDuration() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void validationForCompletedChildTask(Task task) {
        Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
        List<Task> allChildTasks = taskRepository.findByParentTaskId(parentTask.getTaskId());
        boolean isAllChildTaskCompleted = true;
        for (Task childTask : allChildTasks) {
            if (!Objects.equals(task.getTaskId(), childTask.getTaskId()) && !childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
                isAllChildTaskCompleted = false;
            }
        }
        if (isActiveReferenceMeetingPresent(task)) {
            throw new ValidationFailedException("Please remove all the non-completed meeting of work item " + task.getTaskNumber());
        }
        if (isAllChildTaskCompleted) {
            if (isActiveReferenceMeetingPresent(parentTask)) {
                throw new ValidationFailedException("Please remove all the non-completed meeting of parent task to complete last child task");
            }
        }
    }

    public Boolean isMeetingDateRangeIsOutsideOfReferenceTask (Task task) {
        List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());
        if (meetingList != null && !meetingList.isEmpty()) {
            for (Meeting meeting : meetingList) {
                if ((meeting.getStartDateTime() != null && task.getTaskExpStartDate() != null && meeting.getStartDateTime().isBefore(task.getTaskExpStartDate())) ||
                        meeting.getEndDateTime() != null && task.getTaskExpEndDate() != null && meeting.getEndDateTime().isAfter(task.getTaskExpEndDate())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Task findTaskByTeamIdAndTaskNumber(Long teamId, String referenceEntityNumber) {
        return taskRepository.findByFkTeamIdTeamIdAndTaskNumber(teamId, referenceEntityNumber.toUpperCase());
    }

    public void validateDeleteReason (Task taskDelete, DeleteWorkItemRequest deleteWorkItemRequest) {
        if (deleteWorkItemRequest .getDeleteReasonId() != null) {
            if (!Constants.DELETED_WORK_ITEM_REASON_ID.contains(deleteWorkItemRequest.getDeleteReasonId())) {
                throw new ValidationFailedException("Please select the valid reason for deleting the work item");
            }
            if (Objects.equals(Constants.DeleteWorkItemReasonEnum.DUPLICATE.getTypeId(), deleteWorkItemRequest.getDeleteReasonId())) {
                if (deleteWorkItemRequest.getDuplicateWorkItemNumber() == null) {
                    throw new ValidationFailedException("Please enter duplicate work item number");
                }
                Task duplicateWorkItem = findTaskByTeamIdAndTaskNumber(taskDelete.getFkTeamId().getTeamId(), deleteWorkItemRequest.getDuplicateWorkItemNumber());
                if (duplicateWorkItem == null) {
                    throw new ValidationFailedException("Please enter the valid duplicate work item number");
                }
                if (Objects.equals(taskDelete.getTaskId(), duplicateWorkItem.getTaskId())) {
                    throw new ValidationFailedException("Work item can't be duplicated to itself");
                }
                if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, duplicateWorkItem.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                    throw new ValidationFailedException("Duplicated work item can't be deleted");
                }
                deleteWorkItemRequest.setDeleteReason(null);
            } else if (Objects.equals(Constants.DeleteWorkItemReasonEnum.OTHERS.getTypeId(), deleteWorkItemRequest.getDeleteReasonId()) && deleteWorkItemRequest.getDeleteReason() == null) {
                throw new ValidationFailedException("Please enter the delete reason");
            }
            else {
                deleteWorkItemRequest.setDuplicateWorkItemNumber(null);
                deleteWorkItemRequest.setDeleteReason(deleteWorkItemRequest.getDeleteReason() != null ? deleteWorkItemRequest.getDeleteReason().trim() : null);
            }

        }
        else {
            deleteWorkItemRequest.setDeleteReason(null);
            deleteWorkItemRequest.setDuplicateWorkItemNumber(null);
        }
    }

    public void validateAndNormalizeRca (Task task) {
        if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.BUG_TASK) ||
                (task.getIsBug() && Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK))) {
            if (task.getRcaId() != null && !Constants.RCA_ID_LIST.contains(task.getRcaId())) {
                throw new MissingDetailsException("Please select a valid RCA reason ID for this bug or its parent bug");
            }
            if (Constants.WorkFlowStatusIds.COMPLETED.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                if (task.getRcaId() == null) {
                    throw new MissingDetailsException("Root cause analysis can't be empty for this bug or its parent bug");
                }
                if (!Objects.equals(task.getRcaId(), Constants.RCAEnum.RCA_DOES_NOT_REQUIRED.getTypeId()) && (task.getIsRcaDone() == null || Boolean.FALSE.equals(task.getIsRcaDone()))) {
                    throw new MissingDetailsException("Please mark RCA is done for this bug to mark the bug completed");
                }
            }
            if (task.getRcaId() != null && Objects.equals(task.getRcaId(), Constants.RCAEnum.RCA_DOES_NOT_REQUIRED.getTypeId())) {
                task.setIsRcaDone(null);
                task.setRcaReason(null);
                task.setRcaIntroducedBy(null);
            }

        }
        else {
            task.setRcaId(null);
            task.setIsRcaDone(null);
            task.setRcaReason(null);
            task.setRcaIntroducedBy(null);
        }
        if (task.getRcaIntroducedBy() != null && !task.getRcaIntroducedBy().isEmpty()) {
            task.setRcaIntroducedBy(findValidAccountIdList(task.getRcaIntroducedBy()));
        }
        if (task.getRcaReason() != null) {
            task.setRcaReason(task.getRcaReason().trim());
        }
    }

    /**
     * Validates that RCA fields are not modified once a bug/parent bug is marked as completed.
     * PT-14170: RCA fields should not be editable after bug completion.
     *
     * @param task The task being updated (with new values)
     * @param taskDb The original task from database
     * @throws ValidationFailedException if RCA fields are being modified on a completed bug
     */
    public void validateRcaFieldsNotEditableForCompletedBug(Task task, Task taskDb) {
        // Only validate for BUG_TASK or PARENT_TASK with isBug=true
        boolean isBugTask = Objects.equals(taskDb.getTaskTypeId(), Constants.TaskTypes.BUG_TASK) ||
                (taskDb.getIsBug() != null && taskDb.getIsBug() && Objects.equals(taskDb.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK));

        if (!isBugTask) {
            return;
        }

        // Check if the original task (taskDb) is in COMPLETED status
        boolean isCompleted = taskDb.getFkWorkflowTaskStatus() != null &&
                Constants.WorkFlowStatusIds.COMPLETED.contains(taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatusId());

        if (!isCompleted) {
            return;
        }

        // If bug is completed, validate that no RCA field is being modified
        boolean rcaIdChanged = !Objects.equals(task.getRcaId(), taskDb.getRcaId());
        boolean isRcaDoneChanged = !Objects.equals(task.getIsRcaDone(), taskDb.getIsRcaDone());
        boolean rcaReasonChanged = !Objects.equals(
                task.getRcaReason() != null ? task.getRcaReason().trim() : null,
                taskDb.getRcaReason() != null ? taskDb.getRcaReason().trim() : null);
        boolean rcaIntroducedByChanged = !Objects.equals(task.getRcaIntroducedBy(), taskDb.getRcaIntroducedBy());

        if (rcaIdChanged || isRcaDoneChanged || rcaReasonChanged || rcaIntroducedByChanged) {
            throw new ValidationFailedException("RCA fields cannot be modified once the bug is marked as completed");
        }
    }

    public List<Long> findValidAccountIdList (List<Long> accountIdList) {
        List<Long> validAccountIdList = new ArrayList<>();
        for (Long accountId : accountIdList) {
            if (userAccountRepository.findByAccountId(accountId) != null) {
                validAccountIdList.add(accountId);
            }
        }
        return validAccountIdList;
    }

    public void validateExpDateTimeWithEstimate (Task task) {
        if (!Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)
                && task.getTaskEstimate() != null && task.getTaskExpStartDate() != null && task.getTaskExpEndDate() != null
                && Duration.between(task.getTaskExpStartDate(), task.getTaskExpEndDate()).toMinutes() < task.getTaskEstimate().longValue()) {
            throw new ValidationFailedException("Time difference between Expected Start Date Time and Expected End Date Time is not sufficient for the work item. Please reduce the estimate or increase the time period " +
                    "OR if the Work Item is in Started state then Expected Dates of Work Item can't be adjusted, when not fall in Sprint's Expected Date range.");
        }
    }

    public void createOrUpdateReleaseVersionOfTask (Task taskDb, Task task) {
        if (taskDb == null || !Objects.equals(taskDb.getReleaseVersionName(), task.getReleaseVersionName())) {
            if (task.getReleaseVersionName() != null && !releaseVersionRepository.existsByEntityTypeIdAndEntityIdAndReleaseVersionName (Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), task.getReleaseVersionName())) {
                ReleaseVersion releaseVersion = new ReleaseVersion();
                releaseVersion.setReleaseVersionName(task.getReleaseVersionName());
                releaseVersion.setEntityTypeId(Constants.EntityTypes.TEAM);
                releaseVersion.setEntityId(task.getFkTeamId().getTeamId());
                releaseVersionRepository.save(releaseVersion);
            }
        }
    }

    public List<ReleaseVersionResponse> getReleaseVersionOfEntity(Integer entityTypeId, Long entityId, String accountIds) {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Integer> roleIdsForReleaseVersion = Constants.ROLE_IDS_FOR_RELEASE_VERSION;
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(entityTypeId, entityId, accountIdList, true, roleIdsForReleaseVersion)) {
            throw new ValidationFailedException("You are not part of selected entity type");
        }
        List<ReleaseVersionResponse> releaseVersionResponseList = new ArrayList<>();
        List<ReleaseVersion> releaseVersionList = releaseVersionRepository.findByEntityTypeIdAndEntityIdOrderByCreatedDateTimeDesc (entityTypeId, entityId);
        if (releaseVersionList != null && !releaseVersionList.isEmpty()) {
            for (ReleaseVersion releaseVersion : releaseVersionList) {
                ReleaseVersionResponse releaseVersionResponse = new ReleaseVersionResponse();
                BeanUtils.copyProperties(releaseVersion, releaseVersionResponse);
                releaseVersionResponseList.add(releaseVersionResponse);
            }
        }
        return releaseVersionResponseList;
    }

    public String updateStarredFieldOfWorkItem(StarredWorkItemRequest starredWorkItemRequest, String accountIds) {
        if (starredWorkItemRequest.getTaskId() == null) {
            throw new ValidationFailedException("Please mark any work item flagged");
        }

        Task taskDb = taskRepository.findByTaskId(starredWorkItemRequest.getTaskId());
        if (taskDb == null) {
            throw new EntityNotFoundException("Work item doesn't exist");
        }

        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(
                Constants.EntityTypes.TEAM,
                taskDb.getFkTeamId().getTeamId(),
                accountIdList,
                true
        );

        if (accountId == null) {
            throw new ValidationFailedException("You are not part of the team");
        }

        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
        if (userAccount == null) {
            throw new ValidationFailedException("Invalid user account");
        }

        boolean requestIsStarred = Boolean.TRUE.equals(starredWorkItemRequest.getIsStarred());
        boolean currentIsStarred = Boolean.TRUE.equals(taskDb.getIsStarred());

        if (!requestIsStarred && !currentIsStarred) {
            throw new ValidationFailedException("This work item has already been un-flagged");
        }
        if (requestIsStarred && currentIsStarred) {
            throw new ValidationFailedException("This work item has already been flagged");
        }
        if(Objects.equals(taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(),Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE) && requestIsStarred) {
            throw new ValidationFailedException("Deleted work item cannot be marked as flagged");
        }
        List<Integer> userRoles = accessDomainRepository.findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(
                userAccount.getAccountId(),
                Constants.EntityTypes.TEAM,
                taskDb.getFkTeamId().getTeamId()
        );
        EntityPreference entityPreference = entityPreferenceService.fetchEntityPreference(
                Constants.EntityTypes.ORG,
                taskDb.getFkOrgId().getOrgId()
        );

        List<Integer> allowedRoles = (entityPreference != null &&
                entityPreference.getStarringWorkItemRoleIdList() != null &&
                !entityPreference.getStarringWorkItemRoleIdList().isEmpty())
                ? entityPreference.getStarringWorkItemRoleIdList()
                : Constants.DEFAULT_ROLE_IDS_FOR_STARRING_WORK_ITEM;

        Integer userRoleToModify = getAllowedRole(userRoles, allowedRoles)
                .orElseThrow(() -> new ValidationFailedException("You do not have permission to add/remove the flag of work item"));

        Task task = new Task();
        BeanUtils.copyProperties(taskDb, task);

        if (requestIsStarred) {
            taskHistoryService.addTaskHistoryOnUserUpdate(task);
            task.setIsStarred(true);
            task.setFkAccountIdStarredBy(userAccount);
            taskRepository.save(task);
            return "Work item is successfully marked as flagged";
        } else {
            return unstarWorkItem(task, userRoleToModify, allowedRoles);
        }
    }

    private String unstarWorkItem(Task task, Integer modifierRole, List<Integer> allowedRoles) {
        if (!Boolean.TRUE.equals(task.getIsStarred()) || task.getFkAccountIdStarredBy() == null) {
            task.setIsStarred(false);
            task.setFkAccountIdStarredBy(null);
            taskRepository.save(task);
            return "Work item is successfully un-flagged";
        }

        List<Integer> rolesOfStarredByUser = accessDomainRepository.findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(
                task.getFkAccountIdStarredBy().getAccountId(),
                Constants.EntityTypes.TEAM,
                task.getFkTeamId().getTeamId()
        );

        boolean hasPermission = hasPermissionToModifyStar(modifierRole, rolesOfStarredByUser, allowedRoles);

        if (!hasPermission) {
            throw new ValidationFailedException("You do not have permission to remove flag from work item");
        }

        taskHistoryService.addTaskHistoryOnUserUpdate(task);
        task.setIsStarred(false);
        task.setFkAccountIdStarredBy(null);
        taskRepository.save(task);
        return "Work item is successfully un-flagged";
    }

    private Optional<Integer> getAllowedRole(List<Integer> userRoles, List<Integer> allowedRoles) {
        return userRoles == null ? Optional.empty()
                : userRoles.stream()
                .filter(allowedRoles::contains)
                .findFirst();
    }

    private boolean hasPermissionToModifyStar(Integer modifierRole, List<Integer> starredByRoles, List<Integer> allowedRoles) {
        if (starredByRoles == null || starredByRoles.isEmpty()) {
            return true;
        }

        Optional<Integer> starredRole = starredByRoles.stream()
                .filter(allowedRoles::contains)
                .findFirst();

        return starredRole.isEmpty() || modifierRole >= starredRole.get();
    }

    public void validateAccessToMarkWorkItemStarred (Task task, String accountIds) {
        if (task.getIsStarred() != null && task.getIsStarred()) {
            List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
            EntityPreference entityPreference = entityPreferenceService.getEntityPreference(Constants.EntityTypes.ORG, task.getFkOrgId().getOrgId());
            List<Integer> allowedRoles = (entityPreference != null &&
                    entityPreference.getStarringWorkItemRoleIdList() != null &&
                    !entityPreference.getStarringWorkItemRoleIdList().isEmpty())
                    ? entityPreference.getStarringWorkItemRoleIdList()
                    : Constants.DEFAULT_ROLE_IDS_FOR_STARRING_WORK_ITEM;

            if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), accountIdList, true, allowedRoles)) {
                throw new ValidationFailedException("You do not have valid role to flag the work item");
            }
        }
    }

    public String buildErrorMessageForNotAccessToUpdate(String roleNameOfUpdatedTaskUser, String taskNumber, List<String> updatedFields) {
        if (updatedFields != null && !updatedFields.isEmpty()) {
            String restrictedFields = updatedFields.stream()
                    .map(field -> Constants.TaskFieldNames.getOrDefault(field, field))
                    .collect(Collectors.joining(", "));
            return String.format(
                    "Your role in this team is \"%s\" and this role does not have authority to update some of the following fields in Work Item number \"%s\": %s",
                    roleNameOfUpdatedTaskUser, taskNumber, restrictedFields
            );
        } else {
            return String.format(
                    "Your role in this team is \"%s\" and this role does not have authority to update any fields in Work Item number \"%s\".",
                    roleNameOfUpdatedTaskUser, taskNumber
            );
        }
    }

    public void updateMeetingDateOnTaskMovementBetweenSprint (Task task) {
        List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());
        if (meetingList != null && !meetingList.isEmpty()) {
            for (Meeting meeting : meetingList) {
                if (task.getTaskActStDate() != null && meeting.getAttendeeList().parallelStream().anyMatch(attendee -> attendee.getAttendeeDuration() != null)) {
                    continue;
                }
                throw new ValidationFailedException("To proceed, please remove the reference meeting " + meeting.getMeetingNumber() +
                        " from the work item " + task.getTaskNumber() + " or remove the work item from the sprint, make appropriate changes" +
                        " to the dates of the work item and/or the meeting, and then move that work item to another sprint.");
            }
        }
    }

    public void validateCustomEnvironmentForBug(Long teamId, Integer customEnvironmentId) {
        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(teamId);
        boolean environmentExists = customEnvironmentRepository.existsByEntityTypeIdAndEntityIdAndIsActiveAndCustomEnvironmentId(
                Constants.EntityTypes.ORG, orgId, true, customEnvironmentId
        );
        if (!environmentExists) {
            throw new ValidationFailedException ("Environment is not available in the organization");
        }
    }
}

