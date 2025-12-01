package com.tse.core_application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.AiMLDtos.AiDuplicateWorkItemDto;
import com.tse.core_application.dto.AiMLDtos.AiWorkItemDescResponse;
import com.tse.core_application.dto.duplicate_task.DuplicateTaskRequest;
import com.tse.core_application.dto.duplicate_task.DuplicateTaskResponse;
import com.tse.core_application.exception.*;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.IEMailService;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.InvalidRelationTypeException;
import javax.naming.TimeLimitExceededException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/task")
public class TasksController {
    private static final Logger logger = LogManager.getLogger(TasksController.class.getName());
    @Autowired
    IEMailService emailService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private TableColumnsTypeService tableColumnsTypeService;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private ActionService actionService;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentService commentService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PersonalTaskService personalTaskService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private StatsService statsService;
    @Autowired
    private UserService userService;
    @Autowired
    private OpenFireService openFireService;
    @Autowired
    NotificationService notificationService;
    @Autowired
    private DependencyService dependencyService;
    @Autowired
    private RecurrenceService recurrenceService;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private AiMlService aiMlService;
    @Value("${enable.openfire}")
    private Boolean enableOpenfire;


    /*  giving wrong list of tasks because it is using the same method of getStats api for filtering the list of tasks
     * for a given status  - filterTasksForStats() */
    @Deprecated
    @PostMapping(path = "/getTasks")
    public ResponseEntity<Object> getTasks(@Valid @RequestBody StatsRequest taskListRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTasks" + '"' + " method ...");

        List<Task> listTask;
        try {
            listTask = taskService.getTasks(taskListRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTasks" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Not able to execute getTasks", e);
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, listTask);
    }

    /* this api is no longer in use because this api is merged with getTaskByFilter api.This api will now act as the subset
     * of getTaskByFilter api. So getTaskByFilter api will be used for task master as well as to get the details of the
     * task for the particular status. */
    @Deprecated
    @PostMapping(path = "/getAllTaskStatusDetails/{taskStatus}")
    public ResponseEntity<Object> getAllTaskStatusDetails(@PathVariable(name = "taskStatus") String taskStatus, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestBody StatsRequest taskListRequest, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userRepository.findByPrimaryEmail(jwtUtil.getUsernameFromToken(jwtToken)).getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllTaskStatusDetails" + '"' + " method ...");

        List<TaskMaster> allTasksStatusDetails;
        try {
            allTasksStatusDetails = taskService.getTaskDetailsForStatus(taskListRequest, timeZone, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllTaskStatusDetails" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Not able to execute getAllTaskStatusDetails", e);
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, allTasksStatusDetails);
    }

    @Deprecated
    @PostMapping(path = "/tempAdd")
    public ResponseEntity<Object> tempAdd(@RequestBody TaskHistory th, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " tempAdd" + '"' + " method ...");

        Object temp = null;
        try {
            temp = taskService.tempAdd(th);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " tempAdd" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute tempAdd" + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, temp);
    }

    @Deprecated
    @GetMapping(path = "/getTaskHistory")
    public ResponseEntity<Object> getStats(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getStats" + '"' + " method ...");

        List<TaskHistory> listTaskHistory;
        try {
            listTaskHistory = taskService.getAllTaskHistory();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getStats" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getStats" + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, listTaskHistory);
    }

    @PostMapping(path = "/getTask")
    public ResponseEntity<Object> getTask(@Valid @RequestBody TaskNumberRequest taskNumberRequest,
                                          @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone,
                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTask" + '"' + " method ...");

        TaskResponse taskResponse = null;
        String taskNumber = "";
        try {
            taskResponse = taskService.getTaskWithMeetingsByTaskNumber(taskNumberRequest, timeZone, accountIds);
            taskNumber = taskResponse.getTask().getTaskNumber();


            if (taskResponse != null) {
                boolean isViewTaskAllowed = taskService.isTaskViewAllowed(taskResponse.getTask(), accountIds);
                if (isViewTaskAllowed) {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + " getTask" + '"' + " method because completed successfully ...");
                    ThreadContext.clearMap();



                } else {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskViewException());
                    logger.error(request.getRequestURI() + " API: " + "Task view not allowed for username = " + foundUser.getPrimaryEmail() + " ,   " + "taskNumber = " + taskNumber, new Throwable(allStackTraces));
                    throw new TaskViewException();
                }
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskNotFoundException());
                logger.error(request.getRequestURI() + " API: " + "No task found for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskNumber = " + taskNumber, new Throwable(allStackTraces));
                throw new TaskNotFoundException();
            }
        } catch (Exception e) {
            if (e instanceof TaskViewException) {
                logger.error("Task view not allowed to username: "+foundUser.getPrimaryEmail()+" for task: "+taskNumber);
                throw new TaskViewException(); // need to show message on front end: 'Task not found'
            } else {
                if (e instanceof TaskNotFoundException) {
                    logger.error("Task not found with tasknumber: "+taskNumber);
                    throw new TaskNotFoundException();
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the task for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskNumber = " + taskNumber + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskResponse);
    }

    @PostMapping(path = "/addTask")
    @Transactional
    public ResponseEntity<Object> addTask(@Valid @RequestBody Task task, @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addTask" + '"' + " method ...");

        boolean isTaskValidated, result, isTaskTypeValidated;
        ArrayList<Integer> roleIds = null;
        try {
            taskServiceImpl.validateAccounts(task);
            removeLeadingAndTrailingSpacesForTask(task);
            if(task.getIsToCreateDuplicateTask() != null && !task.getIsToCreateDuplicateTask() && task.getTaskTitle() != null && task.getTaskDesc() != null) {
                AiDuplicateWorkItemDto duplicatesWorkItem = aiMlService.isWorkItemCreationIsDuplicate(new AiWorkItemDescResponse(task.getTaskTitle(), task.getTaskDesc(), task.getFkTeamId().getTeamId()), Long.valueOf(accountIds), screenName, timeZone, jwtToken);
                if(duplicatesWorkItem != null && duplicatesWorkItem.getResults() != null && !duplicatesWorkItem.getResults().isEmpty()) {
                    return CustomResponseHandler.generateCustomResponseForCustom(HttpCustomStatus.DUPLICATE_WORK_ITEM, HttpCustomStatus.DUPLICATE_WORK_ITEM.reasonPhrase(), duplicatesWorkItem);
                }
            }
            List<Integer> workItemStatusIdList = Constants.WORK_ITEM_STATUS_ID_LIST;
            if (task.getFkWorkflowTaskStatus() != null && !workItemStatusIdList.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                throw new IllegalAccessException("Work Item status is not valid");
            }
            taskServiceImpl.modifyNewTaskProperties(task, timeZone);
            isTaskValidated = taskServiceImpl.validateNewTaskProperties(task);
            roleIds = accessDomainService.getEffectiveRolesByAccountId(task.getFkAccountId().getAccountId(),Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
        }
        catch (Exception e) {
            e.printStackTrace();
            if (e instanceof TaskEstimateException) {
                throw e;
            } else {
                if (e instanceof ValidationFailedException) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a task for username = " + foundUser.getPrimaryEmail()+ "ERROR :" +e.getMessage() + "Caught Exception" + e ,  new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw e;
                } else {
                    if (e instanceof DateAndTimePairFailedException) {
                        throw e;
                    } else {
                        if (e instanceof WorkflowTypeDoesNotExistException) {
                            throw new WorkflowTypeDoesNotExistException();
                        } else {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a task for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                        }
                    }
                }
            }
        }
        if ((roleIds != null && roleIds.isEmpty()) || !isTaskValidated) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addTask" + '"' + " method because username = " + foundUser.getPrimaryEmail() + " does not have roles or task validation fails.");
            ThreadContext.clearMap();
            throw new ForbiddenException("does not have roles or task validation fails.");
        } else {
            try {
                taskServiceImpl.validateTaskAndAddToSprint(task, accountIds);
                taskServiceImpl.validateTaskReferenceWorkItem(task);
                taskServiceImpl.validateDependencyWithReferenceWorkItemId(task);
                taskServiceImpl.validateAccessToMarkWorkItemStarred(task, accountIds);
                if (task.getIsStarred() != null && task.getIsStarred()) {
                    task.setFkAccountIdStarredBy(task.getFkAccountId());
                }
                if(task.getIsBug() != null && task.getIsBug()) {
                    taskServiceImpl.validateBugTaskRelationWithReferenceWorkItemId(task);
                    taskServiceImpl.validateBugTaskRelationWithDependencyWorkItem(task);
                }
                if(task.getFkEpicId() != null) {
                    taskServiceImpl.validateCreateTaskWithEpicAndModifyProperties(task, accountIds);
                }
                taskServiceImpl.validateExpDateTimeWithEstimate (task);
                if (task.getFkAccountIdAssigned() == null || task.getFkAccountIdAssigned().getAccountId() == null) {
                    result = actionService.isInAction(roleIds, Constants.Task_Add);
                } else {
                    if (task.getFkAccountIdAssigned().getAccountId().equals(task.getFkAccountId().getAccountId())) {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Self_Assignment);
                    } else {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Assignment_Others);
                    }
                }
                if (result) {
                    try {
                        Task taskUpdated = taskService.initializeTaskNumberSetProperties(task);
                        Task taskAdd = taskServiceImpl.addTaskInTaskTable(taskUpdated, timeZone);
                        taskServiceImpl.updateReferenceWorkItem(taskAdd);
                        taskService.setReferencedTaskDetailsResponse(taskAdd);
                        if(taskAdd.getFkEpicId() != null) {
                            taskServiceImpl.setEpicInCreateTask(taskAdd);
                        }
                        if (enableOpenfire) {
                            new Thread(() -> {
                                try {
                                    openFireService.setUpChatRoomWithGroup(taskAdd);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                    logger.error("Could not create chat group/ chat room for task# " + taskAdd.getTaskNumber() + "Caught Exception: " + e, new Throwable(allStackTraces));
                                }
                            }).start();
                        }

                        long estimatedTime = System.currentTimeMillis() - startTime;
                        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                        logger.info("Exited" + '"' + " addTask" + '"' + " method because completed successfully ... ");
                        ThreadContext.clearMap();
                        aiMlService.sendWorkItemDetailOnCreationAndUpdating(taskAdd, true, Long.valueOf(accountIds), screenName, timeZone, jwtToken);
                        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskAdd);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a Work Item for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                    }
                } else {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + "addTask" + '"' + " method because user does not have action to create the Work Item ...");
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException("user does not have action to create a Work Item."));
                    logger.error(request.getRequestURI() + " API: " + "Not able to create a Work Item for username = " + foundUser.getPrimaryEmail() + " because user does not have" + " action to create a task.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ForbiddenException("user does not have action to create a task.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a Work Item for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    /**
     * @return Task if task is created successfully else error message.
     * @Function: Create task quickly with less field requested.
     */

    @PostMapping(path = "/quickCreateTask")
    @Transactional
    public ResponseEntity<Object> quickCreateTask(@Valid @RequestBody QuickCreateTaskRequest quickCreateTaskRequest, @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "quickCreateTask " + '"' + " method ...");

        boolean isTaskValidatedByWorkflowStatus, isTaskValidatedForDateAndTimePairs, isTaskDataValidated, result;
        ArrayList<Integer> roleIds = null;
        Task task = taskServiceImpl.populateTask(quickCreateTaskRequest,accountIds);
        if(quickCreateTaskRequest.getIsToCreateDuplicateTask() != null && !quickCreateTaskRequest.getIsToCreateDuplicateTask() && task.getTaskTitle() != null && task.getTaskDesc() != null) {
            AiDuplicateWorkItemDto duplicatesWorkItem = aiMlService.isWorkItemCreationIsDuplicate(new AiWorkItemDescResponse(task.getTaskTitle(), task.getTaskDesc(), task.getFkTeamId().getTeamId()), Long.valueOf(accountIds), screenName, timeZone, jwtToken);
            if(duplicatesWorkItem != null && duplicatesWorkItem.getResults() != null && !duplicatesWorkItem.getResults().isEmpty()) {
                return CustomResponseHandler.generateCustomResponseForCustom(HttpCustomStatus.DUPLICATE_WORK_ITEM, HttpCustomStatus.DUPLICATE_WORK_ITEM.reasonPhrase(), duplicatesWorkItem);
            }
        }
        try {
            removeLeadingAndTrailingSpacesForTask(task);
            taskServiceImpl.validateAccounts(task);
            taskServiceImpl.setDefaultExpTime(task);
            taskServiceImpl.convertTaskAllUserDateAndTimeInToServerTimeZone(task, timeZone);
            taskServiceImpl.setTaskStateByWorkflowTaskStatus(task);
            List<Integer> workItemStatusIdList = Constants.WORK_ITEM_STATUS_ID_LIST;
            if (task.getFkWorkflowTaskStatus() != null && !workItemStatusIdList.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                throw new IllegalAccessException("Work Item status is not valid");
            }
            if(Objects.equals(task.getTaskTypeId(),Constants.TaskTypes.BUG_TASK)){
                taskServiceImpl.validateCustomEnvironmentForBug(task.getFkTeamId().getTeamId(),task.getEnvironmentId());
            }
            taskServiceImpl.validateAccessToMarkWorkItemStarred(task, accountIds);
            taskServiceImpl.updateCurrentlyScheduledTaskIndicatorForTask(task);
            isTaskValidatedByWorkflowStatus = taskServiceImpl.validateTaskByWorkflowStatus(task);
            isTaskValidatedForDateAndTimePairs = taskServiceImpl.validateAllDateAndTimeForPairs(task);
//            taskServiceImpl.validateTaskReferenceEntityIdByTaskTypeId(task);
            roleIds = accessDomainService.getEffectiveRolesByAccountId(task.getFkAccountId().getAccountId(),Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
        }
        catch (Exception e) {
            e.printStackTrace();
            if (e instanceof TaskEstimateException) {
                throw e;
            } else {
                if (e instanceof ValidationFailedException) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a Work Item for username = " + foundUser.getPrimaryEmail()+ "ERROR :" +e.getMessage() + "Caught Exception" + e ,  new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw e;
                } else {
                    if (e instanceof DateAndTimePairFailedException) {
                        throw e;
                    } else {
                        if (e instanceof WorkflowTypeDoesNotExistException) {
                            throw new WorkflowTypeDoesNotExistException();
                        } else {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a Work Item for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                        }
                    }
                }
            }
        }
        if ((roleIds != null && roleIds.isEmpty()) || !isTaskValidatedByWorkflowStatus || !isTaskValidatedForDateAndTimePairs) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "quickCreateTask" + '"' + " method because username = " + foundUser.getPrimaryEmail() + " does not have roles or Work Item validation fails.");
            ThreadContext.clearMap();
            throw new ForbiddenException("does not have roles or Work Item validation fails.");
        } else {
            try {
                taskServiceImpl.validateTaskAndAddToSprint(task, accountIds);
                taskServiceImpl.validateTaskReferenceWorkItem(task);
                taskServiceImpl.validateDependencyWithReferenceWorkItemId(task);
                if(task.getFkEpicId() != null) {
                    taskServiceImpl.validateCreateTaskWithEpicAndModifyProperties(task, accountIds);
                }
                taskServiceImpl.validateExpDateTimeWithEstimate (task);
                if (task.getFkAccountIdAssigned() == null || task.getFkAccountIdAssigned().getAccountId() == null) {
                    result = actionService.isInAction(roleIds, Constants.Task_Add);
                } else {
                    if (task.getFkAccountIdAssigned().getAccountId().equals(task.getFkAccountId().getAccountId())) {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Self_Assignment);
                    } else {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Assignment_Others);
                    }
                }
                if (result) {
                    try {
                        Task taskUpdated = taskService.initializeTaskNumberSetProperties(task);
                        Task taskAdd = taskServiceImpl.addTaskInTaskTable(taskUpdated, timeZone);
                        taskServiceImpl.updateReferenceWorkItem(taskAdd);
                        taskService.setReferencedTaskDetailsResponse(taskAdd);
                        if(taskAdd.getFkEpicId() != null) {
                            taskServiceImpl.setEpicInCreateTask(taskAdd);
                        }
                        if (enableOpenfire) {
                            new Thread(() -> {
                                try {
                                    String chatRoomName = openFireService.createChatRoomForTask(taskAdd);
                                    String chatGroupName = openFireService.createChatGroupForTask(taskAdd);
                                    openFireService.addGroupToChatRoom(chatRoomName, chatGroupName);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                    logger.error("Could not create chat group/ chat room for task# " + taskAdd.getTaskNumber() + "Caught Exception: " + e, new Throwable(allStackTraces));
                                }
                            }).start();
                        }
                        Task taskResponse = new Task();
                        BeanUtils.copyProperties(taskAdd, taskResponse);
                        taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse, timeZone);
                        long estimatedTime = System.currentTimeMillis() - startTime;
                        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                        logger.info("Exited" + '"' + "quickCreateTask" + '"' + " method because completed successfully ... ");
                        ThreadContext.clearMap();
                        aiMlService.sendWorkItemDetailOnCreationAndUpdating(taskResponse, true, Long.valueOf(accountIds), screenName, timeZone, jwtToken);
                        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a quick Work Item for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                    }
                } else {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + "quickCreateTask"+ '"' + " method because user does not have action to create the Work Item ...");
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException("user does not have action to create a task."));
                    logger.error(request.getRequestURI() + " API: " + "Not able to create a quick Work Item for username = " + foundUser.getPrimaryEmail() + " because user does not have" + " action to create a task.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ForbiddenException("user does not have action to create a task.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a quick Work Item for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }


    @PostMapping(path = "/updateTask/{taskId}")
    @Transactional
    public ResponseEntity<Object> updateTask(@Valid @RequestBody Task task, @PathVariable Long taskId, @RequestParam(required = false) Boolean useSystemDerivedForChild,
                                             @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                             @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws NoSuchFieldException, IllegalAccessException {

        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateTask" + '"' + " method ...");
        Task updatedTask = new Task();
        try {
            removeLeadingAndTrailingSpacesForTask(task);
            updatedTask = taskServiceImpl.updateTask(task, taskId, useSystemDerivedForChild, accountIds, timeZone, foundUser.getPrimaryEmail(), request.getRequestURI(), jwtToken);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update tasks for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, updatedTask);

    }

    @GetMapping(path = "/getHistory/{taskId}")
    public ResponseEntity<Object> getHistoryFromTaskHistoryTable(@PathVariable long taskId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getHistoryFromTaskHistoryTable" + '"' + " method ...");

        ArrayList<HashMap<String, Object>> arraylist;
        try {
            arraylist = taskServiceImpl.findHistoryFromTaskHistoryTable(taskId);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getHistoryFromTaskHistoryTable() for username = " + foundUser.getPrimaryEmail() + " ,   " + "taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        if (arraylist.isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error(request.getRequestURI() + " API: " + "No Data found for username = " + foundUser.getPrimaryEmail() + " ,   taskId = " + taskId, new Throwable(allStackTraces));
            throw new NoDataFoundException();
        } else {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getHistoryFromTaskHistoryTable" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, arraylist);
        }
    }

//    @GetMapping(path = "/tempSendEmail/{email}")
//    public String tempEmailEndpoint(@PathVariable String email) {
//        String otp = emailService.sendOtp(email, "Sample message", "Sample subject");
//        return otp;
//    }

    @PostMapping(path = "/deleteTask/{taskId}")
    public ResponseEntity<Object> deleteTask(@PathVariable Long taskId, @RequestBody DeleteWorkItemRequest deleteWorkItemRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " deleteTask" + '"' + " method ...");

        try {
            Task taskDelete = taskServiceImpl.findTaskByTaskId(taskId);
            if (taskDelete == null) {
                throw new TaskNotFoundException();
            }
            if (!Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE, taskDelete.getFkWorkflowTaskStatus().getWorkflowTaskStatus()) &&
                    !Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, taskDelete.getFkWorkflowTaskStatus().getWorkflowTaskStatus()) &&
                    !Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, taskDelete.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                if (deleteWorkItemRequest.getDeleteReasonId() == null) {
                    throw new ValidationFailedException("Please the reason for delete the work item");
                }
            }
            taskServiceImpl.validateDeleteReason (taskDelete, deleteWorkItemRequest);
            String response = taskServiceImpl.deleteTaskByTaskId(taskId, taskDelete,accountIds,timeZone, false, deleteWorkItemRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exiting" + " " + '"' + " deleteTask" + '"' + " method because Work Item with" + "taskId" + taskId + " has been deleted successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof TaskNotFoundException) {
                logger.error(request.getRequestURI() + " API: " + "Work Item not found for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + taskId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                throw e;
            } else if (e instanceof DeleteTaskException) {
                logger.error(request.getRequestURI() + " API: " + "Not allowed to delete Work Item for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + taskId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                throw e;
            } else {
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete a Work Item for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + taskId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @GetMapping(path = "/getAllUsersToAssignTask/{orgId}/{teamId}")
    public ResponseEntity<Object> getAllUsersToAssignTask(@PathVariable(name = "orgId") Long orgId,
                                                          @PathVariable(name = "teamId") Long teamId,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getAllUsersToAssignTask" + '"' + " method ...");

        try {
            List<EmailFirstLastAccountIdIsActive> emailFirstLastAccountIdIsActiveList = taskService.getAllUsersAssignForTask(orgId, teamId, foundUser);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllUsersToAssignTask" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, emailFirstLastAccountIdIsActiveList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the users to assign a Work Item for username = " + foundUser.getPrimaryEmail() + " for teamId = " + teamId + " in organization = " + orgId + " ,      " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping(path = "/getAllUsersToObserveTask/{orgId}/{teamId}")
    public ResponseEntity<Object> getAllUsersToObserveTask(@PathVariable(name = "orgId") Long orgId, @PathVariable(name = "teamId") Long teamId, @RequestParam(name = "taskId", required = false) Long taskId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getAllUsersToObserveTask" + '"' + " method ...");

        try {
            List<EmailFirstLastAccountIdIsActive> emailFirstLastAccountIdIsActiveList = taskService.getAllUsersObserverForTask(orgId, teamId, taskId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllUsersToObserveTask" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, emailFirstLastAccountIdIsActiveList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the users to observe a Work Item for username = " + foundUser.getPrimaryEmail() + " for teamId = " + teamId + " in organization = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping(path = "/getAllUsersToMentorTask/{orgId}/{teamId}")
    public ResponseEntity<Object> getAllUsersToMentorTask(@PathVariable(name = "orgId") Long orgId, @PathVariable(name = "teamId") Long teamId, @RequestParam(name = "taskId", required = false) Long taskId,
                                                          @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getAllUsersToMentorTask" + '"' + " method ...");

        try {
            List<EmailFirstLastAccountIdIsActive> response = taskService.getAllUsersMentorForTask(orgId, teamId, taskId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllUsersToMentorTask" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the users to mentor a Work Item for username = " + foundUser.getPrimaryEmail() + " for teamId = " + teamId + " in organization = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping(path = "/getTaskByFilter")
    public ResponseEntity<Object> getTaskByFilter(@RequestBody StatsRequest statsRequest,
                                                  @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
                                                  @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize,
                                                  @RequestHeader(name = "screenName") String screenName,
                                                  @RequestHeader(name = "timeZone") String timeZone,
                                                  @RequestHeader(name = "accountIds") String accountIds,
                                                  HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getTaskByFilter" + '"' + " method ...");

        try {
            if (statsRequest.getSprintId() != null && (statsRequest.getFromDate() != null || statsRequest.getFromDateType() != null || statsRequest.getToDate() != null || statsRequest.getToDateType() != null || statsRequest.getNoOfDays() != null)) {
                throw new ValidationFailedException("Date(s) and/or Date Range can't be chosen with a sprint as the spring itself has a date range.");
            }
            TaskByFilterResponse taskByFilterResponse = new TaskByFilterResponse();
            List<TaskMaster> taskMastersList = new ArrayList<>();
            List<TaskMaster> personalTaskMastersList = new ArrayList<>();
            taskMastersList = taskService.getAllTaskByFilter(statsRequest, accountIds, timeZone);
            if(statsRequest.getTaskTypeList()==null ||  statsRequest.getTaskTypeList().isEmpty()) {
                personalTaskMastersList = personalTaskService.getAllFilteredTaskForPersonalUser(statsRequest, accountIds, timeZone);
                taskMastersList.addAll(personalTaskMastersList);
            }
            taskMastersList = taskService.sortTaskMaster(statsRequest, taskMastersList);

            if (statsRequest.getSearches() != null && !statsRequest.getSearches().isEmpty()) {
                taskMastersList = taskService.filterBySearch(taskMastersList, statsRequest.getSearches());
            }

            Integer taskListSize = taskMastersList.size();
            if (statsRequest.getHasPagination()) {
                taskMastersList = taskService.getTasksWithPagination(taskMastersList, pageNumber, pageSize);
            }
            taskByFilterResponse = taskService.getTaskByFilterResponse(statsRequest, taskMastersList, pageNumber, pageSize, taskListSize);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTaskByFilter" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskByFilterResponse);
        } catch (Exception e) {
            if (e instanceof InvalidStatsRequestFilterException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getTaskByFilter() for username = " + foundUser.getPrimaryEmail() + " ,      " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @PostMapping(path = "/getUsersByAllTeamAndOrg")
    public ResponseEntity<Object> getUsersByAllTeamAndOrg(@RequestBody AllTeamAndOrg allTeamAndOrg, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getUsersByAllTeamAndOrg" + '"' + " method ...");
        try {
            List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
            List<EmailNameOrgCustomModel> emailFirstLastAccountIdList = taskService.getAllUsersByAllTeamAndOrg(allTeamAndOrg, accountIdList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getUsersByAllTeamAndOrg" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, emailFirstLastAccountIdList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getUsersByAllTeamAndOrg() for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    //  to get all the immediate attention users of a task
    @GetMapping(path = "/getAllImmediateAttentionUsers/{orgId}/{teamId}")
    public ResponseEntity<Object> getAllImmediateAttentionUsers(@PathVariable(name = "orgId") Long orgId, @PathVariable(name = "teamId") Long teamId, @RequestParam(name = "taskId", required = false) Long taskId, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " getAllImmediateAttentionUsers" + '"' + " method ...");

        try {
            List<EmailFirstLastAccountId> emailFirstLastAccountIdList = taskService.getAllImmediateAttentionUsersForTask(orgId, teamId, taskId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllImmediateAttentionUsers" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, emailFirstLastAccountIdList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all immediate attention users for a Work Item for username = " + foundUser.getPrimaryEmail() + " for teamId = " + teamId + " in organization = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

    }

    @PostMapping(path="/getScheduledTasks")
    public ResponseEntity<Object> getScheduledTasks(@RequestBody ScheduledTasksViewRequest scheduledTasksViewRequest,
                                                  @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                  @RequestHeader(name = "accountIds") String accountIds,
                                                    @RequestHeader(name="userId") String userId, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        Long userIdLong = requestHeaderHandler.getUserIdFromRequestHeader(userId);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", userIdLong.toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getScheduledTasks" + '"' + " method ...");

        List<ScheduledTaskViewResponse> activeTasks = null;

        try {
            if (scheduledTasksViewRequest.getOrgId() != null || scheduledTasksViewRequest.getBuId() != null || scheduledTasksViewRequest.getProjectId() != null || scheduledTasksViewRequest.getTeamId() != null) {
                activeTasks = taskService.getAllScheduledTasksByFilter(userIdLong, scheduledTasksViewRequest);
            } else {
                activeTasks = taskService.getAllScheduledTasksForUser(userIdLong);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getScheduledTasks" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong in fetching scheduled tasks" + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;

        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, activeTasks);
    }

    // created by Kunal but was missed in a merge -- added by Mohan later
    @PostMapping(path="/setCurrentlyActiveTasks")
    @Transactional
    public ResponseEntity<Object> setCurrentlyActiveTasks(@RequestBody AllScheduledTaskIdsRequest allScheduledTaskIdsRequest,
                                                          @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " setCurrentlyActiveTasks" + '"' + " method ...");

        ScheduledTaskViewResponse responseTasks = new ScheduledTaskViewResponse();

//        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

        try{

            responseTasks  = taskService.setCurrentActivityIndicatorForTask(allScheduledTaskIdsRequest, timeZone,accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " setCurrentlyActiveTasks" + '"' + " method because completed successfully...");
            ThreadContext.clearMap();

        }
        catch(Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof ValidationFailedException) {
                logger.error(request.getRequestURI() + "Warning : " + e.getMessage(), new Throwable(allStackTraces));
                return CustomResponseHandler.generateCustomResponse(HttpStatus.ACCEPTED, com.tse.core_application.constants.Constants.FormattedResponse.WARNING, " You already have at least 2 active tasks");

            } else {
                e.printStackTrace();
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getTaskByFilter() for username = " + foundUser.getPrimaryEmail() + " ,      " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }

        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, responseTasks);


    }

    @GetMapping(path = "/getTaskRecordedEffort/{taskId}")
    public ResponseEntity<Object> getTaskRecordedEffort(@PathVariable(name = "taskNumber") Long taskId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTaskRecordedEffort" + '"' + " method ...");

        HashMap<String, List<TotalEffortByDate>> EffortByDateList = new HashMap<>();
        try {
            EffortByDateList = taskServiceImpl.getRecordedEffortByTaskNumberAndTotalEffort(taskId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTaskRecordedEffort" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the recorded effort list for task Id = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, EffortByDateList);
    }

    @PostMapping(path = "/addChildTask")
    @Transactional
    public ResponseEntity<Object> addChildTask(@Valid @RequestBody ChildTaskRequest childTaskRequest, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addChildTask" + '"' + " method ...");

        boolean isTaskValidated, result, isTaskTypeValidated;
        ArrayList<Integer> roleIds = null;
        Task task = new Task();
        try {
            if(childTaskRequest.getTaskExpEndDate()!=null && childTaskRequest.getTaskExpStartDate()!=null){
                childTaskRequest.setTaskExpEndDate(DateTimeUtils.convertUserDateToServerTimezone(childTaskRequest.getTaskExpEndDate(), timeZone));
                childTaskRequest.setTaskExpStartDate(DateTimeUtils.convertUserDateToServerTimezone(childTaskRequest.getTaskExpStartDate(), timeZone));
            }
            Task parentTask = taskRepository.findByTaskId(childTaskRequest.getParentTaskId());
            if(childTaskRequest.getIsToCreateDuplicateTask() != null && !childTaskRequest.getIsToCreateDuplicateTask() && childTaskRequest.getTaskTitle() != null && childTaskRequest.getTaskDesc() != null) {
                AiDuplicateWorkItemDto duplicatesWorkItem = aiMlService.isWorkItemCreationIsDuplicate(
                        new AiWorkItemDescResponse(childTaskRequest.getTaskTitle(), childTaskRequest.getTaskDesc(), parentTask.getFkTeamId().getTeamId())
                        , Long.valueOf(accountIds), screenName, timeZone, jwtToken);
                if(duplicatesWorkItem != null && duplicatesWorkItem.getResults() != null && !duplicatesWorkItem.getResults().isEmpty()) {
                    return CustomResponseHandler.generateCustomResponseForCustom(HttpCustomStatus.DUPLICATE_WORK_ITEM, HttpCustomStatus.DUPLICATE_WORK_ITEM.reasonPhrase(), duplicatesWorkItem);
                }
            }
            task = taskServiceImpl.createNewChildTask(parentTask, childTaskRequest, accountIds);
            removeLeadingAndTrailingSpacesForTask(task);
            taskServiceImpl.modifyNewChildTaskProperties(task);
            isTaskValidated = taskServiceImpl.validateNewChildTaskProperties(task);
            taskServiceImpl.validateAccessToMarkWorkItemStarred(task, accountIds);
            roleIds = accessDomainService.getEffectiveRolesByAccountId(task.getFkAccountId().getAccountId(),Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
        }
        catch (Exception e) {
            e.printStackTrace();
            if (e instanceof TaskEstimateException) {
                throw e;
            } else {
                if (e instanceof ValidationFailedException) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a child task for username = " + foundUser.getPrimaryEmail()+ "ERROR :" +e.getMessage() + "Caught Exception" + e ,  new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ValidationFailedException(e.getMessage().substring(19));
                } else {
                    if (e instanceof DateAndTimePairFailedException) {
                        throw e;
                    } else {
                        if (e instanceof WorkflowTypeDoesNotExistException) {
                            throw new WorkflowTypeDoesNotExistException();
                        } else {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a child task for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                        }
                    }
                }
            }
        }
        if ((roleIds != null && roleIds.isEmpty()) || !isTaskValidated) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addChildTask" + '"' + " method because username = " + foundUser.getPrimaryEmail() + " does not have roles or Work Item validation fails.");
            ThreadContext.clearMap();
            throw new ForbiddenException("does not have roles or Work Item validation fails.");
        } else {
            try {
                taskServiceImpl.validateTaskReferenceWorkItem(task);
                taskServiceImpl.validateDependencyWithReferenceWorkItemId(task);
                if (task.getFkAccountIdAssigned() == null || task.getFkAccountIdAssigned().getAccountId() == null) {
                    result = actionService.isInAction(roleIds, Constants.Task_Add);
                } else {
                    if (task.getFkAccountIdAssigned().getAccountId().equals(task.getFkAccountId().getAccountId())) {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Self_Assignment);
                    } else {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Assignment_Others);
                    }
                }
                if (result) {
                    try {
                        Task taskUpdated = taskService.initializeTaskNumberSetProperties(task);
                        taskServiceImpl.validateExpDateTimeWithEstimate (task);
                        Task taskAdd = taskServiceImpl.addTaskInTaskTable(taskUpdated, timeZone);
                        taskServiceImpl.updateReferenceWorkItem(taskAdd);
                        taskService.setReferencedTaskDetailsResponse(taskAdd);
//                        ChildTaskResponse childTaskResponse = taskServiceImpl.createNewChildTaskResponse(taskAdd);

                        if (enableOpenfire) {
                            new Thread(() -> {
                                try {
                                    openFireService.setUpChatRoomWithGroup(taskAdd);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                    logger.error("Could not create chat group/ chat room for task# " + taskAdd.getTaskNumber() + "Caught Exception: " + e, new Throwable(allStackTraces));
                                }
                            }).start();
                        }
                        Task taskResponse = new Task();
                        BeanUtils.copyProperties(taskAdd, taskResponse);
                        taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse, timeZone);
                        long estimatedTime = System.currentTimeMillis() - startTime;
                        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                        logger.info("Exited" + '"' + " addChildTask" + '"' + " method because completed successfully ... ");
                        ThreadContext.clearMap();
                        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a task for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                    }
                } else {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + "addChildTask" + '"' + " method because user does not have action to create the Work Item ...");
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException("user does not have action to create a task."));
                    logger.error(request.getRequestURI() + " API: " + "Not able to create a Work Item for username = " + foundUser.getPrimaryEmail() + " because user does not have" + " action to create a task.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ForbiddenException("user does not have action to create a task.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a Work Item for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @PostMapping(path = "/getAllTaskWithAttention")
    public ResponseEntity<Object> getAllTaskWithAttention(@Valid @RequestBody AttentionRequest attentionRequest,
                                                          @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request){
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllTaskWithAttention" + '"' + " method ...");
        try {
            if (!attentionRequest.getUserName().isEmpty() && !attentionRequest.getTeamList().isEmpty()) {
                User user = userRepository.findByPrimaryEmail(attentionRequest.getUserName());
                List<Long> accountIdList = userAccountRepository.findAccountIdByFkUserIdUserIdAndIsActive(user, true);
                if(!notificationService.checkAccountIds(accountIdList,jwtToken)){
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + " getAllTaskWithAttention" + '"' + " method because completed successfully ...");
                    ThreadContext.clearMap();
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, com.tse.core_application.constants.Constants.FormattedResponse.VALIDATION_ERROR, "You are not authorized to view provided users attentions.");
                }
                List<AttentionResponse> taskList = taskServiceImpl.getAllTaskWithAttention(attentionRequest);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getAllTaskWithAttention" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskList);
            } else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getAllTaskWithAttention" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_ACCEPTABLE, com.tse.core_application.constants.Constants.FormattedResponse.VALIDATION_ERROR, "Invalid user name or teams.");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get tasks for immediate attention. Caught error: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, com.tse.core_application.constants.Constants.FormattedResponse.SERVER_ERROR, "Internal server error!");
        }
    }

    @PostMapping(path = "/getUpdatedTaskPreview")
    public ResponseEntity<Object> getUpdatedTaskPreview(@Valid @RequestBody TaskNumberRequest taskNumberRequest,
                                                        @RequestHeader(name = "screenName") String screenName,
                                                        @RequestHeader(name = "timeZone") String timeZone,
                                                        @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getUpdatedTaskPreview" + '"' + " method ...");

        Task foundTaskDb = null;
        String taskNumber = " ";

        try {
            foundTaskDb = taskService.getTaskByTaskNumber(taskNumberRequest, timeZone);

            if (foundTaskDb != null) {
                taskNumber = foundTaskDb.getTaskNumber();
                boolean isViewTaskAllowed = taskService.isTaskViewAllowed(foundTaskDb, accountIds);
                if (isViewTaskAllowed) {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + " getUpdatedTaskPreview" + '"' + " method because completed successfully ...");
                    ThreadContext.clearMap();
                } else {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskViewException());
                    logger.error(request.getRequestURI() + " API: " + "Task view not allowed for username = " + foundUser.getPrimaryEmail() + " ,   " + "taskNumber = " + taskNumber, new Throwable(allStackTraces));
                    throw new TaskViewException();
                }
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskNotFoundException());
                logger.error(request.getRequestURI() + " API: " + "No task found for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskNumber = " + taskNumber, new Throwable(allStackTraces));
                throw new TaskNotFoundException();
            }
        } catch (Exception e) {
            if (e instanceof TaskViewException) {
                logger.error("Work Item view not allowed to username: "+foundUser.getPrimaryEmail()+" for Work Item: "+taskNumber);
                throw new TaskNotFoundException(); // need to show message on front end: 'Task not found'
            } else {
                if (e instanceof TaskNotFoundException) {
                    logger.error("Work Item not found with Work Item number: "+taskNumber);
                    throw new TaskNotFoundException();
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the Work Item for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskNumber = " + taskNumber + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }
        }
        TaskPreview taskPreview = new TaskPreview();
        BeanUtils.copyProperties(foundTaskDb, taskPreview);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskPreview);
    }

    @GetMapping(path = "/getAllEnvironmentSeverityResolution/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getAllEnvironmentSeverityResolution(
            @PathVariable Integer entityTypeId,
            @PathVariable Long entityId,
            @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                               @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllEnvironmentSeverityResolution" + '"' + " method ...");

        EnvironmentSeverityResolutionResponse environmentSeverityResolutionResponse = null;
        try {
            environmentSeverityResolutionResponse = taskServiceImpl.getAllEnvironmentSeverityResolution(entityTypeId, entityId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllEnvironmentSeverityResolution" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllEnvironmentSeverityResolution() for the username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, environmentSeverityResolutionResponse);
    }

    /**
     * @return all the open tasks (not completed/ deleted) response where assignedTo is given accountId in the given entity (org/ team)
     */
    @PostMapping (path = "/getOpenTasksAssignedToUser")
    public ResponseEntity<Object> getOpenTasksAssignedToUser(@RequestBody OpenTaskAssignedToUserRequest openTaskAssignedToUserRequest, @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                             @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOpenTasksAssignedToUser" + '"' + " method ...");

        HashMap<Long, List<OpenTaskDetails>> teamWiseOpenTasks = new HashMap<>();
        try {
            teamWiseOpenTasks = taskServiceImpl.getOpenTasksAssignedToUser(openTaskAssignedToUserRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOpenTasksAssignedToUser" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOpenTasksAssignedToUser() for the username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, teamWiseOpenTasks);
    }

    @GetMapping(path = "/getDependencyGraphForTask/{taskId}")
    public ResponseEntity<Object> getDependencyGraphForTask(@PathVariable(name = "taskId") Long taskId, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getDependencyGraphForTask" + '"' + " method ...");

        Task foundTaskDb = null;
        DependencyGraph dependencyGraph = new DependencyGraph();
        try {
            foundTaskDb = taskRepository.findByTaskId(taskId);
            if (foundTaskDb != null) {
                dependencyGraph = dependencyService.getDependencyGraphForTask(taskId);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getDependencyGraphForTask" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, com.tse.core_application.constants.Constants.FormattedResponse.NOTFOUND, "No such task exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create dependency graph for the Work Item for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskNumber = " + foundTaskDb.getTaskNumber() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, dependencyGraph);
    }

    @GetMapping(path = "/getTasksByLabel")
    public ResponseEntity<Object> getTasksByLabel(@Valid @RequestBody TaskByLabelRequest taskByLabelRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTasksByLabel" + '"' + " method ...");
        List<TaskByLabelResponse> taskByLabelResponses = new ArrayList<>();
        try {
            taskByLabelResponses = taskServiceImpl.findTasksByLabels(taskByLabelRequest.getLabels(), taskByLabelRequest.getTeamId(), taskByLabelRequest.getAccountIdAssigned());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTasksByLabel" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get labels of a user for the username = " + username + "for teamId: " + taskByLabelRequest.getTeamId() +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskByLabelResponses);
    }

    @Transactional
    @PostMapping(path = "/createDuplicateTask")
    public ResponseEntity<Object> createDuplicateTask(@RequestBody DuplicateTaskRequest duplicateTaskRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " createDuplicateTask" + '"' + " method ...");

        DuplicateTaskResponse resultTask = null;
        try {
            resultTask = taskService.createDuplicateTask(duplicateTaskRequest, timeZone, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " createDuplicateTask" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            if (e instanceof TaskNotFoundException) {
                logger.error("Work Item not found with tasknumber: "+duplicateTaskRequest.getTaskNumber());
                throw new TaskNotFoundException();
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create duplicate Work Item for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskNumber = " + duplicateTaskRequest.getTaskNumber() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, resultTask);
    }

    @GetMapping(path = "/getTaskAllEfforts/{taskId}")
    public ResponseEntity<Object> getTaskAllEfforts(@PathVariable(name = "taskId") Long taskId,
                                                    @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTaskAllEfforts" + '"' + " method ...");

        List<RecordEffortResponse> recordedEffortsList = new ArrayList<>();
        try {
            recordedEffortsList = taskServiceImpl.getAllEffortsListByTaskNumber(taskId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTaskAllEfforts" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the effort list for taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, recordedEffortsList);
    }

    @PostMapping(path = "/editRecordedEffort/{timeTrackingId}")
    public ResponseEntity<Object> editRecordedEffort(@PathVariable(name = "timeTrackingId") Long timeTrackingId,@Valid @RequestBody NewEffortTrack newEffortTrack, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws TimeLimitExceededException, IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " editRecordedEffort" + '"' + " method ...");

        String message = null;
        try {
            List<Long> accountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
            message = taskServiceImpl.editRecordedEffortsAndUpdateTimesheet(timeTrackingId, accountIdsList, timeZone, newEffortTrack);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " editRecordedEffort" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to edit the recorded efforts " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, message);
    }

    @PostMapping("/exportToCSV")
    public ResponseEntity<ByteArrayResource> exportToCsv(@RequestBody StatsRequest statsRequest, @RequestParam(name = "pageNumber", defaultValue = "0", required = false) Integer pageNumber, @RequestParam(name = "pageSize", defaultValue = "25", required = false) Integer pageSize, @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " exportToCSV" + '"' + " method ...");


        try {
            List<TaskMaster> taskMasterList = new ArrayList<>();
            List<TaskMaster> personalTaskMasterList = new ArrayList<>();
            if (statsRequest.getStatName() != null && !statsRequest.getStatName().isEmpty()) {
                taskMasterList = taskService.getTaskDetailsForStatus(statsRequest, timeZone, accountIds);
            } else {
                taskMasterList = taskService.getAllTaskByFilter(statsRequest, accountIds, timeZone);
            }
            if(statsRequest.getTaskTypeList()==null ||  statsRequest.getTaskTypeList().isEmpty()) {
                personalTaskMasterList = personalTaskService.getAllFilteredTaskForPersonalUser(statsRequest, accountIds, timeZone);
                taskMasterList.addAll(personalTaskMasterList);
            }
            taskMasterList = taskService.sortTaskMaster(statsRequest, taskMasterList);

            if (statsRequest.getSearches() != null && !statsRequest.getSearches().isEmpty()) {
                taskMasterList = taskService.filterBySearch(taskMasterList, statsRequest.getSearches());
            }
            if (statsRequest.getHasPagination()) {
                taskMasterList = taskService.getTasksWithPagination(taskMasterList, pageNumber, pageSize);
            }
            byte[] csvData = taskServiceImpl.convertToCsv(taskMasterList);

            ByteArrayResource resource = new ByteArrayResource(csvData);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=task_master.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(csvData.length)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        } catch (Exception e) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to generate CSV file " + "Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
                else throw e;
        }

    }


    // expects all accountIds of the user
    @PostMapping("/searchTasks")
    public ResponseEntity<Object> searchTasks (@Valid @RequestBody SearchTaskRequest searchTaskRequest, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " searchTasks" + '"' + " method ...");

        List<SearchTaskResponse> response = new ArrayList<>();
        List<Long> accountIdsOfUser = requestHeaderHandler.convertToLongList(accountIds);
        try {
            response = taskServiceImpl.searchTasksByFTSAndTrigram(accountIdsOfUser, searchTaskRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " searchTasks" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }


    /**
     * @return Task if task is created successfully else error message.
     * Create bug task quickly with less field requested.
     */
    @PostMapping(path = "/quickCreateBug")
    @Transactional
    public ResponseEntity<Object> quickCreateBug(@Valid @RequestBody QuickCreateBugRequest quickCreateBugRequest, @RequestHeader(name = "screenName") String screenName,
                                                  @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                  HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "quickCreateBug " + '"' + " method ...");

        boolean isTaskValidatedByWorkflowStatus, isTaskValidatedForDateAndTimePairs, isTaskDataValidated, result;
        ArrayList<Integer> roleIds = null;
        Task task = taskServiceImpl.populateBugTask(quickCreateBugRequest,accountIds);
        if(quickCreateBugRequest.getIsToCreateDuplicateTask() != null && !quickCreateBugRequest.getIsToCreateDuplicateTask() && task.getTaskTitle() != null && task.getTaskDesc() != null) {
            AiDuplicateWorkItemDto duplicatesWorkItem = aiMlService.isWorkItemCreationIsDuplicate(new AiWorkItemDescResponse(task.getTaskTitle(), task.getTaskDesc(), task.getFkTeamId().getTeamId()), Long.valueOf(accountIds), screenName, timeZone, jwtToken);
            if(duplicatesWorkItem != null && duplicatesWorkItem.getResults() != null && !duplicatesWorkItem.getResults().isEmpty()) {
                return CustomResponseHandler.generateCustomResponseForCustom(HttpCustomStatus.DUPLICATE_WORK_ITEM, HttpCustomStatus.DUPLICATE_WORK_ITEM.reasonPhrase(), duplicatesWorkItem);
            }
        }
        try {
            taskServiceImpl.validateAccounts(task);
            taskServiceImpl.validateAndNormalizeRca(task);
            removeLeadingAndTrailingSpacesForTask(task);
            taskServiceImpl.validateAccessToMarkWorkItemStarred(task, accountIds);
            taskServiceImpl.setDefaultExpTime(task);
            taskServiceImpl.convertTaskAllUserDateAndTimeInToServerTimeZone(task, timeZone);
            taskServiceImpl.setTaskStateByWorkflowTaskStatus(task);
            taskServiceImpl.updateCurrentlyScheduledTaskIndicatorForTask(task);
            isTaskValidatedByWorkflowStatus = taskServiceImpl.validateTaskByWorkflowStatus(task);
            isTaskValidatedForDateAndTimePairs = taskServiceImpl.validateAllDateAndTimeForPairs(task);
//            taskServiceImpl.validateTaskReferenceEntityIdByTaskTypeId(task);
            roleIds = accessDomainService.getEffectiveRolesByAccountId(task.getFkAccountId().getAccountId(),Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
            taskServiceImpl.validateTaskAndAddToSprint(task, accountIds);
            taskServiceImpl.validateTaskReferenceWorkItem(task);
            taskServiceImpl.validateBugTaskRelationWithReferenceWorkItemId(task);
            taskServiceImpl.validateDependencyWithReferenceWorkItemId(task);
            taskServiceImpl.validateBugTaskRelationWithDependencyWorkItem(task);
            if(task.getFkEpicId() != null) {
                taskServiceImpl.validateCreateTaskWithEpicAndModifyProperties(task, accountIds);
            }
            taskServiceImpl.validateExpDateTimeWithEstimate (task);
        }
        catch (Exception e) {
            e.printStackTrace();
            if (e instanceof TaskEstimateException) {
                throw e;
            } else {
                if (e instanceof ValidationFailedException) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a bug for username = " + foundUser.getPrimaryEmail()+ "ERROR :" +e.getMessage() + "Caught Exception" + e ,  new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw e;
                } else {
                    if (e instanceof DateAndTimePairFailedException) {
                        throw e;
                    } else {
                        if (e instanceof WorkflowTypeDoesNotExistException) {
                            throw new WorkflowTypeDoesNotExistException();
                        } else {
                            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a bug for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                            ThreadContext.clearMap();
                            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                        }
                    }
                }
            }
        }
        if ((roleIds != null && roleIds.isEmpty()) || !isTaskValidatedByWorkflowStatus || !isTaskValidatedForDateAndTimePairs) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "quickCreateBug" + '"' + " method because username = " + foundUser.getPrimaryEmail() + " does not have roles or Work Item validation fails.");
            ThreadContext.clearMap();
            throw new ForbiddenException("does not have roles or Work Item validation fails.");
        } else {
            try {
                if (task.getFkAccountIdAssigned() == null || task.getFkAccountIdAssigned().getAccountId() == null) {
                    result = actionService.isInAction(roleIds, Constants.Task_Add);
                } else {
                    if (task.getFkAccountIdAssigned().getAccountId().equals(task.getFkAccountId().getAccountId())) {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Self_Assignment);
                    } else {
                        result = actionService.isInAction(roleIds, Constants.Self_Created_Assignment_Others);
                    }
                }
                if (result) {
                    try {
                        Task taskUpdated = taskService.initializeTaskNumberSetProperties(task);
                        Task taskAdd = taskServiceImpl.addTaskInTaskTable(taskUpdated, timeZone);
                        taskServiceImpl.updateReferenceWorkItem(taskAdd);
                        taskService.setReferencedTaskDetailsResponse(taskAdd);
                        if(taskAdd.getFkEpicId() != null) {
                            taskServiceImpl.setEpicInCreateTask(taskAdd);
                        }
                        if (enableOpenfire) {
                            new Thread(() -> {
                                try {
                                    String chatRoomName = openFireService.createChatRoomForTask(taskAdd);
                                    String chatGroupName = openFireService.createChatGroupForTask(taskAdd);
                                    openFireService.addGroupToChatRoom(chatRoomName, chatGroupName);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                                    logger.error("Could not create chat group/ chat room for task# " + taskAdd.getTaskNumber() + "Caught Exception: " + e, new Throwable(allStackTraces));
                                }
                            }).start();
                        }
                        Task taskResponse = new Task();
                        BeanUtils.copyProperties(taskAdd, taskResponse);
                        taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(taskResponse, timeZone);
                        long estimatedTime = System.currentTimeMillis() - startTime;
                        ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                        logger.info("Exited" + '"' + "quickCreateBug" + '"' + " method because completed successfully ... ");
                        ThreadContext.clearMap();
                        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, taskResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a quick bug for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                    }
                } else {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + "quickCreateBug"+ '"' + " method because user does not have action to create the Work Item ...");
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ForbiddenException("user does not have action to create a Work Item."));
                    logger.error(request.getRequestURI() + " API: " + "Not able to create a quick bug for username = " + foundUser.getPrimaryEmail() + " because user does not have" + " action to create a task.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new ForbiddenException("user does not have action to create a Work Item.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create a quick bug for username = " + foundUser.getPrimaryEmail() + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    //expects single accountId in the header
    @Transactional
    @PostMapping(path = "/createRecurringTasks")
    public ResponseEntity<Object> createRecurringTasks(@Valid @RequestBody RecurrenceTaskDTO recurrenceScheduleDTO,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "createRecurringTasks " + '"' + " method ...");
        try {

            String message;
            if(Objects.equals(recurrenceScheduleDTO.getTeamId(),0L)) {
                throw new ForbiddenException("Please create Recurring Personal Tasks through Personal Tasks section.");
            }
            else
                message=taskServiceImpl.createRecurringTasks(recurrenceScheduleDTO, Long.parseLong(accountIds), timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "createRecurringTasks" + '"' + " method because method successfully completed");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, message);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create recurring Work Item for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    // expects a single accountId in the header
    @PostMapping(path = "/getRecurringDatesForTask")
    public ResponseEntity<Object> createRecurringTasks(@Valid @RequestBody RecurrenceScheduleDTO recurrenceScheduleDTO,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "createRecurringTasks " + '"' + " method ...");
        try {
            List<LocalDate[]> datesList = recurrenceService.generateRecurringDates(recurrenceScheduleDTO, Long.parseLong(accountIds));
//            Sorts the Preview Work Items based on ExpStartDate
            if(!datesList.isEmpty())
                Collections.sort(datesList, new Comparator<LocalDate[]>() {
                    @Override
                    public int compare(LocalDate[] array1, LocalDate[] array2) {
                        return array1[0].compareTo(array2[0]);
                    }
                });
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + "createRecurringTasks" + '"' + " method because method successfully completed");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, datesList);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create recurring Work Items for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/updateTaskFields")
    @Transactional
    public ResponseEntity<Object> updateTaskFields(@RequestBody UpdateTaskFieldsRequest updateTaskRequest,
                                             @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                             @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws NoSuchFieldException, IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateTaskFields" + '"' + " method ...");
        try {
            taskServiceImpl.updateTaskFields(updateTaskRequest, accountIds, timeZone, foundUser.getPrimaryEmail(), request.getRequestURI(), foundUser.getUserId(), jwtToken);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Tasks successfully updated");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update Work Item for username =  " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }

    }

    @GetMapping(path = "/getEstimateDrillDown/{taskId}")
    public ResponseEntity<Object> getEstimateDrillDown(@PathVariable(name = "taskId") Long taskId,
                                                    @RequestHeader(name = "screenName") String screenName,
                                                    @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getEstimateDrillDown" + '"' + " method ...");

        EstimateDrillDownResponse response;
        try {
            response = taskServiceImpl.getEstimateDrilldown(taskId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getEstimateDrillDown" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the estimate drill down for taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    /** If a taskId of a given sub-task is provided this api will return the status of all sub tasks of that parent*/
    @GetMapping(path = "/getAllSubTaskStatus/{taskId}")
    public ResponseEntity<Object> getAllSubTaskStatus(@PathVariable(name = "taskId") Long taskId,
                                                       @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone,
                                                       @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllSubTaskStatus" + '"' + " method ...");

        Map<String, String> response = new HashMap<>();
        try {
            response = taskServiceImpl.getAllSubTasksStatus(taskId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllSubTaskStatus" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the status of all subtasks for the given taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    /** This api asks for work status from the users*/
    @PostMapping(path = "/askForStatus")
    public ResponseEntity<Object> askForStatus(@RequestBody TaskIdsRequest askForStatusRequest,
                                                      @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) throws JsonProcessingException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " askForStatus" + '"' + " method ...");

        try {
            taskServiceImpl.askForStatus(askForStatusRequest, accountIds, timeZone, foundUser.getUserId());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " askForStatus" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to ask for the status of work item for user = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Inquiry sent successfully");
    }

    // expects single accountId
    @PostMapping(path = "/updateBugDetails")
    public ResponseEntity<Object> updateBugDetails(@Valid @RequestBody UpdateBugDetailsRequest updateBugDetailsRequest,
                                                  @RequestHeader(name = "screenName") String screenName,
                                                  @RequestHeader(name = "timeZone") String timeZone,
                                                  @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateBugDetails" + '"' + " method ...");
        try {
            taskServiceImpl.updateBugDetails(updateBugDetailsRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateBugDetails" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update bug details for user with username = " + username +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Bug details updated successfully");
    }

    /**
     * This api taks the list of task id and delete them, return two list of success and failure tasks with error message
     */
    @Transactional
    @PostMapping(path = "/deleteTasks")
    public ResponseEntity<Object> deleteTasks(@RequestBody @Valid TaskIdsRequest taskIdsRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entering" + '"' + " deleteTasks" + '"' + " method ...");

        try {
            TaskListForBulkResponse response = taskServiceImpl.deleteTaskInBulk(taskIdsRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exiting deleteTasks method because all tasks were deleted successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof DeleteTaskException) {
                logger.error(request.getRequestURI() + " API: " + "Not allowed to delete tasks for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                throw e;
            } else {
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete tasks for username = " + foundUser.getPrimaryEmail() + " ,    " + "taskId = " + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }
    @PostMapping(path = "/validateRelation")
    public ResponseEntity<Object> validateRelation(@RequestBody @Valid ValidateTaskRequest validateTaskRequest,@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) throws InvalidRelationTypeException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " validateRelation" + '"' + " method ...");
        WorkItem response=new WorkItem();
        try{
            if(validateTaskRequest.getRelationType().equals(RelationType.DEPENDENCY)) {
                response=taskServiceImpl.validateTaskForDependency(validateTaskRequest);
            }
            else if (validateTaskRequest.getRelationType().equals(RelationType.REFERENCE)){
                response=taskServiceImpl.validateTaskForReference(validateTaskRequest);
            }
            else if (validateTaskRequest.getRelationType().equals(RelationType.ASSOCIATION)){
                response=taskServiceImpl.validateTaskForAssociation(validateTaskRequest);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exiting validateRelation method because tasks were validated successfully ...");
            ThreadContext.clearMap();

        }catch (Exception e){
            if(e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK,com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS,response);
    }

    @GetMapping(path = "/checkDeleteForChildTask/{taskId}")
    public ResponseEntity<Object> checkDeleteForChildTask(@PathVariable(name = "taskId") Long taskId,
                                                      @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone,
                                                      @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " checkDeleteForChildTask" + '"' + " method ...");

        try {
            String response = taskServiceImpl.getMessageForDeletingChildTask(taskId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " checkDeleteForChildTask" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the status of all other subtasks for the given taskId = " + taskId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    public void removeLeadingAndTrailingSpacesForTask(Task task) {
        if (task.getTaskTitle() != null) {
            task.setTaskTitle(task.getTaskTitle().trim());
        }
        if (task.getTaskDesc() != null) {
            task.setTaskDesc(task.getTaskDesc().trim());
        }
        if (task.getParkingLot() != null) {
            task.setParkingLot(task.getParkingLot().trim());
        }
        if (task.getKeyDecisions() != null) {
            task.setKeyDecisions(task.getKeyDecisions().trim());
        }
        if (task.getAcceptanceCriteria() != null) {
            task.setAcceptanceCriteria(task.getAcceptanceCriteria().trim());
        }

        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            List<Note> noteList = task.getNotes();
            List<Note> newNoteList = noteList.parallelStream()
                    .map(note -> {
                        if (note.getNote() != null) {
                            note.setNote(note.getNote().trim());
                        }
                        return note;
                    })
                    .collect(Collectors.toList());
            task.setNotes(newNoteList);
        }

        if (task.getDeliverables() != null) {
            task.setDeliverables(task.getDeliverables().trim());
        }

        if (task.getListOfDeliverablesDelivered() != null && !task.getListOfDeliverablesDelivered().isEmpty()) {
            List<DeliverablesDelivered> deliverablesDeliveredList = task.getListOfDeliverablesDelivered();
            List<DeliverablesDelivered> newDeliverablesDeliveredList = deliverablesDeliveredList.parallelStream()
                    .map(deliverablesDelivered -> {
                        if (deliverablesDelivered.getDeliverablesDelivered() != null) {
                            deliverablesDelivered.setDeliverablesDelivered(
                                    deliverablesDelivered.getDeliverablesDelivered().trim()
                            );
                        }
                        return deliverablesDelivered;
                    })
                    .collect(Collectors.toList());
            task.setListOfDeliverablesDelivered(newDeliverablesDeliveredList);
        }

        if (task.getStepsTakenToComplete() != null) {
            task.setStepsTakenToComplete(task.getStepsTakenToComplete().trim());
        }

        if (task.getReleaseVersionName() != null) {
            task.setReleaseVersionName(task.getReleaseVersionName().trim());
        }
    }

    @PostMapping (path = "/getOpenTasksAssignedToUserInSprint")
    public ResponseEntity<Object> getOpenTasksAssignedToUserInSprint(@RequestBody OpenTasksAssignedToUserInSprintRequest openTasksAssignedToUserInSprintRequest, @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                                                     @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getOpenTasksAssignedToUserInSprint" + '"' + " method ...");

        try {
            List<OpenTaskDetails> response = taskServiceImpl.getOpenTasksAssignedToUserInSprint(openTasksAssignedToUserInSprintRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getOpenTasksAssignedToUserInSprint" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getOpenTasksAssignedToUserInSprint() for the username = " +
                    foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @PostMapping(path = "/getDependencyGraphByFilter")
    public ResponseEntity<Object> getDependencyGraphByFilter(@RequestBody DependencyGraphRequest dependencyGraphRequest, @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getDependencyGraphByFilter" + '"' + " method ...");

        List<Task> foundTaskDb = null;
        DependencyGraph dependencyGraph = new DependencyGraph();
        try {
            dependencyGraph = dependencyService.getDependencyGraphByFilter(dependencyGraphRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getDependencyGraphByFilter" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create dependency graph for the given filter." + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, dependencyGraph);
    }

    @GetMapping("/getReleaseVersionOfEntity/{entityTypeId}/{entityId}")
    public ResponseEntity<Object> getReleaseVersionOfEntity(@PathVariable Integer entityTypeId, @PathVariable Long entityId, @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getReleaseVersionOfEntity" + '"' + " method ...");

        try {
            List<ReleaseVersionResponse> response = taskServiceImpl.getReleaseVersionOfEntity(entityTypeId, entityId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getReleaseVersionOfEntity" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get release version of entity for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/markWorkItemStarred")
    public ResponseEntity<Object> markWorkItemStarred (@RequestParam(name = "taskId") Long taskId, @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " markWorkItemStarred" + '"' + " method ...");

        try {
            StarredWorkItemRequest starredWorkItemRequest = new StarredWorkItemRequest();
            starredWorkItemRequest.setTaskId(taskId);
            starredWorkItemRequest.setIsStarred(true);
            String response = taskServiceImpl.updateStarredFieldOfWorkItem (starredWorkItemRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " markWorkItemStarred" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            List<HashMap<String,String>> payLoad=notificationService.sendNotificationForStarringUnstarring(starredWorkItemRequest,timeZone,foundUser);
            taskServiceImpl.sendPushNotification(payLoad);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to mark work item starred for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/markWorkItemUnStarred")
    public ResponseEntity<Object> markWorkItemUnStarred (@RequestParam(name = "taskId") Long taskId, @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " markWorkItemUnStarred" + '"' + " method ...");

        try {
            StarredWorkItemRequest starredWorkItemRequest = new StarredWorkItemRequest();
            starredWorkItemRequest.setTaskId(taskId);
            starredWorkItemRequest.setIsStarred(false);
            String response = taskServiceImpl.updateStarredFieldOfWorkItem (starredWorkItemRequest, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " markWorkItemUnStarred" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            List<HashMap<String,String>> payLoad=notificationService.sendNotificationForStarringUnstarring(starredWorkItemRequest,timeZone,foundUser);
            taskServiceImpl.sendPushNotification(payLoad);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to un-mark work item starred for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}

