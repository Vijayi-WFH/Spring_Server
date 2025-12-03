package com.tse.scheduling.scheduler;

import com.tse.scheduling.constants.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;

@Component
public class Scheduler {
    private static final Logger logger = LogManager.getLogger(Scheduler.class.getName());

    @Value("${application.root.path}")
    private String rootPath;

    /**
     * Scheduling of meeting reminder notification
     */
    @Scheduled(fixedRate = 60000)
    public void meetingReminderScheduler(){
        try {
            //calling api of core application
            logger.info("Meeting reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.meetingRoot + Constants.reminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Meeting reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in meetingReminderScheduler: "+e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void timesheetPreReminderScheduler(){
        try {
            //calling api of core application
            logger.info("Timesheet Pre Reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.timesheetRoot + Constants.timesheetPreReminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Timesheet Pre Reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in timesheetPreReminderScheduler: "+e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void timesheetPostReminderScheduler(){
        try {
            //calling api of core application
            logger.info("Timesheet Post Reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.timesheetRoot + Constants.timesheetPostReminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Timesheet Post Reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in timesheetPostReminderScheduler: "+e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void timesheetBeforeOfficeReminderScheduler(){
        try {
            //calling api of core application
            logger.info("Timesheet Before Office Reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.timesheetRoot + Constants.timesheetBeforeOfficeReminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Timesheet Before Office Reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in timesheetBeforeOfficeReminderScheduler: "+e);
        }
    }

    @Scheduled(cron = "${leave.remaining.reset.time}")
    public void leaveRemainingReset(){
        try {
            //calling api of core application
            logger.info("Leave remaining reset started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.leaveRoot + Constants.getLeaveRemainingReset;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Leave remaining reset completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in leaveRemainingReset: "+e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void startMeetingConfirmation(){
        try {
            //calling api of core application
            logger.info("Start Meeting Confirmation scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.meetingRoot + Constants.getStartMeetingConfirmation;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Start Meeting Confirmation scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in startMeetingConfirmation: "+e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void endMeetingConfirmation(){
        try {
            //calling api of core application
            logger.info("End Meeting Confirmation scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.meetingRoot + Constants.getEndMeetingConfirmation;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("End Meeting Confirmation scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in endMeetingConfirmation: "+e);
        }
    }

    @Scheduled(cron = "${old.notification.deletion}")
    public void deleteOldNotificationsScheduler(){
        try {
            //calling api of core application
            logger.info("Delete old notification scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.notificationRoot + Constants.deleteOldNotifications;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Delete old notification scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in deleteOldNotificationsScheduler: "+e);
        }
    }

    /**
     * Scheduling of blocked task reminder notification
     */
    @Scheduled(cron = "0 0 11 * * ?")
    public void blockedTaskReminderScheduler(){
        try {
            //calling api of core application
            logger.info("Blocked Task reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.blockedTaskRoot + Constants.getBlockedTaskReminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Blocked Task reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in blockedTaskReminderScheduler: "+e);
        }
    }

    @Scheduled(cron = "0 0 12 * * SUN") //runs every sunday at 12 noon
    public void holidaysTimesheetScheduler(){
        try {
            //calling api of core application
            logger.info("Holidays scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.timesheetRoot + Constants.fillHolidaysTimesheet;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Holidays scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in holidaysScheduler: "+e);
        }
    }

    @Scheduled(cron = "59 58 23 L * ?")
    public void leaveRemainingMonthlyUpdate(){
        try {
            //calling api of core application
            logger.info("Leave remaining monthly update started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.leaveRoot + Constants.getLeaveRemainingMonthlyUpdate;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Leave remaining monthly update completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in leaveRemainingMonthlyUpdate: "+e);
        }
    }

    /**
     * Scheduling of dependency task alert notification
     */
    @Scheduled(cron = "0 0 11 * * ?") //runs daily at 11 AM
    public void dependencyAlertSchedular(){
        try {
            //calling api of core application
            logger.info("Dependent Task alert scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.alertRoot + Constants.dependencyAlert;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Dependent Task alert scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in dependencyAlertSchedular: "+e);
        }
    }
    @Scheduled(fixedRate = 60000)
    public void userReminderScheduler(){
        try {
            //calling api of core application
            logger.info("User Reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.reminder + Constants.userReminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("User Reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in userReminderScheduler: "+e);
        }
    }

    @Scheduled(cron = "59 58 23 * * ?")
    public void expireLeaveApplicationsSchedular(){
        try {
            //calling api of core application
            logger.info("User Reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.leaveRoot + Constants.expireLeaveApplications;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("User Reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in userReminderScheduler: "+e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void leaveApprovalReminderSchedular(){
        try {
            //calling api of core application
            logger.info("User Reminder scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.leaveRoot + Constants.sendLeaveApprovalReminder;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("User Reminder scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in userReminderScheduler: "+e);
        }
    }

    @Scheduled(cron = "0 0 03 * * ?")
    public void changeLeaveStatusToConsumed(){
        try {
            //calling api of core application
            logger.info("Change leave status to consumed scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.leaveRoot + Constants.changeLeaveStatusToConsumed;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Changed leave status to consumed scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in changeLeaveStatusToConsumed: "+e);
        }
    }

    //This is temporary fox for 8730
    @Scheduled(fixedRate = 3600000)
    public void sendSprintTasksMail(){
        try {
            //calling api of core application
            logger.info("Send sprint mail scheduler started at "+LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.sprintRoute + Constants.sendSprintTasksMail;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Send sprint mail scheduler completed at "+LocalDateTime.now()+ " with response "+response);
        }
        catch (Exception e){
            logger.error(LocalDateTime.now()+". Caught error in sendSprintTasksMail: "+e);
        }
    }

    /**
     * Scheduled job: Notify users before shift start.
     * Runs every 1 minute.
     */
    @Scheduled(cron = "0 * * * * ?")  // Every minute at 0 seconds
    public void notifyBeforeShiftStartScheduler() {
        try {
            logger.info("Notify before shift start scheduler started at " + LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.geoFenceRoute + Constants.notifyBeforeShiftStart;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Notify before shift start scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in notifyBeforeShiftStartScheduler: " + e.getMessage(), e);
        }
    }

    /**
     * Scheduled job: Auto-checkout users who missed checkout.
     * Runs every 1 minute.
     */
    @Scheduled(cron = "0 * * * * ?")  // Every minute at 0 seconds
    public void autoCheckoutScheduler() {
        try {
            logger.info("Auto-checkout scheduler started at " + LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.geoFenceRoute + Constants.autoCheckout;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Auto-checkout scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in autoCheckoutScheduler: " + e.getMessage(), e);
        }
    }

    /**
     * Scheduled job: Mark missed punches for expired punch requests.
     * Runs every 1 minute.
     */
    @Scheduled(cron = "0 * * * * ?")  // Every minute at 0 seconds
    public void missedPunchScheduler() {
        try {
            logger.info("Missed punch scheduler started at " + LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.geoFenceRoute + Constants.missedPunch;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Missed punch scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in missedPunchScheduler: " + e.getMessage(), e);
        }
    }

    @Scheduled(cron = " 0 0/30 * * * * ")  // Every 30 mins
    public void retryFailedAiRegistration() {
        try {
            logger.info("Retry FailedAiRegistration scheduler started at " + LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.retryFailedAiRegistration;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Retry FailedAiRegistration scheduler completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in missedPunchScheduler: " + e.getMessage(), e);
        }
    }

    @Scheduled(cron = "59 50 23 * * ?")
    public void deleteAlerts() {
        try {
            logger.info("Delete alerts started at " + LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.alertRoot + Constants.deleteAlerts;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response= restTemplate.exchange(uri, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {});
            logger.info("Delete alerts completed at " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in deleteAlerts: " + e.getMessage(), e);
        }
    }
    @Scheduled(cron = "0 0 12 * * ?")
    public void expiredLeaveApplicationNotificationScheduler() {
        try {
            logger.info("Notification scheduler started at " + LocalDateTime.now());
            RestTemplate restTemplate = new RestTemplate();
            String uri = rootPath + Constants.notificationRoot + Constants.expireLeaveApplicationsNotifications;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<String>() {}
            );
            logger.info("Notification scheduler completed at " + LocalDateTime.now() + " with response " + response);
        } catch (Exception e) {
            logger.error(LocalDateTime.now() + ". Caught error in expiredLeaveApplicationNotificationScheduler: " + e);
        }
    }
}
