package com.tse.core_application.service;

//import reactor.core.publisher.Mono;

import com.tse.core_application.model.Task;
import com.tse.core_application.model.UserAccount;

import java.time.LocalDate;

public interface IEMailService {

	String sendOtp(String to, String msg, String subject, String ownerEmail, Boolean isRegister);

	String sendBlockedTaskReminder(String to, String text, String taskNumber);

	void sendFailedLeaveRemainingUpdateNotification(String to, String firstName, String orgName, String accounts);

	void sendTasksMailToAdmin(String to, String firstName, String failedTasks);

	void sendVerificationEmail(String toEmail, String firstName, Long orgId, String orgName);

    void sendDeclinedEmailOfJiraUserImport(String organizationName, UserAccount userAccountOfOrgAdmin, UserAccount userAccount);

    // ==================== Organization Deletion Email Methods ====================

    /**
     * Send email to org admin when deletion is requested.
     * @param toEmail Org admin email
     * @param orgName Organization name
     * @param deletionScheduledDate Date when hard deletion will occur
     */
    void sendOrgDeletionRequestedEmail(String toEmail, String orgName, LocalDate deletionScheduledDate);

    /**
     * Send email to org admin when deletion is reversed by super admin.
     * @param toEmail Org admin email
     * @param orgName Organization name
     */
    void sendOrgDeletionReversedEmail(String toEmail, String orgName);

    /**
     * Send email to org admin when hard deletion is completed.
     * @param toEmail Org admin email
     * @param orgName Organization name
     */
    void sendOrgDeletionCompletedEmail(String toEmail, String orgName);

    /**
     * Send email to system admin when hard deletion fails.
     * @param adminEmail System admin email
     * @param orgName Organization name
     * @param orgId Organization ID
     * @param errorDetails Error message
     */
    void sendOrgDeletionFailedEmail(String adminEmail, String orgName, Long orgId, String errorDetails);
}
