package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.*;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/schedule")
public class SchedulingController {
    private static final Logger logger = LogManager.getLogger(AuthController.class.getName());

    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private LeaveRemainingRepository leaveRemainingRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private OfficeHoursRepository officeHoursRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private CalendarDaysRepository calendarDaysRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserPreferenceService userPreferenceService;


    @PostMapping("/meeting/reminder")
    public ResponseEntity<Object> meetingReminderScheduler(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Meeting reminder method.");

        try {
            List<Meeting> meetingList = meetingRepository.findScheduledMeetings();
            if (meetingList.size()>0) {
//            meeting service
                List<HashMap<String, String>> payloads = schedulingService.meetingReminderService(meetingList);
//            pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(payloads);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Meeting reminder method completed. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Reminder send successfully");
            }
            else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Meeting reminder controller completed with no meeting scheduled currently.");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No meeting reminders to send currently.");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Meeting reminder could not be send. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }

    }

//    @PostMapping("/meeting/meetingFollowUp")
//    public ResponseEntity<Object> meetingFollowUpScheduler(){
//        long sprintStartTime = System.currentTimeMillis();
//        ThreadContext.put("accountId", String.valueOf(0));
//        ThreadContext.put("userId", String.valueOf(0));
//        logger.info("Entered Meeting follow up method. ");
//        try {
//            List<Meeting> meetingFollowUpList = meetingRepository.findMeetingsToFollowUp();
//            if (meetingFollowUpList!=null && meetingFollowUpList.size()>0) {
////            meeting service
//                List<HashMap<String, String>> payloads = schedulingService.meetingFollowUpService(meetingFollowUpList);
////            pass this payload to fcm for notification
//                taskServiceImpl.sendPushNotification(payloads);
//                long estimatedTime = System.currentTimeMillis() - sprintStartTime;
//                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
//                logger.info("Meeting follow up method completed ");
//                ThreadContext.clearMap();
//                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Reminder send successfully");
//            }
//            else {
//                long estimatedTime = System.currentTimeMillis() - sprintStartTime;
//                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
//                logger.info("Meeting follow up controller completed with no meeting follow up currently.");
//                ThreadContext.clearMap();
//                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No meeting reminders to send currently.");
//            }
//        }
//        catch (Exception e){
//            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
//            logger.error("Meeting follow up could not be send. Caught Exception: "+e, new Throwable(allStackTraces));
//            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
//        }
//
//    }

    @PostMapping("/leave/leaveRemainingReset")
    public ResponseEntity<Object> leaveRemainingReset(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Leave Remaining Reset method. ");
        try {
            List<LeaveRemaining> leaveRemainingList = leaveRemainingRepository.findByCurrentlyActive(true);
            if (leaveRemainingList.size()>0) {
                schedulingService.leaveRemainingReset(leaveRemainingList);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Leave Remaining Reset method completed. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Leave Remaining Reset successfully.");
            }
            else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Leave Remaining Reset controller completed with no leaveRemaining updated. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No Leave Remaining Reset.");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Leave Remaining could not be reset. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/timesheet/timesheetPreReminder")
    public ResponseEntity<Object> timesheetPreReminderScheduler(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Timesheet Pre Reminder method. ");
        try {
            List<Long> organizationList = new ArrayList<>();
            //TODO: need to take organizationList form officeHourRepository in future
//            List<Long> organizationList = officeHoursRepository.findOrgIdAccordingToOfficeHoursForPreTimeSheetReminder();
            if(LocalTime.now().isAfter(com.tse.core_application.model.Constants.ConstantForGettingOfficeHourEndTimeForOrg.beforeOfficeEndTime)
            && LocalTime.now().isBefore(com.tse.core_application.model.Constants.ConstantForGettingOfficeHourEndTimeForOrg.beforeOfficeEndTimeLimit)
            && calendarDaysRepository.findIsBusinessDayByCalendarDate(LocalDate.now())){  //here also using the method
                organizationList.addAll(organizationRepository.findAllOrgId());
            }
            for(Long orgId: organizationList) {
                List<Long> accountIdList = userAccountRepository.findAccountIdForTimeSheetReminderAndIsActive(orgId, true);
                if (accountIdList.size() > 0) {
                    List<HashMap<String, String>> payloads = schedulingService.timesheetReminder(accountIdList,orgId,true);
//            pass this payload to fcm for notification
                    taskServiceImpl.sendPushNotification(payloads);
                }
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Timesheet Pre Reminder method completed. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Timesheet pre reminder completed successfully.");
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to send notification for timesheet Pre Reminder . Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/timesheet/timesheetPostReminder")
    public ResponseEntity<Object> timesheetPostReminderScheduler(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Timesheet Post Reminder method. ");
        try {
            List<Long> organizationList = new ArrayList<>();
            //TODO: need to take organizationList form officeHourRepository in future
//            List<Long> organizationList = officeHoursRepository.findOrgIdAccordingToOfficeHoursForPostTimeSheetReminder();
            if(LocalTime.now().isAfter(com.tse.core_application.model.Constants.ConstantForGettingOfficeHourEndTimeForOrg.afterOfficeEndTime)
                    && LocalTime.now().isBefore(com.tse.core_application.model.Constants.ConstantForGettingOfficeHourEndTimeForOrg.afterOfficeEndTimeLimit)
                    && calendarDaysRepository.findIsBusinessDayByCalendarDate(LocalDate.now())){ //need to replace this also
                organizationList.addAll(organizationRepository.findAllOrgId());
            }
            for(Long orgId: organizationList) {
                List<Long> accountIdList = userAccountRepository.findAccountIdForTimeSheetReminderAndIsActive(orgId, true);
                if (accountIdList.size() > 0) {
                    List<HashMap<String, String>> payloads = schedulingService.timesheetReminder(accountIdList,orgId,true);
//            pass this payload to fcm for notification
                    taskServiceImpl.sendPushNotification(payloads);
                }
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Timesheet Post Reminder method completed. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Timesheet pre reminder completed successfully.");
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to send notification for Timesheet Post Reminder. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/meeting/startMeetingConfirmation")
    public ResponseEntity<Object> startMeetingConfirmation(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Start Meeting Confirmation method.");

        try {
            List<Meeting> meetingList = meetingRepository.findStartedMeetings();
            if (meetingList.size()>0) {
//            meeting service
                List<HashMap<String, String>> payloads = schedulingService.startMeetingConfirmation(meetingList);
//            pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(payloads);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Start Meeting Confirmation method completed. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Start Meeting Confirmation send successfully");
            }
            else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Start Meeting Confirmation controller completed with no meeting scheduled currently.");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No Start Meeting Confirmation to send currently.");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Start Meeting Confirmation could not be send. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/meeting/endMeetingConfirmation")
    public ResponseEntity<Object> endMeetingConfirmation(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered End Meeting Confirmation method.");

        try {
            List<Meeting> meetingList = meetingRepository.findEndedMeetings();
            if (meetingList.size()>0) {
//            meeting service
                List<HashMap<String, String>> payloads = schedulingService.endMeetingConfirmation(meetingList);
//            pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(payloads);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("End Meeting Confirmation method completed. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "End Meeting Confirmation send successfully");
            }
            else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("End Meeting Confirmation controller completed with no meeting scheduled currently.");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No End Meeting Confirmation to send currently.");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("End Meeting Confirmation could not be send. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/timesheet/timesheetBeforeOfficeReminder")
    public ResponseEntity<Object> timesheetBeforeOfficeReminderScheduler(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Timesheet Before Office Reminder method. ");
        try {
            List<Long> organizationList = new ArrayList<>();
            //TODO: need to take organizationList form officeHourRepository in future
//            List<Long> organizationList = officeHoursRepository.findOrgIdAccordingToOfficeHoursForPostTimeSheetReminder();
            if(LocalTime.now().isAfter(com.tse.core_application.model.Constants.ConstantForGettingOfficeHourEndTimeForOrg.beforeOfficeStartTime)
                    && LocalTime.now().isBefore(com.tse.core_application.model.Constants.ConstantForGettingOfficeHourEndTimeForOrg.beforeOfficeStartTimeLimit) //need to replace this method also
                    && calendarDaysRepository.findIsBusinessDayByCalendarDate(LocalDate.now().minusDays(1))){
                organizationList.addAll(organizationRepository.findAllOrgId());
            }
            for(Long orgId: organizationList) {
                List<Long> accountIdList = userAccountRepository.findAccountIdForTimeSheetReminderNextDayAndIsActive(orgId, true);
                if (accountIdList.size() > 0) {
                    List<HashMap<String, String>> payloads = schedulingService.timesheetReminder(accountIdList,orgId,false);
//            pass this payload to fcm for notification
                    taskServiceImpl.sendPushNotification(payloads);
                }
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Timesheet Before Office Reminder method completed. ");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Timesheet Before Office reminder completed successfully.");
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to send notification for Timesheet Before Office Reminder. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @Transactional
    @PostMapping("/notification/deleteOldNotifications")
    public ResponseEntity<Object> deleteOldNotifications(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered Delete Old Notifications method.");

        try {
            List<Notification> notificationList = notificationRepository.findOldNotifications();
            if (notificationList.size()>0) {
                schedulingService.deleteOldNotifications(notificationList);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Delete Old Notifications method completed. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Old notifications deleted successfully. ");
            }
            else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Delete Old Notifications method completed with no meeting scheduled currently.");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No old notifications to delete today. ");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Delete Old Notifications could not be send. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @Transactional
    @PostMapping("/blockedTask/blockedTaskReminder")
    public ResponseEntity<Object> blockedTaskReminderScheduler() {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered blockedTaskReminderScheduler method.");

        try {
            List<Task> taskList = schedulingService.getTaskListForReminder();
            if (!taskList.isEmpty()) {
//            Task Services
                List<HashMap<String, String>> payloads = schedulingService.createPayloadForTaskReminder(taskList);
//            pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(payloads);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited the BlockedTaskReminderScheduler method, and it has completed successfully.");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Reminder sent successfully");
            } else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited the BlockedTaskReminderScheduler method without scheduling reminders");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No blocked task reminders to send currently.");
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Blocked Task reminder could not be send. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }

    }

    @Transactional
    @PostMapping("/timesheet/fillHolidaysTimesheet")
    public ResponseEntity<Object> fillHolidaysTimesheet () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered fillHolidaysInTimesheet method.");
        try {
            schedulingService.addHolidaysTimesheetForAllEntity();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the fillHolidaysInTimesheet method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Holidays added for the upcoming week");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to add holiday to timesheet. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/leave/leaveRemainingMonthlyUpdate")
    public ResponseEntity<Object> leaveRemainingMonthlyUpdate(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered leave remaining monthly update method. ");
        try {
            List<LeaveRemaining> leaveRemainingList = leaveRemainingRepository.findByCurrentlyActive(true);
            if (leaveRemainingList.size()>0) {
                schedulingService.leaveRemainingMonthlyUpdate(leaveRemainingList);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Leave remaining monthly update method completed. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "leaveRemainingMonthlyUpdate successfully.");
            }
            else {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Leave remaining monthly update controller completed with no leaveRemaining updated. ");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "No Leave Remaining updated.");
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Leave remaining could not be updated. Caught Exception: "+e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/reminder/userReminder")
    public ResponseEntity<Object> userReminderScheduler(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered user reminder method. ");
        try {
            schedulingService.sendReminder();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the userReminderScheduler method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Reminder sent successfully");

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("User reminder could not be send. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }
    @Transactional
    @PostMapping("/alert/dependencyAlert")
    public ResponseEntity<Object> sendDependencyAlert () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered sendDependencyAlert method.");
        try {
            schedulingService.sendDependencyAlert();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the sendDependencyAlert method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Dependency alert sent successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to send dependency alert. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @Transactional
    @PostMapping("/leave/expireLeaveApplications")
    public ResponseEntity<Object> expireLeaveApplications () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered expireLeaveApplications method.");
        try {
            schedulingService.expireLeaves();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the expireLeaveApplications method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Leaves expired successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to expire leaves. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/leave/sendLeaveApprovalReminder")
    public ResponseEntity<Object> sendLeaveApprovalReminder () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered sendLeaveApprovalReminder method.");
        try {
            schedulingService.sendLeaveApprovalReminder();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the sendLeaveApprovalReminder method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Unapproved leaves notification sent successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to send notification for leaves. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/leave/changeLeaveStatusToConsumed")
    public ResponseEntity<Object> changeLeaveStatusToConsumed () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered changeLeaveStatusToConsumed method.");
        try {
            schedulingService.changeLeaveStatusToConsumed();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the changeLeaveStatusToConsumed method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Approved leaves status changed to consumed successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to change status for leaves. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    //This is temporary fox for 8730
    @PostMapping("/sprint/sendSprintTasksMail")
    public ResponseEntity<Object> sendSprintTasksMail () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered sendSprintTasksMail method.");
        try {
            schedulingService.sendTasksMail();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the sendSprintTasksMail method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Sprints/Tasks analysed successfully.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to send sprint tasks mail. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    /**
     * Scheduled endpoint to notify users before shift start.
     * Runs every minute via @Scheduled annotation in AttendanceSchedulerService.
     */
    @PostMapping("/geo-fencing/notifyBeforeShiftStart")
    @Operation(summary = "Notify users before shift start",
            description = "Internal scheduler endpoint to notify users before their shift starts")
    public ResponseEntity<Object> notifyBeforeShiftStart() {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered notifyBeforeShiftStart method.");
        try {
            schedulingService.processNotifyBeforeShiftStart();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the notifyBeforeShiftStart method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Notification scheduler completed successfully.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    /**
     * Scheduled endpoint to auto-checkout users who missed checkout.
     * Runs every minute via @Scheduled annotation in AttendanceSchedulerService.
     */
    @PostMapping("/geo-fencing/autoCheckout")
    @Operation(summary = "Auto-checkout users after maxCheckoutAfterEndMin",
            description = "Internal scheduler endpoint to mark users as checked out after grace period")
    public ResponseEntity<Object> autoCheckout() {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered autoCheckout method.");
        try {
            schedulingService.processAutoCheckout();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the autoCheckout method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Auto-checkout scheduler completed successfully.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error in autoCheckout scheduler: " + e.getMessage(), e);
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/geo-fencing/missedPunch")
    @Operation(summary = "Auto-checkout users after maxCheckoutAfterEndMin",
            description = "Internal scheduler endpoint to mark users as checked out after grace period")
    public ResponseEntity<Object> missedPunch() {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered missedPunch method.");
        try {
            schedulingService.processMissedPunches();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the missedPunch method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Missed punch scheduler completed successfully.");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error in missedPunch scheduler: " + e.getMessage(), e);
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }


    @PostMapping("/ai/retryFailedUserRegistration")
    @Operation(summary = "Retrying the User Registration into the AI service",
            description = "Internal scheduler endpoint to mark users as checked out after grace period")
    public ResponseEntity<Object> retryFailedUserRegistration() {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered retryFailedUserRegistration method.");
        try {
            schedulingService.retryFailedUserRegistration();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the retryFailedUserRegistration method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Failed Registration run successfully!");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Error in retryFailedUserRegistration scheduler: " + e.getMessage(), e);
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }

    @PostMapping("/alert/deleteAlerts")
    public ResponseEntity<Object> deleteAlerts () {
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered deleteAlerts method.");
        try {
            schedulingService.deleteAlerts();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the deleteAlerts method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Alerts deleted successfully");
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Failed to delete alert. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }
    @PostMapping("/notification/expireLeaveApplicationsNotifications")
    public ResponseEntity<Object> notificationScheduler(){
        long startTime = System.currentTimeMillis();
        ThreadContext.put("accountId", String.valueOf(0));
        ThreadContext.put("userId", String.valueOf(0));
        logger.info("Entered notification scheduler method. ");
        try {
            schedulingService.expiredLeaveApplicationNotification();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited the Notification Scheduler method, and it has completed successfully.");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Notification sent successfully");

        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification could not be send. Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.FormattedResponse.SERVER_ERROR, "ERROR!");
        }
    }
}
