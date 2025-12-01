package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.OrgDetailsForSuperUser;
import com.tse.core_application.dto.RestrictedDomainRequest;
import com.tse.core_application.dto.report.ApplicationReport;
import com.tse.core_application.dto.report.OrganizationReportResponse;
import com.tse.core_application.dto.report.UserLoginInfo;
import com.tse.core_application.dto.report.UserOrganizationsReport;
import com.tse.core_application.dto.super_admin.*;
import com.tse.core_application.dto.GetUserForSuperAdminRequest;
import com.tse.core_application.exception.MissingDetailsException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tse.core_application.dto.UserActivateDeactivateDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private BURepository buRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StickyNoteRepository stickyNoteRepository;

    @Autowired
    private GroupConversationRepository conversationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private PerfNoteRepository perfNoteRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private TaskTemplateRepository templateRepository;

    @Autowired
    private RestrictedDomainsRepository restrictedDomainsRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SprintService sprintService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    public void deactivateAccounts (ReactivateDeactivateUserRequest deactivateUserRequest, String accountIds, User foundUser,boolean isSuperAdmin) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserActivateDeactivateDto userActivateDeactivateDto = verifyAdminRolesForActivateAndDeactivate(isSuperAdmin, accountIds, deactivateUserRequest.getAccountIds());
        if (deactivateUserRequest.getAccountIds() != null && !deactivateUserRequest.getAccountIds().isEmpty()) {
            for (Long accountIdToDeactivate : deactivateUserRequest.getAccountIds()) {
                UserAccount user = userAccountRepository.findByAccountIdAndIsActive(accountIdToDeactivate, true);
                if (user != null && user.getOrgId() != null) {
                    Long orgId = user.getOrgId();
                    List<Long> teamIdList = teamRepository.findTeamIdsByOrgId(orgId);
                    if (teamIdList != null && !teamIdList.isEmpty()) {
                        for (Long teamId : teamIdList) {
                            sprintService.removeMemberFromAllSprintInTeam(teamId, accountIdToDeactivate, headerAccountIds);
                        }
                    }
                }
                userAccountService.markAccountAsInactive(accountIdToDeactivate);
                userAccountRepository.updateIsActiveAndIsDisabledBySamsByAccountId(accountIdToDeactivate, false, true, userActivateDeactivateDto.getRoleId(), userActivateDeactivateDto.getActivateDeactivatedByAccountId());
                auditService.auditForAccountDeactivateReactivate(userActivateDeactivateDto.getUserAccount(), accountIdToDeactivate, Constants.AuditStatusEnum.DEACTIVATE);
            }
            changeIsActiveStatusOfAccountIds(deactivateUserRequest.getAccountIds(), false);
            conversationService.activateDeactivateUserInConversation(deactivateUserRequest.getAccountIds(), foundUser.getPrimaryEmail(), true);
        }
        else if (deactivateUserRequest.getUsername() != null) {
            User user = userRepository.findByPrimaryEmail(deactivateUserRequest.getUsername());
            List<Long> accountIdList = userAccountRepository.findAllAccountIdsByEmailAndIsActive(deactivateUserRequest.getUsername(), true);
            for (Long accountIdToDeactivate : accountIdList) {
                Long orgId = userAccountRepository.findOrgIdByAccountIdAndIsActive(accountIdToDeactivate, true).getOrgId();
                if (orgId != null) {
                    List<Long> teamIdList = teamRepository.findTeamIdsByOrgId(orgId);
                    if (teamIdList != null && !teamIdList.isEmpty()) {
                        for (Long teamId : teamIdList) {
                            sprintService.removeMemberFromAllSprintInTeam(teamId, accountIdToDeactivate, headerAccountIds);
                        }
                    }
                }
            }
            userAccountService.deactivateUserAccount(deactivateUserRequest.getUsername(), userActivateDeactivateDto.getRoleId(), userActivateDeactivateDto.getActivateDeactivatedByAccountId());
            changeIsActiveStatusOfAccountIds(accountIdList, false);
            auditService.auditForUserDeactivateReactivate(userActivateDeactivateDto.getUserAccount(),user, Constants.AuditStatusEnum.DEACTIVATE);
            conversationService.activateDeactivateUserInConversation(accountIdList, foundUser.getPrimaryEmail(), true);
        }
    }

    public void reactivateAccounts (ReactivateDeactivateUserRequest reactivateUserRequest, String accountIds, User foundUser, boolean isSuperAdmin) {
        UserActivateDeactivateDto userActivateDeactivateDto = verifyAdminRolesForActivateAndDeactivate(isSuperAdmin, accountIds,reactivateUserRequest.getAccountIds());
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (reactivateUserRequest.getAccountIds() != null && !reactivateUserRequest.getAccountIds().isEmpty()) {
            for (Long accountIdToDeactivate : reactivateUserRequest.getAccountIds()) {
                userAccountService.markAccountAsActive(accountIdToDeactivate);
                userAccountRepository.updateIsActiveAndIsDisabledBySamsByAccountId(accountIdToDeactivate, true, false, null, null);
                auditService.auditForAccountDeactivateReactivate(userActivateDeactivateDto.getUserAccount(), accountIdToDeactivate, Constants.AuditStatusEnum.REACTIVATE);
            }
            changeIsActiveStatusOfAccountIds(reactivateUserRequest.getAccountIds(), true);
            conversationService.activateDeactivateUserInConversation(reactivateUserRequest.getAccountIds(), foundUser.getPrimaryEmail(), false);
            if (reactivateUserRequest.getUsername() != null) {
               userAccountRepository.updateIsActiveAndIsDisabledBySamsByOrgIdAndEmail(userActivateDeactivateDto.getOrgId(), reactivateUserRequest.getUsername(), true, false, null, null);
                // If user email is present in redis then it should get removed (User is deactivated from all the org using email)
                userAccountService.removeUserFromRedis(reactivateUserRequest.getUsername());
            }
        }
        else if (reactivateUserRequest.getUsername() != null) {
            userAccountService.reactivateUserAccount(reactivateUserRequest.getUsername());
            User user = userRepository.findByPrimaryEmail(reactivateUserRequest.getUsername());
            List<Long> accountIdList = userAccountRepository.findAllAccountIdsByEmailAndIsActive(reactivateUserRequest.getUsername(), true);
            changeIsActiveStatusOfAccountIds(accountIdList, true);
            auditService.auditForUserDeactivateReactivate(userActivateDeactivateDto.getUserAccount(), user, Constants.AuditStatusEnum.REACTIVATE);
            conversationService.activateDeactivateUserInConversation(accountIdList, foundUser.getPrimaryEmail(), false);
        }
    }

    public void deactivateOrganization (Long orgId, String accountIds) {
        validateActivateDeactivateOrganizationAction(accountIds);
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        organizationRepository.updateIsDisabledByOrgId(orgId, true);
        projectRepository.updateIsDisabledByOrgId(orgId, true);
        buRepository.updateIsDisabledByOrgId(orgId, true);
        teamRepository.updateIsDisabledByOrgId(orgId, true);
        userAccountRepository.updateIsActiveAndIsDisabledBySamsByOrgId(orgId, false, true,null,null);
        auditService.auditForOrgDeactivateReactivate(userAccount, orgId, Constants.AuditStatusEnum.DEACTIVATE);
    }

    public void reactivateOrganization (Long orgId, String accountIds) {
        validateActivateDeactivateOrganizationAction(accountIds);
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        organizationRepository.updateIsDisabledByOrgId(orgId, false);
        projectRepository.updateIsDisabledByOrgId(orgId, false);
        buRepository.updateIsDisabledByOrgId(orgId, false);
        teamRepository.updateIsDisabledByOrgId(orgId, false);
        userAccountRepository.updateIsActiveAndIsDisabledBySamsByOrgId(orgId, true, false,null,null);
        auditService.auditForOrgDeactivateReactivate(userAccount, orgId, Constants.AuditStatusEnum.REACTIVATE);
    }

    public List<UserDetailsForSuperAdmin> getUsersForSuperAdmin(GetUserForSuperAdminRequest userRequest, String accountIds) {
        validateActivateDeactivateAccountAction(accountIds);
        StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT a FROM UserAccount a inner join a.fkUserId u WHERE ");

        queryBuilder.append("a.orgId IN :orgIds ");

        List<Long> orgIds = userRequest.getOrganizationList();
        //adding or project id is null to get holidays and leaves
        if (userRequest.getShowPersonal() != null && userRequest.getShowPersonal()) {
            orgIds.add(Constants.OrgIds.PERSONAL.longValue());
        }
        //adding or team id is null to get holidays and leaves
        if (userRequest.getUserName() != null) {
            queryBuilder.append("AND a.email = :email ");
        }

        Query nativeQuery = entityManager.createQuery(queryBuilder.toString(), UserAccount.class);

        // Set parameters
        nativeQuery.setParameter("orgIds", orgIds);
        if (userRequest.getUserName() != null) {
            nativeQuery.setParameter("email", userRequest.getUserName());
        }


        List<UserAccount> userAccountList = nativeQuery.getResultList();
        List<UserDetailsForSuperAdmin> accountList = new ArrayList<>();
        Map<Long, String> orgIdToNameMap = new HashMap<>();
        for (UserAccount userAccount : userAccountList) {
            Long orgId = userAccount.getOrgId();
            String orgName = orgIdToNameMap.get(orgId);
            if (orgName == null) {
                orgName = organizationRepository.findOrgNameByOrgId(orgId);
                orgIdToNameMap.put(orgId, orgName);
            }
            accountList.add(new UserDetailsForSuperAdmin(userAccount.getEmail(), userAccount.getAccountId(), userAccount.getFkUserId().getFirstName(), userAccount.getFkUserId().getLastName(), userAccount.getIsActive(), orgId, orgName, userAccount.getIsDisabledBySams(),userAccount.getDeactivatedByRole(),userAccount.getDeactivatedByAccountId()));
        }
        accountList.sort(Comparator.comparing(UserDetailsForSuperAdmin::getOrgName).thenComparing(UserDetailsForSuperAdmin::getFirstName));

        return accountList;
    }

    private void validateActivateDeactivateAccountAction (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount user = userAccountRepository
                .findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        if (user == null) {
            throw new ValidationFailedException("User not found for the given accountIds");
        }
        Long accountId = user.getAccountId();
        if (!accessDomainRepository.findUserRoleInEntity(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), accountId, true, Constants.ActionId.DEACTIVATE_ACTIVATE_USER_ACCOUNT)) {
            throw new ValidationFailedException("User do not have action to reactivate/deactivate accounts");
        }
    }

    private void validateActivateDeactivateOrganizationAction (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Long accountId = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true).getAccountId();
        if (!accessDomainRepository.findUserRoleInEntity(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), accountId, true, Constants.ActionId.DEACTIVATE_ACTIVATE_USER_ORGANIZATION)) {
            throw new ValidationFailedException("User do not have action to reactivate/deactivate organization");
        }
    }

    public List<OrgDetailsForSuperUser> getAllOrgDetails (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            return Collections.emptyList();
        }
        List<OrgDetailsForSuperUser> orgList = organizationRepository.findAllOrgDetails();
        orgList.sort(Comparator.comparing(OrgDetailsForSuperUser::getOrganizationName));
        return orgList;
    }

    public DefaultEntitiesCountResponse getDefaultEntitiesCount (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalStateException("User not authorized to view default entities count");
        }
        DefaultEntitiesCountResponse defaultEntitiesCountResponse = new DefaultEntitiesCountResponse();
        defaultEntitiesCountResponse.setMaxBuCount(Constants.DefaultEntitiesCount.maxBuCount);
        defaultEntitiesCountResponse.setMaxOrgCount(Constants.DefaultEntitiesCount.maxOrgCount);
        defaultEntitiesCountResponse.setMaxUserCount(Constants.DefaultEntitiesCount.maxUserCount);
        defaultEntitiesCountResponse.setMaxTeamCount(Constants.DefaultEntitiesCount.maxTeamCount);
        defaultEntitiesCountResponse.setMaxProjectCount(Constants.DefaultEntitiesCount.maxProjectCount);
        defaultEntitiesCountResponse.setMaxMemoryQuota(Constants.DefaultEntitiesCount.maxMemoryQuota);
        return defaultEntitiesCountResponse;
    }

    public OrgDetailsForSuperUser updateLimitsInOrg (Long orgId, UpdateLimitsInOrgRequest updateLimitsInOrgRequest, String accountIds) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to update organization limits");
        }
        Organization organization = organizationRepository.findByOrgId(orgId);
        if (organization == null) {
            throw new EntityNotFoundException("Organization not found");
        }
        CommonUtils.copyNonNullProperties(updateLimitsInOrgRequest, organization);
        Organization savedOrganization = organizationRepository.save(organization);
        auditService.auditForOrgLimits(userAccount, orgId, Constants.AuditStatusEnum.UPDATE);
        User user = userRepository.findByPrimaryEmail(organization.getOwnerEmail());
        Optional<EntityPreference> entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, savedOrganization.getOrgId());
        Boolean isGeoFencingAllowed = false;
        Boolean isGeoFencingActive = false;
        if (entityPreference.isPresent()) {
            isGeoFencingActive = entityPreference.get().getIsGeoFencingActive();
            isGeoFencingAllowed = entityPreference.get().getIsGeoFencingAllowed();
        }
        OrgDetailsForSuperUser orgResponse = new OrgDetailsForSuperUser(organization.getOrgId(), (Object) organization.getOrganizationName(), organization.getIsDisabled(),
                organization.getMaxBuCount(), organization.getMaxProjectCount(), organization.getMaxTeamCount(), organization.getMaxUserCount(), organization.getMaxMemoryQuota(),
                (Object) organization.getOwnerEmail(), organization.getPaidSubscription(), organization.getOnTrial(), organization.getOwnerEmail(), user.getFirstName(), user.getLastName(),
                isGeoFencingAllowed, isGeoFencingActive);
        return orgResponse;
    }

    public List<UserOrganizationsReport> getUserOrganizationReport (String email, String accountIds) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to view organization report for users");
        }
        User user = userRepository.findByPrimaryEmail(email);
        if (user == null) {
            throw new ValidationFailedException("Please provide a valid username");
        }
        List<String> userEmailList = new ArrayList<>();
        userEmailList.add(email);
        if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
            userEmailList.addAll(userRepository.findAllUserPrimaryEmailByManagingUserId(user.getUserId()));
        }
        List<Organization> organizationList = organizationRepository.findAllOrgByOwnerEmailIn(userEmailList);
        List<UserOrganizationsReport> userOrganizationsReports = new ArrayList<>();
        for (Organization organization : organizationList) {
            UserOrganizationsReport userOrganizationsReport = new UserOrganizationsReport();
            userOrganizationsReport.setOrgName(organization.getOrganizationName());
            userOrganizationsReport.setIsDisabled(organization.getIsDisabled());
            userOrganizationsReport.setOrgOwnerEmail(organization.getOwnerEmail());
            userOrganizationsReport.setUserCount(userAccountRepository.findUserCountByOrgId(organization.getOrgId()));
            userOrganizationsReport.setTeamCount(teamRepository.findTeamCountByOrgId(organization.getOrgId()));
            userOrganizationsReport.setBuCount(buRepository.findBuCountByOrgId(organization.getOrgId()));
            userOrganizationsReport.setProjectCount(projectRepository.findProjectCountByOrgId(organization.getOrgId()));
            userOrganizationsReport.setDeletedProjectCount(projectRepository.findDeletedProjectCountByOrgId(organization.getOrgId()));
            userOrganizationsReport.setDeletedTeamCount(teamRepository.findDeletedTeamCountByOrgId(organization.getOrgId()));
            userOrganizationsReports.add(userOrganizationsReport);
        }
        return userOrganizationsReports;
    }

    public ApplicationReport getApplicationReport (String accountIds) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to view organization report for users");
        }
        ApplicationReport applicationReport = new ApplicationReport();
        applicationReport.setTotalBu(buRepository.count());
        applicationReport.setTotalComments(commentRepository.count());
        applicationReport.setTotalEpics(epicRepository.count());
        applicationReport.setTotalLeaves(leaveApplicationRepository.count());
        applicationReport.setTotalNotes(noteRepository.count());
        applicationReport.setTotalConversations(conversationRepository.count());
        applicationReport.setTotalOrganization(organizationRepository.count());
        applicationReport.setTotalFeedback(perfNoteRepository.count());
        applicationReport.setTotalProject(projectRepository.count());
        applicationReport.setTotalMeetings(meetingRepository.count());
        applicationReport.setTotalSprints(sprintRepository.count());
        applicationReport.setTotalTask(taskRepository.count());
        applicationReport.setTotalStickyNotes(stickyNoteRepository.count());
        applicationReport.setTotalUser(userRepository.count());
        applicationReport.setTotalTeam(teamRepository.count());
        applicationReport.setTotalTemplates(templateRepository.count());

        return applicationReport;
    }

    public OrganizationReportResponse getOrganizationReportResponse (String orgName, String accountIds) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to view organization report for users");
        }
        String sanitizedOrgName = orgName.trim().replaceAll("\\s+", " ");
        Optional<Organization> organizationOptional = organizationRepository.findByOrganizationName(sanitizedOrgName);
        if (organizationOptional.isEmpty()) {
            throw new IllegalStateException("Please provide a valid organization name");
        }
        Long orgId = organizationOptional.get().getOrgId();
        OrganizationReportResponse organizationReportResponse = new OrganizationReportResponse();
        organizationReportResponse.setBuCount(buRepository.findBuCountByOrgId(orgId));
        organizationReportResponse.setCommentCount(commentRepository.findCommentsCountByOrgId(orgId));
        organizationReportResponse.setEpicCount(epicRepository.findEpicsCountByOrgId(orgId));
        List<UserAccount> userAccountList = userAccountRepository.findByOrgId(orgId);
        List<Long> userAccountIdList = userAccountList.stream().map(UserAccount::getAccountId).collect(Collectors.toList());
        organizationReportResponse.setLeavesCount(leaveApplicationRepository.findLeavesCountByAccountIdIn(userAccountIdList));
        organizationReportResponse.setNoteCount(noteRepository.findNotesCountByOrgId(orgId));
        organizationReportResponse.setOrgName(orgName);
        organizationReportResponse.setOrgOwnerEmail(organizationOptional.get().getOwnerEmail());
        organizationReportResponse.setFeedbackCount(perfNoteRepository.findPerfNotesCountByOrgId(userAccountIdList));
        organizationReportResponse.setProjectCount(projectRepository.findProjectCountByOrgId(orgId));
        organizationReportResponse.setMeetingCount(meetingRepository.findMeetingsCountByOrgId(orgId));
        organizationReportResponse.setSprintCount(sprintRepository.findSprintsCountByFkAccountIdCreatorAccountIdIn(userAccountIdList));
        organizationReportResponse.setTaskCount(taskRepository.findTaskCountByOrgId(orgId));
        organizationReportResponse.setStickyNotesCount(stickyNoteRepository.findStickyNotesCountByOrgId(orgId));
        organizationReportResponse.setUserCount(userAccountIdList.size());
        organizationReportResponse.setTeamCount(teamRepository.findTeamCountByOrgId(orgId));
        organizationReportResponse.setTemplateCount(templateRepository.findTaskTemplateCountByOrgId(orgId));
        organizationReportResponse.setMemoryUsed(organizationOptional.get().getUsedMemoryQuota());
        organizationReportResponse.setMemoryRemaining(organizationOptional.get().getMaxMemoryQuota() - (organizationOptional.get().getUsedMemoryQuota() != null ? organizationOptional.get().getUsedMemoryQuota() : 0));
        organizationReportResponse.setDeletedProjectCount(projectRepository.findDeletedProjectCountByOrgId(orgId));
        organizationReportResponse.setDeletedTeamCount(teamRepository.findDeletedTeamCountByOrgId(orgId));
        List<UserLoginInfo> userLoginInfos = userAccountList.parallelStream()
                .map(userAccount -> {
                    UserLoginInfo userLoginInfo = new UserLoginInfo();
                    userLoginInfo.setEmail(userAccount.getEmail());
                    userLoginInfo.setLastAction(getLastActionMessageForAccountId(userAccount.getAccountId().toString()));
                    return userLoginInfo;
                })
                .collect(Collectors.toList());
        organizationReportResponse.setUserLoginInfo(userLoginInfos);
        organizationReportResponse.setActiveUserCount(userAccountRepository.findUserCountByOrgIdAndIsActive(orgId, true));
        organizationReportResponse.setInactiveUserCount(userAccountRepository.findUserCountByOrgIdAndIsActive(orgId, false));
        return organizationReportResponse;
    }

    public String getLastActionMessageForAccountId(String accountId) {
        String query = "SELECT i.created_date_time FROM tse.info_log i WHERE i.account_id = :accountId ORDER BY i.created_date_time DESC LIMIT 1";

        List<Timestamp> resultList = entityManager.createNativeQuery(query)
                .setParameter("accountId", accountId)
                .getResultList();

        return resultList.isEmpty() ? null : resultList.get(0).toString();
    }

    public RestrictedDomains addRestrictedDomain (RestrictedDomainRequest restrictedDomainRequest, String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to add restricted domains.");
        }
        if (restrictedDomainRequest.getDomain() == null) {
            throw new IllegalStateException("Please provide domain!");
        }
        restrictedDomainRequest.setRestrictedDomainId(null);
        RestrictedDomains restrictedDomains = new RestrictedDomains();
        BeanUtils.copyProperties(restrictedDomainRequest, restrictedDomains);
        String domain = getPrimaryDomainName(restrictedDomainRequest.getDomain());
        if (restrictedDomainsRepository.existsByDomain(domain)) {
            throw new IllegalStateException("Domain already registered");
        }
        restrictedDomains.setDomain(domain);
        if (restrictedDomains.getDisplayName() == null) {
            restrictedDomains.setDisplayName(getPrimaryDomainName(restrictedDomains.getDomain()));
        }

        RestrictedDomains savedRestrictedDomain = restrictedDomainsRepository.save(restrictedDomains);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        auditService.auditForRestrictedDomain(userAccount, savedRestrictedDomain, Constants.AuditStatusEnum.ADD);
        RestrictedDomains response = new RestrictedDomains();
        BeanUtils.copyProperties(savedRestrictedDomain, response);
        convertDateTimeForRestrictedDomains(response, timeZone);
        return response;
    }

    public String getBaseDomain(String url) {
        // Regular expression to match the domain name
        String regex = "(?:http[s]?://)?(?:www\\.)?([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        // If a match is found, return the base domain
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null; // In case the input does not match the expected format
        }
    }

    public String getPrimaryDomainName(String input) {
        // Regular expression to match email addresses, URLs, or domain names
        String regex = "(?:http[s]?://)?(?:www\\.)?([a-zA-Z0-9-]+)\\.[a-zA-Z]{2,6}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // If a match is found, return the primary domain
        if (matcher.find()) {
            String emailPart = matcher.group(0);  // Check if it's an email address or URL
            if (input.contains("@")) {
                return input.split("@")[1].split("\\.")[0];  // Extract domain from email
            }
            return matcher.group(1);  // Extract primary domain from URL or domain name
        } else {
            return input; // In case the input does not match the expected format
        }
    }


    public void convertDateTimeForRestrictedDomains (RestrictedDomains restrictedDomains, String timeZone) {
        if (restrictedDomains.getCreatedDateTime() != null) {
            restrictedDomains.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(restrictedDomains.getCreatedDateTime(), timeZone));
        }

        if (restrictedDomains.getLastUpdatedDateTime() != null) {
            restrictedDomains.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(restrictedDomains.getLastUpdatedDateTime(), timeZone));
        }
    }

    public RestrictedDomains updateRestrictedDomain (RestrictedDomainRequest restrictedDomainRequest, String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to update restricted domains.");
        }

        if (restrictedDomainRequest.getRestrictedDomainId() == null) {
            throw new MissingDetailsException("Please provide restricted domain id to update restricted domain");
        }
        Optional<RestrictedDomains> restrictedDomainsOptional = restrictedDomainsRepository.findById(restrictedDomainRequest.getRestrictedDomainId());
        if (restrictedDomainsOptional.isEmpty()) {
            throw new EntityNotFoundException("Restricted domain not found");
        }
        RestrictedDomains restrictedDomains = restrictedDomainsOptional.get();
        if (restrictedDomainRequest.getDomain() != null) {
            String baseDomain = getPrimaryDomainName(restrictedDomainRequest.getDomain());
            if (!restrictedDomains.getDomain().equalsIgnoreCase(baseDomain)) restrictedDomains.setDomain(baseDomain);
        }
        if (restrictedDomainRequest.getDisplayName() != null) {
            restrictedDomains.setDisplayName(restrictedDomainRequest.getDisplayName());
        }
        if (restrictedDomainRequest.getIsPersonalAllowed() != null && !Objects.equals(restrictedDomainRequest.getIsPersonalAllowed(), restrictedDomains.getIsPersonalAllowed())) {
            restrictedDomains.setIsPersonalAllowed(restrictedDomainRequest.getIsPersonalAllowed());
        }
        if (restrictedDomainRequest.getIsOrgRegistrationAllowed() != null && !Objects.equals(restrictedDomainRequest.getIsOrgRegistrationAllowed(), restrictedDomains.getIsOrgRegistrationAllowed())) {
            restrictedDomains.setIsOrgRegistrationAllowed(restrictedDomainRequest.getIsOrgRegistrationAllowed());
        }

        RestrictedDomains savedRestrictedDomain = restrictedDomainsRepository.save(restrictedDomains);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        auditService.auditForRestrictedDomain(userAccount, savedRestrictedDomain, Constants.AuditStatusEnum.UPDATE);
        RestrictedDomains response = new RestrictedDomains();
        BeanUtils.copyProperties(savedRestrictedDomain, response);
        convertDateTimeForRestrictedDomains(response, timeZone);
        return response;
    }

    public List<RestrictedDomains> getAllRestricedDomains (String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to view restricted domains.");
        }

        List<RestrictedDomains> restrictedDomainsList = restrictedDomainsRepository.findAll().stream()
                .filter(restrictedDomains -> restrictedDomains.getIsDeleted() == null || !restrictedDomains.getIsDeleted())
                .map(restrictedDomains -> {
                    RestrictedDomains copy = new RestrictedDomains();
                    BeanUtils.copyProperties(restrictedDomains, copy);
                    return copy;
                })
                .collect(Collectors.toList());

        restrictedDomainsList.forEach(restrictedDomain -> convertDateTimeForRestrictedDomains(restrictedDomain, timeZone));
        restrictedDomainsList.sort(Comparator.comparing(RestrictedDomains::getDomain));

        return restrictedDomainsList;
    }

    public String deleteRestrictedDomain (String accountIds, Long restrictedDomainId) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), headerAccountIds, List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new IllegalAccessException("User not authorized to view restricted domains.");
        }

        Optional<RestrictedDomains> restrictedDomainsOptional = restrictedDomainsRepository.findById(restrictedDomainId);
        if (restrictedDomainsOptional.isEmpty()) {
            throw new EntityNotFoundException("Restricted domain not found");
        }
        RestrictedDomains restrictedDomain = restrictedDomainsOptional.get();
        restrictedDomain.setIsDeleted(true);
        restrictedDomainsRepository.save(restrictedDomain);
        return "Domain successfully added!";
    }

    public void changeIsActiveStatusOfAccountIds (List<Long> accountIds, Boolean isActive) {
        if (isActive) {
            accessDomainRepository.activateAccountIdsInAccessDomainOnReactivateUser(accountIds);
        }
        else {
            accessDomainRepository.deactivateAccountIdsInAccessDomainOnDeactivateUser(accountIds);
        }
    }

    public UserActivateDeactivateDto verifyAdminRolesForActivateAndDeactivate(boolean isSuperAdmin, String accountIds,List<Long>accountIdList) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Long activateDeactivateByAccountId = null;
        Integer roleId = null;
        UserAccount userAccount = null;
        Long orgId = null;
        UserActivateDeactivateDto userActivateDeactivateDto = new UserActivateDeactivateDto();
        if (!headerAccountIds.isEmpty()) {
            activateDeactivateByAccountId = headerAccountIds.get(0);
            if (!isSuperAdmin) {
                userAccount = userAccountRepository.findByAccountId(activateDeactivateByAccountId);
                orgId = userAccount.getOrgId();
                if(accountIdList != null && !accountIdList.isEmpty()) {
                    Long removeUserOrgId = userAccountRepository.findOrgIdByAccount(accountIdList.get(0));
                    if (!orgId.equals(removeUserOrgId)) {
                        throw new ValidationFailedException("User does not belong to your organization. Action not allowed.");
                    }
                }
                List<Integer> roleIds =
                        accessDomainRepository.findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(
                                activateDeactivateByAccountId,
                                Constants.EntityTypes.ORG,
                                orgId
                        );
                List<Integer> allowedRoles = Arrays.asList(
                        Constants.AdminRoles.ORG_ADMIN,
                        Constants.AdminRoles.BACKUP_ORG_ADMIN
                );
                roleId = roleIds.stream()
                        .filter(allowedRoles::contains)
                        .findFirst()
                        .orElse(null);

                if (roleId == null) {
                    throw new ValidationFailedException("Only Org Admin or Backup Admin can deactivate accounts.");
                }
            } else {
                validateActivateDeactivateAccountAction(accountIds);
                roleId = Constants.AdminRoles.SUPER_ADMIN;
                userAccount = userAccountRepository
                        .findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
                orgId = userAccount.getOrgId();

            }
        }
        userActivateDeactivateDto.setUserAccount(userAccount);
        userActivateDeactivateDto.setActivateDeactivatedByAccountId(activateDeactivateByAccountId);
        userActivateDeactivateDto.setRoleId(roleId);
        userActivateDeactivateDto.setOrgId(orgId);
        return userActivateDeactivateDto;
    }
}
