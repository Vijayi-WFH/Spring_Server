package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.OpenfireException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.ignoreCase;

@Service
public class AccessDomainService {

    private static final Logger logger = LogManager.getLogger(AccessDomainService.class.getName());
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleActionRepository roleActionRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private OpenFireService openFireService;

    @Autowired
    private SecondaryDatabaseService secondaryDatabaseService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private MemberDetailsRepository memberDetailsRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private BUService buService;
    @Autowired
    private StickyNoteService stickyNoteService;
    @Autowired
    private SprintService sprintService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private BURepository buRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Value("${enable.openfire}")
    private Boolean enableOpenfire;

    final int TEAM_ADMIN_ROLE_ID = RoleEnum.TEAM_ADMIN.getRoleId();
    final int BACKUP_ADMIN_ROLE_ID = RoleEnum.BACKUP_TEAM_ADMIN.getRoleId();
    final int TEAM_VIEWER = RoleEnum.TEAM_VIEWER.getRoleId();
    final int ORG_ADMIN_ROLE_ID = RoleEnum.ORG_ADMIN.getRoleId();

    public ArrayList<Integer> getEffectiveRolesByAccountId(Long accountId,Integer entityTypeId, Long teamId) {
        List<AccountIdEntityTypeIdRoleId> userRole = accessDomainRepository
                .findAccountIdEntityTypeIdRoleIdByAccountIdAndEntityIdAndIsActive(accountId,entityTypeId, teamId, true);
        ArrayList<Integer> arrayListRoleId = new ArrayList<Integer>();
        for (AccountIdEntityTypeIdRoleId userrole : userRole) {
            HashMap<String, Object> mapUserRole = objectMapper.convertValue(userrole, HashMap.class);
            Object roleIdValue = mapUserRole.get("roleId");
            int roles = (Integer) roleIdValue;
            arrayListRoleId.add(roles);
        }
        return arrayListRoleId;
    }

    public List<AccountIdEntityIdRoleId> getEffectiveRolesByEntityType(String entitytype) {
        List<AccountIdEntityIdRoleId> userRole = accessDomainRepository
                .getAccountIdEntityIdRoleIdByEntityType(entitytype);
        return userRole;

    }

    public List<AccountIdEntityTypeIdRoleId> getEffectiveRolesByUserId(long userid) {
        List<AccountIdEntityTypeIdRoleId> userRole = accessDomainRepository
                .getAccountIdEntityTypeIdRoleIdByUserId(userid);
        return userRole;
    }

//    public List<AccountIdEntityTypeIdRoleId> getAllEffectiveRolesByAccountId(Long accountid) {
//        List<AccountIdEntityTypeIdRoleId> userRole = accessDomainRepository
//                .findAccountIdEntityTypeIdRoleIdByAccountId(accountid);
//        return userRole;
//    }

    public AccessDomain addAccessDomainAfterTeamAdd(Team team, Long teamAdminId) {
        Long teamAdmin = Objects.equals(team.getFkOrgId().getOrgId(), Constants.OrgIds.PERSONAL.longValue()) ? team.getFkOwnerAccountId().getAccountId() : teamAdminId;
        AccessDomain accessDomainToAdd = new AccessDomain();
        accessDomainToAdd.setAccountId(teamAdmin);
        accessDomainToAdd.setEntityId(team.getTeamId());
        accessDomainToAdd.setEntityTypeId(entityTypeRepository.findEntityTypeIdByEntityType(Constants.EntityTypeNames.Entity_Type_TEAM).getEntityTypeId());
        accessDomainToAdd.setWorkflowTypeId(null);
        accessDomainToAdd.setRoleId(RoleEnum.TEAM_ADMIN.getRoleId());

        // if it is the first team for the account, we add the team as default preference of the user
        Long userIdOfAccount = userAccountRepository.findUserIdByAccountId(teamAdmin);
        List<Long> accountIdsOfUser = userAccountRepository.findAllAccountIdsByUserIdAndIsActive(userIdOfAccount, true);

        if(accessDomainRepository.findByEntityTypeIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, accountIdsOfUser, true).isEmpty()) {
            Optional<UserPreference> userPreferenceOptional = userPreferenceRepository.findById(userIdOfAccount);
            if(userPreferenceOptional.isPresent()) {
                UserPreference userPreference = userPreferenceOptional.get();
                userPreference.setTeamId(team.getTeamId());
                userPreference.setProjectId(team.getFkProjectId().getProjectId());
                userPreference.setOrgId(team.getFkOrgId().getOrgId());
                userPreferenceRepository.save(userPreference);
            }
        }

        AccessDomain accessDomainAdded = accessDomainRepository.save(accessDomainToAdd);
        // ToDo: check later if we want to fill default office minutes using org preference
        MemberDetails memberDetails = new MemberDetails(teamAdmin, Constants.EntityTypes.TEAM,
                team.getTeamId(), com.tse.core_application.constants.Constants.WorkStatus.FULL_TIME, Constants.DEFAULT_OFFICE_MINUTES);
        memberDetailsRepository.save(memberDetails);
        secondaryDatabaseService.insertDataInSecondaryDatabase(team.getTeamId(), accessDomainAdded);
        if (Objects.equals(team.getFkOrgId().getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
            AccessDomain accessDomain = new AccessDomain(teamAdmin, com.tse.core_application.model.Constants.EntityTypes.TEAM, team.getTeamId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
            accessDomainRepository.save(accessDomain);
        }
        return accessDomainAdded;
    }

    public List<Long> getActiveEntityIdsFromAccessDomain(List<Long> accountIds) {
        List<Long> orgIdList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(Constants.EntityTypes.ORG, Constants.ORG_ADMIN_ROLE, accountIds).stream().map(Integer::longValue).collect(Collectors.toList());
        List<Long> buIdList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(Constants.EntityTypes.BU, Constants.BU_ADMIN_ROLE, accountIds).stream().map(Integer::longValue).collect(Collectors.toList());
        List<Long> projectIdList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(Constants.EntityTypes.PROJECT, Constants.PROJECT_ADMIN_ROLE, accountIds).stream().map(Integer::longValue).collect(Collectors.toList());
        List<Long> teamIdList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(Constants.EntityTypes.TEAM, Constants.TEAM_ADMIN_ROLE, accountIds).stream().map(Integer::longValue).collect(Collectors.toList());
        List<Long> teamIdListForOrgAdmin = teamRepository.findTeamIdsByOrgIds(orgIdList);
        List<Long> teamIdListForBuAdmin = teamRepository.findTeamIdsByBuIds(buIdList);
        List<Long> teamIdListForProjectAdmin = teamRepository.findTeamIdsByFkProjectIdProjectIdIn(projectIdList);
        Set<Long> entityIds = new HashSet<>();
        entityIds.addAll(teamIdList);
        entityIds.addAll(teamIdListForProjectAdmin);
        entityIds.addAll(teamIdListForBuAdmin);
        entityIds.addAll(teamIdListForOrgAdmin);

        return new ArrayList<>(entityIds);
    }


    //  get List<EmailFirstLastNameRoleName>
    public List<HashMap<String, Object>> getEmailFirstNameLastNameRoleList(Long teamId) {
        Team team = teamRepository.findByTeamId(teamId);
        if (team == null) {
            throw new EntityNotFoundException("Team is either invalid or deleted");
        }
        List<AccessDomain> accessDomainList = new ArrayList<>();
        List<AccessDomain> accessDomainListForTeam = accessDomainRepository.findByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, teamId, true);
        List<AccessDomain> accessDomainListForProjectAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, team.getFkProjectId().getProjectId(), Constants.PROJECT_ADMIN_ROLE, true);
//        List<AccessDomain> accessDomainListForBuAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.BU, team.getFkProjectId().getBuId(), Constants.BU_ADMIN_ROLE, true);
        List<AccessDomain> accessDomainListForOrgAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId(), Constants.ORG_ADMIN_ROLE, true);
        Map<Long, MemberDetails> memberDetailsMap = getMemberDetailsOfTeam(teamId);
        MemberDetails defaultMemberDetails = new MemberDetails();
        defaultMemberDetails.setWorkStatus(com.tse.core_application.constants.Constants.WorkStatus.FULL_TIME);
        defaultMemberDetails.setWorkMinutes(Constants.DEFAULT_OFFICE_MINUTES);
        if (accessDomainListForTeam != null && !accessDomainListForTeam.isEmpty()) {
            accessDomainList.addAll(accessDomainListForTeam);
        }
        if (accessDomainListForProjectAdmin != null && !accessDomainListForProjectAdmin.isEmpty()) {
            accessDomainList.addAll(accessDomainListForProjectAdmin);
        }
//        if (accessDomainListForBuAdmin != null && !accessDomainListForBuAdmin.isEmpty()) {
//            accessDomainList.addAll(accessDomainListForBuAdmin);
//        }
        if (accessDomainListForOrgAdmin != null && !accessDomainListForOrgAdmin.isEmpty()) {
            accessDomainList.addAll(accessDomainListForOrgAdmin);
        }
        List<HashMap<String, Object>> teamMembersMapList = new ArrayList<>();
        for (AccessDomain accessDomain : accessDomainList) {
//            UserAccount userFromUserAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accessDomain.getAccountId());
            PageRequest pageRequest = PageRequest.of(0, 1);
            List<EmailFirstLastAccountId> emailFirstLastAccountIdPageable = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdActiveOrInActive(accessDomain.getAccountId(), pageRequest);
            EmailFirstLastAccountId emailFirstLastAccountIdDb = emailFirstLastAccountIdPageable.get(0);

            HashMap<String, Object> teamMembersMap = new HashMap<>();
            teamMembersMap.put("email", emailFirstLastAccountIdDb.getEmail());
            teamMembersMap.put("firstName", emailFirstLastAccountIdDb.getFirstName());
            teamMembersMap.put("lastName", emailFirstLastAccountIdDb.getLastName());
            RoleName roleNameDb = roleRepository.findRoleNameByRoleId(accessDomain.getRoleId());
            teamMembersMap.put("roleId", accessDomain.getRoleId());
            teamMembersMap.put("roleName", roleNameDb.getRoleName());
            teamMembersMap.put("isActive", accessDomain.getIsActive());
            MemberDetails memberDetails = memberDetailsMap.getOrDefault(accessDomain.getAccountId(), defaultMemberDetails);
            teamMembersMap.put("workStatus", memberDetails.getWorkStatus());
            teamMembersMap.put("workMinutes", memberDetails.getWorkMinutes());
            teamMembersMapList.add(teamMembersMap);
        }
        if(teamMembersMapList.isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No data found for teamId = " + teamId, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        }
        else {
            // Sort
            teamMembersMapList.sort(Comparator
                    .comparing((HashMap<String, Object> m) -> (Integer) m.get("roleId"), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing((HashMap<String, Object> m) -> {
                        Boolean active = (Boolean) m.get("isActive");
                        if (active == null) return 2;
                        return active ? 0 : 1;
                    })
                    .thenComparing((HashMap<String, Object> m) ->
                                    m.get("firstName") != null ? m.get("firstName").toString() : null,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    )
                    .thenComparing((HashMap<String, Object> m) ->
                                    m.get("lastName") != null ? m.get("lastName").toString() : null,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    )
                    .thenComparing((HashMap<String, Object> m) ->
                                    (Long) m.get("accountId"),
                            Comparator.nullsLast(Long::compareTo)
                    )
            );

        }
        return teamMembersMapList;

    }

    Map<Long, MemberDetails> getMemberDetailsOfTeam (Long teamId) {
        List<MemberDetails> memberDetailsList = memberDetailsRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
        return memberDetailsList.stream().collect(Collectors.toMap(MemberDetails::getAccountId, memberDetails -> memberDetails));
    }


    //  delete the added teamMembers
    public void deleteAddedTeamMembers(RemoveTeamMemberRequest request, String timeZone, List<Long> accountIdsOfRequestor) throws IllegalAccessException {
        String email = request.getEmail();
        Long teamId = request.getTeamId();
        String roleName = request.getRoleName();

        if (!teamRepository.existsById(teamId)) {
            throw new IllegalArgumentException("No such team exits");
        }

        Role teamAdminRole = roleRepository.findRoleByRoleId(RoleEnum.TEAM_ADMIN.getRoleId());
        Integer teamAdminRoleId = teamAdminRole.getRoleId();
        Integer backUpTeamAdminRoleId = RoleEnum.BACKUP_TEAM_ADMIN.getRoleId();
        List<AccountId> teamAdminOrBackUpAdminList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, List.of(backUpTeamAdminRoleId, teamAdminRoleId), true);
        List<Long> teamAdminOrBackUpAdminListLong = teamAdminOrBackUpAdminList.stream().map(AccountId::getAccountId).collect(Collectors.toList());
        Long newTeamAdminAccountId = null;

        Team foundTeam = teamService.getTeamByTeamId(teamId);

        Boolean isModifiedAccountOrgAdminOrBackupOrgAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, foundTeam.getFkOrgId().getOrgId(),
                accountIdsOfRequestor, List.of(RoleEnum.ORG_ADMIN.getRoleId(), RoleEnum.BACKUP_ORG_ADMIN.getRoleId()), true);
        Boolean isModifierAccountTeamAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, foundTeam.getTeamId(),
                accountIdsOfRequestor, List.of(RoleEnum.TEAM_ADMIN.getRoleId()), true);
        Boolean isModifierAccountBackUpTeamAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, foundTeam.getTeamId(),
                accountIdsOfRequestor, List.of(RoleEnum.BACKUP_TEAM_ADMIN.getRoleId()), true);
        Boolean isModifierAccountProjectAdminOrBackUpProjectAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, foundTeam.getFkProjectId().getProjectId(),
                accountIdsOfRequestor, List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId()), true);

        if (!isModifiedAccountOrgAdminOrBackupOrgAdmin && !isModifierAccountProjectAdminOrBackUpProjectAdmin && !isModifierAccountTeamAdmin && !isModifierAccountBackUpTeamAdmin) throw new ValidationFailedException("User not authorized to remove the team member");

        UserAccount activeAccountDbOfRemovedUser = userAccountService.getActiveUserAccountByPrimaryEmailAndOrgId(email, foundTeam.getFkOrgId().getOrgId());
        AccessDomain orgAdminAccessDomain = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdAndIsActive(Constants.EntityTypes.ORG, foundTeam.getFkOrgId().getOrgId(), RoleEnum.ORG_ADMIN.getRoleId(), true);
        RoleId roleIdToDelete = roleRepository.findRoleIdByRoleName(roleName);
        List<AccessDomain> teamAdmins = accessDomainRepository.findDistinctByEntityIdAndRoleIdAndIsActive(teamId, teamAdminRoleId, true);
        List<AccessDomain> backupTeamAdmins = accessDomainRepository.findDistinctByEntityIdAndRoleIdAndIsActive(teamId, backUpTeamAdminRoleId, true);

        List<Long> activeAccountOfUsers = List.of(activeAccountDbOfRemovedUser.getAccountId());
        List<Integer> roleIdOfUsers = List.of(roleIdToDelete.getRoleId());
        List<Long> entityIdsOfUsers = List.of(teamId);
        boolean exists = accessDomainRepository.existsByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityIdsOfUsers, activeAccountOfUsers, roleIdOfUsers, true);
        if (!exists) {
            throw new ValidationFailedException("No active entry found for this team member!");
        }
        if (request.getTaskIdAssignedToList() != null && !request.getTaskIdAssignedToList().isEmpty()) {
            validateOpenTaskAssignedToUser(request);
        }
      
        List<Long> allActiveAccounts = accessDomainRepository.findAllActiveAccountIdsByEntityAndTypeIds(Constants.EntityTypes.TEAM, teamId);


        if (!roleName.equalsIgnoreCase(RoleEnum.TEAM_ADMIN.getRoleName())) {
            // if the role to delete is not team admin -- set the isActive flag to false
            accessDomainRepository.deactivateUserAccessDomainFromTeam(activeAccountDbOfRemovedUser.getAccountId(), roleIdToDelete.getRoleId(), teamId);
            newTeamAdminAccountId = teamAdmins.get(0).getAccountId();
        } else {
            UserAccount userAccountNewTeamAdmin = new UserAccount();
            //check for team admin , team admin should not be able to remove himself if s/he doesn't have higher admin role
            if (!isModifiedAccountOrgAdminOrBackupOrgAdmin && !isModifierAccountProjectAdminOrBackUpProjectAdmin && Objects.equals(roleIdToDelete.getRoleId(),RoleEnum.TEAM_ADMIN.getRoleId()) && accountIdsOfRequestor.contains(activeAccountDbOfRemovedUser.getAccountId())) {
                throw new ValidationFailedException("Team admin cannot remove themselves from the team");
            }
            List<Long> backupTeamAdminAccountIds = new ArrayList<>();
            for (AccessDomain backupTeamAdmin : backupTeamAdmins) {
                backupTeamAdminAccountIds.add(backupTeamAdmin.getAccountId());
            }
            if (!isModifiedAccountOrgAdminOrBackupOrgAdmin && !isModifierAccountProjectAdminOrBackUpProjectAdmin && Objects.equals(roleIdToDelete.getRoleId(),RoleEnum.TEAM_ADMIN.getRoleId()) && CommonUtils.containsAny(accountIdsOfRequestor, backupTeamAdminAccountIds)) {
                throw new ValidationFailedException("Backup team admin cannot remove team admin");
            }
            // if we are deleting a team admin
            if (teamAdmins.size() > 1) {
                // multiple team admins exists -- simply set the isActive flag to false
                newTeamAdminAccountId = Objects.equals(teamAdmins.get(0).getAccountId(), activeAccountDbOfRemovedUser.getAccountId()) ? teamAdmins.get(1).getAccountId() : teamAdmins.get(0).getAccountId();
                userAccountNewTeamAdmin = userAccountRepository.findByAccountIdAndIsActive(newTeamAdminAccountId, true);
            } else if (backupTeamAdmins != null && !backupTeamAdmins.isEmpty()) {
                // if there is one team admin and at least one backup team admin -- make backup team admin as the new admin
                AccessDomain modifiedAccessDomainBackUpTeamAdmin = backupTeamAdmins.get(0);
                modifiedAccessDomainBackUpTeamAdmin.setRoleId(teamAdminRoleId);
                modifiedAccessDomainBackUpTeamAdmin.setRole(teamAdminRole);
                accessDomainRepository.save(modifiedAccessDomainBackUpTeamAdmin);
                newTeamAdminAccountId = modifiedAccessDomainBackUpTeamAdmin.getAccountId();
                userAccountNewTeamAdmin = userAccountRepository.findByAccountIdAndIsActive(newTeamAdminAccountId, true);
            } else  {
                // no other team admin or backUp team admin exists -- make org admin as the new Team admin
                AccessDomain newTeamAdminAccessDomain = new AccessDomain();
//                AccessDomain oldTeamAdminAccessDomain = teamAdmins.stream().filter(accessDomain -> accessDomain.getAccountId().equals(activeAccountDbOfRemovedUser.getAccountId())).findFirst().get();
                AccessDomain oldTeamAdminAccessDomain = teamAdmins.get(0);
                BeanUtils.copyProperties(oldTeamAdminAccessDomain, newTeamAdminAccessDomain);
                newTeamAdminAccessDomain.setAccountId(orgAdminAccessDomain.getAccountId());
                newTeamAdminAccessDomain.setLastUpdatedDateTime(null);
                newTeamAdminAccessDomain.setCreatedDateTime(null);
                newTeamAdminAccessDomain.setAccessDomainId(null);
                userAccountNewTeamAdmin = userAccountRepository.findByAccountIdAndIsActive(orgAdminAccessDomain.getAccountId(), true);
                newTeamAdminAccountId = userAccountNewTeamAdmin.getAccountId();
                newTeamAdminAccessDomain.setUserAccount(userAccountNewTeamAdmin);
                accessDomainRepository.save(newTeamAdminAccessDomain);
            }
            teamRepository.updateOwnerAccountIdByTeamId(userAccountNewTeamAdmin, (long) teamId);
            accessDomainRepository.deactivateUserAccessDomainFromTeam(activeAccountDbOfRemovedUser.getAccountId(), roleIdToDelete.getRoleId(), teamId);
        }

        List<AccessDomain> allAccessDomainsOfRemovedUser = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, teamId, activeAccountDbOfRemovedUser.getAccountId(), true);
        boolean isAnyOtherValidRoleOfUserExistsInTeam = false;
        List<Integer> disAllowedRoleIds = List.of(teamAdminRoleId, backUpTeamAdminRoleId, RoleEnum.TEAM_VIEWER.getRoleId(), roleIdToDelete.getRoleId());
        for(AccessDomain accessDomain: allAccessDomainsOfRemovedUser) {
            if(!disAllowedRoleIds.contains(accessDomain.getRoleId())) {
                isAnyOtherValidRoleOfUserExistsInTeam = true;
                break;
            }
        }

        if(!isAnyOtherValidRoleOfUserExistsInTeam) {
            OpenTaskAssignedToUserRequest openTaskAssignedToUserRequest = new OpenTaskAssignedToUserRequest();
            openTaskAssignedToUserRequest.setAccountIdAssigned(activeAccountDbOfRemovedUser.getAccountId());
            openTaskAssignedToUserRequest.setEntityTypeId(Constants.EntityTypes.TEAM);
            openTaskAssignedToUserRequest.setEntityId(teamId);
            HashMap<Long, List<OpenTaskDetails>> allOpenWorkItemOfDeletedUser = taskServiceImpl.getOpenTasksAssignedToUser(openTaskAssignedToUserRequest);

            if (allOpenWorkItemOfDeletedUser != null && !allOpenWorkItemOfDeletedUser.isEmpty()) {
                if (request.getTaskIdAssignedToList() == null || !Objects.equals(allOpenWorkItemOfDeletedUser.get(teamId).size(), request.getTaskIdAssignedToList().size())) {
                    throw new ValidationFailedException("Please assign all work item of deleted member to other member");
                }
            }

            // cancel the future meetings organized by the deleted account and remove the user as attendee from all future meetings
            meetingService.cancelOrganizedMeetingsByUserRemovedInEntityAndSendNotification(Constants.EntityTypes.TEAM, teamId, activeAccountDbOfRemovedUser.getAccountId(), newTeamAdminAccountId,timeZone);
            // remove the user as an attendee from all the future meetings
            meetingService.removeUserAsAttendeeFromAllFutureMeetings(Constants.EntityTypes.TEAM, teamId, activeAccountDbOfRemovedUser.getAccountId(), newTeamAdminAccountId, timeZone);
            // modify the user preference if the user's preferred team is the team from which he is being removed
            userPreferenceService.editUserPreferenceOnUserRemovalFromEntity(Constants.EntityTypes.TEAM, teamId, activeAccountDbOfRemovedUser.getAccountId());
            // remove the deleted account as mentor/ observer
            taskServiceImpl.removeDeletedAccountAsMentorObserverAssignedToImmediateAttention(activeAccountDbOfRemovedUser.getAccountId(), Constants.EntityTypes.TEAM, teamId, newTeamAdminAccountId, request.getTaskIdAssignedToList(), timeZone);
            // remove the deleted account from the shared sticky notes
            stickyNoteService.removeDeletedAccountFromStickyNotesSharedList(Constants.EntityTypes.TEAM, teamId, activeAccountDbOfRemovedUser.getAccountId());
            // remove member from sprint of this team
            sprintService.removeMemberFromAllSprintInTeam (teamId, activeAccountDbOfRemovedUser.getAccountId(), accountIdsOfRequestor);
        }

        // if there is no role exist in the team, we are removing that member from the SystemGroup of Conversation.
        Map<Long, Long> accountIdOccurrencesMap = allActiveAccounts.stream().collect(Collectors.groupingBy(accountId -> accountId, Collectors.counting()));
        if(accountIdOccurrencesMap.get(activeAccountDbOfRemovedUser.getAccountId()) == 1){
            conversationService.removeUsersFromGroup(List.of(activeAccountDbOfRemovedUser.getFkUserId().getUserId()), teamId, Constants.EntityTypes.TEAM, activeAccountDbOfRemovedUser.getFkUserId());
        }
        // create audit of the deleted member
        Audit insertedAudit = auditService.auditForDeletedTeamMembers(teamId, email, roleName);
        secondaryDatabaseService.deleteDataFromSecondaryDatabase(email);

        try {
            List<HashMap<String, String>> payload = notificationService.removeAccessDomainNotification(activeAccountDbOfRemovedUser, foundTeam, roleName, timeZone);
            taskServiceImpl.sendPushNotification(payload);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Notification can not be created for deleted access domain. Caught Error: " + e, new Throwable(allStackTraces));
        }
    }

    //  to add list of team members
//    public void addAccessDomains(Integer teamId, List<AccessDomain> accessDomains, String timeZone, String accountIds) {
//        for (AccessDomain accessDomain : accessDomains) {
//            ExampleMatcher modelMatcher = ExampleMatcher.matching()
//                    .withIgnorePaths("accessDomainId")
//                    .withMatcher("accountId", ignoreCase())
//                    .withMatcher("entityId", ignoreCase())
//                    .withMatcher("roleId", ignoreCase());
//            AccessDomain probe = new AccessDomain();
//            probe.setAccountId(accessDomain.getAccountId());
//            probe.setEntityId(teamId);
//            probe.setRoleId(accessDomain.getRoleId());
//            Example<AccessDomain> example = Example.of(probe, modelMatcher);
//            boolean isAccessDomainExists = accessDomainRepository.exists(example);
//
//            // if it is the first team for the account, we add the team as default preference of the user
//            Long userIdOfAccount = userAccountRepository.findUserIdByAccountId(accessDomain.getAccountId());
//            List<Long> accountIdsOfUser = userAccountRepository.findAllAccountIdsByUserIdAndIsActive(userIdOfAccount, true);
//            if(accessDomainRepository.findByEntityIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, accountIdsOfUser, true).isEmpty()) {
//                Optional<UserPreference> userPreferenceOptional = userPreferenceRepository.findById(userIdOfAccount);
//                if(userPreferenceOptional.isPresent()) {
//                    UserPreference userPreference = userPreferenceOptional.get();
//                    userPreference.setTeamId(Long.valueOf(teamId));
//                    userPreferenceRepository.save(userPreference);
//                }
//            }
//
//            if (!isAccessDomainExists) {
//                accessDomain.setEntityId(teamId);
//                AccessDomain accessDomainInserted = accessDomainRepository.save(accessDomain);
//                try {
//                    List<HashMap<String, String>> payload = notificationService.newAccessDomainNotification(teamId, accessDomain, timeZone, accountIds);
//                    taskServiceImpl.sendPushNotification(payload);
//                } catch (Exception e) {
//                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
//                    logger.error("Notification can not be created for new access domain. Caught Error: " + e, new Throwable(allStackTraces));
//                }
//                try {
//                    boolean isMemberAdded = openFireService.addMemberInChatRoom(accessDomainInserted.getEntityId(), accessDomainInserted.getAccountId());
//                } catch (OpenfireException e){
//                    e.printStackTrace();
//                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
//                    logger.error("Something went wrong in call to openfire add-member method: Not able to add-member in chat room " + e, new Throwable(allStackTraces));
//                    ThreadContext.clearMap();
//                }
//                Long teamIdLong = (long) teamId;
//                secondaryDatabaseService.insertDataInSecondaryDatabase(teamIdLong, accessDomainInserted);
//                Audit createdAudit = auditService.auditForAddedTeamMembers(teamId, accessDomainInserted);
//            }
//        }
//    }

    public void addAndEditAccessDomains(Long teamId, List<AccessDomain> accessDomains, String timeZone, String accountIds) throws IllegalAccessException {
        long modifierAccountId;
        try{
            modifierAccountId = Long.parseLong(accountIds);
        } catch (Exception e) {
            throw new IllegalArgumentException("Expects single accountId in header");
        }
        Team team = teamRepository.findByTeamId(teamId);
        Long orgId = team.getFkOrgId().getOrgId();
        Long projectId = team.getFkProjectId().getProjectId();

        Boolean isModifiedAccountOrgAdminOrBackupOrgAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId,
                List.of(modifierAccountId), List.of(RoleEnum.ORG_ADMIN.getRoleId(), RoleEnum.BACKUP_ORG_ADMIN.getRoleId()), true);
        Boolean isModifierAccountTeamAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId,
                List.of(modifierAccountId), List.of(RoleEnum.TEAM_ADMIN.getRoleId()), true);
        Boolean isModifierAccountBackUpTeamAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId,
                List.of(modifierAccountId), List.of(RoleEnum.BACKUP_TEAM_ADMIN.getRoleId()), true);
        Boolean isModifierAccountTeamViewer = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId,
                List.of(modifierAccountId), List.of(RoleEnum.TEAM_VIEWER.getRoleId()), true);
        Boolean isModifierAccountProjectAdminOrBackUpProjectAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, projectId,
                List.of(modifierAccountId), List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId()), true);

        if(!isModifiedAccountOrgAdminOrBackupOrgAdmin && !isModifierAccountTeamAdmin && !isModifierAccountBackUpTeamAdmin && !isModifierAccountProjectAdminOrBackUpProjectAdmin) throw new ValidationFailedException("You're not authorized to modify roles within the team.");

        validateProjectManagerRole (accessDomains, modifierAccountId, teamId);

        AccessDomain newAccessDomainInserted = null, updatedAccessDomain = null;
        for (AccessDomain accessDomain : accessDomains) {
            Long accountIdOfUser = accessDomain.getAccountId();
            Integer newRoleId = accessDomain.getRoleId();
            MemberDetailsTeam memberDetailsInTeam = accessDomain.getMemberDetailsTeam();
            if (memberDetailsInTeam == null) {
                MemberDetails memberDetailsDb = memberDetailsRepository.findByEntityTypeIdAndEntityIdAndAccountId(Constants.EntityTypes.TEAM, accessDomain.getEntityId(), accessDomain.getAccountId());
                MemberDetailsTeam memberDetailsTeamDefault = new MemberDetailsTeam();
                if (memberDetailsDb != null) {
                    BeanUtils.copyProperties(memberDetailsDb, memberDetailsTeamDefault);
                    memberDetailsInTeam = memberDetailsTeamDefault;
                }
                else {
                    memberDetailsTeamDefault.setWorkStatus(com.tse.core_application.constants.Constants.WorkStatus.FULL_TIME);
                    memberDetailsTeamDefault.setWorkMinutes(entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(orgId).getSecond());
                    accessDomain.setMemberDetailsTeam(memberDetailsTeamDefault);
                    memberDetailsInTeam = memberDetailsTeamDefault;
                }
            } else if (memberDetailsInTeam.getWorkStatus() == null || memberDetailsInTeam.getWorkMinutes() == null) {
                throw new ValidationFailedException("Missing workStatus or workMinutes value");
            }

            // if the exact same record already exists in the accessDomain then no processing is required
            if(doActiveAccessDomainExists(accessDomain, teamId)) {
                updateMemberDetails(memberDetailsInTeam, accessDomain);
                continue;
            }

            // -------- if it is the first team for the account, we add the team as default preference of the user -----
            Long userIdOfAccount = userAccountRepository.findUserIdByAccountId(accountIdOfUser);
            List<Long> allAccountIdsOfUser = userAccountRepository.findAllAccountIdsByUserIdAndIsActive(userIdOfAccount, true);
            if(accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, allAccountIdsOfUser, true).isEmpty()) {
                Optional<UserPreference> userPreferenceOptional = userPreferenceRepository.findById(userIdOfAccount);
                if(userPreferenceOptional.isPresent()) {
                    UserPreference userPreference = userPreferenceOptional.get();
                    if (userPreference.getTeamId() == null || userPreference.getProjectId() == null || userPreference.getOrgId() == null) {
                        userPreference.setTeamId(team.getTeamId());
                        userPreference.setProjectId(team.getFkProjectId().getProjectId());
                        userPreference.setOrgId(team.getFkOrgId().getOrgId());
                    }
                    userPreferenceRepository.save(userPreference);
                }
            }

            Boolean userRoleUpdated = true;
            List<AccessDomain> currentRolesOfUserInTeam = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, teamId, accountIdOfUser, true);
            AccessDomain userNonAdminRoleAccessDomain = null, userAdminRoleAccessDomain = null;
            Integer oldRoleId = 1; // setting it as 1 for default value
            // assumption: that the user can have a single non admin role and single admin role
            for (AccessDomain domain : currentRolesOfUserInTeam) {
                if (domain.getRoleId() >= 1 && domain.getRoleId() <= 15 && domain.getRoleId() != TEAM_VIEWER) {
                    userNonAdminRoleAccessDomain = domain;
                } else if (domain.getRoleId() == TEAM_ADMIN_ROLE_ID || domain.getRoleId() == BACKUP_ADMIN_ROLE_ID || domain.getRoleId() == TEAM_VIEWER) {
                    userAdminRoleAccessDomain = domain;
                }
            }

            if (newRoleId >= RoleEnum.TASK_BASIC_USER.getRoleId() && newRoleId <= RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId() && newRoleId != RoleEnum.TEAM_VIEWER.getRoleId()) {
                if (userNonAdminRoleAccessDomain != null) {
                    oldRoleId = userNonAdminRoleAccessDomain.getRoleId();
                    // If any role already exists then update that record
                    userNonAdminRoleAccessDomain.setRoleId(newRoleId);
                    updatedAccessDomain = accessDomainRepository.save(userNonAdminRoleAccessDomain);
                } else {
                    accessDomain.setEntityTypeId(Constants.EntityTypes.TEAM);
                    accessDomain.setEntityId(teamId);
                    accessDomain.setIsActive(true);
                    newAccessDomainInserted = accessDomainRepository.save(accessDomain);
                    if (accessDomain.getAddInSprint()) {
                        // ZZZZZZ 14-04-2025
                        sprintService.addMemberInAllSprintInTeam(teamId, accessDomain.getAccountId(), List.of(modifierAccountId), timeZone);
                    }
                    userRoleUpdated = false;
                    addUserInConversationGroup(userIdOfAccount, teamId);
                }
            } else if (newRoleId == TEAM_ADMIN_ROLE_ID || newRoleId == BACKUP_ADMIN_ROLE_ID || newRoleId == TEAM_VIEWER) {
                if (!isModifiedAccountOrgAdminOrBackupOrgAdmin
                        && !isModifierAccountProjectAdminOrBackUpProjectAdmin
                        && !isModifierAccountTeamAdmin) {

                    throw new ValidationFailedException("Only team admin or higher admin can edit admin roles of team.");

                } else if (isModifierAccountTeamAdmin
                        && accessDomain.getAccountId().equals(modifierAccountId)
                        && (accessDomain.getRoleId() == TEAM_ADMIN_ROLE_ID
                        || accessDomain.getRoleId() == BACKUP_ADMIN_ROLE_ID
                        || accessDomain.getRoleId() == TEAM_VIEWER)) {

                    throw new ValidationFailedException("Team Admin can't edit self admin roles");
                }


                if (userAdminRoleAccessDomain != null) {
                    // Update existing admin role
                    oldRoleId = userAdminRoleAccessDomain.getRoleId();
                    userAdminRoleAccessDomain.setRoleId(newRoleId);
                    updatedAccessDomain = accessDomainRepository.save(userAdminRoleAccessDomain);
                } else {
                    // create new admin role
                    accessDomain.setEntityTypeId(Constants.EntityTypes.TEAM);
                    accessDomain.setEntityId(teamId);
                    accessDomain.setIsActive(true);
                    newAccessDomainInserted = accessDomainRepository.save(accessDomain);
                    userRoleUpdated = false;
                    //add user to conversations group as per the entityId and entityTypeId
                    addUserInConversationGroup(userIdOfAccount, teamId);
                }
            }

            // update the member details after the access domain is saved
            updateMemberDetails(memberDetailsInTeam, accessDomain);

            // Mohan: to create an edit access domain notification in future if required
            try {
                if (!Objects.equals(accessDomain.getAccountId(), modifierAccountId)) {
                    List<HashMap<String, String>> payload = notificationService.newAccessDomainNotification(teamId, accessDomain, timeZone, accountIds, userRoleUpdated);
                    taskServiceImpl.sendPushNotification(payload);
                }
            } catch (Exception e) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Notification can not be created for new access domain. Caught Error: " + e, new Throwable(allStackTraces));
            }

            if (newAccessDomainInserted != null) {
                if (enableOpenfire) {
                    try {
                        boolean isMemberAdded = openFireService.addMemberInChatRoom(newAccessDomainInserted.getEntityId(), newAccessDomainInserted.getAccountId());
                    } catch (OpenfireException e) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("Something went wrong in call to openfire add-member method: Not able to add-member in chat room " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                    }
                }

                secondaryDatabaseService.insertDataInSecondaryDatabase(teamId, newAccessDomainInserted);
                auditService.auditForAddedTeamMembers(teamId, newAccessDomainInserted);
            } else if (updatedAccessDomain != null) {
                Audit editedAudit = auditService.auditForEditedTeamMembers(teamId, updatedAccessDomain, oldRoleId);
            }
        }
    }

    private void updateMemberDetails(MemberDetailsTeam memberDetailsTeam, AccessDomain accessDomain) {
        MemberDetails memberDetailsDb = memberDetailsRepository.findByEntityTypeIdAndEntityIdAndAccountId(Constants.EntityTypes.TEAM, accessDomain.getEntityId().longValue(), accessDomain.getAccountId());
        boolean isUpdated = false, isAnySprintActiveInTeam = false;
        isAnySprintActiveInTeam = sprintRepository.existsByEntityTypeIdAndEntityIdAndSprintStatusNotIn(Constants.EntityTypes.TEAM, accessDomain.getEntityId().longValue(), List.of(Constants.SprintStatusEnum.COMPLETED.getSprintStatusId(), Constants.SprintStatusEnum.DELETED.getSprintStatusId(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId()));

        if (memberDetailsDb != null) {
            if (isAnySprintActiveInTeam && (!Objects.equals(memberDetailsDb.getWorkStatus(), memberDetailsTeam.getWorkStatus()) || !Objects.equals(memberDetailsDb.getWorkMinutes(), memberDetailsTeam.getWorkMinutes()))) {
                throw new ValidationFailedException("Work status or work minutes cannot be modified while a sprint is active for the team. Complete the ongoing sprint before making changes to member's work details.");
            }

            if (!Objects.equals(memberDetailsDb.getWorkStatus(), memberDetailsTeam.getWorkStatus())) {
                memberDetailsDb.setWorkStatus(memberDetailsTeam.getWorkStatus());
                isUpdated = true;
            }
            if (!Objects.equals(memberDetailsDb.getWorkMinutes(), memberDetailsTeam.getWorkMinutes())) {
                memberDetailsDb.setWorkMinutes(memberDetailsTeam.getWorkMinutes());
                isUpdated = true;
            }
            if (isUpdated) {
                memberDetailsRepository.save(memberDetailsDb);
            }
        } else {
            MemberDetails memberDetails = new MemberDetails();
            BeanUtils.copyProperties(memberDetailsTeam, memberDetails);
            memberDetails.setAccountId(accessDomain.getAccountId());
            memberDetails.setEntityTypeId(accessDomain.getEntityTypeId());
            memberDetails.setEntityId(accessDomain.getEntityId().longValue());
            memberDetailsRepository.save(memberDetails);
        }
    }


    public boolean doActiveAccessDomainExists(AccessDomain accessDomain, Long teamId) {
        ExampleMatcher modelMatcher = ExampleMatcher.matching()
                    .withIgnorePaths("accessDomainId")
                    .withMatcher("accountId", ignoreCase())
                    .withMatcher("entityTypeId", ignoreCase())
                    .withMatcher("entityId", ignoreCase())
                    .withMatcher("roleId", ignoreCase())
                    .withMatcher("isActive", ignoreCase());
            AccessDomain probe = new AccessDomain();
            probe.setAccountId(accessDomain.getAccountId());
            probe.setEntityId(teamId);
            probe.setEntityTypeId(Constants.EntityTypes.TEAM);
            probe.setRoleId(accessDomain.getRoleId());
            probe.setIsActive(true);
            Example<AccessDomain> example = Example.of(probe, modelMatcher);
            return accessDomainRepository.exists(example);
    }

        //  to add in accessDomain when new org is registered
    public void addAccessDomainForNewOrg(UserAccount userAccount, Organization organization, Project project) {
        List<AccessDomain> accessDomainList = new ArrayList<>();
        AccessDomain accessDomainToAdd = new AccessDomain();
        accessDomainToAdd.setAccountId(userAccount.getAccountId());
        accessDomainToAdd.setEntityId(organization.getOrgId());
        accessDomainToAdd.setEntityTypeId(Constants.EntityTypes.ORG);
        accessDomainToAdd.setRoleId(RoleEnum.ORG_ADMIN.getRoleId());
        accessDomainToAdd.setWorkflowTypeId(null);
        accessDomainList.add(accessDomainToAdd);

        AccessDomain accessDomainToAddForProject = new AccessDomain();
        accessDomainToAddForProject.setAccountId(userAccount.getAccountId());
        accessDomainToAddForProject.setEntityId(project.getProjectId());
        accessDomainToAddForProject.setEntityTypeId(Constants.EntityTypes.PROJECT);
        accessDomainToAddForProject.setRoleId(RoleEnum.PROJECT_ADMIN.getRoleId());
        accessDomainToAddForProject.setWorkflowTypeId(null);
        accessDomainList.add(accessDomainToAddForProject);

        AccessDomain accessDomainForProjectManagerToAddInProject = new AccessDomain();
        accessDomainForProjectManagerToAddInProject.setAccountId(userAccount.getAccountId());
        accessDomainForProjectManagerToAddInProject.setEntityId(project.getProjectId());
        accessDomainForProjectManagerToAddInProject.setEntityTypeId(Constants.EntityTypes.PROJECT);
        accessDomainForProjectManagerToAddInProject.setRoleId(RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId());
        accessDomainForProjectManagerToAddInProject.setWorkflowTypeId(null);
        accessDomainList.add(accessDomainForProjectManagerToAddInProject);

        AccessDomain accessDomainToAddBu = new AccessDomain();
        accessDomainToAddBu.setAccountId(userAccount.getAccountId());
        accessDomainToAddBu.setEntityId(project.getBuId());
        accessDomainToAddBu.setEntityTypeId(Constants.EntityTypes.BU);
        accessDomainToAddBu.setRoleId(RoleEnum.BU_ADMIN.getRoleId());
        accessDomainToAddBu.setWorkflowTypeId(null);
        accessDomainList.add(accessDomainToAddBu);

        accessDomainRepository.saveAll(accessDomainList);
    }

    public List<CustomAccessDomain> findAllActiveAccessDomainByAccountId(Long accountId) {
        List<CustomAccessDomain> accessDomainList = new ArrayList<>();
        List<AccessDomain> accessDomains = accessDomainRepository.findByAccountIdAndIsActive(accountId, true);
        for (AccessDomain accessDomain: accessDomains) {
            CustomAccessDomain customAccessDomainToAdd = new CustomAccessDomain();
            customAccessDomainToAdd.setAccountId(accessDomain.getAccountId());
            customAccessDomainToAdd.setEntityId(accessDomain.getEntityId());
            customAccessDomainToAdd.setEntityTypeId(accessDomain.getEntityTypeId());
            customAccessDomainToAdd.setRoleId(accessDomain.getRoleId());
            customAccessDomainToAdd.setWorkflowTypeId(accessDomain.getWorkflowTypeId());
            boolean isAccessDomainAdded = accessDomainList.add(customAccessDomainToAdd);
        }
        return accessDomainList;
    }

    public List<CustomAccessDomain> getAllActiveAccessDomainsByAllAccountIds(List<Long> accountIds) {
        List<CustomAccessDomain> accessDomainList = new ArrayList<>();
        if (!accountIds.isEmpty()) {
            for (Long accountId: accountIds) {
                List<AccessDomain> accessDomains = accessDomainRepository.findByAccountIdAndIsActive(accountId, true);
                for (AccessDomain accessDomain: accessDomains) {
                    CustomAccessDomain customAccessDomainToAdd = new CustomAccessDomain();
                    customAccessDomainToAdd.setAccountId(accessDomain.getAccountId());
                    customAccessDomainToAdd.setRoleId(accessDomain.getRoleId());
                    customAccessDomainToAdd.setWorkflowTypeId(accessDomain.getWorkflowTypeId());
                    customAccessDomainToAdd.setEntityId(accessDomain.getEntityId());
                    customAccessDomainToAdd.setEntityTypeId(accessDomain.getEntityTypeId());
                    boolean isAccessDomainAdded =  accessDomainList.add(customAccessDomainToAdd);
                }
            }
            return accessDomainList;
        } else {
          return accessDomainList;
        }
    }

    public List<CustomAccessDomain> getAccessDomainByAccountIdAndEntityId(Long accountId, Long entityId) {
        List<AccessDomain> accessDomainDb = accessDomainRepository.findByAccountIdAndEntityIdAndIsActive(accountId, entityId, true);
        List<CustomAccessDomain> accessDomains = new ArrayList<>();
        for(AccessDomain accessDomain: accessDomainDb) {
            CustomAccessDomain customAccessDomain = new CustomAccessDomain();
            customAccessDomain.setAccountId(accessDomain.getAccountId());
            customAccessDomain.setEntityId(accessDomain.getEntityId());
            customAccessDomain.setEntityTypeId(accessDomain.getEntityTypeId());
            customAccessDomain.setRoleId(accessDomain.getRoleId());
            customAccessDomain.setWorkflowTypeId(accessDomain.getWorkflowTypeId());
            accessDomains.add(customAccessDomain);
        }
        return accessDomains;
    }

    public List<CustomAccessDomain> getAccessDomainsByAccountIdsAndEntityTypeIdAndIsActive(Integer entityTypeId, List<Long> accountIds) {
        List<AccessDomain> accessDomainsFoundDb = accessDomainRepository.findByEntityTypeIdAndAccountIdInAndIsActive(entityTypeId, accountIds, true);
        List<CustomAccessDomain> finalCustomAccessDomains = new ArrayList<>();

        for(AccessDomain accessDomain: accessDomainsFoundDb) {
            CustomAccessDomain customAccessDomainToAdd = new CustomAccessDomain();
            customAccessDomainToAdd.setWorkflowTypeId(accessDomain.getWorkflowTypeId());
            customAccessDomainToAdd.setEntityId(accessDomain.getEntityId());
            customAccessDomainToAdd.setAccountId(accessDomain.getAccountId());
            customAccessDomainToAdd.setRoleId(accessDomain.getRoleId());
            customAccessDomainToAdd.setEntityTypeId(accessDomain.getEntityTypeId());
            finalCustomAccessDomains.add(customAccessDomainToAdd);
        }
        return finalCustomAccessDomains;
    }

    public List<CustomAccessDomain> getAccessDomainsByAccountIdsAndEntityId(Long entityId, List<Long> accountIds) {
        List<AccessDomain> accessDomainsFoundDb = accessDomainRepository.findByEntityIdAndAccountIdInAndIsActive(entityId, accountIds, true);
        List<CustomAccessDomain> finalCustomAccessDomains = new ArrayList<>();

        for(AccessDomain accessDomain: accessDomainsFoundDb) {
            CustomAccessDomain customAccessDomainToAdd = new CustomAccessDomain();
            customAccessDomainToAdd.setWorkflowTypeId(accessDomain.getWorkflowTypeId());
            customAccessDomainToAdd.setEntityId(accessDomain.getEntityId());
            customAccessDomainToAdd.setAccountId(accessDomain.getAccountId());
            customAccessDomainToAdd.setRoleId(accessDomain.getRoleId());
            customAccessDomainToAdd.setEntityTypeId(accessDomain.getEntityTypeId());
            finalCustomAccessDomains.add(customAccessDomainToAdd);
        }
        return finalCustomAccessDomains;
    }

    /** gets all active team members */
    public List<AccountId> getAllActiveDistinctTeamMembers(Long teamId) {
        return accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, teamId, true);
    }

    public List<Long> getActiveAccountIdsOfHigherRoleMembersInTeam(Long teamId, Integer roleId){
        List<Integer> roleIds = new ArrayList<>(Constants.HIGHER_ROLE_IDS);
        if(roleId!=null)
            roleIds.removeIf(role -> (role < roleId ));
        List<AccountId> accountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, roleIds, true);
        return accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toList());
    }

    // This function validates that the account should exist in the team and org and the team should exists in the org.
    public Boolean validateOrgAndTeamInRequest(Long orgId, Long teamId, String accountIds){

        boolean isValidOrg = false, isValidTeam = false;

        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

       List<Integer> allValidTeams =  accessDomainRepository.findEntityIdByEntityTypeIdAndAccountIdsInAndIsActive(Constants.EntityTypes.TEAM, accountIdsList);
       HashSet<Long> allValidTeamsSet = new HashSet<>(allValidTeams.stream().map(id -> (long)(int)id).collect(Collectors.toList()));

        for(Long accountId : accountIdsList) {
           OrgId  org_id = userAccountRepository.findOrgIdByAccountIdAndIsActive(accountId, true);
            if(Objects.equals(org_id.getOrgId(), orgId)){
                isValidOrg = true;
                break;
            }
        }

        if(isValidOrg) {
           Long validOrgByTeam = teamRepository.findFkOrgIdOrgIdByTeamId(teamId);


            if (allValidTeamsSet.contains(teamId) && Objects.equals(validOrgByTeam, orgId)) {
                isValidTeam = true;
            }
        }

        return isValidOrg && isValidTeam;
    }

    public void deactivateAllAccessDomainsInAllTeams(Long accountId) {
        accessDomainRepository.deactivateUserAllAccessDomainsInAllTeams(accountId);
    }

    /**
     * filter request to check the condition that a user can have max one non admin role and max one admin role in a team
     * remove any lower roles entries from the request for a given accountId
     */
    public List<AccessDomain> filterAccessDomain(List<AccessDomain> accessDomains) {
        Map<Long, AccessDomain> highestNonAdminRoles = new HashMap<>();
        Map<Long, AccessDomain> highestAdminRoles = new HashMap<>();

        for (AccessDomain ad : accessDomains) {

            if (!Objects.equals(ad.getEntityTypeId(), Constants.EntityTypes.TEAM) || !Constants.TEAM_ROLE_IDS.contains(ad.getRoleId())) {
                throw new ValidationFailedException("Invalid Request: Only team roles can be added in a team");
            }

            if (!userAccountRepository.existsByAccountIdAndIsActive(ad.getAccountId(), true)) {
                throw new ValidationFailedException("User with account id " + ad.getAccountId() + " does not belong to the organization");
            }

            // check if the role is admin or non-admin based on the roleId
            boolean isAdmin = ad.getRoleId() == 101 || ad.getRoleId() == 102;

            Map<Long, AccessDomain> relevantMap = isAdmin ? highestAdminRoles : highestNonAdminRoles;
            AccessDomain currentHighest = relevantMap.get(ad.getAccountId());

            // if we have not encountered this account yet or the current role is higher than the one in the map
            if (currentHighest == null || ad.getRoleId() > currentHighest.getRoleId()) {
                relevantMap.put(ad.getAccountId(), ad);
            }
        }

        // Combine the highest roles from both admin and non-admin into a single list
        List<AccessDomain> filteredAccessDomains = new ArrayList<>(highestNonAdminRoles.values());
        filteredAccessDomains.addAll(highestAdminRoles.values());

        return filteredAccessDomains;
    }

    public boolean IsActiveAccessDomain(Long accountId, Long entityId) {

        if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, entityId, accountId, true)) {
            return true;
        }
        return false;
    }

    public void ifOrgPersonalAndTeamDefault (String accountId, Long teamId) throws IllegalAccessException {
        Long headerAccountId = Long.parseLong(accountId);
        UserAccount userAccount = userAccountRepository.findByAccountId(headerAccountId);
        if (Objects.equals(userAccount.getOrgId().intValue(), Constants.OrgIds.PERSONAL) && teamRepository.findByTeamId(teamId).getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
            throw new IllegalAccessException("Access denied: Users in personal organization are not allowed to access this feature");
        }
    }

    public List<EmailFirstLastAccountId> getEntityMembers(Integer entityTypeId, Long entityId, String accountIds) {
        List<EmailFirstLastAccountId> responseMembersList;
        List<Long> headerAccountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<AccountId> membersAccountIdList;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            membersAccountIdList = userAccountRepository.findAccountIdByOrgIdAndIsActive(entityId, true);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            membersAccountIdList= projectService.getprojectMembersAccountIdList(List.of(entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            membersAccountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, entityId, true);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.BU)) {
            membersAccountIdList= buService.getBuMembersAccountIdList(entityId);
        } else {
            throw new IllegalArgumentException("Invalid Request");
        }

        if (membersAccountIdList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> accountIdList = membersAccountIdList.stream()
                .map(AccountId::getAccountId)
                .collect(Collectors.toList());

        if (CommonUtils.containsAny(accountIdList, headerAccountIdList)) {
            responseMembersList = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(accountIdList);
            responseMembersList.sort(Comparator
                    .comparing(EmailFirstLastAccountId::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountId::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountId::getAccountId, Comparator.nullsLast(Long::compareTo))
            );
        } else {
            responseMembersList = Collections.emptyList();
        }
        return responseMembersList;
    }

    public void deactivateAllAccessDomainsInAllProjects(Long accountId) {
        accessDomainRepository.deactivateUserAllAccessDomainsInAllProjects(accountId);
    }

    public Boolean isOrgAminOrProjectAdmin (Integer entityTypeId, Long entityId, List<Long> accountIdsList) {
        Long orgId = 0L;
        Long projectId = null;

        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            orgId = entityId;
        } else {
            switch (entityTypeId) {
                case Constants.EntityTypes.PROJECT:
                    Project project = projectRepository.findByProjectId(entityId);
                    orgId = project.getOrgId();
                    projectId = project.getProjectId();
                    break;
                case Constants.EntityTypes.TEAM:
                    Team team = teamRepository.findByTeamId(entityId);
                    orgId = team.getFkOrgId().getOrgId();
                    projectId = team.getFkProjectId().getProjectId();
                    break;
            }
        }
        Boolean isAuthorized = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId,
                accountIdsList, List.of(RoleEnum.ORG_ADMIN.getRoleId()), true);
        if (!isAuthorized && projectId != null) isAuthorized = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, projectId,
                accountIdsList, List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId()), true);

        return isAuthorized;
    }

    public List<EmailFirstLastAccountIdIsActive> getAllEntityMembers(Integer entityTypeId, Long entityId, String accountIds) {
        List<EmailFirstLastAccountIdIsActive> responseMembersList;
        List<Long> headerAccountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<AccountId> membersAccountIdList;
        Long orgId;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            membersAccountIdList = userAccountRepository.findAccountIdByOrgId(entityId);
            orgId = entityId;
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            membersAccountIdList = projectService.getAllprojectMembersAccountIdList(entityId);
            orgId = projectRepository.findOrgIdByProjectId(entityId).getOrgId();
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            membersAccountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, entityId);
            orgId = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.BU)) {
            membersAccountIdList = buService.getAllBuMembersAccountIdList(entityId);
            orgId = buRepository.findOrgIdBybuId(entityId).getOrgId();
        } else {
            throw new IllegalArgumentException("Invalid Request");
        }
        Organization organization = organizationRepository.findByOrgId(orgId);
        UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(organization.getOwnerEmail(), orgId, true);
        Long orgAdmin = userAccount.getAccountId();
        Set<Long> accountIdSet = membersAccountIdList.stream()
                .map(AccountId::getAccountId)
                .collect(Collectors.toSet());
        if (headerAccountIdList.contains(orgAdmin)) {
            accountIdSet.add(orgAdmin);
        }
        List<Long> accountIdList = new ArrayList<>(accountIdSet);
        if (accountIdList.isEmpty()) {
            return Collections.emptyList();
        }
        if (CommonUtils.containsAny(accountIdList, headerAccountIdList)) {
            responseMembersList = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(accountIdList);

            if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
                responseMembersList = setTeamMemberIsActiveOrNotInTeam (responseMembersList, entityTypeId, entityId);
            }
            if (responseMembersList != null && !responseMembersList.isEmpty()) {
                responseMembersList.sort(Comparator
                        .comparing((EmailFirstLastAccountIdIsActive status) -> {
                            if (status.getIsActive() == null) return 2;
                            return status.getIsActive() ? 0 : 1;
                        })
                        .thenComparing(EmailFirstLastAccountIdIsActive::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(EmailFirstLastAccountIdIsActive::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(EmailFirstLastAccountIdIsActive::getAccountId, Comparator.nullsLast(Long::compareTo))
                );
            }
        } else {
            responseMembersList = Collections.emptyList();
        }
        return responseMembersList;
    }

    public List<EmailFirstLastAccountIdIsActive> setTeamMemberIsActiveOrNotInTeam(List<EmailFirstLastAccountIdIsActive> memberDetailsInOrg, Integer entityTypeId, Long entityId) {

        return memberDetailsInOrg.stream().map(member -> {
            if (Boolean.TRUE.equals(member.getIsActive())) {
                boolean isInTeam = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(
                        entityTypeId, entityId, member.getAccountId(), true);
                member.setIsActive(isInTeam);
            }
            return member;
        }).collect(Collectors.toList());
    }

    public void addProjectManagerOnTeamCreation(Team team) {
        List<Integer> roleIdList = Constants.ROLE_IDS_FOR_PROJECT_MANAGER_ON_TEAM_CREATION;

        List<AccessDomain> accessDomainList = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, team.getFkProjectId().getProjectId(), roleIdList, true);
        for (AccessDomain accessDomain : accessDomainList) {
            AccessDomain accessDomainToAdd = new AccessDomain();
            accessDomainToAdd.setAccountId(accessDomain.getAccountId());
            accessDomainToAdd.setEntityId(team.getTeamId());
            accessDomainToAdd.setEntityTypeId(Constants.EntityTypes.TEAM);
            accessDomainToAdd.setWorkflowTypeId(null);
            if (Objects.equals(accessDomain.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId())) {
                accessDomainToAdd.setRoleId(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
            }
            else {
                accessDomainToAdd.setRoleId(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
            }
            AccessDomain accessDomainAdded = accessDomainRepository.save(accessDomainToAdd);
        }

    }

    public Boolean isManangerInTeam (Long teamId, List<Long> accountIdsList) {
        return accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamId,
                accountIdsList, List.of(RoleEnum.TEAM_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.TEAM_MANAGER_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId()), true);
    }

    public void validateProjectManagerRole (List<AccessDomain> accessDomains, Long modifierAccountId, Long teamId) {
        for (AccessDomain accessDomain : accessDomains) {
            if (Objects.equals(accessDomain.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT) || Objects.equals(accessDomain.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT)) {
                throw new ValidationFailedException("Project manager role can't be added in team");
            }
            if (accessDomain.getRoleId() >= RoleEnum.TASK_BASIC_USER.getRoleId() && accessDomain.getRoleId() <= RoleEnum.TEAM_MANAGER_SPRINT.getRoleId()) {
                if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(Constants.EntityTypes.TEAM, teamId, accessDomain.getAccountId(), true, List.of(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId()))) {
                    UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accessDomain.getAccountId(), true);
                    throw new ValidationFailedException(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName() + " has project manager role so new non admin role can't be assigned");
                }
            }
        }
    }

    private void addUserInConversationGroup(Long userIdOfAccount, Long teamId){
        User user = userRepository.findByUserId(userIdOfAccount);
        ConversationGroup convGroup = conversationService.getGroup(teamId, (long) Constants.EntityTypes.TEAM, user);
        if(convGroup!=null && convGroup.getGroupId()!=null){
            conversationService.addUsersToGroup(convGroup, List.of(userIdOfAccount), user);
        }
    }

    public void validateOpenTaskAssignedToUser(RemoveTeamMemberRequest request) {
        List<Long> assignedToUserIds = request.getTaskIdAssignedToList()
                .stream()
                .map(TaskIdAssignedTo::getAccountIdAssignedTo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!assignedToUserIds.isEmpty()) {
            List<Long> entityIdsOfUsers = List.of(request.getTeamId());
            boolean hasAdminAssigned = accessDomainRepository.existsByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityIdsOfUsers, assignedToUserIds, Constants.TEAM_ADMIN_ROLE, true);
            List<Long> accountIdsOfAdminRole = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, List.of(request.getTeamId()), Constants.TEAM_ADMIN_ROLE, assignedToUserIds, true);
            List<Long> accountIdsOfNonAdminRole = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, List.of(request.getTeamId()), Constants.TEAM_NON_ADMIN_ROLE, assignedToUserIds, true);
            if (accountIdsOfAdminRole != null && accountIdsOfNonAdminRole != null && !accountIdsOfAdminRole.stream()
                    .filter(id -> !accountIdsOfNonAdminRole.contains(id))
                    .findAny()
                    .isEmpty()) {
                throw new IllegalArgumentException("Task cannot be assigned to Team Admin role.");
            }
        }
    }

    public List<Long> getAllEntityMembersByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId) {
        List<AccountId> membersAccountIdList;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            membersAccountIdList = userAccountRepository.findAccountIdByOrgId(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            membersAccountIdList = projectService.getAllprojectMembersAccountIdList(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            membersAccountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(
                    Constants.EntityTypes.TEAM, entityId);
        } else {
            throw new IllegalArgumentException("Invalid Request: Unsupported entityTypeId " + entityTypeId);
        }
        if (membersAccountIdList == null || membersAccountIdList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> accountIds = membersAccountIdList.stream()
                .map(AccountId::getAccountId)  // Assuming AccountId has getAccountId() method
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return accountIds;
    }

}
