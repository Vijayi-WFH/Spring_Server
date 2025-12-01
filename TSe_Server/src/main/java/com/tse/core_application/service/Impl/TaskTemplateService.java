package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.TaskTemplateRequest;
import com.tse.core_application.dto.TaskTemplateResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.WorkflowTypeDoesNotExistException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.personal_task.PersonalTaskTemplate;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskTemplateService {
    private static final Logger logger = LogManager.getLogger(TaskTemplateService.class.getName());

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonalTaskTemplateRepository personalTaskTemplateRepository;

    @Autowired
    private PersonalTemplateService personalTemplateService;

    @Autowired
    private AuditService auditService;

    /**
     * This method validates and saves template in the database
     */
    public TaskTemplateResponse addTemplate (TaskTemplateRequest templateRequest, String accountIds) {
        validateTemplateCreator(templateRequest, accountIds, false);
        removeLeadingAndTrailingSpacesForTemplate(templateRequest);
        if (!Constants.DEFAULT_WORKFLOW.contains(templateRequest.getTaskWorkflowId())) {
            throw new WorkflowTypeDoesNotExistException();
        }
        TaskTemplate taskTemplate = new TaskTemplate();
        CommonUtils.copyNonNullProperties(templateRequest, taskTemplate);
        Long templateNumberDb = taskTemplateRepository.getMaxTemplateNumber();

        if (templateNumberDb == null) {
            taskTemplate.setTemplateNumber(ControllerConstants.TASK_TEMPLATE_NUMBER_START);
        } else {
            taskTemplate.setTemplateNumber(templateNumberDb + 1);
        }
        WorkFlowTaskStatus templateStatus = workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatusId(templateRequest.getTaskWorkFlowStatus());
        if (templateStatus == null) {
            throw new IllegalStateException("Please provide valid workflow status");
        }
        if (!templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) && !templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
            throw new IllegalStateException("User only allowed to create template with workflow status as 'Backlog' or 'Not-Started'");
        }
        taskTemplate.setFkWorkflowTaskStatus(templateStatus);
        if (templateRequest.getOrgId() != null) {
            Organization org = organizationRepository.findByOrgId(templateRequest.getOrgId());
            taskTemplate.setFkOrgId(org);
        }
        if (templateRequest.getProjectId() != null) {
            Project project = projectRepository.findByProjectId(templateRequest.getProjectId());
            if (!Objects.equals(project.getOrgId(), taskTemplate.getFkOrgId().getOrgId())) {
                throw new IllegalStateException("Project assigned to template do not belong to the provided organization");
            }
            taskTemplate.setFkProjectId(project);
        }
        if (templateRequest.getTeamId() != null) {
            Team team = teamRepository.findByTeamId(templateRequest.getTeamId());
            if (!Objects.equals(team.getFkProjectId().getProjectId(), taskTemplate.getFkProjectId().getProjectId())) {
                throw new IllegalStateException("Team assigned to template do not belong to the provided project");
            }
            if (!Objects.equals(team.getFkOrgId().getOrgId(), taskTemplate.getFkOrgId().getOrgId())) {
                throw new IllegalStateException("Team assigned to template do not belong to the provided organization");
            }
            taskTemplate.setFkTeamId(team);
        }
        if (taskTemplate.getTemplateTitle() == null) {
            taskTemplate.setTemplateTitle(templateRequest.getTemplateTitle());
        }
        TaskTemplate savedTaskTemplate= taskTemplateRepository.save(taskTemplate);
        auditService.auditForTemplate(userAccountRepository.findByAccountIdAndIsActive(savedTaskTemplate.getFkAccountIdCreator().getAccountId(), true), savedTaskTemplate, false);
        TaskTemplateResponse response = createTemplateResponse(savedTaskTemplate, accountIds);
        return response;
    }

    /**
     * This method validates and updates template in the database
     */
    public TaskTemplateResponse updateTemplate (TaskTemplateRequest templateRequest, String accountIds, Long templateId, String timeZone) throws IllegalAccessException {
        validateTemplateCreator(templateRequest, accountIds, true);
        removeLeadingAndTrailingSpacesForTemplate(templateRequest);
        if (!Constants.DEFAULT_WORKFLOW.contains(templateRequest.getTaskWorkflowId())) {
            throw new WorkflowTypeDoesNotExistException();
        }
        TaskTemplate taskTemplateDb = taskTemplateRepository.findByTemplateId(templateId);
        if (taskTemplateDb == null) {
            throw new IllegalStateException("Template not found");
        }
        if (!Objects.equals(taskTemplateDb.getTemplateId(), templateId)) {
            throw new IllegalAccessException("Unable to update template : please provide proper template Ids");
        }
        validateTemplateUpdates(templateRequest, taskTemplateDb);
        TaskTemplate taskTemplate = new TaskTemplate();
        BeanUtils.copyProperties(taskTemplateDb, taskTemplate);
        CommonUtils.copyNonNullProperties(templateRequest, taskTemplateDb);
        WorkFlowTaskStatus templateStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusIdAndFkWorkFlowTypeWorkflowTypeId(templateRequest.getTaskWorkFlowStatus(), templateRequest.getTaskWorkflowId());
        if (templateStatus == null) {
            throw new IllegalStateException("Please provide valid workflow status");
        }
        if (!templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) && !templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
            throw new IllegalStateException("User only allowed to create template with workflow status as 'Backlog' or 'Not-Started'");
        }
        taskTemplateDb.setFkWorkflowTaskStatus(templateStatus);
        if (templateRequest.getOrgId() != null) {
            Organization org = organizationRepository.findByOrgId(templateRequest.getOrgId());
            taskTemplateDb.setFkOrgId(org);
        }
        if (templateRequest.getProjectId() != null) {
            Project project = projectRepository.findByProjectId(templateRequest.getProjectId());
            if (!Objects.equals(project.getOrgId(), taskTemplateDb.getFkOrgId().getOrgId())) {
                throw new IllegalStateException("Project assigned to template do not belong to the provided organization");
            }
            taskTemplateDb.setFkProjectId(project);
        }
        if (templateRequest.getTeamId() != null) {
            Team team = teamRepository.findByTeamId(templateRequest.getTeamId());
            if (!Objects.equals(team.getFkProjectId().getProjectId(), taskTemplateDb.getFkProjectId().getProjectId())) {
                throw new IllegalStateException("Team assigned to template do not belong to the provided project");
            }
            if (!Objects.equals(team.getFkOrgId().getOrgId(), taskTemplateDb.getFkOrgId().getOrgId())) {
                throw new IllegalStateException("Team assigned to template do not belong to the provided organization");
            }
            taskTemplateDb.setFkTeamId(team);
        }
        TaskTemplate savedTaskTemplate= taskTemplateRepository.save(taskTemplateDb);
        auditService.auditForTemplate(userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(CommonUtils.convertToLongList(accountIds),userAccountRepository.findOrgIdByAccountIdAndIsActive(taskTemplateDb.getFkAccountIdCreator().getAccountId(), true).getOrgId(), true), savedTaskTemplate, true);
        TaskTemplateResponse response = createTemplateResponse(savedTaskTemplate, accountIds);
        if (!Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.USER)) {
            try {
                List<HashMap<String, String>> payload = notificationService.updateTaskTemplateNotification(savedTaskTemplate, taskTemplate, accountIds, timeZone);
                taskServiceImpl.sendPushNotification(payload);
            } catch (Exception e) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Notification can not be created for new access domain. Caught Error: " + e, new Throwable(allStackTraces));
            }
        }
        return response;
    }

    /**
     * This method creates custom response for template that is created by the user
     */
    private TaskTemplateResponse createTemplateResponse (TaskTemplate taskTemplate, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        TaskTemplateResponse response = new TaskTemplateResponse();
        CommonUtils.copyNonNullProperties(taskTemplate, response);
        EmailFirstLastAccountId creatorDetails = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdAndIsActive(taskTemplate.getFkAccountIdCreator().getAccountId());
        response.setCreatorDetails(creatorDetails);
        if (taskTemplate.getFkOrgId() != null) {
            OrgIdOrgName orgIdOrgName = new OrgIdOrgName(taskTemplate.getFkOrgId().getOrgId(), taskTemplate.getFkOrgId().getOrganizationName());
            response.setOrg(orgIdOrgName);
        }
        if (taskTemplate.getFkProjectId() != null) {
            ProjectIdProjectName projectIdProjectName = new ProjectIdProjectName(taskTemplate.getFkProjectId().getProjectId(), taskTemplate.getFkProjectId().getProjectName(), taskTemplate.getFkProjectId().getIsDeleted());
            response.setProject(projectIdProjectName);
        }
        if (taskTemplate.getFkTeamId() != null) {
            TeamIdAndTeamName teamIdTeamName = new TeamIdAndTeamName(taskTemplate.getFkTeamId().getTeamId(), taskTemplate.getFkTeamId().getTeamName(), taskTemplate.getFkTeamId().getTeamCode(), taskTemplate.getFkTeamId().getIsDeleted());
            response.setTeam(teamIdTeamName);
        }
        if (response.getTemplateTitle() == null) {
            response.setTemplateTitle(taskTemplate.getTaskTitle());
        }
        Long userId = userAccountRepository.findUserIdByAccountId(headerAccountIds.get(0));
        if (Objects.equals(taskTemplate.getEntityTypeId(), Constants.EntityTypes.USER)) {
            response.setIsEditable(Objects.equals(taskTemplate.getEntityId(), userId));
        } else {
            response.setIsEditable(validateUserPermission(taskTemplate.getEntityId(), taskTemplate.getEntityTypeId(), headerAccountIds));
        }
        return response;
    }

    /**
     * This methods validates the updates of template
     */
    public void validateTemplateUpdates(TaskTemplateRequest templateRequest, TaskTemplate taskTemplateDb) throws IllegalAccessException {
        if (!Objects.equals(taskTemplateDb.getEntityTypeId(), Constants.EntityTypes.USER)) {
            if (!Objects.equals(templateRequest.getEntityId(), taskTemplateDb.getEntityId())) {
                throw new IllegalAccessException("User not allowed to change Entity of the template");
            }
            if (!Objects.equals(templateRequest.getEntityTypeId(), taskTemplateDb.getEntityTypeId())) {
                throw new IllegalAccessException("User not allowed to change Entity of the template");
            }

            if (taskTemplateDb.getFkOrgId() != null && !Objects.equals(taskTemplateDb.getFkOrgId().getOrgId(), templateRequest.getOrgId())) {
                throw new IllegalAccessException("User not allowed to change the organization of template");
            }

            if (taskTemplateDb.getFkProjectId() != null && !Objects.equals(taskTemplateDb.getFkProjectId().getProjectId(), templateRequest.getProjectId())) {
                throw new IllegalAccessException("User not allowed to change the project of template");
            }

            if (taskTemplateDb.getFkTeamId() != null && !Objects.equals(taskTemplateDb.getFkTeamId().getTeamId(), templateRequest.getTeamId())) {
                throw new IllegalAccessException("User not allowed to change the team of template");
            }
        }
    }

    /**
     * This method validates user permissions
     */
    private void validateTemplateCreator (TaskTemplateRequest templateRequest, String accountIds, boolean isUpdateTemplate) {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);

        if (!isUpdateTemplate) {
            validateUserAccount(templateRequest.getFkAccountIdCreator().getAccountId(), headerAccountIdsList);
        }

        //validating if user is member of organization provided in request
        if (templateRequest.getOrgId() != null) {
            validateUserMembership(templateRequest.getOrgId(), Constants.EntityTypes.ORG, Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.USER) ? headerAccountIdsList : Collections.singletonList(templateRequest.getFkAccountIdCreator().getAccountId()));
        }
        //validating if user is member of project provided in request
        if (templateRequest.getProjectId() != null) {
            validateUserMembership(templateRequest.getProjectId(), Constants.EntityTypes.PROJECT, Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.USER) ? headerAccountIdsList : Collections.singletonList(templateRequest.getFkAccountIdCreator().getAccountId()));
        }
        //validating if user is member of team provided in request
        if (templateRequest.getTeamId() != null) {
            validateUserMembership(templateRequest.getTeamId(), Constants.EntityTypes.TEAM, Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.USER) ? headerAccountIdsList : Collections.singletonList(templateRequest.getFkAccountIdCreator().getAccountId()));
        }

        //validating user permissions
        if (!Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.USER)) {
            if (Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.TEAM)) {
                if (templateRequest.getTeamId() == null || !Objects.equals(templateRequest.getTeamId(), templateRequest.getEntityId())) {
                    throw new IllegalStateException("Please provide proper team information");
                }
                if (templateRequest.getProjectId() == null) {
                    throw new IllegalStateException("Please provide project information");
                }
                if (templateRequest.getOrgId() == null) {
                    throw new IllegalStateException("Please provide organization information");
                }
            } else if (Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.PROJECT)) {
                if (templateRequest.getProjectId() == null || !Objects.equals(templateRequest.getProjectId(), templateRequest.getEntityId())) {
                    throw new IllegalStateException("Please provide proper project information");
                }
                if (templateRequest.getOrgId() == null) {
                    throw new IllegalStateException("Please provide organization information");
                }
            } else if (Objects.equals(templateRequest.getEntityTypeId(), Constants.EntityTypes.ORG)) {
                if (templateRequest.getOrgId() == null || !Objects.equals(templateRequest.getOrgId(), templateRequest.getEntityId())) {
                    throw new IllegalStateException("Please provide proper organization information");
                }
            } else {
                throw new IllegalStateException("Please provide valid entity to add a template");
            }
            if (!validateUserPermission(templateRequest.getEntityId(), templateRequest.getEntityTypeId(), headerAccountIdsList)) {
                throw new ValidationFailedException("Insufficient privileges to create a template in the selected entity. Please review your assigned roles and permissions before attempting to create a template");
            }
        }
    }

    /**
     * this verifies if user is using his own account
     */
    private void validateUserAccount (Long accountId, List<Long> headerAccountIdsList) {

        //validating if user is creating his own template and not for someone else
        if (!headerAccountIdsList.contains(accountId)) {
            throw new ValidationFailedException("User is not authorized to access a template services on the behalf of another user");
        }
    }

    /**
     * This method verifies user permissions in the provided entity
     */
    private Boolean validateUserPermission (Long entityId, Integer entityTypeId, List<Long> headerAccountIdsList) {
        List<Integer> roleIdList = new ArrayList<>();
        List<AccountId> accountIdList = new ArrayList<>();
        roleIdList.add(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
        roleIdList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            roleIdList.add(RoleEnum.FORMAL_TEAM_LEAD_LEVEL_2.getRoleId());
            roleIdList.add(RoleEnum.FORMAL_TEAM_LEAD_LEVEL_1.getRoleId());
            roleIdList.add(RoleEnum.TEAM_MANAGER_NON_SPRINT.getRoleId());
            roleIdList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
            accountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityId, roleIdList, true);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            List<Long> projectTeamIdList = teamRepository.findTeamIdsByProjectId(entityId);
            accountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, projectTeamIdList, roleIdList, true);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            List<Long> orgTeamIdList = teamRepository.findTeamIdsByOrgId(entityId);
            accountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, orgTeamIdList, roleIdList, true);
            roleIdList.add(RoleEnum.ORG_ADMIN.getRoleId());
            roleIdList.add(RoleEnum.BACKUP_ORG_ADMIN.getRoleId());
            accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, entityId, roleIdList, true));
        } else {
            throw new IllegalStateException("Please provide valid entity to add a template");
        }

        List<Long> authorizedAccountIds = accountIdList.stream().map(AccountId::getAccountId).collect(Collectors.toList());

        if (CommonUtils.containsAny(headerAccountIdsList, authorizedAccountIds)) {
            return true;
        }
        return false;
    }

    /**
     * This method verifies if user is part of the provided entity or not
     */
    public void validateUserMembership (Long entityId, Integer entityTypeId, List<Long> accountIdsList) {

        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            List<Long> orgMembers = userAccountRepository.findAccountIdByOrgIdAndIsActive(entityId, true).stream().map(AccountId::getAccountId).collect(Collectors.toList());
            if (!CommonUtils.containsAny(accountIdsList, orgMembers)) {
                throw new ValidationFailedException("User not authorized to create template for provided organization");
            }
        }

        if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            List<Long> projectMembers = projectService.getprojectMembersAccountIdList(List.of(entityId)).stream().map(AccountId::getAccountId).collect(Collectors.toList());
            if (!CommonUtils.containsAny(accountIdsList, projectMembers)) {
                throw new ValidationFailedException("User not authorized to create template for provided project");
            }
        }

        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            List<Long> teamMembers = accessDomainRepository.findDistinctAccountIdsByEntityTypeAndEntityTypeIdAndIsActive(entityId, Constants.EntityTypes.TEAM);
            if (!CommonUtils.containsAny(accountIdsList, teamMembers)) {
                throw new ValidationFailedException("User not authorized to create template for provided team");
            }
        }
    }

    /**
     * This method returns all the templates created by the user
     */
    public List<TaskTemplateResponse> getAllUserCreatedTemplate (Long accountId, String accountIds) {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        validateUserAccount(accountId, headerAccountIdsList);
        List<TaskTemplate> taskTemplateList = taskTemplateRepository.findAllTemplatesByFkAccountIdCreatorAccountId(accountId);
        List<TaskTemplateResponse> responseList = new ArrayList<>();
        for (TaskTemplate taskTemplate : taskTemplateList) {
            TaskTemplateResponse taskTemplateResponse = createTemplateResponse(taskTemplate, accountIds);
            responseList.add(taskTemplateResponse);
        }
        List<PersonalTaskTemplate> personalTaskTemplateList = personalTaskTemplateRepository.findAllTemplatesByFkAccountIdAccountId(accountId);
        for (PersonalTaskTemplate personalTaskTemplate : personalTaskTemplateList) {
            TaskTemplateResponse taskTemplateResponse = personalTemplateService.createPersonalTaskTemplateResponse(personalTaskTemplate);
            responseList.add(taskTemplateResponse);
        }
        responseList.sort(Comparator.comparing(TaskTemplateResponse::getTemplateNumber, Comparator.reverseOrder()));
        return responseList;
    }


    /**
     * This method returns all the templates for an entity
     */
    public List<TaskTemplateResponse> getAllTemplateForEntity (Long entityId, Integer entityTypeId, String accountIds) {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        validateUserMembership(entityId, entityTypeId, headerAccountIdsList);
        List<TaskTemplate> taskTemplateList = taskTemplateRepository.findAllTemplateByEntityIdAndEntityTypeId(entityId, entityTypeId);
        List<TaskTemplateResponse> responseList = new ArrayList<>();
        if (Objects.equals(entityId, Constants.PERSONAL_TEAM_ID) && !Objects.equals(entityTypeId, Constants.EntityTypes.USER)) {
            List<PersonalTaskTemplate> personalTaskTemplateList = personalTaskTemplateRepository.findAllTemplatesByFkAccountIdAccountIdIn(headerAccountIdsList);
            for (PersonalTaskTemplate personalTaskTemplate : personalTaskTemplateList) {
                TaskTemplateResponse taskTemplateResponse = personalTemplateService.createPersonalTaskTemplateResponse(personalTaskTemplate);
                responseList.add(taskTemplateResponse);
            }
        }
        for (TaskTemplate taskTemplate : taskTemplateList) {
            TaskTemplateResponse taskTemplateResponse = createTemplateResponse(taskTemplate,accountIds);
            responseList.add(taskTemplateResponse);
        }
        responseList.sort(Comparator.comparing(TaskTemplateResponse::getTemplateNumber, Comparator.reverseOrder()));
        return responseList;
    }

    /**
     * This method returns all the templates that user can create
     */
    public List<TaskTemplateResponse> getUserTemplates (String accountIds) {
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        List<Long> orgIdList = userAccountRepository.getAllOrgIdByAccountIdInAndIsActive(accountIdList, true);
        List<Long> teamIdList = accessDomainRepository.findDistinctEntityIdsByActiveAccountIds(Constants.EntityTypes.TEAM, accountIdList);
        List<Long> projectIdList = projectService.getUserAllProjectId(accountIdList);
        List<TaskTemplate> taskTemplateList = taskTemplateRepository.findAllUserTemplates(orgIdList,projectIdList,teamIdList,accountIdList);
        List<TaskTemplateResponse> responseList = new ArrayList<>();
        for (TaskTemplate taskTemplate : taskTemplateList) {
            TaskTemplateResponse taskTemplateResponse = createTemplateResponse(taskTemplate, accountIds);
            responseList.add(taskTemplateResponse);
        }
        List<PersonalTaskTemplate> personalTaskTemplateList = personalTaskTemplateRepository.findAllTemplatesByFkAccountIdAccountIdIn(accountIdList);
        for (PersonalTaskTemplate personalTaskTemplate : personalTaskTemplateList) {
            TaskTemplateResponse taskTemplateResponse = personalTemplateService.createPersonalTaskTemplateResponse(personalTaskTemplate);
            responseList.add(taskTemplateResponse);
        }
        responseList.sort(Comparator.comparing(TaskTemplateResponse::getTemplateNumber, Comparator.reverseOrder()));
        return responseList;
    }

    public TaskTemplateResponse getTemplateById (Long templateId, String accountIds) {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        TaskTemplate taskTemplate = taskTemplateRepository.findByTemplateId(templateId);
        TaskTemplateResponse taskTemplateResponse = new TaskTemplateResponse();
        if (taskTemplate != null) {
            if (!Objects.equals(taskTemplate.getEntityTypeId(), Constants.EntityTypes.USER)) {
                if (!validateUserPermission(taskTemplate.getEntityId(), taskTemplate.getEntityTypeId(), headerAccountIdsList)) {
                    throw new ValidationFailedException("Insufficient privileges to create a template in the selected entity. Please review your assigned roles and permissions before attempting to create a template");
                }
            } else {
                List<Long> accountIdList = userAccountRepository.findAllAccountIdsByUserIdAndIsActive(taskTemplate.getEntityId(), true);
                if (CommonUtils.containsAny(accountIdList, headerAccountIdsList)) {
                    throw new ValidationFailedException("User not authorized to access template");
                }
            }
            taskTemplateResponse = createTemplateResponse(taskTemplate, accountIds);
        }
        return taskTemplateResponse;
    }

    public List<String> getUpdatedFields(TaskTemplate updatedTemplate, TaskTemplate taskTemplate) {
        List<String> updatedFields = new ArrayList<>();

        if (!Objects.equals(updatedTemplate.getTaskTitle(), taskTemplate.getTaskTitle())) {
            updatedFields.add("taskTitle");
        }
        if (!Objects.equals(updatedTemplate.getTemplateTitle(), taskTemplate.getTemplateTitle())) {
            updatedFields.add("templateTitle");
        }
        if (!Objects.equals(updatedTemplate.getTaskDesc(), taskTemplate.getTaskDesc())) {
            updatedFields.add("taskDesc");
        }
        if (!Objects.equals(updatedTemplate.getTaskWorkflowId(), taskTemplate.getTaskWorkflowId())) {
            updatedFields.add("taskWorkflowId");
        }
        if (!Objects.equals(updatedTemplate.getTaskEstimate(), taskTemplate.getTaskEstimate())) {
            updatedFields.add("taskEstimate");
        }
        if (!Objects.equals(updatedTemplate.getTaskPriority(), taskTemplate.getTaskPriority())) {
            updatedFields.add("taskPriority");
        }
        if (!Objects.equals(updatedTemplate.getFkWorkflowTaskStatus(), taskTemplate.getFkWorkflowTaskStatus())) {
            updatedFields.add("fkWorkflowTaskStatus");
        }
        if (!Objects.equals(updatedTemplate.getFkTeamId(), taskTemplate.getFkTeamId())) {
            updatedFields.add("fkTeamId");
        }
        if (!Objects.equals(updatedTemplate.getFkProjectId(), taskTemplate.getFkProjectId())) {
            updatedFields.add("fkProjectId");
        }
        if (!Objects.equals(updatedTemplate.getFkOrgId(), taskTemplate.getFkOrgId())) {
            updatedFields.add("fkOrgId");
        }

        return updatedFields;
    }

    public void removeLeadingAndTrailingSpacesForTemplate (TaskTemplateRequest taskTemplateRequest) {
        if (taskTemplateRequest.getTaskTitle() != null) {
            taskTemplateRequest.setTaskTitle(taskTemplateRequest.getTaskTitle().trim());
        }
        if (taskTemplateRequest.getTaskDesc() != null) {
            taskTemplateRequest.setTaskDesc(taskTemplateRequest.getTaskDesc().trim());
        }
        if (taskTemplateRequest.getTemplateTitle() != null) {
            taskTemplateRequest.setTemplateTitle(taskTemplateRequest.getTemplateTitle().trim());
        }
    }
}
