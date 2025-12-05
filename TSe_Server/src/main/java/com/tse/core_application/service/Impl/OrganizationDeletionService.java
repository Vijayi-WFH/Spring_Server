package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.org_deletion.OrgDeletionResponse;
import com.tse.core_application.dto.org_deletion.RequestOrgDeletionRequest;
import com.tse.core_application.dto.org_deletion.ReverseOrgDeletionRequest;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.IOrganizationDeletionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OrganizationDeletionService implements IOrganizationDeletionService {

    private static final Logger logger = LogManager.getLogger(OrganizationDeletionService.class.getName());

    private static final int GRACE_PERIOD_DAYS = 30;
    private static final String DEACTIVATED_ACCOUNTS_KEY = "DEACTIVATED_ACCOUNTS";

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private BURepository buRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TimeSheetRepository timeSheetRepository;

    @Autowired
    private StickyNoteRepository stickyNoteRepository;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private LeaveRemainingRepository leaveRemainingRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private DeletedOrganizationStatsRepository deletedOrganizationStatsRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmailService emailService;

    @Value("${conversation.application.root.path}")
    private String conversationBaseUrl;

    @Value("${system.admin.email}")
    private String systemAdminEmail;

    @Override
    @Transactional
    public OrgDeletionResponse requestOrganizationDeletion(RequestOrgDeletionRequest request, Long requesterAccountId) {
        logger.info("Processing organization deletion request for orgId: {}", request.getOrgId());

        Organization organization = organizationRepository.findByOrgId(request.getOrgId());
        if (organization == null) {
            logger.error("Organization not found with id: {}", request.getOrgId());
            return OrgDeletionResponse.builder()
                    .orgId(request.getOrgId())
                    .status("FAILED")
                    .message("Organization not found")
                    .build();
        }

        if (Boolean.TRUE.equals(organization.getIsDeletionRequested())) {
            logger.warn("Organization {} is already scheduled for deletion", request.getOrgId());
            return OrgDeletionResponse.builder()
                    .orgId(request.getOrgId())
                    .organizationName(organization.getOrganizationName())
                    .status("ALREADY_PENDING")
                    .message("Organization is already scheduled for deletion")
                    .scheduledDeletionDate(organization.getScheduledDeletionDate())
                    .gracePeriodDays(GRACE_PERIOD_DAYS)
                    .build();
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp scheduledDeletionDate = Timestamp.valueOf(LocalDateTime.now().plusDays(GRACE_PERIOD_DAYS));

        organizationRepository.updateDeletionRequestFields(
                request.getOrgId(),
                true,
                now,
                requesterAccountId,
                request.getReason(),
                scheduledDeletionDate
        );

        List<Long> activeAccountIds = userAccountRepository.findActiveAccountIdsByOrgId(request.getOrgId());
        logger.info("Deactivating {} accounts for orgId: {}", activeAccountIds.size(), request.getOrgId());

        userAccountRepository.deactivateAllActiveAccountsByOrgIdForDeletion(request.getOrgId());

        addAccountsToDeactivatedCache(activeAccountIds);

        deactivateChatAccounts(activeAccountIds);

        sendDeletionRequestedEmail(organization, scheduledDeletionDate);

        logger.info("Organization deletion request processed successfully for orgId: {}", request.getOrgId());

        return OrgDeletionResponse.builder()
                .orgId(request.getOrgId())
                .organizationName(organization.getOrganizationName())
                .status("PENDING_DELETION")
                .message("Organization deletion request successful. All accounts deactivated. Hard deletion scheduled in " + GRACE_PERIOD_DAYS + " days.")
                .scheduledDeletionDate(scheduledDeletionDate)
                .gracePeriodDays(GRACE_PERIOD_DAYS)
                .build();
    }

    @Override
    @Transactional
    public OrgDeletionResponse reverseOrganizationDeletion(ReverseOrgDeletionRequest request, Long requesterAccountId) {
        logger.info("Processing organization deletion reversal for orgId: {}", request.getOrgId());

        Organization organization = organizationRepository.findByOrgId(request.getOrgId());
        if (organization == null) {
            logger.error("Organization not found with id: {}", request.getOrgId());
            return OrgDeletionResponse.builder()
                    .orgId(request.getOrgId())
                    .status("FAILED")
                    .message("Organization not found")
                    .build();
        }

        if (!Boolean.TRUE.equals(organization.getIsDeletionRequested())) {
            logger.warn("Organization {} is not scheduled for deletion", request.getOrgId());
            return OrgDeletionResponse.builder()
                    .orgId(request.getOrgId())
                    .organizationName(organization.getOrganizationName())
                    .status("NOT_PENDING")
                    .message("Organization is not scheduled for deletion")
                    .build();
        }

        organizationRepository.clearDeletionRequestFields(request.getOrgId());

        List<Long> accountsToReactivate = userAccountRepository.findAccountIdsInactivatedOnOrgDeletion(request.getOrgId());
        logger.info("Reactivating {} accounts for orgId: {}", accountsToReactivate.size(), request.getOrgId());

        userAccountRepository.reactivateAccountsDeactivatedForOrgDeletion(request.getOrgId());

        removeAccountsFromDeactivatedCache(accountsToReactivate);

        reactivateChatAccounts(accountsToReactivate);

        sendDeletionReversedEmail(organization);

        logger.info("Organization deletion reversal processed successfully for orgId: {}", request.getOrgId());

        return OrgDeletionResponse.builder()
                .orgId(request.getOrgId())
                .organizationName(organization.getOrganizationName())
                .status("REVERSED")
                .message("Organization deletion has been reversed. All accounts reactivated.")
                .build();
    }

    @Override
    public List<OrgDeletionResponse> processScheduledDeletions() {
        logger.info("Processing scheduled organization deletions");

        List<Organization> orgsToDelete = organizationRepository.findOrganizationsScheduledForDeletion();
        List<OrgDeletionResponse> responses = new ArrayList<>();

        for (Organization org : orgsToDelete) {
            try {
                OrgDeletionResponse response = hardDeleteOrganization(org.getOrgId());
                responses.add(response);
            } catch (Exception e) {
                logger.error("Failed to hard delete organization {}: {}", org.getOrgId(), e.getMessage());
                String stackTrace = StackTraceHandler.getAllStackTraces(e);
                logger.error(stackTrace);

                sendDeletionFailedEmail(org, e.getMessage());

                responses.add(OrgDeletionResponse.builder()
                        .orgId(org.getOrgId())
                        .organizationName(org.getOrganizationName())
                        .status("FAILED")
                        .message("Hard deletion failed: " + e.getMessage())
                        .build());
            }
        }

        logger.info("Processed {} scheduled organization deletions", responses.size());
        return responses;
    }

    @Override
    @Transactional
    public OrgDeletionResponse hardDeleteOrganization(Long orgId) {
        logger.info("Starting hard deletion for organization: {}", orgId);

        Organization organization = organizationRepository.findById(orgId).orElse(null);
        if (organization == null) {
            logger.error("Organization not found with id: {}", orgId);
            return OrgDeletionResponse.builder()
                    .orgId(orgId)
                    .status("FAILED")
                    .message("Organization not found")
                    .build();
        }

        DeletedOrganizationStats stats = collectOrganizationStats(organization);

        List<Long> buIds = buRepository.findAllBuIdsByOrgId(orgId);
        List<Long> projectIds = projectRepository.findAllProjectIdsByOrgId(orgId);
        List<Long> teamIds = teamRepository.findAllTeamIdsByOrgId(orgId);
        List<Long> accountIds = userAccountRepository.findAllAccountIdsByOrgId(orgId);
        List<Long> taskIds = taskRepository.findAllTaskIdsByOrgId(orgId);

        logger.info("Organization {} has {} BUs, {} projects, {} teams, {} accounts, {} tasks",
                orgId, buIds.size(), projectIds.size(), teamIds.size(), accountIds.size(), taskIds.size());

        deleteEntityTypeBasedTables(orgId, buIds, projectIds, teamIds);

        deleteTaskAndMeetingDependents(orgId, taskIds, teamIds);

        deleteRuntimeOperationalData(orgId, accountIds);

        deletePoliciesAndConfigTables(orgId, teamIds);

        deleteCoreEntities(orgId);

        deleteAccountBasedTables(orgId, accountIds);

        deleteChatDatabaseData(orgId, accountIds);

        organizationRepository.deleteById(orgId);

        deletedOrganizationStatsRepository.save(stats);

        sendDeletionCompletedEmail(organization, stats);

        logger.info("Hard deletion completed for organization: {}", orgId);

        return OrgDeletionResponse.builder()
                .orgId(orgId)
                .organizationName(organization.getOrganizationName())
                .status("DELETED")
                .message("Organization and all associated data have been permanently deleted")
                .build();
    }

    private DeletedOrganizationStats collectOrganizationStats(Organization org) {
        Long orgId = org.getOrgId();

        Integer buCount = buRepository.findBuCountByOrgId(orgId);
        Integer projectCount = projectRepository.findProjectCountByOrgId(orgId);
        Integer teamCount = teamRepository.findTeamCountByOrgId(orgId);
        Integer totalUserCount = userAccountRepository.findUserCountByOrgId(orgId);
        Integer activeUserCount = userAccountRepository.findUserCountByOrgIdAndIsActive(orgId, true);
        Integer inactiveUserCount = totalUserCount - activeUserCount;
        Integer taskCount = taskRepository.findTaskCountByOrgId(orgId);
        Integer deletedProjectsCount = projectRepository.findDeletedProjectCountByOrgId(orgId);
        Integer deletedTeamsCount = teamRepository.findDeletedTeamCountByOrgId(orgId);

        return DeletedOrganizationStats.builder()
                .orgId(orgId)
                .organizationName(org.getOrganizationName())
                .ownerEmail(org.getOwnerEmail())
                .buCount(buCount != null ? buCount : 0)
                .projectCount(projectCount != null ? projectCount : 0)
                .teamCount(teamCount != null ? teamCount : 0)
                .totalUserCount(totalUserCount != null ? totalUserCount : 0)
                .activeUserCount(activeUserCount != null ? activeUserCount : 0)
                .inactiveUserCount(inactiveUserCount != null ? inactiveUserCount : 0)
                .epicCount(0)
                .sprintCount(0)
                .taskCount(taskCount != null ? taskCount : 0)
                .noteCount(0)
                .commentCount(0)
                .templateCount(0)
                .meetingCount(0)
                .stickyNotesCount(0)
                .leavesCount(0)
                .feedbackCount(0)
                .deletedProjectsCount(deletedProjectsCount != null ? deletedProjectsCount : 0)
                .deletedTeamsCount(deletedTeamsCount != null ? deletedTeamsCount : 0)
                .usedMemoryBytes(0L)
                .maxMemoryQuotaBytes(org.getMaxMemoryQuota())
                .deletionReason(org.getDeletionReason())
                .deletionRequestedAt(org.getDeletionRequestedAt())
                .deletionRequestedByAccountId(org.getDeletionRequestedByAccountId())
                .orgCreatedAt(org.getCreatedDateTime())
                .paidSubscription(org.getPaidSubscription())
                .onTrial(org.getOnTrial())
                .build();
    }

    private void deleteEntityTypeBasedTables(Long orgId, List<Long> buIds, List<Long> projectIds, List<Long> teamIds) {
        logger.info("Deleting entity-type based tables for orgId: {}", orgId);

        accessDomainRepository.deleteByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);

        if (!buIds.isEmpty()) {
            accessDomainRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.BU, buIds);
        }
        if (!projectIds.isEmpty()) {
            accessDomainRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.PROJECT, projectIds);
        }
        if (!teamIds.isEmpty()) {
            accessDomainRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIds);
        }

        entityPreferenceRepository.deleteByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);

        if (!teamIds.isEmpty()) {
            labelRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIds);
        }
    }

    private void deleteTaskAndMeetingDependents(Long orgId, List<Long> taskIds, List<Long> teamIds) {
        logger.info("Deleting task and meeting dependents for orgId: {}", orgId);

        if (!taskIds.isEmpty()) {
            commentRepository.deleteAllByTaskIdIn(taskIds);
        }

        meetingRepository.deleteAllByOrgId(orgId);

        if (!teamIds.isEmpty()) {
            sprintRepository.deleteAllByTeamIdIn(teamIds);
            epicRepository.deleteAllByTeamIdIn(teamIds);
        }

        taskRepository.deleteAllByOrgId(orgId);
    }

    private void deleteRuntimeOperationalData(Long orgId, List<Long> accountIds) {
        logger.info("Deleting runtime operational data for orgId: {}", orgId);

        timeSheetRepository.deleteAllByOrgId(orgId);

        if (!accountIds.isEmpty()) {
            stickyNoteRepository.deleteAllByAccountIdIn(accountIds);
            leaveApplicationRepository.deleteAllByAccountIdIn(accountIds);
            leaveRemainingRepository.deleteAllByAccountIdIn(accountIds);
        }

        notificationRepository.deleteAllByOrgId(orgId);
    }

    private void deletePoliciesAndConfigTables(Long orgId, List<Long> teamIds) {
        logger.info("Deleting policies and config tables for orgId: {}", orgId);

        leavePolicyRepository.deleteAllByOrgId(orgId);
    }

    private void deleteCoreEntities(Long orgId) {
        logger.info("Deleting core entities for orgId: {}", orgId);

        teamRepository.deleteAllByOrgId(orgId);
        projectRepository.deleteAllByOrgId(orgId);
        buRepository.deleteAllByOrgId(orgId);
    }

    private void deleteAccountBasedTables(Long orgId, List<Long> accountIds) {
        logger.info("Deleting account-based tables for orgId: {}", orgId);

        if (!accountIds.isEmpty()) {
            accessDomainRepository.deleteByAccountIdIn(accountIds);
        }

        userAccountRepository.deleteAllByOrgId(orgId);
    }

    private void deleteChatDatabaseData(Long orgId, List<Long> accountIds) {
        logger.info("Deleting chat database data for orgId: {}", orgId);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = conversationBaseUrl + "/internal/org/" + orgId + "/delete";

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-Type", "application/json");

            HttpEntity<List<Long>> requestEntity = new HttpEntity<>(accountIds, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Chat database data deleted successfully for orgId: {}", orgId);
            } else {
                logger.warn("Chat database deletion returned non-success status for orgId: {}. Status: {}",
                        orgId, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to delete chat database data for orgId: {}. Error: {}", orgId, e.getMessage());
        }
    }

    private void addAccountsToDeactivatedCache(List<Long> accountIds) {
        try {
            for (Long accountId : accountIds) {
                redisTemplate.opsForSet().add(DEACTIVATED_ACCOUNTS_KEY, String.valueOf(accountId));
            }
            logger.info("Added {} accounts to deactivated cache", accountIds.size());
        } catch (Exception e) {
            logger.error("Failed to add accounts to deactivated cache: {}", e.getMessage());
        }
    }

    private void removeAccountsFromDeactivatedCache(List<Long> accountIds) {
        try {
            for (Long accountId : accountIds) {
                redisTemplate.opsForSet().remove(DEACTIVATED_ACCOUNTS_KEY, String.valueOf(accountId));
            }
            logger.info("Removed {} accounts from deactivated cache", accountIds.size());
        } catch (Exception e) {
            logger.error("Failed to remove accounts from deactivated cache: {}", e.getMessage());
        }
    }

    private void deactivateChatAccounts(List<Long> accountIds) {
        if (accountIds.isEmpty()) return;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = conversationBaseUrl + "/internal/users/deactivate";

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-Type", "application/json");

            HttpEntity<List<Long>> requestEntity = new HttpEntity<>(accountIds, headers);

            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            logger.info("Chat accounts deactivated successfully for {} accounts", accountIds.size());
        } catch (Exception e) {
            logger.error("Failed to deactivate chat accounts: {}", e.getMessage());
        }
    }

    private void reactivateChatAccounts(List<Long> accountIds) {
        if (accountIds.isEmpty()) return;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = conversationBaseUrl + "/internal/users/reactivate";

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-Type", "application/json");

            HttpEntity<List<Long>> requestEntity = new HttpEntity<>(accountIds, headers);

            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            logger.info("Chat accounts reactivated successfully for {} accounts", accountIds.size());
        } catch (Exception e) {
            logger.error("Failed to reactivate chat accounts: {}", e.getMessage());
        }
    }

    private void sendDeletionRequestedEmail(Organization org, Timestamp scheduledDeletionDate) {
        try {
            emailService.sendOrgDeletionRequestedEmail(
                    org.getOwnerEmail(),
                    org.getOrganizationName(),
                    scheduledDeletionDate,
                    GRACE_PERIOD_DAYS
            );
        } catch (Exception e) {
            logger.error("Failed to send deletion requested email for org {}: {}", org.getOrgId(), e.getMessage());
        }
    }

    private void sendDeletionReversedEmail(Organization org) {
        try {
            emailService.sendOrgDeletionReversedEmail(
                    org.getOwnerEmail(),
                    org.getOrganizationName()
            );
        } catch (Exception e) {
            logger.error("Failed to send deletion reversed email for org {}: {}", org.getOrgId(), e.getMessage());
        }
    }

    private void sendDeletionCompletedEmail(Organization org, DeletedOrganizationStats stats) {
        try {
            emailService.sendOrgDeletionCompletedEmail(
                    org.getOwnerEmail(),
                    org.getOrganizationName(),
                    stats
            );
        } catch (Exception e) {
            logger.error("Failed to send deletion completed email for org {}: {}", org.getOrgId(), e.getMessage());
        }
    }

    private void sendDeletionFailedEmail(Organization org, String errorMessage) {
        try {
            emailService.sendOrgDeletionFailedEmail(
                    systemAdminEmail,
                    org.getOrgId(),
                    org.getOrganizationName(),
                    errorMessage
            );
        } catch (Exception e) {
            logger.error("Failed to send deletion failed email for org {}: {}", org.getOrgId(), e.getMessage());
        }
    }
}
