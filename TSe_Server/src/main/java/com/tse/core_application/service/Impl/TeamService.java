package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.dto.conversations.GroupUpdateRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.*;
import com.tse.core_application.model.Notification;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.TeamIdentifierGenerator;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private static final Logger logger = LogManager.getLogger(TeamService.class.getName());

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RoleActionRepository roleActionRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecondaryDatabaseService secondaryDatabaseService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private RecurringMeetingRepository recurringMeetingRepository;
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;
    @Autowired
    private StickyNoteRepository stickyNoteRepository;
    @Autowired
    private LeavePolicyRepository leavePolicyRepository;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private InviteService inviteService;
    @Autowired
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ConversationService conversationService;


    // update team object
    public Team updateTeamInTeamTable(Team team, Long headerAccountId, User user) {
        Team teamDbToUpdate = teamRepository.findByTeamId(team.getTeamId());
        GroupUpdateRequest groupUpdateRequest = new GroupUpdateRequest();

        if (team.getTeamName() != null) {
            team.setTeamName(team.getTeamName().trim());
        }
        if (team.getTeamDesc() != null) {
            team.setTeamDesc(team.getTeamDesc().trim());
        }
        Organization teamOrg = organizationRepository.findByOrgId(team.getFkOrgId().getOrgId());
        validateTeamNameAndTeamCode(team, teamOrg, false);
        Boolean hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, teamDbToUpdate.getFkOrgId().getOrgId(),
                List.of(headerAccountId), List.of(RoleEnum.ORG_ADMIN.getRoleId()), true);
        if (!hasTeamUpdateAccess) hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.BU, teamDbToUpdate.getFkProjectId().getBuId(),
                List.of(headerAccountId), List.of(RoleEnum.BU_ADMIN.getRoleId()), true);
        if (!hasTeamUpdateAccess) hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, teamDbToUpdate.getFkProjectId().getProjectId(),
                List.of(headerAccountId), List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId()), true);
        if (!hasTeamUpdateAccess) hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamDbToUpdate.getTeamId(),
                List.of(headerAccountId), List.of(RoleEnum.TEAM_ADMIN.getRoleId(), RoleEnum.BACKUP_TEAM_ADMIN.getRoleId()), true);

        if (!hasTeamUpdateAccess) throw new ValidationFailedException("User not authorized to update the provided team");

        if (!Objects.equals(team.getTeamCode(), teamDbToUpdate.getTeamCode())) {
            throw new ValidationFailedException("Team Code can not be updated");
        }

        if (teamDbToUpdate.getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) && teamOrg.getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
            throw new ValidationFailedException("User not authorized to edit team");
        }
        //updating team title/desc in conversation DB
        if(team.getTeamName()!=null || team.getTeamDesc()!=null){
            if(team.getTeamName()!=null){
                groupUpdateRequest.setGroupName(team.getTeamName());
            }
            if (team.getTeamDesc()!=null){
                groupUpdateRequest.setGroupDesc(team.getTeamDesc());
            }
            conversationService.updateGroupDetails(team.getTeamId(), (long) Constants.EntityTypes.TEAM, groupUpdateRequest, user);
        }
        ArrayList<String> fieldsToUpdate = getFieldsToUpdate(team);
        for (String updateField : fieldsToUpdate) {
            try {

                Field field = Team.class.getDeclaredField(updateField);
                field.setAccessible(true);
                Object fieldValue = field.get(team);
                field.set(teamDbToUpdate, fieldValue);

            } catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        Team teamUpdated = teamRepository.save(teamDbToUpdate);
        Audit insertedAudit = auditService.auditForUpdateTeam(teamUpdated);
        if (fieldsToUpdate.contains("teamName")) {
            secondaryDatabaseService.updateTeamNameInSecondaryDatabase(teamUpdated.getTeamId(), teamUpdated.getTeamName());
        }
        if (fieldsToUpdate.contains("fkProjectId")) {
            modifyEntitiesOnProjectChange(teamUpdated);
        }

        return teamUpdated;
    }

    public void modifyEntitiesOnProjectChange(Team updatedTeam) {
        if (updatedTeam != null && updatedTeam.getFkProjectId() != null) {
            Long newProjectId = updatedTeam.getFkProjectId().getProjectId();
            Long teamId = updatedTeam.getTeamId();

            // Update Tasks
            List<Task> tasks = taskRepository.findByFkTeamIdTeamId(teamId);
            tasks.forEach(task -> task.setFkProjectId(updatedTeam.getFkProjectId()));
            taskRepository.saveAll(tasks);

            // Update TimeSheets
            List<TimeSheet> timeSheets = timeSheetRepository.findByTeamId(teamId);
            timeSheets.forEach(timeSheet -> timeSheet.setProjectId(newProjectId));
            timeSheetRepository.saveAll(timeSheets);

            // Update Notifications
            List<Notification> notifications = notificationRepository.findByTeamIdTeamId(teamId);
            notifications.forEach(notification -> notification.setProjectId(updatedTeam.getFkProjectId()));
            notificationRepository.saveAll(notifications);

            // Update Attendee
            List<Attendee> attendees = attendeeRepository.findByTeamId(teamId);
            attendees.forEach(attendee -> attendee.setProjectId(newProjectId));
            attendeeRepository.saveAll(attendees);

            // Update Leave Policy
            List<LeavePolicy> leavePolicies = leavePolicyRepository.findByTeamId(teamId);
            leavePolicies.forEach(leavePolicy -> leavePolicy.setProjectId(newProjectId));
            leavePolicyRepository.saveAll(leavePolicies);

            // Update Meeting
            List<Meeting> meetings = meetingRepository.findByTeamId(teamId);
            meetings.forEach(meeting -> meeting.setProjectId(newProjectId));
            meetingRepository.saveAll(meetings);

            // Update Recurring Meeting
            List<RecurringMeeting> recurringMeetings = recurringMeetingRepository.findByTeamId(teamId);
            recurringMeetings.forEach(recurringMeeting -> recurringMeeting.setProjectId(newProjectId));
            recurringMeetingRepository.saveAll(recurringMeetings);

            // Update Task History
            List<TaskHistory> taskHistories = taskHistoryRepository.findByFkTeamIdTeamId(teamId);
            taskHistories.forEach(taskHistory -> taskHistory.setFkProjectId(updatedTeam.getFkProjectId()));
            taskHistoryRepository.saveAll(taskHistories);

            // Update StickyNote
            List<StickyNote> stickyNotes = stickyNoteRepository.findByTeamId(teamId);
            stickyNotes.forEach(stickyNote -> stickyNote.setProjectId(newProjectId));
            stickyNoteRepository.saveAll(stickyNotes);
        }
    }


    //  get list of fields to update
    public ArrayList<String> getFieldsToUpdate(Team team) {
        Team teamDb = teamRepository.findByTeamId(team.getTeamId());
        ArrayList<String> arrayListFields = new ArrayList<String>();
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> mapTeam = objectMapper.convertValue(team, HashMap.class);
        HashMap<String, Object> mapTeamDb = objectMapper.convertValue(teamDb, HashMap.class);
        arrayList.add(mapTeamDb);
        arrayList.add(mapTeam);
        for (int i = 0; i < (arrayList.size() - 1); i++) {
            for (Map.Entry<String, Object> entry : arrayList.get(i).entrySet()) {
                String key = entry.getKey();
                // Skip fkProjectId as it is handled separately
                if ("fkProjectId".equals(key)) continue;

                Object value1 = entry.getValue();
                Object value2 = arrayList.get(i + 1).get(key);
                if (value2 != null) {
                    if (!value2.equals(value1)) {
                        arrayListFields.add(key);
                    }
                }
            }
        }

        // Special handling for project change
        Project currentProject = teamDb.getFkProjectId();
        Project newProject = projectService.getProjectByProjectId(team.getFkProjectId().getProjectId());
        if (currentProject != null && newProject != null && !currentProject.getProjectId().equals(newProject.getProjectId())) {
            if (currentProject.getProjectType().equals(com.tse.core_application.constants.Constants.ProjectType.DEFAULT_PROJECT) &&
                    newProject.getProjectType().equals(com.tse.core_application.constants.Constants.ProjectType.USER_PROJECT)) {
                arrayListFields.add("fkProjectId");
            } else {
                throw new ValidationFailedException("Invalid project change");
            }
        }

        return arrayListFields;
    }

    public List<Team> getAllTeamsForCreateTask(String email, Long orgId) {
        User user = userRepository.findByPrimaryEmail(email);
        UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(orgId, user.getUserId(), true);
        if (userAccount == null && user.getIsUserManaging() != null && user.getIsUserManaging()) {
            List<Long> userList = userRepository.findAllUserIdByManagingUserId(user.getUserId());
            userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdInAndIsActive(orgId, userList, true);
        }
        List<AccessDomain> accessDomainList = new ArrayList<>();
        if (userAccount != null) {
            accessDomainList = accessDomainRepository.findByAccountIdAndIsActive(userAccount.getAccountId(), true);
        }
        List<Integer> roleIdsFromAccessDomain = new ArrayList<>();
        for (AccessDomain accessDomain : accessDomainList) {
            roleIdsFromAccessDomain.add(accessDomain.getRoleId());
        }
        List<RoleAction> roleActionList = roleActionRepository.findByActionId(Constants.ActionId.TASK_ADD_WITHOUT_ASSIGNMENT);
        List<Integer> roleIdsFromRoleAction = new ArrayList<>();
        for (RoleAction roleAction : roleActionList) {
            roleIdsFromRoleAction.add(roleAction.getRoleId());
        }
        if (organizationRepository.findByOrgId(orgId).getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
            roleIdsFromRoleAction.add(RoleEnum.PERSONAL_USER.getRoleId());
        }
        roleIdsFromAccessDomain.retainAll(roleIdsFromRoleAction);
        List<Long> entityIdsFromAccessDomain = new ArrayList<>();
        for (Integer roleIdFromAccessDomain : roleIdsFromAccessDomain) {
            List<EntityId> entityIds = accessDomainRepository.findEntityIdByAccountIdAndRoleIdAndIsActive(userAccount.getAccountId(), roleIdFromAccessDomain, true);
            for (EntityId entityId : entityIds) {
                entityIdsFromAccessDomain.add(entityId.getEntityId());
            }
        }
        List<Team> teams = teamRepository.findByTeamIdIn(entityIdsFromAccessDomain);
//        List<Team> finalTeams = new ArrayList<>();
//        for (Team team: teams) {
//            if (team.getFkProjectId().getProjectId() == projectId) {
//                finalTeams.add(team);
//            }
//        }
        return teams;

    }

    /* @deprecated: This method is used to get all the teams of a user by userId from all the organizations irrespective
     * whether the user is part of that team or not. Ex. In org "ABC" there is one team "Team ABC" and the user is not
     * part of this team "Team ABC". This method will bring such teams. However, such teams should not come for that user
     * if user is not part of that team. Hence, this method is replaced by method "getAllMyTeamsForUserId". */
    @Deprecated(since = "2022-09-08")
    public List<TeamOrgBuAndProjectName> getAllTeamsForUserId(Long userId) {
        List<UserAccount> userAccounts = userAccountRepository.findByFkUserIdUserIdAndIsActive(userId, true);
        List<TeamOrgBuAndProjectName> teamOrgAndProjectNames = new ArrayList<>();
        for (UserAccount userAccount : userAccounts) {
            List<Team> teamsDb = teamRepository.findByFkOrgIdOrgId(userAccount.getOrgId());
            for (Team team : teamsDb) {
                TeamOrgBuAndProjectName teamOrgAndProjectName = new TeamOrgBuAndProjectName(team.getTeamId(), team.getTeamCode(), team.getFkOrgId().getOrgId(), team.getFkProjectId().getProjectId(), team.getFkProjectId().getBuId(), team.getTeamName(), team.getFkOrgId().getOrganizationName(), team.getFkProjectId().getProjectName(), "bu_DFLT");//since this is deprecated, adding bu name as default to skip the analysis of this code while compilation
                teamOrgAndProjectNames.add(teamOrgAndProjectName);
            }
        }
        return teamOrgAndProjectNames;
    }

    /**
     * This method is used to get only those teams where user is part of the team. If user is part of the organization but
     * not part of any teams or team in that organization then that team will not come. Ex. In org "ABC" there are two teams
     * "Team ABC" and "Team XYZ". The user is part of only team "Team ABC". This method will return details(team name and organization name)
     * on only team "Team ABC".
     */
    public List<TeamOrgBuAndProjectName> getAllMyTeamsForUserId(Long userId) {
//        List<TeamNameOrgName> teamNameOrgNames = new ArrayList<>();
//        if (userId != null) {
//            List<UserAccount> userAccounts = userAccountRepository.findByFkUserIdUserId(userId);
//            if (!userAccounts.isEmpty()) {
//                List<Long> accountIds = new ArrayList<>();
//                for (UserAccount userAccount : userAccounts) {
//                    accountIds.add(userAccount.getAccountId());
//                }
//                List<CustomAccessDomain> allAccessDomainsFoundDb = accessDomainService.getAccessDomainsByAccountIdsAndEntityTypeId(Constants.EntityTypes.TEAM, accountIds);
//                if (!allAccessDomainsFoundDb.isEmpty()) {
//                    List<Long> allTeamIds = new ArrayList<>();
//                    for (CustomAccessDomain accessDomain : allAccessDomainsFoundDb) {
//                        allTeamIds.add(Long.valueOf(accessDomain.getEntityId()));
//                    }
//                    List<Team> allTeamsFoundDb = getTeamsByTeamIds(allTeamIds);
//                    if (!allTeamsFoundDb.isEmpty()) {
//                        for (Team team : allTeamsFoundDb) {
//                            TeamNameOrgName teamNameOrgName = new TeamNameOrgName(team.getTeamId(), team.getFkOrgId().getOrgId(),team.getTeamName(), team.getFkOrgId().getOrganizationName());
//                            teamNameOrgNames.add(teamNameOrgName);
//                        }
//                    }
//                }
//            }
//        }
//        return teamNameOrgNames;
        List<Long> userList = new ArrayList<>();
        userList.add(userId);
        User user = userRepository.findByUserId(userId);
        if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
            userList.addAll(userRepository.findAllUserIdByManagingUserId(user.getUserId()));
        }
        return teamRepository.getAllMyTeamsForUserIdIn(userList, Constants.EntityTypes.TEAM);
    }


    /**
     * This method is used to get all the teams belonging to one particular organization using orgId
     */
    public List<TeamIdAndTeamName> getAllTeamNameAndTeamIdByOrgId(Long orgId) {

        return teamRepository.findTeamIdTeamNameByFkOrgIdOrgId(orgId);

    }

    /**
     * This method is used to get only those teams where user is part of the team. If user is part of the organization but
     * not part of any teams or team in that organization then that team will not come. Ex. In org "ABC" there are two teams
     * "Team ABC" and "Team XYZ". The user is part of only team "Team ABC". This method will return only team "Team ABC".
     */
    public List<Team> getAllMyTeamsByUserId(Long userId) {
        List<Team> allTeamsFoundDb = new ArrayList<>();
        if (userId != null) {
            User foundUser = userRepository.findByUserId(userId);
            List<Long> accountIds = userService.getAllAccountIds(foundUser);
            if (!accountIds.isEmpty()) {
                List<CustomAccessDomain> allAccessDomainsFoundDb = accessDomainService.getAccessDomainsByAccountIdsAndEntityTypeIdAndIsActive(Constants.EntityTypes.TEAM, accountIds);
                if (!allAccessDomainsFoundDb.isEmpty()) {
                    List<Long> allTeamIds = new ArrayList<>();
                    for (CustomAccessDomain accessDomain : allAccessDomainsFoundDb) {
                        allTeamIds.add(Long.valueOf(accessDomain.getEntityId()));
                    }
                    allTeamsFoundDb = getTeamsByTeamIds(allTeamIds);
                }
            }
        }
        return allTeamsFoundDb;
    }

    public List<Team> getTeamsByTeamIds(List<Long> teamIds) {
        List<Team> allTeamsFoundDb = new ArrayList<>();
        if (!teamIds.isEmpty()) {
            allTeamsFoundDb = teamRepository.findByTeamIdIn(teamIds);
        }
        return allTeamsFoundDb;
    }

    public List<TeamIdAndTeamName> getAllTeamsByOrgIdAndProjectId(Long orgId, Long projectId) {
        List<TeamIdAndTeamName> teamIdAndTeamNames = teamRepository.findTeamIdTeamNameByFkOrgIdOrgIdAndFkProjectIdProjectId(orgId, projectId);
        return teamIdAndTeamNames;
    }

    public Team getTeamByTeamId(Long teamId) {
        Team foundTeamDb = teamRepository.findByTeamId(teamId);
        return foundTeamDb;
    }

    public String createChatRoomNameForTeam(Long teamId) {
        String chatRoomName = "Team" + "_" + teamId;
        return chatRoomName;
    }

    public Team createAndAuditTeam(Team team, Long userId, Long teamAdminId, Long headerAccountId) {
        Organization teamOrg = organizationRepository.findByOrgId(team.getFkOrgId().getOrgId());
        if (teamOrg.getMaxTeamCount()!=null && ((Objects.equals(teamOrg.getMaxTeamCount(), 0)) || (teamOrg.getMaxTeamCount() > 0 && !teamRepository.isTeamRegistrationAllowed(teamOrg.getOrgId(), teamOrg.getMaxTeamCount().longValue())))) {
            throw new IllegalStateException("Organization exceeded it's quota of registering teams");
        }
        Project project = projectService.getProjectByProjectId(team.getFkProjectId().getProjectId());
        if (!userAccountRepository.existsByAccountIdAndOrgIdAndIsActive(teamAdminId, teamOrg.getOrgId(), true)) {
            throw new ValidationFailedException("The specified team admin either does not belong to the organization or is not an active member.");
        }
        Boolean hasTeamCreateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, teamOrg.getOrgId(),
                List.of(headerAccountId), List.of(RoleEnum.ORG_ADMIN.getRoleId()), true);
        if (!hasTeamCreateAccess) hasTeamCreateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.BU, project.getBuId(),
                List.of(headerAccountId), List.of(RoleEnum.BU_ADMIN.getRoleId()), true);
        if (!hasTeamCreateAccess) hasTeamCreateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, project.getProjectId(),
                List.of(headerAccountId), List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId()), true);
        if (Objects.equals(teamOrg.getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
            if (!Objects.equals(teamAdminId, headerAccountId)) {
                throw new ValidationFailedException("User not authorized to create team in personal organization");
            }
            hasTeamCreateAccess = true;
        }//updated by akshit
        if (!hasTeamCreateAccess) throw new ValidationFailedException("User not authorized to create a team");

        User user = userRepository.findByUserId(userId);
        List<Long> accountIds = userService.getAllAccountIds(user);

        if (teamOrg == null || project == null) {
            throw new EntityNotFoundException("Provided organization or project not found");
        }

        if (!Objects.equals(project.getOrgId(), teamOrg.getOrgId())) {
            throw new IllegalArgumentException("Given project is not part of this organization");
        }

        if (!accountIds.contains(team.getFkOwnerAccountId().getAccountId())) {
            throw new IllegalArgumentException("Invalid owner account");
        }

        if ((team.getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) || (team.getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS))) && teamOrg.getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
            throw new ValidationFailedException("This name is reserved for the system. Please enter a different name.");
        }
        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(userId, Constants.OrgIds.PERSONAL.longValue(), true) && teamOrg.getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
            throw new IllegalStateException("Please register to personal organization before making team.");
        }

        validateTeamNameAndTeamCode(team, teamOrg, true);
        if (team.getTeamCode() == null) {
            List<String> teamInitialsInOrg = teamRepository.findTeamCodeByOrgId(team.getFkOrgId().getOrgId());
            String teamInitials = TeamIdentifierGenerator.generateUniqueIdentifier(team.getTeamName(), teamInitialsInOrg);
            team.setTeamCode(teamInitials);
        }

        Team savedTeam = teamRepository.save(team);
        savedTeam.setChatRoomName(createChatRoomNameForTeam(team.getTeamId()));
        teamRepository.save(savedTeam);
        auditService.auditForCreateTeam(savedTeam);
        if (savedTeam.getTeamId() != null) {
            accessDomainService.addAccessDomainAfterTeamAdd(savedTeam, teamAdminId);
            if (!Objects.equals(team.getFkOrgId().getOrgId(), Constants.OrgIds.PERSONAL.longValue())){
                accessDomainService.addProjectManagerOnTeamCreation(savedTeam);
            }
        }

        ConversationGroup conversationGroup = new ConversationGroup();

        conversationGroup.setEntityTypeId((long) Constants.EntityTypes.TEAM);
        conversationGroup.setEntityId(team.getTeamId());

        ConversationGroup convGroup = conversationService.createNewGroup(conversationGroup, team.getTeamName(), Constants.ConversationsGroupTypes.TEAM, team.getFkOrgId().getOrgId(), user);

        List<Long> userIds = accessDomainRepository
                .findUserIdsByEntityTypeIdAndEntityIdAndActive(Constants.EntityTypes.TEAM, team.getTeamId(), true);

        conversationService.addUsersToGroup(convGroup, userIds, user);

        return savedTeam;
    }

    /** checks whether a given team code already exists for a organization */
    public Boolean doTeamCodeExistsInOrg(String initials, Long orgId) {
        return teamRepository.existsByOrgIdAndTeamCode(orgId, initials);
    }

    /** method is used to generate Initials for a given team name in an organization */
    public String generateTeamCode(GenerateTeamCodeRequest request) {
        List<String> excludedInitials = teamRepository.findTeamCodeByOrgId(request.getOrgId());
        if (request.getExclusions() != null && !request.getExclusions().isEmpty()) {
            excludedInitials.addAll(request.getExclusions());
        }

        return TeamIdentifierGenerator.generateUniqueIdentifier(request.getTeamName(), excludedInitials);
    }

    /**
     * This method will find the orgId for the given teamId.
     *
     * @param teamId The teamId for which the orgId has to be found.
     * @return Long value (i.e. orgId)
     */
    public Long getOrgIdByTeamId(Long teamId) {
        Organization foundOrgId = teamRepository.findByTeamId(teamId).getFkOrgId();
        return foundOrgId.getOrgId();
    }

    /**
     * This method will find the list of all teams for the given orgIds.
     *
     * @param orgIds the list of orgIds.
     * @return List<Team>
     */
    public List<Team> getAllTeamsByOrgIds(List<Long> orgIds) {
        return teamRepository.findByFkOrgIdOrgIdIn(orgIds);
    }

    /**
     * returns a list of teams in the given list of projects
     */
    public List<Team> getAllTeamsByProjectIds(List<Long> projectIds) {
        return teamRepository.findByFkProjectIdProjectIdIn(projectIds);
    }

    public List<Team> getAllTeamsByBuIds(List<Long> buIds) {
        List<Project> projects = projectService.getAllProjectsByBuIds(buIds);
        List<Long> projectIds = projects.stream().map(Project::getProjectId).collect(Collectors.toList());
        return teamRepository.findByFkProjectIdProjectIdIn(projectIds);
    }

    public void filterTeamsForPersonalOrg(List<Team> teamList) {
        teamList.forEach(team -> {
            if (Objects.equals(team.getTeamName(), Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) && organizationRepository.findByOrgId(team.getFkOrgId().getOrgId()).getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
                List<Team> orgTeams = teamRepository.findByFkOrgIdOrgId(team.getFkOrgId().getOrgId());
                if (orgTeams.size() > 1) {
                    if (team.getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
                        team.setTeamName(Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                    }
                } else {
                    team.setTeamName(Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                }
            }
        });
    }

    public void removePersonalTeamFromTeamNameAndTeamIdResponse(List<TeamIdAndTeamName> teamIdAndTeamNames, Long orgId) {
        Iterator<TeamIdAndTeamName> iterator = teamIdAndTeamNames.iterator();

        while (iterator.hasNext()) {
            TeamIdAndTeamName team = iterator.next();

            if (Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) &&
                    organizationRepository.findByOrgId(orgId).getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                iterator.remove();
            }
        }
    }


    public void filterTeamOrgAndProjectNameListResponse(List<TeamOrgBuAndProjectName> teamOrgAndProjectNameList) {
        teamOrgAndProjectNameList.forEach(team -> {
            if (Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) && organizationRepository.findByOrgId(team.getOrgId()).getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                List<Team> orgTeams = teamRepository.findByFkOrgIdOrgId(team.getOrgId());
                if (orgTeams.size() > 1) {
                    team.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                } else {
                    team.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                }
            }
        });
    }

    public void filterTeamNameAndTeamIdResponse(User user, List<TeamIdAndTeamName> teamList, Long orgId) {
        if (organizationRepository.findByOrgId(orgId).getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
            teamList.forEach(team -> {
                if (Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
                    if (getAllTeamsForCreateTask(user.getPrimaryEmail(), orgId).size() > 1) {
                        team.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                    } else {
                        team.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                    }
                }
            });
        }
    }

    public Boolean ifUserExistsInTeam(Long teamId, String accountIds) {
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<AccessDomain> accessDomainList = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, teamId, accountIdList, true);
        if (accessDomainList.isEmpty()) {
            return false;
        }
        return true;
    }

    public Integer addMemberToTeamByInvite(String inviteId, User user, String timezone) {
        Invite invite = inviteService.validateInviteId(inviteId, timezone);
        UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(Constants.OrgIds.PERSONAL.longValue(), user.getUserId(), true);
        if (userAccount == null) {
            return 1;
        }
        Team team = teamRepository.findByTeamId(invite.getEntityId());
        if (team == null) {
            return 2;
        }
        AccessDomain accessDomain = new AccessDomain(userAccount.getAccountId(), com.tse.core_application.model.Constants.EntityTypes.TEAM, team.getTeamId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
        accessDomainRepository.save(accessDomain);
        inviteService.markInviteAsAccepted(inviteId);
        ConversationGroup conversationGroup = conversationService.getGroup((long) com.tse.core_application.model.Constants.EntityTypes.TEAM, team.getTeamId(), user);
        conversationService.addUsersToGroup(conversationGroup, List.of(user.getUserId()), user);
        return 3;
    }

    private void validateTeamNameAndTeamCode(Team team, Organization teamOrg, Boolean isCreated) {
        char firstChar = team.getTeamName().charAt(0);
        if (!Character.isLetter(firstChar)) {
            throw new ValidationFailedException("Team name should start with a letter");
        }

        if ((team.getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) || (team.getTeamName().equalsIgnoreCase(Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS))) && teamOrg.getOrganizationName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
            throw new ValidationFailedException("This name is reserved for the system. Please enter a different name.");
        }

        if (isCreated && team.getTeamCode() != null) {
            List<String> teamInitialsInOrg = teamRepository.findTeamCodeByOrgId(team.getFkOrgId().getOrgId());
            if (teamInitialsInOrg.contains(team.getTeamCode())) {
                throw new ValidationFailedException("Team Initials already exists. Please choose different initials");
            }
        }

        List<Team> teamList = teamRepository.findByFkProjectIdProjectId(team.getFkProjectId().getProjectId());
        if (teamList != null && !teamList.isEmpty()) {
            for (Team team1 : teamList) {
                if (!Objects.equals(team1.getTeamCode(), team.getTeamCode()) && team1.getTeamName().equalsIgnoreCase(team.getTeamName()) && !(Objects.equals(team.getFkOrgId().getOrgId(), Constants.OrgIds.PERSONAL.longValue()) && !teamRepository.existsByOrgIdAndTeamNameAndOwnerAccountId(Constants.OrgIds.PERSONAL.longValue(), team.getTeamName(), team.getFkOwnerAccountId().getAccountId()))) {
                    throw new IllegalStateException("Team with the name '" + team.getTeamName() + "' already exists in the project");
                }
            }
        }
    }

    /** temporary method to update existing team table data with the team initials */
    public void updateAllTeamInitials() {
        List<Team> teams = teamRepository.findAll();
        Map<Long, Set<String>> orgInitialsMap = new HashMap<>();

        for (Team team : teams) {
            Long orgId = team.getFkOrgId().getOrgId();
            Set<String> existingInitials = orgInitialsMap.getOrDefault(orgId, new HashSet<>());
            String uniqueInitials = TeamIdentifierGenerator.generateUniqueIdentifier(team.getTeamName(), new ArrayList<>(existingInitials));

            // Update team initials
            team.setTeamCode(uniqueInitials);
            teamRepository.save(team);

            // Store in map to check for future conflicts
            existingInitials.add(uniqueInitials);
            orgInitialsMap.put(orgId, existingInitials);
        }
    }

    public Boolean isProjectManagerInTeams (List<Long> teamIdList, Long accountId) {
        List<Long> authorizedAccoundIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM,
                teamIdList, List.of(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId()),
                true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
        return authorizedAccoundIds.contains(accountId);
    }

    public Team fillTeamDetailsAndCreateTeamObject (CreateTeamRequest createTeamRequest) {
        if (createTeamRequest.getTeamName() != null) {
            createTeamRequest.setTeamName(createTeamRequest.getTeamName().trim());
        }
        if (createTeamRequest.getTeamDesc() != null) {
            createTeamRequest.setTeamDesc(createTeamRequest.getTeamDesc().trim());
        }
        Team team = new Team();
        CommonUtils.copyNonNullProperties(createTeamRequest, team);
        Project project = projectRepository.findByProjectId(createTeamRequest.getProjectId());
        Organization organization = organizationRepository.findByOrgId(createTeamRequest.getOrgId());
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(createTeamRequest.getOwnerAccountId(), true);
        if (userAccount == null) {
            throw new IllegalStateException("Please provide a valid user account");
        }
        team.setFkOrgId(organization);
        team.setFkProjectId(project);
        team.setFkOwnerAccountId(userAccount);
        return team;
    }

    public DeleteTeamResponse deleteTeam (Long teamId, String accountIds, String timeZone, Boolean onProjectDelete, User user) {
        DeleteTeamResponse deleteTeamResponse = new DeleteTeamResponse();
        UserAccount modifyingAccount = userAccountRepository.findByAccountIdAndIsActive(Long.valueOf(accountIds), true);
        String message = "Team deletion completed. All associated user roles have been deactivated, and related Work Items have been removed successfully.";

        if (teamRepository.existsByTeamIdAndIsDeleted(teamId, true)) {
            throw new IllegalStateException("Team already deleted.");
        }
        Team team = teamRepository.findByTeamId(teamId);
        if (team == null) {
            throw new EntityNotFoundException("Team not found.");
        }
        List<Integer> authorizedAccountIds = Constants.ROLE_IDS_FOR_DELETE_TEAM_ACTION;
        Boolean hasTeamDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, team.getFkProjectId().getProjectId(),
                List.of(modifyingAccount.getAccountId()), authorizedAccountIds, true);
        if (!hasTeamDeleteAccess) hasTeamDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId(),
                List.of(modifyingAccount.getAccountId()), authorizedAccountIds, true);
        if (!hasTeamDeleteAccess) hasTeamDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, team.getTeamId(),
                List.of(modifyingAccount.getAccountId()), authorizedAccountIds, true);

        if (!hasTeamDeleteAccess && !onProjectDelete) throw new ValidationFailedException("User not authorized to delete team : '" + team.getTeamName() + "'");


        List<Long> taskIdList = taskRepository.findTaskIdByFkTeamIdTeamId(teamId);
        TaskListForBulkResponse taskListForBulkResponse = new TaskListForBulkResponse();
        List<TaskForBulkResponse> successList = new ArrayList<>();
        List<TaskForBulkResponse> failureList = new ArrayList<>();
        DeleteWorkItemRequest deleteWorkItemRequest = new DeleteWorkItemRequest();
        deleteWorkItemRequest.setDeleteReasonId(Constants.DeleteWorkItemReasonEnum.OTHERS.getTypeId());
        deleteWorkItemRequest.setDeleteReason("Work item deleted along with the team deletion");
        for (Long taskId : taskIdList) {
            Task taskDelete = taskRepository.findByTaskId(taskId);
            //Catching the error for failed tasks and adding them to failure list
            try {
                if (taskDelete == null) {
                    continue;
                }
                if (!Objects.equals(taskDelete.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE) && !Objects.equals(taskDelete.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
                    taskServiceImpl.deleteTaskByTaskId(taskId, taskDelete, accountIds, timeZone, true, deleteWorkItemRequest);
                }
                successList.add(new TaskForBulkResponse(taskDelete.getTaskId(), taskDelete.getTaskNumber(), taskDelete.getTaskTitle(), taskDelete.getFkTeamId().getTeamId(), "Task deleted successfully"));
            } catch (Exception e) {
                logger.error("Something went wrong: Not able to remove Work Item " + taskDelete.getTaskNumber() + " Caught Exception: " + e.getMessage());
                failureList.add(new TaskForBulkResponse(taskDelete.getTaskId(), taskDelete.getTaskNumber(), taskDelete.getTaskTitle(), taskDelete.getFkTeamId().getTeamId(), e.getMessage()));
            }
        }
        taskListForBulkResponse.setSuccessList(successList);
        taskListForBulkResponse.setFailureList(failureList);
        if (!failureList.isEmpty()) {
            message = "Failed to delete the team. The following Work Items could not be removed:";
        } else {
            team.setIsDeleted(true);
            accessDomainRepository.deactivateAllUserAccessDomainFromTeam(teamId);
            List<UserPreference> userPreferenceList = userPreferenceRepository.findByTeamId(team.getTeamId());
            for (UserPreference userPreference : userPreferenceList) {
                UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(team.getFkOrgId().getOrgId(), userPreference.getUserId(), true);
                Team userPreferenceTeam = null;
                List<Team> userPreferenceTeamList = teamRepository.findTeamForUserPreferenceByProjectId(team.getFkOrgId().getOrgId(), team.getFkProjectId().getProjectId(), userAccount.getAccountId(), PageRequest.of(0, 1));
                if (userPreferenceTeamList.isEmpty())
                    userPreferenceTeamList = teamRepository.findTeamForUserPreferenceByOrgId(team.getFkOrgId().getOrgId(), userAccount.getAccountId(), PageRequest.of(0, 1));
                if (userPreferenceTeamList.isEmpty())
                    userPreferenceTeamList = teamRepository.findTeamForUserPreference(userAccount.getAccountId(), PageRequest.of(0, 1));
                if (userPreferenceTeamList != null && !userPreferenceTeamList.isEmpty()) {
                    userPreferenceTeam = userPreferenceTeamList.get(0);
                }
                if (userPreferenceTeam != null) {
                    userPreference.setTeamId(userPreferenceTeam.getTeamId());
                    userPreference.setProjectId(userPreferenceTeam.getFkProjectId().getProjectId());
                    userPreference.setOrgId(userPreferenceTeam.getFkOrgId().getOrgId());
                    notificationService.userPreferenceChangeNotificationForDeleteTeam(team, userPreference.getUserId(), userPreferenceTeam.getTeamName(), modifyingAccount, List.of(userAccount.getAccountId()), timeZone);
                } else {
                    userPreference.setTeamId(null);
                    userPreference.setProjectId(null);
                    notificationService.userPreferenceChangeNotificationForDeleteTeam(team, userPreference.getUserId(), null, modifyingAccount, List.of(userAccount.getAccountId()), timeZone);
                }
            }
            team.setDeletedOn(LocalDateTime.now());
            team.setFkDeletedByAccountId(modifyingAccount);
            teamRepository.save(team);
            conversationService.updateGroupDetails(teamId, (long) Constants.EntityTypes.TEAM, new GroupUpdateRequest(null, null, false), user);
        }
        deleteTeamResponse.setMessage(message);
        deleteTeamResponse.setTaskListForBulkResponse(taskListForBulkResponse);
        return deleteTeamResponse;
    }

    public List<TeamIdAndTeamName> getAllTeamNameAndTeamIdByProjectId(Long projectId, String accountIds) {
        List<Integer> authorizedAccountIds = Constants.ROLE_IDS_FOR_DELETE_TEAM_ACTION;
        Boolean hasTeamDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, projectId,
                List.of(Long.valueOf(accountIds)), authorizedAccountIds, true);

        if (!hasTeamDeleteAccess) throw new ValidationFailedException("User not authorized to view teams in provided project.");

        return teamRepository.findTeamIdTeamNameByFkProjectIdProjectId(projectId);

    }

    public List<DeletedTeamReport> getAllDeletedTeamReport (Long projectId, String accountIds, String timeZone, Boolean skipValidation) {
        List<DeletedTeamReport> deleteTeamReportList = new ArrayList<>();
        Long headerAccountId = Long.valueOf(accountIds);
        Project project = projectRepository.findByProjectId(projectId);
        if (project == null) {
            throw new EntityNotFoundException("Project not found");
        }
        List<Integer> authorizedAccountIds = Constants.ROLE_IDS_FOR_DELETE_TEAM_ACTION;
        Boolean hasTeamDeleteAccess = skipValidation;
        if (!hasTeamDeleteAccess) hasTeamDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, projectId,
                List.of(headerAccountId), authorizedAccountIds, true);
        if (!hasTeamDeleteAccess) hasTeamDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, project.getOrgId(),
                List.of(headerAccountId), authorizedAccountIds, true);

        if (!hasTeamDeleteAccess) throw new ValidationFailedException("User not authorized to view deleted team report for project : '" + project.getProjectName() + "'");
        List<Team> teamList = teamRepository.findByFkProjectIdProjectIdAndIsDeleted(projectId, true);
        for (Team team : teamList) {
            DeletedTeamReport deletedTeamReport = new DeletedTeamReport();
            deletedTeamReport.setTeamName(team.getTeamName());
            deletedTeamReport.setTeamId(team.getTeamId());
            deletedTeamReport.setTeamCode(team.getTeamCode());
            deletedTeamReport.setDeletedOn(DateTimeUtils.convertServerDateToUserTimezone(team.getDeletedOn(), timeZone));
            deletedTeamReport.setDeletedBy(new EmailFirstLastAccountIdIsActive(team.getFkDeletedByAccountId().getEmail(), team.getFkDeletedByAccountId().getAccountId(), team.getFkDeletedByAccountId().getFkUserId().getFirstName(), team.getFkDeletedByAccountId().getFkUserId().getLastName(), team.getFkDeletedByAccountId().getIsActive()));
            deleteTeamReportList.add(deletedTeamReport);
        }

        return deleteTeamReportList;
    }

    public void convertTeamServerDateTimeToLocalDateTime (Team team, String timeZone) {
        if (team == null) {
            return;
        }
        if (team.getCreatedDateTime() != null) {
            Timestamp createdDateTime = Timestamp.valueOf(DateTimeUtils.convertServerDateToUserTimezone(team.getCreatedDateTime().toLocalDateTime(), timeZone));
            team.setCreatedDateTime(createdDateTime);
        }
        if (team.getLastUpdatedDateTime() != null) {
            Timestamp updatedDateTime = Timestamp.valueOf(DateTimeUtils.convertServerDateToUserTimezone(team.getLastUpdatedDateTime().toLocalDateTime(), timeZone));
            team.setLastUpdatedDateTime(updatedDateTime);
        }
        if (team.getDeletedOn() != null) {
            LocalDateTime deletedDateTime = DateTimeUtils.convertServerDateToUserTimezone(team.getDeletedOn(), timeZone);
            team.setDeletedOn(deletedDateTime);
        }
    }

    public TeamResponseDto createTeamResponse(Team team, String timeZone) {
        if (team == null || timeZone == null) {
            return null;
        }
        TeamResponseDto dto = new TeamResponseDto();
        dto.setTeamId(team.getTeamId());
        dto.setTeamName(team.getTeamName());
        dto.setTeamDesc(team.getTeamDesc());
        dto.setParentTeamId(team.getParentTeamId());
        dto.setChatRoomName(team.getChatRoomName());
        dto.setTeamCode(team.getTeamCode());
        dto.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(team.getCreatedDateTime(),timeZone));
        dto.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(team.getLastUpdatedDateTime(),timeZone));
        dto.setDeletedOn(team.getDeletedOn() != null
                ? DateTimeUtils.convertServerDateToUserTimezone(team.getDeletedOn(), timeZone)
                : null);
        dto.setFkProjectId(team.getFkProjectId());
        dto.setFkOrgId(team.getFkOrgId());
        dto.setFkOwnerAccountId(team.getFkOwnerAccountId() != null ? team.getFkOwnerAccountId() : null);
        dto.setFkDeletedByAccountId(team.getFkDeletedByAccountId() != null ? team.getFkDeletedByAccountId() : null);
        dto.setIsDeleted(team.getIsDeleted());
        dto.setIsDisabled(team.getIsDisabled());
        return dto;
    }

    public Map<Long, String> getTeamNameByIds(Set<Long> teamIds) {
        return teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getTeamId, Team::getTeamName));
    }
}
