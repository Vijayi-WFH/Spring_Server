package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.ProjectIdProjectName;
import com.tse.core_application.custom.model.TeamIdAndTeamName;
import com.tse.core_application.dto.TaskTemplateResponse;
import com.tse.core_application.dto.personal_task.PersonalTaskTemplateRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.*;
import com.tse.core_application.model.personal_task.PersonalTaskTemplate;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PersonalTemplateService {
    private static final Logger logger = LogManager.getLogger(PersonalTemplateService.class.getName());

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

    /**
     * This method creates personal task template for personal users, since personal task templates are individual templates, we keep accountId as the unique identifier
     */
    public TaskTemplateResponse addPersonalTaskTemplate (PersonalTaskTemplateRequest templateRequest, String accountIds) {
        UserAccount userAccount = validatePersonalTaskTemplateCreator(Long.parseLong(accountIds));
        PersonalTaskTemplate taskTemplate = new PersonalTaskTemplate();
        CommonUtils.copyNonNullProperties(templateRequest, taskTemplate);
        Long templateNumberDb = personalTaskTemplateRepository.getMaxTemplateNumber();
        if (templateNumberDb == null) {
            taskTemplate.setTemplateNumber(ControllerConstants.TASK_TEMPLATE_NUMBER_START);
        } else {
            taskTemplate.setTemplateNumber(templateNumberDb + 1);
        }
        taskTemplate.setFkAccountId(userAccount);
        WorkFlowTaskStatus templateStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusIdAndFkWorkFlowTypeWorkflowTypeId(templateRequest.getTaskWorkFlowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);
        if (templateStatus == null) {
            throw new IllegalStateException("Please provide valid workflow status");
        }
        if (!templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) && !templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DUE_DATE_NOT_PROVIDED)) {
            throw new IllegalStateException("Invalid workflow status");
        }
        taskTemplate.setFkWorkflowTaskStatus(templateStatus);
        Organization org = organizationRepository.findByOrgId(Constants.OrgIds.PERSONAL.longValue());
        taskTemplate.setFkOrgId(org);
        Project project = projectRepository.findByProjectId(Constants.PERSONAL_PROJECT_ID);
        taskTemplate.setFkProjectId(project);

        Team team = teamRepository.findByTeamId(Constants.PERSONAL_TEAM_ID);
        taskTemplate.setFkTeamId(team);
        if (taskTemplate.getTemplateTitle() == null) {
            taskTemplate.setTemplateTitle(templateRequest.getTemplateTitle());
        }
        PersonalTaskTemplate savedTaskTemplate = personalTaskTemplateRepository.save(taskTemplate);
        return createPersonalTaskTemplateResponse(savedTaskTemplate);
    }

    /**
     * This method validates whether the user is accessing the api through his/her account and belongs to the personal organization
     */
    public UserAccount validatePersonalTaskTemplateCreator (Long accountIds) {

        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(Collections.singletonList(accountIds), Constants.OrgIds.PERSONAL.longValue(), true);
        if (userAccount == null) {
            throw new ValidationFailedException("User is not registered to personal organization");
        }
        return userAccount;
    }

    /**
     * This api creates a task template response object from personal task template object so that all the templates can be listed on the frontend together
     */
    public TaskTemplateResponse createPersonalTaskTemplateResponse (PersonalTaskTemplate taskTemplate) {
        TaskTemplateResponse response = new TaskTemplateResponse();
        CommonUtils.copyNonNullProperties(taskTemplate, response);
        EmailFirstLastAccountId creatorDetails = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdAndIsActive(taskTemplate.getFkAccountId().getAccountId());
        response.setCreatorDetails(creatorDetails);
        OrgIdOrgName orgIdOrgName = new OrgIdOrgName(taskTemplate.getFkOrgId().getOrgId(), taskTemplate.getFkOrgId().getOrganizationName());
        response.setOrg(orgIdOrgName);
        ProjectIdProjectName projectIdProjectName = new ProjectIdProjectName(taskTemplate.getFkProjectId().getProjectId(), taskTemplate.getFkProjectId().getProjectName(), taskTemplate.getFkProjectId().getIsDeleted());
        response.setProject(projectIdProjectName);
        TeamIdAndTeamName teamIdTeamName = new TeamIdAndTeamName(taskTemplate.getFkTeamId().getTeamId(), Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS, taskTemplate.getFkTeamId().getTeamCode(), taskTemplate.getFkTeamId().getIsDeleted());
        response.setTeam(teamIdTeamName);
        if (response.getTemplateTitle() == null) {
            response.setTemplateTitle(taskTemplate.getTaskTitle());
        }
        response.setEntityId(Constants.PERSONAL_TEAM_ID);
        response.setEntityTypeId(Constants.EntityTypes.TEAM);
        response.setIsEditable(true);
        return response;
    }

    /**
     * This method updates the personal task templates
     */
    public TaskTemplateResponse updatePersonalTemplate (PersonalTaskTemplateRequest templateRequest, String accountIds, Long templateId, String timeZone) throws IllegalAccessException {
        validatePersonalTaskTemplateCreator(Long.parseLong(accountIds));
        PersonalTaskTemplate taskTemplateDb = personalTaskTemplateRepository.findByTemplateId(templateId);
        if (taskTemplateDb == null) {
            throw new IllegalStateException("Template not found");
        }
        PersonalTaskTemplate taskTemplate = new PersonalTaskTemplate();
        BeanUtils.copyProperties(taskTemplateDb, taskTemplate);
        CommonUtils.copyNonNullProperties(templateRequest, taskTemplate);
        WorkFlowTaskStatus templateStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusIdAndFkWorkFlowTypeWorkflowTypeId(templateRequest.getTaskWorkFlowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);
        if (templateStatus == null) {
            throw new IllegalStateException("Please provide valid workflow status");
        }
        if (!templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) && !templateStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DUE_DATE_NOT_PROVIDED)) {
            throw new IllegalStateException("Invalid workflow status");
        }
        taskTemplate.setFkWorkflowTaskStatus(templateStatus);
        PersonalTaskTemplate savedTaskTemplate= personalTaskTemplateRepository.save(taskTemplate);
        return createPersonalTaskTemplateResponse(savedTaskTemplate);
    }

    public TaskTemplateResponse getPersonalTemplateById (Long templateId, String accountIds) {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        PersonalTaskTemplate taskTemplate = personalTaskTemplateRepository.findByTemplateId(templateId);
        TaskTemplateResponse taskTemplateResponse = new TaskTemplateResponse();
        if (taskTemplate != null) {
            if (!headerAccountIdsList.contains(taskTemplate.getFkAccountId().getAccountId())) {
                throw new ValidationFailedException("User not allowed to view personal template of other users");
            }
            taskTemplateResponse = createPersonalTaskTemplateResponse(taskTemplate);
        }
        return taskTemplateResponse;
    }
}
