package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.NotificationTypeToCategory;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.constants.geo_fencing.EventAction;
import com.tse.core_application.constants.geo_fencing.EventKind;
import com.tse.core_application.constants.geo_fencing.EventSource;
import com.tse.core_application.constants.geo_fencing.IntegrityVerdict;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.UserName;
import com.tse.core_application.dto.AlertRequest;
import com.tse.core_application.dto.TasksMailResponse;
import com.tse.core_application.dto.UserPreferenceDTO;
import com.tse.core_application.dto.leave.Request.ChangeLeaveStatusRequest;
import com.tse.core_application.dto.leave.Request.LeaveApplicationNotificationRequest;
import com.tse.core_application.dto.notification_payload.Payload;
import com.tse.core_application.exception.JsonException;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.geo_fencing.attendance.AttendanceDay;
import com.tse.core_application.model.geo_fencing.attendance.AttendanceEvent;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.model.geo_fencing.punch.PunchRequest;
import com.tse.core_application.repository.*;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.geo_fencing.punch.PunchRequestRepository;
import com.tse.core_application.service.Impl.geo_fencing.attendance.DayRollupService;
import com.tse.core_application.service.Impl.geo_fencing.attendance.HolidayProvider;
import com.tse.core_application.service.Impl.geo_fencing.attendance.OfficePolicyProvider;
import com.tse.core_application.service.Impl.geo_fencing.membership.MembershipProvider;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.constants.Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID;
import static java.time.LocalTime.now;
import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class SchedulingService {
    @Autowired
    MeetingRepository meetingRepository;
    @Autowired
    AttendeeRepository attendeeRepository;

    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    NotificationTypeRepository notificationTypeRepository;
    @Autowired
    NotificationViewRepository notificationViewRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    LeavePolicyRepository leavePolicyRepository;
    @Autowired
    LeaveRemainingRepository leaveRemainingRepository;

    @Autowired
    ScrollToRepository scrollToRepository;

    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    UserPreferenceService userPreferenceService;
    @Autowired
    EmailService emailService;
    @Autowired
    EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    TimeSheetRepository timeSheetRepository;
    @Autowired
    EntityPreferenceService entityPreferenceService;
    @Autowired
    ReminderRepository reminderRepository;
    @Autowired
    TaskServiceImpl taskServiceImpl;
    @Autowired
    NotificationService notificationService;
    @Autowired
    AlertService alertService;
    @Autowired
    DependencyRepository dependencyRepository;
    @Autowired
    LeaveApplicationRepository leaveApplicationRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SprintRepository sprintRepository;
    @Autowired
    LeaveRemainingHistoryRepository leaveRemainingHistoryRepository;
    @Autowired
    private UserPreferenceRepository userPreferenceRepository;
    @Autowired
    private HolidayProvider holidayProvider;
    @Autowired
    private AttendancePolicyRepository policyRepository;
    @Autowired
    private OfficePolicyProvider officePolicyProvider;
    @Autowired
    private AttendanceDayRepository dayRepository;
    @Autowired
    private AttendanceEventRepository eventRepository;
    @Autowired
    private DayRollupService dayRollupService;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private PunchRequestRepository punchRequestRepository;
    @Autowired
    private MembershipProvider membershipProvider;
    @Autowired
    private AiMlService aiMlService;
    @Autowired AlertRepository alertRepository;

    @Value("${default.office.minutes}")
    private Long defaultOfficeMinutes;


    @Value("${system.admin.email}")
    private String adminEmail;

    @Value("${system.admin.firstname}")
    private String adminFirstName;

    @Value("${app.environment}")
    private Integer environment;

    private static final Logger logger = LogManager.getLogger(SchedulingService.class.getName());

    Gson gson = new Gson();

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     *
     * @param meetingList
     * @return list of hashmap<string, string>
     *     which identifies as keys and values in payload for meeting notification.
     */

    public List<HashMap<String,String>> meetingReminderService(List<Meeting> meetingList){
        List<HashMap<String,String>> remindersList = new ArrayList<HashMap<String, String>>();
        try {
        for(Meeting meeting:meetingList) {
            // get attendees from each meeting
            Long meetingId = meeting.getMeetingId();
            HashSet<Long> accountIdList= new HashSet<>();
            accountIdList.add(meeting.getOrganizerAccountId());
            accountIdList.addAll(attendeeRepository.findAccountIdByMeetingIdAndAttendeeInvitationStatus(meetingRepository.findByMeetingId(meetingId),ATTENDEE_INVITED_ID));
            //set payload for reminder notification
            remindersList.addAll(updatingPayloadFormat(accountIdList,meeting,Constants.NotificationType.MEETING_REMINDER, NotificationTypeToCategory.MEETING_REMINDER.getCategoryId(),
                    Constants.MeetingReminder.title(meeting.getReminderTime(),meeting.getMeetingNumber()),
                    Constants.MeetingReminder.body(meeting.getVenue(),meeting.getReminderTime(),meeting.getMeetingNumber())));
        }
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification not created in meetingReminderService. Caught Exception: "+e, new Throwable(allStackTraces));
        }
        //return the payload
        return remindersList;
    }


    /**
     *
     * @param accountIdList
     * @param meeting
     * @return list of hashmap<string,string>
     *     which will be added to the main list of payload.
     * @Function: converts and adjust the payload to a format which is readable and account specific.
     */
    private List<HashMap<String, String>> updatingPayloadFormat(HashSet<Long> accountIdList, Meeting meeting, String notificationType, Integer categoryId, String title, String body) {
        NotificationType notificationTypeId=notificationTypeRepository.findByNotificationType(notificationType);
        List<HashMap<String, String>> listOfPayload = new ArrayList<HashMap<String, String>>();
        //creating new notificationView for notification for each accountId
        for(Long accountId:accountIdList){
        Payload load = new Payload();
        load.setCreatedDateTime(String.valueOf(meeting.getCreatedDateTime()));
        load.setNotificationType(notificationType);
        load.setTitle(title);
        load.setBody(body);
        load.setMeetingId(String.valueOf(meeting.getMeetingId()));
        if(notificationType.equals(Constants.NotificationType.END_MEETING_CONFIRMATION)) {
            load.setMeetingDate(meeting.getEndDateTime().toString());
        } else {
            load.setMeetingDate(meeting.getStartDateTime().toString());
        }
        if(meeting.getMeetingTypeIndicator().equals(com.tse.core_application.constants.Constants.Meeting_Type_Indicator.ONLINE)){
            load.setMeetingMode(Constants.MeetingMode.ONLINE);
        }
        else if(meeting.getMeetingTypeIndicator().equals(com.tse.core_application.constants.Constants.Meeting_Type_Indicator.OFFLINE)){
            load.setMeetingMode(Constants.MeetingMode.OFFLINE);
        }
        else{
            load.setMeetingMode(Constants.MeetingMode.HYBRID);
        }
        load.setMeetingVenue(meeting.getMeetingKey());
        load.setTaskNumber(null);
        load.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));

        //Create notification for meeting reminder
        Notification notifi = createMeetingReminderNotification(notificationTypeId, categoryId, meeting,load);
            newNotificationView(notifi,accountId);
            load.setAccountId(String.valueOf(accountId));
            load.setNotificationId(String.valueOf(notifi.getNotificationId()));
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> entry.getValue() == null);
            listOfPayload.add(loadMap);
        }
        return listOfPayload;
    }


    /**
     *
     * @param notifi
     * @param accountId
     * @Function: creates a new notification view for a particular accountId
     */
    public void newNotificationView(Notification notifi, Long accountId) {
        NotificationView notificationView = new NotificationView();
        notificationView.setNotificationId(notifi);
        notificationView.setAccountId(userAccountRepository.findByAccountId(accountId));
        notificationView.setIsRead(false);
        notificationViewRepository.save(notificationView);
    }

    /**
     *
     * @param notificationTypeId
     * @param meeting
     * @param load
     * @return new notification which is added to database recently.
     * @Function: create and save a new notification for the new meeting reminder.
     */
    private Notification createMeetingReminderNotification(NotificationType notificationTypeId, Integer categoryId, Meeting meeting, Payload load) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeId);
        newNotification.setOrgId(organizationRepository.findByOrgId(meeting.getOrgId()));
        newNotification.setCategoryId(categoryId);
        newNotification.setBuId(meeting.getBuId());
        newNotification.setProjectId(projectRepository.findByProjectId(meeting.getProjectId()));
        newNotification.setTeamId(teamRepository.findByTeamId(meeting.getTeamId()));
        newNotification.setAccountId(userAccountRepository.findByAccountId(meeting.getCreatedAccountId()));
        newNotification.setMeetingId(meeting.getMeetingId());
        newNotification.setNotificationTitle(notificationService.convertTypeToString(load.getTitle()));
        newNotification.setNotificationBody(load.getBody());
        newNotification.setPayload(gson.toJson(load));

        return notificationRepository.save(newNotification);
    }

    /**
     * @param leaveRemainingList
     * @Function: Reset all active leaveRemaining.
     */
    public void leaveRemainingReset(List<LeaveRemaining> leaveRemainingList) {
        for(LeaveRemaining leaveRemaining : leaveRemainingList){
            LeavePolicy leavePolicy=leavePolicyRepository.findByLeavePolicyId(leaveRemaining.getLeavePolicyId());
            // checking for entity preference if leaves are updated on the basis of pro rate or not
            if (entityPreferenceService.getIsYearlyLeaveUpdateOnProRata(leavePolicy.getTeamId(), leavePolicy.getOrgId())) {
                if (leavePolicy.getIsLeaveCarryForward()) {
                    if (leaveRemaining.getLeaveRemaining() > leavePolicy.getMaxLeaveCarryForward()) {
                        leaveRemaining.setLeaveRemaining(leavePolicy.getMaxLeaveCarryForward());
                    }
                } else {
                    leaveRemaining.setLeaveRemaining(0f);
                }
                leaveRemaining.setLeaveTaken(0f);
                leaveRemaining.setCalenderYear((short) (LocalDate.now().getYear()));
            }
        }
        leaveRemainingRepository.saveAll(leaveRemainingList);
    }

    /**
     * @param meeting
     * @return payload of notification for sending the meeting follow up after the meeting is completed.
     */
    public List<HashMap<String, String>> meetingFollowUpService(Meeting meeting) {
        List<HashMap<String, String>> remindersList = new ArrayList<HashMap<String, String>>();
        try{
            // get attendees from each meeting
            Long meetingId = meeting.getMeetingId();
            HashSet<Long> accountIdList = new HashSet<>();
            accountIdList.add(meeting.getOrganizerAccountId());
            accountIdList.addAll(attendeeRepository.findAccountIdByMeetingIdAndAttendeeInvitationStatus(meetingRepository.findByMeetingId(meetingId), ATTENDEE_INVITED_ID));
            //set payload for reminder notification
            remindersList.addAll(updatingPayloadFormat(accountIdList, meeting, Constants.NotificationType.MEETING_FOLLOW_UP, NotificationTypeToCategory.MEETING_FOLLOW_UP.getCategoryId(),
                    Constants.MeetingFollowUp.title,
                    Constants.MeetingFollowUp.body(meeting.getMeetingNumber())));
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification not created in meetingFollowUpService. Caught Exception: "+e, new Throwable(allStackTraces));
        }
        //return the payload
        return remindersList;
    }

    /**
     * @param accountIdList
     * @param orgId
     * @return payload of notification for timesheetReminder if it is not filled by an accountId
     */
        public List<HashMap<String, String>> timesheetReminder(List<Long> accountIdList, Long orgId, boolean forToday) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.TIMESHEET_REMINDER));
        newNotification.setOrgId(organizationRepository.findByOrgId(orgId));
        newNotification.setTeamId(null);
        newNotification.setAccountId(null);
        newNotification.setNotificationTitle(Constants.TimeSheetReminder.title);
        if(forToday)
            newNotification.setNotificationBody(Constants.TimeSheetReminder.body);
        else
            newNotification.setNotificationBody(Constants.DayBeforeTimeSheetReminder.body);
        newNotification.setPayload(timesheetPayload(newNotification,Constants.NotificationType.TIMESHEET_REMINDER));
        newNotification.setCategoryId(newNotification.getNotificationTypeID().getNotificationCategoryId());

        Notification notifi = notificationRepository.save(newNotification);
        for(Long accountId : accountIdList) {
            newNotificationView(notifi,accountId);
        }
        return updatePayloadFormat(notifi.getPayload(), notifi.getNotificationId(),notifi.getCategoryId(), accountIdList, notifi.getCreatedDateTime());

    }

    /**
     * @param newNotification
     * @param timesheetReminder
     * @return Create payload for the timesheet
     */
    private String timesheetPayload(Notification newNotification, String timesheetReminder) {
        Payload payload = new Payload();
        payload.setAccountId(null);
        payload.setNotificationId(String.valueOf(newNotification.getNotificationId()));
        payload.setNotificationType(timesheetReminder);
        payload.setTitle(newNotification.getNotificationTitle());
        payload.setBody(newNotification.getNotificationBody());
        payload.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.TASK_STATE)));

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

    /**
     * @param payload
     * @param notificationId
     * @param accountIds
     * @param createdDateTime
     * @return Update payload format to send as notification
     */
    private List<HashMap<String, String>> updatePayloadFormat(String payload, Long notificationId,Integer categoryId, List<Long> accountIds, LocalDateTime createdDateTime){
        Payload load = gson.fromJson(payload, Payload.class);
        load.setNotificationId(String.valueOf(notificationId));
        load.setCreatedDateTime(String.valueOf(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(createdDateTime, String.valueOf(ZoneId.systemDefault()))));
        load.setCategoryId(String.valueOf(categoryId));
        List<HashMap<String, String>> listOfPayload = new ArrayList<HashMap<String, String>>();
        for (Long userAccountId : accountIds) {
            load.setAccountId(String.valueOf(userAccountId));
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> (entry.getValue() == null || entry.getValue().equals("null")));
            listOfPayload.add(loadMap);
        }

        return listOfPayload;
    }

    public List<HashMap<String, String>> startMeetingConfirmation(List<Meeting> meetingList) {
        String title = "START MEETING CONFIRMATION";
        List<HashMap<String, String>> response = new ArrayList<>();
        for(Meeting meeting: meetingList) {
            String body = Constants.StartMeetingConfirmation.body(meeting.getMeetingNumber());
            response.addAll(createNotificationAndNotificationViewForOrganizer(List.of(meeting),Constants.NotificationType.START_MEETING_CONFIRMATION, NotificationTypeToCategory.START_MEETING_CONFIRMATION.getCategoryId(), title,body));
        }
        return response;
    }

    public List<HashMap<String, String>> endMeetingConfirmation(List<Meeting> meetingList) {
        String title = "END MEETING CONFIRMATION";
        List<HashMap<String, String>> response = new ArrayList<>();
        for(Meeting meeting: meetingList) {
            String body = Constants.EndMeetingConfirmation.body(meeting.getMeetingNumber());
            response.addAll(createNotificationAndNotificationViewForOrganizer(List.of(meeting),Constants.NotificationType.END_MEETING_CONFIRMATION, NotificationTypeToCategory.END_MEETING_CONFIRMATION.getCategoryId(), title,body));
        }
        return response;
    }

    private List<HashMap<String, String>> createNotificationAndNotificationViewForOrganizer(List<Meeting> meetingList, String notificationType, Integer categoryId, String title, String body) {
        List<HashMap<String, String>> payloadList = new ArrayList<>();
        try {
            for (Meeting meeting : meetingList) {
                HashSet<Long> accountIdList= new HashSet<>();
                accountIdList.add(meeting.getOrganizerAccountId());
                payloadList.addAll(updatingPayloadFormat(accountIdList, meeting, notificationType, categoryId, title,body));
            }
            return payloadList;
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification is not created for "+title+". Caught Exception: "+e, new Throwable(allStackTraces));
            return Collections.emptyList();
        }
    }

    public boolean deleteOldNotifications(List<Notification> notificationsList) {
        try {
            for(Notification notification:notificationsList) {
                //delete all notification views
                notificationViewRepository.removeByNotificationId(notification);
                //delete notification from notification table
                notificationRepository.removeByNotificationId(notification.getNotificationId());
            }
            return true;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to delete notification. Caught Error: "+ e,new Throwable(allStackTraces));
            return false;
        }
    }


    /**
     * This method creates a payload for making notifications for task reminders
     * @param taskList
     * @return
     */
    public List<HashMap<String, String>> createPayloadForTaskReminder(List<Task> taskList) {
        List<HashMap<String, String>> remindersList = new ArrayList<HashMap<String, String>>();
        try {
            for (Task task : taskList) {
                // get attendees from each meeting
                Long taskId = task.getTaskId();
                String text;
                HashSet<Long> accountIdList = new HashSet<>();
                if(Objects.equals(Constants.BlockedType.OTHER_REASON_ID,task.getBlockedReasonTypeId()) || Objects.equals(task.getBlockedReasonTypeId(),Constants.BlockedType.EXTERNAL_SOURCE))
                {
                    text="Work Item " + (task.getTaskNumber() + " is Blocked and Reason is " + task.getBlockedReason());
                    if(task.getFkAccountIdAssigned() != null) {
                        accountIdList.add(task.getFkAccountIdAssigned().getAccountId());
                    }
                }else{
                    accountIdList.add(task.getFkAccountIdRespondent().getAccountId());
                    UserName nameRespondent = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdRespondent().getAccountId())).getFkUserId().getUserId());
                    String byNameRespondent = nameRespondent.getFirstName() + " " + ((nameRespondent.getLastName() == null) ? "" : nameRespondent.getLastName());
                    text="Work Item " + (task.getTaskNumber() + " is Blocked by " + byNameRespondent + " and Reason is " + task.getBlockedReason());
                }
                //set payload for reminder notification
                remindersList.addAll(updatePayloadFormatForTaskReminder(accountIdList, task, Constants.NotificationType.TASK_REMINDER,
                        Constants.TaskReminder.title(task.getTaskNumber()),
                        Constants.TaskReminder.body(task.getTaskNumber(),text)));
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification not created in createPayloadForTaskReminder. Caught Exception: " + e, new Throwable(allStackTraces));
        }
        //return the payload
        return remindersList;
    }

    private List<HashMap<String, String>> updatePayloadFormatForTaskReminder(HashSet<Long> accountIdList, Task task, String notificationType, String title, String body) {
        Payload load = new Payload();
        load.setCreatedDateTime(String.valueOf(task.getCreatedDateTime()));
        load.setNotificationType(notificationType);
        load.setTitle(title);
        load.setBody(body);
        load.setTaskNumber(String.valueOf(task.getTaskNumber()));
        load.setScrollTo(String.valueOf(scrollToRepository.findScrollToIdByScrollToTitle(Constants.ScrollToType.SCROLL_NOT_REQUIRED)));

        NotificationType notificationTypeId = notificationTypeRepository.findByNotificationType(notificationType);
        List<HashMap<String, String>> listOfPayload = new ArrayList<HashMap<String, String>>();
        //Create notification for meeting reminder
        load.setAccountId(null);
        Notification notifi = createTaskReminderNotification(notificationTypeId, task, load);
        //creating new notificationView for notification for each accountId
        for (Long accountId : accountIdList) {
            newNotificationView(notifi, accountId);
            load.setAccountId(String.valueOf(accountId));
            load.setNotificationId(String.valueOf(notifi.getNotificationId()));
            HashMap<String, String> loadMap = objectMapper.convertValue(load, HashMap.class);
            loadMap.entrySet().removeIf(entry -> entry.getValue() == null);
            listOfPayload.add(loadMap);
        }
        return listOfPayload;
    }

    private Notification createTaskReminderNotification(NotificationType notificationTypeId, Task task, Payload load) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeId);
        newNotification.setOrgId(task.getFkOrgId());
        newNotification.setBuId(task.getBuId());
        newNotification.setProjectId(task.getFkProjectId());
        newNotification.setTeamId(task.getFkTeamId());
        newNotification.setAccountId(task.getFkAccountIdCreator());
        newNotification.setTaskNumber(task.getTaskNumber());
        newNotification.setNotificationTitle(notificationService.convertTypeToString(load.getTitle()));
        newNotification.setNotificationBody(load.getBody());
        newNotification.setPayload(gson.toJson(load));

        return notificationRepository.save(newNotification);
    }

    public List<Task> getTaskListForReminder() {
        List<Integer>blockedTypeList=List.of(Constants.BlockedType.EXTERNAL_SOURCE,Constants.BlockedType.INTERNAL_TEAM_MEMBER,Constants.BlockedType.OTHER_REASON_ID,Constants.BlockedType.INTERNAL_TO_ORG);
        List<Task> taskListForReminders = taskRepository.findTaskByReasonTypeIdAndWorkFlowStatusBlockedAndReminderIntervalNotNull(blockedTypeList, com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE);
        if (!taskListForReminders.isEmpty()) {
            List<Task> taskList = new ArrayList<>();
            //creating a list of tasks which require notifications
            for (Task taskForReminder : taskListForReminders) {

                //checking task on basis of workflow status and nextReminderDateTime
                if ((Objects.equals(taskForReminder.getNextReminderDateTime().toLocalDate(), LocalDate.now()))) {

                    //adding tasks to list according to user preferences
                    UserPreferenceDTO userPreferenceDTO = userPreferenceService.getUserPreference(taskForReminder.getFkAccountIdAssigned().getFkUserId().getUserId());
                    if (userPreferenceDTO.getTaskReminderPreference() != null && Objects.equals(userPreferenceDTO.getTaskReminderPreference(), com.tse.core_application.model.Constants.TaskReminder.EMAIL)) {
                        if(Objects.equals(taskForReminder.getBlockedReasonTypeId(),Constants.BlockedType.OTHER_REASON_ID) || Objects.equals(taskForReminder.getBlockedReasonTypeId(),Constants.BlockedType.EXTERNAL_SOURCE)){
                            if(taskForReminder.getFkAccountIdAssigned() != null) {
                                String text="Work Item " + taskForReminder.getTaskNumber() + " is Blocked and Reason is " + taskForReminder.getBlockedReason();
                                emailService.sendBlockedTaskReminder(taskForReminder.getFkAccountIdAssigned().getEmail(),text, String.valueOf(taskForReminder));
                            }
                        }else if(taskForReminder.getFkAccountIdRespondent() != null) {
                            UserName nameRespondent = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(taskForReminder.getFkAccountIdRespondent().getAccountId())).getFkUserId().getUserId());
                            String byNameRespondent = nameRespondent.getFirstName() + " " + ((nameRespondent.getLastName() == null) ? "" : nameRespondent.getLastName());
                            String text="Work Item " + (taskForReminder.getTaskNumber() + " is Blocked by " + byNameRespondent + " and Reason is " + taskForReminder.getBlockedReason());
                            emailService.sendBlockedTaskReminder(taskForReminder.getFkAccountIdRespondent().getEmail(),text, String.valueOf(taskForReminder.getTaskNumber()));
                        }
                    } else if (userPreferenceDTO.getTaskReminderPreference() != null && Objects.equals(userPreferenceDTO.getTaskReminderPreference(), com.tse.core_application.model.Constants.TaskReminder.NOTIFICATION)) {
                        taskList.add(taskForReminder);
                    } else {
                        if(Objects.equals(taskForReminder.getBlockedReasonTypeId(),Constants.BlockedType.OTHER_REASON_ID) || Objects.equals(taskForReminder.getBlockedReasonTypeId(),Constants.BlockedType.EXTERNAL_SOURCE)){
                            if(taskForReminder.getFkAccountIdAssigned() != null) {
                                String text="Work Item " + taskForReminder.getTaskNumber() + " is Blocked and Reason is " + taskForReminder.getBlockedReason();
                                emailService.sendBlockedTaskReminder(taskForReminder.getFkAccountIdAssigned().getEmail(), text, String.valueOf(taskForReminder.getTaskNumber()));
                            }
                        }else if(taskForReminder.getFkAccountIdRespondent() != null) {
                            UserName nameRespondent = userRepository.findFirstNameAndLastNameByUserId((userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(taskForReminder.getFkAccountIdRespondent().getAccountId())).getFkUserId().getUserId());
                            String byNameRespondent = nameRespondent.getFirstName() + " " + ((nameRespondent.getLastName() == null) ? "" : nameRespondent.getLastName());
                            String text="Work Item " + (taskForReminder.getTaskNumber() + " is Blocked by " + byNameRespondent + " and Reason is " + taskForReminder.getBlockedReason());
                            emailService.sendBlockedTaskReminder(taskForReminder.getFkAccountIdRespondent().getEmail(), text, String.valueOf(taskForReminder.getTaskNumber()));
                        }
                        taskList.add(taskForReminder);
                    }
                    taskRepository.updateNextReminderDateTime(taskForReminder.getTaskId(), taskForReminder.getNextReminderDateTime().plusDays(taskForReminder.getReminderInterval()));
                }
            }
            return taskList;
        }
        return Collections.emptyList();

    }

    /**
     * This method gets all the organizations with holidays and pass them to addHolidayTimesheetForEntity
     */
    public void addHolidaysTimesheetForAllEntity () {
        List<EntityPreference> entityPreferenceList = entityPreferenceRepository.findByHolidayOffDayNotNullAndEntityTypeId(Constants.EntityTypes.ORG);

        if (!entityPreferenceList.isEmpty()) {
            for (EntityPreference entityPreference : entityPreferenceList) {
                List<AccountId> accountIdList = userAccountRepository.findAccountIdByOrgIdAndIsActive(entityPreference.getEntityId(), true);
                addHolidayTimesheetForEntity(entityPreference, accountIdList);
            }
        }
    }

    /**
     * This method gets the holidays for entity and pass them to addHolidayTimesheetForAccount
     */
    public void addHolidayTimesheetForEntity (EntityPreference entityPreference, List<AccountId> accountIdList) {
        Long officeMinutes = defaultOfficeMinutes;
        if (entityPreference.getOfficeHrsStartTime() != null && entityPreference.getOfficeHrsEndTime() != null) {
            officeMinutes = entityPreference.getOfficeHrsStartTime().until(entityPreference.getOfficeHrsEndTime(), ChronoUnit.MINUTES);
        }
        if (entityPreference.getBreakDuration() != null) {
            officeMinutes -= Long.valueOf(entityPreference.getBreakDuration());
        }
        List<HolidayOffDay> holidayOffDayList = entityPreference.getHolidayOffDays();
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        for (HolidayOffDay holidayOffDay : holidayOffDayList) {
            if (holidayOffDay.getDate().isAfter(LocalDate.now()) && !holidayOffDay.getDate().isAfter(nextSunday)) {
                for (AccountId accountId : accountIdList) {
                    addHolidayTimesheetForAccount(holidayOffDay, accountId, officeMinutes);
                }
            }
        }
    }

    /**
     * This method saves holiday timesheet in time tracking for account provided
     */
    public void addHolidayTimesheetForAccount (HolidayOffDay holidayOffDay, AccountId accountId, Long officeMinutes) {
        DataEncryptionConverter dataEncryptionConverter = new DataEncryptionConverter();
        TimeSheet timeSheet;
        timeSheet = timeSheetRepository.findByAccountIdAndEntityIdandEntityTypeId(accountId.getAccountId(), Constants.EntityTypes.HOLIDAY, holidayOffDay.getHolidayId(),holidayOffDay.getDate());
        if (timeSheet != null) {
            return;
        }
        UserAccount userAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accountId.getAccountId());
        timeSheet = new TimeSheet();
        timeSheet.setNewEffort(Math.toIntExact(officeMinutes));
        timeSheet.setNewEffortDate(holidayOffDay.getDate());
        timeSheet.setEntityId(holidayOffDay.getHolidayId());
        timeSheet.setEntityTitle(dataEncryptionConverter.convertToDatabaseColumn(holidayOffDay.getDescription()));
        timeSheet.setEntityTypeId(Constants.EntityTypes.HOLIDAY);
        timeSheet.setTaskTypeId(null);
        timeSheet.setBuId(null);
        timeSheet.setProjectId(null);
        timeSheet.setTeamId(null);
        timeSheet.setAccountId(userAccount.getAccountId());
        timeSheet.setOrgId(userAccount.getOrgId());
        timeSheet.setUserId(userAccount.getFkUserId().getUserId());
        timeSheet.setEarnedTime(Math.toIntExact(officeMinutes));

        timeSheetRepository.save(timeSheet);
    }

    /**
     * Update all leaveRemaining remaining leaves every months.
     */
    public void leaveRemainingMonthlyUpdate(List<LeaveRemaining> leaveRemainingList) {
        HashMap<Long, Set<Long>> failedAccountIds = new HashMap<>();
        createHistoryOfLeaveRemaining (leaveRemainingList);
        for(LeaveRemaining leaveRemaining : leaveRemainingList){
            LeavePolicy leavePolicy=leavePolicyRepository.findByLeavePolicyId(leaveRemaining.getLeavePolicyId());
            // checking for entity preference if leaves are updated on the basis of pro rate or not
            try {
                Boolean allowMonthlyUpdate = entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(leavePolicy.getTeamId(), leavePolicy.getOrgId());
                if (allowMonthlyUpdate) {
                    float monthlyLeave = leavePolicy.getInitialLeaves() / 12;
                    float remainingLeaves;
                    if (leaveRemaining.getStartLeavePolicyUsedOnce()) {
                        remainingLeaves = (leaveRemaining.getLeaveRemaining() != null ? leaveRemaining.getLeaveRemaining() : 0) + monthlyLeave;
                    } else {
                        YearMonth yearMonth = YearMonth.from(LocalDateTime.now());
                        int totalDaysInMonth = yearMonth.lengthOfMonth();
                        float assignmentDays = (float) (leaveRemaining.getStartLeavePolicyOn().until(LocalDateTime.now(), ChronoUnit.DAYS) + 1) / totalDaysInMonth;
                        if (assignmentDays < 0) continue;
                        if (assignmentDays > 1) {
                            float totalLeavesToAdd = 0;
                            int totalMonthsInBetween = 1;
                            while (assignmentDays > 1) {
                                totalLeavesToAdd += monthlyLeave;
                                assignmentDays--;
                                totalMonthsInBetween++;
                            }
                            YearMonth startMonth = yearMonth.minusMonths(totalMonthsInBetween);
                            int totalDaysOfStartMonth = startMonth.lengthOfMonth();
                            float leaveToAddFromStartMonth = (((float) (leaveRemaining.getStartLeavePolicyOn().toLocalDate().until(startMonth.atEndOfMonth(), ChronoUnit.DAYS) + 1) / totalDaysOfStartMonth) * monthlyLeave) + totalLeavesToAdd + (((float) (yearMonth.atDay(1).until(LocalDate.now(), DAYS) + 1) / totalDaysInMonth) * monthlyLeave);
                            remainingLeaves = (leaveRemaining.getLeaveRemaining() != null ? leaveRemaining.getLeaveRemaining() : 0) + leaveToAddFromStartMonth;
                        } else {
                            remainingLeaves = (leaveRemaining.getLeaveRemaining() != null ? leaveRemaining.getLeaveRemaining() : 0) + assignmentDays * monthlyLeave;
                        }
                        leaveRemaining.setStartLeavePolicyUsedOnce(true);
                    }

                    leaveRemaining.setLeaveRemaining(remainingLeaves);
                }
            } catch (Exception e) {
                Long orgId = leavePolicy != null ? leavePolicy.getOrgId() : userAccountRepository.findOrgIdByAccountIdAndIsActive(leaveRemaining.getAccountId(), true).getOrgId();
                Set<Long> accountIds = failedAccountIds.getOrDefault(orgId, new HashSet<>());
                accountIds.add(leaveRemaining.getAccountId());
                failedAccountIds.put(orgId, accountIds);
                logger.error("Update monthly leaves failed for accountId " + leaveRemaining.getAccountId() + " with error message : " + e.getMessage());
            }
        }
        leaveRemainingRepository.saveAll(leaveRemainingList);
        if (!failedAccountIds.isEmpty()) {
            failedAccountIds.forEach((orgId, accountIds) -> {
                Organization org = organizationRepository.findByOrgId(orgId);
                List<String> firstNameList = userAccountRepository.findFirstNameByAccountIdIn(new ArrayList<>(accountIds));
                String accounts = String.join(", ", firstNameList);
                emailService.sendFailedLeaveRemainingUpdateNotification(org.getOwnerEmail(), userRepository.findFirstNameByPrimaryEmail(org.getOwnerEmail()), org.getOrganizationName(), accounts);
                emailService.sendFailedLeaveRemainingUpdateNotification(adminEmail, adminFirstName, org.getOrganizationName(), accounts);
            });
        }
    }

    private void createHistoryOfLeaveRemaining(List<LeaveRemaining> leaveRemainingList) {
        if (leaveRemainingList != null && !leaveRemainingList.isEmpty()) {
            List<LeaveRemainingHistory> leaveRemainingHistoryList = new ArrayList<>();
            Short calenderMonth = (short) LocalDateTime.now().getMonth().getValue();
            for (LeaveRemaining leaveRemaining : leaveRemainingList) {
                LeaveRemainingHistory leaveRemainingHistory = new LeaveRemainingHistory();
                leaveRemainingHistory.setLeaveRemainingId(leaveRemaining.getLeaveRemainingId());
                leaveRemainingHistory.setAccountId(leaveRemaining.getAccountId());
                leaveRemainingHistory.setLeavePolicyId(leaveRemaining.getLeavePolicyId());
                leaveRemainingHistory.setLeaveTypeId(leaveRemaining.getLeaveTypeId());
                leaveRemainingHistory.setLeaveRemaining(leaveRemaining.getLeaveRemaining());
                leaveRemainingHistory.setLeaveTaken(leaveRemaining.getLeaveTaken());
                leaveRemainingHistory.setCalenderMonth(calenderMonth);
                leaveRemainingHistory.setCalenderYear(leaveRemaining.getCalenderYear());
                leaveRemainingHistoryList.add(leaveRemainingHistory);
            }
            leaveRemainingHistoryRepository.saveAll(leaveRemainingHistoryList);
        }
    }

    public void sendReminder () {
        Set<Reminder> allReminderList = new HashSet<>();
        List<Reminder> reminderList = reminderRepository.findAllByReminderDateAndReminderTimeAndReminderStatus(LocalDate.now(), now().withSecond(0).withNano(0), Constants.ReminderStatusEnum.PENDING.getStatus());
        List<Reminder> userPreferenceReminder = reminderRepository.findAllByReminderDateAndReminderStatus(LocalDate.now(), Constants.ReminderStatusEnum.PENDING.getStatus());
        for (Reminder reminder : userPreferenceReminder) {
            UserAccount user = reminder.getFkAccountIdCreator();
            UserPreference userPreference = userPreferenceRepository
                    .findByUserId(user.getFkUserId().getUserId());
            if (userPreference != null && userPreference.getUserPreferredReminderNotification() != null) {
                int preferredMinutes = userPreference.getUserPreferredReminderNotification();
                LocalTime notifyAt = reminder.getReminderTime().minusMinutes(preferredMinutes);
                LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
                if (notifyAt.equals(now)) {
                    allReminderList.add(reminder);
                }
            }
        }
        allReminderList.addAll(reminderList);
        List<Reminder> earlyRemindersList = reminderRepository.findDueEarlyReminders(Constants.ReminderStatusEnum.PENDING.getStatus(),LocalDateTime.now().withSecond(0).withNano(0));
        allReminderList.addAll(earlyRemindersList);
        List<Reminder> allReminderUniqueList = new ArrayList<>(allReminderList);
        if (!allReminderList.isEmpty()) {
            List<HashMap<String, String>> payloads = notificationService.createPayloadForUserReminder(allReminderUniqueList);
            taskServiceImpl.sendPushNotification(payloads);
            if (!reminderList.isEmpty()) {
                for (Reminder reminder : reminderList) {
                    if (reminder.getReminderTime().equals(now().withSecond(0).withNano(0))) {
                        reminder.setReminderStatus(Constants.ReminderStatusEnum.COMPLETED.getStatus());
                    }
                }
            }
            reminderRepository.saveAll(reminderList);
        }
    }


    /**
     * This method sends alert for all the delayed dependency tasks
     */
    public void sendDependencyAlert () {
        try{
            List<Task> taskList = taskRepository.findAllByTaskProgressSystemAndDependencyIdsNotNull(StatType.DELAYED);
            for (Task task : taskList) {
                List<Long> dependencyIds = new ArrayList<>(task.getDependencyIds());
                List<Dependency> currentTaskDependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(dependencyIds, false);
                if (!currentTaskDependencies.isEmpty()) {
                    List<Long> predecessorTaskIds = currentTaskDependencies.stream().map(Dependency::getPredecessorTaskId).collect(Collectors.toList());
                    if (!predecessorTaskIds.contains(task.getTaskId())) {
                        continue;
                    }
                }
                AlertRequest alertRequest = new AlertRequest();
                alertRequest.setAssociatedTaskId(task.getTaskId());
                alertRequest.setAssociatedTaskNumber(task.getTaskNumber());
                alertRequest.setAlertTitle(Constants.taskTypeMessages.get(task.getTaskTypeId()) + " " + task.getTaskNumber() + " , has dependency ");
                alertRequest.setAlertReason("Others Work Item are impacted due to dependency");
                alertRequest.setAlertType(Constants.AlertTypeEnum.DEPENDENCY.getType());
                alertRequest.setProjectId(task.getFkProjectId().getProjectId());
                alertRequest.setOrgId(task.getFkOrgId().getOrgId());
                alertRequest.setTeamId(task.getFkTeamId().getTeamId());
                alertRequest.setAccountIdReceiver(task.getFkAccountIdAssigned().getAccountId());
                alertRequest.setAccountIdSender(task.getFkAccountIdAssigned().getAccountId());
                UserAccount user = userAccountRepository.findByAccountId(task.getFkAccountIdAssigned().getAccountId());
                alertService.addAlert(alertRequest, task.getFkAccountIdAssigned().getAccountId().toString(), user.getFkUserId().getTimeZone());
            }
        } catch (Exception e) {
            throw e;
        }

    }

    public void expireLeaves () {
        List<LeaveApplication> allPendingLeaveApplications = leaveApplicationRepository.findByFromDateLessThanEqualAndLeaveApplicationStatusId(LocalDate.now(), Constants.LeaveApplicationStatusIds.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
        for (LeaveApplication leaveApplication : allPendingLeaveApplications) {
            leaveApplication.setLeaveApplicationStatusId(Constants.LeaveApplicationStatusIds.LEAVE_APPLICATION_EXPIRED_STATUS_ID);
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(leaveApplication.getAccountId(), true);
            if (userAccount == null) {
                continue;
            }
            leaveApplicationRepository.save(leaveApplication);
            String timeZone = userAccount.getFkUserId().getTimeZone();
            ChangeLeaveStatusRequest changeLeaveStatusRequest = new ChangeLeaveStatusRequest();
            changeLeaveStatusRequest.setLeaveApplicationStatusId(leaveApplication.getLeaveApplicationStatusId());
            changeLeaveStatusRequest.setAccountId(leaveApplication.getAccountId());
            changeLeaveStatusRequest.setApplicationId(leaveApplication.getLeaveApplicationId());
            changeLeaveStatusRequest.setApproverReason(leaveApplication.getApproverReason());
            LeaveApplicationNotificationRequest leaveApplicationNotificationRequest = new LeaveApplicationNotificationRequest();
            leaveApplicationNotificationRequest.setApplicantAccountId(leaveApplication.getAccountId());
            leaveApplicationNotificationRequest.setNotificationFor(Constants.NOTIFY_FOR_LEAVE_EXPIRY);
            leaveApplicationNotificationRequest.setSendNotification(true);
            leaveApplicationNotificationRequest.setApproverAccountId(leaveApplication.getApproverAccountId());
            leaveApplicationNotificationRequest.setFromDate(leaveApplication.getFromDate().toString());
            leaveApplicationNotificationRequest.setToDate(leaveApplication.getToDate().toString());
            List<Long> notifyToList = new ArrayList<>();
            notifyToList.add(leaveApplication.getAccountId());
            notifyToList.add(leaveApplication.getApproverAccountId());
            leaveApplicationNotificationRequest.setNotifyTo(notifyToList);
            List<HashMap<String, String>> payloadList = notificationService.notifyForLeaveApplication(changeLeaveStatusRequest, leaveApplicationNotificationRequest, timeZone);
            taskServiceImpl.sendPushNotification(payloadList);
        }
    }


    public void sendLeaveApprovalReminder () {
        List<LeaveApplication> allPendingLeaveApplications = leaveApplicationRepository.findByFromDateBetweenAndLeaveApplicationStatusId(LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), Constants.LeaveApplicationStatusIds.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
        for (LeaveApplication leaveApplication : allPendingLeaveApplications) {
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(leaveApplication.getApproverAccountId(), true);
            if (userAccount == null) {
                continue;
            }
            String timeZone = userAccount.getFkUserId().getTimeZone();
            LocalDateTime userDateTime = DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now(), timeZone);
            if (Objects.equals(LocalTime.from(userDateTime).withSecond(0).withNano(0), Constants.NOTIFICATION_TIME_FOR_LEAVE_APPROVAL_REMINDER)) {
                List<HashMap<String, String>> payloadList = notificationService.createPayloadForLeaveApprovalReminder(leaveApplication, userAccount);
                taskServiceImpl.sendPushNotification(payloadList);
            }
        }
    }

    public void changeLeaveStatusToConsumed () {
        List<LeaveApplication> allConsumedLeaveApplications = leaveApplicationRepository.findLeavesToSetConsumed(LocalDate.now(), Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID);
        for (LeaveApplication leaveApplication : allConsumedLeaveApplications) {
            leaveApplication.setLeaveApplicationStatusId(Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID);
        }
        leaveApplicationRepository.saveAll(allConsumedLeaveApplications);
    }


    public void sendTasksMail() throws JsonProcessingException {
        sendSprintTasksMail();
        sendParentChildTaskMail();
    }

    public void sendSprintTasksMail() throws JsonProcessingException {
        List<Sprint> sprintList = sprintRepository.getCustomSprints(Constants.ACTIVE_AND_FUTURE_SPRINT_STATUS_LIST);
        List<TasksMailResponse> sprintTasksMailResponseList = new ArrayList<>();
        HashMap<Integer, String> taskTypeMap = Constants.taskTypeMap;
        HashMap<Long, Task> parentTaskMap = new HashMap<>();

        for (Sprint sprint : sprintList) {
            List<Task> taskList = taskRepository.findBySprintId(sprint.getSprintId());

            for (Task task : taskList) {
                if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    parentTaskMap.putIfAbsent(task.getTaskId(), task);
                }

                if (isOutsideSprint(task, sprint)) {
                    sprintTasksMailResponseList.add(prepareTaskResponse(task, sprint, taskTypeMap, null));
                }
                else if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK) && task.getParentTaskId() != null) {
                    Task parentTask = fetchParentTask(task, parentTaskMap);
                    if (isOutsideParent(task, parentTask)) {
                        sprintTasksMailResponseList.add(prepareTaskResponse(task, sprint, taskTypeMap, parentTask));
                    }
                }
            }
        }

        if (!sprintTasksMailResponseList.isEmpty()) {
            String messageBody = buildHtmlEmailBody(sprintTasksMailResponseList);
            emailService.sendTasksMailToAdmin(adminEmail, adminFirstName, messageBody);
        }
    }

    public void sendParentChildTaskMail() throws JsonProcessingException {
        List<TasksMailResponse> childTaskMailResponseList = new ArrayList<>();
        HashMap<Integer, String> taskTypeMap = Constants.taskTypeMap;
        List<Task> parentTaskList = taskRepository.findByTaskTypeId (Constants.TaskTypes.PARENT_TASK);
        if (parentTaskList != null && !parentTaskList.isEmpty()) {
            for (Task parentTask : parentTaskList) {
                if (Objects.equals(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) ||
                        Objects.equals(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    continue;
                }
                List<Task> childTaskList = taskRepository.findByParentTaskId(parentTask.getTaskId());
                if (childTaskList != null && !childTaskList.isEmpty()) {
                    for (Task childTask : childTaskList) {
                        if (Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                            continue;
                        }
                        if (isOutsideParent(childTask, parentTask)) {
                            TasksMailResponse childTasksMailResponse = prepareTaskResponse(childTask, null, taskTypeMap, parentTask);
                            childTaskMailResponseList.add(childTasksMailResponse);
                        }
                    }
                }
            }
        }
        if (!childTaskMailResponseList.isEmpty()) {
            String messageBody = buildHtmlEmailBody(childTaskMailResponseList);
            emailService.sendTasksMailToAdmin(adminEmail, adminFirstName, messageBody);
        }
    }

    private boolean isOutsideSprint(Task task, Sprint sprint) {
        return ((task.getTaskExpStartDate() != null && task.getTaskActStDate() == null &&
                task.getTaskExpStartDate().isBefore(sprint.getSprintExpStartDate())) ||
                (task.getTaskExpEndDate() != null && task.getTaskExpEndDate().isAfter(sprint.getSprintExpEndDate())));
    }

    private boolean isOutsideParent(Task childTask, Task parentTask) {
        if (parentTask == null) return false;

        return ((parentTask.getTaskExpStartDate() != null && childTask.getTaskExpStartDate() != null &&
                childTask.getTaskExpStartDate().isBefore(parentTask.getTaskExpStartDate())) ||
                (parentTask.getTaskExpEndDate() != null && childTask.getTaskExpEndDate() != null &&
                        childTask.getTaskExpEndDate().isAfter(parentTask.getTaskExpEndDate())));
    }

    private Task fetchParentTask(Task childTask, Map<Long, Task> parentTaskMap) {
        Long parentId = childTask.getParentTaskId();
        if (!parentTaskMap.containsKey(parentId)) {
            Task parentTask = taskRepository.findByTaskId(parentId);
            if (parentTask != null) {
                parentTaskMap.put(parentId, parentTask);
            }
        }
        return parentTaskMap.get(parentId);
    }

    private TasksMailResponse prepareTaskResponse(Task task, Sprint sprint, Map<Integer, String> taskTypeMap, Task parentTask) {
        TasksMailResponse response = new TasksMailResponse();
        response.setTaskType(taskTypeMap.get(task.getTaskTypeId()));
        response.setTaskNumber(task.getTaskNumber());
        response.setTaskExpStartDate(task.getTaskExpStartDate());
        response.setTaskExpEndDate(task.getTaskExpEndDate());
        if (sprint != null) {
            response.setSprintTitle(sprint.getSprintTitle());
            response.setSprintExpStartDate(sprint.getSprintExpStartDate());
            response.setSprintExpEndDate(sprint.getSprintExpEndDate());
        }

        if (parentTask != null) {
            response.setParentTaskExpStartDate(parentTask.getTaskExpStartDate());
            response.setParentTaskExpEndDate(parentTask.getTaskExpEndDate());
        }

        return response;
    }

    private String buildHtmlEmailBody(List<TasksMailResponse> taskResponses) {
        String envIndicator;
        if (Objects.equals(com.tse.core_application.model.Constants.Environment.PROD.getTypeId(), environment)) {
            envIndicator = "[Environment: Prod]";
        } else if (Objects.equals(com.tse.core_application.model.Constants.Environment.PRE_PROD.getTypeId(), environment)) {
            envIndicator = "[Environment: Pre-Prod]";
        } else if (Objects.equals(com.tse.core_application.model.Constants.Environment.QA.getTypeId(), environment)) {
            envIndicator = "[Environment: QA]";
        } else {
            envIndicator = "[Environment: NA]";
        }

        StringBuilder messageBody = new StringBuilder();
        messageBody.append("<div style='font-family: Arial, sans-serif;'>")
                .append("<p><strong>").append(envIndicator).append("</strong></p>")
                .append("<ul>");

        for (TasksMailResponse task : taskResponses) {
            messageBody.append("<li>")
                    .append("<strong>Task Number:</strong> ").append(task.getTaskNumber()).append("<br>")
                    .append("<strong>Task Type:</strong> ").append(task.getTaskType()).append("<br>")
                    .append("<strong>Task Expected Start Date:</strong> ").append(task.getTaskExpStartDate()).append("<br>")
                    .append("<strong>Task Expected End Date:</strong> ").append(task.getTaskExpEndDate());

            if (task.getSprintTitle() != null || task.getSprintExpStartDate() != null || task.getSprintExpEndDate() != null) {
                messageBody.append("<br><strong>Sprint Title:</strong> ").append(task.getSprintTitle())
                        .append("<br><strong>Sprint Expected Start Date:</strong> ").append(task.getSprintExpStartDate())
                        .append("<br><strong>Sprint Expected End Date:</strong> ").append(task.getSprintExpEndDate());
            }
            if (task.getParentTaskExpStartDate() != null || task.getParentTaskExpEndDate() != null) {
                messageBody.append("<br><strong>Parent Task Expected Start Date:</strong> ").append(task.getParentTaskExpStartDate())
                        .append("<br><strong>Parent Task Expected End Date:</strong> ").append(task.getParentTaskExpEndDate());
            }

            messageBody.append("</li>");
        }

        messageBody.append("</ul></div>");
        return messageBody.toString();
    }

    // Geo fencing schedular

    /**
     * Process shift start notifications for all active organizations.
     * Called by scheduler or manually via controller.
     */
    public void processNotifyBeforeShiftStart() {
        // Get all policies where geo-fencing is active
        List<AttendancePolicy> activePolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getIsActive() != null && policy.getIsActive())
                .distinct()
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
            logger.info("No active attendance policies found");
            return;
        }

//        logger.info("Processing shift notifications for " + activePolicies.size() + " organizations");

        // Process each organization in parallel for optimization
        activePolicies.parallelStream().forEach(policy -> {
            try {
                processOrgNotification(policy);
            } catch (Exception e) {
                logger.error("Error processing notification for orgId=" + policy.getOrgId() + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Process shift notification for a single organization.
     */
    private void processOrgNotification(AttendancePolicy policy) {
        Long orgId = policy.getOrgId();

        // Get office hours for the org
        LocalTime officeStartTime = officePolicyProvider.getOfficeStartTime(orgId);
        Integer notifyBeforeShiftStartMin = policy.getNotifyBeforeShiftStartMin();
        String timezone = officePolicyProvider.getOperationalTimezone(orgId);

        // Calculate trigger time
        LocalTime triggerTime = officeStartTime.minusMinutes(notifyBeforeShiftStartMin);

        // Get current time in org's timezone
        ZoneId zoneId = ZoneId.of(timezone);
        LocalTime currentTime = LocalTime.now(zoneId);

        // Check if current time matches trigger time (rounded to minute)
        if (currentTime.getHour() == triggerTime.getHour() &&
                currentTime.getMinute() == triggerTime.getMinute()) {

//            logger.info("Trigger time matched for orgId=" + orgId +
//                    ". Sending notifications. TriggerTime=" + triggerTime +
//                    ", CurrentTime=" + currentTime);

            // Get all active users for this org (excluding already checked-in users)
            List<Long> usersToNotify = new ArrayList<>(new HashSet<>(getUsersToNotify(orgId)));

            if (!usersToNotify.isEmpty()) {
//                logger.info("Sending shift notifications to " + usersToNotify.size() +
//                        " users for orgId=" + orgId);

                sendShiftNotifications(orgId, usersToNotify, officeStartTime);

                // TODO: Record audit log
//                logger.info("Notifications sent to users: " + usersToNotify);
            } else {
                logger.info("No users to notify for orgId=" + orgId);
            }
        }
    }

    /**
     * Get list of users to notify (exclude already checked-in users).
     */
    private List<Long> getUsersToNotify(Long orgId) {
        // TODO: Replace with actual user service integration
        // For now, this is a placeholder that gets users who haven't checked in today

        LocalDate today = LocalDate.now(ZoneId.of(officePolicyProvider.getOperationalTimezone(orgId)));

        // Get all attendance days for today in this org
        List<AttendanceDay> todayAttendance = dayRepository.findAll().stream()
                .filter(day -> day.getOrgId().equals(orgId) && day.getDateKey().equals(today)).distinct()
                .collect(Collectors.toList());

        // Get users who already checked in (have firstInUtc set)
        Set<Long> checkedInUsers = todayAttendance.stream()
                .filter(day -> day.getFirstInUtc() != null)
                .map(AttendanceDay::getAccountId)
                .collect(Collectors.toSet());

        // TODO: Get all active users from org and filter out checked-in users
        List<Long> allActiveUsers = userAccountRepository.findAllAccountIdByOrgIdAndIsActive(orgId, true);
        return allActiveUsers.stream()
                .filter(accountId -> !checkedInUsers.contains(accountId)).distinct()
                .collect(Collectors.toList());

//        logger.info("Users already checked in for orgId=" + orgId + ": " + checkedInUsers.size());
//        return new ArrayList<>();  // Placeholder - needs user service integration
    }

    /**
     * Send shift start notifications to users.
     */
    private void sendShiftNotifications(Long orgId, List<Long> accountIds, LocalTime officeStartTime) {
        // TODO: Implement notification sending using the pattern from the example
        // This would follow the pattern:
        // 1. Create Notification entity
        // 2. Save to database
        // 3. Create notification views for each user
        // 4. Format payloads
        // 5. Send via FCM (taskService.sendPushNotification)

        String notificationMessage = String.format(
                "Reminder: Please check in. Your office hours start at %s.",
                officeStartTime.toString()
        );

//        logger.info("Notification message for orgId=" + orgId + ": " + notificationMessage);
//        logger.info("Would send notifications to " + accountIds.size() + " users");

        // TODO: Implement actual notification logic:
         List<HashMap<String, String>> payloads = getFencingCheckInNotification(orgId, accountIds, notificationMessage);
         taskServiceImpl.sendPushNotification(payloads);
    }

    public List<HashMap<String, String>> getFencingCheckInNotification(Long orgId, List<Long> accountIdList, String notificationMessage) {
        Notification newNotification = new Notification();
        newNotification.setNotificationTypeID(notificationTypeRepository.findByNotificationType(Constants.NotificationType.CHECK_IN_NOTIFICATION));
        newNotification.setOrgId(organizationRepository.findByOrgId(orgId));
        newNotification.setTeamId(null);
        newNotification.setAccountId(null);
        newNotification.setNotificationTitle("Geo-fencing check in reminder");
        newNotification.setNotificationBody(notificationMessage);
        newNotification.setPayload(timesheetPayload(newNotification,Constants.NotificationType.CHECK_IN_NOTIFICATION));
        newNotification.setCategoryId(newNotification.getNotificationTypeID().getNotificationCategoryId());

        Notification notifi = notificationRepository.save(newNotification);
        for(Long accountId : accountIdList) {
            newNotificationView(notifi,accountId);
        }
        return updatePayloadFormat(notifi.getPayload(), notifi.getNotificationId(),notifi.getCategoryId(), accountIdList, notifi.getCreatedDateTime());

    }

    /**
     * Process auto-checkout for all organizations.
     * Called by scheduler or manually via controller.
     */
    @Transactional
    public void processAutoCheckout() {
        // Get all active policies
        List<AttendancePolicy> activePolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getIsActive() != null && policy.getIsActive())
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
            logger.info("No active attendance policies found for auto-checkout");
            return;
        }

        logger.info("Processing auto-checkout for " + activePolicies.size() + " organizations");

        // Process each organization in parallel for optimization
        activePolicies.parallelStream().forEach(policy -> {
            try {
                processOrgAutoCheckout(policy);
            } catch (Exception e) {
                logger.error("Error processing auto-checkout for orgId=" + policy.getOrgId() + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Process auto-checkout for a single organization.
     * Handles:
     * - Missing checkout after maxCheckoutAfterEndMin
     * - Missing break end
     * - Respects holidays (only process if non-holiday or if check-in exists on holiday)
     * - Handles day boundary crossing (e.g., office end at 11:55 PM + 20 min grace = next day)
     */
    @Transactional
    public void processOrgAutoCheckout(AttendancePolicy policy) {
        Long orgId = policy.getOrgId();
        String timezone = Constants.DEFAULT_TIME_ZONE;
        List<AccountId> orgAdminAccountId = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId, List.of(RoleEnum.ORG_ADMIN.getRoleId()), true);
        if (orgAdminAccountId != null && !orgAdminAccountId.isEmpty()) {
            Long accountId = orgAdminAccountId.get(0).getAccountId();
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
            if (userAccount != null && userAccount.getFkUserId().getTimeZone() != null) {
                timezone = userAccount.getFkUserId().getTimeZone();
            }
        }
        ZoneId zoneId = ZoneId.of(timezone);

        // Get office hours
        LocalTime officeEndTime = officePolicyProvider.getOfficeEndTime(orgId);
        Integer maxCheckoutAfterEndMin = policy.getMaxCheckoutAfterEndMin();

        // Calculate cutoff time for auto-checkout
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();

        // Calculate when TODAY's office ends and its cutoff
        LocalDateTime todayOfficeEnd = today.atTime(officeEndTime);
        LocalDateTime todayCutoff = todayOfficeEnd.plusMinutes(maxCheckoutAfterEndMin);

        // Determine which date we should process
        final LocalDate dateToProcess;
        final LocalDateTime cutoffDateTime;

        if (todayCutoff.toLocalDate().equals(today)) {
            // Cutoff is still today (no midnight crossing)
            // Example: Office ends 5 PM, grace 60 min = cutoff 6 PM (same day)
            dateToProcess = today;
            cutoffDateTime = todayCutoff;
        } else {
            // Cutoff is tomorrow (crossed midnight)
            // Example: Office ends 11:55 PM, grace 20 min = cutoff 12:15 AM (next day)
            // When it's 12:15 AM on Oct 6, we process Oct 5's data
            dateToProcess = today.minusDays(1);
            cutoffDateTime = dateToProcess.atTime(officeEndTime).plusMinutes(maxCheckoutAfterEndMin);
        }

        LocalTime cutoffTime = cutoffDateTime.toLocalTime();
        LocalTime currentTime = now.toLocalTime();

        if (currentTime.getHour() != cutoffTime.getHour() ||
                currentTime.getMinute() != cutoffTime.getMinute()) {
//             Not the exact cutoff minute, skip
            return;
        }

//        logger.info("Processing auto-checkout for orgId=" + orgId + ", date=" + dateToProcess +
//                ", cutoffTime=" + cutoffDateTime);

        // Get all attendance days for the date to process
        List<AttendanceDay> dayRecords = dayRepository.findAll().stream()
                .filter(day -> day.getOrgId().equals(orgId) && day.getDateKey().equals(dateToProcess))
                .filter(day -> day.getOrgId().equals(orgId) &&
                        day.getDateKey().equals(dateToProcess) &&
                        day.getFirstInUtc() != null &&      // Has checked in
                        day.getLastOutUtc() == null)        // Hasn't checked out yet
                .collect(Collectors.toList());

//        logger.info("Found " + dayRecords.size() + " attendance records for orgId=" + orgId +
//                " on date=" + dateToProcess);

        for (AttendanceDay dayRecord : dayRecords) {
            try {
                boolean isHoliday = holidayProvider.isHoliday(orgId, dateToProcess, dayRecord.getAccountId());
                // If it's a holiday and no check-in, skip
                if (isHoliday && dayRecord.getFirstInUtc() == null) {
                    continue;
                }

                processUserAutoCheckout(orgId, dayRecord, dateToProcess, policy);
            } catch (Exception e) {
                logger.error("Error processing auto-checkout for accountId=" + dayRecord.getAccountId() +
                        " in orgId=" + orgId + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process auto-checkout for a single user.
     */
    @Transactional
    public void processUserAutoCheckout(Long orgId, AttendanceDay dayRecord, LocalDate dateKey, AttendancePolicy policy) {
        Long accountId = dayRecord.getAccountId();

        // Get all events for this user on this day
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();

        List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, accountId, dayStart, dayEnd);

        if (events.isEmpty()) {
            return;
        }

        // Determine current state by walking through events
        boolean needsCheckout = false;
        boolean needsBreakEnd = false;
        EventKind lastSuccessfulEvent = null;

        for (AttendanceEvent event : events) {
            if (event.getSuccess() != null && event.getSuccess()) {
                lastSuccessfulEvent = event.getEventKind();
            }
        }

        // Determine what needs to be done
        if (lastSuccessfulEvent == EventKind.CHECK_IN) {
            needsCheckout = true;
        } else if (lastSuccessfulEvent == EventKind.BREAK_START) {
            needsBreakEnd = true;
            needsCheckout = true;  // After break end, still need checkout
        }

        // Missing check-in - can't auto-checkout
        if (dayRecord.getFirstInUtc() == null) {
//            logger.info("Skipping auto-checkout for accountId=" + accountId +
//                    " - no check-in found");
            return;
        }

        // Process missing break end
        if (needsBreakEnd) {
            createAutoEvent(orgId, accountId, EventKind.BREAK_END, events);
//            logger.info("Created auto BREAK_END for accountId=" + accountId);

            // Refresh events after creating break end
            events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, dayStart, dayEnd);
        }

        // Process missing checkout
        if (needsCheckout && dayRecord.getLastOutUtc() == null) {
            createAutoEvent(orgId, accountId, EventKind.CHECK_OUT, events);
//            logger.info("Created auto CHECK_OUT for accountId=" + accountId);

            // Update day rollup
            events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, dayStart, dayEnd);
            dayRollupService.updateDayRollup(orgId, accountId, dateKey, events);

//            logger.info("Auto-checkout completed for accountId=" + accountId + " on date=" + dateKey);
        }
    }

    /**
     * Create an automatic event (BREAK_END or CHECK_OUT).
     */
    private void createAutoEvent(Long orgId, Long accountId, EventKind eventKind, List<AttendanceEvent> existingEvents) {
        AttendanceEvent autoEvent = new AttendanceEvent();
        autoEvent.setOrgId(orgId);
        autoEvent.setAccountId(accountId);
        autoEvent.setEventKind(eventKind);
        autoEvent.setEventSource(EventSource.MANUAL);  // Could also use a new EventSource.SYSTEM
        autoEvent.setEventAction(EventAction.AUTO);
        autoEvent.setTsUtc(LocalDateTime.now());
        autoEvent.setSuccess(true);
        autoEvent.setVerdict(IntegrityVerdict.PASS);
        autoEvent.setFlags(new HashMap<>());

        // Add flag to indicate this was auto-generated
        Map<String, Object> flags = new HashMap<>();
        flags.put("auto_checkout", true);
        flags.put("reason", "Missing " + eventKind.name().toLowerCase() + " after grace period");
        autoEvent.setFlags(flags);

        eventRepository.save(autoEvent);
    }

    /**
     * Process missed punches for expired punch requests.
     * Called by scheduler or manually via controller.
     */
    @Transactional
    public void processMissedPunches() {
        LocalDateTime now = LocalDateTime.now();

        // Get all active policies
        List<AttendancePolicy> activePolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getIsActive() != null && policy.getIsActive())
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
//            logger.info("No active attendance policies found for missed punch processing");
            return;
        }

//        logger.info("Processing missed punches for " + activePolicies.size() + " organizations");

        // Process each organization
        for (AttendancePolicy policy : activePolicies) {
            try {
                processOrgMissedPunches(policy.getOrgId(), now);
            } catch (Exception e) {
                logger.error("Error processing missed punches for orgId=" + policy.getOrgId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process missed punches for a single organization.
     * Finds all expired punch requests and marks missed punches for users who didn't respond.
     */
    @Transactional
    public void processOrgMissedPunches(Long orgId, LocalDateTime now) {
        String timezone = officePolicyProvider.getOperationalTimezone(orgId);
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDate today = LocalDate.now(zoneId);

        // Find all punch requests that have just expired (expiresAt matches current minute)
        List<PunchRequest> allRequests = punchRequestRepository.findAll();
        List<PunchRequest> expiredRequests = allRequests.stream()
                .filter(pr -> pr.getOrgId().equals(orgId))
                .filter(pr -> pr.getState() == PunchRequest.State.PENDING)
                .filter(pr -> {
                    LocalDateTime expiresAt = pr.getExpiresAt();
                    // Check if expires at current minute
                    return expiresAt.getYear() == now.getYear() &&
                            expiresAt.getMonthValue() == now.getMonthValue() &&
                            expiresAt.getDayOfMonth() == now.getDayOfMonth() &&
                            expiresAt.getHour() == now.getHour() &&
                            expiresAt.getMinute() == now.getMinute();
                })
                .collect(Collectors.toList());

        if (expiredRequests.isEmpty()) {
            return;
        }

//        logger.info("Found " + expiredRequests.size() + " expired punch requests for orgId=" + orgId);

        // Process each expired request
        for (PunchRequest request : expiredRequests) {
            try {
                processExpiredPunchRequest(orgId, request, today, zoneId);

                // Mark request as EXPIRED
                request.setState(PunchRequest.State.EXPIRED);
                punchRequestRepository.save(request);

                logger.info("Marked punch request " + request.getId() + " as EXPIRED");
            } catch (Exception e) {
                logger.error("Error processing expired punch request " + request.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process a single expired punch request.
     * Resolves all account IDs based on entity type and marks missed punches.
     */
    @Transactional
    public void processExpiredPunchRequest(Long orgId, PunchRequest request, LocalDate dateKey, ZoneId zoneId) {
        // Resolve account IDs based on entity type
        Set<Long> accountIds = resolveAccountIds(orgId, request.getEntityTypeId(), request.getEntityId());

        if (accountIds.isEmpty()) {
//            logger.info("No accounts found for punch request " + request.getId());
            return;
        }

//        logger.info("Processing " + accountIds.size() + " accounts for punch request " + request.getId());

        // For each account, check if they missed the punch
        for (Long accountId : accountIds) {
            try {
                processMissedPunchForAccount(orgId, accountId, request, dateKey, zoneId);
            } catch (Exception e) {
//                logger.error("Error processing missed punch for accountId=" + accountId +
//                        " in punch request " + request.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Resolve account IDs based on entity type and entity ID.
     *
     * For USER: Returns the entity ID directly as account ID.
     * For TEAM/PROJECT/ORG: This is a simplified implementation that checks all unique account IDs
     * from attendance events in the org. In production, this should use a dedicated UserService
     * or AccountService to get all active users in the organization/team/project.
     */
    private Set<Long> resolveAccountIds(Long orgId, Integer entityTypeId, Long entityId) {
        Set<Long> accountIds = new HashSet<>();

        if (entityTypeId == Constants.EntityTypes.USER) {
            // Direct user - entity ID is the account ID
            accountIds.add(entityId);
        } else if (entityTypeId == Constants.EntityTypes.TEAM) {
            // Get all unique account IDs from attendance events for this org
            // Then filter by team membership
            Set<Long> allAccountIds = getAllAccountIdsInOrg(orgId);

            for (Long accountId : allAccountIds) {
                List<Long> teams = membershipProvider.listTeamsForUser(orgId, accountId);
                if (teams.contains(entityId)) {
                    accountIds.add(accountId);
                }
            }
        } else if (entityTypeId == Constants.EntityTypes.PROJECT) {
            // Get all unique account IDs from attendance events for this org
            // Then filter by project membership
            Set<Long> allAccountIds = getAllAccountIdsInOrg(orgId);

            for (Long accountId : allAccountIds) {
                List<Long> teamIdList = membershipProvider.listTeamsForUser(orgId, accountId);
                List<Long> projects = membershipProvider.listProjectsForUser(orgId, accountId, teamIdList);
                if (projects.contains(entityId)) {
                    accountIds.add(accountId);
                }
            }
        } else if (entityTypeId == Constants.EntityTypes.ORG) {
            // Get all users in the organization
            accountIds = getAllAccountIdsInOrg(orgId);
        }

        return accountIds;
    }

    /**
     * Get all unique account IDs that have interacted with the attendance system for this org.
     * This includes accounts from both attendance days and attendance events.
     *
     * NOTE: This is a workaround. In production, you should use a UserService or AccountService
     * to get all active users in the organization, not just those with attendance records.
     */
    private Set<Long> getAllAccountIdsInOrg(Long orgId) {
        List<Long> accountIdList = userAccountRepository.findAllAccountIdByOrgIdAndIsActive(orgId, true);

        return accountIdList != null ? new HashSet<>(accountIdList) : new HashSet<>();
    }

    /**
     * Process missed punch for a single account.
     * Checks if user has checked in for the day and if they didn't respond to the punch request.
     */
    @Transactional
    public void processMissedPunchForAccount(Long orgId, Long accountId, PunchRequest request,
                                             LocalDate dateKey, ZoneId zoneId) {
        // Get attendance day record for this user
        Optional<AttendanceDay> dayRecordOpt = dayRepository.findByOrgIdAndAccountIdAndDateKey(orgId, accountId, dateKey);

        if (!dayRecordOpt.isPresent()) {
            // User hasn't checked in at all today, skip
            return;
        }

        AttendanceDay dayRecord = dayRecordOpt.get();

        // Check if user has checked in (has firstInUtc)
        if (dayRecord.getFirstInUtc() == null) {
            // User hasn't checked in, skip
            return;
        }

        // Get all events for this user on this day
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();
        List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, accountId, dayStart, dayEnd);

        // Check if user responded to this punch request
        boolean hasResponded = events.stream()
                .anyMatch(event -> {
                    Long punchRequestId = event.getPunchRequestId();
                    return punchRequestId != null && punchRequestId.equals(request.getId());
                });

        if (hasResponded) {
            // User responded to the punch request, no need to mark as missed
//            logger.info("User " + accountId + " responded to punch request " + request.getId());
            return;
        }

        // User missed the punch - create a missed punch event
        createMissedPunchEvent(orgId, accountId, request, events);

        logger.info("Marked missed punch for accountId=" + accountId + " for punch request " + request.getId());
    }

    /**
     * Create a missed punch event.
     */
    private void createMissedPunchEvent(Long orgId, Long accountId, PunchRequest request,
                                        List<AttendanceEvent> existingEvents) {
        AttendanceEvent missedEvent = new AttendanceEvent();
        missedEvent.setOrgId(orgId);
        missedEvent.setAccountId(accountId);
        missedEvent.setEventKind(EventKind.PUNCHED);  // Use PUNCHED kind for missed punches
        missedEvent.setEventSource(EventSource.MANUAL);
        missedEvent.setEventAction(EventAction.AUTO);
        missedEvent.setTsUtc(LocalDateTime.now());
        missedEvent.setSuccess(false);  // Marked as unsuccessful
        missedEvent.setVerdict(IntegrityVerdict.FAIL);
        missedEvent.setPunchRequestId(request.getId());
        missedEvent.setRequesterAccountId(request.getRequesterAccountId());
        missedEvent.setFailReason("Missed punch: User did not respond to punch request within time window");

        // Add flags to indicate this was auto-generated as missed
        Map<String, Object> flags = new HashMap<>();
        flags.put("missed_punch", true);
        flags.put("punch_request_id", request.getId());
        flags.put("requested_datetime", request.getRequestedDatetime().toString());
        flags.put("expired_at", request.getExpiresAt().toString());
        missedEvent.setFlags(flags);

        eventRepository.save(missedEvent);
    }

    public void retryFailedUserRegistration() {
        List<Long> failedRegisteredUser = userAccountRepository.findAllAccountIdByIsRegisteredInAiService(false);
        if(!failedRegisteredUser.isEmpty()){
            for (Long accountId : failedRegisteredUser) {
                try {
                    aiMlService.registerUserIntoAiService(accountId, com.tse.core_application.constants.Constants.AiMlConstants.MAX_TOKENS, "UTC");
                } catch (Exception e) {
                    System.out.println("User with AccountId: " + accountId + " is failed to register to AiService.");
                }
            }
        }
    }

    @Transactional
    public void deleteAlerts() {
        LocalDateTime cutoffLdt = LocalDateTime.now().minusDays(Constants.Alert_Deletion_Days);
        Timestamp cutoff = Timestamp.valueOf(cutoffLdt);

        int updated = alertRepository.updateIsDeletedTrueWhereCreatedDateTimeOlderThan(cutoff);
    }
}
