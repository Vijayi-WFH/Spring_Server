package com.tse.core_application.service.Impl;

/**
 * This class contains the services for notifications.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.tse.core_application.constants.NotificationTypeToCategory;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.UserName;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.leave.Request.ChangeLeaveStatusRequest;
import com.tse.core_application.dto.leave.Request.LeaveApplicationNotificationRequest;
import com.tse.core_application.dto.leave.Response.LeaveApplicationResponse;
import com.tse.core_application.exception.JsonException;
import com.tse.core_application.dto.notification_payload.Payload;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.FCMNotificationUtil;
import com.tse.core_application.utils.JWTUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.model.Constants.NotificationType.*;

@Service
public class NotificationService {

    private static final Logger logger = LogManager.getLogger(NotificationService.class.getName());

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTypeRepository notificationTypeRepository;

    @Autowired
    private NotificationViewRepository notificationViewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ScrollToRepository scrollToRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BURepository buRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private TaskTemplateService taskTemplateService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FCMNotificationUtil fcmNotificationUtil;

    @Autowired
    private CommentRepository commentRepository;

    Gson gson = new Gson();

    ObjectMapper objectMapper = new ObjectMapper();


    /**
     * @param userAccountIdsList
     * @param jwtToken
     * @Function: checks if token have the given accountIdsList
     */
    public Boolean checkAccountIds(List<Long> userAccountIdsList, String jwtToken){
        try {
            List<Long> tokenAccountIds = jwtUtil.getAllAccountIdsFromToken(jwtToken);
            if(tokenAccountIds==null || tokenAccountIds.isEmpty()){
                return false;
            }
            for(Long id: userAccountIdsList){
                if(!tokenAccountIds.contains(id)){
                    return false;
                }
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     *
     * @Function: return payload of notification for when a task is marked as delayed or watchlisted
     */
    public List<HashMap<String, String>> taskStateNotification(Task task, StatType state) {
        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdObserver1()!=null) ? task.getFkAccountIdObserver1().getAccountId():-1),
                            ((task.getFkAccountIdObserver2()!=null) ? task.getFkAccountIdObserver2().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                            ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            Integer roleIdToSearch = Constants.HIGHER_ROLE_IDS.get(0);
            if (task.getFkAccountIdAssigned() != null) {
                roleIdToSearch = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
            }
            accountIds.addAll(new HashSet<>(accessDomainService.getActiveAccountIdsOfHigherRoleMembersInTeam(task.getFkTeamId().getTeamId(),roleIdToSearch)));
            accountIds.remove((long) -1);
            accountIds.remove(task.getFkAccountIdCreator().getAccountId());

            Notification notification = new Notification();
            String notificationType = "";
            //for notification type, title and body
            if(state.equals(StatType.DELAYED)) {
                // some business logic was present
            }
            else if(state.equals(StatType.WATCHLIST)){
                // some business logic was present
            }
            //for required fields from tasks
            notification.setOrgId(task.getFkOrgId());
            notification.setBuId(task.getBuId());
            notification.setProjectId(task.getFkProjectId());
            notification.setTeamId(task.getFkTeamId());
            notification.setAccountId(task.getFkAccountIdCreator());
            notification.setTaskNumber(task.getTaskNumber());

            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), notificationType, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_STATE, NotificationTypeToCategory.TASK_GETTING_DELAYED.getCategoryId()));
            Notification newNotification = notificationRepository.save(notification);
            //Notification view
            //loop for all
            newNotificationView(notification, accountIds);

            return updatingPayloadFormat(accountIds, newNotification.getPayload(), newNotification.getNotificationId(), newNotification.getCreatedDateTime());
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for Work Item state changed to delay or watchlist: " + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    /**
     * @Function: Send immediate Attention Notification to the assigned user
     */
    public void immediateAttentionNotification(Alert alert,String timeZone,String headerAccountIds) {
            try {
                //Stakeholders list
                User user = alert.getFkAccountIdReceiver().getFkUserId();
                HashSet<Long> accountIds = new HashSet<>(List.of(alert.getFkAccountIdReceiver().getAccountId()));
                //Creating Immediate Attention
                Notification notification = newNotificationObjectCreaterForNotificationCreatorAccountId(alert.getFkOrgId(), null, alert.getFkProjectId(),alert.getFkTeamId(),alert.getFkAccountIdSender(),alert.getAssociatedTaskNumber(), NotificationTypeToCategory.IMMEDIATE_ATTENTION.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle(alert.getAlertTitle());
                notification.setNotificationBody(alert.getAlertReason());
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.IMMEDIATE_ATTENTION, alert.getAssociatedTaskNumber(), alert.getAssociatedTaskId(), alert.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.IMMEDIATE_ATTENTION.getCategoryId()));
                notification.setTaggedAccountIds(List.of((alert.getFkAccountIdReceiver().getAccountId())));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.IMMEDIATE_ATTENTION));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                //storing payload
                HashMap<String,String> payload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
                taskServiceImpl.sendFcmNotification(user.getUserId(),payload);

            } catch (Exception e) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Unable to create notification for immediateAttentionNotification " + e,new Throwable(allStackTraces));
            }
    }


    /**
     * Send immediate Attention Notification to the immediateAttentionFrom user
     */
    public void immediateAttentionNotification(Task task,String timeZone) {
        try {
            //Stakeholders list
            User user = userRepository.findByPrimaryEmail(task.getImmediateAttentionFrom());
            List<Long> accountIdList = userAccountRepository.findAccountIdByFkUserIdUserIdAndIsActive(user, true);
            Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM,task.getFkTeamId().getTeamId(),accountIdList, true);
            HashSet<Long> accountIds = new HashSet<>(List.of(accountId));
            //Creating Immediate Attention
            Notification notification=newNotificationObjectCreater(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.IMMEDIATE_ATTENTION.getCategoryId());
            notification.setNotificationTitle("ATTENTION!");
            notification.setNotificationBody("You have received immediate attention for Work Item: "+task.getTaskNumber());
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.IMMEDIATE_ATTENTION, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.IMMEDIATE_ATTENTION.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.IMMEDIATE_ATTENTION));
            notification.setTaggedAccountIds(List.of(accountId));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            //storing payload
            HashMap<String,String> payload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
            taskServiceImpl.sendFcmNotification(user.getUserId(),payload);

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for immediateAttentionNotification " + e,new Throwable(allStackTraces));
        }
    }

    /**
     *
     * @return list of hashmap(payload)
     * @Function: Create a new notification when a task is created.
     */
    public List<HashMap<String, String>> createNewTaskNotification(Task task) {
        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdObserver1()!=null) ? task.getFkAccountIdObserver1().getAccountId():-1),
                            ((task.getFkAccountIdObserver2()!=null) ? task.getFkAccountIdObserver2().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                            ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            Integer roleIdToSearch = Constants.HIGHER_ROLE_IDS.get(0);
            if (task.getFkAccountIdAssigned() != null) {
                roleIdToSearch = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
            }
            accountIds.addAll(new HashSet<>(accessDomainService.getActiveAccountIdsOfHigherRoleMembersInTeam(task.getFkTeamId().getTeamId(),roleIdToSearch)));
            accountIds.remove((long) -1);
            accountIds.remove(task.getFkAccountIdCreator().getAccountId());

            Notification notification = new Notification();
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.CREATE_TASK));
            notification.setCategoryId(NotificationTypeToCategory.CREATE_TASK.getCategoryId());
            //for required fields from tasks
            notification.setOrgId(task.getFkOrgId());
            notification.setBuId(task.getBuId());
            notification.setProjectId(task.getFkProjectId());
            notification.setTeamId(task.getFkTeamId());
            notification.setAccountId(task.getFkAccountIdCreator());
            notification.setTaskNumber(task.getTaskNumber());
            //for title and body
            notification.setNotificationTitle(newTaskTitle(task.getFkAccountIdAssigned(), task.getTaskNumber(), task.getTaskPriority(),task.getTaskTypeId()));
            notification.setNotificationBody(newTaskBody(task.getTaskTitle(), task.getFkAccountIdCreator(),task.getTaskTypeId()));
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.CREATE_TASK, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.CREATE_TASK.getCategoryId()));
            Notification newNotification = notificationRepository.save(notification);
            //Notification view
            //loop for all
            newNotificationView(notification, accountIds);

            return updatingPayloadFormat(accountIds, newNotification.getPayload(), newNotification.getNotificationId(), newNotification.getCreatedDateTime());
        } catch (Exception e) {
            logger.error("Unable to create notification for new Work Item: " + e);
            return Collections.emptyList();
        }
    }

    public List<HashMap<String, String>> createRecurringTaskNotification(Task task, String taskNumbers) {
        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdObserver1()!=null) ? task.getFkAccountIdObserver1().getAccountId():-1),
                            ((task.getFkAccountIdObserver2()!=null) ? task.getFkAccountIdObserver2().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                            ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            Integer roleIdToSearch = Constants.HIGHER_ROLE_IDS.get(0);
            if (task.getFkAccountIdAssigned() != null) {
                roleIdToSearch = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
            }
            accountIds.addAll(new HashSet<>(accessDomainService.getActiveAccountIdsOfHigherRoleMembersInTeam(task.getFkTeamId().getTeamId(),roleIdToSearch)));
            accountIds.remove((long) -1);
            accountIds.remove(task.getFkAccountIdCreator().getAccountId());

            Notification notification = new Notification();
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.CREATE_TASK));
            notification.setCategoryId(NotificationTypeToCategory.CREATE_TASK.getCategoryId());
            //for required fields from tasks
            notification.setOrgId(task.getFkOrgId());
            notification.setBuId(task.getBuId());
            notification.setProjectId(task.getFkProjectId());
            notification.setTeamId(task.getFkTeamId());
            notification.setAccountId(task.getFkAccountIdCreator());
//            notification.setTaskNumber(task.getTaskNumber());
            //for title and body
            notification.setNotificationTitle(newRecurringTaskTitle(task.getFkAccountIdAssigned(), task.getTaskNumber(), task.getTaskPriority(), taskNumbers));
            notification.setNotificationBody(newRecurringTaskBody(task.getTaskTitle(), task.getFkAccountIdCreator()));
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.CREATE_TASK, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.CREATE_TASK.getCategoryId()));
            Notification newNotification = notificationRepository.save(notification);
            //Notification view
            //loop for all
            newNotificationView(notification, accountIds);

            return updatingPayloadFormat(accountIds, newNotification.getPayload(), newNotification.getNotificationId(), newNotification.getCreatedDateTime());
        } catch (Exception e) {
            logger.error("Unable to create notification for new Work Item: " + e);
            return Collections.emptyList();
        }
    }

    /**
     *
     * @return Title for new task as a string
     * @Function: Create title for notification for new task
     */
    private String newTaskTitle(UserAccount fkAccountIdAssigned, String taskNumber, String taskPriority, Integer taskTypeId) {
        StringBuilder title = new StringBuilder();
        String taskType=setTaskType(taskTypeId);
        title.append(taskType).append(taskNumber).append(" created");
        //task priority
        if (taskPriority != null)
            title.append(" with ").append(taskPriority).append(" priority");
        // assigned to
        if(fkAccountIdAssigned != null) {
            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(fkAccountIdAssigned.getAccountId())).getFkUserId().getUserId());
            title.append(" and assigned to ").append(name.getFirstName()).append(" ");
            title.append((name.getLastName()==null) ? "":name.getLastName());
        }
        return title.toString();
    }

    private String newRecurringTaskTitle(UserAccount fkAccountIdAssigned, String taskNumber, String taskPriority, String createdTasks) {
        StringBuilder title = new StringBuilder();
        title.append(createdTasks);
        //task priority
        if (taskPriority != null)
            title.append(" with ").append(taskPriority).append(" priority");
        // assigned to
        if(fkAccountIdAssigned != null) {
            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(fkAccountIdAssigned.getAccountId())).getFkUserId().getUserId());
            title.append(" and assigned to ").append(name.getFirstName()).append(" ");
            title.append((name.getLastName()==null) ? "":name.getLastName());
        }
        return title.toString();
    }

    /**
     *
     * @return body creation for newly created task
     * @Function: Create notification body for the newly created task
     */
    private String newTaskBody(String taskTitle, UserAccount fkAccountIdCreator, Integer taskTypeId) {
        StringBuilder body = new StringBuilder();
        String taskType=setTaskType(taskTypeId);
        UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(fkAccountIdCreator.getAccountId())).getFkUserId().getUserId());
        body.append(name.getFirstName()).append(" ");
        body.append((name.getLastName()==null) ? "":name.getLastName());
        body.append(" has created this ").append(taskType);
        body.append(taskType).append(" title is '").append(taskTitle).append("'.");
        return body.toString();
    }

    private String newRecurringTaskBody(String taskTitle, UserAccount fkAccountIdCreator) {
        StringBuilder body = new StringBuilder();
        UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(fkAccountIdCreator.getAccountId())).getFkUserId().getUserId());
        body.append(name.getFirstName()).append(" ");
        body.append((name.getLastName()==null) ? "":name.getLastName());
        body.append(" has created recurring tasks.");
        body.append(" Work Item title is ").append("'").append(taskTitle).append("'.");
        return body.toString();
    }

    /**
     *
     * @Function: creates new entry for notification view
     */
    private void newNotificationView(Notification notification, HashSet<Long> accountList) {
        List<NotificationView> notificationViews = new ArrayList<>();
        for (Long userAccountId : accountList) {
            if (userAccountId != null) {
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(userAccountId, true);
                if (userAccount == null) {
                    continue;
                }
                NotificationView notificationView = new NotificationView();
                notificationView.setNotificationId(notification);
                notificationView.setIsRead(false);
                notificationView.setModifiedDateTime(null);
                notificationView.setAccountId(userAccount);
                notificationViewRepository.save(notificationView);
                notificationView.setAccountId(userAccountRepository.findByAccountIdAndIsActive(userAccountId, true));
                notificationViews.add(notificationView);
            }
        }
        notificationViewRepository.saveAll(notificationViews);
    }

    /**
     *
     * @return creates and returns new object of notification
     */
    private Notification newNotificationObjectCreater(Organization orgId,Long buId,Project projectId,Team teamId, UserAccount accountId,String taskNumber, Integer categoryId){
        Notification notification = new Notification();
        //for required fields from tasks
        notification.setOrgId(orgId);
        notification.setBuId(buId);
        notification.setProjectId(projectId);
        notification.setTeamId(teamId);
        notification.setAccountId(accountId);
        notification.setTaskNumber(taskNumber);
        notification.setCategoryId(categoryId);
        return notification;
    }

    private Notification newNotificationObjectCreaterForNotificationCreatorAccountId(Organization orgId,Long buId,Project projectId,Team teamId, UserAccount accountId,String taskNumber, Integer categoryId,String accountIds){
        Notification notification = new Notification();
        notification.setOrgId(orgId);
        notification.setBuId(buId);
        notification.setProjectId(projectId);
        notification.setTeamId(teamId);
        notification.setAccountId(accountId);
        notification.setTaskNumber(taskNumber);
        notification.setCategoryId(categoryId);
        if(accountIds!= null) {
            List<Long> accountIdList = Arrays.stream(accountIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            if(!accountIdList.isEmpty()) {
                UserAccount userAccount=userAccountRepository.findByAccountId(accountIdList.get(0));
                notification.setNotificationCreatorAccountId(userAccount);
            }
        }
        return notification;
    }

    /**
     *
     * @return list of payload in the form of hashmap
     * @Function: Create a notification and notification view when a task is updated.
     */
    public List<HashMap<String, String>> updateTaskNotification(ArrayList<String> updateFields, Task task, Task taskDbPrev, String timeZone,String headerAccountIds) {
        try {
            List<HashMap<String, String>> payloadList = new ArrayList<HashMap<String, String>>();
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null && task.getFkAccountIdAssigned().getIsActive()) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdObserver1()!=null) ? task.getFkAccountIdObserver1().getAccountId():-1),
                            ((task.getFkAccountIdObserver2()!=null) ? task.getFkAccountIdObserver2().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                          ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            Integer roleIdToSearch = Constants.HIGHER_ROLE_IDS.get(0);
            if (task.getFkAccountIdAssigned() != null) {
                roleIdToSearch = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
            }
            accountIds.addAll(new HashSet<>(accessDomainService.getActiveAccountIdsOfHigherRoleMembersInTeam(task.getFkTeamId().getTeamId(), roleIdToSearch)));
            accountIds.remove((long) -1);
            accountIds.remove(task.getFkAccountIdLastUpdated().getAccountId());

            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdLastUpdated().getAccountId())).getFkUserId().getUserId());
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            //Checking the changes made
            List<HashMap<String, String>> newPayload = null;
            Boolean isWorkflowStatusChangeAndComplete = false;
            if (task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED) && !taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
                isWorkflowStatusChangeAndComplete = true;
            }

                //checking if updates are in backlog task
            if (Constants.NotificationType.WORKFLOW_TASK_STATUS.contains(taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {   //checking for different backlogs
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_IN_BACKLOG.getCategoryId(),headerAccountIds);
                String oldValue = taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
                String newValue = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
                if (!Objects.equals(oldValue, newValue)) {
                    notification.setOldValue(oldValue);
                    notification.setNewValue(newValue);
                }
                notification.setNotificationTitle(setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated in backlog.");
                notification.setNotificationBody(byName + " has updated " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_PROGRESS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.UPDATE_TASK_IN_BACKLOG.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_PROGRESS));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                //have to copy code from here
                newNotificationView(notification, accountIds);
                //storing payload
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
                return payloadList;
            }
            //check for task estimates
            if (updateFields.contains("taskEstimate")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_ESTIMATE.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Estimates for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed estimate"+ ((taskDbPrev.getTaskEstimate()==null)?"":(" from " + taskDbPrev.getTaskEstimate()/60 + "h " + taskDbPrev.getTaskEstimate()%60 + "m")+ " to " + task.getTaskEstimate()/60+"h " + task.getTaskEstimate()%60 + "m."));
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_ESTIMATE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_ESTIMATE, NotificationTypeToCategory.UPDATE_TASK_ESTIMATE.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_ESTIMATE));
                Integer oldEstimate = (taskDbPrev != null) ? taskDbPrev.getTaskEstimate() : null;
                Integer newEstimate = (task != null) ? task.getTaskEstimate() : null;
                String oldValue = (oldEstimate != null)
                        ? (oldEstimate / 60) + "h " + (oldEstimate % 60) + "m"
                        : "N/A";
                String newValue = (newEstimate != null)
                        ? (newEstimate / 60) + "h " + (newEstimate % 60) + "m"
                        : "N/A";
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                Notification notifi = notificationRepository.save(notification);
                newNotificationView(notification, accountIds);
                //storing payload
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);

            }
            // check for task user perceived expected time to completion
            if (updateFields.contains("userPerceivedRemainingTimeForCompletion") && !isWorkflowStatusChangeAndComplete) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_EFFORT.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("user perceived time to completion for" + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed user perceived time to completion"+ ((taskDbPrev.getUserPerceivedRemainingTimeForCompletion()==null)?"":(" from " + taskDbPrev.getUserPerceivedRemainingTimeForCompletion() ))+ " to " + task.getUserPerceivedRemainingTimeForCompletion()+".");
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_EFFORT, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.UPDATE_TASK_EFFORT.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_EFFORT));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                //storing payload
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);

            }
            //check for task schedule(Expected date time)
            if (updateFields.contains("taskExpStartDate")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Expected Start Date for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed expected start date"+((taskDbPrev.getTaskExpStartDate()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskExpStartDate(),timeZone).toLocalDate() ))+ " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpStartDate(),timeZone).toLocalDate()+".");
                LocalDateTime oldExpStartDate = (taskDbPrev != null) ? taskDbPrev.getTaskExpStartDate() : null;
                LocalDateTime newExpStartDate = (task != null) ? task.getTaskExpStartDate() : null;
                String oldValue = "N/A";
                if (oldExpStartDate != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldExpStartDate, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate() + " " + convertedOld.toLocalTime();
                    }
                }
                String newValue = "N/A";
                if (newExpStartDate != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newExpStartDate, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate() + " " + convertedNew.toLocalTime();
                    }
                }
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);

                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_EXP_START_DATETIME, NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            if (updateFields.contains("taskExpStartTime")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Expected Start Time for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed expected start time" +((taskDbPrev.getTaskExpStartTime()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskExpStartDate(),timeZone).toLocalTime() )) + " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpStartDate(),timeZone).toLocalTime()+".");
                LocalDateTime oldExpStart = (taskDbPrev != null) ? taskDbPrev.getTaskExpStartDate() : null;
                LocalDateTime newExpStart = (task != null) ? task.getTaskExpStartDate() : null;
                String oldValue = "N/A";
                if (oldExpStart != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldExpStart, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate() + " " + convertedOld.toLocalTime();
                    }
                }
                String newValue = "N/A";
                if (newExpStart != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newExpStart, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate() + " " + convertedNew.toLocalTime();
                    }
                }
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_EXP_START_DATETIME, NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            if (updateFields.contains("taskExpEndDate")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Expected End Date for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed expected end date" +((taskDbPrev.getTaskExpEndDate()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskExpEndDate(),timeZone).toLocalDate() ))  + " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(),timeZone).toLocalDate()+".");
                LocalDateTime oldExpEndDate = (taskDbPrev != null) ? taskDbPrev.getTaskExpEndDate() : null;
                String oldValue = "N/A";
                if (oldExpEndDate != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldExpEndDate, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate() + " " + convertedOld.toLocalTime();
                    }
                }
                notification.setOldValue(oldValue);
                LocalDateTime newExpEndDate = (task != null) ? task.getTaskExpEndDate() : null;
                String newValue = "N/A";
                if (newExpEndDate != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newExpEndDate, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate() + " " + convertedNew.toLocalTime();
                    }
                }
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_EXP_END_DATETIME, NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            if (updateFields.contains("taskExpEndTime")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Expected End Time for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed expected end time" +((taskDbPrev.getTaskExpEndTime()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskExpEndDate(),timeZone).toLocalTime() )) + " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(),timeZone).toLocalTime()+".");
                LocalDateTime oldExpEnd = (taskDbPrev != null) ? taskDbPrev.getTaskExpEndDate() : null;
                LocalDateTime newExpEnd = (task != null) ? task.getTaskExpEndDate() : null;
                String oldValue = "N/A";
                if (oldExpEnd != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldExpEnd, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate() + " " + convertedOld.toLocalTime();
                    }
                }
                String newValue = "N/A";
                if (newExpEnd != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newExpEnd, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate() + " " + convertedNew.toLocalTime();
                    }
                }
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_EXP_END_DATETIME, NotificationTypeToCategory.UPDATE_TASK_SCHEDULE.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //check for task current activity
            if (updateFields.contains("currentActivityIndicator") && !isWorkflowStatusChangeAndComplete) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_CURR_ACTIVITY_IND.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Scheduling of " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " has switched " + ((taskDbPrev.getCurrentActivityIndicator() == null) ? "" : (taskDbPrev.getCurrentActivityIndicator() - task.getCurrentActivityIndicator() == -1 ? "on " : "off ") + "Current Activity Indicator"));
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_CURR_ACTIVITY_IND, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_CURRENT_ACTIVITY, NotificationTypeToCategory.UPDATE_TASK_CURR_ACTIVITY_IND.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_CURR_ACTIVITY_IND));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //check for task progress
            //actual start date
            if (updateFields.contains("taskActStDate")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Actual start date of " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed actual start date"+((taskDbPrev.getTaskActStDate()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskActStDate(),timeZone).toLocalDate() ))+ " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActStDate(),timeZone).toLocalDate()+".");
                LocalDateTime oldStartDate = (taskDbPrev != null) ? taskDbPrev.getTaskActStDate() : null;
                LocalDateTime newStartDate = (task != null) ? task.getTaskActStDate() : null;
                String oldValue = "N/A";
                if (oldStartDate != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldStartDate, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate() + " " + convertedOld.toLocalTime();
                    }
                }
                String newValue = "N/A";
                if (newStartDate != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newStartDate, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate() + " " + convertedNew.toLocalTime();
                    }
                }
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_ACT_START_DATETIME, NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //actual start time
            if (updateFields.contains("taskActStTime")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Actual start time of " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed actual start time"+((taskDbPrev.getTaskActStTime()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskActStDate(),timeZone).toLocalTime())) + " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActStDate(),timeZone).toLocalTime()+".");
                LocalDateTime oldStartDate = (taskDbPrev != null) ? taskDbPrev.getTaskActStDate() : null;
                LocalDateTime newStartDate = (task != null) ? task.getTaskActStDate() : null;
                String oldValue = (oldStartDate != null)
                        ? DateTimeUtils.convertServerDateToUserTimezone(oldStartDate, timeZone)
                        .toLocalTime()
                        .toString()
                        : "N/A";
                String newValue = (newStartDate != null)
                        ? DateTimeUtils.convertServerDateToUserTimezone(newStartDate, timeZone)
                        .toLocalTime()
                        .toString()
                        : "N/A";
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_ACT_START_DATETIME, NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);

            }
            //actual end date
            if (updateFields.contains("taskActEndDate") && !isWorkflowStatusChangeAndComplete) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Actual end date of " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed actual end date"+((taskDbPrev.getTaskActEndDate()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskActEndDate(),timeZone).toLocalDate() ))+ " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActEndDate(),timeZone).toLocalDate()+".");
                LocalDateTime oldEndDate = (taskDbPrev != null) ? taskDbPrev.getTaskActEndDate() : null;
                LocalDateTime newEndDate = (task != null) ? task.getTaskActEndDate() : null;
                String oldValue = "N/A";
                if (oldEndDate != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldEndDate, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate().toString();
                    }
                }
                String newValue = "N/A";
                if (newEndDate != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newEndDate, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate().toString();
                    }
                }
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_ACT_END_DATETIME, NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);

            }
            //actual end time
            if (updateFields.contains("taskActEndTime") && !isWorkflowStatusChangeAndComplete) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Actual end time of " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
                notification.setNotificationBody(byName + " changed actual end time"+((taskDbPrev.getTaskActEndTime()==null)?"":(" from "
                        + DateTimeUtils.convertServerDateToUserTimezone(taskDbPrev.getTaskActEndDate(),timeZone).toLocalTime())) + " to "
                        + DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActEndDate(),timeZone).toLocalTime()+".");
                LocalDateTime oldEndDate = (taskDbPrev != null) ? taskDbPrev.getTaskActEndDate() : null;
                LocalDateTime newEndDate = (task != null) ? task.getTaskActEndDate() : null;
                String oldValue = "N/A";
                if (oldEndDate != null && timeZone != null) {
                    LocalDateTime convertedOld = DateTimeUtils.convertServerDateToUserTimezone(oldEndDate, timeZone);
                    if (convertedOld != null) {
                        oldValue = convertedOld.toLocalDate()+" "+convertedOld.toLocalTime();
                    }
                }
                String newValue = "N/A";
                if (newEndDate != null && timeZone != null) {
                    LocalDateTime convertedNew = DateTimeUtils.convertServerDateToUserTimezone(newEndDate, timeZone);
                    if (convertedNew != null) {
                        newValue = convertedNew.toLocalDate()+" "+convertedNew.toLocalTime();
                    }
                }
                notification.setOldValue(oldValue);
                notification.setNewValue(newValue);
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_SCHEDULE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_ACT_END_DATETIME, NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_SCHEDULE));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //checking for workflow status
            if (!Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId(),headerAccountIds);
                String[] wordsOfStatus = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().split(" ");
                String status = String.join(" ", Arrays.copyOfRange(wordsOfStatus, 1, wordsOfStatus.length));
                notification.setNotificationTitle(setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " is marked as " + "'" + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + "'");
                notification.setNotificationBody(byName + " changed the status of the Work Item from '" + taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + "' to '" + task.getFkWorkflowTaskStatus().getWorkflowTaskStatus()+"'.");
                notification.setOldValue(taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                notification.setNewValue(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_PROGRESS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_WORKFLOW_STATUS, NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_PROGRESS));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //checking task efforts
            if (updateFields.contains("recordedEffort") && !isWorkflowStatusChangeAndComplete) {
                HashSet<Long> notificationAccountIds = new HashSet<>();
                notificationAccountIds.addAll(accountIds);
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_EFFORT.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("New efforts recorded for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                int diff = task.getRecordedEffort() - (taskDbPrev.getRecordedEffort()!=null ? taskDbPrev.getRecordedEffort() : 0);
                if(diff/60>0){
                    notification.setNotificationBody(byName + " has recorded new effort of " + diff/60+" hours "+(diff-(diff/60)*60)+" minutes.");
                }
                else {
                    notification.setNotificationBody(byName + " has recorded new effort of " + diff+" minutes.");
                }
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_EFFORT, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_RECORDED_EFFORT, NotificationTypeToCategory.UPDATE_TASK_EFFORT.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_EFFORT));
                Notification notifi = notificationRepository.save(notification);
                if (task.getFkAccountIdAssigned() != null && !Objects.equals(task.getFkAccountIdAssigned().getAccountId(), task.getFkAccountIdLastUpdated().getAccountId())) {
                    HashSet<Long> assignedToAccountId = new HashSet<>();
                    assignedToAccountId.add(task.getFkAccountIdAssigned().getAccountId());
                    notificationAccountIds.remove(task.getFkAccountIdAssigned().getAccountId());
                    Notification notificationForAssignedTo=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.EFFORT_ALERTS.getCategoryId(),headerAccountIds);
                    User lastUpdatedUser = userRepository.findByUserId(task.getFkAccountIdLastUpdated().getFkUserId().getUserId());
                    notificationForAssignedTo.setNotificationTitle("New efforts recorded for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " by " + lastUpdatedUser.getFirstName() + " " + (lastUpdatedUser.getLastName() != null ? lastUpdatedUser.getLastName() : ""));
                    int difference=task.getRecordedEffort()- (taskDbPrev.getRecordedEffort()!=null ? taskDbPrev.getRecordedEffort() : 0);
                    if(difference/60>0){
                        notificationForAssignedTo.setNotificationBody(byName + " has recorded new effort of " + difference/60+" hours "+(difference-(difference/60)*60)+" minutes.");
                    }
                    else {
                        notificationForAssignedTo.setNotificationBody(byName + " has recorded new effort of " + difference+" minutes.");
                    }
                    notificationForAssignedTo.setPayload(TaskPayload(notificationForAssignedTo.getNotificationTitle(), notificationForAssignedTo.getNotificationBody(), notificationForAssignedTo.getNotificationId(), EFFORT_ALERTS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_RECORDED_EFFORT, NotificationTypeToCategory.EFFORT_ALERTS.getCategoryId()));
                    notificationForAssignedTo.setNotificationTypeID(notificationTypeRepository.findByNotificationType(EFFORT_ALERTS));
                    Notification notifiAssignedTo = notificationRepository.save(notificationForAssignedTo);
                    newNotificationView(notificationForAssignedTo, assignedToAccountId);
                    newPayload = updatingPayloadFormat(assignedToAccountId, notifiAssignedTo.getPayload(), notifiAssignedTo.getNotificationId(), notifiAssignedTo.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
                //Notification view
                newNotificationView(notification, notificationAccountIds);
                newPayload = updatingPayloadFormat(notificationAccountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }

            //checking task stakeholder
            //observer1
            if (updateFields.contains("fkAccountIdObserver1")) {
                //Adding new observer1
                {
                    UserName newName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdObserver1().getAccountId())).getFkUserId().getUserId());
                    String bynewName = newName.getFirstName() + " " + ((newName.getLastName()==null) ? "":newName.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " added " + bynewName + " as the observer.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_PROGRESS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_OBSERVER, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }

                //Removing previous observer1
                if (taskDbPrev.getFkAccountIdObserver1() != null) {
                    UserName newName1 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdObserver2().getAccountId())).getFkUserId().getUserId());
                    String bynewName1 = newName1.getFirstName() + " " + ((newName1.getLastName()==null) ? "":newName1.getLastName());
                    UserName newName2 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(taskDbPrev.getFkAccountIdObserver2().getAccountId())).getFkUserId().getUserId());
                    String bynewName2 = newName2.getFirstName() + " " + ((newName2.getLastName()==null) ? "":newName2.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " removed " + bynewName2 +
                            " and added" + bynewName1 + " as the observer.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_PROGRESS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_OBSERVER, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    accountIds.remove(task.getFkAccountIdObserver1().getAccountId());
                    if (taskDbPrev.getFkAccountIdObserver1() != null)
                        accountIds.add(taskDbPrev.getFkAccountIdObserver1().getAccountId());
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                    if (task.getFkAccountIdObserver1() != null)
                        accountIds.add(task.getFkAccountIdObserver1().getAccountId());
                    accountIds.remove(taskDbPrev.getFkAccountIdObserver1().getAccountId());
                }
            }
            if (updateFields.contains("fkAccountIdObserver2")) {
                //Adding new observer2
                {
                    UserName newName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdObserver2().getAccountId())).getFkUserId().getUserId());
                    String bynewName = newName.getFirstName() + " " + ((newName.getLastName()==null) ? "":newName.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " added " + bynewName + " as the observer.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_OBSERVER, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }

                //Removing previous observer2
                if (taskDbPrev.getFkAccountIdObserver2() != null) {
                    UserName newName1 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdObserver2().getAccountId())).getFkUserId().getUserId());
                    String bynewName1 = newName1.getFirstName() + " " + ((newName1.getLastName()==null) ? "":newName1.getLastName());
                    UserName newName2 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(taskDbPrev.getFkAccountIdObserver2().getAccountId())).getFkUserId().getUserId());
                    String bynewName2 = newName2.getFirstName() + " " + ((newName2.getLastName()==null) ? "":newName2.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " removed " + bynewName2 +
                            " and added" + bynewName1 + " as the observer.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_OBSERVER, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    accountIds.remove(task.getFkAccountIdObserver2().getAccountId());
                    if (taskDbPrev.getFkAccountIdObserver2() != null)
                        accountIds.add(taskDbPrev.getFkAccountIdObserver2().getAccountId());
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                    if (task.getFkAccountIdObserver2() != null)
                        accountIds.add(task.getFkAccountIdObserver2().getAccountId());
                    accountIds.remove(taskDbPrev.getFkAccountIdObserver2().getAccountId());
                }
            }
            if (updateFields.contains("fkAccountIdMentor1")) {
                //Adding new mentor1
                {
                    UserName newName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdMentor1().getAccountId())).getFkUserId().getUserId());
                    String bynewName = newName.getFirstName() + " " + ((newName.getLastName()==null) ? "":newName.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " added " + bynewName + " as the mentor.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_MENTOR, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }

                //Removing previous mentor1
                if (taskDbPrev.getFkAccountIdMentor1() != null) {
                    UserName newName1 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdMentor1().getAccountId())).getFkUserId().getUserId());
                    String bynewName1 = newName1.getFirstName() + " " + ((newName1.getLastName()==null) ? "":newName1.getLastName());
                    UserName newName2 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(taskDbPrev.getFkAccountIdMentor1().getAccountId())).getFkUserId().getUserId());
                    String bynewName2 = newName2.getFirstName() + " " + ((newName2.getLastName()==null) ? "":newName2.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " removed " + bynewName2 +
                            " and added" + bynewName1 + " as the mentor.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_MENTOR, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    accountIds.remove(task.getFkAccountIdMentor1().getAccountId());
                    if (taskDbPrev.getFkAccountIdMentor1() != null)
                        accountIds.add(taskDbPrev.getFkAccountIdMentor1().getAccountId());
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                    if (task.getFkAccountIdMentor1() != null)
                        accountIds.add(task.getFkAccountIdMentor1().getAccountId());
                    accountIds.remove(taskDbPrev.getFkAccountIdMentor1().getAccountId());
                }
            }
            if (updateFields.contains("fkAccountIdMentor2")) {
                //Adding new mentor2
                {
                    UserName newName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdMentor2().getAccountId())).getFkUserId().getUserId());
                    String bynewName = newName.getFirstName() + " " + ((newName.getLastName()==null) ? "":newName.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " added " + bynewName + " as the mentor.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_MENTOR, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
                //Removing previous mentor2
                if (taskDbPrev.getFkAccountIdMentor2() != null) {
                    UserName newName1 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdMentor2().getAccountId())).getFkUserId().getUserId());
                    String bynewName1 = newName1.getFirstName() + " " + ((newName1.getLastName()==null) ? "":newName1.getLastName());
                    UserName newName2 = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(taskDbPrev.getFkAccountIdMentor2().getAccountId())).getFkUserId().getUserId());
                    String bynewName2 = newName2.getFirstName() + " " + ((newName2.getLastName()==null) ? "":newName2.getLastName());
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Stakeholder modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " removed " + bynewName2 +
                            " and added" + bynewName1 + " as the observer.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_MENTOR, NotificationTypeToCategory.UPDATE_TASK_STAKEHOLDERS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_STAKEHOLDERS));
                    Notification notifi = notificationRepository.save(notification);
                    accountIds.remove(task.getFkAccountIdMentor2().getAccountId());
                    if (taskDbPrev.getFkAccountIdMentor2() != null)
                        accountIds.add(taskDbPrev.getFkAccountIdMentor2().getAccountId());
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                    if (task.getFkAccountIdMentor2() != null)
                        accountIds.add(task.getFkAccountIdMentor2().getAccountId());
                    accountIds.remove(taskDbPrev.getFkAccountIdMentor2().getAccountId());
                }
            }

            //checking task priority
            if (updateFields.contains("taskPriority")) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PRIORITY.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Priority modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                notification.setNotificationBody(byName + " has changed Work Item priority from " + taskDbPrev.getTaskPriority() + " to " + task.getTaskPriority()+".");
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_PRIORITY, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_PRIORITY, NotificationTypeToCategory.UPDATE_TASK_PRIORITY.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_PRIORITY));
                notification.setOldValue(taskDbPrev.getTaskPriority());
                notification.setNewValue(task.getTaskPriority());
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }

            //checking task assignment
            if (task.getFkAccountIdAssigned()!=null && !Objects.equals(task.getFkAccountIdAssigned().getAccountId(),taskDbPrev.getFkAccountIdAssigned().getAccountId())){
                //AssignedTo new
                {
                    UserName newName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdAssigned().getAccountId())).getFkUserId().getUserId());
                    String bynewName = newName.getFirstName() + " " + ((newName.getLastName()==null) ? "":newName.getLastName());

                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_ASSIGNMENT.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle(setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " assigned to " + bynewName);
                    notification.setNotificationBody(byName + " has assigned " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " to " + bynewName+".");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_ASSIGNMENT, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_ASSIGNED, NotificationTypeToCategory.UPDATE_TASK_ASSIGNMENT.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_ASSIGNMENT));
                    Notification notifi = notificationRepository.save(notification);
                    {
                        List<Long> headerAccountIdList = new ArrayList<>();
                        if (headerAccountIds != null && !headerAccountIds.isBlank()) {
                            headerAccountIdList = Arrays.stream(headerAccountIds.split(","))
                                    .map(String::trim)
                                    .filter(id -> !id.isEmpty())
                                    .map(Long::parseLong)
                                    .collect(Collectors.toList());
                        }
                        Long assignedToAccountId = (task.getFkAccountIdAssigned() != null)
                                ? task.getFkAccountIdAssigned().getAccountId()
                                : null;
                        if (assignedToAccountId != null && !headerAccountIdList.contains(assignedToAccountId)) {
                            accountIds.add(assignedToAccountId);
                            newNotificationView(notification, accountIds);
                        }
                        newNotificationView(notification, accountIds);
                        newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                        payloadList.addAll(newPayload);
                        accountIds.remove(assignedToAccountId);
                    }
                    if (taskDbPrev.getFkAccountIdAssigned() != null) {
                        accountIds.remove(task.getFkAccountIdAssigned().getAccountId());
                        if (taskDbPrev.getFkAccountIdAssigned() != null)
                            accountIds.add(taskDbPrev.getFkAccountIdAssigned().getAccountId());
                        //Notification view for previous assignedTo
                        newNotificationView(notification, accountIds);
                        newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                        payloadList.addAll(newPayload);
                        if (task.getFkAccountIdAssigned() != null)
                            accountIds.add(task.getFkAccountIdAssigned().getAccountId());
                        accountIds.remove(taskDbPrev.getFkAccountIdAssigned().getAccountId());
                    }
                }
            }
            //checking for remaining updates
            //comments
            if(taskDbPrev.getComments()!=null || task.getComments()!=null) {
                if ((task.getComments()!=null) && ((taskDbPrev.getComments()==null) || (taskDbPrev.getComments().size() < task.getComments().size()))) {
                    Notification notification = newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(), task.getBuId(), task.getFkProjectId(), task.getFkTeamId(), task.getFkAccountId(), task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_COMMENTS.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("New comment for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " has added a new comment.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_COMMENTS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_COMMENTS, NotificationTypeToCategory.UPDATE_TASK_COMMENTS.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_COMMENTS));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
            }
            //notes
            if(taskDbPrev.getNotes()!=null || task.getNotes()!=null) {
                Set<Long> newNotesId = task.getNotes().stream().map(Note::getNoteLogId).collect(Collectors.toSet());
                if (newNotesId.contains(null)) {
                    Notification notification = newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(), task.getBuId(), task.getFkProjectId(), task.getFkTeamId(), task.getFkAccountId(), task.getTaskNumber(), NotificationTypeToCategory.NOTE.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("New note for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " has added a new note.");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_NOTES, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_NOTES, NotificationTypeToCategory.NOTE.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_NOTES));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
            }
            //Key Decisions
            if(updateFields.contains("keyDecisions")){
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_OTHER_ACTIVITIES.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Key Decisions modified for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " has changed key decisions"+((taskDbPrev.getKeyDecisions()==null)?"":(" from " + taskDbPrev.getKeyDecisions())) + " to " + task.getKeyDecisions()+".");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_KEY_DECISIONS, NotificationTypeToCategory.UPDATE_TASK_OTHER_ACTIVITIES.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES));
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
            //Task title
            if(updateFields.contains("taskTitle")){
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_TITLE.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Task title changed for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " has changed Work Item title from \"" + taskDbPrev.getTaskTitle() + "\" to \"" + task.getTaskTitle()+"\".");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_TITLE, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_TITLE, NotificationTypeToCategory.UPDATE_TASK_TITLE.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_TITLE));
                notification.setOldValue(taskDbPrev.getTaskTitle());
                notification.setNewValue(task.getTaskTitle());
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
            //Task desc
            if(updateFields.contains("taskDesc")){
                    Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_DESC.getCategoryId(),headerAccountIds);
                    notification.setNotificationTitle("Work Item desc changed for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                    notification.setNotificationBody(byName + " has changed Work Item desc from \"" + CommonUtils.truncateWithEllipsis(taskDbPrev.getTaskDesc(), 250) + "\" to \"" + CommonUtils.truncateWithEllipsis(task.getTaskDesc(), 250)+"\".");
                    notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_DESC, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_DESC, NotificationTypeToCategory.UPDATE_TASK_DESC.getCategoryId()));
                    notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_DESC));
                notification.setOldValue(taskDbPrev.getTaskDesc());
                notification.setNewValue(task.getTaskDesc());
                    Notification notifi = notificationRepository.save(notification);
                    //Notification view
                    newNotificationView(notification, accountIds);
                    newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                    payloadList.addAll(newPayload);
                }
            //Perceived Percentage
            if(updateFields.contains("userPerceivedPercentageTaskCompleted") && !isWorkflowStatusChangeAndComplete){
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_OTHER_ACTIVITIES.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Perceived Percentage changed for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                notification.setNotificationBody(byName + " has updated task Perceived Percentage"+((taskDbPrev.getUserPerceivedPercentageTaskCompleted()==null)?"":(" from " + taskDbPrev.getUserPerceivedPercentageTaskCompleted() ))+ " to " + task.getUserPerceivedPercentageTaskCompleted()+".");
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_USER_PERCEIVED_PERCENTAGE, NotificationTypeToCategory.UPDATE_TASK_OTHER_ACTIVITIES.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //List Of Deliverables Delivered
            if(updateFields.contains("listOfDeliverablesDelivered")){
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_OTHER_ACTIVITIES.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("List of deliverables delivered updated for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
                notification.setNotificationBody(byName + " has updated List of deliverables delivered.");
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_LIST_OF_DELIVERABLES_DELIVERED, NotificationTypeToCategory.UPDATE_TASK_OTHER_ACTIVITIES.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            //currentlyScheduledTaskIndicator
            if (updateFields.contains("currentlyScheduledTaskIndicator") && !isWorkflowStatusChangeAndComplete) {
                Notification notification=newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(),task.getBuId(),task.getFkProjectId(),task.getFkTeamId(),task.getFkAccountId(),task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_CURR_SCHEDULED_TASK_IND.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Currently scheduled task indicator has changed for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + ".");
                notification.setNotificationBody(byName + " has switched "+ (task.getCurrentlyScheduledTaskIndicator() ? "on " : "off ") +"Currently Scheduled Indication");
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_OTHER_ACTIVITIES, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_CURRENTLY_SCHEDULED_TASK_INDICATOR, NotificationTypeToCategory.UPDATE_TASK_CURR_SCHEDULED_TASK_IND.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_CURR_SCHEDULED_TASK_IND));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIds);
                newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                payloadList.addAll(newPayload);
            }
            if (updateFields.contains("fkWorkflowTaskStatus") && updateFields.contains("blockedReasonTypeId")) {
                HashSet<Long> accountIdList = new HashSet<>();
                if(task.getFkAccountIdAssigned() != null) {
                    accountIdList.add(task.getFkAccountIdAssigned().getAccountId());
                }
                else if(task.getFkAccountIdLastUpdated()!=null)
                {
                    accountIdList.add(task.getFkAccountIdLastUpdated().getAccountId());
                }
                else if(task.getFkAccountIdCreator()!= null)
                {
                    accountIdList.add(task.getFkAccountIdCreator().getAccountId());
                }
                Notification notification = newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(), task.getBuId(), task.getFkProjectId(), task.getFkTeamId(), task.getFkAccountId(), task.getTaskNumber(), NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId(),headerAccountIds);
                notification.setNotificationTitle("Work Item is Blocked.");
                notification.setOldValue(taskDbPrev.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                notification.setNewValue(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                if (updateFields.contains("fkAccountIdRespondent") && task.getFkAccountIdRespondent() != null) {
                    accountIdList.add(task.getFkAccountIdRespondent().getAccountId());
                    UserName nameRespondent = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdRespondent().getAccountId())).getFkUserId().getUserId());
                    String byNameRespondent = nameRespondent.getFirstName() + " " + ((nameRespondent.getLastName() == null) ? "" : nameRespondent.getLastName());
                    notification.setNotificationBody("Work Item " + (task.getTaskNumber() + " is Blocked by " + byNameRespondent + " and Reason is " + task.getBlockedReason()));
                } else {
                    notification.setNotificationBody("Work Item " + (task.getTaskNumber() + " is Blocked and Reason is " + task.getBlockedReason()));
                }
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), UPDATE_TASK_PROGRESS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_WORKFLOW_STATUS, NotificationTypeToCategory.UPDATE_TASK_PROGRESS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATE_TASK_PROGRESS));
                Notification notifi = notificationRepository.save(notification);
                //Notification view
                newNotificationView(notification, accountIdList);
                newPayload = updatingPayloadFormat(accountIdList, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
                taskServiceImpl.sendPushNotification(newPayload);
                payloadList.addAll(newPayload);
            }

            return payloadList;
        } catch (Exception e) {
            logger.error("Unable to create notification for updateTaskNotification " +e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     *
     * @return updating payload fields
     */
    private List<HashMap<String, String>> updatingPayloadFormatForGc(HashSet<Long> accountIds, String payload, Long notificationId, LocalDateTime createdDateTime, GroupConversation gcMessage) {
        Payload load = gson.fromJson(payload, Payload.class);
        load.setNotificationId(String.valueOf(notificationId));
//        // Convert LocalDateTime to ZonedDateTime with a server timezone information
//        if (createdDateTime != null) {
//            ZonedDateTime zonedDateTime = ZonedDateTime.of(createdDateTime, ZoneId.systemDefault());
//            load.setCreatedDateTime(zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)); // Format with timezone information
//        }
        load.setGcEntityId(String.valueOf(gcMessage.getEntityId()));
        load.setGcEntityTypeId(String.valueOf(gcMessage.getEntityTypeId()));
        load.setGroupConversationId(String.valueOf(gcMessage.getGroupConversationId()));

        List<HashMap<String, String>> listOfPayload = new ArrayList<>();
        for (Long userAccountId : accountIds) {
            load.setAccountId(String.valueOf(userAccountId));
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(userAccountId, true);
            if (userAccount != null && createdDateTime != null) {
                ZonedDateTime systemZonedDateTime = createdDateTime.atZone(ZoneId.systemDefault());
                ZoneId userTimeZone = ZoneId.of(userAccount.getFkUserId().getTimeZone());
//                ZonedDateTime zonedDateTime = createdDateTime.atZone(userTimeZone);
                ZonedDateTime zonedDateTime = systemZonedDateTime.withZoneSameInstant(userTimeZone);
                LocalDateTime localDateTime = LocalDateTime.from(zonedDateTime);
                load.setCreatedDateTime(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));            }
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> (entry.getValue() == null || entry.getValue().equals("null")));
            listOfPayload.add(loadMap);
        }

        return listOfPayload;
    }

    private List<HashMap<String, String>> updatingPayloadFormat(HashSet<Long> accountIds, String payload, Long notificationId, LocalDateTime createdDateTime) {
        Payload load = gson.fromJson(payload, Payload.class);

        // Convert LocalDateTime to ZonedDateTime with a server timezone information
//        if (createdDateTime != null) {
//            ZonedDateTime zonedDateTime = ZonedDateTime.of(createdDateTime, ZoneId.systemDefault());
//            load.setCreatedDateTime(zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)); // Format with timezone information
//        }

        load.setNotificationId(String.valueOf(notificationId));
        load.setCreatedDateTime(String.valueOf(createdDateTime));

        List<HashMap<String, String>> listOfPayload = new ArrayList<>();
        for (Long userAccountId : accountIds) {
            load.setAccountId(String.valueOf(userAccountId));
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(userAccountId, true);
            if (userAccount != null && createdDateTime != null) {
                ZonedDateTime systemZonedDateTime = createdDateTime.atZone(ZoneId.systemDefault());
                ZoneId userTimeZone = ZoneId.of(userAccount.getFkUserId().getTimeZone());
//                ZonedDateTime zonedDateTime = createdDateTime.atZone(userTimeZone);
                ZonedDateTime zonedDateTime = systemZonedDateTime.withZoneSameInstant(userTimeZone);
                LocalDateTime localDateTime = LocalDateTime.from(zonedDateTime);
                load.setCreatedDateTime(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> (entry.getValue() == null || entry.getValue().equals("null")));
            listOfPayload.add(loadMap);
        }

        return listOfPayload;
    }

    /**
     *
     * @return creating new payload for task updates
     */
    private String TaskPayload(String notificationTitle, String notificationBody, Long notificationId, String Tasktype, String taskNumber, Long taskId, Long teamId, String scrollTo, Integer categoryId) {
        Payload payload = new Payload();
        payload.setAccountId(null);
        payload.setNotificationId(String.valueOf(notificationId));
        payload.setNotificationType(Tasktype);
        payload.setTitle(notificationTitle);
        payload.setBody(notificationBody);
        payload.setCategoryId(String.valueOf(categoryId));
        payload.setTaskNumber(taskNumber);
        payload.setTaskId(taskId != null ? String.valueOf(taskId) : "");
        payload.setTeamId(teamId != null ? String.valueOf(teamId) : "");
        payload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(scrollTo)));

        //Convert to String
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String taskPayloadString;
        try {
            taskPayloadString = objectWriter.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Json to string"+e);
            throw new JsonException("Error converting Json to string");
        }

        return taskPayloadString;
    }

    private String gcPayload(GroupConversation gcMessage, String notificationTitle, String notificationBody, Long notificationId, String Tasktype, String taskNumber, String scrollTo, Integer categoryId) {
        Payload payload = new Payload();
        payload.setAccountId(null);
        payload.setNotificationId(String.valueOf(notificationId));
        payload.setNotificationType(Tasktype);
        payload.setTitle(notificationTitle);
        payload.setBody(notificationBody);
        payload.setCategoryId(String.valueOf(categoryId));
        payload.setTaskNumber(String.valueOf(taskNumber));
        payload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(scrollTo)));
        payload.setGcEntityId(String.valueOf(gcMessage.getEntityId()));
        payload.setGcEntityTypeId(String.valueOf(gcMessage.getEntityTypeId()));
        payload.setGroupConversationId(String.valueOf(gcMessage.getGroupConversationId()));

        //Convert to String
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String taskPayloadString;
        try {
            taskPayloadString = objectWriter.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Json to string"+e);
            throw new JsonException("Error converting Json to string");
        }

        return taskPayloadString;
    }


    /**
     *
     * @return returns all the notifications for an accountId
     */
    public List<NotificationResponse> getNotificationForAUser(NotificationRequest notificationRequest, String timeZone) {
        List<NotificationResponse> notificationResponseList = new ArrayList<>();
        if(notificationRequest.getNotificationId() == null) {
            for (Long accountId : notificationRequest.getAccountIdsList()) {
                notificationResponseList.addAll(getCustomNotification(accountId, timeZone));
            }
        } else {
            for (Long accountId : notificationRequest.getAccountIdsList()) {
                notificationResponseList.addAll(getNotificationsAfterSpecifiedId(accountId, timeZone, notificationRequest.getNotificationId()));
            }
        }
        notificationResponseList.sort(Comparator.comparing(n -> DateTimeUtils.parseDateTime(((Payload) n.getPayload()).getCreatedDateTime()), Comparator.reverseOrder()));
        return notificationResponseList;
    }

    /**
     *
     * @return List of NotificationResponse
     * @Function: Customize notification for user view
     */
    private List<NotificationResponse> getCustomNotification(Long accountId, String timeZone) {
        List<NotificationResponse> notificationResponselist = new ArrayList<>();
        try {
            UserAccount account = userAccountRepository.findByAccountId(accountId);
            List<NotificationView> notificationList = notificationViewRepository.findNotificationByAccountId(account);
            for (NotificationView notifi : notificationList) {
                NotificationResponse notificationResponse = new NotificationResponse();
                notificationResponse.setNotificationId(String.valueOf(notifi.getNotificationId().getNotificationId()));
                notificationResponse.setCategoryId(String.valueOf(notifi.getNotificationId().getCategoryId()));
                notificationResponse.setNotificationType(notifi.getNotificationId().getNotificationTypeID().getNotificationType());
                notificationResponse.setIsRead(String.valueOf(notifi.getIsRead()));
                notificationResponse.setPayload(getNotificationPayload(notifi, notifi.getNotificationId().getNotificationTypeID(), accountId, timeZone));
                notificationResponselist.add(notificationResponse);
            }
            return notificationResponselist;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to customise notification. Caught error: " + e, new Throwable(allStackTraces));
            return notificationResponselist;
        }
    }

    /**
     * gets all the notifications for an account id that are present after the given notificationId
     * @return List of NotificationResponse
     * @Function: Customize notification for user view
     */
    private List<NotificationResponse> getNotificationsAfterSpecifiedId(Long accountId, String timeZone, Long notificationId) {
        List<NotificationResponse> notificationResponselist = new ArrayList<>();
        try {
            UserAccount account = userAccountRepository.findByAccountId(accountId);
            List<NotificationView> notificationList = notificationViewRepository.findNotificationsAfterGivenId(account, notificationId);
            for (NotificationView notifi : notificationList) {
                NotificationResponse notificationResponse = new NotificationResponse();
                notificationResponse.setNotificationId(String.valueOf(notifi.getNotificationId().getNotificationId()));
                notificationResponse.setCategoryId(String.valueOf(notifi.getNotificationId().getCategoryId()));
                notificationResponse.setNotificationType(notifi.getNotificationId().getNotificationTypeID().getNotificationType());
                notificationResponse.setIsRead(String.valueOf(notifi.getIsRead()));
                notificationResponse.setPayload(getNotificationPayload(notifi, notifi.getNotificationId().getNotificationTypeID(), accountId, timeZone));
                notificationResponselist.add(notificationResponse);
            }
            return notificationResponselist;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to customise notification. Caught error: " + e, new Throwable(allStackTraces));
            return notificationResponselist;
        }
    }

    /**
     *
     * @return notification payload
     * @Function: update some payload values for user
     */
    private Object getNotificationPayload(NotificationView notificationView, NotificationType notificationTypeId, Long accountId, String timeZone) {
        Payload load = gson.fromJson(notificationView.getNotificationId().getPayload(), Payload.class);
        load.setAccountId(String.valueOf(accountId));
        load.setNotificationId(String.valueOf(notificationView.getNotificationId().getNotificationId()));
        load.setNotificationType(notificationTypeId.getNotificationType());
        load.setCategoryId(String.valueOf(notificationView.getNotificationId().getCategoryId()));
        load.setCreatedDateTime(String.valueOf(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(notificationView.getNotificationId().getCreatedDateTime(), timeZone)));
        return load;
    }

    /**
     * This function marks the isread for an account to true.
     * @return boolean
     */
    public boolean markNotificationCheckedForAUser(Long notificationId, Long accountId) {
        try {
            Notification notification = notificationRepository.findByNotificationId(notificationId);
            if (notification == null) {
                return false;
            }
            UserAccount account = userAccountRepository.findByAccountId(accountId);
            return notificationViewRepository.setIsReadTrueByNotificationIdAndAccountId(notification, account)!=0;
        } catch (Exception e) {
            logger.error("Unable to update isRead for notificationId: " + notificationId + " and accountId "+accountId + e);
            return false;
        }
    }

    //Below are the services for meeting

    /**
     * @return payload in the form of hashmap
     * @Function: create notification when a new meeting is created.
     */
    public List<HashMap<String, String>> newMeetingNotification(Meeting meeting, String timeZone, List<Attendee> savedAttendee) {
        Notification notification = new Notification();
        //for notification type
        notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.MEETING_INVITE));
        notification.setCategoryId(NotificationTypeToCategory.MEETING_INVITE.getCategoryId());
        //for required fields from meeting
        notification.setOrgId(organizationRepository.findByOrgId(meeting.getOrgId()));
        notification.setBuId(meeting.getBuId());
        notification.setProjectId(projectRepository.findByProjectId(meeting.getProjectId()));
        notification.setTeamId(teamRepository.findByTeamId(meeting.getTeamId()));
        notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(meeting.getCreatedAccountId(), true));
        notification.setTaskNumber(null);
        notification.setMeetingId(meeting.getMeetingId());
        //for title and body
        notification.setNotificationTitle(Constants.NotificationType.MEETING_INVITE_TITLE);
        UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(meeting.getCreatedAccountId())).getFkUserId().getUserId());
        if (savedAttendee == null || savedAttendee.isEmpty() || savedAttendee.size() < 2) {
            notification.setNotificationBody(name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName()) + " has created a meeting with you.");
        } else {
            notification.setNotificationBody(name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName()) + " has created a meeting with you and others.");
        }
        notification.setPayload(setMeetingPayload(notification, meeting, Constants.NotificationType.MEETING_INVITE,timeZone, NotificationTypeToCategory.MEETING_INVITE.getCategoryId()));

        Notification notifi = notificationRepository.save(notification);
        if (savedAttendee != null && !savedAttendee.isEmpty()) {
            List<Attendee> notifyAttendees = new ArrayList<>();
            HashSet<Long> accountIds = new HashSet<>();
            for (Attendee attendee : savedAttendee) {
                if(!Objects.equals(attendee.getAccountId(), meeting.getCreatedAccountId())) {
                    notifyAttendees.add(attendee);
                    accountIds.add(attendee.getAccountId());
                }
            }
            meetingNotificationView(notifi, notifyAttendees);
            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @return payload in the form of string
     */
    private String setMeetingPayload(Notification notification, Meeting meeting, String notificationType, String timeZone, Integer categoryId) {
        Payload meetingPayload = new Payload();
        meetingPayload.setAccountId(null);
        meetingPayload.setNotificationId(String.valueOf(notification.getNotificationId()));
        meetingPayload.setNotificationType(notificationType);
        meetingPayload.setCreatedDateTime(String.valueOf(DateTimeUtils.convertServerDateToUserTimezone(meeting.getCreatedDateTime(),timeZone)));
        meetingPayload.setTitle(notification.getNotificationTitle());
        meetingPayload.setBody(notification.getNotificationBody());
        meetingPayload.setCategoryId(String.valueOf(categoryId));
        meetingPayload.setTeamId(String.valueOf(meeting.getTeamId()));
        meetingPayload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));
        if (meeting.getMeetingTypeIndicator().equals(com.tse.core_application.constants.Constants.Meeting_Type_Indicator.ONLINE)) {
            meetingPayload.setMeetingMode(Constants.MeetingMode.ONLINE);
        } else if (meeting.getMeetingTypeIndicator().equals(com.tse.core_application.constants.Constants.Meeting_Type_Indicator.OFFLINE)) {
            meetingPayload.setMeetingMode(Constants.MeetingMode.OFFLINE);
        } else {
            meetingPayload.setMeetingMode(Constants.MeetingMode.HYBRID);
        }

        meetingPayload.setMeetingVenue(meeting.getVenue());
        meetingPayload.setMeetingId(meeting.getMeetingId().toString());
        meetingPayload.setMeetingDate(DateTimeUtils.convertServerDateToUserTimezone(meeting.getStartDateTime(),timeZone).toString());

        //Convert to String
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String taskPayloadString;
        try {
            taskPayloadString = objectWriter.writeValueAsString(meetingPayload);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Json to string"+e);
            throw new JsonException("Error converting Json to string");
        }

        return taskPayloadString;
    }

//    /**
//     * @param notification
//     * @param recurringMeeting
//     * @param notificationType
//     * @return payload in the form of string
//     */
//    private String setRecurringMeetingPayload(Notification notification, RecurringMeeting recurringMeeting, String notificationType, String timeZone) {
//        Payload recurringMeetingPayload = new Payload();
//        recurringMeetingPayload.setAccountId(null);
//        recurringMeetingPayload.setNotificationId(String.valueOf(notification.getNotificationId()));
//        recurringMeetingPayload.setNotificationType(notificationType);
//        recurringMeetingPayload.setTitle(notification.getNotificationTitle());
//        recurringMeetingPayload.setBody(notification.getNotificationBody());
//        recurringMeetingPayload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));
//        if (recurringMeeting.getMeetingType().equalsIgnoreCase(Constants.MeetingMode.ONLINE)) {
//            recurringMeetingPayload.setMeetingMode(Constants.MeetingMode.ONLINE);
//        } else if (recurringMeeting.getMeetingType().equalsIgnoreCase(Constants.MeetingMode.OFFLINE)) {
//            recurringMeetingPayload.setMeetingMode(Constants.MeetingMode.OFFLINE);
//        } else {
//            recurringMeetingPayload.setMeetingMode(Constants.MeetingMode.HYBRID);
//        }
//        recurringMeetingPayload.setMeetingVenue(recurringMeeting.getVenue());
//        recurringMeetingPayload.setMeetingDate(DateTimeUtils.convertServerDateToUserTimezone(recurringMeeting.getRecurringMeetingStartDateTime(),timeZone).toString());
//
//        //Convert to String
//        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
//        String taskPayloadString;
//        try {
//            taskPayloadString = objectWriter.writeValueAsString(recurringMeetingPayload);
//        } catch (JsonProcessingException e) {
//            logger.error("Error converting Json to string"+e);
//            throw new JsonException("Error converting Json to string");
//        }
//
//        return taskPayloadString;
//    }

    /**
     * @Function: create view for notification for all attendees
     */
    private void meetingNotificationView(Notification notification,List<Attendee> AttendeeList) {
        for (Attendee attendee : AttendeeList) {
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(attendee.getAccountId(), true);
            if (userAccount == null) {
                continue;
            }
            NotificationView notificationView = new NotificationView();
            notificationView.setNotificationId(notification);
            notificationView.setIsRead(false);
            notificationView.setAccountId(userAccount);
            notificationViewRepository.save(notificationView);
        }
    }

    /**
     *
     * @param meeting
     * @param meetingDbPrev
     * @return payload in the form of hashmap
     * @Function: create notification if there is an update in the meeting
     */
    public List<HashMap<String,String>> updateMeetingNotification(Meeting meeting, Meeting meetingDbPrev, String timeZone, List<Attendee> updatedAttendeeList,
                                                                  List<Attendee> preAttendeeList, ArrayList<String> updatedMeetingsFieldsByUser){
        Notification notification=new Notification();
        //for notification type
        notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.MEETING_UPDATE));
        notification.setCategoryId(NotificationTypeToCategory.MEETING_UPDATE.getCategoryId());
        //for required fields from meeting
        notification.setOrgId(organizationRepository.findByOrgId(meeting.getOrgId()));
        notification.setBuId(meeting.getBuId());
        notification.setProjectId(projectRepository.findByProjectId(meeting.getProjectId()));
        notification.setTeamId(teamRepository.findByTeamId(meeting.getTeamId()));
        notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(meeting.getUpdatedAccountId(), true));
        notification.setMeetingId(meeting.getMeetingId());
        notification.setTaskNumber(null);
        //for title and body
        notification.setNotificationTitle(updateMeetingTitle(meeting.getUpdatedAccountId(),meeting.getMeetingNumber()));
        notification.setNotificationBody(updateMeetingBody(meeting, meetingDbPrev,updatedAttendeeList,preAttendeeList, updatedMeetingsFieldsByUser,timeZone));

        notification.setPayload(setMeetingPayload(notification,meeting,Constants.NotificationType.MEETING_UPDATE,timeZone, NotificationTypeToCategory.MEETING_UPDATE.getCategoryId()));

        Notification notifi = notificationRepository.save(notification);
        if (updatedAttendeeList != null && !updatedAttendeeList.isEmpty()) {
            meetingNotificationView(notifi, updatedAttendeeList);
            HashSet<Long> accountIds = new HashSet<>();
            for (Attendee attendee : updatedAttendeeList) {
                accountIds.add(attendee.getAccountId());
            }
            accountIds.remove(meeting.getUpdatedAccountId());
            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        } else {
            return Collections.emptyList();
        }
    }

    /** method to send attendee disinvite notification to organizer when a user is removed from an entity */
    public List<HashMap<String, String>> disInviteAttendeeFromMeetingNotification(Meeting meeting, Long disInvitedUserAccountId, String timeZone) {
        HashSet<Long> accountIds = new HashSet<>();
        Notification notifi = new Notification();
        try {
            Notification notification = new Notification();
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.MEETING_UPDATE));
            notification.setCategoryId(NotificationTypeToCategory.MEETING_UPDATE.getCategoryId());
            notification.setOrgId(organizationRepository.findByOrgId(meeting.getOrgId()));
            notification.setBuId(meeting.getBuId());
            notification.setProjectId(projectRepository.findByProjectId(meeting.getProjectId()));
            notification.setTeamId(teamRepository.findByTeamId(meeting.getTeamId()));
            if (meeting.getUpdatedAccountId() != null) {
                notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(meeting.getUpdatedAccountId(), true));
            }
            notification.setMeetingId(meeting.getMeetingId());
            notification.setTaskNumber(null);

            UserAccount userAccountDisInvitedUser = userAccountRepository.findFirstByAccountId(disInvitedUserAccountId);
            notification.setNotificationTitle("Meeting " + meeting.getMeetingNumber() + "updated: " + "Attendee " + userAccountDisInvitedUser.getFkUserId().getFirstName() + " " + userAccountDisInvitedUser.getFkUserId().getLastName() + " is disinvited from the meeting.");
            notification.setNotificationBody(userAccountDisInvitedUser.getFkUserId().getFirstName() + " " + userAccountDisInvitedUser.getFkUserId().getLastName() + " has been removed as an attendee from the meeting with the title: " + "'" + meeting.getMeetingKey() + "'");

            notification.setPayload(setMeetingPayload(notification, meeting, Constants.NotificationType.MEETING_UPDATE, timeZone, NotificationTypeToCategory.MEETING_UPDATE.getCategoryId()));

            notifi = notificationRepository.save(notification);
            accountIds.add(meeting.getOrganizerAccountId());
            NotificationView notificationView = new NotificationView();
            notificationView.setNotificationId(notifi);
            notificationView.setIsRead(false);
            UserAccount organizerAccount = userAccountRepository.findByAccountIdAndIsActive(meeting.getOrganizerAccountId(), true);
            if (organizerAccount != null) {
                notificationView.setAccountId(organizerAccount);
                notificationViewRepository.save(notificationView);
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for update meeting" + e.getMessage(), new Throwable(allStackTraces));
        }
        return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
    }

    private String updateMeetingTitle(Long accountId, String meetingNumber){
        UserName name=userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accountId)).getFkUserId().getUserId());
        return name.getFirstName()+" "+((name.getLastName() == null) ? "" : name.getLastName())+" has updated the meeting "+meetingNumber+".";
    }

    /**
     *
     * @return new body for an update meeting
     */
    private String updateMeetingBody(Meeting newMeeting, Meeting meetingDbPrev , List<Attendee> updatedAttendeeList,
                                     List<Attendee> preAttendeeList, ArrayList<String> updatedMeetingsFieldsByUser, String timeZone) {

        StringBuilder body = new StringBuilder();

        //checking for updated fields
        //Meeting Mode
        if(updatedMeetingsFieldsByUser.contains("meetingTypeIndicator")){
                if(newMeeting.getMeetingTypeIndicator().equals(com.tse.core_application.constants.Constants.Meeting_Type_Indicator.ONLINE)){
                    body.append("Type is changed to "+Constants.MeetingMode.ONLINE+". \n");
                }
                else if(newMeeting.getMeetingTypeIndicator().equals(com.tse.core_application.constants.Constants.Meeting_Type_Indicator.OFFLINE)){
                    body.append("Type is changed to "+Constants.MeetingMode.OFFLINE+". \n");
                }
                else{
                    body.append("Type is changed to "+Constants.MeetingMode.HYBRID+". \n");
                }
        }
        //Venue
        if(updatedMeetingsFieldsByUser.contains("venue")){
            body.append("Venue is changed to \"").append(newMeeting.getVenue()).append("\". \n");
        }
        //Organizer Account Id
        if(updatedMeetingsFieldsByUser.contains("organizerAccountId")){
            UserName organizerName=userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(newMeeting.getOrganizerAccountId())).getFkUserId().getUserId());
            body.append("Organizer is changed to \"").append(organizerName.getFirstName()).append(" ").append((organizerName.getLastName() == null) ? "" : organizerName.getLastName()+"\". \n");
        }
        //StartDateTime
        if(updatedMeetingsFieldsByUser.contains("startDateTime")){
            LocalDate date = DateTimeUtils.convertServerDateToUserTimezone(newMeeting.getStartDateTime(),timeZone).toLocalDate();
            LocalTime time = DateTimeUtils.convertServerDateToUserTimezone(newMeeting.getStartDateTime(),timeZone).toLocalTime();
            body.append("Start time is changed to ").append(date).append(" at ").append(time).append(". \n");
        }
        //EndDateTime
        if(updatedMeetingsFieldsByUser.contains("endDateTime")){
            LocalDate date = DateTimeUtils.convertServerDateToUserTimezone(newMeeting.getEndDateTime(),timeZone).toLocalDate();
            LocalTime time = DateTimeUtils.convertServerDateToUserTimezone(newMeeting.getEndDateTime(),timeZone).toLocalTime();
            body.append("End time is changed to ").append(date).append(" at ").append(time).append(". \n");
        }
        //Meeting duration
        if(updatedMeetingsFieldsByUser.contains("duration")){
            body.append("Duration is changed to ").append(newMeeting.getDuration()).append(". \n");
        }
        //Agenda
        if(updatedMeetingsFieldsByUser.contains("agenda")){
                body.append("Agenda is changed to \"").append(newMeeting.getAgenda()).append("\". \n");
        }
        //Attendee
        if(updatedMeetingsFieldsByUser.contains("attendeeResponseList")){
            ArrayList<Attendee> newlyAddedAttendee = new ArrayList<>(updatedAttendeeList.stream().filter(n -> !preAttendeeList.contains(n)).collect(Collectors.toList()));
            ArrayList<Attendee> removedAttendee = new ArrayList<>(preAttendeeList.stream().filter(n -> !updatedAttendeeList.contains(n)).collect(Collectors.toList()));
            if(!newlyAddedAttendee.isEmpty()){
                if(newlyAddedAttendee.size()<5) {
                    for (Attendee attendee : newlyAddedAttendee) {
                        UserName attendeeName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(attendee.getAccountId())).getFkUserId().getUserId());
                        body.append(attendeeName.getFirstName()).append(" ").append((attendeeName.getLastName() == null) ? "" : attendeeName.getLastName()).append(", ");
                    }
                    body.deleteCharAt(body.length() - 2);
                    body.append((newlyAddedAttendee.size() > 1) ? "are" : "is").append(" also invited. \n");
                }
                else{
                    body.append("New attendees are invited. \n");
                }
            }
            if(!removedAttendee.isEmpty()){
                if(removedAttendee.size()<5) {
                    for (Attendee attendee : removedAttendee) {
                        UserName attendeeName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(attendee.getAccountId())).getFkUserId().getUserId());
                        body.append(attendeeName.getFirstName()).append(" ").append((attendeeName.getLastName() == null) ? "" : attendeeName.getLastName()).append(", ");
                    }
                    body.deleteCharAt(body.length() - 2);
                    body.append((removedAttendee.size() > 1) ? "are" : "is").append(" removed. \n");
                }
                else{
                    body.append("Some attendees are removed. \n");
                }
            }
        }

        return body.toString();
    }

    /**
     *
     * @return List of hash map of type payload
     * @Function: create notification when a leave is approved or cancelled after approval for all those to which applicant has requested to notify.
     */
    public List<HashMap<String, String>> notifyForLeaveApplication(ChangeLeaveStatusRequest changeLeaveStatusRequest, LeaveApplicationNotificationRequest leaveApplicationNotificationRequest, String timeZone) {

        Notification notification = new Notification();

        //for required fields from meeting
        notification.setOrgId(organizationRepository.findByOrgId(changeLeaveStatusRequest.getOrgId()));
        notification.setBuId(changeLeaveStatusRequest.getBuId());
        notification.setProjectId(projectRepository.findByProjectId(changeLeaveStatusRequest.getProjectId()));
        notification.setTeamId(teamRepository.findByTeamId(changeLeaveStatusRequest.getTeamId()));
        notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(leaveApplicationNotificationRequest.getApproverAccountId(), true));
        notification.setTaskNumber(null);
        notification.setMeetingId(null);
        notification.setLeaveApplicationId(leaveApplicationNotificationRequest.getLeaveApplicationId());
        UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(leaveApplicationNotificationRequest.getApplicantAccountId())).getFkUserId().getUserId());
        if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), "APPROVED")) {
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.LEAVE_APPROVAL));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_APPROVAL.getCategoryId());
            //for title and body
            notification.setNotificationTitle(LEAVE_APPROVAL_TITLE);
            notification.setNotificationBody(name.getFirstName() + " " + ((name.getLastName() == null) ? "" : name.getLastName()) + " will be on leave from: "
            +leaveApplicationNotificationRequest.getFromDate()+" to: "
            +leaveApplicationNotificationRequest.getToDate()+". ");
            notification.setPayload(setLeaveApplicationPayload(notification, Constants.NotificationType.LEAVE_APPROVAL, NotificationTypeToCategory.LEAVE_APPROVAL.getCategoryId()));
        } else if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), "REJECTED")) {
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.LEAVE_REJECTED));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_REJECTED.getCategoryId());
            //for title and body
            notification.setNotificationTitle(LEAVE_REJECTED_TITLE);
            notification.setNotificationBody(name.getFirstName() + " " + ((name.getLastName() == null) ? "" : name.getLastName()) + "'s leave is rejected.");
            notification.setPayload(setLeaveApplicationPayload(notification, Constants.NotificationType.LEAVE_REJECTED, NotificationTypeToCategory.LEAVE_REJECTED.getCategoryId()));
        } else if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), Constants.NOTIFY_FOR_LEAVE_EXPIRY)) {
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(LEAVE_EXPIRED));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_EXPIRED.getCategoryId());
            //for title and body
            notification.setNotificationTitle(Constants.LEAVE_EXPIRED_TITLE);
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String formattedDate = today.format(formatter);
            String firstName = name.getFirstName();
            String lastName = (name.getLastName() == null) ? "" : name.getLastName();
            String message = firstName + " " + lastName +
                    "'s leave application has expired on " + formattedDate + ".";
            notification.setNotificationBody(message);
            notification.setPayload(setLeaveApplicationPayload(notification, LEAVE_EXPIRED, NotificationTypeToCategory.LEAVE_EXPIRED.getCategoryId()));
        } else if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), Constants.NOTIFY_FOR_LEAVE_CANCELLED)) {
            UserName cancelledByName = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(leaveApplicationNotificationRequest.getApproverAccountId())).getFkUserId().getUserId());
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(LEAVE_CANCELLED));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_CANCELLED.getCategoryId());
            //for title and body
            notification.setNotificationTitle(LEAVE_CANCELLED_TITLE);
            notification.setNotificationBody("Your leave was cancelled by " + cancelledByName.getFirstName() + " " + ((cancelledByName.getLastName() == null) ? "" : cancelledByName.getLastName()));
            notification.setPayload(setLeaveApplicationPayload(notification, LEAVE_CANCELLED, NotificationTypeToCategory.LEAVE_CANCELLED.getCategoryId()));
        } else if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), Constants.WAITING_FOR_CANCEL)) {
            //for notification type
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(LEAVE_CANCELLED));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_CANCELLED.getCategoryId());
            //for title and body
            notification.setNotificationTitle(LEAVE_CANCELLATION_REQUEST);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            LocalDate fromDateObj = LocalDate.parse(leaveApplicationNotificationRequest.getFromDate());
            LocalDate toDateObj = LocalDate.parse(leaveApplicationNotificationRequest.getToDate());
            String fromDate = fromDateObj.format(formatter);
            String toDate = toDateObj.format(formatter);
            String firstName = name.getFirstName();
            String lastName = (name.getLastName() == null) ? "" : name.getLastName();
            String message;
            if (fromDateObj.equals(toDateObj)) {
                message = firstName + " " + lastName +
                        " has requested leave cancellation for " + fromDate + ".";
            } else {
                message = firstName + " " + lastName +
                        " has requested leave cancellation from " + fromDate +
                        " to " + toDate + ".";
            }
            notification.setNotificationBody(message);
            notification.setPayload(setLeaveApplicationPayload(notification, LEAVE_CANCELLED, NotificationTypeToCategory.LEAVE_CANCELLED.getCategoryId()));
        }
        Notification notifi = notificationRepository.save(notification);
        if (leaveApplicationNotificationRequest.getNotifyTo()!=null && !leaveApplicationNotificationRequest.getNotifyTo().isEmpty()) {
                leaveNotificationView(notifi,new HashSet<>(leaveApplicationNotificationRequest.getNotifyTo()));
                return updatingPayloadFormat(new HashSet<>(leaveApplicationNotificationRequest.getNotifyTo()), notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        }
        return Collections.emptyList();
    }

    public List<HashMap<String, String>> notifyForCreateOrUpdateLeaveApplication(LeaveApplicationResponse leaveApplicationResponse, Boolean onCreate, String timeZone) {

        if (leaveApplicationResponse == null) {
            return Collections.emptyList();
        }
        Notification notification = new Notification();

        Organization org = organizationRepository.findByOrgId(userAccountRepository.findOrgIdByAccountIdAndIsActive(leaveApplicationResponse.getApplicantDetails().getAccountId(), true).getOrgId());
        if (org != null) {
            notification.setOrgId(org);
        }
        notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(leaveApplicationResponse.getApplicantDetails().getAccountId(), true));
        notification.setTaskNumber(null);
        notification.setMeetingId(null);
        notification.setLeaveApplicationId(leaveApplicationResponse.getLeaveApplicationId());

        if (onCreate) {
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.LEAVE_APPLIED));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_APPLIED.getCategoryId());
            //for title and body
            notification.setNotificationTitle(LEAVE_APPLIED_TITLE);
            String firstName = leaveApplicationResponse.getApplicantDetails().getFirstName();
            String lastName = leaveApplicationResponse.getApplicantDetails().getLastName() == null ? "" : leaveApplicationResponse.getApplicantDetails().getLastName();
            LocalDate fromDate = leaveApplicationResponse.getFromDate();
            LocalDate toDate = leaveApplicationResponse.getToDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String formattedFromDate = fromDate.format(formatter);
            String formattedToDate = toDate.format(formatter);
            String message;
            if (fromDate.equals(toDate)) {
                message = firstName + " " + lastName +
                        " has applied for leave on " + formattedFromDate + ".";
            } else {
                message = firstName + " " + lastName +
                        " has applied for leave from " + formattedFromDate +
                        " to " + formattedToDate + ".";
            }
            notification.setNotificationBody(message);
            notification.setPayload(setLeaveApplicationPayload(notification, Constants.NotificationType.LEAVE_APPLIED, NotificationTypeToCategory.LEAVE_APPLIED.getCategoryId()));
        }
        else {
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.LEAVE_UPDATED));
            notification.setCategoryId(NotificationTypeToCategory.LEAVE_UPDATED.getCategoryId());
            notification.setNotificationTitle(LEAVE_UPDATED_TITLE);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String firstName = leaveApplicationResponse.getApplicantDetails().getFirstName();
            String lastName = (leaveApplicationResponse.getApplicantDetails().getLastName() == null)
                    ? "" : leaveApplicationResponse.getApplicantDetails().getLastName();
            LocalDate fromDate = leaveApplicationResponse.getFromDate();
            LocalDate toDate = leaveApplicationResponse.getToDate();
            String formattedFromDate = fromDate.format(formatter);
            String formattedToDate = toDate.format(formatter);
            String message;
            if (fromDate.equals(toDate)) {
                message = firstName + " " + lastName +
                        " has updated leave on " + formattedFromDate + " .";
            } else {
                message = firstName + " " + lastName +
                        " has updated leave from " + formattedFromDate +
                        " to " + formattedToDate + " .";
            }
            notification.setNotificationBody(message);
            notification.setPayload(setLeaveApplicationPayload(notification, Constants.NotificationType.LEAVE_UPDATED, NotificationTypeToCategory.LEAVE_UPDATED.getCategoryId()));
        }

        HashSet<Long> notifyToSet = new HashSet<>();

        //setNotifier accountId
        if (leaveApplicationResponse.getNotifyTo() != null) {
            notifyToSet.addAll(leaveApplicationResponse.getNotifyTo());
        }
        //setting approver accountId

        if (leaveApplicationResponse.getApprover() != null && leaveApplicationResponse.getApprover().getAccountId() != null) {
            notifyToSet.add(leaveApplicationResponse.getApprover().getAccountId());
        }

        Notification notifi = notificationRepository.save(notification);
        if (notifyToSet!=null && !notifyToSet.isEmpty()) {
            leaveNotificationView(notifi, notifyToSet);
            return updatingPayloadFormat(notifyToSet, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        }
        return Collections.emptyList();
    }

    /**
     *
     * @Function: create entry in notification view for all account ids to whom notification will be sending.
     */
    private void leaveNotificationView(Notification notifi, Set<Long> notifyTo) {
        for (Long userAccountId : notifyTo) {
            if (userAccountId != null) {
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(userAccountId, true);
                if (userAccount == null) {
                    continue;
                }
                NotificationView notificationView = new NotificationView();
                notificationView.setNotificationId(notifi);
                notificationView.setIsRead(false);
                notificationView.setModifiedDateTime(null);
                notificationView.setAccountId(userAccount);
                notificationViewRepository.save(notificationView);
            }
        }
    }

    /**
     *
     * @param notification
     * @param notificationType
     * @Function: create payload for notification
     */
    private String setLeaveApplicationPayload(Notification notification, String notificationType, Integer categoryId) {
        Payload payload = new Payload();
        payload.setAccountId(null);
        payload.setNotificationId(String.valueOf(notification.getNotificationId()));
        payload.setNotificationType(notificationType);
        payload.setTitle(notification.getNotificationTitle());
        payload.setBody(notification.getNotificationBody());
        payload.setCategoryId(String.valueOf(categoryId));
        payload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));

        //Convert to String
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String taskPayloadString;
        try {
            taskPayloadString = objectWriter.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Json to string" + e);
            throw new JsonException("Error converting Json to string");
        }

        return taskPayloadString;
    }

    /**
     * @param accountIds
     * @Function: mark All Notification as read
     */
    public boolean markAllNotificationCheckedForAUser(List<Long> accountIds) {
        try {
            for(Long accountId: accountIds) {
                UserAccount account = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
                if (account != null) {
                    notificationViewRepository.setIsReadTrueByAccountId(account);
                }
            }
            return true;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to mark all notification as read for accountIds: "+accountIds + e,new Throwable(allStackTraces));
            return false;
        }
    }

    /**
     * @Function: clear single notification view as per the account id
     */
    public boolean clearNotificationForAUser(List<Long> notificationIdList, List<Long> accountIds) {
        try {
            List<UserAccount> userAccountsList = new ArrayList<>();
            for(Long accountId:accountIds) {
                UserAccount account = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
                if (account != null) {
                    userAccountsList.add(account);
                }
            }
            for(Long notificationId:notificationIdList) {
                Notification notification = notificationRepository.findByNotificationId(notificationId);
                notificationViewRepository.removeByNotificationIdAndAccountId(notification, userAccountsList);
            }
            return true;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to clear notification with notificationIds: " + notificationIdList + " and accountId "+accountIds + e,new Throwable(allStackTraces));
            return false;
        }
    }

    /**
     * @return clear all notifications view as per the account id
     */
    public boolean clearAllNotificationForAUser(List<Long> accountIds) {
        try {
            List<UserAccount> userAccountsList = new ArrayList<>();
            for(Long accountId:accountIds) {
                UserAccount account = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
                if (account != null) {
                    userAccountsList.add(account);
                }
            }
            return notificationViewRepository.removeByAccountId(userAccountsList)!=0;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to clear all notification for accountId: "+accountIds+ e,new Throwable(allStackTraces));
            return false;
        }
    }

    /**
     * @Function: Create notification when a new comment is added to a task
     */
    public List<HashMap<String, String>> createNotificationForNewCommentInTask(Task task, Comment comment, String timeZone, List<Long> taggedAccountIds) {

        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdObserver1()!=null) ? task.getFkAccountIdObserver1().getAccountId():-1),
                            ((task.getFkAccountIdObserver2()!=null) ? task.getFkAccountIdObserver2().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                            ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            Integer roleIdToSearch = Constants.HIGHER_ROLE_IDS.get(0);
            if (task.getFkAccountIdAssigned() != null) {
                roleIdToSearch = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
            }
            accountIds.addAll(new HashSet<>(accessDomainService.getActiveAccountIdsOfHigherRoleMembersInTeam(task.getFkTeamId().getTeamId(),roleIdToSearch)));
            accountIds.remove((long) -1);
            accountIds.remove(comment.getPostedByAccountId());
            if(taggedAccountIds != null) taggedAccountIds.forEach(accountIds::remove);

            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(comment.getPostedByAccountId())).getFkUserId().getUserId());
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            //Checking the changes made
            Notification notification = newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(), task.getBuId(), task.getFkProjectId(), task.getFkTeamId(), task.getFkAccountId(), task.getTaskNumber(), NotificationTypeToCategory.COMMENT.getCategoryId(),comment.getPostedByAccountId().toString());
            notification.setNotificationTitle("New comment for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
            notification.setNotificationBody(byName + " has added a new comment.");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.UPDATE_TASK_COMMENTS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_COMMENTS, NotificationTypeToCategory.UPDATE_TASK_COMMENTS.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATE_TASK_COMMENTS));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for add comment " + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    /**
     * @Function: Create notification when a user is mentioned/ tagged in a comment
     */
    public List<HashMap<String, String>> createMentionInCommentNotification(Task task, Comment comment, String timeZone, List<Long> taggedAccountIds,String headerAccountIds) {

        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>();
            if(taggedAccountIds != null) accountIds.addAll(taggedAccountIds);

            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(comment.getPostedByAccountId())).getFkUserId().getUserId());
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            //Checking the changes made
            Notification notification = newNotificationObjectCreaterForNotificationCreatorAccountId(task.getFkOrgId(), task.getBuId(), task.getFkProjectId(), task.getFkTeamId(), task.getFkAccountId(), task.getTaskNumber(), NotificationTypeToCategory.MENTION_ALERTS_TC.getCategoryId(),headerAccountIds);
            notification.setNotificationTitle("Mentioned in comment for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());
            notification.setCommentLogId(comment.getCommentLogId());
            notification.setNotificationBody(byName + " mentioned you in a comment.");
            notification.setTaggedAccountIds(taggedAccountIds);

            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), MENTION_ALERTS_TC, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_COMMENTS, NotificationTypeToCategory.MENTION_ALERTS_TC.getCategoryId()));
            System.out.println(notificationTypeRepository.findByNotificationType(MENTION_ALERTS_TC));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(MENTION_ALERTS_TC));
            //here need to convert Long<List> to String
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for add comment " + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    /**
     * Create notification when a new message is added to a group (team/ org)
     * @param taggedAccountIds -- accountIds of users that have been tagged in the message.
     */
    public List<HashMap<String, String>> createNotificationForMessageInEntity(GroupConversation gcMessage, String timeZone, List<Long> taggedAccountIds) {
        try {
            Integer entityTypeId = gcMessage.getEntityTypeId();
            Long entityId = gcMessage.getEntityId();
            Long groupConversationId = gcMessage.getGroupConversationId();
            HashSet<Long> accountIds = new HashSet<>();
            String entityName = null;
            Organization orgEntity = null;
            Team teamEntity = null;
            UserAccount postedByAccountIdUserAccount = userAccountRepository.findByAccountIdAndIsActive(gcMessage.getPostedByAccountId(), true);

            if(entityTypeId.equals(Constants.EntityTypes.ORG)) {
                orgEntity = organizationRepository.findByOrgId(entityId);
            } else if (entityTypeId .equals(Constants.EntityTypes.TEAM)) {
                teamEntity = teamRepository.findByTeamId(entityId);
            }

            //Stakeholders list
            if(entityTypeId != null && entityId != null) {
                if(Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
//                    List<AccountId> allAccountIds = userAccountRepository.findAccountIdByOrgId(entityId);
                    List<AccountId> allAccountIds = userAccountRepository.findAccountIdByOrgIdAndIsActive(entityId, true);
                    accountIds = (HashSet<Long>) allAccountIds.stream().map(AccountId::getAccountId).collect(Collectors.toSet());
                    entityName = orgEntity.getOrganizationName();
                } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
                    accountIds = new HashSet<>(accessDomainRepository.findDistinctAccountIdsByEntityTypeAndEntityTypeIdAndIsActive(entityId, entityTypeId));
                    entityName = teamEntity.getTeamName();
                }
            }
            accountIds.remove(gcMessage.getPostedByAccountId());
            if (taggedAccountIds != null && !taggedAccountIds.isEmpty()) taggedAccountIds.forEach(accountIds::remove);

            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(gcMessage.getPostedByAccountId())).getFkUserId().getUserId());
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            Notification notification = new Notification();
            if(entityTypeId.equals(Constants.EntityTypes.ORG)){
                notification = newNotificationObjectCreater(orgEntity, null, null, null, postedByAccountIdUserAccount, null, NotificationTypeToCategory.GROUP_MESSAGE.getCategoryId());
            } else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
                notification = newNotificationObjectCreater(null, null, null, teamEntity, postedByAccountIdUserAccount, null, NotificationTypeToCategory.GROUP_MESSAGE.getCategoryId());
            }
            notification.setNotificationTitle("New message in " + entityName + " group");
            notification.setNotificationBody(byName + " has added a new message.");
            notification.setPayload(gcPayload(gcMessage, notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.GROUP_MESSAGE, null, null, NotificationTypeToCategory.GROUP_MESSAGE.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.GROUP_MESSAGE));
            notification.setAccountId(postedByAccountIdUserAccount);
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            return updatingPayloadFormatForGc(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime(), gcMessage);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for add message in entity" + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    /**
     * Create notification when a new message is added to a group (team/ org)
     * and there is mention to a user (@UserMention)
     */
    public List<HashMap<String, String>> createNotificationForTagInGroupConversation(GroupConversation gcMessage, List<Long> taggedAccountIds, String timeZone,String headerAccountIds) {
        try {
            Integer entityTypeId = gcMessage.getEntityTypeId();
            Long entityId = gcMessage.getEntityId();
            HashSet<Long> accountIds = new HashSet<>();
            if(taggedAccountIds != null) accountIds.addAll(taggedAccountIds);
            String entityName = null;
            Organization orgEntity = null;
            Team teamEntity = null;
            UserAccount postedByAccountIdUserAccount = userAccountRepository.findByAccountIdAndIsActive(gcMessage.getPostedByAccountId(), true);

            if(entityTypeId.equals(Constants.EntityTypes.ORG)) {
                orgEntity = organizationRepository.findByOrgId(entityId);
                entityName = orgEntity.getOrganizationName();
            } else if (entityTypeId .equals(Constants.EntityTypes.TEAM)) {
                teamEntity = teamRepository.findByTeamId(entityId);
                entityName = teamEntity.getTeamName();
            }

            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(gcMessage.getPostedByAccountId())).getFkUserId().getUserId());
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            Notification notification = new Notification();
            if(entityTypeId.equals(Constants.EntityTypes.ORG)){
                notification = newNotificationObjectCreater(orgEntity, null, null, null, postedByAccountIdUserAccount, null, NotificationTypeToCategory.MENTION_ALERTS_GC.getCategoryId());
            } else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
                notification = newNotificationObjectCreaterForNotificationCreatorAccountId(null, null, null, teamEntity, postedByAccountIdUserAccount, null, NotificationTypeToCategory.MENTION_ALERTS_GC.getCategoryId(),headerAccountIds);
            }
            notification.setNotificationTitle("Mentioned in " + entityName + " Group");
            notification.setNotificationBody(byName + " mentioned you in a message.");
            notification.setPayload(gcPayload(gcMessage, notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.MENTION_ALERTS_GC, null, null, NotificationTypeToCategory.MENTION_ALERTS_GC.getCategoryId()));
            notification.setAccountId(postedByAccountIdUserAccount);
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.MENTION_ALERTS_GC));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            return updatingPayloadFormatForGc(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime(), gcMessage);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for add message in entity" + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    public List<HashMap<String, String>> newAccessDomainNotification(Long teamId, AccessDomain accessDomain, String timeZone, String accountIds, Boolean userRoleUpdated) {
        try{
            Notification notification = new Notification();
            notification.setOrgId(organizationRepository.findByOrgId(userAccountRepository.findByAccountId(accessDomain.getAccountId()).getOrgId()));
            notification.setTeamId(teamRepository.findByTeamId(teamId));
            //TODO: change buId and projectId
            notification.setBuId(null);
            notification.setProjectId(null);
            notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(accessDomain.getAccountId(), true));
            notification.setTaskNumber(null);
            notification.setMeetingId(null);
            notification.setLeaveApplicationId(null);
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.NEW_ACCESS_DOMAIN));
            notification.setCategoryId(NotificationTypeToCategory.NEW_ACCESS_DOMAIN.getCategoryId());
            String roleName = roleRepository.findRoleNameByRoleId(accessDomain.getRoleId()).getRoleName();
            if(userRoleUpdated) {
                notification.setNotificationTitle(Constants.NotificationType.updateAccessDomainForAddRole.title(teamRepository.findTeamNameByTeamId(teamId)));
            }
            else {
                notification.setNotificationTitle(Constants.NotificationType.NewAccessDomain.title(teamRepository.findTeamNameByTeamId(teamId)));
            }
            List<String> account = new ArrayList<>(List.of(accountIds.split(", ")));
            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(Long.valueOf(account.get(0))).getFkUserId().getUserId()));
            if(userRoleUpdated) {
                notification.setNotificationBody(Constants.NotificationType.updateAccessDomainForAddRole.body(teamRepository.findTeamNameByTeamId(teamId),organizationRepository.findOrganizationNameByOrgId(userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accessDomain.getAccountId()).getOrgId()),name.getFirstName(),((name.getLastName()==null) ? "":name.getLastName()), roleName));
            }
            else{
                notification.setNotificationBody(Constants.NotificationType.NewAccessDomain.body(teamRepository.findTeamNameByTeamId(teamId),organizationRepository.findOrganizationNameByOrgId(userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accessDomain.getAccountId()).getOrgId()),name.getFirstName(),((name.getLastName()==null) ? "":name.getLastName()), roleName));
            }
            notification.setPayload(createPayloadForAccessDomainNotification(notification, NEW_ACCESS_DOMAIN, NotificationTypeToCategory.NEW_ACCESS_DOMAIN.getCategoryId()));
            Notification notifi = notificationRepository.save(notification);
            //creating notification view
            newNotificationView(notifi,new HashSet<>(List.of(accessDomain.getAccountId())));
            return updatingPayloadFormat(new HashSet<>(List.of(accessDomain.getAccountId())), notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());

        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error in creating notification for new accessDomain entry. Caught Error: " + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    private String createPayloadForAccessDomainNotification(Notification notification,String notificationType, Integer categoryId) {
        Payload payload = new Payload();
        payload.setAccountId(null);
        payload.setNotificationId(String.valueOf(notification.getNotificationId()));
        payload.setNotificationType(notificationType);
        payload.setTitle(notification.getNotificationTitle());
        payload.setBody(notification.getNotificationBody());
        payload.setCategoryId(String.valueOf(categoryId));
        payload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));

        //Convert to String
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String taskPayloadString;
        try {
            taskPayloadString = objectWriter.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Json to string"+e);
            throw new JsonException("Error converting Json to string");
        }

        return taskPayloadString;
    }

    public List<HashMap<String, String>> removeAccessDomainNotification(UserAccount userAccount, Team team, String roleName, String timeZone) {
        try{

            Notification notification = new Notification();
            notification.setOrgId(organizationRepository.findByOrgId(userAccount.getOrgId()));
            notification.setTeamId(teamRepository.findByTeamId(team.getTeamId()));
            notification.setBuId(null);
            notification.setProjectId(null);
            notification.setAccountId(userAccount);
            notification.setTaskNumber(null);
            notification.setMeetingId(null);
            notification.setLeaveApplicationId(null);
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.UPDATED_ACCESS_DOMAIN));
            notification.setCategoryId(NotificationTypeToCategory.UPDATED_ACCESS_DOMAIN.getCategoryId());
            notification.setNotificationTitle(Constants.NotificationType.updateAccessDomain.title(team.getTeamName()));
            notification.setNotificationBody(Constants.NotificationType.updateAccessDomain.body(roleName,team.getTeamName()));
            notification.setPayload(createPayloadForAccessDomainNotification(notification, UPDATED_ACCESS_DOMAIN, NotificationTypeToCategory.UPDATED_ACCESS_DOMAIN.getCategoryId()));
            Notification notifi = notificationRepository.save(notification);
            //creating notification view
            newNotificationView(notifi,new HashSet<>(List.of(userAccount.getAccountId())));
            return updatingPayloadFormat(new HashSet<>(List.of(userAccount.getAccountId())), notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());

        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error in creating notification for new accessDomain entry. Caught Error: " + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    public void requestForNewOrg(NewOrgMemberRequest newOrgMemberRequest, Long orgAdminAccountId, String timeZone) {
        try {
            //Stakeholders list
            User user = userRepository.findByPrimaryEmail(newOrgMemberRequest.getUserName());
            List<Long> accountIdList = userAccountRepository.findAccountIdByFkUserIdUserIdAndIsActive(user, true);
//            Long accountId = accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdIn(Constants.EntityTypes.TEAM,Math.toIntExact(task.getFkTeamId().getTeamId()),accountIdList);
            HashSet<Long> accountIds = new HashSet<>(List.of(orgAdminAccountId));
            //Creating Immediate Attention
            Notification notification=newNotificationObjectCreater(organizationRepository.findByOrgId(newOrgMemberRequest.getOrgId()),null,null,null,userAccountRepository.findByAccountIdAndIsActive(orgAdminAccountId, true),null, null);
            notification.setNotificationTitle("New Organization Request!");
            notification.setNotificationBody("You have received a request to be a part of \""+organizationRepository.findByOrgId(newOrgMemberRequest.getOrgId()).getOrganizationName()+"\" organization.");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.ORG_REQUEST, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, null));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.ORG_REQUEST));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
//            newNotificationView(notification, accountIds);
            //storing payload
            HashMap<String,String> payload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
            taskServiceImpl.sendFcmNotification(user.getUserId(),payload);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to send notification for request of org to user. Caught Exception: "+e,new Throwable(StackTraceHandler.getAllStackTraces(e)));
        }
    }


    /**
     * method to send notification for record voice status in task
     */
    public List<HashMap<String, String>> recordVoiceStatusNotification(Task task, Long accountIdOfUserUpdatingStatus) {
        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdObserver1()!=null) ? task.getFkAccountIdObserver1().getAccountId():-1),
                            ((task.getFkAccountIdObserver2()!=null) ? task.getFkAccountIdObserver2().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                            ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            Integer roleIdToSearch = Constants.HIGHER_ROLE_IDS.get(0);
            if (task.getFkAccountIdAssigned() != null) {
                roleIdToSearch = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(task.getFkAccountIdAssigned().getAccountId(), Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
            }
            accountIds.addAll(new HashSet<>(accessDomainService.getActiveAccountIdsOfHigherRoleMembersInTeam(task.getFkTeamId().getTeamId(),roleIdToSearch)));
            accountIds.remove((long) -1);
            accountIds.remove(accountIdOfUserUpdatingStatus);

            Notification notification = new Notification();
            String notificationType = Constants.NotificationType.UPDATE_TASK_RECORD_VOICE_STATUS;
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(notificationType));
            notification.setCategoryId(NotificationTypeToCategory.UPDATE_TASK_RECORD_VOICE_STATUS.getCategoryId());
            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accountIdOfUserUpdatingStatus)).getFkUserId().getUserId());
            String byName = "Unknown";
            if (name != null) {
                byName = name.getFirstName() + " " + ((name.getLastName() == null) ? "" : name.getLastName());
            }

            notification.setNotificationTitle("Voice status for " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + " updated.");
            notification.setNotificationBody(byName + " has updated the voice status in " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber());

            //for required fields from tasks
            notification.setOrgId(task.getFkOrgId());
            notification.setBuId(task.getBuId());
            notification.setProjectId(task.getFkProjectId());
            notification.setTeamId(task.getFkTeamId());
            notification.setAccountId(task.getFkAccountIdCreator());
            notification.setTaskNumber(task.getTaskNumber());

            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), notificationType, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.TASK_RECORD_VOICE_STATUS, NotificationTypeToCategory.UPDATE_TASK_RECORD_VOICE_STATUS.getCategoryId()));
            Notification newNotification = notificationRepository.save(notification);
            newNotificationView(notification, accountIds);

            return updatingPayloadFormat(accountIds, newNotification.getPayload(), newNotification.getNotificationId(), newNotification.getCreatedDateTime());
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for record voice status in " + setTaskType(task.getTaskTypeId()) + task.getTaskNumber() + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    /**
     *
     * @return payload in the form of hashmap
     * @Function: create notification if a meeting is cancelled
     */
    public List<HashMap<String,String>> cancelMeetingNotification(Meeting meeting, Long adminAccountId, String timeZone){
        Notification notification=new Notification();
        notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.MEETING_UPDATE));
        notification.setCategoryId(NotificationTypeToCategory.MEETING_UPDATE.getCategoryId());
        notification.setOrgId(organizationRepository.findByOrgId(meeting.getOrgId()));
        notification.setBuId(meeting.getBuId());
        notification.setProjectId(projectRepository.findByProjectId(meeting.getProjectId()));
        notification.setTeamId(teamRepository.findByTeamId(meeting.getTeamId()));
        notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(adminAccountId, true));
        notification.setMeetingId(meeting.getMeetingId());
        notification.setTaskNumber(null);
        //for title and body
        notification.setNotificationTitle("Meeting " + meeting.getMeetingNumber() + " is cancelled");
        notification.setNotificationBody("Meeting for " + "'" + meeting.getMeetingKey() + "'" + " is cancelled");

        notification.setPayload(setMeetingPayload(notification,meeting,Constants.NotificationType.MEETING_UPDATE,timeZone, NotificationTypeToCategory.MEETING_UPDATE.getCategoryId()));

        Notification notifi = notificationRepository.save(notification);
        List<Attendee> updatedAttendeeList = meeting.getAttendeeList();
        if (updatedAttendeeList != null && !updatedAttendeeList.isEmpty()) {
            meetingNotificationView(notifi, updatedAttendeeList);
            HashSet<Long> accountIds = new HashSet<>();
            for (Attendee attendee : updatedAttendeeList) {
                accountIds.add(attendee.getAccountId());
            }
            accountIds.remove(adminAccountId);
            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        } else {
            return Collections.emptyList();
        }
    }

//    /**
//     *
//     * @param recurringMeeting
//     * @return payload in the form of hashmap
//     * @Function: create notification if a recurring meeting is cancelled
//     */
//    public List<HashMap<String,String>> cancelRecurringMeetingNotification(RecurringMeeting recurringMeeting, Long adminAccountId, String timeZone){
//        Notification notification = new Notification();
//        notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.MEETING_UPDATE));
//        notification.setOrgId(organizationRepository.findByOrgId(recurringMeeting.getOrgId()));
//        notification.setBuId(recurringMeeting.getBuId());
//        notification.setProjectId(projectRepository.findByProjectId(recurringMeeting.getProjectId()));
//        notification.setTeamId(teamRepository.findByTeamId(recurringMeeting.getTeamId()));
//        notification.setAccountId(userAccountRepository.findByAccountIdAndIsActive(adminAccountId, true));
////        notification.setMeetingId(recurringMeeting.getRecurringMeetingId()); -- Discuss if I should add another field in the notification table
//        notification.setTaskNumber(null);
//        //for title and body
//        notification.setNotificationTitle("Recurring meeting " + recurringMeeting.getRecurringMeetingNumber() + " is cancelled");
//        notification.setNotificationBody("Meeting for " + "'" + recurringMeeting.getMeetingKey() + "'" + " is cancelled");
//
//        notification.setPayload(setRecurringMeetingPayload(notification,recurringMeeting,Constants.NotificationType.MEETING_UPDATE,timeZone));
//
//        Notification notifi = notificationRepository.save(notification);
//        List<Attendee> updatedAttendeeList = recurringMeeting.get
//        if (updatedAttendeeList != null && !updatedAttendeeList.isEmpty()) {
//            meetingNotificationView(notifi, updatedAttendeeList);
//            HashSet<Long> accountIds = new HashSet<>();
//            for (Attendee attendee : updatedAttendeeList) {
//                accountIds.add(attendee.getAccountId());
//            }
//            accountIds.remove(adminAccountId);
//            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
//        } else {
//            return Collections.emptyList();
//        }
//    }

    /** Sends a notification to inform a frontend to remove user. From this notification, frontend will be informed to logout the user at their end*/
    public void sendRemoveUserNotification(RemoveOrgMemberRequest removeOrgMemberRequest, String timeZone) {
        UserAccount userAccount = userAccountRepository.findByAccountId(removeOrgMemberRequest.getAccountId());
        Payload payload = new Payload();
        payload.setAccountId(removeOrgMemberRequest.getAccountId().toString());
        payload.setNotificationId("-1"); // it's not necessary to set, but we're setting to -1 so the frontend is able to handle it properly
        payload.setNotificationType(USER_REMOVE_UPDATE);
        payload.setCategoryId(String.valueOf(NotificationTypeToCategory.getCategoryIdByNotificationType(USER_REMOVE_UPDATE)));
        payload.setTitle("Account with email " + userAccount.getFkUserId().getPrimaryEmail() + " was removed from the org");
        payload.setBody("Access Domain updated for user: " + userAccount.getFkUserId().getPrimaryEmail());
        DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(LocalDateTime.now(),timeZone);
        HashMap<String, String> payloadMap = objectMapper.convertValue(payload, new TypeReference<HashMap<String, String>>() {});
        payloadMap.entrySet().removeIf(entry -> (entry.getValue() == null || entry.getValue().equals("null")));
        try {
            taskServiceImpl.sendFcmNotification(userAccount.getFkUserId().getUserId(), payloadMap);
        } catch (Exception e){
            logger.error("Error in sending FCM notification for method " + " sendRemoveUserNotification " + " for user: " + userAccount.getFkUserId().getPrimaryEmail());
        }
    }
    /** Sends a notification to inform a frontend of updated user preferences. From this notification, frontend will be informed to refresh the user preference at their end*/
    public void sendUpdateUserPreferenceNotification(UserPreference userPreference, Long userId, String timeZone) {
        User user = userRepository.findByUserId(userId);
        Payload payload = new Payload();
        payload.setAccountId("-1"); // it's not necessary to set, but we're setting to -1 so the frontend is able to handle it properly
        payload.setNotificationId("-1"); // it's not necessary to set, but we're setting to -1 so the frontend is able to handle it properly
        payload.setNotificationType(USER_PREFERENCE_UPDATE);
        payload.setCategoryId(String.valueOf(NotificationTypeToCategory.getCategoryIdByNotificationType(USER_PREFERENCE_UPDATE)));
        payload.setTitle("Entity details updated in userPreference");
        payload.setBody("Entity details updated in userPreference for user: " + user.getPrimaryEmail());
        DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(LocalDateTime.now(),timeZone);
        HashMap<String, String> payloadMap = objectMapper.convertValue(payload, new TypeReference<HashMap<String, String>>() {});
        payloadMap.entrySet().removeIf(entry -> (entry.getValue() == null || entry.getValue().equals("null")));
        try {
            taskServiceImpl.sendFcmNotification(userId, payloadMap);
        } catch (Exception e){
            logger.error("Error in sending FCM notification for method " + " sendUpdateUserPreferenceNotification " + " for user: " + user.getPrimaryEmail());
        }
    }

    /**
     *
     * @return list of payload in the form of hashmap
     * @Function: Create a notification and notification view when a task template is updated.
     */
    public List<HashMap<String, String>> updateTaskTemplateNotification(TaskTemplate taskTemplate, TaskTemplate taskTemplateDb, String accountIdString, String timeZone) {
        try {
            List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIdString);
            List<HashMap<String, String>> payloadList = new ArrayList<HashMap<String, String>>();
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>();
            if (Objects.equals(Constants.EntityTypes.TEAM, taskTemplate.getEntityTypeId())) {
                accountIds.addAll(new HashSet<>(accessDomainRepository.findDistinctAccountIdsByEntityTypeAndEntityTypeIdAndIsActive(taskTemplate.getEntityId(), Constants.EntityTypes.TEAM)));
            } else if (Objects.equals(Constants.EntityTypes.PROJECT, taskTemplate.getEntityTypeId())) {
                accountIds.addAll(new HashSet<>(projectService.getprojectMembersAccountIdList(List.of(taskTemplate.getEntityId())).stream().map(AccountId::getAccountId).collect(Collectors.toList())));
            } else if (Objects.equals(Constants.EntityTypes.ORG, taskTemplate.getEntityTypeId())) {
                accountIds.addAll(new HashSet<>(userAccountRepository.findAccountIdByOrgIdAndIsActive(taskTemplate.getEntityId(), true).stream().map(AccountId::getAccountId).collect(Collectors.toList())));
            }
            accountIds.removeAll(new HashSet<>(headerAccountIdsList));

            Long userId = userAccountRepository.findUserIdByAccountId(headerAccountIdsList.get(0));

            UserName name = userRepository.findFirstNameAndLastNameByUserId(userId);
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            //Checking the changes made
            List<HashMap<String, String>> newPayload = null;
            List<String> updatedFeilds = taskTemplateService.getUpdatedFields(taskTemplate, taskTemplateDb);
            Notification notification = newNotificationObjectCreater(taskTemplate.getFkOrgId(), null, taskTemplate.getFkProjectId(), taskTemplate.getFkTeamId(), taskTemplate.getFkAccountIdCreator(), null, NotificationTypeToCategory.TEMPLATE_UPDATE.getCategoryId());
            notification.setNotificationTitle("There was a change in template " + taskTemplate.getTemplateNumber());
            notification.setNotificationBody(byName + " has updated \"" + updatedFeilds + "\" in template with template title \"" + taskTemplateDb.getTemplateTitle() + "\".");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), TEMPLATE_UPDATE, taskTemplate.getTemplateNumber().toString(), taskTemplate.getTemplateId(), taskTemplate.getFkTeamId().getTeamId(), Constants.ScrollToType.TEMPLATE, NotificationTypeToCategory.TEMPLATE_UPDATE.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(TEMPLATE_UPDATE));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
            payloadList.addAll(newPayload);
            return payloadList;
        } catch (Exception e) {
            logger.error("Unable to create notification for updateTaskTemplateNotification " +e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * This method creates a payload for making notifications for user reminders
     */
    public List<HashMap<String, String>> createPayloadForUserReminder(List<Reminder> reminders) {
        List<HashMap<String, String>> remindersList = new ArrayList<HashMap<String, String>>();
        try {
            for (Reminder reminder : reminders) {
                // get attendees from each meeting
                Long reminderId = reminder.getReminderId();
                HashSet<Long> accountIdList = new HashSet<>();
                accountIdList.add(reminder.getFkAccountIdCreator().getAccountId());
                //set payload for reminder notification
                remindersList.addAll(updatePayloadFormatForUserReminder(accountIdList, reminder, Constants.NotificationType.USER_REMINDER,
                        Constants.UserReminder.title(reminder.getReminderTitle()),
                        Constants.UserReminder.body(reminder.getDescription() != null ? reminder.getDescription() : "")));
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification not created in createPayloadForTaskReminder. Caught Exception: " + e, new Throwable(allStackTraces));
        }
        //return the payload
        return remindersList;
    }

    private List<HashMap<String, String>> updatePayloadFormatForUserReminder(HashSet<Long> accountIdList, Reminder reminder, String notificationType, String title, String body) {
        Payload load = new Payload();
        LocalDateTime createdDateTime = reminder.getCreatedDateTime();
        load.setNotificationType(notificationType);
        load.setTitle(title);
        load.setBody(body);
        load.setTaskNumber(String.valueOf(reminder.getReminderId()));
        load.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));

        NotificationType notificationTypeId = notificationTypeRepository.findByNotificationType(notificationType);
        List<HashMap<String, String>> listOfPayload = new ArrayList<HashMap<String, String>>();
        //Create notification for meeting reminder
        load.setAccountId(reminder.getFkAccountIdCreator().getAccountId().toString());
        load.setCategoryId(notificationTypeId.getNotificationCategoryId().toString());
        Notification notifi = createUserReminderNotification(notificationTypeId, reminder, load);
        //creating new notificationView for notification for each accountId
        for (Long accountId : accountIdList) {
            schedulingService.newNotificationView(notifi, accountId);
            load.setAccountId(String.valueOf(accountId));
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
            if (userAccount != null && createdDateTime != null) {
                ZonedDateTime systemZonedDateTime = createdDateTime.atZone(ZoneId.systemDefault());
                ZoneId userTimeZone = ZoneId.of(userAccount.getFkUserId().getTimeZone());
//                ZonedDateTime zonedDateTime = createdDateTime.atZone(userTimeZone);
                ZonedDateTime zonedDateTime = systemZonedDateTime.withZoneSameInstant(userTimeZone);
                LocalDateTime localDateTime = LocalDateTime.from(zonedDateTime);
                load.setCreatedDateTime(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            load.setNotificationId(String.valueOf(notifi.getNotificationId()));
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> entry.getValue() == null);
            listOfPayload.add(loadMap);
        }
        return listOfPayload;
    }

    private Notification createUserReminderNotification(NotificationType notificationTypeId, Reminder reminder, Payload load) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeId);
        newNotification.setAccountId(reminder.getFkAccountIdCreator());
        newNotification.setNotificationTitle(convertTypeToString(load.getTitle()));
        newNotification.setNotificationBody(load.getBody());
        newNotification.setPayload(gson.toJson(load));
        newNotification.setCategoryId(NotificationTypeToCategory.USER_REMINDER.getCategoryId());
        return notificationRepository.save(newNotification);
    }

    /** Sends a notification to inform a frontend to logout user. From this notification, frontend will be informed to logout the user at their end*/
    public void sendLogoutNotification(RemoveOrgMemberRequest removeOrgMemberRequest, String timeZone) {
        UserAccount userAccount = userAccountRepository.findByAccountId(removeOrgMemberRequest.getAccountId());
        Payload payload = new Payload();
        payload.setAccountId(removeOrgMemberRequest.getAccountId().toString());
        payload.setNotificationId("-1"); // it's not necessary to set, but we're setting to -1 so the frontend is able to handle it properly
        payload.setNotificationType(USER_REMOVE_UPDATE);
        payload.setCategoryId(String.valueOf(NotificationTypeToCategory.getCategoryIdByNotificationType(USER_REMOVE_UPDATE)));
        payload.setTitle("Account with email " + userAccount.getFkUserId().getPrimaryEmail() + " was logged out.");
        payload.setBody("Account access updated for user: " + userAccount.getFkUserId().getPrimaryEmail());
        DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(LocalDateTime.now(),timeZone);
        HashMap<String, String> payloadMap = objectMapper.convertValue(payload, new TypeReference<HashMap<String, String>>() {});
        payloadMap.entrySet().removeIf(entry -> (entry.getValue() == null || entry.getValue().equals("null")));
        try {
            taskServiceImpl.sendFcmNotification(userAccount.getFkUserId().getUserId(), payloadMap);
        } catch (Exception e){
            logger.error("Error in sending FCM notification for method " + " sendLogoutNotification " + " for user: " + userAccount.getFkUserId().getPrimaryEmail());
        }
    }

    /**
     * @Function: Create notification when a new comment is added to a task
     */
    public List<HashMap<String, String>> createNotificationForStatusInquiryInTask(Task task, Comment comment, String timeZone) {

        try {
            //Stakeholders list
            HashSet<Long> accountIds = new HashSet<>(
                    List.of(((task.getFkAccountIdAssigned()!=null) ? task.getFkAccountIdAssigned().getAccountId():-1),
                            ((task.getFkAccountIdMentor1()!=null) ? task.getFkAccountIdMentor1().getAccountId():-1),
                            ((task.getFkAccountIdMentor2()!=null) ? task.getFkAccountIdMentor2().getAccountId():-1)));
            accountIds.remove((long) -1);
            accountIds.remove(comment.getPostedByAccountId());

            UserName name = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(comment.getPostedByAccountId())).getFkUserId().getUserId());
            String byName = name.getFirstName() + " " + ((name.getLastName()==null) ? "":name.getLastName());
            //Checking the changes made
            Notification notification = newNotificationObjectCreater(task.getFkOrgId(), task.getBuId(), task.getFkProjectId(), task.getFkTeamId(), task.getFkAccountId(), task.getTaskNumber(), NotificationTypeToCategory.STATUS_INQUIRY.getCategoryId());
            notification.setNotificationTitle(comment.getComment());
            notification.setNotificationBody(byName + " has added a new query.");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.STATUS_INQUIRY, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.STATUS_INQUIRY, NotificationTypeToCategory.STATUS_INQUIRY.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.STATUS_INQUIRY));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            return updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for status query " + e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    public List<HashMap<String, String>> createPayloadForLeaveApprovalReminder(LeaveApplication leaveApplication, UserAccount userAccount) {
        List<HashMap<String, String>> listOfPayload = new ArrayList<HashMap<String, String>>();
        try {
            Payload load = new Payload();
            Long accountId = userAccount.getAccountId();
            LocalDateTime createdDateTime = leaveApplication.getCreatedDateTime();
            load.setNotificationType(LEAVE_APPROVAL_REMINDER);
            load.setTitle(Constants.LEAVE_APPROVAL_REMINDER_TITLE);
            load.setBody("Reason for leave is " + leaveApplication.getLeaveReason());
            load.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));

            NotificationType notificationTypeId = notificationTypeRepository.findByNotificationType(LEAVE_APPROVAL_REMINDER);

            load.setAccountId(null);
            Notification newNotification = new Notification();
            newNotification.setNotificationTypeID(notificationTypeId);
            newNotification.setAccountId(userAccount);
            newNotification.setNotificationTitle(convertTypeToString(load.getTitle()));
            newNotification.setNotificationBody(load.getBody());
            newNotification.setPayload(gson.toJson(load));
            Notification notifi = notificationRepository.save(newNotification);

            schedulingService.newNotificationView(notifi, accountId);
            load.setAccountId(String.valueOf(accountId));
            if (createdDateTime != null) {
                ZonedDateTime systemZonedDateTime = createdDateTime.atZone(ZoneId.systemDefault());
                ZoneId userTimeZone = ZoneId.of(userAccount.getFkUserId().getTimeZone());
                ZonedDateTime zonedDateTime = systemZonedDateTime.withZoneSameInstant(userTimeZone);
                LocalDateTime localDateTime = LocalDateTime.from(zonedDateTime);
                load.setCreatedDateTime(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            load.setNotificationId(String.valueOf(notifi.getNotificationId()));
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> entry.getValue() == null);
            listOfPayload.add(loadMap);

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification not created in createPayloadForLeaveApprovalReminder. Caught Exception: " + e, new Throwable(allStackTraces));
        }
        //return the payload
        return listOfPayload;
    }

    public void updateSprintNotification(Sprint sprint, String timeZone, String accountIds, Integer sprintStatusId) {
        try {
            List<HashMap<String, String>> payloadList = new ArrayList<HashMap<String, String>>();
            //Stakeholders list
            Optional<Team> team = Optional.empty();
            Optional<Project> project = Optional.empty();
            Set<EmailFirstLastAccountId> sprintMemberList = sprint.getSprintMembers();
            if (sprintMemberList == null) {
                sprintMemberList = new HashSet<>();
            }
            HashSet<Long> userAccountIds = new HashSet<>(sprintMemberList.stream().map(EmailFirstLastAccountId::getAccountId).collect(Collectors.toList()));
            userAccountIds = new HashSet<>(userAccountRepository.findAllAccountIdsByAccountIdInAndIsActive (new ArrayList<>(userAccountIds), true));
//            Team team = teamRepository.findByTeamId(sprint.getSprintId());
            if(sprint.getEntityTypeId() == Constants.EntityTypes.TEAM) {
                team = Optional.ofNullable(teamRepository.findByTeamId(sprint.getEntityId()));
                List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
                UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, team.get().getFkOrgId().getOrgId(), true);
                Notification notification = newNotificationObjectCreater(team.get().getFkOrgId(),team.get().getFkProjectId().getBuId(),team.get().getFkProjectId(),team.get(),null,null, NotificationTypeToCategory.OTHERS.getCategoryId());
                notification.setNotificationTitle("Sprint " + sprint.getSprintTitle());
                if(Objects.equals(sprintStatusId, Constants.SprintStatusEnum.STARTED.getSprintStatusId()))
                    notification.setNotificationBody("Sprint " + sprint.getSprintTitle() + " has started.");
                else if(Objects.equals(sprintStatusId, Constants.SprintStatusEnum.COMPLETED.getSprintStatusId()))
                    notification.setNotificationBody("Sprint " + sprint.getSprintTitle() + " has completed.");
                else
                    notification.setNotificationBody("Sprint " + sprint.getSprintTitle() + " has deleted.");
                notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), OTHERS, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.OTHERS.getCategoryId()));
                notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(OTHERS));
                notification.setAccountId(userAccount);
                Notification notifi = notificationRepository.save(notification);
                newNotificationView(notification,userAccountIds);
                HashMap<String,String> payload = updatingPayloadFormat(userAccountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
                taskServiceImpl.sendFcmNotification(null,payload);
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for updateTaskNotification " +e.getMessage());
        }
    }

    public void updateSprintNotification1(Alert alert,String timeZone) {
        try {
            //Stakeholders list
            User user = alert.getFkAccountIdReceiver().getFkUserId();
            HashSet<Long> accountIds = new HashSet<>(List.of(alert.getFkAccountIdReceiver().getAccountId()));
            //Creating Immediate Attention
            Notification notification = newNotificationObjectCreater(alert.getFkOrgId(), null, alert.getFkProjectId(),alert.getFkTeamId(),alert.getFkAccountIdSender(),alert.getAssociatedTaskNumber(), NotificationTypeToCategory.IMMEDIATE_ATTENTION.getCategoryId());
            notification.setNotificationTitle(alert.getAlertTitle());
            notification.setNotificationBody(alert.getAlertReason());
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), Constants.NotificationType.IMMEDIATE_ATTENTION, alert.getAssociatedTaskNumber(), alert.getAssociatedTaskId(), alert.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.IMMEDIATE_ATTENTION.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.IMMEDIATE_ATTENTION));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            //storing payload
            HashMap<String,String> payload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
            taskServiceImpl.sendFcmNotification(user.getUserId(),payload);

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for immediateAttentionNotification " + e,new Throwable(allStackTraces));
        }
    }

    public String setTaskType(Integer taskTypeId) {
        String taskType="Task ";
        if(taskTypeId==Constants.TaskTypes.PARENT_TASK)
            taskType="Parent Task ";
        if(taskTypeId==Constants.TaskTypes.CHILD_TASK)
            taskType="Child Task ";
        if(taskTypeId==Constants.TaskTypes.BUG_TASK)
            taskType="Bug ";
        return taskType;
    }

    public void userPreferenceChangeNotificationForDeleteTeam(Team team, Long userId, String teamName, UserAccount modifyingAccount, List<Long> accountIdList, String timeZone) {
        try {
            HashSet<Long> accountIds = new HashSet<>(accountIdList);
            //Creating Immediate Attention
            Notification notification = newNotificationObjectCreater(team.getFkOrgId(), null, team.getFkProjectId(), null, modifyingAccount, null, NotificationTypeToCategory.USER_PREFERENCE_UPDATE.getCategoryId());
            notification.setNotificationTitle("Team preference updated!");
            if (teamName != null) {
                notification.setNotificationBody("You preferred team has been updated to '" + teamName + "' as the previous team was deleted.");
            } else {
                notification.setNotificationBody("You preferred team has been removed because the selected team is no longer available.");
            }
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), USER_PREFERENCE_UPDATE, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.USER_PREFERENCE_UPDATE.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(USER_PREFERENCE_UPDATE));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            //storing payload
            HashMap<String,String> payload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
            taskServiceImpl.sendFcmNotification(userId,payload);

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for user preference update " + e,new Throwable(allStackTraces));
        }
    }

    public void userPreferenceChangeNotificationForDeleteProject(Project project, Long userId, String projectName, UserAccount modifyingAccount, List<Long> accountIdList, String timeZone) {
        try {
            HashSet<Long> accountIds = new HashSet<>(accountIdList);
            //Creating Immediate Attention
            Organization organization = organizationRepository.findByOrgId(project.getOrgId());
            Notification notification = newNotificationObjectCreater(organization, project.getBuId(), null, null, modifyingAccount, null, NotificationTypeToCategory.USER_PREFERENCE_UPDATE.getCategoryId());
            notification.setNotificationTitle("Project preference updated!");
            if (projectName != null) {
                notification.setNotificationBody("You preferred project has been updated to '" + projectName + "' as the previous project was deleted.");
            } else {
                notification.setNotificationBody("You preferred project has been removed because the selected project is no longer available.");
            }
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), USER_PREFERENCE_UPDATE, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.USER_PREFERENCE_UPDATE.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(USER_PREFERENCE_UPDATE));
            Notification notifi = notificationRepository.save(notification);
            //Notification view
            newNotificationView(notification, accountIds);
            //storing payload
            HashMap<String,String> payload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime()).get(0);
            taskServiceImpl.sendFcmNotification(userId,payload);

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to create notification for user preference update " + e,new Throwable(allStackTraces));
        }
    }

    public List<HashMap<String, String>> sendNotificationForEpicStarted(Epic epic, List<Long> accountIdList, String timeZone) {
        try {
            HashSet<Long> accountIds = new HashSet<>(accountIdList);
            Notification notification = newNotificationObjectCreater(epic.getFkOrgId(), null, epic.getFkProjectId(), null, epic.getFkEpicOwner(), null, NotificationTypeToCategory.EPIC_STARTED.getCategoryId());
            notification.setNotificationTitle("Epic is started");
            notification.setNotificationBody("Workflow status of Epic " + epic.getEpicNumber() + " is now In-Progress");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), EPIC_STARTED, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.EPIC_STARTED.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(EPIC_STARTED));
            Notification notifi = notificationRepository.save(notification);
            newNotificationView(notification, accountIds);
            List<HashMap<String, String>> newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());

            return newPayload;
        } catch (Exception e) {
            logger.error("Unable to create notification for In-Progress epic " +e.getMessage());
            return Collections.emptyList();
        }
    }


    public String convertTypeToString (String notificationType) {
        if (notificationType.contains("_")) {
            // Split the constant by underscores
            String[] words = notificationType.split("_");
            StringBuilder titleBuilder = new StringBuilder();

            // Capitalize the first letter of each word and append to the title
            for (String word : words) {
                if (!word.isEmpty()) {
                    titleBuilder.append(word.substring(0, 1).toUpperCase())
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
                }
            }

            // Remove the trailing space and return the result
            return titleBuilder.toString().trim();
        } else {
            return notificationType;
        }
    }

    public List<HashMap<String, String>> sendNotificationForChangedUserName(
            User user, Long accountId, Long requesterAccountId, String timeZone) {
        try {
            List<HashMap<String, String>> allPayloads = new ArrayList<>();
            EmailFirstLastAccountId requesterUserAccount =
                    userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(requesterAccountId);
            UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
            List<UserAccount> userAccountList =
                    userAccountRepository.findByFkUserIdUserIdAndIsActive(user.getUserId(), true);
            HashSet<Long> ownerAccountIds = new HashSet<>();
            for (UserAccount ua : userAccountList) {
                if (ua.getOrgId() != null) {
                    Organization org = organizationRepository.findByOrgId(ua.getOrgId());
                    if (org != null && org.getOwnerEmail() != null) {
                        UserAccount ownerAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(org.getOwnerEmail(), org.getOrgId(), true);
                        if (ownerAccount.getAccountId() != requesterAccountId) {
                            ownerAccountIds.add(ownerAccount.getAccountId());
                        }
                    }
                }
            }
            HashSet<Long> accountIds = new HashSet<>(Collections.singleton(accountId));
            if (!accountIds.isEmpty()) {
                Notification notificationForUser = newNotificationObjectCreater(
                        null, null, null, null, userAccount, null,
                        NotificationTypeToCategory.UPDATED_USER_NAME.getCategoryId()
                );
                notificationForUser.setNotificationTitle("User Name Updated");
                notificationForUser.setNotificationBody("Your username updated to " + user.getFirstName() + " "
                        + user.getLastName() + ". Updated by " + requesterUserAccount.getFirstName() + " "
                        + requesterUserAccount.getLastName() + ".");
                notificationForUser.setPayload(TaskPayload(
                        notificationForUser.getNotificationTitle(),
                        notificationForUser.getNotificationBody(),
                        notificationForUser.getNotificationId(),
                        UPDATED_USER_NAME, null, null, null,
                        Constants.ScrollToType.SCROLL_NOT_REQUIRED,
                        NotificationTypeToCategory.UPDATED_USER_NAME.getCategoryId()
                ));
                notificationForUser.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATED_USER_NAME));
                //System.out.println(notificationForUser.getNotificationTypeID());
                Notification saved = notificationRepository.save(notificationForUser);
                newNotificationView(notificationForUser, accountIds);
                allPayloads.addAll(updatingPayloadFormat(
                        accountIds, saved.getPayload(), saved.getNotificationId(),
                        saved.getCreatedDateTime()
                ));
            }
            Notification notificationForRequester = newNotificationObjectCreater(
                    null, null, null, null, userAccount, null,
                    NotificationTypeToCategory.UPDATED_USER_NAME.getCategoryId()
            );
            notificationForRequester.setNotificationTitle("User Name Updated Successfully");
            notificationForRequester.setNotificationBody("You updated " + user.getFirstName() + " "
                    + user.getLastName() + "'s username successfully.");
            notificationForRequester.setPayload(TaskPayload(
                    notificationForRequester.getNotificationTitle(),
                    notificationForRequester.getNotificationBody(),
                    notificationForRequester.getNotificationId(),
                    UPDATED_USER_NAME, null, null, null,
                    Constants.ScrollToType.SCROLL_NOT_REQUIRED,
                    NotificationTypeToCategory.UPDATED_USER_NAME.getCategoryId()
            ));
            notificationForRequester.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATED_USER_NAME));
            Notification requesterNotifi = notificationRepository.save(notificationForRequester);
            newNotificationView(notificationForRequester, new HashSet<>(Collections.singleton(requesterAccountId)));
            allPayloads.addAll(updatingPayloadFormat(
                    new HashSet<>(Collections.singleton(requesterAccountId)),
                    requesterNotifi.getPayload(),
                    requesterNotifi.getNotificationId(),
                    requesterNotifi.getCreatedDateTime()
            ));
            if (!ownerAccountIds.isEmpty()) {
                Notification notificationForOwners = newNotificationObjectCreater(
                        null, null, null, null, userAccount, null,
                        NotificationTypeToCategory.UPDATED_USER_NAME.getCategoryId()
                );
                notificationForOwners.setNotificationTitle("User Name Change in Organization");
                notificationForOwners.setNotificationBody("User " + user.getFirstName() + " " + user.getLastName()
                        + "s username was Updated");
                notificationForOwners.setPayload(TaskPayload(
                        notificationForOwners.getNotificationTitle(),
                        notificationForOwners.getNotificationBody(),
                        notificationForOwners.getNotificationId(),
                        UPDATED_USER_NAME, null, null, null,
                        Constants.ScrollToType.SCROLL_NOT_REQUIRED,
                        NotificationTypeToCategory.UPDATED_USER_NAME.getCategoryId()
                ));
                notificationForOwners.setNotificationTypeID(notificationTypeRepository.findByNotificationType(UPDATED_USER_NAME));
                System.out.println(notificationForOwners.getNotificationTypeID());
                Notification ownersNotifi = notificationRepository.save(notificationForOwners);
                newNotificationView(notificationForOwners, ownerAccountIds);
                allPayloads.addAll(updatingPayloadFormat(
                        ownerAccountIds,
                        ownersNotifi.getPayload(),
                        ownersNotifi.getNotificationId(),
                        ownersNotifi.getCreatedDateTime()
                ));
            }
            return allPayloads;
        } catch (Exception e) {
            logger.error("Unable to create notification for change user name ", e);
            return Collections.emptyList();
        }
    }

    public List<HashMap<String, String>> sendNotificationForJiraImport(Team team, String notificationBody, Long accountIdOfUser, String timeZone) {
        try {
            HashSet<Long> accountIds = new HashSet<>();
            accountIds.add(accountIdOfUser);
            UserAccount userAccount = userAccountRepository.findByAccountId(accountIdOfUser);
            Notification notification = newNotificationObjectCreater(team.getFkOrgId(), null, team.getFkProjectId(), team, userAccount, null, NotificationTypeToCategory.IMPORT_JIRA.getCategoryId());
            notification.setNotificationTitle("Jira Import Complete");
            notification.setNotificationBody(notificationBody);
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), IMPORT_JIRA, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.IMPORT_JIRA.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(IMPORT_JIRA));
            Notification notifi = notificationRepository.save(notification);
            newNotificationView(notification, accountIds);
            List<HashMap<String, String>> newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());

            return newPayload;
        } catch (Exception e) {
            logger.error("Unable to create notification for import jira " +e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<HashMap<String, String>> sendNotificationForDeclineInvite (Organization organization, UserAccount userAccount, UserAccount userAccountIdOfOrgAdmin, String timeZone) {
        try {
            HashSet<Long> accountIds = new HashSet<>();
            accountIds.add(userAccountIdOfOrgAdmin.getAccountId());
            Notification notification = newNotificationObjectCreater(organization, null, null, null, userAccountIdOfOrgAdmin, null, NotificationTypeToCategory.JIRA_INVITE_DECLINE.getCategoryId());
            notification.setNotificationTitle("Decline invite of jira user import");
            notification.setNotificationBody("User " + userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName() + " declined the invite of import of jira user");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), JIRA_INVITE_DECLINE, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.JIRA_INVITE_DECLINE.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(IMPORT_JIRA));
            Notification notifi = notificationRepository.save(notification);
            newNotificationView(notification, accountIds);
            List<HashMap<String, String>> newPayload = updatingPayloadFormat(accountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());

            return newPayload;
        } catch (Exception e) {
            logger.error("Unable to create notification for declining import of jira user " +e.getMessage());
            return Collections.emptyList();
        }
    }

    public void submitMeetingEffortNotification(Task task, HashSet<Long> recipientAccountIds, String timeZone, String accountIds, Integer hours) {
        try {
            List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
            UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, task.getFkOrgId().getOrgId(), true);
            Notification notification = newNotificationObjectCreater(task.getFkOrgId(), null, null, task.getFkTeamId(), userAccount, task.getTaskNumber(), NotificationTypeToCategory.MEETING_FOLLOW_UP.getCategoryId());
            notification.setNotificationTitle("Tagged Meeting of Task# " + task.getTaskNumber());
            notification.setNotificationBody("Please add your efforts to the Tagged Meetings linked to Task# " + task.getTaskNumber() + " within " + hours + " hours.");
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), OTHERS, task.getTaskNumber(), task.getTaskId(), task.getFkTeamId().getTeamId(), Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.MEETING_FOLLOW_UP.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(MEETING_FOLLOW_UP));
            notification.setAccountId(userAccount);
            Notification notifi = notificationRepository.save(notification);
            newNotificationView(notification, recipientAccountIds);
            List<HashMap<String, String>> payload = updatingPayloadFormat(recipientAccountIds, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
            fcmNotificationUtil.sendPushNotification(payload);
        } catch (Exception e) {
            logger.error("Unable to create notification for submitMeetingNotification for task#" + task.getTaskNumber() + " " + e.getMessage());
        }
    }

    public List<HashMap<String, String>> sendNotificationForMeetingAnalysis(Meeting meeting, String notificationTitle, String notificationBody, HashSet<Long> accountIdListToSendNotification, Long accountIdOfUser, String timeZone) {
        try {
            UserAccount userAccount = userAccountRepository.findByAccountId(accountIdOfUser);
            Organization organization = null;
            Project project = null;
            Team team = null;
            if (meeting.getOrgId() != null) {
                organization = organizationRepository.findByOrgId(meeting.getOrgId());
            }
            if (meeting.getProjectId() != null) {
                project = projectRepository.findByProjectId(meeting.getProjectId());
            }
            if (meeting.getTeamId() != null) {
                team = teamRepository.findByTeamId(meeting.getTeamId());
            }
            Notification notification = newNotificationObjectCreater(organization, null, project, team, userAccount, null, NotificationTypeToCategory.MEETING_ANALYSIS.getCategoryId());
            notification.setNotificationTitle(notificationTitle);
            notification.setMeetingId(meeting.getMeetingId());
            notification.setNotificationBody(notificationBody);
            notification.setPayload(TaskPayload(notification.getNotificationTitle(), notification.getNotificationBody(), notification.getNotificationId(), MEETING_ANALYSIS, null, null, null, Constants.ScrollToType.SCROLL_NOT_REQUIRED, NotificationTypeToCategory.MEETING_ANALYSIS.getCategoryId()));
            notification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(MEETING_ANALYSIS));
            Notification notifi = notificationRepository.save(notification);
            newNotificationView(notification, accountIdListToSendNotification);
            List<HashMap<String, String>> newPayload = updatingPayloadFormat(accountIdListToSendNotification, notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());
            return newPayload;
        } catch (Exception e) {
            logger.error("Unable to create notification for meeting analysis " +e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<HashMap<String, String>> sendNotificationForStarringUnstarring(
            StarredWorkItemRequest starredWorkItemRequest, String timeZone, User user) {
        try {
            Task taskDb = taskRepository.findByTaskId(starredWorkItemRequest.getTaskId());
            // current user account (who marked flagged/unflagged)
            UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(
                    user.getPrimaryEmail(),
                    taskDb.getFkOrgId().getOrgId(),
                    true
            );
            HashSet<Long> accountIdListToSendNotification = new HashSet<>();
            if (Boolean.TRUE.equals(starredWorkItemRequest.getIsStarred())) {
                if (taskDb.getFkAccountIdAssigned() != null &&
                        !taskDb.getFkAccountIdAssigned().getAccountId().equals(userAccount.getAccountId())) {
                    accountIdListToSendNotification.add(taskDb.getFkAccountIdAssigned().getAccountId());
                }
            }
            if (Boolean.FALSE.equals(starredWorkItemRequest.getIsStarred())) {
                if (taskDb.getFkAccountIdAssigned() != null &&
                        !taskDb.getFkAccountIdAssigned().getAccountId().equals(userAccount.getAccountId())) {
                    accountIdListToSendNotification.add(taskDb.getFkAccountIdAssigned().getAccountId());
                }
                if (taskDb.getFkAccountIdStarredBy() != null &&
                        !taskDb.getFkAccountIdStarredBy().getAccountId().equals(userAccount.getAccountId())) {
                    accountIdListToSendNotification.add(taskDb.getFkAccountIdStarredBy().getAccountId());
                }
            }
            String notificationTitle;
            String notificationBody;
            if (Boolean.TRUE.equals(starredWorkItemRequest.getIsStarred())) {
                notificationTitle = "Work Item Marked as Flagged";
                notificationBody = "Work Item " + taskDb.getTaskNumber() +
                        " has been Flagged by " + user.getFirstName() + " " + user.getLastName();
            } else {
                notificationTitle = "Work Item Marked as Unflagged";
                notificationBody = "Work Item " + taskDb.getTaskNumber() +
                        " has been Unflagged by " + user.getFirstName() + " " + user.getLastName();
            }
            Notification notification = newNotificationObjectCreater(
                    null, null, null, null, null, null,
                    NotificationTypeToCategory.FLAGGED_UNFLAGGED_NOTIFICATION.getCategoryId()
            );
            notification.setNotificationTitle(notificationTitle);
            notification.setNotificationBody(notificationBody);
            notification.setPayload(TaskPayload(
                    notification.getNotificationTitle(),
                    notification.getNotificationBody(),
                    notification.getNotificationId(),
                    NotificationTypeToCategory.FLAGGED_UNFLAGGED_NOTIFICATION.getNotificationType(),
                    null, null, null,
                    Constants.ScrollToType.SCROLL_NOT_REQUIRED,
                    NotificationTypeToCategory.FLAGGED_UNFLAGGED_NOTIFICATION.getCategoryId()
            ));
            notification.setNotificationTypeID(
                    notificationTypeRepository.findByNotificationType("FLAGGED_UNFLAGGED_NOTIFICATION")
            );
            Notification savedNotification = notificationRepository.save(notification);
            newNotificationView(savedNotification, accountIdListToSendNotification);
            return updatingPayloadFormat(
                    accountIdListToSendNotification,
                    savedNotification.getPayload(),
                    savedNotification.getNotificationId(),
                    savedNotification.getCreatedDateTime()
            );
        } catch (Exception e) {
            logger.error("Unable to create notification for Flagging/Unflagging of Work Item " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Set<TaskUpdationDetailsResponseDto> getAllTaskUpdationDetails(
            TaskUpdationDetailsRequest request, String accountIds, String timeZone) {

        validateTaskUpdationRequest(request, accountIds);

        List<Long> creatorAccountIdList = request.getAccountIdList() != null ? request.getAccountIdList() : new ArrayList<>();
        Set<Notification> responseList = new HashSet<>();
        List<Long> notificationTypeList = new ArrayList<>();
        List<Long> mentionsList = new ArrayList<>();

        if (request.getNotificationTypeIdList() != null && !request.getNotificationTypeIdList().isEmpty()) {
            for (String notificationTypeId : request.getNotificationTypeIdList()) {
                if (notificationTypeId != null && NotificationTaskUpdationDetails.contains(notificationTypeId)) {
                    List<NotificationType> notificationTypes =
                            notificationTypeRepository.findNotificationTypeIdByNotificationType(notificationTypeId);
                    if (notificationTypes == null || notificationTypes.isEmpty()) continue;

                    for (NotificationType type : notificationTypes) {
                        if (type != null && type.getNotificationTypeId() != null) {
                            if (MENTION_ALERTS_TC.equalsIgnoreCase(notificationTypeId)
                                    || IMMEDIATE_ATTENTION.equalsIgnoreCase(notificationTypeId)) {
                                mentionsList.add(type.getNotificationTypeId());
                            } else {
                                notificationTypeList.add(type.getNotificationTypeId());
                            }
                        }
                    }
                }
            }
        }

        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();
        Integer entityTypeId = request.getEntityTypeId();

        List<Notification> notifications = new ArrayList<>();

        if (entityTypeId != null) {
            if (Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
                notifications.addAll(Optional.ofNullable(
                                notificationRepository.findByCreatorAccountIdAndTypeIdsAndTeamIdAndCreatedDateBetween(
                                        creatorAccountIdList, notificationTypeList, request.getEntityId(), fromDate, toDate))
                        .orElse(Collections.emptyList()));
            } else if (Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
                notifications.addAll(Optional.ofNullable(
                                notificationRepository.findByCreatorAccountIdAndTypeIdsAndProjectIdAndCreatedDateBetween(
                                        creatorAccountIdList, notificationTypeList, request.getEntityId(), fromDate, toDate))
                        .orElse(Collections.emptyList()));
            } else if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
                notifications.addAll(Optional.ofNullable(
                                notificationRepository.findByCreatorAccountIdAndTypeIdsAndOrgIdAndCreatedDateBetween(
                                        creatorAccountIdList, notificationTypeList, request.getEntityId(), fromDate, toDate))
                        .orElse(Collections.emptyList()));
            }
        }
        responseList.addAll(notifications);
        List<Notification> mentionNotificationList = new ArrayList<>();
        if (!mentionsList.isEmpty() && !creatorAccountIdList.isEmpty() && entityTypeId != null) {
            for (Long mentionAccountId : creatorAccountIdList) {
                if (mentionAccountId == null) continue;
                List<Notification> notificationsForMention = new ArrayList<>();
                if (Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
                    notificationsForMention = Optional.ofNullable(
                                    notificationRepository.findByTaggedIdAndNotificationTypeIdsAndTeamIdAndCreatedDateBetween(
                                            mentionAccountId, mentionsList, request.getEntityId(), fromDate, toDate))
                            .orElse(Collections.emptyList());
                } else if (Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
                    notificationsForMention = Optional.ofNullable(
                                    notificationRepository.findByTaggedIdAndNotificationTypeIdsAndProjectIdAndCreatedDateBetween(
                                            mentionAccountId, mentionsList, request.getEntityId(), fromDate, toDate))
                            .orElse(Collections.emptyList());
                } else if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
                    notificationsForMention = Optional.ofNullable(
                                    notificationRepository.findByTaggedIdAndNotificationTypeIdsAndOrgIdAndCreatedDateBetween(
                                            mentionAccountId, mentionsList, request.getEntityId(), fromDate, toDate))
                            .orElse(Collections.emptyList());
                }
                if (!notificationsForMention.isEmpty()) {
                    mentionNotificationList.addAll(notificationsForMention);
                }
            }
        }
        responseList.addAll(mentionNotificationList);
        return createTaskUpdationDetailResponse(request, responseList, timeZone);
    }


    public void validateTaskUpdationRequest(TaskUpdationDetailsRequest request, String accountIds) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getFromDate() == null || request.getToDate() == null || request.getFromDate().isAfter(request.getToDate())) {
            LocalDate toDate = LocalDate.now();
            LocalDate fromDate = toDate.minusWeeks(1);
            request.setFromDate(fromDate);
            request.setToDate(toDate);
        }
        if (request.getAccountIdList() == null || request.getAccountIdList().isEmpty()) {
            List<Long> membersList = accessDomainService.getAllEntityMembersByEntityTypeIdAndEntityId(
                    request.getEntityTypeId(),
                    request.getEntityId()
            );
            if (membersList == null) {
                membersList = new ArrayList<>();
            }
            request.setAccountIdList(membersList);
        }
    }


    public Set<TaskUpdationDetailsResponseDto> createTaskUpdationDetailResponse(TaskUpdationDetailsRequest request, Set<Notification> notificationList, String timeZone) {
        Set<TaskUpdationDetailsResponseDto> responseList = new HashSet<>();
        if (notificationList == null || notificationList.isEmpty()) {
            return responseList;
        }
        for (Notification notification : notificationList) {
            if (notification == null) continue;
            TaskUpdationDetailsResponseDto dto = new TaskUpdationDetailsResponseDto();
            dto.setNotificationType(notification.getNotificationTypeID().getNotificationType());
            String taskNumber = notification.getTaskNumber();
            Long teamId = (notification.getTeamId() != null) ? notification.getTeamId().getTeamId() : null;
            if (taskNumber != null && teamId != null) {
                List<Task> tasks = taskRepository.findByTaskNumberAndFkTeamIdTeamId(taskNumber, teamId);
                if (tasks != null && !tasks.isEmpty()) {
                    Task task = tasks.get(0);
                    if (task != null) {
                        if (task.getFkAccountIdAssigned() != null &&
                                task.getFkAccountIdAssigned().getAccountId() != null) {

                            dto.setAssigneeUser(
                                    userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(
                                            task.getFkAccountIdAssigned().getAccountId()
                                    )
                            );
                        }
                        if (task.getTaskId() != null) dto.setTaskId(task.getTaskId());
                        dto.setTaskNumber(task.getTaskNumber());

                        if (task.getFkTeamId() != null && task.getFkTeamId().getTeamId() != null)
                            dto.setTeamId(task.getFkTeamId().getTeamId());

                        if (task.getFkProjectId() != null && task.getFkProjectId().getProjectId() != null)
                            dto.setProjectId(task.getFkProjectId().getProjectId());

                        if (task.getFkOrgId() != null && task.getFkOrgId().getOrgId() != null)
                            dto.setOrgId(task.getFkOrgId().getOrgId());
                    }
                }
            }
            if (notification.getCreatedDateTime() != null && timeZone != null)
                dto.setUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(notification.getCreatedDateTime(), timeZone));
            Long creatorId = (notification.getNotificationCreatorAccountId() != null)
                    ? notification.getNotificationCreatorAccountId().getAccountId()
                    : null;
            if (creatorId != null)
                dto.setUpdatedBy(userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(creatorId));
            if (notification.getCommentLogId() != null) {
                Comment commentDb = commentRepository.findByCommentLogId(notification.getCommentLogId());
                if (commentDb != null) {
                    dto.setComment(commentDb.getComment());
                    dto.setCommentId(commentDb.getCommentId());
                    dto.setCommentLogId(commentDb.getCommentLogId());
                }
            }
            if (notification.getTaggedAccountIds() != null && !notification.getTaggedAccountIds().isEmpty()) {
                List<EmailFirstLastAccountIdIsActive> taggedUsers = new ArrayList<>();
                for (Long taggedId : notification.getTaggedAccountIds()) {
                    if (taggedId != null) {
                        EmailFirstLastAccountIdIsActive userDto =
                                userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(taggedId);
                        if (userDto != null) {
                            taggedUsers.add(userDto);
                        }
                    }
                }
                dto.setTaggedMentionUsers(taggedUsers);
            }
            dto.setPayLoad(notification.getPayload());
            dto.setOldValue(notification.getOldValue());
            dto.setNewValue(notification.getNewValue());
            responseList.add(dto);
        }
        return sortTaskUpdationDetails(request, responseList);
    }

    public Set<TaskUpdationDetailsResponseDto> sortTaskUpdationDetails(
            TaskUpdationDetailsRequest request,
            Set<TaskUpdationDetailsResponseDto> updationTaskList) {

        if (updationTaskList == null || updationTaskList.isEmpty()) {
            return Collections.emptySet();
        }
        List<TaskUpdationDetailsResponseDto> sortedList = new ArrayList<>(updationTaskList);
        if (Boolean.TRUE.equals(request.getSortByName())) {
            sortedList.sort(Comparator.comparing(
                    dto -> (dto.getUpdatedBy() != null
                            ? (dto.getUpdatedBy().getFirstName() + " " + dto.getUpdatedBy().getLastName())
                            : ""),
                    String.CASE_INSENSITIVE_ORDER
            ));
        }
        else if (Boolean.TRUE.equals(request.getSortByTaskNumber())) {
            sortedList.sort(Comparator.comparing(
                    TaskUpdationDetailsResponseDto::getTaskNumber,
                    String.CASE_INSENSITIVE_ORDER
            ));
        }
        else {
            sortedList.sort(Comparator.comparing(
                    TaskUpdationDetailsResponseDto::getUpdatedDateTime,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));
        }
        return new LinkedHashSet<>(sortedList);
    }

    public List<HashMap<String, String>> getFencingPunchRequestNotification(Long orgId, List<Long> accountIdList, String notificationMessage, Long punchRequestId) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.PUNCH_REQUEST));
        newNotification.setOrgId(organizationRepository.findByOrgId(orgId));
        newNotification.setTeamId(null);
        newNotification.setAccountId(null);
        newNotification.setNotificationTitle("Geo-fencing Punch Request");
        newNotification.setPunchRequestId(punchRequestId);
        newNotification.setNotificationBody(notificationMessage);
        newNotification.setCategoryId(newNotification.getNotificationTypeID().getNotificationCategoryId());
        newNotification.setPayload(geoFencingPayload(newNotification,Constants.NotificationType.PUNCH_REQUEST));

        Notification notifi = notificationRepository.save(newNotification);
        HashSet<Long> accountIdListToSendNotification = new HashSet<>();
        if (accountIdList != null) {
            accountIdListToSendNotification.addAll(new HashSet<>(accountIdList));
        }
            newNotificationView(notifi, accountIdListToSendNotification);
        return updatingPayloadFormat(new HashSet<>(accountIdList), notifi.getPayload(), notifi.getNotificationId(), notifi.getCreatedDateTime());

    }



    private String geoFencingPayload(Notification newNotification, String geoFenceType) {
        Payload payload = new Payload();
        payload.setAccountId(null);
        payload.setNotificationId(String.valueOf(newNotification.getNotificationId()));
        payload.setNotificationType(geoFenceType);
        payload.setTitle(newNotification.getNotificationTitle());
        payload.setBody(newNotification.getNotificationBody());
        payload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.TASK_STATE)));
        payload.setCategoryId(String.valueOf(newNotification.getCategoryId()));
        if(newNotification.getOrgId()!=null) {
            payload.setOrgId(String.valueOf(newNotification.getOrgId().getOrgId()));
        }
        if (newNotification.getPunchRequestId() != null) {
            payload.setPunchRequestId(String.valueOf(newNotification.getPunchRequestId()));
        }
        //Convert to String
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String taskPayloadString;
        try {
            taskPayloadString = objectWriter.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new JsonException("Error converting Json to string");
        }

        return taskPayloadString;
    }
    public List<HashMap<String, String>> createPayloadForLeavesNotification(List<LeaveApplication> leaveApplicationList) {
        List<HashMap<String, String>> notificationList = new ArrayList<HashMap<String, String>>();
        try {
            String title="Leave Application Expired";
            for (LeaveApplication leaveApplication:leaveApplicationList) {
                Long applicationId = leaveApplication.getLeaveApplicationId();
                UserAccount userAccount=userAccountRepository.findByAccountId(leaveApplication.getAccountId());
                String body="The leave application for "+userAccount.getEmail()+" from"+leaveApplication.getFromDate()+" to "+leaveApplication.getToDate() +" will expired and requires your review.";
                HashSet<Long> accountIdList = new HashSet<>();
                accountIdList.add(leaveApplication.getApproverAccountId());
                notificationList.addAll(updatePayloadFormatForExpiredLeaves(accountIdList, leaveApplication, LEAVE_EXPIRED, title,body));
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification not created in createPayloadForLeavesNotification. Caught Exception: " + e, new Throwable(allStackTraces));
        }
        return notificationList;
    }
    private List<HashMap<String, String>> updatePayloadFormatForExpiredLeaves(HashSet<Long> accountIdList, LeaveApplication leaveApplication, String notificationType, String title, String body) {
        Payload load = new Payload();
        LocalDateTime createdDateTime = leaveApplication.getCreatedDateTime();
        load.setNotificationType(notificationType);
        load.setTitle(title);
        load.setBody(body);
        load.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));
        NotificationType notificationTypeId = notificationTypeRepository.findByNotificationType(notificationType);
        List<HashMap<String, String>> listOfPayload = new ArrayList<HashMap<String, String>>();
        //Create notification for meeting reminder
        load.setAccountId(leaveApplication.getAccountId().toString());
        load.setCategoryId(notificationTypeId.getNotificationCategoryId().toString());
        Notification notifi = createExpiredLeavesNotification(notificationTypeId, leaveApplication, load);
        //creating new notificationView for notification for each accountId
        for (Long accountId : accountIdList) {
            schedulingService.newNotificationView(notifi, accountId);
            load.setAccountId(String.valueOf(accountId));
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
            if (userAccount != null && createdDateTime != null) {
                ZonedDateTime systemZonedDateTime = createdDateTime.atZone(ZoneId.systemDefault());
                ZoneId userTimeZone = ZoneId.of(userAccount.getFkUserId().getTimeZone());
//                ZonedDateTime zonedDateTime = createdDateTime.atZone(userTimeZone);
                ZonedDateTime zonedDateTime = systemZonedDateTime.withZoneSameInstant(userTimeZone);
                LocalDateTime localDateTime = LocalDateTime.from(zonedDateTime);
                load.setCreatedDateTime(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            load.setNotificationId(String.valueOf(notifi.getNotificationId()));
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> entry.getValue() == null);
            listOfPayload.add(loadMap);
        }
        return listOfPayload;
    }

    private Notification createExpiredLeavesNotification(NotificationType notificationTypeId, LeaveApplication leaveApplication, Payload load) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeId);
        newNotification.setAccountId(userAccountRepository.findByAccountId(leaveApplication.getApproverAccountId()));
        newNotification.setNotificationTitle(convertTypeToString(load.getTitle()));
        newNotification.setNotificationBody(load.getBody());
        newNotification.setPayload(gson.toJson(load));
        newNotification.setCategoryId(NotificationTypeToCategory.LEAVE_EXPIRED.getCategoryId());
        return notificationRepository.save(newNotification);
    }
}


