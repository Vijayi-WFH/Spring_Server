package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.org_deletion.OrgDeletionResponse;
import com.tse.core_application.dto.org_deletion.RequestOrgDeletionRequest;
import com.tse.core_application.dto.org_deletion.ReverseOrgDeletionRequest;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.repository.geo_fencing.assignment.FenceAssignmentRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.geo_fencing.fence.GeoFenceRepository;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.geo_fencing.punch.PunchRequestRepository;
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

    // Core entity repositories
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

    // Task-related repositories
    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private NoteHistoryRepository noteHistoryRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private TaskAttachmentHistoryRepository taskAttachmentHistoryRepository;

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private TaskHistoryMetadataRepository taskHistoryMetadataRepository;

    @Autowired
    private TaskHistoryColumnsMappingRepository taskHistoryColumnsMappingRepository;

    @Autowired
    private TaskSequenceRepository taskSequenceRepository;

    @Autowired
    private TaskMediaRepository taskMediaRepository;

    @Autowired
    private TaskRatingRepository taskRatingRepository;

    @Autowired
    private DeliverablesDeliveredRepository deliverablesDeliveredRepository;

    @Autowired
    private DeliverablesDeliveredHistoryRepository deliverablesDeliveredHistoryRepository;

    @Autowired
    private DependencyRepository dependencyRepository;

    @Autowired
    private WorkItemGithubBranchRepository workItemGithubBranchRepository;

    @Autowired
    private JiraToTseTaskMappingRepository jiraToTseTaskMappingRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    // Epic-related repositories
    @Autowired
    private EpicTaskRepository epicTaskRepository;

    @Autowired
    private EpicSequenceRepository epicSequenceRepository;

    // Meeting-related repositories
    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private ActionItemRepository actionItemRepository;

    @Autowired
    private MeetingNoteRepository meetingNoteRepository;

    @Autowired
    private MeetingSequenceRepository meetingSequenceRepository;

    @Autowired
    private MeetingAnalysisUploadedFileRepository meetingAnalysisUploadedFileRepository;

    @Autowired
    private RecurringMeetingRepository recurringMeetingRepository;

    @Autowired
    private RecurringMeetingSequenceRepository recurringMeetingSequenceRepository;

    // Sprint-related repositories
    @Autowired
    private SprintHistoryRepository sprintHistoryRepository;

    @Autowired
    private CompletedSprintStatsRepository completedSprintStatsRepository;

    @Autowired
    private SprintCapacityMetricsRepository sprintCapacityMetricsRepository;

    @Autowired
    private UserCapacityMetricsRepository userCapacityMetricsRepository;

    // Leave-related repositories
    @Autowired
    private LeaveApplicationHistoryRepository leaveApplicationHistoryRepository;

    @Autowired
    private LeaveRemainingHistoryRepository leaveRemainingHistoryRepository;

    // Sticky note-related repositories
    @Autowired
    private PinnedStickyNoteRepository pinnedStickyNoteRepository;

    @Autowired
    private ImportantStickyNoteRepository importantStickyNoteRepository;

    @Autowired
    private DashboardPinnedStickyNoteRepository dashboardPinnedStickyNoteRepository;

    // Personal task-related repositories
    @Autowired
    private PersonalTaskRepository personalTaskRepository;

    @Autowired
    private PersonalNoteRepository personalNoteRepository;

    @Autowired
    private PersonalAttachmentRepository personalAttachmentRepository;

    @Autowired
    private PersonalTaskTemplateRepository personalTaskTemplateRepository;

    @Autowired
    private PersonalTaskSequenceRepository personalTaskSequenceRepository;

    // Performance notes repositories
    @Autowired
    private PerfNoteRepository perfNoteRepository;

    @Autowired
    private PerfNoteHistoryRepository perfNoteHistoryRepository;

    // Notification-related repositories
    @Autowired
    private NotificationViewRepository notificationViewRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private AlertRepository alertRepository;

    // User-related repositories
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private FirebaseTokenRepository firebaseTokenRepository;

    @Autowired
    private GithubAccountRepository githubAccountRepository;

    @Autowired
    private GithubAccountAndRepoPreferenceRepository githubAccountAndRepoPreferenceRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private MemberDetailsRepository memberDetailsRepository;

    @Autowired
    private InviteRepository inviteRepository;

    // Group conversation and attachment repositories
    @Autowired
    private GroupConversationRepository groupConversationRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    // Audit repository
    @Autowired
    private AuditRepository auditRepository;

    // Geo-fencing repositories
    @Autowired
    private GeoFenceRepository geoFenceRepository;

    @Autowired
    private FenceAssignmentRepository fenceAssignmentRepository;

    @Autowired
    private AttendanceDayRepository attendanceDayRepository;

    @Autowired
    private AttendanceEventRepository attendanceEventRepository;

    @Autowired
    private AttendancePolicyRepository attendancePolicyRepository;

    @Autowired
    private PunchRequestRepository punchRequestRepository;

    // Org/Calendar-related repositories
    @Autowired
    private OrgRequestsRepository orgRequestsRepository;

    @Autowired
    private HolidayOffDayRepository holidayOffDayRepository;

    @Autowired
    private OfficeHoursRepository officeHoursRepository;

    @Autowired
    private BusinessDaysRepository businessDaysRepository;

    @Autowired
    private CalendarDaysRepository calendarDaysRepository;

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

        // Collect all entity IDs needed for cascading deletion
        List<Long> buIds = buRepository.findAllBuIdsByOrgId(orgId);
        List<Long> projectIds = projectRepository.findAllProjectIdsByOrgId(orgId);
        List<Long> teamIds = teamRepository.findAllTeamIdsByOrgId(orgId);
        List<Long> accountIds = userAccountRepository.findAllAccountIdsByOrgId(orgId);
        List<Long> taskIds = taskRepository.findAllTaskIdsByOrgId(orgId);
        List<Long> sprintIds = sprintRepository.findAllSprintIdsByTeamIds(teamIds);
        List<Long> epicIds = epicRepository.findAllEpicIdsByTeamIds(teamIds);
        List<Long> meetingIds = meetingRepository.findAllMeetingIdsByOrgId(orgId);
        List<Long> recurringMeetingIds = recurringMeetingRepository.findAllRecurringMeetingIdsByOrgId(orgId);
        List<Long> leaveApplicationIds = leaveApplicationRepository.findAllLeaveApplicationIdsByAccountIds(accountIds);
        List<Long> stickyNoteIds = stickyNoteRepository.findAllStickyNoteIdsByAccountIds(accountIds);
        List<Long> personalTaskIds = personalTaskRepository.findAllPersonalTaskIdsByAccountIds(accountIds);

        logger.info("Organization {} has {} BUs, {} projects, {} teams, {} accounts, {} tasks, {} sprints, {} epics, {} meetings",
                orgId, buIds.size(), projectIds.size(), teamIds.size(), accountIds.size(), taskIds.size(),
                sprintIds.size(), epicIds.size(), meetingIds.size());

        // DELETE IN FK-SAFE ORDER (children before parents)

        // 1. Delete polymorphic entity-type based tables first
        deleteEntityTypeBasedTables(orgId, buIds, projectIds, teamIds, taskIds, meetingIds, epicIds, sprintIds, accountIds);

        // 2. Delete task dependents (deepest level first)
        deleteTaskDependents(taskIds, accountIds);

        // 3. Delete meeting dependents
        deleteMeetingDependents(meetingIds, recurringMeetingIds);

        // 4. Delete sprint and epic dependents
        deleteSprintAndEpicDependents(sprintIds, epicIds, teamIds, taskIds);

        // 5. Delete leave dependents
        deleteLeaveDependents(leaveApplicationIds, accountIds, orgId);

        // 6. Delete sticky note dependents
        deleteStickyNoteDependents(stickyNoteIds, accountIds);

        // 7. Delete personal task dependents
        deletePersonalTaskDependents(personalTaskIds, accountIds);

        // 8. Delete performance notes
        deletePerformanceNotes(orgId, accountIds);

        // 9. Delete geo-fencing data
        deleteGeoFencingData(orgId);

        // 10. Delete notification and user-related data
        deleteNotificationAndUserData(orgId, accountIds);

        // 11. Delete runtime operational data
        deleteRuntimeOperationalData(orgId, accountIds, taskIds);

        // 12. Delete policies and config tables
        deletePoliciesAndConfigTables(orgId, teamIds);

        // 13. Delete org calendar data
        deleteOrgCalendarData(orgId);

        // 14. Delete chat database data
        deleteChatDatabaseData(orgId, accountIds);

        // 15. Delete core entities (in order: tasks → meetings → sprints → epics → teams → projects → BUs → accounts)
        deleteCoreEntities(orgId, taskIds, meetingIds, recurringMeetingIds, sprintIds, epicIds, teamIds, projectIds, buIds, accountIds);

        // 16. Delete the organization itself
        organizationRepository.deleteById(orgId);

        // 17. Save deletion stats for audit
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

    private void deleteEntityTypeBasedTables(Long orgId, List<Long> buIds, List<Long> projectIds,
                                               List<Long> teamIds, List<Long> taskIds, List<Long> meetingIds,
                                               List<Long> epicIds, List<Long> sprintIds, List<Long> accountIds) {
        logger.info("Deleting entity-type based tables for orgId: {}", orgId);

        // Delete access_domain entries for all entity types
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
        if (!taskIds.isEmpty()) {
            accessDomainRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TASK, taskIds);
        }
        if (!accountIds.isEmpty()) {
            accessDomainRepository.deleteByAccountIdIn(accountIds);
        }

        // Delete entity_preference entries for all entity types
        entityPreferenceRepository.deleteByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (!teamIds.isEmpty()) {
            entityPreferenceRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIds);
        }
        if (!projectIds.isEmpty()) {
            entityPreferenceRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.PROJECT, projectIds);
        }

        // Delete labels for all entity types
        if (!teamIds.isEmpty()) {
            labelRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIds);
        }
        if (!projectIds.isEmpty()) {
            labelRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.PROJECT, projectIds);
        }

        // Delete group conversations for all entity types
        if (!taskIds.isEmpty()) {
            groupConversationRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TASK, taskIds);
            // Delete attachments associated with group conversations
            attachmentRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TASK, taskIds);
        }
        if (!meetingIds.isEmpty()) {
            groupConversationRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.MEETING, meetingIds);
            attachmentRepository.deleteByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.MEETING, meetingIds);
        }

        logger.info("Deleted entity-type based tables for orgId: {}", orgId);
    }

    private void deleteTaskDependents(List<Long> taskIds, List<Long> accountIds) {
        logger.info("Deleting task dependents for {} tasks", taskIds.size());

        if (!taskIds.isEmpty()) {
            // Delete task history related tables (deepest level first)
            taskHistoryMetadataRepository.deleteByTaskIdIn(taskIds);
            // Get task history IDs to delete related columns mapping
            List<Long> taskHistoryIds = taskHistoryRepository.findAllTaskHistoryIdsByTaskIds(taskIds);
            if (!taskHistoryIds.isEmpty()) {
                taskHistoryColumnsMappingRepository.deleteByTaskHistoryIdIn(taskHistoryIds);
            }
            taskHistoryRepository.deleteByTaskIdIn(taskIds);

            // Delete task attachment related tables
            taskAttachmentHistoryRepository.deleteByTaskIdIn(taskIds);
            taskAttachmentRepository.deleteByTaskIdIn(taskIds);

            // Delete note related tables
            noteHistoryRepository.deleteByTaskIdIn(taskIds);
            noteRepository.deleteByTaskIdIn(taskIds);

            // Delete comments
            commentRepository.deleteAllByTaskIdIn(taskIds);

            // Delete deliverables
            deliverablesDeliveredHistoryRepository.deleteByTaskIdIn(taskIds);
            deliverablesDeliveredRepository.deleteByTaskIdIn(taskIds);

            // Delete task sequence
            taskSequenceRepository.deleteByTaskIdIn(taskIds);

            // Delete task media
            taskMediaRepository.deleteByTaskIdIn(taskIds);

            // Delete task ratings
            taskRatingRepository.deleteByTaskIdIn(taskIds);

            // Delete dependencies
            dependencyRepository.deleteByTaskIdIn(taskIds);

            // Delete GitHub branch mappings
            workItemGithubBranchRepository.deleteByTaskIdIn(taskIds);

            // Delete Jira mappings (use orgId since no taskId-based delete)
            // jiraToTseTaskMappingRepository uses deleteByOrgId which is called later
        }

        // Delete task templates
        if (!accountIds.isEmpty()) {
            taskTemplateRepository.deleteByAccountIdIn(accountIds);
        }

        logger.info("Deleted task dependents for {} tasks", taskIds.size());
    }

    private void deleteMeetingDependents(List<Long> meetingIds, List<Long> recurringMeetingIds) {
        logger.info("Deleting meeting dependents for {} meetings", meetingIds.size());

        if (!meetingIds.isEmpty()) {
            // Delete attendees
            attendeeRepository.deleteByMeetingIdIn(meetingIds);

            // Delete action items
            actionItemRepository.deleteByMeetingIdIn(meetingIds);

            // Delete meeting notes
            meetingNoteRepository.deleteByMeetingIdIn(meetingIds);

            // Delete meeting sequence
            meetingSequenceRepository.deleteByMeetingIdIn(meetingIds);

            // Delete meeting analysis uploaded files
            meetingAnalysisUploadedFileRepository.deleteByMeetingIdIn(meetingIds);
        }

        if (!recurringMeetingIds.isEmpty()) {
            // Delete recurring meeting sequences
            recurringMeetingSequenceRepository.deleteByRecurringMeetingIdIn(recurringMeetingIds);
        }

        logger.info("Deleted meeting dependents for {} meetings", meetingIds.size());
    }

    private void deleteSprintAndEpicDependents(List<Long> sprintIds, List<Long> epicIds, List<Long> teamIds, List<Long> taskIds) {
        logger.info("Deleting sprint and epic dependents");

        if (!sprintIds.isEmpty()) {
            // Delete sprint history
            sprintHistoryRepository.deleteBySprintIdIn(sprintIds);

            // Delete completed sprint stats
            completedSprintStatsRepository.deleteBySprintIdIn(sprintIds);

            // Delete sprint capacity metrics
            sprintCapacityMetricsRepository.deleteBySprintIdIn(sprintIds);

            // Delete user capacity metrics (uses orgId)
            // userCapacityMetricsRepository.deleteByOrgId is called later in deleteRuntimeOperationalData
        }

        if (!epicIds.isEmpty()) {
            // Delete epic-task mappings
            epicTaskRepository.deleteByEpicIdIn(epicIds);

            // Delete epic sequences
            epicSequenceRepository.deleteByEpicIdIn(epicIds);
        }

        logger.info("Deleted sprint and epic dependents");
    }

    private void deleteLeaveDependents(List<Long> leaveApplicationIds, List<Long> accountIds, Long orgId) {
        logger.info("Deleting leave dependents for orgId: {}", orgId);

        if (!accountIds.isEmpty()) {
            // Delete leave application history (uses accountId)
            leaveApplicationHistoryRepository.deleteByAccountIdIn(accountIds);

            // Delete leave remaining history
            leaveRemainingHistoryRepository.deleteByAccountIdIn(accountIds);

            // Delete leave remaining
            leaveRemainingRepository.deleteAllByAccountIdIn(accountIds);

            // Delete leave applications
            leaveApplicationRepository.deleteAllByAccountIdIn(accountIds);
        }

        logger.info("Deleted leave dependents for orgId: {}", orgId);
    }

    private void deleteStickyNoteDependents(List<Long> stickyNoteIds, List<Long> accountIds) {
        logger.info("Deleting sticky note dependents");

        if (!stickyNoteIds.isEmpty()) {
            // Delete pinned sticky notes
            pinnedStickyNoteRepository.deleteByNoteIdIn(stickyNoteIds);

            // Delete important sticky notes
            importantStickyNoteRepository.deleteByNoteIdIn(stickyNoteIds);
        }

        if (!accountIds.isEmpty()) {
            // Delete dashboard pinned sticky notes (uses accountId)
            dashboardPinnedStickyNoteRepository.deleteByAccountIdIn(accountIds);

            // Delete sticky notes
            stickyNoteRepository.deleteAllByAccountIdIn(accountIds);
        }

        logger.info("Deleted sticky note dependents");
    }

    private void deletePersonalTaskDependents(List<Long> personalTaskIds, List<Long> accountIds) {
        logger.info("Deleting personal task dependents");

        if (!personalTaskIds.isEmpty()) {
            // Delete personal notes
            personalNoteRepository.deleteByPersonalTaskIdIn(personalTaskIds);

            // Delete personal attachments
            personalAttachmentRepository.deleteByPersonalTaskIdIn(personalTaskIds);
        }

        if (!accountIds.isEmpty()) {
            // Delete personal task sequences
            personalTaskSequenceRepository.deleteByAccountIdIn(accountIds);

            // Delete personal task templates
            personalTaskTemplateRepository.deleteByAccountIdIn(accountIds);

            // Delete personal tasks
            personalTaskRepository.deleteByAccountIdIn(accountIds);
        }

        logger.info("Deleted personal task dependents");
    }

    private void deletePerformanceNotes(Long orgId, List<Long> accountIds) {
        logger.info("Deleting performance notes for orgId: {}", orgId);

        // Delete perf note history first (if exists)
        perfNoteHistoryRepository.deleteByOrgId(orgId);

        // Delete perf notes
        perfNoteRepository.deleteByOrgId(orgId);

        logger.info("Deleted performance notes for orgId: {}", orgId);
    }

    private void deleteGeoFencingData(Long orgId) {
        logger.info("Deleting geo-fencing data for orgId: {}", orgId);

        // Delete punch requests
        punchRequestRepository.deleteByOrgId(orgId);

        // Delete attendance events
        attendanceEventRepository.deleteByOrgId(orgId);

        // Delete attendance days
        attendanceDayRepository.deleteByOrgId(orgId);

        // Delete fence assignments
        fenceAssignmentRepository.deleteByOrgId(orgId);

        // Delete geo fences
        geoFenceRepository.deleteByOrgId(orgId);

        // Delete attendance policy
        attendancePolicyRepository.deleteByOrgId(orgId);

        logger.info("Deleted geo-fencing data for orgId: {}", orgId);
    }

    private void deleteNotificationAndUserData(Long orgId, List<Long> accountIds) {
        logger.info("Deleting notification and user data for orgId: {}", orgId);

        if (!accountIds.isEmpty()) {
            // Delete notification views
            notificationViewRepository.deleteByAccountIdIn(accountIds);

            // Delete reminders
            reminderRepository.deleteByAccountIdIn(accountIds);

            // Alerts are deleted by org - done later

            // Delete devices
            deviceRepository.deleteByAccountIdIn(accountIds);

            // Delete firebase tokens
            firebaseTokenRepository.deleteByAccountIdIn(accountIds);

            // GitHub accounts and preferences are deleted by orgId - handled later

            // User preferences are deleted by orgId - handled later

            // User feature access is deleted by orgId - handled later

            // Delete user roles
            userRoleRepository.deleteByAccountIdIn(accountIds);

            // Delete member details
            memberDetailsRepository.deleteByAccountIdIn(accountIds);

            // Invites are deleted by entityTypeId/entityId - handled in deleteEntityTypeBasedTables

            // Delete audits
            auditRepository.deleteByAccountIdIn(accountIds);
        }

        // Delete notifications
        notificationRepository.deleteAllByOrgId(orgId);

        logger.info("Deleted notification and user data for orgId: {}", orgId);
    }

    private void deleteRuntimeOperationalData(Long orgId, List<Long> accountIds, List<Long> taskIds) {
        logger.info("Deleting runtime operational data for orgId: {}", orgId);

        // Delete timesheets
        timeSheetRepository.deleteAllByOrgId(orgId);

        // Delete org requests
        orgRequestsRepository.deleteByOrgId(orgId);

        // Delete Jira mappings
        jiraToTseTaskMappingRepository.deleteByOrgId(orgId);

        // Delete user capacity metrics
        userCapacityMetricsRepository.deleteByOrgId(orgId);

        // Delete alerts
        alertRepository.deleteByOrgId(orgId);

        // Delete GitHub accounts and preferences
        githubAccountAndRepoPreferenceRepository.deleteAll();  // Will cascade with org deletion
        githubAccountRepository.deleteByOrgId(orgId);

        // Delete user preferences
        userPreferenceRepository.deleteByOrgId(orgId);

        // Delete user feature access
        userFeatureAccessRepository.deleteByOrgId(orgId);

        logger.info("Deleted runtime operational data for orgId: {}", orgId);
    }

    private void deletePoliciesAndConfigTables(Long orgId, List<Long> teamIds) {
        logger.info("Deleting policies and config tables for orgId: {}", orgId);

        // Delete leave policies
        leavePolicyRepository.deleteAllByOrgId(orgId);

        logger.info("Deleted policies and config tables for orgId: {}", orgId);
    }

    private void deleteOrgCalendarData(Long orgId) {
        logger.info("Deleting org calendar data for orgId: {}", orgId);

        // Delete holiday off days
        holidayOffDayRepository.deleteByOrgId(orgId);

        // Delete office hours
        officeHoursRepository.deleteByOrgId(orgId);

        // Delete business days
        businessDaysRepository.deleteByOrgId(orgId);

        // Delete calendar days
        calendarDaysRepository.deleteByOrgId(orgId);

        logger.info("Deleted org calendar data for orgId: {}", orgId);
    }

    private void deleteCoreEntities(Long orgId, List<Long> taskIds, List<Long> meetingIds,
                                    List<Long> recurringMeetingIds, List<Long> sprintIds,
                                    List<Long> epicIds, List<Long> teamIds, List<Long> projectIds,
                                    List<Long> buIds, List<Long> accountIds) {
        logger.info("Deleting core entities for orgId: {}", orgId);

        // Delete tasks first (has FK to team)
        taskRepository.deleteAllByOrgId(orgId);

        // Delete meetings (has FK to team)
        meetingRepository.deleteAllByOrgId(orgId);

        // Delete recurring meetings (has FK to team)
        recurringMeetingRepository.deleteByOrgId(orgId);

        // Delete sprints (has FK to team)
        if (!teamIds.isEmpty()) {
            sprintRepository.deleteAllByTeamIdIn(teamIds);
        }

        // Delete epics (has FK to team)
        if (!teamIds.isEmpty()) {
            epicRepository.deleteAllByTeamIdIn(teamIds);
        }

        // Delete teams (has FK to project)
        teamRepository.deleteAllByOrgId(orgId);

        // Delete projects (has FK to BU)
        projectRepository.deleteAllByOrgId(orgId);

        // Delete BUs (has FK to org)
        buRepository.deleteAllByOrgId(orgId);

        // Delete user accounts (has FK to org)
        userAccountRepository.deleteAllByOrgId(orgId);

        logger.info("Deleted core entities for orgId: {}", orgId);
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
