package com.tse.core_application.service;

//import reactor.core.publisher.Mono;

import com.tse.core_application.model.Task;
import com.tse.core_application.model.UserAccount;

public interface IEMailService {

	String sendOtp(String to, String msg, String subject, String ownerEmail, Boolean isRegister);

	String sendBlockedTaskReminder(String to, String text, String taskNumber);

	void sendFailedLeaveRemainingUpdateNotification(String to, String firstName, String orgName, String accounts);

	void sendTasksMailToAdmin(String to, String firstName, String failedTasks);

	void sendVerificationEmail(String toEmail, String firstName, Long orgId, String orgName);

    void sendDeclinedEmailOfJiraUserImport(String organizationName, UserAccount userAccountOfOrgAdmin, UserAccount userAccount);
}
