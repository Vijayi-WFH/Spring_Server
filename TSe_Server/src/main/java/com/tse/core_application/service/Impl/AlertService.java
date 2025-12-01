package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.ProjectIdProjectName;
import com.tse.core_application.custom.model.TeamIdAndTeamName;
import com.tse.core_application.dto.AlertRequest;
import com.tse.core_application.dto.AlertResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {

    private static final Logger logger = LogManager.getLogger(AlertService.class.getName());

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private NotificationService notificationService;


    /**
     * This api adds alert for users
     */
    public AlertResponse addAlert (AlertRequest alertRequest, String accountIds, String timeZone) {
        validateUser(alertRequest, accountIds);
        Alert alert = new Alert();
        if (!Constants.AlertTypeList.contains(alertRequest.getAlertType())) {
            throw new IllegalStateException("Invalid Alert Type: " + alertRequest.getAlertType() + ". The provided alert type is not supported.");
        }
        UserAccount userAccountReceiver = userAccountRepository.findByAccountIdAndIsActive(alertRequest.getAccountIdReceiver(), true);
        UserAccount userAccountSender = userAccountRepository.findByAccountIdAndIsActive(alertRequest.getAccountIdSender(), true);
        if (userAccountReceiver == null) {
            logger.error("Receiver account is not present/active ");
            return new AlertResponse();
        }
        if (Objects.equals(alertRequest.getAlertType(), Constants.AlertTypeEnum.USER.getType())) {
            alertRequest.setAlertTitle("User " + userAccountSender.getFkUserId().getFirstName() + " " + userAccountSender.getFkUserId().getLastName() + " need attention from you.");
        }
        CommonUtils.copyNonNullProperties(alertRequest, alert);
        Team team = teamRepository.findByTeamId(alertRequest.getTeamId());
        Project project = projectRepository.findByProjectId(alertRequest.getProjectId());
        Organization organization = organizationRepository.findByOrgId(alertRequest.getOrgId());
        alert.setFkOrgId(organization);
        alert.setFkProjectId(project);
        alert.setFkTeamId(team);
        if (userAccountSender != null) {
            alert.setFkAccountIdSender(userAccountSender);
        }
        alert.setFkAccountIdReceiver(userAccountReceiver);
        alert.setAlertStatus(Constants.AlertStatusEnum.UNVIEWED.getStatus());
        alertRepository.save(alert);
        notificationService.immediateAttentionNotification(alert, timeZone,accountIds);
        return getAlertResponse(alert, timeZone);
    }

    /**
     * This api generate response for alert
     */
    public AlertResponse getAlertResponse(Alert alert, String timeZone) {
        AlertResponse alertResponse = new AlertResponse();
        alertResponse.setAlertId(alert.getAlertId());
        alertResponse.setAlertReason(alert.getAlertReason());
        alertResponse.setAlertStatus(alert.getAlertStatus());
        alertResponse.setAlertTitle(alert.getAlertTitle());
        alertResponse.setAlertType(alert.getAlertType());
        alertResponse.setAssociatedTaskId(alert.getAssociatedTaskId());
        alertResponse.setAssociatedTaskNumber(alert.getAssociatedTaskNumber());
        TeamIdAndTeamName teamDetails = new TeamIdAndTeamName(alert.getFkTeamId().getTeamId(), alert.getFkTeamId().getTeamName(),alert.getFkTeamId().getTeamCode(), alert.getFkTeamId().getIsDeleted());
        OrgIdOrgName orgDetails = new OrgIdOrgName(alert.getFkOrgId().getOrgId(), alert.getFkOrgId().getOrganizationName());
        ProjectIdProjectName projectDetails = new ProjectIdProjectName(alert.getFkProjectId().getProjectId(), alert.getFkProjectId().getProjectName(), alert.getFkProjectId().getIsDeleted());
        EmailFirstLastAccountId senderDetails = new EmailFirstLastAccountId(alert.getFkAccountIdSender().getEmail(), alert.getFkAccountIdSender().getAccountId(), alert.getFkAccountIdSender().getFkUserId().getFirstName(), alert.getFkAccountIdSender().getFkUserId().getLastName());
        EmailFirstLastAccountId receiverDetails = new EmailFirstLastAccountId(alert.getFkAccountIdReceiver().getEmail(), alert.getFkAccountIdReceiver().getAccountId(), alert.getFkAccountIdReceiver().getFkUserId().getFirstName(), alert.getFkAccountIdReceiver().getFkUserId().getLastName());
        alertResponse.setOrgDetails(orgDetails);
        alertResponse.setProjectDetails(projectDetails);
        alertResponse.setTeamDetails(teamDetails);
        if (!Objects.equals(alert.getAlertType(), Constants.AlertTypeEnum.DEPENDENCY.getType())) {
            alertResponse.setSenderDetails(senderDetails);
        }
        alertResponse.setReceiverDetails(receiverDetails);
        LocalDateTime serverDateTime = DateTimeUtils.convertServerDateToUserTimezone(alert.getCreatedDateTime().toLocalDateTime(), timeZone);
        alertResponse.setCreatedDateTime(serverDateTime);
        return alertResponse;
    }

    /**
     * This method gets all the sent alerts by user
     */
    public List<AlertResponse> getUserSentAlerts (String accountIds, String timeZone) {
        List<AlertResponse> alertResponseList = new ArrayList<>();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Alert> alertList = alertRepository.findAllByFkAccountIdSenderAccountIdInAndIsDeleted(accountIdList, false);
        if (!alertList.isEmpty()) {
            for (Alert alert : alertList) {
                if (Objects.equals(alert.getAlertType(), Constants.AlertTypeEnum.DEPENDENCY.getType())) {
                    continue;
                }
                alertResponseList.add(getAlertResponse(alert, timeZone));
            }
        }
        alertResponseList.sort(Comparator.comparing(AlertResponse::getCreatedDateTime).reversed());
        return alertResponseList;
    }

    /**
     * This api returns all the user received alerts
     */
    public List<AlertResponse> getUserReceivedAlerts (String accountIds, String timeZone) {
        List<AlertResponse> alertResponseList = new ArrayList<>();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Alert> alertList = alertRepository.findAllByFkAccountIdReceiverAccountIdInAndIsDeleted(accountIdList, false);
        if (!alertList.isEmpty()) {
            for (Alert alert : alertList) {
                alertResponseList.add(getAlertResponse(alert, timeZone));
            }
        }
        alertResponseList.sort(Comparator.comparing(AlertResponse::getCreatedDateTime).reversed());
        return alertResponseList;
    }

    /**
     * This method gets the alert with alert id
     */
    public AlertResponse getAlert (Long alertId, String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Optional<Alert> alertOptional = alertRepository.findById(alertId);
        if (!alertOptional.isPresent()) {
            throw new ValidationFailedException("No alert found");
        }
        Alert alert = alertOptional.get();
        if (!accountIdList.contains(alert.getFkAccountIdSender().getAccountId()) && !accountIdList.contains(alert.getFkAccountIdReceiver().getAccountId())) {
            throw new IllegalAccessException("User not authorized to view provided alert");
        }

        return getAlertResponse(alert, timeZone);

    }

    /**
     * This method marks the provided alert as viewed
     */
    public AlertResponse markAlertAsViewed (Long alertId, String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Optional<Alert> alertOptional = alertRepository.findById(alertId);
        if (!alertOptional.isPresent()) {
            throw new ValidationFailedException("No alert found");
        }
        Alert alert = alertOptional.get();
        if (!accountIdList.contains(alert.getFkAccountIdReceiver().getAccountId())) {
            throw new IllegalAccessException("User not authorized to mark provided alert as viewed");
        }
        alert.setAlertStatus(Constants.AlertStatusEnum.VIEWED.getStatus());
        alertRepository.save(alert);
        return getAlertResponse(alert, timeZone);

    }

    /**
     * This method gets the alert for user to view
     */
    public List<AlertResponse> getUserAlertsToView (String accountIds, String timeZone) {
        List<AlertResponse> alertResponseList = new ArrayList<>();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Alert> alertList = alertRepository.findAllByFkAccountIdReceiverAccountIdInAndAlertStatusAndIsDeleted(accountIdList, Constants.AlertStatusEnum.UNVIEWED.getStatus(), false);
        if (!alertList.isEmpty()) {
            for (Alert alert : alertList) {
                alertResponseList.add(getAlertResponse(alert, timeZone));
            }
        }
        alertResponseList.sort(Comparator.comparing(AlertResponse::getCreatedDateTime).reversed());
        return alertResponseList;
    }

    /**
     * This method validtaes the user for alert access
     */
    private void validateUser(AlertRequest request, String accountIds) {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        if (!Objects.equals(request.getAlertType(), Constants.AlertTypeEnum.DEPENDENCY.getType()) && !accountIdList.contains(request.getAccountIdSender())) {
            throw new ValidationFailedException("User not authorized to send alert");
        }

        if (!Objects.equals(request.getAlertType(), Constants.AlertTypeEnum.DEPENDENCY.getType()) && !(accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, request.getTeamId(), request.getAccountIdSender(), true) || accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, request.getTeamId(), request.getAccountIdReceiver(), true))) {
            throw new ValidationFailedException("User no more part of the team provided");
        }
    }
}
