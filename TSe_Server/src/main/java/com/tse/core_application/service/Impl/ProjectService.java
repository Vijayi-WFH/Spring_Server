package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.dto.conversations.GroupUpdateRequest;
import com.tse.core_application.dto.project.*;
import com.tse.core_application.exception.InvalidRequestParamater;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.ProjectNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.specification.OpenTaskSpecification;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger logger = LogManager.getLogger(ProjectService.class.getName());

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private BURepository buRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private  UserPreferenceRepository userPreferenceRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private LeaveService leaveService;

    private static final Comparator<String> NULL_SAFE_STRING_COMPARATOR =
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    public ResponseEntity<Object> getProjectIdByOrgIdFormattedResponse(List<ProjectIdProjectName> projectIdProjectNameList) {
        if (projectIdProjectNameList.isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No projects found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        } else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, projectIdProjectNameList);
        }
    }

    public ResponseEntity<Object> getProjectByProjectIdFormattedResponse(Project project) {
        if (project != null) {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, project);
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ProjectNotFoundException());
            logger.error("No project found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ProjectNotFoundException();
        }
    }

    /**
     * This method creates and returns a new Project based on the provided ProjectRequest, performing validations for
     * organization, business unit existence, and user permissions.
     */
    public ProjectResponse validateAndCreateProject(ProjectRequest request, String accountIds, String timeZone) throws IllegalAccessException {
        Long accountIdOfCreator = Long.parseLong(accountIds);

        // validations
        if(Objects.equals(request.getProjectName(),Constants.PROJECT_NAME))
            throw new ValidationFailedException("Default project name can not be given to any other project");
        Organization organization = organizationRepository.findById(request.getOrgId()).orElseThrow(() -> new IllegalArgumentException("Invalid request. Organization not found"));
        if (organization.getMaxProjectCount() != null && ( Objects.equals(organization.getMaxProjectCount(), 0) || (organization.getMaxProjectCount() > 0 && !projectRepository.isProjectRegistrationAllowed(organization.getOrgId(), organization.getMaxProjectCount().longValue())))) {
            throw new IllegalStateException("Organization exceeded it's quota of registering projects");
        }
        BU bu = buRepository.findById(request.getBuId()).orElseThrow(() -> new IllegalArgumentException("Invalid request. Business Unit not found"));
        if (!Objects.equals(bu.getOrgId(), request.getOrgId()))
            throw new ValidationFailedException("Business Unit does not exist within the specified organization.");
        if (!isUserOrgOrBuAdmin(organization.getOrgId(), bu.getBuId(), accountIdOfCreator))
            throw new ValidationFailedException("User doesn't have necessary permission to create a project");
        if (request.getAccessDomains() == null || request.getAccessDomains().isEmpty()){
            throw new IllegalStateException("Please provide project admin to create project");
        }
        if (request.getProjectName() != null) {
            request.setProjectName(request.getProjectName().trim());
        }
        if (request.getProjectDesc() != null) {
            request.setProjectDesc(request.getProjectDesc().trim());
        }
        List<Integer> providedRoleIdsList = request.getAccessDomains().stream().map(AccessDomainRequest::getRoleId).collect(Collectors.toList());
        if (!providedRoleIdsList.contains(RoleEnum.PROJECT_ADMIN.getRoleId())) {
            throw new IllegalStateException("Please provide project admin to create project");
        }

        // save the new project
        Project projectToAdd = new Project();
        projectToAdd.setOrgId(request.getOrgId());
        projectToAdd.setProjectName(request.getProjectName());
        projectToAdd.setProjectDesc(request.getProjectDesc());
        projectToAdd.setBuId(request.getBuId());
        projectToAdd.setOwnerAccountId(accountIdOfCreator);
        Project savedProject = projectRepository.save(projectToAdd);
        auditService.auditForCreateProject(savedProject);

        // save the access domain for the new project
        if (!request.getAccessDomains().isEmpty()) {
            saveAccessDomainForProject(savedProject, request.getAccessDomains(), accountIdOfCreator, providedRoleIdsList, accountIds, timeZone);
        }

        User user = userAccountRepository.findFirstByAccountId(accountIdOfCreator).getFkUserId();

        List<Long> userIds = accessDomainRepository
                .findUserIdsByEntityTypeIdAndEntityIdAndActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, savedProject.getProjectId(), true);

        ConversationGroup conversationGroup = new ConversationGroup();

        conversationGroup.setEntityTypeId((long) com.tse.core_application.model.Constants.EntityTypes.PROJECT);
        conversationGroup.setEntityId(savedProject.getProjectId());

        ConversationGroup convGroup = conversationService.createNewGroup(conversationGroup, savedProject.getProjectName(), com.tse.core_application.model.Constants.ConversationsGroupTypes.PROJ, savedProject.getOrgId(), user);
//        conversationService.getGroup()
        conversationService.addUsersToGroup(convGroup, userIds, user);

        return makeProjectResponseFromProject(savedProject, timeZone, accountIds);
    }

    /** method saves access domain when creating a new project*/
    public void saveAccessDomainForProject(Project project, List<AccessDomainRequest> accessDomainRequestList, Long creatorOrModifierAdminId, List<Integer> providedRoleIdsList, String accountIds, String timeZone) throws IllegalAccessException {
        List<Integer> allowedAdminRoleIds = List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.PROJECT_VIEWER.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId());
        List<Integer> allowedNonAdminRoleIds = List.of(RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId());
        List<Long> uniqueAdminAccountIds = new ArrayList<>();
        List<Long> uniqueNonAdminAccountIds = new ArrayList<>();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Long> teamIdList = teamRepository.findTeamIdsByProjectId(project.getProjectId());
        Set<Long> accountIdOfManager = new HashSet<>();
        Set<Long> accountIdOfDeletedManager = new HashSet<>();
        List<Long> managersAccountId = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, List.of(project.getProjectId()), allowedNonAdminRoleIds, true);
        List<Long> allActiveAccounts = accessDomainRepository.findAllActiveAccountIdsByEntityAndTypeIds(com.tse.core_application.model.Constants.EntityTypes.PROJECT, project.getProjectId());

        if (managersAccountId != null && !managersAccountId.isEmpty()) {
            accountIdOfManager.addAll(managersAccountId);
        }
        for (AccessDomainRequest accessDomainRequest : accessDomainRequestList) {
            if (allowedNonAdminRoleIds.contains(accessDomainRequest.getRoleId())) {
                if (accessDomainRequest.getToDelete()) {
                    accountIdOfDeletedManager.add(accessDomainRequest.getAccountId());
                } else {
                    accountIdOfManager.add(accessDomainRequest.getAccountId());
                }
            }
        }
        accountIdOfManager.removeAll(accountIdOfDeletedManager);
        if (accountIdOfManager.isEmpty()) {
            throw new ValidationFailedException("There should be at least one project manager in a project");
        }

        List<Long> deletedProjectAdminAccountIdList = new ArrayList<>();

        for (AccessDomainRequest accessDomainRequest : accessDomainRequestList) {
            AccessDomain accessDomain;
            if (!allowedAdminRoleIds.contains(accessDomainRequest.getRoleId()) && !allowedNonAdminRoleIds.contains(accessDomainRequest.getRoleId())) {
                throw new InvalidRequestParamater("Invalid project roles");
            }
            if (allowedAdminRoleIds.contains(accessDomainRequest.getRoleId()) && uniqueAdminAccountIds.contains(accessDomainRequest.getAccountId())) {
                throw new ValidationFailedException("Single admin role can be assigned to user");
            }
            if (allowedNonAdminRoleIds.contains(accessDomainRequest.getRoleId()) && uniqueNonAdminAccountIds.contains(accessDomainRequest.getAccountId())) {
                throw new ValidationFailedException("Single non admin role can be assigned to user");
            }
            if (allowedAdminRoleIds.contains(accessDomainRequest.getRoleId())) {
                uniqueAdminAccountIds.add(accessDomainRequest.getAccountId());
            }
            if (allowedNonAdminRoleIds.contains(accessDomainRequest.getRoleId())) {
                uniqueNonAdminAccountIds.add(accessDomainRequest.getAccountId());
            }
            List<AccessDomain> foundAccessDomain = new ArrayList<>();
            if (allowedAdminRoleIds.contains(accessDomainRequest.getRoleId())) {
                foundAccessDomain = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                        project.getProjectId(), accessDomainRequest.getAccountId(), allowedAdminRoleIds, true);
            }
            else if (allowedNonAdminRoleIds.contains(accessDomainRequest.getRoleId())) {
                foundAccessDomain = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                        project.getProjectId(), accessDomainRequest.getAccountId(), allowedNonAdminRoleIds, true);
            }

            if (!foundAccessDomain.isEmpty()) {
                accessDomain = foundAccessDomain.get(0); // there can be a single role of an account in a project
                if (accessDomain.getRoleId().equals(RoleEnum.PROJECT_ADMIN.getRoleId())) {
                    Boolean doesRoleExists = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndRoleIdAndIsActiveAndAccountIdNot(com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                            project.getProjectId(), RoleEnum.PROJECT_ADMIN.getRoleId(), true, accessDomain.getAccountId());
                    if (!doesRoleExists && !providedRoleIdsList.contains(RoleEnum.PROJECT_ADMIN.getRoleId())) {
                        throw new IllegalStateException("The project must have at least one Project Admin role assigned. Please ensure that a Project Admin is included in the provided roles.");
                    }
                }
                if (accessDomainRequest.getToDelete()) {
                    if (teamIdList != null && !teamIdList.isEmpty()) {
                        if (allowedNonAdminRoleIds.contains(accessDomain.getRoleId())) {
                            removeProjectManagerFromAllTeam(teamIdList, accessDomain.getAccountId(), creatorOrModifierAdminId, accountIdOfManager.iterator().next(), timeZone);
                        }
                    }
                    accessDomain.setIsActive(false);
                    if (Objects.equals(RoleEnum.PROJECT_ADMIN.getRoleId(), accessDomain.getRoleId())) {
                        deletedProjectAdminAccountIdList.add(accessDomain.getAccountId());
                    }
                    auditService.auditForDeletedProjectMember(project, accessDomain, creatorOrModifierAdminId);
                } else {
                    Integer oldRoleId = accessDomain.getRoleId();
                    if (teamIdList != null && !teamIdList.isEmpty()) {
                        if (Objects.equals(accessDomainRequest.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId())) {
                            addProjectManagerInAllTeam(teamIdList, RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(), accessDomainRequest.getAccountId());
                        } else if (Objects.equals(accessDomainRequest.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId())) {
                            addProjectManagerInAllTeam(teamIdList, RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), accessDomainRequest.getAccountId());
                        }
                    }
                    accessDomain.setRoleId(accessDomainRequest.getRoleId());
                    auditService.auditForEditedProjectMember(project, accessDomain, creatorOrModifierAdminId, oldRoleId);
                }
                accessDomainRepository.save(accessDomain);
            } else {
                if (accessDomainRequest.getToDelete()) {
                    throw new ValidationFailedException("Invalid attempt to delete a role that doesn't exist");
                }
                if (!userAccountRepository.existsByAccountIdAndIsActive(accessDomainRequest.getAccountId(), true)) {
                    throw new IllegalStateException("User with the specified account does not exist or is not active in the organization");
                }
                if (teamIdList != null && !teamIdList.isEmpty()) {
                    if (Objects.equals(accessDomainRequest.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId())) {
                        addProjectManagerInAllTeam(teamIdList, RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(), accessDomainRequest.getAccountId());
                    } else if (Objects.equals(accessDomainRequest.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId())) {
                        addProjectManagerInAllTeam(teamIdList, RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), accessDomainRequest.getAccountId());
                    }
                }
                accessDomain = new AccessDomain();
                accessDomain.setAccountId(accessDomainRequest.getAccountId());
                accessDomain.setEntityTypeId(com.tse.core_application.model.Constants.EntityTypes.PROJECT);
                accessDomain.setEntityId(project.getProjectId());
                accessDomain.setRoleId(accessDomainRequest.getRoleId());
                AccessDomain savedAccessDomain = accessDomainRepository.save(accessDomain);
                auditService.auditForAddedProjectMember(project, savedAccessDomain, creatorOrModifierAdminId);
            }
        }

        changeLeaveApprovalAccountIdOnProjectAdminRemoval (project, deletedProjectAdminAccountIdList);
        //while updating the project,
        // if admin removes all the role of the Member, then it should also be removed from the Conversation group
        removeProjectMembersFromConversationGroup(project, accessDomainRequestList, allActiveAccounts, creatorOrModifierAdminId);

        // while updating the project, if admin adds any new member with
        // some role then we would add that member in the Project System Group.
        addNewProjectMembersInConversationGroup(project, accessDomainRequestList, allActiveAccounts, creatorOrModifierAdminId);
    }

    public void changeLeaveApprovalAccountIdOnProjectAdminRemoval(Project project, List<Long> deletedProjectAdminAccountIdList) {
        if (deletedProjectAdminAccountIdList == null || deletedProjectAdminAccountIdList.isEmpty()) {
            return;
        }

        try {
            Set<Long> removedApproverIds = new HashSet<>(deletedProjectAdminAccountIdList);

            List<Long> orgAndBackupIds = Optional.ofNullable(
                    accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndAccountIdInAndIsActive(
                            com.tse.core_application.model.Constants.EntityTypes.ORG,
                            List.of(project.getOrgId()),
                            List.of(RoleEnum.ORG_ADMIN.getRoleId(), RoleEnum.BACKUP_ORG_ADMIN.getRoleId()),
                            new ArrayList<>(removedApproverIds),
                            true
                    )
            ).orElseGet(Collections::emptyList);

            removedApproverIds.removeAll(new HashSet<>(orgAndBackupIds));
            if (removedApproverIds.isEmpty()) {
                return;
            }

            List<Long> projectUserAccountIdList = Optional
                    .ofNullable(getprojectMembersAccountIdList(List.of(project.getProjectId())))
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .map(AccountId::getAccountId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (projectUserAccountIdList.isEmpty()) {
                return;
            }

            Long orgAdminAccountId = Optional.ofNullable(
                            accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(
                                    com.tse.core_application.model.Constants.EntityTypes.ORG,
                                    project.getOrgId(),
                                    List.of(RoleEnum.ORG_ADMIN.getRoleId()),
                                    true
                            )
                    ).orElseGet(Collections::emptyList)
                    .stream()
                    .map(AccountId::getAccountId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No active ORG_ADMIN found for orgId=" + project.getOrgId()
                    ));

            leaveService.changeApproverOfLeaveApplications(projectUserAccountIdList, new ArrayList<>(removedApproverIds), orgAdminAccountId);

        } catch (Exception e) {
            logger.error("Not able to update approver account id of leave on removing project admin", e);
            // optionally rethrow if you want caller to know
        }
    }


    /** method validates and updates a project details and members */
    public ProjectResponse validateAndUpdateProject(UpdateProjectRequest request, String accountIds, String timeZone, User user) throws IllegalAccessException {
        Project savedProject;
        Long accountIdOfCreator = Long.parseLong(accountIds);
        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(() -> new IllegalArgumentException("No Such Project Exists"));
        GroupUpdateRequest groupUpdateRequest = new GroupUpdateRequest();
        if(Objects.equals(request.getProjectName(),Constants.PROJECT_NAME) && !Objects.equals(project.getProjectName(),request.getProjectName()))
            throw new ValidationFailedException("Default project name can not be given to any other project");

        if (!isUserOrgOrBuAdmin(project.getOrgId(), project.getBuId(), accountIdOfCreator) && !isUserAnyProjectAdmin(project.getProjectId(), accountIdOfCreator))
            throw new ValidationFailedException("User doesn't have necessary permission to update the project");
        List<Integer> providedRoleIdsList = new ArrayList<>();
        if (request.getAccessDomains() != null && !request.getAccessDomains().isEmpty()) {
            providedRoleIdsList = request.getAccessDomains().stream().map(AccessDomainRequest::getRoleId).collect(Collectors.toList());
        }
        if (request.getProjectName() != null) {
            request.setProjectName(request.getProjectName().trim());
        }
        if (request.getProjectDesc() != null) {
            request.setProjectDesc(request.getProjectDesc().trim());
        }
        if (request.getProjectName() != null || request.getProjectDesc() != null) {
            if (request.getProjectName() != null){
                project.setProjectName(request.getProjectName());
                groupUpdateRequest.setGroupName(request.getProjectName());
            }
            if (request.getProjectDesc() != null){
                project.setProjectDesc(request.getProjectDesc());
                groupUpdateRequest.setGroupDesc(request.getProjectDesc());
            }
            savedProject = projectRepository.save(project);
            conversationService.updateGroupDetails(project.getProjectId(), (long) com.tse.core_application.model.Constants.EntityTypes.PROJECT, groupUpdateRequest, user);
        } else {
            savedProject = project;
        }

        if (!request.getAccessDomains().isEmpty()) {
            saveAccessDomainForProject(savedProject, request.getAccessDomains(), accountIdOfCreator, providedRoleIdsList, accountIds, timeZone);
        }
        auditService.auditForUpdateProject(project);
        savedProject.setLastUpdatedDateTime(LocalDateTime.now());
        return makeProjectResponseFromProject(savedProject, timeZone, accountIds);
    }
    /**
     * This method checks whether a user with the specified account ID has permission to create a project within the given organization (orgId) and business unit (buId).
     * The permission is granted to organization admin (and backup admin) and bu admin (and backup admin), and currently, the logic is implemented for organization admin role.
     */
    public boolean isUserOrgOrBuAdmin(Long orgId, Long buId, Long accountId) {
        boolean isAllowed = false;
        // create project permission is allowed to org admin, backup org admin, bu admin and backup bu admin roles --
        // currently we only have the logic defined for the org admin, so we include that only
        AccessDomain accessDomainOfOrgAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdAndIsActive(com.tse.core_application.model.Constants.EntityTypes.ORG,
                orgId, RoleEnum.ORG_ADMIN.getRoleId(), true);
        List<AccessDomain> accessDomainOfBuAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.BU,
                buId, List.of(RoleEnum.BU_ADMIN.getRoleId(), RoleEnum.BACKUP_BU_ADMIN.getRoleId()), true);
        if (accessDomainOfBuAdmin != null && !accessDomainOfBuAdmin.isEmpty()) {
            for (AccessDomain accessDomain : accessDomainOfBuAdmin) {
                if (Objects.equals(accessDomain.getAccountId(), accountId)) {
                    isAllowed = true;
                    break;
                }
            }
        }
        if (accessDomainOfOrgAdmin != null && Objects.equals(accessDomainOfOrgAdmin.getAccountId(), accountId)) isAllowed = true;
        return isAllowed;
    }

    /**
     * This method checks whether a user with the specified account ID has Project Admin or Backup Project Admin role.
     */
    public boolean isUserAnyProjectAdmin(Long projectId, Long accountId) {
        List<AccountId> accountIdList = new ArrayList<>();
        accountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                projectId, List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId()), true);

        return accountIdList.stream().anyMatch(accountIdObject -> accountIdObject.getAccountId().equals(accountId));
    }

    /**
     * if the user is orgAdmin/ orgBackUpAdmin/ buAdmin/ buBackUpAdmin then we can send list of all projects
     * otherwise, we send details of projects in which the user is Project Admin/ BackUpProjectAdmin/ ProjectViewer
     */
    public List<ProjectResponse> getProjectsListByBu(Long buId, String accountIds, String timeZone) {
        Long accountIdOfRequester = Long.parseLong(accountIds);
        BU bu = buRepository.findById(buId).orElseThrow(() -> new IllegalArgumentException("Invalid request. Business Unit not found"));

        List<Project> projects = new ArrayList<>();
        List<Project> filteredProjects = new ArrayList<>();
        List<ProjectResponse> responseList = new ArrayList<>();

        projects = getAllProjectsByBuIdAndOrgId(buId, bu.getOrgId());
        Map<Long, Project> projectMap = projects.stream().collect(Collectors.toMap(Project::getProjectId, project -> project));
        if (isUserOrgOrBuAdmin(bu.getOrgId(), buId, accountIdOfRequester)) {
            filteredProjects = projects;
        } else {
            List<Integer> allowedRoleIds = List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId(), RoleEnum.PROJECT_VIEWER.getRoleId());
            List<AccessDomain> accessDomainList = accessDomainRepository.findByAccountIdInAndRoleIdInAndIsActive(List.of(accountIdOfRequester), allowedRoleIds, true);
            for (AccessDomain accessDomain : accessDomainList) {
                if (Objects.equals(accessDomain.getEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.PROJECT)) {
                    filteredProjects.add(projectMap.get(accessDomain.getEntityId().longValue()));
                }
            }
        }

        for (Project project : filteredProjects) {
            responseList.add(makeProjectResponseFromProject(project, timeZone, accountIds));
        }

        return responseList;
    }

    /**
     * creates a project response object from the project object
     */
    private ProjectResponse makeProjectResponseFromProject(Project project, String timeZone, String accountIds) {
        ProjectResponse response = new ProjectResponse();
        BeanUtils.copyProperties(project, response);
        if (response.getCreatedDateTime() != null) {
            response.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(response.getCreatedDateTime(), timeZone));
        }
        if (response.getLastUpdatedDateTime() != null) {
            response.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(response.getLastUpdatedDateTime(), timeZone));
        }

        List<Team> teamsInProject = teamService.getAllTeamsByProjectIds(List.of(project.getProjectId()));
        List<CustomTeamResponse> teamResponseList = new ArrayList<>();
        for (Team team : teamsInProject) {
            CustomTeamResponse teamResponse = new CustomTeamResponse();
            CommonUtils.copyNonNullProperties(team, teamResponse);
            teamResponseList.add(teamResponse);
        }
        response.setActiveTeamDetails(teamResponseList);
        response.setDeletedTeamDetails(teamService.getAllDeletedTeamReport(project.getProjectId(), accountIds, timeZone, true));

        List<CustomAccessDomain> accessDomains = getSortedAccessDomainsForProject (project);
        response.setAccessDomains(accessDomains);

        return response;
    }

    public List<CustomAccessDomain> getSortedAccessDomainsForProject(Project project) {
        Integer PROJECT_TYPE = com.tse.core_application.model.Constants.EntityTypes.PROJECT;
        Integer ORG_TYPE     = com.tse.core_application.model.Constants.EntityTypes.ORG;

        List<Integer> ORG_ADMIN_ROLE_IDS = com.tse.core_application.model.Constants.ORG_ADMIN_ROLE;

        List<AccessDomain> ads = accessDomainRepository.findAllForProjectAndOrgWithUsers(
                PROJECT_TYPE, project.getProjectId(),
                ORG_TYPE, project.getOrgId(),
                ORG_ADMIN_ROLE_IDS
        );

        Set<Long> missing = ads.stream()
                .filter(ad -> ad.getUserAccount() == null || ad.getUserAccount().getFkUserId() == null)
                .map(AccessDomain::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Build a fallback accountId -> User map with ONE extra query (if needed)
        final Map<Long, User> fallbackUserMap;
        if (!missing.isEmpty()) {
            List<UserAccount> uaList = userAccountRepository.findAllById(missing);
            fallbackUserMap = uaList.stream()
                    .filter(ua -> ua.getFkUserId() != null)
                    .collect(Collectors.toMap(
                            UserAccount::getAccountId,
                            UserAccount::getFkUserId,
                            (a, b) -> a
                    ));
        } else {
            fallbackUserMap = Collections.emptyMap();
        }

        Comparator<AccessDomain> cmp = Comparator
                .comparing(AccessDomain::getRoleId, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ad -> {
                    User u = getUser(ad, fallbackUserMap);
                    return u != null ? u.getFirstName() : null;
                }, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ad -> {
                    User u = getUser(ad, fallbackUserMap);
                    return u != null ? u.getLastName() : null;
                }, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ad -> {
                    User u = getUser(ad, fallbackUserMap);
                    return u != null ? u.getPrimaryEmail() : null;
                }, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

        ads.sort(cmp);

        List<CustomAccessDomain> result = new ArrayList<>(ads.size());
        for (AccessDomain ad : ads) {
            CustomAccessDomain dto = new CustomAccessDomain();
            dto.setAccountId(ad.getAccountId());
            dto.setEntityTypeId(ad.getEntityTypeId());
            dto.setEntityId(ad.getEntityId());
            dto.setRoleId(ad.getRoleId());
            dto.setWorkflowTypeId(ad.getWorkflowTypeId());
            result.add(dto);
        }

        return result;
    }

    private static User getUser(AccessDomain ad, Map<Long, User> fallbackUserMap) {
        if (ad == null) return null;
        UserAccount ua = ad.getUserAccount();
        if (ua != null && ua.getFkUserId() != null) return ua.getFkUserId();
        if (fallbackUserMap == null) return null;
        return fallbackUserMap.get(ad.getAccountId());
    }


    public Project addProject(Organization org, BU bu, Long ownerAccountId) {
        Project addedProject = null;
        if (org != null && bu != null) {
            Project projectToAdd = new Project();
            projectToAdd.setOrgId(org.getOrgId());
            String projectName = Constants.PROJECT_NAME;
            projectToAdd.setProjectName(projectName);
            projectToAdd.setProjectType(Constants.ProjectType.DEFAULT_PROJECT);
            projectToAdd.setBuId(bu.getBuId());
            projectToAdd.setOwnerAccountId(ownerAccountId);
            addedProject = projectRepository.save(projectToAdd);
        }
        return addedProject;
    }

    public List<ProjectIdProjectName> getProjectByOrgId(Long orgId) {
        List<ProjectIdProjectName> projectIdProjectNames = projectRepository.findByOrgId(orgId);
        return projectIdProjectNames;
    }

    public List<Project> getAllProjectsByBuIdAndOrgId(Long buId, Long orgId) {
        return projectRepository.findByBuIdAndOrgId(buId, orgId);
    }

    public Project getProjectByProjectId(Long projectId) {
        Project foundProjectDb = null;
        if (projectId != null) {
            foundProjectDb = projectRepository.findByProjectId(projectId);
        }
        return foundProjectDb;
    }

    public List<Project> getAllProjectsByProjectsIds(List<Integer> projectIds) {
        List<Project> allProjectsFoundDb = new ArrayList<>();
        if (!projectIds.isEmpty()) {
            List<Long> projectIdsLong = new ArrayList<>();
            for (Integer projectId : projectIds) {
                projectIdsLong.add(Long.valueOf(projectId));
            }
            allProjectsFoundDb = projectRepository.findByProjectIdIn(projectIdsLong);
        }
        return allProjectsFoundDb;
    }

    public Project getProjectByProjectIdAndOrgId(Long projectId, Long orgId) {
        Project project = null;
        if (projectId != null && orgId != null) {
            project = projectRepository.findByProjectIdAndOrgId(projectId, orgId);
        }
        return project;
    }

    /**
     * This method will find the orgId for the given projectId.
     *
     * @param projectId The projectId for which the orgId has to be found.
     * @return Long value (i.e. orgId)
     */
    public Long getOrgIdByProjectId(Long projectId) {
        OrgId foundOrgId = projectRepository.findOrgIdByProjectId(projectId);
        return foundOrgId.getOrgId();
    }

    /**
     * This method will find all the projects for the given orgIds.
     *
     * @param orgIds the list of orgIds.
     * @return List<Project>
     */
    public List<Project> getAllProjectsByOrgIds(List<Long> orgIds) {
        return projectRepository.findByOrgIdIn(orgIds);
    }

    public List<Project> getAllProjectsByBuIds(List<Long> buIds) {
        return projectRepository.findByBuIdIn(buIds);
    }

    public List<EmailFirstLastAccountId> getAllMembersInProject (Long projectId, String accountId) {
        Long userAccountId = Long.parseLong(accountId);
        Project project = projectRepository.findByProjectId(projectId);
        if (!isUserOrgOrBuAdmin(project.getOrgId(), project.getBuId(), userAccountId) && !isUserAnyProjectAdmin(project.getProjectId(), userAccountId) && !isProjectViewer(projectId, userAccountId)) {
            throw new ValidationFailedException("User doesn't have necessary permission to view project members");
        }
        List<AccountId> projectMembersList = getprojectMembersAccountIdList(List.of(projectId));
        List<Long> accountIdList = projectMembersList.stream()
                .map(AccountId::getAccountId)
                .collect(Collectors.toList());
        List<EmailFirstLastAccountId> emailFirstLastAccountIdList = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(accountIdList);

        return emailFirstLastAccountIdList;
    }

    public Boolean isProjectViewer (Long projectId, Long userAccountId) {
        return accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                projectId, List.of(RoleEnum.PROJECT_VIEWER.getRoleId()), true).contains(new AccountId(userAccountId));
    }

    /** gets project members by project id*/
    public List<AccountId> getprojectMembersAccountIdList (List<Long> projectIdList) {
        List<AccountId> accountIdListProject = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectIdList, true);
        List<Long> teamIdList = teamRepository.findTeamIdsByFkProjectIdProjectIdIn(projectIdList);
        List<AccountId> teamAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamIdList, true);
        Set<AccountId> combinedList = new HashSet<>(accountIdListProject);
        combinedList.addAll(teamAccountIds);

        return new ArrayList<>(combinedList);
    }

    public List<Long> getUserAllProjectId (List<Long> accountIdsList) {
        Set<Long> uniqueProjectIds = new HashSet<>();
        uniqueProjectIds.addAll(accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accountIdsList).stream().map(Project::getProjectId).collect(Collectors.toSet()));
        uniqueProjectIds.addAll(accessDomainRepository.findEntityIdByEntityTypeIdAndAccountIdsInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT,accountIdsList).stream().map(id->id.longValue()).collect(Collectors.toSet()));
        return uniqueProjectIds.stream().collect(Collectors.toList());
    }

    public Boolean validateOrgAndProjectWithAccountIds (Long orgId, Long projectId, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Project project = projectRepository.findByProjectId(projectId);
        if (project.getProjectName().contains(com.tse.core_application.model.Constants.DEFAULT_INDICATOR)) {
            throw new ValidationFailedException("User not authorized to create meeting in provided project");
        }
        if (userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(headerAccountIds, orgId, true) && projectRepository.existsByOrgIdAndProjectIdAndIsDisabled(orgId, projectId, false)) {
            return true;
        }
        return false;
    }

    public Boolean existInProject (Long accountId, Long projectId) {
        List<Long> userProjects = getUserAllProjectId(List.of(accountId));
        if (userProjects.contains(projectId)) {
            return true;
        }
        return false;
    }

    public List<AccountId> getAllprojectMembersAccountIdList (Long projectId) {
        List<AccountId> accountIdListProject = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectId);
        List<Long> teamIdList = teamRepository.findTeamIdsByProjectId(projectId);
        List<AccountId> teamAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdIn(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamIdList);
        Set<AccountId> combinedList = new HashSet<>(accountIdListProject);
        combinedList.addAll(teamAccountIds);

        return new ArrayList<>(combinedList);
    }

    public DeleteProjectResponse deleteProject (Long projectId, String accountIds, String timeZone, Boolean onBuDelete, User user) {
        DeleteProjectResponse deleteProjectResponse = new DeleteProjectResponse();
        UserAccount modifyingAccount = userAccountRepository.findByAccountIdAndIsActive(Long.valueOf(accountIds), true);
        String message = "Project deletion completed. All team is deleted, associated user roles have been deactivated, and related tasks have been removed successfully.";

        Project project = projectRepository.findByProjectId(projectId);
        if (project == null) {
            throw new EntityNotFoundException("Project not found");
        }
        List<ProjectIdProjectName> projectListByOrg = projectRepository.findByOrgId(project.getOrgId());
        long projectCountInOrg = projectListByOrg.stream().filter(projectName -> projectName.getIsDeleted() == null || !projectName.getIsDeleted()).count();
        if(projectCountInOrg <= 1){
            throw new ValidationFailedException("Not allowed to delete the only remaining Project in the Organization.");
        }

        if(project.getIsDeleted() != null && project.getIsDeleted()) {
            throw new ValidationFailedException(project.getProjectName() + " is already deleted");
        }
        List<Integer> authorizedAccountIds = com.tse.core_application.model.Constants.ROLE_IDS_FOR_DELETE_PROJECT_ACTION;
        Boolean hasProjectDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.BU, project.getBuId(),
                List.of(modifyingAccount.getAccountId()), authorizedAccountIds, true);
        if (!hasProjectDeleteAccess) hasProjectDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.ORG, project.getOrgId(),
                List.of(modifyingAccount.getAccountId()), authorizedAccountIds, true);
        if (!hasProjectDeleteAccess) hasProjectDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, project.getProjectId(),
                List.of(modifyingAccount.getAccountId()), authorizedAccountIds, true);

        if (!hasProjectDeleteAccess && !onBuDelete) throw new ValidationFailedException("User not authorized to delete project : '" + project.getProjectName() + "'");


        List<Long> teamIdList = teamRepository.findTeamIdByFkProjectIdProjectId(projectId);
        TeamListForBulkResponse teamListForBulkResponse = new TeamListForBulkResponse();
        List<TeamForBulkResponse> successList = new ArrayList<>();
        List<TeamForBulkResponse> failureList = new ArrayList<>();
        for (Long teamId : teamIdList) {
            Team teamDelete = teamRepository.findByTeamId(teamId);
            if (teamDelete == null) {
                continue;
            }
            teamService.deleteTeam(teamId, accountIds, timeZone, true, user);
            Boolean isTeamDeleted = teamRepository.existsByTeamIdAndIsDeleted(teamId, true);
            if(isTeamDeleted) {
                successList.add(new TeamForBulkResponse(teamDelete.getTeamId(), teamDelete.getTeamName(), teamDelete.getTeamCode(), "Team deleted successfully"));
            }
            else {
                failureList.add(new TeamForBulkResponse(teamDelete.getTeamId(), teamDelete.getTeamName(), teamDelete.getTeamCode(), "Failed to delete this team"));
            }
        }
        teamListForBulkResponse.setSuccessList(successList);
        teamListForBulkResponse.setFailureList(failureList);
        if (!failureList.isEmpty()) {
            message = "Failed to delete the project. The following team could not be removed:";
        } else {
            project.setIsDeleted(true);
            accessDomainRepository.deactivateAllUserAccessDomainFromProject(projectId);
            List<UserPreference> userPreferenceList = userPreferenceRepository.findByProjectId(project.getProjectId());
            for (UserPreference userPreference : userPreferenceList) {
                UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(project.getOrgId(), userPreference.getUserId(), true);
                Project userPreferenceProject = null;
                List<Project> userPreferenceProjectList = projectRepository.findProjectForUserPreference(project.getOrgId(), userAccount.getAccountId(), PageRequest.of(0,1));
                if (userPreferenceProjectList != null && !userPreferenceProjectList.isEmpty()) {
                    userPreferenceProject = userPreferenceProjectList.get(0);
                }
                if (userPreferenceProject != null) {
                    userPreference.setProjectId(userPreferenceProject.getProjectId());
                    notificationService.userPreferenceChangeNotificationForDeleteProject(project, userPreference.getUserId(), userPreferenceProject.getProjectName(), modifyingAccount, List.of(userAccount.getAccountId()), timeZone);
                } else {
                    userPreference.setTeamId(null);
                    notificationService.userPreferenceChangeNotificationForDeleteProject(project, userPreference.getUserId(), null, modifyingAccount, List.of(userAccount.getAccountId()), timeZone);
                }
            }
            project.setDeletedOn(LocalDateTime.now());
            project.setFkDeletedByAccountId(modifyingAccount);
            projectRepository.save(project);
            conversationService.updateGroupDetails(projectId, (long) com.tse.core_application.model.Constants.EntityTypes.PROJECT, new GroupUpdateRequest(null, null, false), user);
        }
        deleteProjectResponse.setMessage(message);
        deleteProjectResponse.setTeamListForBulkResponse(teamListForBulkResponse);
        return deleteProjectResponse;
    }

    public List<DeletedProjectReport> getAllDeletedProjectReport (Long buId, String accountIds, String timeZone, Boolean onBuDelete) {
        List<DeletedProjectReport> deletedProjectReportList = new ArrayList<>();
        Long headerAccountId = Long.valueOf(accountIds);
        BU bu = buRepository.findByBuId(buId);
        if (bu == null) {
            throw new EntityNotFoundException("Bu not found");
        }
        List<Integer> authorizedAccountIds = com.tse.core_application.model.Constants.ROLE_IDS_FOR_VIEW_PROJECT_TEAM_REPORT;
        Boolean hasProjectDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.BU, buId,
                List.of(headerAccountId), authorizedAccountIds, true);
        if (!hasProjectDeleteAccess) hasProjectDeleteAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.ORG, bu.getOrgId(),
                List.of(headerAccountId), authorizedAccountIds, true);

        if (!hasProjectDeleteAccess && !onBuDelete) throw new ValidationFailedException("User not authorized to view deleted project report for Bu : '" + bu.getBuName() + "'");
        List<Project> projectList = projectRepository.findByBuIdAndIsDeleted(buId, true);
        for (Project project : projectList) {
            DeletedProjectReport deletedProjectReport = new DeletedProjectReport();
            deletedProjectReport.setProjectName(project.getProjectName());
            deletedProjectReport.setProjectId(project.getProjectId());
            deletedProjectReport.setDeletedOn(DateTimeUtils.convertServerDateToUserTimezone(project.getDeletedOn(), timeZone));
            deletedProjectReport.setDeletedBy(new EmailFirstLastAccountIdIsActive(project.getFkDeletedByAccountId().getEmail(), project.getFkDeletedByAccountId().getAccountId(), project.getFkDeletedByAccountId().getFkUserId().getFirstName(), project.getFkDeletedByAccountId().getFkUserId().getLastName(), project.getFkDeletedByAccountId().getIsActive()));
            deletedProjectReportList.add(deletedProjectReport);
        }

        return deletedProjectReportList;
    }

    public void addProjectManagerInAllTeam (List<Long> teamIdList, Integer managerRoleId, Long accountIdOfUser) {
        for (Long teamId : teamIdList) {
            Team team = teamRepository.findByTeamId(teamId);
            List<AccessDomain> currentRolesOfUserInTeam = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, team.getTeamId(), accountIdOfUser, true);
            AccessDomain userNonAdminRoleAccessDomain = null;
            for (AccessDomain domain : currentRolesOfUserInTeam) {
                if (domain.getRoleId() >= RoleEnum.TASK_BASIC_USER.getRoleId() && domain.getRoleId() <= RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId() && domain.getRoleId() != RoleEnum.TEAM_VIEWER.getRoleId()) {
                    userNonAdminRoleAccessDomain = domain;
                }
            }
            if (userNonAdminRoleAccessDomain != null && Objects.equals(managerRoleId, userNonAdminRoleAccessDomain.getRoleId())) {
                continue;
            }

            if (userNonAdminRoleAccessDomain != null) {
                userNonAdminRoleAccessDomain.setRoleId(managerRoleId);
                AccessDomain updatedAccessDomain = accessDomainRepository.save(userNonAdminRoleAccessDomain);
            }
            else {
                AccessDomain accessDomainToAdd = new AccessDomain();
                accessDomainToAdd.setAccountId(accountIdOfUser);
                accessDomainToAdd.setEntityId(team.getTeamId());
                accessDomainToAdd.setEntityTypeId(com.tse.core_application.model.Constants.EntityTypes.TEAM);
                accessDomainToAdd.setWorkflowTypeId(null);
                accessDomainToAdd.setRoleId(managerRoleId);
                AccessDomain newAccessDomainInserted = accessDomainRepository.save(accessDomainToAdd);
            }
        }
    }

    public void removeProjectManagerFromAllTeam (List<Long> teamIdList, Long removedUserAccountId, Long modifierAccountId, Long assignedToAccountId, String timeZone) throws IllegalAccessException {
        List<Integer> allowedNonAdminRoleIds = List.of(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        for (Long teamId : teamIdList) {
            Team team = teamRepository.findByTeamId(teamId);
            List<AccessDomain> currentRolesOfUserInTeam = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, team.getTeamId(), removedUserAccountId, true);
            AccessDomain userNonAdminRoleAccessDomain = null;
            for (AccessDomain domain : currentRolesOfUserInTeam) {
                if (domain.getRoleId() >= RoleEnum.TASK_BASIC_USER.getRoleId() && domain.getRoleId() <= RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId() && domain.getRoleId() != RoleEnum.TEAM_VIEWER.getRoleId()) {
                    userNonAdminRoleAccessDomain = domain;
                }
            }
            if (userNonAdminRoleAccessDomain != null && allowedNonAdminRoleIds.contains(userNonAdminRoleAccessDomain.getRoleId())) {
                Specification<Task> spec = Specification
                        .where(OpenTaskSpecification.notCompletedOrDeleted())
                        .and(OpenTaskSpecification.assignedTo(removedUserAccountId)
                        .and(OpenTaskSpecification.matchesEntity(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamId)));

                List<Long> taskIdList = taskRepository.findAll(spec).stream()
                        .map(Task::getTaskId)
                        .collect(Collectors.toList());

                List<TaskIdAssignedTo> taskIdAssignedToList = new ArrayList<>();

                for (Long taskId : taskIdList) {
                    TaskIdAssignedTo taskIdAssignedTo = new TaskIdAssignedTo();
                    taskIdAssignedTo.setTaskId(taskId);
                    taskIdAssignedTo.setAccountIdAssignedTo(assignedToAccountId);
                    taskIdAssignedToList.add(taskIdAssignedTo);
                }

                taskServiceImpl.removeDeletedAccountAsMentorObserverAssignedToImmediateAttention(removedUserAccountId, com.tse.core_application.model.Constants.EntityTypes.TEAM, teamId, modifierAccountId, taskIdAssignedToList, timeZone);
                sprintService.removeMemberFromAllSprintInTeam (teamId, removedUserAccountId, List.of(modifierAccountId));

                userNonAdminRoleAccessDomain.setIsActive(false);
                AccessDomain updatedAccessDomain = accessDomainRepository.save(userNonAdminRoleAccessDomain);
            }

        }
    }

    public List<UserAllProjectResponseWithOrg> getAllProjectsOfUser (String email, String timeZone) {
        List<UserAllProjectResponseWithOrg> userAllProjectResponseWithOrgList = new ArrayList<>();
        List<UserAccount> userAccountList = userAccountRepository.findByEmailAndIsActiveAndIsVerifiedTrue(email, true);
        HashMap<Long, String> orgIdToNameMap = new HashMap<>();
        HashMap<Long, String> buIdToNameMap = new HashMap<>();
        HashMap<Long, List<Long>> orgToBuListMap = new HashMap<>();
        List<Long> orgList = new ArrayList<>();
        HashMap<Long, Long> orgToAccountIdList = new HashMap<>();
        List<Long> accountIdList = new ArrayList<>();
        for (UserAccount userAccount : userAccountList) {
            if (!Objects.equals(com.tse.core_application.model.Constants.OrgIds.PERSONAL.longValue(), userAccount.getOrgId())) {
                OrgIdOrgName orgIdOrgNameDb = organizationRepository.findOrgIdAndOrganizationNameByOrgId(userAccount.getOrgId());
                orgIdToNameMap.put(orgIdOrgNameDb.getOrgId(), orgIdOrgNameDb.getOrganizationName());
                orgList.add(userAccount.getOrgId());
                List<BU> buList = buRepository.findByOrgId(userAccount.getOrgId());
                orgToBuListMap.put(userAccount.getOrgId(), buList.stream().map(BU :: getBuId).collect(Collectors.toList()));
                buList.forEach(bu -> buIdToNameMap.putIfAbsent(bu.getBuId(), bu.getBuName()));
                orgToAccountIdList.put(userAccount.getOrgId(), userAccount.getAccountId());
                accountIdList.add(userAccount.getAccountId());
            }
        }
        for (Long orgId : orgList) {
            UserAllProjectResponseWithOrg userAllProjectResponseWithOrg = new UserAllProjectResponseWithOrg();
            List<Project> projectList = new ArrayList<>();
            List<ProjectResponse> projectResponseList = new ArrayList<>();
            getAllProjectUserHaveAccess (orgId, projectList, accountIdList, orgToBuListMap);
            for (Project project : projectList) {
                projectResponseList.add(makeProjectResponseFromProject(project, timeZone, orgToAccountIdList.get(orgId).toString()));
            }
            createGetAllProjectResponseWithBuForEachOrg (orgId, projectResponseList, buIdToNameMap, orgIdToNameMap, userAllProjectResponseWithOrg, userAllProjectResponseWithOrgList);
        }
        sortUserAllProjectResponseWithOrgList (userAllProjectResponseWithOrgList);
        return userAllProjectResponseWithOrgList;
    }

    public void sortUserAllProjectResponseWithOrgList(List<UserAllProjectResponseWithOrg> userAllProjectResponseWithOrgList) {
        if (userAllProjectResponseWithOrgList == null) return;

        // Sort orgs by orgName
        userAllProjectResponseWithOrgList.sort(
                Comparator.comparing(
                        UserAllProjectResponseWithOrg::getOrgName,
                        NULL_SAFE_STRING_COMPARATOR
                )
        );

        userAllProjectResponseWithOrgList.forEach(this::sortOrgLevelForProjects);
    }

    private void sortOrgLevelForProjects(UserAllProjectResponseWithOrg org) {
        if (org == null || org.getUserAllProjectResponseWithBuList() == null) return;

        // Sort BUs by buName
        org.getUserAllProjectResponseWithBuList().sort(
                Comparator.comparing(
                        UserAllProjectResponseWithBu::getBuName,
                        NULL_SAFE_STRING_COMPARATOR
                )
        );

        org.getUserAllProjectResponseWithBuList().forEach(this::sortBuLevelForProjects);
    }

    private void sortBuLevelForProjects(UserAllProjectResponseWithBu bu) {
        if (bu == null || bu.getProjectResponseList() == null) return;

        // Sort projects by projectName
        bu.getProjectResponseList().sort(
                Comparator.comparing(
                        ProjectResponse::getProjectName,
                        NULL_SAFE_STRING_COMPARATOR
                )
        );

        bu.getProjectResponseList().forEach(this::sortProjectLevelForTeams);
    }

    private void sortProjectLevelForTeams(ProjectResponse project) {
        if (project == null) return;

        // Sort active teams by teamName
        if (project.getActiveTeamDetails() != null) {
            project.getActiveTeamDetails().sort(
                    Comparator.comparing(
                            CustomTeamResponse::getTeamName,
                            NULL_SAFE_STRING_COMPARATOR
                    )
            );
        }

        // Sort deleted teams by teamName
        if (project.getDeletedTeamDetails() != null) {
            project.getDeletedTeamDetails().sort(
                    Comparator.comparing(
                            DeletedTeamReport::getTeamName,
                            NULL_SAFE_STRING_COMPARATOR
                    )
            );
        }
    }

    public void getAllProjectUserHaveAccess (Long orgId, List<Project> projectList, List<Long> accountIdList, HashMap<Long, List<Long>> orgToBuListMap) {
        if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.ORG, orgId, accountIdList, com.tse.core_application.model.Constants.ORG_ADMIN_ROLE, true)) {
            projectList.addAll(projectRepository.findByOrgIdIn(List.of(orgId)));
        }
        else {
            List<Long> buIdList = orgToBuListMap.get(orgId);
            for (Long buId : buIdList) {
                if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.BU, buId, accountIdList, com.tse.core_application.model.Constants.BU_ADMIN_ROLE, true)) {
                    projectList.addAll(projectRepository.findByBuIdIn(List.of(buId)));
                }
                else {
                    List<Long> projectIdListOfBu = projectRepository.findProjectIdsByBuId(buId);
                    List<Long> projectIdWithAdminRole = accessDomainRepository.findDistinctEntityIdByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectIdListOfBu, accountIdList, com.tse.core_application.model.Constants.PROJECT_ADMIN_ROLE, true);
                    if (projectIdWithAdminRole != null && !projectIdWithAdminRole.isEmpty()) {
                        projectList.addAll(projectRepository.findByProjectIdIn(projectIdWithAdminRole));
                    }
                }
            }
        }
    }

    public void createGetAllProjectResponseWithBuForEachOrg (Long orgId, List<ProjectResponse> projectResponseList, HashMap<Long, String> buIdToNameMap, HashMap<Long, String> orgIdToNameMap, UserAllProjectResponseWithOrg userAllProjectResponseWithOrg, List<UserAllProjectResponseWithOrg> userAllProjectResponseWithOrgList) {
        if (!projectResponseList.isEmpty()) {
            Map<Long, List<ProjectResponse>> buToProjectResponseMap = projectResponseList.stream()
                    .collect(Collectors.groupingBy(ProjectResponse::getBuId));

            List<UserAllProjectResponseWithBu> userAllProjectResponseWithBuList = new ArrayList<>();

            for (Map.Entry<Long, List<ProjectResponse>> entry : buToProjectResponseMap.entrySet()) {
                Long buId = entry.getKey();
                List<ProjectResponse> projects = entry.getValue();

                String buName = buIdToNameMap.getOrDefault(buId, "Unknown BU Name");

                UserAllProjectResponseWithBu userAllProjectResponseWithBu = new UserAllProjectResponseWithBu();
                userAllProjectResponseWithBu.setBuId(buId);
                userAllProjectResponseWithBu.setBuName(buName);
                userAllProjectResponseWithBu.setProjectResponseList(projects);

                userAllProjectResponseWithBuList.add(userAllProjectResponseWithBu);
            }

            userAllProjectResponseWithOrg.setOrgId(orgId);
            userAllProjectResponseWithOrg.setOrgName(orgIdToNameMap.getOrDefault(orgId, "Unknown Org Name"));
            userAllProjectResponseWithOrg.setUserAllProjectResponseWithBuList(userAllProjectResponseWithBuList);
            userAllProjectResponseWithOrgList.add(userAllProjectResponseWithOrg);
        }
    }

    public List<CustomAccessDomain> getAllAdminRoleForEntity (Integer entityTypeId, Long entityId) {
        List<CustomAccessDomain> accessDomains = new ArrayList<>();
        List<AccessDomain> accessDomainsFromDb = new ArrayList<>();
        Long teamId = null;
        Long projectId = null;
        Long buId = null;
        Long orgId = null;
        if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.TEAM, entityTypeId)) {
            Team team = teamRepository.findByTeamId(entityId);
            if (team != null) {
                teamId = entityId;
                projectId = team.getFkProjectId().getProjectId();
                buId = team.getFkProjectId().getBuId();
                orgId = team.getFkOrgId().getOrgId();
            }
        }
        else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.PROJECT, entityTypeId)) {
            Project project = projectRepository.findByProjectId(entityId);
            if (project != null) {
                projectId = entityId;
                buId = project.getBuId();
                orgId = project.getOrgId();
            }
        }
        else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.BU, entityTypeId)) {
            BU bu = buRepository.findByBuId(entityId);
            if (bu != null) {
                buId = entityId;
                orgId = bu.getOrgId();
            }
        }
        else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.ORG, entityTypeId)) {
            orgId = entityId;
        }
        else {
            throw new ValidationFailedException("Please enter valid entity type id");
        }

        getAllAdminAccessDomainForEntity (orgId, buId, projectId, teamId, accessDomainsFromDb);

        for (AccessDomain accessDomain : accessDomainsFromDb) {
            CustomAccessDomain customAccessDomain = new CustomAccessDomain();
            BeanUtils.copyProperties(accessDomain, customAccessDomain);
            accessDomains.add(customAccessDomain);
        }
        return accessDomains;
    }

    public void  getAllAdminAccessDomainForEntity (Long orgId, Long buId, Long projectId, Long teamId, List<AccessDomain> accessDomainsFromDb) {
        if (orgId != null) {
            List<AccessDomain> accessDomainListForOrgAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.ORG, orgId, com.tse.core_application.model.Constants.ORG_ADMIN_ROLE, true);
            if (accessDomainListForOrgAdmin != null && !accessDomainListForOrgAdmin.isEmpty()) {
                accessDomainsFromDb.addAll(accessDomainListForOrgAdmin);
            }
        }
//        if (buId != null) {
//            List<AccessDomain> accessDomainListForBuAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.BU, buId, com.tse.core_application.model.Constants.BU_ADMIN_ROLE, true);
//            if (accessDomainListForBuAdmin != null && !accessDomainListForBuAdmin.isEmpty()) {
//                accessDomainsFromDb.addAll(accessDomainListForBuAdmin);
//            }
//        }
        if (projectId != null) {
            List<AccessDomain> accessDomainListForProjectAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectId, com.tse.core_application.model.Constants.PROJECT_ADMIN_ROLE, true);
            if (accessDomainListForProjectAdmin != null && !accessDomainListForProjectAdmin.isEmpty()) {
                accessDomainsFromDb.addAll(accessDomainListForProjectAdmin);
            }
        }
        if (teamId != null) {
            List<AccessDomain> accessDomainListForTeamAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamId, com.tse.core_application.model.Constants.TEAM_ADMIN_ROLE, true);
            if (accessDomainListForTeamAdmin != null && !accessDomainListForTeamAdmin.isEmpty()) {
                accessDomainsFromDb.addAll(accessDomainListForTeamAdmin);
            }
        }
    }

    public List<CustomAccessDomain> getAllPMRoleOfProject (Long projectId) {
        Project project = projectRepository.findByProjectId(projectId);
        if (project == null) {
            throw new ValidationFailedException("Project doesn't exist");
        }
        List<CustomAccessDomain> customAccessDomainList = new ArrayList<>();
        List<AccessDomain> projectManagerAccessDomainList = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectId, List.of(RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId()), true);
        for (AccessDomain accessDomain : projectManagerAccessDomainList) {
            CustomAccessDomain customAccessDomain = new CustomAccessDomain();
            BeanUtils.copyProperties(accessDomain, customAccessDomain);
            customAccessDomainList.add(customAccessDomain);
        }
        return customAccessDomainList;
    }

    public Map<Long, String> getProjectNameByIds(Set<Long> projectIds) {
        return projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getProjectId, Project::getProjectName));
    }

    private void removeProjectMembersFromConversationGroup(Project project, List<AccessDomainRequest> accessDomainRequestList, List<Long> allActiveAccounts, Long creatorOrModifierAdminId){
        List<AccessDomainRequest> deletedAccessDomain = accessDomainRequestList.stream().filter(AccessDomainRequest::getToDelete).collect(Collectors.toList());
        Map<Long, Long> accountIdOccurrence = allActiveAccounts.stream().collect(Collectors.groupingBy(accountId -> accountId, Collectors.counting()));
        if(!deletedAccessDomain.isEmpty()){
            List<Long> allowedRemovedAccountIds = new ArrayList<>();
            User user = userAccountRepository.findByAccountId(creatorOrModifierAdminId).getFkUserId();
            for(AccessDomainRequest accessDomainRequest : deletedAccessDomain){
                if(accountIdOccurrence.get(accessDomainRequest.getAccountId()) == 1){
                    allowedRemovedAccountIds.add(accessDomainRequest.getAccountId());
                }
            }
            if(!allowedRemovedAccountIds.isEmpty()){
                List<Long> userIds = userAccountRepository.findByAccountIdInAndIsActive(allowedRemovedAccountIds, true)
                        .stream().map(UserAccount::getFkUserId).map(User::getUserId).collect(Collectors.toList());
                conversationService.removeUsersFromGroup(userIds, project.getProjectId(), com.tse.core_application.model.Constants.EntityTypes.PROJECT, user);
            }
        }
    }


    private void addNewProjectMembersInConversationGroup(Project project, List<AccessDomainRequest> accessDomainRequestList, List<Long> allActiveAccounts, Long creatorOrModifierAdminId) {

        List<Long> newDomainRequest = accessDomainRequestList.stream().filter(domain -> !domain.getToDelete()
                    && !allActiveAccounts.contains(domain.getAccountId()))
                .map(AccessDomainRequest::getAccountId).collect(Collectors.toList());
        if(!newDomainRequest.isEmpty()) {
            List<Long> userIds = userAccountRepository.findActiveUserIdsFromAccountIds(newDomainRequest);
            User user = userAccountRepository.findByAccountId(creatorOrModifierAdminId).getFkUserId();
            ConversationGroup conversationGroup = conversationService.getGroup(project.getProjectId(), (long) com.tse.core_application.model.Constants.EntityTypes.PROJECT, user);
            if (conversationGroup != null && conversationGroup.getGroupId() != null) {
                conversationService.addUsersToGroup(conversationGroup, userIds, user);
            }
        }
    }
}
