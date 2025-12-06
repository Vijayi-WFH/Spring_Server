package com.tse.core_application.service.Impl;

import com.tse.core_application.config.DebugConfig;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.EntityPreferenceRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.service.IEMailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class EmailService implements IEMailService {

    private static final Logger logger = LogManager.getLogger(EmailService.class.getName());

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${system.admin.email}")
    private String adminEmail;

    @Value("${application.domain}")
    private String baseDomain;

    @Value("${app.environment}")
    private Integer environment;

    @Override
    public String sendOtp(String to, String otp, String subject, String ownerEmail, Boolean isRegister) {
        try {
            // Fetch user name
            String emailContent = createBodyForOtpEmail(to, otp);

            // Create MimeMessage
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setBcc(adminEmail);
            helper.setSubject(subject);
            helper.setFrom(username);
            helper.setText(emailContent, true); // true = HTML content

            List<String> orgAdminsEmailList = new ArrayList<>();
            if (isRegister) {
                if (ownerEmail != null) {
                    List<Long> orgIdList = userAccountRepository.findAllOrgIdByEmailAndIsActive(to, true);
                    if (orgIdList == null || orgIdList.isEmpty()) {
                        orgAdminsEmailList.add(ownerEmail);
                        helper.setCc(orgAdminsEmailList.toArray(new String[0]));
                    }
                }
            } else {
                getAllOrgAdminsOfUserOrg(to, orgAdminsEmailList, true);
                if (orgAdminsEmailList != null && !orgAdminsEmailList.isEmpty()) {
                    orgAdminsEmailList.remove(to);
                    if (!orgAdminsEmailList.isEmpty()) {
                        helper.setCc(orgAdminsEmailList.toArray(new String[0]));
                    }
                }
            }

            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("From try of EmailService.sendOtp() otp sent was =  " + otp);
            }

            emailSender.send(mimeMessage);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Not able to send OTP", e);
            ThreadContext.clearMap();
            return e.toString();
        }

        return Constants.SUCCESS;
    }

    public String createBodyForOtpEmail (String to, String otp) {
        List<UserAccount> userAccountList = userAccountRepository.findByEmail(to);

        UserAccount userAccount = null;
        if (userAccountList != null && !userAccountList.isEmpty()) {
            userAccount = userAccountList.get(0);
        }

        String firstName = userAccount != null ? userAccount.getFkUserId().getFirstName() : to;
        String lastName = userAccount != null ? userAccount.getFkUserId().getLastName() : "";

        // Determine environment label
        String envIndicator = null;
        if (Objects.equals(com.tse.core_application.model.Constants.Environment.PRE_PROD.getTypeId(), environment)) {
            envIndicator = "[Environment: Pre-Prod]";
        } else if (Objects.equals(com.tse.core_application.model.Constants.Environment.QA.getTypeId(), environment)) {
            envIndicator = "[Environment: QA]";
        }

        // Build HTML content using Thymeleaf
        return buildOtpEmailContent(otp, firstName, lastName, envIndicator);
    }

    @Override
    public String sendBlockedTaskReminder(String to, String text, String taskNumber) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Work Item Blocked Remainder");
        message.setText(text);
        message.setFrom(username);
        try {
            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("Message sent to " + to);
            }
            emailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Not able to send message", e);
            ThreadContext.clearMap();
            return e.toString();
        }
        return Constants.SUCCESS;
    }
    /** method to send email to invite user to a new organization*/
    public void sendInviteEmail(String to, String subject, String content, String ownerEmail) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // 'true' indicates content is HTML

            if (ownerEmail != null) {
                helper.setCc(ownerEmail);
            }

            emailSender.send(message);
        } catch (Exception e) {
            // Handle exceptions (e.g., Email sending failure)
        }
    }

    @Override
    public void sendFailedLeaveRemainingUpdateNotification(String to, String firstName, String orgName, String accounts) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("orgName", orgName);
            context.setVariable("accounts", accounts);

            String content = templateEngine.process("leavesFailureTemplate", context);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Unable to update leaves quota");
            helper.setText(content, true); // 'true' indicates content is HTML

            emailSender.send(message);
        } catch (Exception e) {
            // Handle exceptions (e.g., Email sending failure)
        }
    }

    //temporary fix for 8730
    @Override
    public void sendTasksMailToAdmin(String to, String firstName, String failedTasks) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("failedTasks", failedTasks);

            String content = templateEngine.process("sprintTasksFailTemplate", context);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Error in Sprint and Work Item Date Time");
            helper.setText(content, true); // 'true' indicates content is HTML

            emailSender.send(message);
        } catch (Exception e) {
            // Handle exceptions (e.g., Email sending failure)
        }
    }

    public void getAllOrgAdminsOfUserOrg (String email, List<String> orgAdminsEmailList, Boolean isOtp) {
        List<Long> orgIdList = userAccountRepository.findAllOrgIdByEmailAndIsActive(email, true);
        if (orgIdList != null && !orgIdList.isEmpty()) {
            if (orgIdList.size() != 1) {
                return;
            }
            List<Long> orgIdListToSendOtpOrInvite = null;
            if (isOtp) {
                orgIdListToSendOtpOrInvite = entityPreferenceRepository.findEntityIdsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin (com.tse.core_application.model.Constants.EntityTypes.ORG, orgIdList, true);
            }
            else {
                orgIdListToSendOtpOrInvite = entityPreferenceRepository.findEntityIdsByEntityTypeIdAndEntityIdInAndShouldInviteLinkSendToOrgAdmin (com.tse.core_application.model.Constants.EntityTypes.ORG, orgIdList, true);
            }
            if (orgIdListToSendOtpOrInvite != null && !orgIdListToSendOtpOrInvite.isEmpty()) {
                List<Long> orgAdminAccountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.ORG, orgIdListToSendOtpOrInvite, List.of(RoleEnum.ORG_ADMIN.getRoleId()), true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
                orgAdminsEmailList.addAll(userAccountRepository.findDistinctFkUserIdEmailByAccountIdInAndIsActive(orgAdminAccountIdList, true));
            }
        }
    }

    public void sendVerificationEmail(String toEmail, String firstName, Long orgId, String orgName) {
        String subject = "Verify Your Account for " + orgName;

        // Construct verification link
        String verificationLink = baseDomain + com.tse.core_application.model.Constants.InviteBaseDomain.VERIFY_DOMAIN + "?orgId=" + orgId + "&orgName=" + URLEncoder.encode(orgName, StandardCharsets.UTF_8) + "&email=" + URLEncoder.encode(toEmail, StandardCharsets.UTF_8);

        // Prepare Thymeleaf context
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("orgName", orgName);
        context.setVariable("verificationLink", verificationLink);

        // Generate HTML content using template
        String content = templateEngine.process("verifyEmailTemplate", context);

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            emailSender.send(message);
        } catch (Exception e) {
            // Log error or rethrow for handling
            logger.error("Failed to send verification email to " + toEmail, e);
        }
    }

    private String buildOtpEmailContent(String otp, String firstName, String lastName, String envIndicator) {
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("firstName", firstName);
        context.setVariable("lastName", lastName);
        context.setVariable("envIndicator", envIndicator);

        return templateEngine.process("otpEmailTemplate", context);
    }

    @Override
    public void sendDeclinedEmailOfJiraUserImport(String organizationName, UserAccount userAccountOfOrgAdmin, UserAccount userAccountOfUser) {
        try {
            String orgAdminEmail = userAccountOfOrgAdmin.getFkUserId().getPrimaryEmail();
            String firstNameOfOrgAdmin = userAccountOfOrgAdmin.getFkUserId().getFirstName();
            String nameOfUserDeclinedInvite = userAccountOfUser.getFkUserId().getFirstName() + " " + userAccountOfUser.getFkUserId().getLastName();

            String emailContent = createBodyForDeclinedInviteEmail(firstNameOfOrgAdmin, nameOfUserDeclinedInvite, organizationName);

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(orgAdminEmail);
            helper.setSubject("User Declined Organization Invitation");
            helper.setFrom(username);
            helper.setText(emailContent, true);

            emailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("Failed to send declined invitation email", e);
            ThreadContext.clearMap();
        }
    }

    private String createBodyForDeclinedInviteEmail(String orgAdminFirstName, String declinedUserFullName, String organizationName) {
        Context context = new Context();
        context.setVariable("orgAdminFirstName", orgAdminFirstName);
        context.setVariable("declinedUserFullName", declinedUserFullName);
        context.setVariable("organizationName", organizationName);

        return templateEngine.process("declinedInviteEmailTemplate", context);
    }

    public void sendOrgDeletionRequestedEmail(String toEmail, String organizationName, java.sql.Timestamp scheduledDeletionDate, int gracePeriodDays) {
        try {
            String emailContent = createBodyForOrgDeletionRequestedEmail(organizationName, scheduledDeletionDate, gracePeriodDays);

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Organization Deletion Request - " + organizationName);
            helper.setFrom(username);
            helper.setText(emailContent, true);
            helper.setBcc(adminEmail);

            emailSender.send(mimeMessage);
            logger.info("Org deletion requested email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send org deletion requested email to: {}", toEmail, e);
            ThreadContext.clearMap();
        }
    }

    private String createBodyForOrgDeletionRequestedEmail(String organizationName, java.sql.Timestamp scheduledDeletionDate, int gracePeriodDays) {
        Context context = new Context();
        context.setVariable("organizationName", organizationName);
        context.setVariable("scheduledDeletionDate", scheduledDeletionDate.toLocalDateTime().toLocalDate().toString());
        context.setVariable("gracePeriodDays", gracePeriodDays);
        context.setVariable("envIndicator", getEnvIndicator());

        return templateEngine.process("orgDeletionRequestedTemplate", context);
    }

    public void sendOrgDeletionReversedEmail(String toEmail, String organizationName) {
        try {
            String emailContent = createBodyForOrgDeletionReversedEmail(organizationName);

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Organization Deletion Reversed - " + organizationName);
            helper.setFrom(username);
            helper.setText(emailContent, true);
            helper.setBcc(adminEmail);

            emailSender.send(mimeMessage);
            logger.info("Org deletion reversed email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send org deletion reversed email to: {}", toEmail, e);
            ThreadContext.clearMap();
        }
    }

    private String createBodyForOrgDeletionReversedEmail(String organizationName) {
        Context context = new Context();
        context.setVariable("organizationName", organizationName);
        context.setVariable("envIndicator", getEnvIndicator());

        return templateEngine.process("orgDeletionReversedTemplate", context);
    }

    public void sendOrgDeletionCompletedEmail(String toEmail, String organizationName, com.tse.core_application.model.DeletedOrganizationStats stats) {
        try {
            String emailContent = createBodyForOrgDeletionCompletedEmail(organizationName, stats);

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Organization Deletion Completed - " + organizationName);
            helper.setFrom(username);
            helper.setText(emailContent, true);
            helper.setBcc(adminEmail);

            emailSender.send(mimeMessage);
            logger.info("Org deletion completed email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send org deletion completed email to: {}", toEmail, e);
            ThreadContext.clearMap();
        }
    }

    private String createBodyForOrgDeletionCompletedEmail(String organizationName, com.tse.core_application.model.DeletedOrganizationStats stats) {
        Context context = new Context();
        context.setVariable("organizationName", organizationName);
        context.setVariable("buCount", stats.getBuCount());
        context.setVariable("projectCount", stats.getProjectCount());
        context.setVariable("teamCount", stats.getTeamCount());
        context.setVariable("totalUserCount", stats.getTotalUserCount());
        context.setVariable("activeUserCount", stats.getActiveUserCount());
        context.setVariable("inactiveUserCount", stats.getInactiveUserCount());
        context.setVariable("taskCount", stats.getTaskCount());
        context.setVariable("deletedProjectsCount", stats.getDeletedProjectsCount());
        context.setVariable("deletedTeamsCount", stats.getDeletedTeamsCount());
        context.setVariable("envIndicator", getEnvIndicator());

        return templateEngine.process("orgDeletionCompletedTemplate", context);
    }

    public void sendOrgDeletionFailedEmail(String toEmail, Long orgId, String organizationName, String errorMessage) {
        try {
            String emailContent = createBodyForOrgDeletionFailedEmail(orgId, organizationName, errorMessage);

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("ALERT: Organization Deletion Failed - " + organizationName);
            helper.setFrom(username);
            helper.setText(emailContent, true);

            emailSender.send(mimeMessage);
            logger.info("Org deletion failed email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send org deletion failed email to: {}", toEmail, e);
            ThreadContext.clearMap();
        }
    }

    private String createBodyForOrgDeletionFailedEmail(Long orgId, String organizationName, String errorMessage) {
        Context context = new Context();
        context.setVariable("orgId", orgId);
        context.setVariable("organizationName", organizationName);
        context.setVariable("errorMessage", errorMessage);
        context.setVariable("timestamp", java.time.LocalDateTime.now().toString());
        context.setVariable("envIndicator", getEnvIndicator());

        return templateEngine.process("orgDeletionFailedTemplate", context);
    }

    private String getEnvIndicator() {
        if (Objects.equals(com.tse.core_application.model.Constants.Environment.PRE_PROD.getTypeId(), environment)) {
            return "[Environment: Pre-Prod]";
        } else if (Objects.equals(com.tse.core_application.model.Constants.Environment.QA.getTypeId(), environment)) {
            return "[Environment: QA]";
        }
        return null;
    }

}
