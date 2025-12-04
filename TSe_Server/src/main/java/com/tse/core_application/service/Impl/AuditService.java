package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.leave.Response.LeaveApplicationResponse;
import com.tse.core_application.model.*;
import com.tse.core_application.dto.RegistrationRequest;
import com.tse.core_application.model.performance_notes.PerfNote;
import com.tse.core_application.model.personal_task.PersonalTask;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AuditService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    //  to create audit for add, delete and update task
    public Audit createAudit(Task task, Integer noOfAudit, Long taskId, String fieldsUpdated) {
        Audit addAudit = new Audit();
        // we are setting accountId of the user who is updating/ creating the task
        addAudit.setAccountId(task.getFkAccountIdLastUpdated().getAccountId());
        addAudit.setAffectedEntityId(task.getTaskId());
        addAudit.setAffectedEntityTypeId(Constants.EntityTypes.TASK);
        // we are setting accountId of the user who is updating/ creating the task
        UserAccount userDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdLastUpdated().getAccountId());
        addAudit.setUserId(userDb.getFkUserId().getUserId());
        UserName nameDb = userRepository.findFirstNameAndLastNameByUserId(userDb.getFkUserId().getUserId());
        String lastNameDb = nameDb.getLastName();
        String firstNameDb = nameDb.getFirstName();
        String taskNumberDb = task.getTaskNumber();
        String taskTitleDb = task.getTaskTitle();

        if (taskId == null) {
            if (noOfAudit == 1) {
                String message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has added the Work Item" + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
                addAudit.setMessageForUser(message);
                return addAudit;
            } else {
                UserAccount userAssigned = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdAssigned().getAccountId());
                UserName userAssignedName = userRepository.findFirstNameAndLastNameByUserId(userAssigned.getFkUserId().getUserId());
                String userAssignedLastName = userAssignedName.getLastName();
                String userAssignedFirstName = userAssignedName.getFirstName();
                String message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has assigned the Work Item" + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]" + " " + "to user" + '"' + userAssignedLastName + "," + userAssignedFirstName + '"';
                addAudit.setMessageForUser(message);
                return addAudit;
            }
        } else {
            // ToDo : this, if condition is a temporary fix to remove the attachments of comments from the audit
            //  Task object as we are getting Blob error possible solution for this is
            //  to use DTO instead of the whole TASK entity Object.
            if(task.getComments() != null){
                task.getComments().forEach(comment -> {
                    if(comment.getTaskAttachments() != null){
                        comment.setTaskAttachments(null);
                    }
                });
            }
            if (fieldsUpdated == null) {
                String message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has deleted the Work Item" + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
                addAudit.setMessageForUser(message);
                return addAudit;
            } else {
                if (fieldsUpdated.equalsIgnoreCase("fkAccountIdAssigned") && (task.getFkAccountIdAssigned() != null && task.getFkAccountIdAssigned().getAccountId() != null)) {
                    UserAccount assignedUserDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdAssigned().getAccountId());
                    UserName assignedUserNameDb = userRepository.findFirstNameAndLastNameByUserId(assignedUserDb.getFkUserId().getUserId());
                    String assignedUserLastNameDb = assignedUserNameDb.getLastName();
                    String assignedUserFirstNameDb = assignedUserNameDb.getFirstName();
                    String message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has updated" + '"' + fieldsUpdated + '"' + "to" + '"' + assignedUserLastNameDb + "," + assignedUserFirstNameDb + '"' + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
                    addAudit.setMessageForUser(message);
                    return addAudit;

                } else {
                    String message = null;
                    HashMap<String, Object> map1 = objectMapper.convertValue(task, HashMap.class);
                    List<String> fieldsList = new ArrayList<>();
                    fieldsList.add("fkAccountIdMentor1");
                    fieldsList.add("fkAccountIdMentor2");
                    fieldsList.add("fkAccountIdObserver1");
                    fieldsList.add("fkAccountIdObserver2");
                        if(!fieldsList.contains(fieldsUpdated)) {
                            Object fieldsUpdatedValue = map1.get(fieldsUpdated);
                            message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has updated" + '"' + fieldsUpdated + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
                        } else {
                            UserAccount assignedUserDb = null;
                            if(fieldsUpdated.equalsIgnoreCase("fkAccountIdMentor1")) {
                                if(task.getFkAccountIdMentor1()!=null)
                                assignedUserDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdMentor1().getAccountId());
                            } else {
                                if(fieldsUpdated.equalsIgnoreCase("fkAccountIdMentor2")) {
                                    if(task.getFkAccountIdMentor2()!=null)
                                    assignedUserDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdMentor2().getAccountId());
                                } else {
                                    if(fieldsUpdated.equalsIgnoreCase("fkAccountIdObserver1")) {
                                        if(task.getFkAccountIdObserver1()!=null)
                                        assignedUserDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdObserver1().getAccountId());
                                    } else {
                                        if(task.getFkAccountIdObserver2()!=null)
                                        assignedUserDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountIdObserver2().getAccountId());
                                    }
                                }
                            }
                            if(assignedUserDb!=null) {
                                UserName assignedUserNameDb = userRepository.findFirstNameAndLastNameByUserId(assignedUserDb.getFkUserId().getUserId());
                                String assignedUserLastNameDb = assignedUserNameDb.getLastName();
                                String assignedUserFirstNameDb = assignedUserNameDb.getFirstName();
                                message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has updated" + '"' + fieldsUpdated + '"' + "to" + '"' + assignedUserLastNameDb + "," + assignedUserFirstNameDb + '"' + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
                            }
                            else {
                                message = '"' + lastNameDb + ',' + firstNameDb + '"' + " " + "has updated" + '"' + fieldsUpdated + '"' + "to" + '"' + "null" + '"' + " " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
                            }
                        }
                    addAudit.setMessageForUser(message);
                    return addAudit;
                }
            }
        }
    }

    /** method to audit creation of add personal task */
    public void createAuditForAddPersonalTask(PersonalTask task) {
        Audit addAudit = new Audit();
        // we are setting accountId of the user who is updating/creating the task
        addAudit.setAccountId(task.getFkAccountId().getAccountId());
        addAudit.setAffectedEntityId(task.getPersonalTaskId());
        addAudit.setAffectedEntityTypeId(Constants.EntityTypes.TASK);
        UserAccount userDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountId().getAccountId());
        addAudit.setUserId(userDb.getFkUserId().getUserId());
        UserName nameDb = userRepository.findFirstNameAndLastNameByUserId(userDb.getFkUserId().getUserId());
        String lastNameDb = nameDb.getLastName();
        String firstNameDb = nameDb.getFirstName();
        String taskNumberDb = task.getPersonalTaskNumber();
        String taskTitleDb = task.getTaskTitle();

        String message = '"' + firstNameDb + ' ' + lastNameDb + '"' + " has added the Work Item " + "[" + taskNumberDb + "-" + taskTitleDb + "]";
        addAudit.setMessageForUser(message);
        auditRepository.save(addAudit);
    }

    /** method to audit update of a personal task */
    public void createAuditForUpdatePersonalTask(PersonalTask task, String updatedFields) {
        Audit addAudit = new Audit();
        // we are setting accountId of the user who is updating the task
        // since this is personal task it can only be updated by the user who created this
        addAudit.setAccountId(task.getFkAccountId().getAccountId());
        addAudit.setAffectedEntityId(task.getPersonalTaskId());
        addAudit.setAffectedEntityTypeId(Constants.EntityTypes.TASK);
        UserAccount userDb = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(task.getFkAccountId().getAccountId());
        addAudit.setUserId(userDb.getFkUserId().getUserId());
        UserName nameDb = userRepository.findFirstNameAndLastNameByUserId(userDb.getFkUserId().getUserId());
        String lastNameDb = nameDb.getLastName();
        String firstNameDb = nameDb.getFirstName();
        String taskNumberDb = task.getPersonalTaskNumber();
        String taskTitleDb = task.getTaskTitle();
        String message = '"' + firstNameDb + ' ' + lastNameDb + '"' + " has updated the Work Item [" + taskNumberDb + "-" + taskTitleDb + "] with fields: " + updatedFields;

        addAudit.setMessageForUser(message);
        auditRepository.save(addAudit);
    }

    //  to create audit for login and sign up
    public Audit auditForSignUpAndLogin(RegistrationRequest request, AuthRequest authRequest) {
        Audit addAudit = new Audit();
        if (request != null) {
            List<UserAccount> usersDb = userAccountRepository.findByEmail(request.getPrimaryEmail());
            addAudit.setUserId(usersDb.get(0).getFkUserId().getUserId());
            if(usersDb.size() > 1) {
                addAudit.setAccountId(0L);
            } else {
                addAudit.setAccountId(usersDb.get(0).getAccountId());
            }
            addAudit.setAffectedEntityTypeId(Constants.EntityTypes.USER);
            addAudit.setAffectedEntityId(usersDb.get(0).getFkUserId().getUserId());
            String userFirstName = request.getFirstName();
            String userLastName = request.getLastName();
            String userLoggedInAt = getCurrentTimeStamp();
            String message = '"' + userLastName + ',' + userFirstName + '"' + " " + " has logged in at" + " " + userLoggedInAt;
            addAudit.setMessageForUser(message);
            return addAudit;
        } else {
            User userDb = userRepository.findByPrimaryEmail(authRequest.getUsername());
            addAudit.setUserId(userDb.getUserId());
            List<UserAccount> userAccountsFoundDb = userAccountRepository.findByEmail(authRequest.getUsername());
            if(userAccountsFoundDb.size() > 1) {
                addAudit.setAccountId(0L);
            } else {
                addAudit.setAccountId(userAccountsFoundDb.get(0).getAccountId());
            }
            addAudit.setAffectedEntityTypeId(Constants.EntityTypes.USER);
            addAudit.setAffectedEntityId(userDb.getUserId());
            String userFirstName = userDb.getFirstName();
            String userLastName = userDb.getLastName();
            String userLoggedInAt = getCurrentTimeStamp();
            String message = '"' + userLastName + ',' + userFirstName + '"' + " " + " has logged in at" + " " + userLoggedInAt;
            addAudit.setMessageForUser(message);
            return addAudit;

        }
    }

    //  to get the system's current date and time
    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        return sdfDate.format(now);
    }

    //  audit when team members are inserted in accessDomain table
    public Audit auditForAddedTeamMembers(Long teamId, AccessDomain accessDomain) {
        Audit auditToInsert = new Audit();
        Team teamDb = teamRepository.findByTeamId(teamId);
        auditToInsert.setAccountId(teamDb.getFkOwnerAccountId().getAccountId());
        auditToInsert.setAffectedEntityId(teamDb.getTeamId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        UserAccount userAccount = userAccountRepository.findByAccountId(accessDomain.getAccountId());
        UserAccount userAccountDb = userAccountRepository.findByAccountId(teamDb.getFkOwnerAccountId().getAccountId());
        auditToInsert.setUserId(userAccountDb.getFkUserId().getUserId());
        RoleName roleName = roleRepository.findRoleNameByRoleId(accessDomain.getRoleId());
        String messageForUser = "The user  " + '"' + userAccount.getEmail() + '"' + "  has been assigned with a role  " + '"' + roleName.getRoleName() + '"' + " in team  " + teamId;
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    //  audit when project members are inserted in accessDomain table
    public Audit auditForAddedProjectMember(Project project, AccessDomain accessDomain, Long creatorOrModifierAdminId) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(creatorOrModifierAdminId);
        auditToInsert.setAffectedEntityId(project.getProjectId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.PROJECT);
        UserAccount userAccount = userAccountRepository.findByAccountId(accessDomain.getAccountId());
        UserAccount userAccountUpdatingProject = userAccountRepository.findByAccountId(creatorOrModifierAdminId);
        auditToInsert.setUserId(userAccountUpdatingProject.getFkUserId().getUserId());
        RoleName roleName = roleRepository.findRoleNameByRoleId(accessDomain.getRoleId());
        String messageForUser = "The user  " + '"' + userAccount.getEmail() + '"' + "  has been assigned with a role  " + '"' + roleName.getRoleName() + '"' + " in project  " + project.getProjectName() + " with Id: " + project.getProjectId() + " by admin " + userAccountUpdatingProject.getEmail();
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForEditedTeamMembers(Long teamId, AccessDomain accessDomain, Integer oldRoleId) {
        Audit auditToInsert = new Audit();
        Team teamDb = teamRepository.findByTeamId(teamId);
        auditToInsert.setAccountId(teamDb.getFkOwnerAccountId().getAccountId());
        auditToInsert.setAffectedEntityId(teamDb.getTeamId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        UserAccount userAccount = userAccountRepository.findByAccountId(accessDomain.getAccountId());
        UserAccount userAccountDb = userAccountRepository.findByAccountId(teamDb.getFkOwnerAccountId().getAccountId());
        auditToInsert.setUserId(userAccountDb.getFkUserId().getUserId());
        RoleName roleName = roleRepository.findRoleNameByRoleId(accessDomain.getRoleId());
        String messageForUser = "The user  " + '"' + userAccount.getEmail() + '"' + "  role has been updated. It has been assigned with a new role  " + '"' + roleName.getRoleName() + '"' + " in team  " + teamId + " .The previous role of the user was " + RoleEnum.valueOf(oldRoleId).getRoleName();
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public void auditForEditedProjectMember(Project project, AccessDomain accessDomain, Long creatorOrModifierAdminId, Integer oldRoleId) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(creatorOrModifierAdminId);
        auditToInsert.setAffectedEntityId(project.getProjectId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.PROJECT);
        UserAccount userAccount = userAccountRepository.findByAccountId(accessDomain.getAccountId());
        UserAccount userAccountUpdatingProject = userAccountRepository.findByAccountId(creatorOrModifierAdminId);
        auditToInsert.setUserId(userAccountUpdatingProject.getFkUserId().getUserId());
        RoleName roleName = roleRepository.findRoleNameByRoleId(accessDomain.getRoleId());
        String messageForUser = "The user  " + '"' + userAccount.getEmail() + '"' + "  role has been updated. It has been assigned with a new role  " + '"' + roleName.getRoleName() + '"' + " in project  " + project.getProjectName() + " with projectId: " + project.getProjectId() + " . The previous role of the user was " + RoleEnum.valueOf(oldRoleId).getRoleName();
        auditToInsert.setMessageForUser(messageForUser);
        auditRepository.save(auditToInsert);
    }

    //  audit when team members are deleted from accessDomain table
    public Audit auditForDeletedTeamMembers(Long teamId, String email, String roleName) {
        Audit auditToInsert = new Audit();
        Team teamDb = teamRepository.findByTeamId(teamId);
        auditToInsert.setAccountId(teamDb.getFkOwnerAccountId().getAccountId());
        auditToInsert.setAffectedEntityId(teamDb.getTeamId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(teamDb.getFkOwnerAccountId().getAccountId(), true);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The user " + '"' + email + '"' + "  with role  " + roleName + "  has been deleted from team  " + teamId + " by the team admin";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public void auditForDeletedProjectMember(Project project, AccessDomain accessDomain, Long creatorOrModifierAdminId) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(creatorOrModifierAdminId);
        auditToInsert.setAffectedEntityId(project.getProjectId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.PROJECT);
        UserAccount userAccount = userAccountRepository.findByAccountId(accessDomain.getAccountId());
        UserAccount userAccountUpdatingProject = userAccountRepository.findByAccountId(creatorOrModifierAdminId);
        auditToInsert.setUserId(userAccountUpdatingProject.getFkUserId().getUserId());
        String messageForUser = "The user " + '"' + userAccount.getEmail() + '"' + "  with role  " + RoleEnum.valueOf(accessDomain.getRoleId()) + "  has been deleted from project  " + project.getProjectName() + " with projectId: "  + project.getProjectId() + "by the admin: " + userAccountUpdatingProject.getEmail();
        auditToInsert.setMessageForUser(messageForUser);
        auditRepository.save(auditToInsert);
    }

    public Audit auditForDeletedOrgMember(Long orgId, Long accountIdOfRemovedUser) {
        Audit auditToInsert = new Audit();
        AccessDomain orgAdminAccessDomain = accessDomainRepository.findByEntityTypeIdAndEntityIdAndRoleIdAndIsActive(Constants.EntityTypes.ORG, orgId, RoleEnum.ORG_ADMIN.getRoleId(), true);
        auditToInsert.setAccountId(orgAdminAccessDomain.getAccountId());
        auditToInsert.setAffectedEntityId(orgAdminAccessDomain.getEntityId().longValue());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.ORG);
        UserAccount userAccountOrgAdmin = userAccountRepository.findByAccountIdAndIsActive(orgAdminAccessDomain.getAccountId(), true);
        auditToInsert.setUserId(userAccountOrgAdmin.getFkUserId().getUserId());
        String messageForUser = "The user with accountId " + '"' + accountIdOfRemovedUser + '"' + "  has been deleted from org  " + orgId + " by the org admin";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    //  audit when team is created
    public Audit auditForCreateTeam(Team team) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(team.getFkOwnerAccountId().getAccountId());
        auditToInsert.setAffectedEntityId(team.getTeamId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        UserAccount userAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(team.getFkOwnerAccountId().getAccountId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The team  " + '"' + team.getTeamId() + '"' + "  has been created";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    // audit for new Project
    public Audit auditForCreateProject(Project project) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(project.getOwnerAccountId());
        auditToInsert.setAffectedEntityId(project.getProjectId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.PROJECT);
        UserAccount userAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(project.getOwnerAccountId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The project  with id: " + '"' + project.getProjectId() + '"' + "and name: " +  '"' + project.getProjectName() + '"' + "  has been created";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    //  audit when team is updated
    public Audit auditForUpdateTeam(Team team) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(team.getFkOwnerAccountId().getAccountId());
        auditToInsert.setAffectedEntityId(team.getTeamId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        UserAccount userAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(team.getFkOwnerAccountId().getAccountId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The team  " + '"' + team.getTeamId() + '"' + "  has been updated";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    //  audit when loggedIn user has different timeZone
    public Audit auditForDifferentTimeZone(User user, String timeZone) {
        Audit auditToInsert = new Audit();
        List<UserAccount> userAccounts = userAccountRepository.findByEmail(user.getPrimaryEmail());
        auditToInsert.setAccountId(userAccounts.get(0).getAccountId());
        auditToInsert.setAffectedEntityId(user.getUserId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(user.getUserId());
        String initialTimeZone = user.getTimeZone();
        String messageForUser = "TimeZone has been updated from  " + '"' + initialTimeZone + '"' + " to " + '"' + timeZone + '"' +
                " for userId " + "=" + user.getUserId();
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    //  to create audit when new org is registered
    public Audit auditForNewOrg(UserAccount userAccount, Organization organization) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(organization.getOrgId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.ORG);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has registered with new"
                + " organization" + " " + '"' + organization.getOrganizationName() + '"';
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForLoginWithGoogle(String username, String sub) {
        Audit addAudit = new Audit();

            User userDb = userRepository.findByPrimaryEmail(username);
            addAudit.setUserId(userDb.getUserId());
            List<UserAccount> userAccountsFoundDb = userAccountRepository.findByEmail(username);
            if(userAccountsFoundDb.size() > 1) {
                addAudit.setAccountId(0L);
            } else {
                addAudit.setAccountId(userAccountsFoundDb.get(0).getAccountId());
            }
            addAudit.setAffectedEntityTypeId(Constants.EntityTypes.USER);
            addAudit.setAffectedEntityId(userDb.getUserId());
            String userFirstName = userDb.getFirstName();
            String userLastName = userDb.getLastName();
            String userLoggedInAt = getCurrentTimeStamp();
            String message = '"' + userLastName + ',' + userFirstName + '"' + " " + " has logged in with GOOGLE with SUB \""+sub+"\" at" + " " + userLoggedInAt;
            addAudit.setMessageForUser(message);
            return addAudit;


    }

    public Audit auditForBlockedRegistration(UserAccount userAccount, BlockedRegistration blockedRegistration, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(blockedRegistration.getBlockedRegistrationId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " blocked user" + " " + '"' + blockedRegistration.getEmail() + '"';
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForExceptionalRegistration(UserAccount userAccount, ExceptionalRegistration exceptionalRegistration, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(exceptionalRegistration.getExceptionalRegistrationId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " exceptional user" + " " + '"' + exceptionalRegistration.getEmail() + '"';
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForRestrictedDomain(UserAccount userAccount, RestrictedDomains restrictedDomains, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(restrictedDomains.getRestrictedDomainId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " domain" + " " + '"' + restrictedDomains.getDisplayName() + '"';
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForAccountDeactivateReactivate(UserAccount userAccount, Long accountId, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(accountId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " account id " + accountId;
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForOrgDeactivateReactivate(UserAccount userAccount, Long orgId, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(orgId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " organization id " + orgId;
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForUserDeactivateReactivate(UserAccount userAccount, User user, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(user.getUserId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " user " + user.getPrimaryEmail();
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForOrgLimits(UserAccount userAccount, Long orgId, Constants.AuditStatusEnum auditStatusEnum) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(orgId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.ORG);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "A user with userId " + "=" + userAccount.getFkUserId().getUserId() + " has " + auditStatusEnum.getType()
                + " limits of organization with organization id " + orgId;
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForCreateSprint(String accountIds, Sprint sprint) {
        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(sprint.getEntityId());
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(CommonUtils.convertToLongList(accountIds), orgId, true);
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(sprint.getEntityId());
        auditToInsert.setAffectedEntityTypeId(sprint.getEntityTypeId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The sprint with sprint title  " + '"' + sprint.getSprintTitle() + '"' + " and id : " + sprint.getSprintId() + "  was created";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForUpdateSprint(String  accountIds, Sprint sprint) {
        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(sprint.getEntityId());
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(CommonUtils.convertToLongList(accountIds), orgId, true);
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(sprint.getEntityId());
        auditToInsert.setAffectedEntityTypeId(sprint.getEntityTypeId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The sprint with sprint title  " + '"' + sprint.getSprintTitle() + '"' + " and id : " + sprint.getSprintId() + "  was updated";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForSprintStatusUpdate(String accountIds, Sprint sprint, Integer sprintStatus) {
        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(sprint.getEntityId());
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(CommonUtils.convertToLongList(accountIds), orgId, true);
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(sprint.getEntityId());
        auditToInsert.setAffectedEntityTypeId(sprint.getEntityTypeId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The sprint with sprint title  " + '"' + sprint.getSprintTitle() + '"' + " and id : " + sprint.getSprintId() + "  was " + Constants.SprintStatusEnum.getById(sprintStatus).getSprintStatus();
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForSprintTaskMovement(String accountIds, Long teamId) {
        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(teamId);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(CommonUtils.convertToLongList(accountIds), orgId, true);
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(teamId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "Tasks in sprint of team  " + '"' + teamId + "  were either moved, removed or added.";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForMeeting(UserAccount userAccount, Meeting meeting, Boolean updated) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        if(Objects.nonNull(meeting.getTeamId())){
            auditToInsert.setAffectedEntityId(meeting.getTeamId());
            auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        }
        else if(Objects.nonNull(meeting.getProjectId())){
            auditToInsert.setAffectedEntityId(meeting.getProjectId());
            auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.PROJECT);
        }
        else if(Objects.nonNull(meeting.getOrgId())){
            auditToInsert.setAffectedEntityId(meeting.getOrgId());
            auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.ORG);
        }
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser ="The meeting  " + '"' + meeting.getMeetingNumber() + '"' + ( updated ? "was updated" : "  was created");
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForUpdateProject(Project project) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(project.getOwnerAccountId());
        auditToInsert.setAffectedEntityId(project.getProjectId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.PROJECT);
        UserAccount userAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(project.getOwnerAccountId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The project  with id: " + '"' + project.getProjectId() + '"' + "and name: " +  '"' + project.getProjectName() + '"' + "  has been updated";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForReminder(UserAccount userAccount, Reminder reminder, Boolean isUpdate) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(reminder.getFkAccountIdCreator().getAccountId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The reminder  " + '"' + reminder.getReminderId() + '"' + ( isUpdate ? "was updated" : "  was created");
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForEpic(UserAccount userAccount, Epic epic, Boolean isUpdate) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(epic.getEntityId());
        auditToInsert.setAffectedEntityTypeId(epic.getEntityTypeId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The epic  " + '"' + epic.getEpicId() + '"' + ( isUpdate ? "was updated" : "  was created");
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForUpdateEntityPreference(UserAccount userAccount, EntityPreference entityPreference) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(entityPreference.getEntityId());
        auditToInsert.setAffectedEntityTypeId(entityPreference.getEntityTypeId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The entity preference  " + '"' + entityPreference.getEntityPreferenceId() + '"' + "  has been updated";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForSendingOrgInvite(UserAccount userAccount, Invite invite) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(invite.getEntityId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.ORG);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "An invite for org  " + '"' + invite.getEntityId() + '"' + "  was sent";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForSendingTeamInvite(UserAccount userAccount, Invite invite) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(invite.getEntityId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TEAM);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "An invite for team  " + '"' + invite.getEntityId() + '"' + "  was sent";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForAddingPerfNote(UserAccount userAccount, PerfNote perfNote) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(perfNote.getTaskId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TASK);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The performance note for task  " + '"' + perfNote.getTaskId() + '"' + "  was added";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForUpdatingPerfNote(UserAccount userAccount, PerfNote perfNote) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(perfNote.getTaskId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.TASK);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The performance note for task  " + '"' + perfNote.getTaskId() + '"' + "  has been updated";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForApplyLeave(LeaveApplicationResponse leaveApplication, Boolean isUpdate) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(leaveApplication.getApplicantDetails().getAccountId());
        auditToInsert.setAffectedEntityId(leaveApplication.getLeaveApplicationId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.LEAVE);
        auditToInsert.setUserId(userAccountRepository.findUserIdByAccountId(leaveApplication.getApplicantDetails().getAccountId()));
        String messageForUser = "The leave  " + '"' + leaveApplication.getLeaveApplicationId() + '"' + (isUpdate ? " was updated" : "  was created");
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForChangeLeaveStatus(UserAccount userAccount, Long leaveApplicationId) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(leaveApplicationId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.LEAVE);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The status of leave  " + '"' + leaveApplicationId + '"' + "  was updated";
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForStickyNote(UserAccount userAccount, StickyNote stickyNote, Boolean isUpdate) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(stickyNote.getCreatedByUserId());
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.USER);
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The sticky note  " + '"' + stickyNote.getNoteId() + '"' + (isUpdate ? "  has been updated" : " has been created");
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    public Audit auditForTemplate(UserAccount userAccount, TaskTemplate template, Boolean isUpdate) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(userAccount.getAccountId());
        auditToInsert.setAffectedEntityId(template.getEntityId());
        auditToInsert.setAffectedEntityTypeId(template.getEntityTypeId());
        auditToInsert.setUserId(userAccount.getFkUserId().getUserId());
        String messageForUser = "The template  " + '"' + template.getTemplateId() + '"' + (isUpdate ? " was updated" : "  was created");
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    /**
     * Claude change: PT-14409 - Audit method for editing consumed leave by Org Admin
     * @param adminAccount The Org Admin who edited the leave
     * @param leaveApplicationId The leave application ID being edited
     * @param reason The reason for editing
     * @return Audit record
     */
    public Audit auditForEditConsumedLeave(UserAccount adminAccount, Long leaveApplicationId, String reason) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(adminAccount.getAccountId());
        auditToInsert.setAffectedEntityId(leaveApplicationId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.LEAVE);
        auditToInsert.setUserId(adminAccount.getFkUserId().getUserId());
        String messageForUser = "Consumed leave " + '"' + leaveApplicationId + '"' + " was edited by " +
                adminAccount.getFirstName() + " " + adminAccount.getLastName() + ". Reason: " + reason;
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

    /**
     * Claude change: PT-14409 - Audit method for deleting consumed leave by Org Admin
     * @param adminAccount The Org Admin who deleted the leave
     * @param leaveApplicationId The leave application ID being deleted
     * @param reason The reason for deleting
     * @return Audit record
     */
    public Audit auditForDeleteConsumedLeave(UserAccount adminAccount, Long leaveApplicationId, String reason) {
        Audit auditToInsert = new Audit();
        auditToInsert.setAccountId(adminAccount.getAccountId());
        auditToInsert.setAffectedEntityId(leaveApplicationId);
        auditToInsert.setAffectedEntityTypeId(Constants.EntityTypes.LEAVE);
        auditToInsert.setUserId(adminAccount.getFkUserId().getUserId());
        String messageForUser = "Consumed leave " + '"' + leaveApplicationId + '"' + " was deleted by " +
                adminAccount.getFirstName() + " " + adminAccount.getLastName() + ". Reason: " + reason;
        auditToInsert.setMessageForUser(messageForUser);
        return auditRepository.save(auditToInsert);
    }

}
