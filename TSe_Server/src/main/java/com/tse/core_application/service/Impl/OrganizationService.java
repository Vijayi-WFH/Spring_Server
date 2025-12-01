package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.BuIdAndBuName;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.dto.NewOrgMemberRequest;
import com.tse.core_application.dto.RemoveOrgMemberRequest;
import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.dto.conversations.ConversationUser;
import com.tse.core_application.dto.conversations.GroupAndUsersDTO;
import com.tse.core_application.dto.org_response.*;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.UnauthorizedException;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.IOrganizationService;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import com.tse.core_application.utils.LongListConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizationService implements IOrganizationService {

    @Value("${system.admin.email}")
    private String superAdminMail;

    @Value("${spring.mail.username}")
    private String username;

    private static final Logger logger = LogManager.getLogger(OrganizationService.class.getName());

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private BURepository buRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private OrgRequestsRepository orgRequestsRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private MeetingService meetingService;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private StickyNoteService stickyNoteService;
    @Autowired
    private SprintService sprintService;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private ConversationService conversationService;

    @Value("${conversation.application.root.path}")
    private String conversationBaseUrl;

    @Autowired
    UserFeatureAccessService userFeatureAccessService;


    @Override
    public Organization addOrganization(Organization organization, ExceptionalRegistration exceptionalRegistration, String userEmail, User user) {
        Integer maxBuCount = exceptionalRegistration != null ? exceptionalRegistration.getMaxBuCount() : Constants.DefaultEntitiesCount.maxBuCount;
        Integer maxTeamCount = exceptionalRegistration != null ? exceptionalRegistration.getMaxTeamCount() : Constants.DefaultEntitiesCount.maxTeamCount;
        Integer maxUserCount = exceptionalRegistration != null ? exceptionalRegistration.getMaxUserCount() : Constants.DefaultEntitiesCount.maxUserCount;
        Integer maxProjectCount = exceptionalRegistration != null ? exceptionalRegistration.getMaxProjectCount() : Constants.DefaultEntitiesCount.maxProjectCount;
        Long maxMemoryQuota = exceptionalRegistration != null ? exceptionalRegistration.getMaxMemoryQuota() : Constants.DefaultEntitiesCount.maxMemoryQuota;
        String ownerEmail = exceptionalRegistration != null ? exceptionalRegistration.getEmail() : userEmail;
        Boolean onTrial = (exceptionalRegistration != null && exceptionalRegistration.getOnTrial() != null) ? exceptionalRegistration.getOnTrial() : false;
        Boolean paidSubscription = (exceptionalRegistration != null && exceptionalRegistration.getPaidSubscription() != null) ? exceptionalRegistration.getPaidSubscription() : false;


        organization.setMaxBuCount(maxBuCount);
        organization.setMaxTeamCount(maxTeamCount);
        organization.setMaxUserCount(maxUserCount);
        organization.setMaxProjectCount(maxProjectCount);
        organization.setMaxMemoryQuota(maxMemoryQuota);
        organization.setOwnerEmail(ownerEmail);
        organization.setOnTrial(onTrial);
        organization.setPaidSubscription(paidSubscription);
        Organization savedOrganization = this.organizationRepository.save(organization);

        return organization;
    }

    @Override
    public Organization getOrganizationByOrganizationName(String orgName) {
        String orgToFind = orgName.trim().replaceAll("\\s+", " ");
        if (orgToFind.length() < 2 || orgToFind.length() > 100) {
            throw new ValidationFailedException("Organization name must be between 2 to 100 characters long");
        }
        List<Organization> allOrganizations = organizationRepository.findAll();
        for(Organization organization: allOrganizations) {
            if(orgToFind.equalsIgnoreCase(organization.getOrganizationName().trim().replaceAll("\\s+", " "))) {
                return organization;
            }
        }
        return null;
    }


    public List<OrgIdOrgName> getOrganizationByToken(String userName) {
        User userDb = userRepository.findByPrimaryEmail(userName);
        if (userDb != null) {
            List<OrgIdOrgName> orgIdOrgNameList = new ArrayList<>();
            List<UserAccount> userAccountDb = userAccountService.getAllUserAccountByUserIdAndIsActive(userDb.getUserId());

            for (UserAccount userAccount : userAccountDb) {
                OrgIdOrgName orgIdOrgNameDb = organizationRepository
                        .findOrgIdAndOrganizationNameByOrgId(userAccount.getOrgId());
                orgIdOrgNameList.add(orgIdOrgNameDb);
            }

            if (orgIdOrgNameList.isEmpty()) {
                String allSTackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
                logger.error("No organizations list found. ", new Throwable(allSTackTraces));
                ThreadContext.clearMap();
                throw new NoDataFoundException();
            } else {
                // Sort by organizationName ASC (case-insensitive, nulls last)
                orgIdOrgNameList.sort(
                        Comparator.comparing(
                                OrgIdOrgName::getOrganizationName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                );
                return orgIdOrgNameList;
            }
        }
        return null;
    }

    public UserAccount getUserAccountIdFromUserAccount(String email, Long orgId) {
        User userDb = userRepository.findByPrimaryEmail(email);
        if(userDb != null) {
            UserAccount userAccountDb = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(orgId, userDb.getUserId(), true);
            if(userAccountDb != null) {
                return userAccountDb;
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
                logger.error("No record found in the user account table by orgId = " + orgId + " and userId = " + userDb.getUserId(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new NoDataFoundException();
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new UserDoesNotExistException());
            logger.error("User does not exist in the user table by email = " + email, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new UserDoesNotExistException();
        }
    }

    public OrgIdOrgName getOrganizationByOrgId(Long orgId) {
        OrgIdOrgName orgIdOrgName = organizationRepository.findOrgIdAndOrganizationNameByOrgId(orgId);
        return orgIdOrgName;
    }

    public List<Organization> getAllOrganizationByOrgIds(List<Long> orgIds) {
        List<Organization> allOrganizations = new ArrayList<>();
        if(!orgIds.isEmpty()) {
            allOrganizations = organizationRepository.findByOrgIdIn(orgIds);
        }
        return allOrganizations;
    }

    /** This method is used to get all the structure of the org: org --> all BUs inside org --> all projects inside all BUs --> all teams inside all projects --> all users inside all teams */
    public OrgStructureResponse getOrgStructureToEdit(Long orgId, List<Long> account) {
            OrgStructureResponse orgStructureResponse = new OrgStructureResponse();
            // find the account id associated with the org id from access doamin
             Long ownerAccountId =  accessDomainRepository.getAccountIdByOrgId(orgId);
            Organization organization;

             if(account.contains(ownerAccountId)){
                 organization = organizationRepository.findByOrgId(orgId);
                 if(organization==null){
                     throw new NoDataFoundException();
                 }
                 if (organization.getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
                     return null;
                 }
             }
             else{
                 throw new ValidationFailedException("You are not authorised to access the organization");
             }


            orgStructureResponse.setOrgId(organization.getOrgId());
            orgStructureResponse.setOrgName(organization.getOrganizationName());
            List<BU> buList = buRepository.findByOrgIdIn(List.of(orgId));
            List<BuDetail> buDetailList = new ArrayList<>();
            for (BU bu : buList) {
                BuDetail buDetail = new BuDetail();
                buDetail.setBuId(bu.getBuId());
                buDetail.setBuName(bu.getBuName());
                List<Project> projectList = projectRepository.findByBuIdAndOrgId(bu.getBuId(), orgId);
                List<ProjectDetail> projectDetailList = new ArrayList<>();
                for (Project project : projectList) {
                    ProjectDetail projectDetail = new ProjectDetail();
                    projectDetail.setProjectId(project.getProjectId());
                    projectDetail.setProjectName(project.getProjectName());
                    List<Team> teamList = teamRepository.findByFkProjectIdProjectId(project.getProjectId());
                    List<TeamDetail> teamDetailList = new ArrayList<>();
                    for (Team team : teamList) {
                        TeamDetail teamDetail = new TeamDetail();
                        teamDetail.setTeamId(team.getTeamId());
                        teamDetail.setTeamName(team.getTeamName());
                        List<Long> accountIdsList = accessDomainRepository.findDistinctAccountIdsByEntityTypeAndEntityTypeIdAndIsActive(team.getTeamId(), Constants.EntityTypes.TEAM);
                        List<UserDetail> userDetailList = new ArrayList<>();
                        for (Long accountId : accountIdsList) {
                            UserDetail userDetail = new UserDetail();
                            UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
                            userDetail.setAccountId(accountId);
                            userDetail.setEmail(userAccount.getEmail());
                            userDetail.setFirstName(userAccount.getFkUserId().getFirstName());
                            userDetail.setLastName(userAccount.getFkUserId().getLastName());
                            userDetailList.add(userDetail);
                        }
                        teamDetail.setUserDetail(userDetailList);
                        teamDetailList.add(teamDetail);
                    }
                    projectDetail.setTeam(teamDetailList);
                    projectDetailList.add(projectDetail);
                }
                buDetail.setProject(projectDetailList);
                buDetailList.add(buDetail);
            }
            orgStructureResponse.setBu(buDetailList);
            return orgStructureResponse;
        }

    /** This method is used to get all the orgs of the account ids list passed in the header */

    public List<OrgIdOrgName> getOrgForOrgAdmin(List<Long> accounts) {

        List<Long> allOrgIds = accessDomainRepository.findDistinctOrgIdByEntityTypeIdAndRoleIdAndAccountIdInAndIsActive(Constants.EntityTypes.ORG, RoleEnum.ORG_ADMIN.getRoleId(), accounts);
        return organizationRepository.findOrgIdAndOrganizationNameByOrgIdIn(allOrgIds);
    }

    /** This method is used to remove all the account ids passed in the request from the org passed in the request after validations */
    public boolean removeMemberFromOrg(RemoveOrgMemberRequest removeOrgMemberRequest, List<Long> accountIds, String timeZone, User user) throws JsonProcessingException, IllegalAccessException {

        if (!organizationRepository.existsById(removeOrgMemberRequest.getOrgId())) {
            return false;
        }

        // accountId of the org admin
        Long orgAdminAccountId = accessDomainRepository.getAccountIdByOrgId(removeOrgMemberRequest.getOrgId());
        Integer roleIdOfTeamAdmin = RoleEnum.TEAM_ADMIN.getRoleId();
        Integer roleIdOfBackUpTeamAdmin = RoleEnum.BACKUP_TEAM_ADMIN.getRoleId();
        Long removedUserAccountId = removeOrgMemberRequest.getAccountId();
        UserAccount removedUser = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(removedUserAccountId);
        userAccountService.removeAccountFromUsers(removedUser.getFkUserId().getUserId(), Collections.singletonList(removedUserAccountId));

        // Todo: As of now, we don't have back org admin saved anywhere. When we add backup org admin functionality -- we need to allow back up org admin to delete user from org
        if (!accountIds.contains(orgAdminAccountId)) {
            throw new ValidationFailedException("You are not authorized to edit the org");
        }

        if (removedUserAccountId.equals(orgAdminAccountId)) {
            throw new ValidationFailedException("org admin cannot remove himself from the organization");
        }

        // if the user is admin in any of the teams - we modify the access domain and team tables
//        for (Long removedUserAccountId : removeOrgMemberRequest.getAccountIds()) {

        // find all access domains of the user where its role = team admin, entityType = Team and accountId = accountId
        List<AccessDomain> removedUserAccessDomains = accessDomainRepository.findByEntityTypeIdAndRoleIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, roleIdOfTeamAdmin, removedUserAccountId, true);
        for (AccessDomain removedUserAccessDomain : removedUserAccessDomains) {
            Long teamId = removedUserAccessDomain.getEntityId();
            // if any other team admin exists for that team - simply mark the deleted user as inactive
            AccessDomain accessDomainOfOtherTeamAdmin = accessDomainRepository.findFirstByEntityTypeIdAndEntityIdAndRoleIdAndAccountIdNotAndIsActive(Constants.EntityTypes.TEAM, teamId, roleIdOfTeamAdmin, removedUserAccountId, true);
            if (accessDomainOfOtherTeamAdmin != null) {
                removedUserAccessDomain.setIsActive(false);
                if(teamRepository.findByTeamId((long) teamId).getFkOwnerAccountId().getAccountId().equals(removeOrgMemberRequest.getAccountId())) {
                    teamRepository.updateOwnerAccountIdByTeamId(userAccountRepository.findByAccountId(accessDomainOfOtherTeamAdmin.getAccountId()), (long) teamId);
                }
            } else {
                List<AccessDomain> accessDomainsOfBackUpTeamAdmin = accessDomainRepository.findDistinctByEntityIdAndRoleIdAndIsActive(teamId, roleIdOfBackUpTeamAdmin, true);
                // no other team admin or back up team admin exists
                if (accessDomainsOfBackUpTeamAdmin == null || accessDomainsOfBackUpTeamAdmin.isEmpty()) {
                    AccessDomain accessDomainOfOrgAdmin = new AccessDomain();
                    BeanUtils.copyProperties(removedUserAccessDomain, accessDomainOfOrgAdmin);
                    accessDomainOfOrgAdmin.setAccountId(orgAdminAccountId);
                    teamRepository.updateOwnerAccountIdByTeamId(userAccountRepository.findByAccountId(orgAdminAccountId), (long) teamId);
                    accessDomainRepository.save(accessDomainOfOrgAdmin);
                    removedUserAccessDomain.setIsActive(false);
                } else {
                    // assign any backup admin as the new team admin
                    AccessDomain accessDomainOfBackUpAdmin = accessDomainsOfBackUpTeamAdmin.get(0);
                    accessDomainOfBackUpAdmin.setRoleId(roleIdOfTeamAdmin);
                    teamRepository.updateOwnerAccountIdByTeamId(userAccountRepository.findByAccountId(accessDomainOfBackUpAdmin.getAccountId()), (long) teamId);
                    accessDomainRepository.save(accessDomainOfBackUpAdmin);
                    removedUserAccessDomain.setIsActive(false);
                }
            }
        }
        removeProjectRoles(roleIdOfTeamAdmin, removedUserAccountId, orgAdminAccountId, removeOrgMemberRequest);

        accessDomainService.deactivateAllAccessDomainsInAllTeams(removedUserAccountId);
        meetingService.cancelOrganizedMeetingsByUserRemovedInEntityAndSendNotification(Constants.EntityTypes.ORG, removeOrgMemberRequest.getOrgId(), removedUserAccountId, orgAdminAccountId, timeZone);
        meetingService.removeUserAsAttendeeFromAllFutureMeetings(Constants.EntityTypes.ORG, removeOrgMemberRequest.getOrgId(), removedUserAccountId, orgAdminAccountId, timeZone);
        taskServiceImpl.removeDeletedAccountAsMentorObserverAssignedToImmediateAttention(removedUserAccountId, Constants.EntityTypes.ORG, removeOrgMemberRequest.getOrgId(), orgAdminAccountId, removeOrgMemberRequest.getTaskIdAssignedToList(), timeZone);

        List<Long> teamIdList = teamRepository.findTeamIdsByOrgId(removeOrgMemberRequest.getOrgId());
        if (teamIdList != null && !teamIdList.isEmpty()) {
            for (Long teamId : teamIdList) {
                sprintService.removeMemberFromAllSprintInTeam(teamId, removeOrgMemberRequest.getAccountId(), accountIds);
            }
        }
        userPreferenceService.editUserPreferenceOnUserRemovalFromEntity(Constants.EntityTypes.ORG, removeOrgMemberRequest.getOrgId(), removedUserAccountId);
        stickyNoteService.removeDeletedAccountFromStickyNotesSharedList(Constants.EntityTypes.ORG, removeOrgMemberRequest.getOrgId(), removedUserAccountId);
        auditService.auditForDeletedOrgMember(removeOrgMemberRequest.getOrgId(), removedUserAccountId);
        userAccountService.markAccountAsInactive(removedUserAccountId);

        // set isActive false in userAccount
        int isUserAccountUpdated = userAccountRepository.updateIsActiveByOrgIdAndAccountIdIn(removeOrgMemberRequest.getOrgId(), removedUserAccountId);
        if (isUserAccountUpdated < 0) {
            throw new UserDoesNotExistException();
        }

        //Look for the user in Conversation DB
        removeUserFromConversationDB2(removedUserAccountId, user);
        userFeatureAccessService.addRemoveFeatureAccessOfOrg(removeOrgMemberRequest.getOrgId(), removedUserAccountId, true);
        return true;
    }

    public int addNewMemberToOrg(NewOrgMemberRequest newOrgMemberRequest, Long accountId, String timezone) throws JsonProcessingException {
        // check if user exists
        if(userRepository.existsByPrimaryEmail(newOrgMemberRequest.getUserName())) {
            //check if user is a part of requested org and is active
            UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(newOrgMemberRequest.getUserName(), newOrgMemberRequest.getOrgId(), true);
            if (userAccount !=null){
                return 0;
            }
            //check if org exists
            if(!organizationRepository.existsById(newOrgMemberRequest.getOrgId())){
                return 2;
            }
            User userToAdd = userRepository.findByPrimaryEmail(newOrgMemberRequest.getUserName());
            //Check if user is already requested
            if(orgRequestsRepository.existsByOrgIdUserIdAndIsAccepted(newOrgMemberRequest.getOrgId(), userToAdd.getUserId())!=0){
                return 1;
            }
            Organization org = organizationRepository.findByOrgId(newOrgMemberRequest.getOrgId());
            if (Objects.equals(org.getMaxUserCount(), 0) || (org.getMaxUserCount() > 0 && !userAccountRepository.isUserRegistrationAllowed(org.getOrgId(), org.getMaxUserCount().longValue()))) {
                throw new IllegalStateException("Organization exceeded it's quota of registering users");
            }
            //if not, send a request to user
            OrgRequests orgRequests = new OrgRequests();
            orgRequests.setFromOrgId(newOrgMemberRequest.getOrgId());
            orgRequests.setForUserId(userToAdd.getUserId());
            orgRequestsRepository.save(orgRequests);
            Optional<EntityPreference> entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, newOrgMemberRequest.getOrgId());
            if (entityPreference.isPresent()) {
                schedulingService.addHolidayTimesheetForEntity(entityPreference.get(), Collections.singletonList(new AccountId(accountId)));
            }
            userAccountService.addAccountToUsers(userToAdd.getUserId(), Collections.singletonList(accountId));
            notificationService.requestForNewOrg(newOrgMemberRequest,accountId,timezone);
            return 3;
        }
        throw new UserDoesNotExistException();
    }

    public List<OrgRequests> getOrgRequest(Long userId, String accountIds) {
        if(!userRepository.existsById(userId)){
            throw new UserDoesNotExistException();
        }
        LongListConverter longListConverter = new LongListConverter();
        List<Long> accountIdList = longListConverter.convertToEntityAttribute(accountIds);
        List<Long> userAccountIds = userAccountRepository.findAccountIdByFkUserIdUserIdAndIsActive(userRepository.findByUserId(userId), true);
        userAccountIds.retainAll(accountIdList);
        if(userAccountIds.isEmpty()){
            throw new UnauthorizedException("Unauthorized attempt");
        }
        return orgRequestsRepository.findByForUserIdAndIsAccepted(userId);
    }

    public void updateOrgRequest(Long orgRequestId, Boolean response) {
        if(orgRequestsRepository.existsById(orgRequestId)){
            orgRequestsRepository.updateIsAcceptedByOrgRequestId(response,orgRequestId);
        }
        else {
            throw new NoDataFoundException();
        }
    }

    // checks whether the organization already exists by the org name
    public Boolean doesOrganizationExist(String orgName) {
        if (getOrganizationByOrganizationName(orgName) != null && orgName.equalsIgnoreCase(Constants.PERSONAL_ORG)) {
            return false;
        }
        return getOrganizationByOrganizationName(orgName) != null;

    }

    //this method returns list of BU for an organization
    public List<BuIdAndBuName> getOrgAllBU (Long orgId, String accountId) {
        List<Long> accountIds = jwtRequestFilter.getAccountIdsFromHeader(accountId);
        Long orgAdminAccountId = accessDomainRepository.getAccountIdByOrgId(orgId);
        if (!accountIds.contains(orgAdminAccountId)) {
            throw new ValidationFailedException("You are not authorized to view BU for the organization");
        }
        return buRepository.findBuIdAndBuNameByOrgId(orgId);
    }

    public Boolean validateOrgUser (Long orgId, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        return userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(headerAccountIds, orgId, true);
    }

    private void removeProjectRoles(Integer roleIdOfTeamAdmin, Long removedUserAccountId, Long orgAdminAccountId, RemoveOrgMemberRequest removeOrgMemberRequest) {

        // find all access domains of the user where its role = project admin, entityType = Project and accountId = accountId
        List<AccessDomain> removedUserProjectAccessDomains = accessDomainRepository.findByEntityTypeIdAndRoleIdAndAccountIdAndIsActive(Constants.EntityTypes.PROJECT, RoleEnum.PROJECT_ADMIN.getRoleId(), removedUserAccountId, true);
        for (AccessDomain removedUserProjectAccessDomain : removedUserProjectAccessDomains) {
            Long projectId = removedUserProjectAccessDomain.getEntityId();
            // if any other team admin exists for that team - simply mark the deleted user as inactive
            AccessDomain accessDomainOfOtherProjectAdmin = accessDomainRepository.findFirstByEntityTypeIdAndEntityIdAndRoleIdAndAccountIdNotAndIsActive(Constants.EntityTypes.PROJECT, projectId, RoleEnum.PROJECT_ADMIN.getRoleId(), removedUserAccountId, true);
            if (accessDomainOfOtherProjectAdmin != null) {
                removedUserProjectAccessDomain.setIsActive(false);
                if(projectRepository.findByProjectId((long) projectId).getOwnerAccountId().equals(removeOrgMemberRequest.getAccountId())) {
                    projectRepository.updateOwnerAccountIdByProjectId(accessDomainOfOtherProjectAdmin.getAccountId(), (long) projectId);
                }
            } else {
                List<AccessDomain> accessDomainsOfBackUpProjectAdmin = accessDomainRepository.findDistinctByEntityIdAndRoleIdAndIsActive(projectId, RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId(), true);
                // no other team admin or back up team admin exists
                if (accessDomainsOfBackUpProjectAdmin == null || accessDomainsOfBackUpProjectAdmin.isEmpty()) {
                    AccessDomain accessDomainOfOrgAdmin = new AccessDomain();
                    BeanUtils.copyProperties(removedUserProjectAccessDomain, accessDomainOfOrgAdmin);
                    accessDomainOfOrgAdmin.setAccountId(orgAdminAccountId);
                    projectRepository.updateOwnerAccountIdByProjectId(orgAdminAccountId, (long) projectId);
                    accessDomainRepository.save(accessDomainOfOrgAdmin);
                    removedUserProjectAccessDomain.setIsActive(false);
                } else {
                    // assign any backup admin as the new team admin
                    AccessDomain accessDomainOfBackUpAdmin = accessDomainsOfBackUpProjectAdmin.get(0);
                    accessDomainOfBackUpAdmin.setRoleId(roleIdOfTeamAdmin);
                    projectRepository.updateOwnerAccountIdByProjectId(accessDomainOfBackUpAdmin.getAccountId(), (long) projectId);
                    accessDomainRepository.save(accessDomainOfBackUpAdmin);
                    removedUserProjectAccessDomain.setIsActive(false);
                }
            }
        }
        accessDomainService.deactivateAllAccessDomainsInAllProjects(removedUserAccountId);
    }

    @Override
    public Integer getOrganizationCountByEmail (String email) {
        return this.organizationRepository.getOrgCountByEmail (email);
    }

    public void removeUserFromConversationDB(Long removedUserAccountId, User user) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String getUserUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl + ControllerConstants.Conversations.getUserByAccountId).buildAndExpand(removedUserAccountId).toUriString();

            com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
            tokenUser.setUsername(user.getPrimaryEmail());
            String token = jwtUtil.generateToken(tokenUser, List.of(0L));

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<ConversationUser> userResponse = restTemplate.exchange(getUserUrl, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<ConversationUser>() {}
            );
            ConversationUser conversationUser = userResponse.getBody();
            //Get all the groups that the user is a part of
            try {
                String getGroupsUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl + ControllerConstants.Conversations.getUserGroups).buildAndExpand(conversationUser.getUserId()).toUriString();

                ResponseEntity<RestResponseWithData> groupsResponse = restTemplate.exchange(getGroupsUrl, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<RestResponseWithData>() {}
                );

                LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) groupsResponse.getBody().getData();

                ObjectMapper objectMapper = new ObjectMapper();
                List<ConversationGroup> conversationGroups = objectMapper.convertValue(dataMap, objectMapper.getTypeFactory().constructCollectionType(List.class, ConversationGroup.class));

                //Remove the user from all the groups they are a part of
                for (ConversationGroup conversationGroup : conversationGroups) {
                    try {
                        Long groupId = conversationGroup.getUsers().stream().anyMatch(convUser -> conversationUser.getUserId().equals(convUser.getUserId())) ? conversationGroup.getGroupId() : null;
                        if (groupId == null)
                            continue;
                        HttpEntity<Object> requestEntityGroup = new HttpEntity<>(new GroupAndUsersDTO(groupId, List.of(conversationUser.getUserId())), headers);
                        ResponseEntity<ConversationGroup> groupResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.removeUsersFromGroup, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<ConversationGroup>() {
                        });
                    } catch (Exception e) {
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("Could not update Conversations DB " + e, new Throwable(allStackTraces));
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setTo(superAdminMail);
                        message.setSubject("Error occurred while creating Conversation Group");
                        message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
                        message.setFrom(username);
                        emailSender.send(message);
                    }
                }

                //Delete the user
                try {
                    conversationUser.setIsActive(false);
                    HttpEntity<Object> requestEntityGroup = new HttpEntity<>(conversationUser, headers);
                    ResponseEntity<ConversationUser> savedUserResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.createUser, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<ConversationUser>() {
                    });
                    System.out.println(savedUserResponse);
                }
                catch (Exception e) {
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Could not update Conversations DB " + e, new Throwable(allStackTraces));
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(superAdminMail);
                    message.setSubject("Error occurred while creating Conversation Group");
                    message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
                    message.setFrom(username);
                    emailSender.send(message);
                }

            } catch (Exception e) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Could not update Conversations DB " + e, new Throwable(allStackTraces));
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(superAdminMail);
                message.setSubject("Error occurred while creating Conversation Group");
                message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
                message.setFrom(username);
                emailSender.send(message);
            }
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not update Conversations DB " + e, new Throwable(allStackTraces));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(superAdminMail);
            message.setSubject("Error occurred while creating Conversation Group");
            message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
            message.setFrom(username);
            emailSender.send(message);
        }
    }

    public void removeUserFromConversationDB2(Long removedUserAccountId, User user) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String deleteUserUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl + ControllerConstants.Conversations.deleteUserFromOrg).buildAndExpand(removedUserAccountId).toUriString();

            com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
            tokenUser.setUsername(user.getPrimaryEmail());
            String token = jwtUtil.generateToken(tokenUser, List.of(0L));

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(deleteUserUrl, HttpMethod.POST, requestEntity, String.class);

        } catch (Exception e){
            logger.error("Could not remove user from Conversations DB " + e);
            throw e;
        }
    }

    public Map<Long, String> getOrgNamesByIds(Set<Long> orgIds) {
        return organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getOrgId, Organization::getOrganizationName));
    }
}
