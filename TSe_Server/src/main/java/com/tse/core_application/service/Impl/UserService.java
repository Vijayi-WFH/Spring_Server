package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.*;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.controller.AuthController;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.User;
import com.tse.core_application.dto.user_access_response.*;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.ILoginService;
import com.tse.core_application.service.IUserService;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements IUserService {

    private static final Logger logger = LogManager.getLogger(UserService.class.getName());
	
	@Autowired
	private UserRepository userRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private LoginService loginService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private BUService buService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;

    @Autowired
    private AuthController authController;

    @Autowired
    private OtpService otpService;

    @Autowired
    private ILoginService tokenService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;

    @Autowired
    private BURepository buRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Value("${conversation.application.root.path}")
    private String conversationBaseUrl;

    private static final Comparator<String> NULL_SAFE_STRING_COMPARATOR =
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    ObjectMapper objectMapper = new ObjectMapper();

	@Override
    public User findByUsername(String username, String password, String timeZone) {
        com.tse.core_application.model.User foundUser = userRepository.findByPrimaryEmail(username);
        if(foundUser!=null){
            com.tse.core_application.model.User user = loginService.validateAndUpdateLoginUserTimeZone(foundUser, timeZone);
            User u = new User(foundUser.getPrimaryEmail(), password, true, List.of(Roles.ROLE_USER));
            return u;
        }
        return null;

    }

    /** checks if any of the entities of the user require the sign up process to be complete with all details including gender, age range, higher education and city*/
    public boolean isSignUpComplete(com.tse.core_application.model.User user) {
        List<Long> orgIds = userAccountService.getAllOrgIdsByUserId(user.getUserId());
        List<EntityPreference> entityPreferences = entityPreferenceService.findEntityPreferenceForEntities(com.tse.core_application.model.Constants.EntityTypes.ORG, orgIds);
        boolean isSignUpCompletionMandatory = false;
        for (EntityPreference entityPreference : entityPreferences) {
            if (!entityPreference.getRequireMinimumSignUpDetails()) {
                isSignUpCompletionMandatory = true;
                break;
            }
        }

        return !isSignUpCompletionMandatory || (user.getAgeRange() != null && user.getGender() != null && user.getHighestEducation() != null && user.getCity() != null);
    }

    public com.tse.core_application.model.User getUserByUserName(String username){
	    return userRepository.findByPrimaryEmail(username);
    }

    /** this method sets chatUserName and chatUserPassword and saves the user in repo */
	@Override
    public com.tse.core_application.model.User addUser(com.tse.core_application.model.User user, String timeZone){
        user.setTimeZone(timeZone);

        String chatUserString = user.getPrimaryEmail().replace("@", "_");
        user.setChatUserName(chatUserString);
        String guid = UUID.randomUUID().toString();
        user.setChatPassword(guid);

        return this.userRepository.save(user);
    }

    @Override
    public com.tse.core_application.model.User updateUser(com.tse.core_application.model.User user, RegistrationRequest req){

        Optional.ofNullable(req.getAlternateEmail()).ifPresent(user::setAlternateEmail);
        Optional.ofNullable(req.getIsAlternateEmailPersonal()).ifPresent(user::setIsAlternateEmailPersonal);
        Optional.ofNullable(getPersonalEmail(req)).ifPresent(user::setPersonalEmail);
        Optional.ofNullable(req.getGivenName()).ifPresent(user::setGivenName);
//        Optional.ofNullable(req.getLocale()).ifPresent(user::setLocale);
//        Optional.ofNullable(req.getCity()).ifPresent(user::setCity);
//        Optional.ofNullable(req.getHighestEducation()).ifPresent(user::setHighestEducation);
//        Optional.ofNullable(req.getSecondHighestEducation()).ifPresent(user::setSecondHighestEducation);
//        Optional.ofNullable(req.getGender()).ifPresent(user::setGender);
//        Optional.ofNullable(req.getAgeRange()).ifPresent(user::setAgeRange);
        Optional.ofNullable(req.getCountry()).ifPresent(user::setFkCountryId);

        return userRepository.save(user);
    }

    private String getPersonalEmail(RegistrationRequest req) {
        String personalEmail = null;
        if (req.getIsPrimaryEmailPersonal() != null && req.getIsPrimaryEmailPersonal()) {
            personalEmail = req.getPrimaryEmail();
        } else if (req.getIsAlternateEmailPersonal() != null && req.getIsAlternateEmailPersonal()) {
            personalEmail = req.getAlternateEmail();
        }
        return personalEmail;
    }

	@Override
    public com.tse.core_application.model.User getUser(String primaryEmail){
    	return userRepository.findByPrimaryEmail(primaryEmail);
    }

    @Override
    public List<UserRole> getRolesForUser(User user){
        List<UserRole> userRoles = new ArrayList<>();
        if(user!=null){
            com.tse.core_application.model.User dbUser = userRepository.findByPrimaryEmail(user.getUsername());
            List<Long> accountIdsForUser = userAccountService.getActiveAccountIdsForUserId(dbUser.getUserId(),null);
            if(accountIdsForUser!=null && accountIdsForUser.size()>0){
              userRoles = userRoleRepository.findByAccountIdIn(accountIdsForUser);
            }
        }
       return userRoles;
    }

    public UserIdFirstLastName getUserIdFirstLastNameByPrimaryEmail(String primaryEmail) {
        com.tse.core_application.model.User foundUserDb = this.getUserByUserName(primaryEmail);
        UserIdFirstLastName userIdFirstLastName = null;
        if(foundUserDb != null) {
            userIdFirstLastName = new UserIdFirstLastName();
            userIdFirstLastName.setUserId(foundUserDb.getUserId());
            userIdFirstLastName.setFirstName(foundUserDb.getFirstName());
            userIdFirstLastName.setLastName(foundUserDb.getLastName());
        }
        return userIdFirstLastName;
    }


    public List<Long> getAccountIdsForUser(User user){
        List<UserRole> userRoles = new ArrayList<>();
        if(user!=null){
            com.tse.core_application.model.User dbUser = userRepository.findByPrimaryEmail(user.getUsername());
            List<Long> accountIdsForUser = userAccountService.getActiveAccountIdsForUserId(dbUser.getUserId(),null);
            return accountIdsForUser;
        }
      return null;
    }

    public UserDetailsResponse getUserDetailsByUserName(String userName) {
        com.tse.core_application.model.User user = this.getUserByUserName(userName);
        UserDetailsResponse userDetailsResponse = new UserDetailsResponse();
        if (user == null) {
            return null;
        }
        List<UserAccount> userAccountsDb = userAccountService.getAllUserAccountByUserIdAndIsActive(user.getUserId());
        List<Object> userAccounts = new ArrayList<>();
        for(UserAccount userAccount: userAccountsDb) {
            HashMap<String, Object> userAccountMap = objectMapper.convertValue(userAccount, HashMap.class);
            userAccountMap.remove("fkUserId");
            userAccounts.add(userAccountMap);
        }
        List<Long> accountIdsForUser = userService.getAllAccountIds(user);
        List<CustomAccessDomain> accessDomainList = accessDomainService.getAllActiveAccessDomainsByAllAccountIds(accountIdsForUser);

        HashMap<String, Object> userMap = objectMapper.convertValue(user, HashMap.class);
        userMap.put("userAccount", userAccounts);
        userMap.put("accessDomains", accessDomainList);
        userDetailsResponse.setUser(userMap);
        return userDetailsResponse;
    }

    public ResponseEntity<Object> getUserDetailsFormattedResponse(UserDetailsResponse userDetailsResponse, String userName) {
        if (userDetailsResponse != null) {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, userDetailsResponse);
        }
        String allStackTraces = StackTraceHandler.getAllStackTraces(new UserDoesNotExistException());
        logger.error("User does not exist by username = " + userName, new Throwable(allStackTraces));
        ThreadContext.clearMap();
        throw new UserDoesNotExistException();
    }

    /* This method removes all the default entities from the organization structure of the user. This method is no longer in
    * use after discussion with android developer. In order to reuse this method, 3 methods have to be re-created which are already
    * called in this method. */
/*
    public List<OrgBUProjectTeamStructure> getAllOrgStructure(Long userId) {
        List<UserAccount> userAccounts = userAccountService.getAllUserAccountByUserId(userId);
        List<OrgBUProjectTeamStructure> finalOrgStr = new ArrayList<>();

        // all org loop
        for(UserAccount userAccount: userAccounts) {
            LinkedHashMap<String, Object> orgMap = new LinkedHashMap<>();

            // finding org
            OrgIdOrgName orgIdOrgName = organizationService.getOrganizationByOrgId(userAccount.getOrgId());
            if(orgIdOrgName != null) {
                orgMap.put("orgId", orgIdOrgName.getOrgId());
                orgMap.put("orgName", orgIdOrgName.getOrganizationName());
            }

            // finding all Bu for this org
            List<BU> allBuFoundDb = buService.getBUByOrgId(userAccount.getOrgId());
            if(!allBuFoundDb.isEmpty()) {

                // Bu loop starts
                List<LinkedHashMap<String, Object>> finalBUStr = new ArrayList<>();

                List<ProjectTeam> projectTeams = new ArrayList<>();
                List<TeamIdAndTeamName> allTeamsWithoutProject = new ArrayList<>();

                List<ProjectTeam> projectTeamWithBU = null;
                List<TeamIdAndTeamName> allTeamsWithoutProjectWithBU = null;

                for(BU bu: allBuFoundDb) {
                    if(bu.getBuName().contains(Constants.BU_NAME)) {

                        // finding all projects for this default Bu
                        List<Project> allProjectsFoundDb = projectService.getAllProjectsByBuId(bu.getBuId(), userAccount.getOrgId());

                        // project loop starts
                        for(Project project: allProjectsFoundDb) {
                             if(project.getProjectName().contains(Constants.PROJECT_NAME)) {
                                List<TeamIdAndTeamName> allTeamsFoundDb = teamService.getAllTeamsByOrgIdAndProjectId(userAccount.getOrgId(), project.getProjectId());
                                allTeamsWithoutProject.addAll(allTeamsFoundDb);
                             } else {
                                 List<TeamIdAndTeamName> allTeamsFoundDb = teamService.getAllTeamsByOrgIdAndProjectId(userAccount.getOrgId(), project.getProjectId());
                                 ProjectTeam projectTeam = new ProjectTeam();
                                 projectTeam.setProjectId(project.getProjectId());
                                 projectTeam.setProjectName(project.getProjectName());
                                 projectTeam.setTeams(allTeamsFoundDb);
                                 projectTeams.add(projectTeam);
                             }
                        } // project loop ends

                    } else {
                        LinkedHashMap<String, Object> finalBUMap = new LinkedHashMap<>();
                        finalBUMap.put("buId", bu.getBuId());
                        finalBUMap.put("buName", bu.getBuName());

                        List<Project> projects = projectService.getAllProjectsByBuId(bu.getBuId(), userAccount.getOrgId());

                        if (!projects.isEmpty()) {

                            projectTeamWithBU = new ArrayList<>();
                            allTeamsWithoutProjectWithBU = new ArrayList<>();

                            for (Project project: projects) {
                                if(project.getProjectName().contains(Constants.PROJECT_NAME)) {
                                    List<TeamIdAndTeamName> allTeamsFoundDb = teamService.getAllTeamsByOrgIdAndProjectId(userAccount.getOrgId(), project.getProjectId());
                                    allTeamsWithoutProjectWithBU.addAll(allTeamsFoundDb);
                                } else {
                                    ProjectTeam projectTeam = new ProjectTeam();
                                    projectTeam.setProjectId(project.getProjectId());
                                    projectTeam.setProjectName(project.getProjectName());
                                    List<TeamIdAndTeamName> allTeamsFoundDb = teamService.getAllTeamsByOrgIdAndProjectId(userAccount.getOrgId(), project.getProjectId());
                                    projectTeam.setTeams(allTeamsFoundDb);
                                    projectTeamWithBU.add(projectTeam);
                                }
                            }

                            if(!allTeamsWithoutProjectWithBU.isEmpty()) {
                                finalBUMap.put("teams", allTeamsWithoutProjectWithBU);
                            }
                            if(!projectTeamWithBU.isEmpty()) {
                                finalBUMap.put("projects", projectTeamWithBU);
                            }

                        }
                        finalBUStr.add(finalBUMap);
                    }

                } // Bu loop ends

                if(!allTeamsWithoutProject.isEmpty()) {
                    orgMap.put("teams", allTeamsWithoutProject);
                }
                if(!projectTeams.isEmpty()) {
                    orgMap.put("project", projectTeams);
                }
                if(!finalBUStr.isEmpty()) {
                    orgMap.put("bu", finalBUStr);
                }

            }

            OrgBUProjectTeamStructure orgBUProjectTeamStructure = new OrgBUProjectTeamStructure();
            orgBUProjectTeamStructure.setOrganization(orgMap);
            finalOrgStr.add(orgBUProjectTeamStructure);

        }
        return finalOrgStr;
    }
*/


    /* This method is the alternative version of the above method for the organization structure of the user. This method gives
    * all the entities of the user. i.e. it does not remove the default entities from the organization structure. */
    @Deprecated(since = "2022-09-12")
    public Organizations getAllOrgStructure(String userName) {
        com.tse.core_application.model.User foundUserDb = getUserByUserName(userName);
        List<UserAccount> userAccounts = userAccountService.getAllUserAccountByUserIdAndIsActive(foundUserDb.getUserId());
        List<OrgBUProjectTeamStructure> allOrgStrWithBuProjectTeams = new ArrayList<>();

        for(UserAccount userAccount: userAccounts) {
            OrgBUProjectTeamStructure orgBUProjectTeamStructure = new OrgBUProjectTeamStructure();
            OrgIdOrgName orgIdOrgName = organizationService.getOrganizationByOrgId(userAccount.getOrgId());
            orgBUProjectTeamStructure.setOrgId(orgIdOrgName.getOrgId());
            orgBUProjectTeamStructure.setOrgName(orgIdOrgName.getOrganizationName());

            List<BU> allFoundBUDb = buService.getBUByOrgId(userAccount.getOrgId());
            List<BUProject> allBuWithProjects = new ArrayList<>();
            for(BU bu: allFoundBUDb) {
                BUProject buProject = new BUProject();
                buProject.setBuId(bu.getBuId());
                buProject.setBuName(bu.getBuName());

                List<Project> allProjectsFoundDb = projectService.getAllProjectsByBuIdAndOrgId(bu.getBuId(), userAccount.getOrgId());
                List<ProjectTeam> allProjectsWithTeams = new ArrayList<>();
                for(Project project: allProjectsFoundDb) {
                    ProjectTeam projectTeam = new ProjectTeam();
                    projectTeam.setProjectId(project.getProjectId());
                    projectTeam.setProjectName(project.getProjectName());

                    List<TeamIdAndTeamName> teamIdAndTeamNamesFoundDb = teamService.getAllTeamsByOrgIdAndProjectId(userAccount.getOrgId(), project.getProjectId());
                    projectTeam.setTeams(teamIdAndTeamNamesFoundDb);
                    allProjectsWithTeams.add(projectTeam);
                }
                buProject.setProjects(allProjectsWithTeams);
                allBuWithProjects.add(buProject);
            }
            orgBUProjectTeamStructure.setBu(allBuWithProjects);
            allOrgStrWithBuProjectTeams.add(orgBUProjectTeamStructure);
        }

        Organizations organizations = new Organizations();
        organizations.setOrganizations(allOrgStrWithBuProjectTeams);
        return organizations;
    }

    public Organizations getAllOrgStructures(String userName) {
        Organizations organizations = new Organizations();
        List<Organization> userAllOrganizations = new ArrayList<>();
        List<BU> userAllBU = new ArrayList<>();
        List<Project> userAllProject = new ArrayList<>();
        List<Team> allMyTeamsFound = new ArrayList<>();

        if(userName != null && !userName.isEmpty() && !userName.isBlank()) {
            com.tse.core_application.model.User foundUserDb = getUserByUserName(userName);
            if(foundUserDb != null) {
                allMyTeamsFound = teamService.getAllMyTeamsByUserId(foundUserDb.getUserId());
                if(!allMyTeamsFound.isEmpty()) {
                    List<Integer> allProjectIds = new ArrayList<>();
                    List<Long> allOrgIds = new ArrayList<>();
                    List<Long> allBUIds = new ArrayList<>();
                    for(Team team: allMyTeamsFound) {
                        if(team.getFkProjectId() != null && !allProjectIds.contains(Math.toIntExact(team.getFkProjectId().getProjectId()))) {
                            allProjectIds.add(Math.toIntExact(team.getFkProjectId().getProjectId()));
                        }
                        if(team.getFkProjectId() != null && team.getFkProjectId() != null) {
                            Project foundProjectDb = projectService.getProjectByProjectIdAndOrgId(team.getFkProjectId().getProjectId(), team.getFkOrgId().getOrgId());
                            if(foundProjectDb != null) {
                                if (!allBUIds.contains(foundProjectDb.getBuId())) {
                                    allBUIds.add(foundProjectDb.getBuId());
                                }
                            }
                        }
                        if(team.getFkOrgId() != null && !allOrgIds.contains(team.getFkOrgId().getOrgId())) {
                            allOrgIds.add(team.getFkOrgId().getOrgId());
                        }
                    }
                    if(!allProjectIds.isEmpty()) {
                        userAllProject = projectService.getAllProjectsByProjectsIds(allProjectIds);
                    }
                    if(!allOrgIds.isEmpty()) {
                        userAllOrganizations = organizationService.getAllOrganizationByOrgIds(allOrgIds);
                    }
                    if(!allBUIds.isEmpty()) {
                        userAllBU = buService.getAllBUsByBUIds(allBUIds);
                    }
                }

                List<OrgBUProjectTeamStructure> orgBUProjectTeamStructures = new ArrayList<>();
                for(Organization organization: userAllOrganizations) {
                    OrgBUProjectTeamStructure orgBUProjectTeamStructure = new OrgBUProjectTeamStructure();
                    orgBUProjectTeamStructure.setOrgId(organization.getOrgId());
                    orgBUProjectTeamStructure.setOrgName(organization.getOrganizationName());

                    List<BUProject> buProjects = new ArrayList<>();
                    List<BU> userAllBUs = new ArrayList<>();
//                    userAllBU.retainAll(Collections.singleton(organization.getOrgId()));
                    for(BU bu: userAllBU) {
                        if(Objects.equals(bu.getOrgId(), organization.getOrgId())) {
                            userAllBUs.add(bu);
                        }
                    }

                    for(BU bu: userAllBUs) {
                        BUProject buProject = new BUProject();
                        buProject.setBuId(bu.getBuId());
                        buProject.setBuName(bu.getBuName());

                        List<ProjectTeam> projectTeams = new ArrayList<>();
                        List<Project> allProjectsForBUAndOrg = new ArrayList<>();
                        for(Project project: userAllProject) {
                            if(project.getBuId() != null && project.getOrgId() != null) {
                                if(Objects.equals(project.getBuId(), bu.getBuId()) && Objects.equals(project.getOrgId(), organization.getOrgId())) {
                                    allProjectsForBUAndOrg.add(project);
                                }
                            }
                        }

                        for(Project project: allProjectsForBUAndOrg) {
                            ProjectTeam projectTeam = new ProjectTeam();
                            projectTeam.setProjectId(project.getProjectId());
                            projectTeam.setProjectName(project.getProjectName());

                            List<TeamIdAndTeamName> teamIdAndTeamNames = new ArrayList<>();
                            for(Team team: allMyTeamsFound) {
                                if(team.getFkProjectId() != null && team.getFkOrgId() != null) {
                                    if(Objects.equals(team.getFkProjectId().getProjectId(), project.getProjectId()) && Objects.equals(team.getFkOrgId().getOrgId(), organization.getOrgId())) {
                                        TeamIdAndTeamName teamIdAndTeamName = new TeamIdAndTeamName(team.getTeamId(), team.getTeamName(), team.getTeamCode(), team.getIsDeleted());
                                        if (organization.getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                                            if (Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
                                                if (teamService.getAllTeamsForCreateTask(foundUserDb.getPrimaryEmail(), organization.getOrgId()).size() > 1) {
                                                    teamIdAndTeamName.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                                                } else {
                                                    teamIdAndTeamName.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                                                }
                                            }
                                        }
                                        teamIdAndTeamNames.add(teamIdAndTeamName);
                                    }
                                }
                            }
                            projectTeam.setTeams(teamIdAndTeamNames);
                            projectTeams.add(projectTeam);
                        }
                        buProject.setProjects(projectTeams);
                        buProjects.add(buProject);
                    }
                    orgBUProjectTeamStructure.setBu(buProjects);
                    orgBUProjectTeamStructures.add(orgBUProjectTeamStructure);
                }
                organizations.setOrganizations(orgBUProjectTeamStructures);
            }
        }
        return organizations;
    }

    public boolean validateGetOrgTeamDropdownStructureInputs(String userName, String token) {
        boolean isUserNameValidated = false;
        String tokenUserName = jwtUtil.getUsernameFromToken(token);
        if(Objects.equals(tokenUserName, userName)) {
            isUserNameValidated = true;
        }
        return isUserNameValidated;
    }

    public UserName getFullNameByUserId(Long userId) {
        return userRepository.findFirstNameAndLastNameByUserId(userId);
    }

    /**
     * method to get the User Profile Details from the User table
     * @param userId
     * @return
     */
    public UserProfileDTO getUserProfileDetails(Long userId) {
        Optional<com.tse.core_application.model.User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            com.tse.core_application.model.User user = userOptional.get();
            UserProfileDTO userProfileDTO = new UserProfileDTO();
            BeanUtils.copyProperties(user, userProfileDTO);
            userProfileDTO.setCountryId(user.getFkCountryId().getCountryId());
            userProfileDTO.setCountryName(user.getFkCountryId().getCountryName());
            return userProfileDTO;
        } else {
            return null;
        }
    }

    /**
     * This method returns the list of non editable fields in userProfile dto
     * @param userProfileDTO
     * @return
     */
    public ArrayList<String> getNonEditableFields(UserProfileDTO userProfileDTO) {
        ArrayList<String> nonEditableFields = new ArrayList<>();
        if (userProfileDTO.getPrimaryEmail() != null) {
            nonEditableFields.add("primaryEmail");
        }
        if (userProfileDTO.getIsPrimaryEmailPersonal() != null) {
            nonEditableFields.add("isPrimaryEmailPersonal");
        }
//        if (userProfileDTO.getAlternateEmail() != null ) {
//            nonEditableFields.add("alternateEmail");
//        }
//        if (userProfileDTO.getIsAlternateEmailPersonal() != null) {
//            nonEditableFields.add("isAlternateEmailPersonal");
//        }
//        if (userProfileDTO.getPersonalEmail() != null) {
//            nonEditableFields.add("personalEmail");
//        }
        if (userProfileDTO.getCurrentOrgEmail() != null) {
            nonEditableFields.add("currentOrgEmail");
        }
        if (userProfileDTO.getFirstName() != null) {
            nonEditableFields.add("firstName");
        }
        if (userProfileDTO.getMiddleName() != null) {
            nonEditableFields.add("middleName");
        }
        if (userProfileDTO.getLastName() != null) {
            nonEditableFields.add("lastName");
        }

        return nonEditableFields;
    }

    /**
     * This method edits user profile
     * @param userProfileDTO
     * @return
     */
    public Boolean editUserProfile(UserProfileDTO userProfileDTO, String timeZone) {
        Optional<com.tse.core_application.model.User> userOptional = userRepository.findById(userProfileDTO.getUserId());

        if (userOptional.isPresent()) {
            com.tse.core_application.model.User user = userOptional.get();

            // non-editable fields lists -- this returns list of any fields that are not editable but contained in the userProfileDTO request
            ArrayList<String> nonEditableFields = getNonEditableFields(userProfileDTO);

            // Check for non-editable fields
            for (String nonEditableField : nonEditableFields) {
                if (com.tse.core_application.model.Constants.nonEditableFieldsInUserProfile.contains(nonEditableField)) {
                    throw new ValidationFailedException(nonEditableField + " is not editable");
                }
            }

            // Copy editable fields
            CommonUtils.copyNonNullProperties(userProfileDTO, user);

            // Editable fields requiring additional processing
            if (userProfileDTO.getCountryId() != null) {
                Optional<Country> countryOptional = countryRepository.findById(userProfileDTO.getCountryId());

                if (countryOptional.isPresent()) {
                    Country country = countryOptional.get();
                    boolean isCountryValidAsPerTimeZone = TimeZoneCountryMapping.isValidTimeZoneForCountry(timeZone, country.getIsoCountryCode());
                    if (isCountryValidAsPerTimeZone) {
                        user.setFkCountryId(country);
                    } else {
                        throw new ValidationFailedException("The provided timezone and country information do not match. " +
                                "Please ensure that the provided country is within your current timezone.\"");
                    }
                }
            }

            userRepository.save(user);
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method retrieves user access structures for all organizations, business units (BUs),
     * projects, and teams associated with the given user. It constructs a hierarchical structure
     * representing the user's access at different levels within the organizational hierarchy.
     */
    public UserAccessResponse getUserAllOrgScreenAccess(String userName, String screenName) {
        UserAccessResponse userAccessResponse = new UserAccessResponse();
        List<Organization> userAllOrganizations = new ArrayList<>();
        List<BU> userAllBU = new ArrayList<>();
        List<Project> userAllProject = new ArrayList<>();
        List<Team> allMyTeamsFound = new ArrayList<>();

        if (userName != null && !userName.isEmpty() && !userName.isBlank()) {
            com.tse.core_application.model.User foundUserDb = getUserByUserName(userName);
            if (foundUserDb != null) {
                List<Long> accountIdList = getAllAccountIds(foundUserDb);
                Map<Integer, List<Integer>> entityIdsWithHigherRoles = getEntityIdsWithScreenRoles(accountIdList, screenName.toLowerCase());
                //getting all the org ids for user
                List<Long> allOrgIds = userAccountRepository.findOrgIdByFkUserIdUserIdAndIsActive(foundUserDb.getUserId(), true).stream().map(OrgId::getOrgId).collect(Collectors.toList());
                if (!allOrgIds.isEmpty()) {
                    List<Integer> allProjectIds = new ArrayList<>();
                    List<Long> allBUIds = new ArrayList<>();
                    if ((screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.JIRAMIGRATION.getType())) || (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.FEATUREACCESS.getType()))) {
                        List<Long> accountIds = userService.getAllAccountIds(foundUserDb);
                        List<Long> orgIdList = userAccountRepository.findByAccountIdIn(accountIds)
                                .stream()
                                .map(UserAccount::getOrgId)
                                .collect(Collectors.toList());
                        if (orgIdList != null && !orgIdList.isEmpty()) {
                            allMyTeamsFound = teamRepository.findByFkOrgIdOrgIdIn(orgIdList);
                        }
                    } else {
                        allMyTeamsFound = teamService.getAllMyTeamsByUserId(foundUserDb.getUserId());
                        if (com.tse.core_application.model.Constants.ScreenRoleEnum.TEAMTIMESHEET.getType().equalsIgnoreCase(screenName) ||com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType().equalsIgnoreCase(screenName) ) {
                            List<Integer> teamIdInts = (entityIdsWithHigherRoles == null) ? null : entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.TEAM);

                            if (teamIdInts != null && !teamIdInts.isEmpty()) {
                                List<Long> teamIds = teamIdInts.stream()
                                        .map(Integer::longValue)
                                        .collect(java.util.stream.Collectors.toList());

                                List<Team> hrTeamList = teamRepository.findByTeamIdIn(teamIds);
                                if (hrTeamList != null && !hrTeamList.isEmpty()) {
                                    allMyTeamsFound.addAll(hrTeamList);
                                    allMyTeamsFound = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(allMyTeamsFound));
                                }
                            }
                        }
                    }
                    if(!allMyTeamsFound.isEmpty()) {
                        for(Team team: allMyTeamsFound) {
                            if(team.getFkProjectId() != null && !allProjectIds.contains(Math.toIntExact(team.getFkProjectId().getProjectId()))) {
                                allProjectIds.add(Math.toIntExact(team.getFkProjectId().getProjectId()));
                            }
                            if(team.getFkProjectId() != null && team.getFkProjectId() != null) {
                                Project foundProjectDb = projectService.getProjectByProjectIdAndOrgId(team.getFkProjectId().getProjectId(), team.getFkOrgId().getOrgId());
                                if(foundProjectDb != null) {
                                    if (!allBUIds.contains(foundProjectDb.getBuId())) {
                                        allBUIds.add(foundProjectDb.getBuId());
                                    }
                                }
                            }
                            if(team.getFkOrgId() != null && !allOrgIds.contains(team.getFkOrgId().getOrgId())) {
                                allOrgIds.add(team.getFkOrgId().getOrgId());
                            }
                        }
                    }
                    if(!allProjectIds.isEmpty()) {
                        userAllProject = projectService.getAllProjectsByProjectsIds(allProjectIds);
                    }
                    if(!allOrgIds.isEmpty()) {
                        userAllOrganizations = organizationService.getAllOrganizationByOrgIds(allOrgIds);
                    }
                    if(!allBUIds.isEmpty()) {
                        userAllBU = buService.getAllBUsByBUIds(allBUIds);
                    }
                }

                List<UserOrgAccessStructureResponse> userOrgAccessStructureResponses = new ArrayList<>();
                for (Organization organization : userAllOrganizations) {
                    if ((screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MANAGESPRINTS.getType())
                            || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MANAGEORGANIZATION.getType())
                            || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType())
                            || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.LEAVES.getType())
                            || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ALLBOARDVIEW.getType()))
                            && Objects.equals(organization.getOrgId(), com.tse.core_application.model.Constants.OrgIds.PERSONAL.longValue())) {
                        continue;
                    }
                    UserOrgAccessStructureResponse userOrgAccessStructureResponse = new UserOrgAccessStructureResponse();
                    userOrgAccessStructureResponse.setOrgId(organization.getOrgId());
                    userOrgAccessStructureResponse.setOrgName(organization.getOrganizationName());
                    if (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.JIRAMIGRATION.getType()) && (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MEETINGS.getType()) || (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MEETINGS.getType()) && !Objects.equals(organization.getOrgId(), 0L))) && (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.ORG).contains(organization.getOrgId().intValue()))) {
                        userOrgAccessStructureResponse.setIsSelectable(Boolean.TRUE);
                    }
                    if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.FEATUREACCESS.getType()) &&
                            (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.ORG).contains(organization.getOrgId().intValue()))) {
                        userOrgAccessStructureResponse.setIsSelectable(Boolean.TRUE);
                    }
                    if (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MANAGEORGANIZATION.getType())) {

                        List<UserBuAccessDetail> userBuAccessDetails = new ArrayList<>();
                        List<BU> userAllBUs = new ArrayList<>();
//                    userAllBU.retainAll(Collections.singleton(organization.getOrgId()));
                        for (BU bu : userAllBU) {
                            if (Objects.equals(bu.getOrgId(), organization.getOrgId())) {
                                userAllBUs.add(bu);
                            }
                        }

                        for (BU bu : userAllBUs) {
                            UserBuAccessDetail userBuAccessDetail = new UserBuAccessDetail();
                            userBuAccessDetail.setBuId(bu.getBuId());
                            userBuAccessDetail.setBuName(bu.getBuName());
                            if (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.JIRAMIGRATION.getType()) &&
                                    (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.BU).contains(bu.getBuId().intValue()) || (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType()) && userOrgAccessStructureResponse.getIsSelectable()))) {
                                userBuAccessDetail.setIsSelectable(Boolean.TRUE);
                            }
                            if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.FEATUREACCESS.getType()) &&
                                    (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.ORG).contains(bu.getOrgId().intValue()))) {
                                userBuAccessDetail.setIsSelectable(Boolean.TRUE);
                            }

                            if (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MANAGEPROJECTS.getType())) {
                                List<UserProjectAccessDetail> userProjectAccessDetails = new ArrayList<>();
                                List<Project> allProjectsForBUAndOrg = new ArrayList<>();
                                for (Project project : userAllProject) {
                                    if (project.getBuId() != null && project.getOrgId() != null) {
                                        if (Objects.equals(project.getBuId(), bu.getBuId()) && Objects.equals(project.getOrgId(), organization.getOrgId())) {
                                            allProjectsForBUAndOrg.add(project);
                                        }
                                    }
                                }

                                for (Project project : allProjectsForBUAndOrg) {
                                    if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.MANAGEPROJECTS.getType()) && Objects.equals(project.getProjectId(), com.tse.core_application.model.Constants.PERSONAL_TEAM_ID)) {
                                        continue;
                                    }
                                    UserProjectAccessDetail userProjectAccessDetail = new UserProjectAccessDetail();
                                    userProjectAccessDetail.setProjectId(project.getProjectId());
                                    userProjectAccessDetail.setProjectName(project.getProjectName());
                                    if (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.JIRAMIGRATION.getType()) &&
                                            (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.PROJECT).contains(project.getProjectId().intValue()) || (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType()) && userOrgAccessStructureResponse.getIsSelectable()))) {
                                        userProjectAccessDetail.setIsSelectable(Boolean.TRUE);
                                    }
                                    if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.FEATUREACCESS.getType()) &&
                                            (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.ORG).contains(project.getOrgId().intValue()))) {
                                        userProjectAccessDetail.setIsSelectable(Boolean.TRUE);
                                    }

                                    if (!screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.CREATETEAM.getType())) {
                                        List<UserTeamAccessDetail> userTeamAccessDetails = new ArrayList<>();
                                        for (Team team : allMyTeamsFound) {
                                            if ((screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.TEAMTIMESHEET.getType()) || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.EDITTEAM.getType())) && Objects.equals(team.getTeamId(), com.tse.core_application.model.Constants.PERSONAL_TEAM_ID)) {
                                                continue;
                                            }
                                            if (team.getFkProjectId() != null && team.getFkOrgId() != null) {
                                                if (Objects.equals(team.getFkProjectId().getProjectId(), project.getProjectId()) && Objects.equals(team.getFkOrgId().getOrgId(), organization.getOrgId())) {
                                                    UserTeamAccessDetail userTeamAccessDetail = new UserTeamAccessDetail();
                                                    userTeamAccessDetail.setTeamId(team.getTeamId());
                                                    userTeamAccessDetail.setTeamName(team.getTeamName());
                                                    userTeamAccessDetail.setTeamCode(team.getTeamCode());
                                                    if (organization.getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                                                        if (teamService.getAllTeamsForCreateTask(foundUserDb.getPrimaryEmail(), organization.getOrgId()).size() > 1) {
                                                            if (Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
                                                                userTeamAccessDetail.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                                                            }
                                                        } else {
                                                            userTeamAccessDetail.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                                                        }
                                                    }
                                                    if (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.TEAM).contains(team.getTeamId().intValue()) || (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType()) && userOrgAccessStructureResponse.getIsSelectable())) {
                                                        userTeamAccessDetail.setIsSelectable(Boolean.TRUE);
                                                        if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.TEMPLATES.getType())) {
                                                            userOrgAccessStructureResponse.setIsSelectable(Boolean.TRUE);
                                                        }
                                                        if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.CREATEEPIC.getType()) || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.VIEWEPIC.getType())) {
                                                            userProjectAccessDetail.setIsSelectable(Boolean.TRUE);
                                                        }
                                                    }
                                                    // In jira if user is org/project admin then s/he can import task in any team of that org and project
                                                    if ((screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.JIRAMIGRATION.getType()) || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.FEATUREACCESS.getType())) &&
                                                            (entityIdsWithHigherRoles == null || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.ORG).contains(team.getFkOrgId().getOrgId().intValue()) || entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.PROJECT).contains(team.getFkProjectId().getProjectId().intValue()))) {
                                                        userTeamAccessDetail.setIsSelectable(Boolean.TRUE);
                                                    }
                                                    // in case of view task we won't send the teams where the is selectable is false
                                                    if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.VIEW_TASK.getType())) {
                                                        if (entityIdsWithHigherRoles != null && entityIdsWithHigherRoles.get(com.tse.core_application.model.Constants.EntityTypes.TEAM).contains(team.getTeamId().intValue())) {
                                                            userTeamAccessDetails.add(userTeamAccessDetail);
                                                        }
                                                    } else {
                                                        userTeamAccessDetails.add(userTeamAccessDetail);
                                                    }
                                                }
                                            }
                                        }
                                        userProjectAccessDetail.setTeams(userTeamAccessDetails);
                                    }
                                    userProjectAccessDetails.add(userProjectAccessDetail);
                                }
                                userBuAccessDetail.setProjects(userProjectAccessDetails);
                            }
                            userBuAccessDetails.add(userBuAccessDetail);
                        }
                        userOrgAccessStructureResponse.setBu(userBuAccessDetails);
                    }
                    userOrgAccessStructureResponses.add(userOrgAccessStructureResponse);
                }
                userAccessResponse.setOrganizations(userOrgAccessStructureResponses);
            }
        }
        // Sort the org then project then bu and then team in ascending order
        sortUserAccessResponse (userAccessResponse);

        return userAccessResponse;
    }

    private void sortUserAccessResponse(UserAccessResponse response) {
        if (response == null || response.getOrganizations() == null) return;

        response.getOrganizations().sort(Comparator.comparing(UserOrgAccessStructureResponse::getOrgName, NULL_SAFE_STRING_COMPARATOR));
        response.getOrganizations().forEach(this::sortOrgLevel);
    }

    private void sortOrgLevel(UserOrgAccessStructureResponse org) {
        if (org.getBu() == null) return;
        org.getBu().sort(Comparator.comparing(UserBuAccessDetail::getBuName, NULL_SAFE_STRING_COMPARATOR));
        org.getBu().forEach(this::sortBuLevel);
    }

    private void sortBuLevel(UserBuAccessDetail bu) {
        if (bu.getProjects() == null) return;
        bu.getProjects().sort(Comparator.comparing(UserProjectAccessDetail::getProjectName, NULL_SAFE_STRING_COMPARATOR));
        bu.getProjects().forEach(this::sortProjectLevel);
    }

    private void sortProjectLevel(UserProjectAccessDetail project) {
        if (project.getTeams() == null) return;
        project.getTeams().sort(Comparator.comparing(UserTeamAccessDetail::getTeamName, NULL_SAFE_STRING_COMPARATOR));
    }

    /**
     * This method returns map of entities, where user have higher roles
     */
    public Map<Integer, List<Integer>> getEntityIdsWithScreenRoles(List<Long> accountIdList, String screenName) {
        Map<Integer, List<Integer>> entityList = new HashMap<>();
        com.tse.core_application.model.Constants.ScreenRoleEnum roleEnum = com.tse.core_application.model.Constants.ScreenRoleEnum.getByType(screenName);
        if (roleEnum == null) {
            throw new IllegalStateException("Unable to determine user role: Screen state unknown.");
        }
        List<Integer> roleList = roleEnum.getList();
        if (roleList == null) {
            return null;
        }
        List<Integer> orgList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(com.tse.core_application.model.Constants.EntityTypes.ORG, roleList, accountIdList);
        List<Integer> buList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(com.tse.core_application.model.Constants.EntityTypes.BU, roleList, accountIdList);
        List<Integer> projectList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(com.tse.core_application.model.Constants.EntityTypes.PROJECT, roleList, accountIdList);
        List<Integer> teamList = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndRoleIdInAndAccountIdIn(com.tse.core_application.model.Constants.EntityTypes.TEAM, roleList, accountIdList);
        setEntityIdOfHrOfEntityTypeAccess (orgList, buList, projectList, teamList, accountIdList, screenName);
        entityList.put(com.tse.core_application.model.Constants.EntityTypes.ORG, orgList);
        entityList.put(com.tse.core_application.model.Constants.EntityTypes.BU, buList);
        entityList.put(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectList);
        entityList.put(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamList);

        return entityList;
    }

    private void setEntityIdOfHrOfEntityTypeAccess(List<Integer> orgList, List<Integer> buList, List<Integer> projectList, List<Integer> teamList, List<Long> accountIdList, String screenName) {
        if (screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.TEAMTIMESHEET.getType()) || screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType())){
            if (orgList == null) {
                orgList = new ArrayList<>();
            }
            if (buList == null) {
                buList = new ArrayList<>();
            }
            if (projectList == null) {
                projectList = new ArrayList<>();
            }
            if (teamList == null) {
                teamList = new ArrayList<>();
            }
            Integer actionId=0;
            if(screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.TEAMTIMESHEET.getType())) {
                actionId = com.tse.core_application.model.Constants.ActionId.VIEW_TIMESHEET;
            }
            if(screenName.equalsIgnoreCase(com.tse.core_application.model.Constants.ScreenRoleEnum.ATTENDANCE.getType())) {
                actionId = com.tse.core_application.model.Constants.ActionId.VIEW_ATTENDENCE;
            }
            addEntityIdsAsIntegers(orgList, buList, projectList, teamList, com.tse.core_application.model.Constants.EntityTypes.ORG, accountIdList, actionId);
            addEntityIdsAsIntegers(orgList, buList, projectList, teamList, com.tse.core_application.model.Constants.EntityTypes.BU, accountIdList, actionId);
            addEntityIdsAsIntegers(orgList, buList, projectList, teamList, com.tse.core_application.model.Constants.EntityTypes.PROJECT, accountIdList, actionId);
            addEntityIdsAsIntegers(orgList, buList, projectList, teamList, com.tse.core_application.model.Constants.EntityTypes.TEAM, accountIdList, actionId);

            removeDuplicates(orgList);
            removeDuplicates(buList);
            removeDuplicates(projectList);
            removeDuplicates(teamList);
        }
    }

    private void addEntityIdsAsIntegers(List<Integer> orgList, List<Integer> buList, List<Integer> projectList, List<Integer> teamList, Integer entityTypeId, List<Long> accountIds, Integer actionId) {

        List<Long> entityIdList = userFeatureAccessRepository.findDistinctEntityIdByEntityTypeIdAndAccountIdInAndActionIdAndIsDeleted(entityTypeId, accountIds, actionId, false);

        if (entityIdList == null || entityIdList.isEmpty()) {
            return;
        }
        if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.ORG, entityTypeId)) {
            orgList.addAll(toIntList(entityIdList));

            List<BU> buEntities = handleNullOrEmptyList(buRepository.findByOrgIdIn(entityIdList));
            buList.addAll(
                    buEntities.stream()
                            .map(BU::getBuId)
                            .filter(Objects::nonNull)
                            .filter(id -> id >= Integer.MIN_VALUE && id <= Integer.MAX_VALUE)
                            .map(Long::intValue)
                            .collect(Collectors.toList())
            );

            projectList.addAll(toIntList(projectRepository.findProjectIdsByOrgIds(entityIdList)));

            teamList.addAll(toIntList(teamRepository.findTeamIdsByOrgIds(entityIdList)));

        } else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.BU, entityTypeId)) {
            buList.addAll(toIntList(entityIdList));

            projectList.addAll(toIntList(projectRepository.findProjectIdsByBuIdIn(entityIdList)));

            teamList.addAll(toIntList(teamRepository.findTeamIdsByBuIds(entityIdList)));

        } else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.PROJECT, entityTypeId)) {
            projectList.addAll(toIntList(entityIdList));

            List<Long> teamIdsByProject = entityIdList.stream()
                    .filter(Objects::nonNull)
                    .map(teamRepository::findTeamIdsByProjectIdIn)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            teamList.addAll(toIntList(teamIdsByProject));

        } else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.TEAM, entityTypeId)) {
            teamList.addAll(toIntList(entityIdList));
        }
    }

    private static <T> List<T> handleNullOrEmptyList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static List<Integer> toIntList(List<Long> longs) {
        return handleNullOrEmptyList(longs).stream()
                .filter(Objects::nonNull)
                .filter(v -> v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
                .map(Long::intValue)
                .distinct()
                .collect(Collectors.toList());
    }

    private static void removeDuplicates(List<Integer> list) {
        if (list != null && !list.isEmpty()) {
            Set<Integer> uniqueSet = new LinkedHashSet<>(list);
            list.clear();
            list.addAll(uniqueSet);
        }
    }

    /**
     * This api returns the org team drop down structure, with all the organizations
     */
    public Organizations getAllOrgTeamsStructures(String userName) {
        Organizations organizations = new Organizations();
        List<Organization> userAllOrganizations = new ArrayList<>();
        List<BU> userAllBU = new ArrayList<>();
        List<Project> userAllProject = new ArrayList<>();
        List<Team> allMyTeamsFound = new ArrayList<>();

        if(userName != null && !userName.isEmpty() && !userName.isBlank()) {
            com.tse.core_application.model.User foundUserDb = getUserByUserName(userName);
            if(foundUserDb != null) {
                //getting all the org ids for user
                List<Long> allOrgIds = userAccountRepository.findOrgIdByFkUserIdUserIdAndIsActive(foundUserDb.getUserId(), true).stream().map(OrgId::getOrgId).collect(Collectors.toList());
                if (!allOrgIds.isEmpty()) {
                    allMyTeamsFound = teamService.getAllMyTeamsByUserId(foundUserDb.getUserId());
                    if(!allMyTeamsFound.isEmpty()) {
                        List<Integer> allProjectIds = new ArrayList<>();
                        List<Long> allBUIds = new ArrayList<>();
                        for(Team team: allMyTeamsFound) {
                            if(team.getFkProjectId() != null && !allProjectIds.contains(Math.toIntExact(team.getFkProjectId().getProjectId()))) {
                                allProjectIds.add(Math.toIntExact(team.getFkProjectId().getProjectId()));
                            }
                            if(team.getFkProjectId() != null && team.getFkProjectId() != null) {
                                Project foundProjectDb = projectService.getProjectByProjectIdAndOrgId(team.getFkProjectId().getProjectId(), team.getFkOrgId().getOrgId());
                                if(foundProjectDb != null) {
                                    if (!allBUIds.contains(foundProjectDb.getBuId())) {
                                        allBUIds.add(foundProjectDb.getBuId());
                                    }
                                }
                            }
                            if(team.getFkOrgId() != null && !allOrgIds.contains(team.getFkOrgId().getOrgId())) {
                                allOrgIds.add(team.getFkOrgId().getOrgId());
                            }
                        }

                        if(!allProjectIds.isEmpty()) {
                            userAllProject = projectService.getAllProjectsByProjectsIds(allProjectIds);
                        }
                        if(!allOrgIds.isEmpty()) {
                            userAllOrganizations = organizationService.getAllOrganizationByOrgIds(allOrgIds);
                        }
                        if(!allBUIds.isEmpty()) {
                            userAllBU = buService.getAllBUsByBUIds(allBUIds);
                        }
                    }
                }


                List<OrgBUProjectTeamStructure> orgBUProjectTeamStructures = new ArrayList<>();
                for(Organization organization: userAllOrganizations) {
                    OrgBUProjectTeamStructure orgBUProjectTeamStructure = new OrgBUProjectTeamStructure();
                    orgBUProjectTeamStructure.setOrgId(organization.getOrgId());
                    orgBUProjectTeamStructure.setOrgName(organization.getOrganizationName());

                    List<BUProject> buProjects = new ArrayList<>();
                    List<BU> userAllBUs = new ArrayList<>();
//                    userAllBU.retainAll(Collections.singleton(organization.getOrgId()));
                    for(BU bu: userAllBU) {
                        if(Objects.equals(bu.getOrgId(), organization.getOrgId())) {
                            userAllBUs.add(bu);
                        }
                    }

                    for(BU bu: userAllBUs) {
                        BUProject buProject = new BUProject();
                        buProject.setBuId(bu.getBuId());
                        buProject.setBuName(bu.getBuName());

                        List<ProjectTeam> projectTeams = new ArrayList<>();
                        List<Project> allProjectsForBUAndOrg = new ArrayList<>();
                        for(Project project: userAllProject) {
                            if(project.getBuId() != null && project.getOrgId() != null) {
                                if(Objects.equals(project.getBuId(), bu.getBuId()) && Objects.equals(project.getOrgId(), organization.getOrgId())) {
                                    allProjectsForBUAndOrg.add(project);
                                }
                            }
                        }

                        for(Project project: allProjectsForBUAndOrg) {
                            ProjectTeam projectTeam = new ProjectTeam();
                            projectTeam.setProjectId(project.getProjectId());
                            projectTeam.setProjectName(project.getProjectName());

                            List<TeamIdAndTeamName> teamIdAndTeamNames = new ArrayList<>();
                            for(Team team: allMyTeamsFound) {
                                if(team.getFkProjectId() != null && team.getFkOrgId() != null) {
                                    if(Objects.equals(team.getFkProjectId().getProjectId(), project.getProjectId()) && Objects.equals(team.getFkOrgId().getOrgId(), organization.getOrgId())) {
                                        TeamIdAndTeamName teamIdAndTeamName = new TeamIdAndTeamName(team.getTeamId(), team.getTeamName(), team.getTeamCode(), team.getIsDeleted());
                                        if (organization.getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                                            if (Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
                                                if (teamService.getAllTeamsForCreateTask(foundUserDb.getPrimaryEmail(), organization.getOrgId()).size() > 1) {
                                                    teamIdAndTeamName.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                                                } else {
                                                    teamIdAndTeamName.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                                                }
                                            }
                                        }
                                        teamIdAndTeamNames.add(teamIdAndTeamName);
                                    }
                                }
                            }
                            projectTeam.setTeams(teamIdAndTeamNames);
                            projectTeams.add(projectTeam);
                        }
                        buProject.setProjects(projectTeams);
                        buProjects.add(buProject);
                    }
                    orgBUProjectTeamStructure.setBu(buProjects);
                    orgBUProjectTeamStructures.add(orgBUProjectTeamStructure);
                }
                organizations.setOrganizations(orgBUProjectTeamStructures);
            }
        }
        return organizations;
    }

    /**
     * This method validates and send user otp for verification
     */
    public ResponseEntity<Object> sendManagingUserRequest(Otp req, com.tse.core_application.model.User user, String screenName, String timeZone) {
        validateUsersForManagement(req, user);
        ResponseEntity<String> otp = new ResponseEntity<String>(otpService.sendOtp(req), HttpStatus.OK);
        return otpService.getFormattedSendOtpResponse(otp, req);
    }

    private void validateUsersForManagement (Otp req, com.tse.core_application.model.User foundUser) {
        if (foundUser.getPrimaryEmail().equalsIgnoreCase(req.getUsername())) {
            throw new IllegalStateException("User not allowed request managing access for same account");
        }
        com.tse.core_application.model.User managedUser = userRepository.findByPrimaryEmail(req.getUsername().toLowerCase());
        if (managedUser == null) {
            throw new ValidationFailedException("User doesnot exists");
        }
        if (managedUser.getIsUserManaging() != null && managedUser.getIsUserManaging()) {
            throw new IllegalStateException("User can either be a managing User or get managed");
        }
        if (managedUser.getManagingUserId() != null) {
            throw new IllegalStateException("User is being managed by other user, please remove the managing access from previous user before adding other managing account");
        }

    }

    public void verifyOtpAndAddManagedUser(AddUserRequest req, com.tse.core_application.model.User managingUser, String timeZone) throws AuthenticationException {
        String verifyOtpResp = otpService.verifyOtp(req.getDeviceUniqueIdentifier(), req.getUsername(), req.getOtp());
        if(verifyOtpResp.equalsIgnoreCase(Constants.SUCCESS)) {
            List<com.tse.core_application.model.User> userList = new ArrayList<>();
            com.tse.core_application.model.User managedUser = userRepository.findByPrimaryEmail(req.getUsername().toLowerCase());

            if (managedUser != null && managingUser != null) {
                if (managedUser.getManagingUserId() != null) {
                    throw new ValidationFailedException("User is already being managed.");
                }
                if (managingUser.getManagingUserId() != null) {
                    throw new ValidationFailedException("User is can either manage or get managed.");
                }
                managedUser.setManagingUserId(managingUser.getUserId());
                managingUser.setIsUserManaging(true);
                userList.add(managedUser);
                userList.add(managingUser);
                userRepository.saveAll(userList);
                List<Long> accountIdList = userAccountRepository.findAllAccountIdsByUserIdAndIsActive(managingUser.getUserId(), true);

                if (accountIdList != null && !accountIdList.isEmpty()) {
                    for (Long accountId : accountIdList) {
                        RemoveOrgMemberRequest removeOrgMemberRequest = new RemoveOrgMemberRequest();
                        removeOrgMemberRequest.setAccountId(accountId);
                        notificationService.sendLogoutNotification(removeOrgMemberRequest, timeZone);
                    }
                }
            }
        } else {
            throw new AuthenticationException("Please enter a valid otp");
        }

    }

    public ResponseEntity<Object> sendRemovingUserRequest(Otp req, com.tse.core_application.model.User user, String screenName, String timeZone) {
        validateUsersForRemoval(req, user);
        ResponseEntity<String> otp = new ResponseEntity<String>(otpService.sendOtp(req), HttpStatus.OK);
        return otpService.getFormattedSendOtpResponse(otp, req);
    }

    private void validateUsersForRemoval(Otp req, com.tse.core_application.model.User foundUser) {
        if (foundUser.getPrimaryEmail().equalsIgnoreCase(req.getUsername())) {
            throw new IllegalStateException("User not allowed remove managing access for same account");
        }

        com.tse.core_application.model.User managedUser = userRepository.findByPrimaryEmail(req.getUsername().toLowerCase());
        if (managedUser == null) {
            throw new ValidationFailedException("User does not exists");
        }

        if (managedUser.getManagingUserId() != null && !Objects.equals(managedUser.getManagingUserId(), foundUser.getUserId())) {
            throw new IllegalStateException("User not authorized to remove managing access for other users.");
        }
    }

    public void verifyOtpAndRemoveManagedUser(AddUserRequest req, com.tse.core_application.model.User managingUser, String timeZone) throws AuthenticationException {
        String verifyOtpResp = otpService.verifyOtp(req.getDeviceUniqueIdentifier(), req.getUsername(), req.getOtp());
        if(verifyOtpResp.equalsIgnoreCase(Constants.SUCCESS)) {
            List<com.tse.core_application.model.User> userList = new ArrayList<>();
            com.tse.core_application.model.User managedUser = userRepository.findByPrimaryEmail(req.getUsername().toLowerCase());

            if (managedUser != null && managingUser != null) {
                if (managedUser.getManagingUserId() != null && !Objects.equals(managedUser.getManagingUserId(), managingUser.getUserId())) {
                    throw new ValidationFailedException("User is already being managed by some other user.");
                }
                if (!managingUser.getIsUserManaging()) {
                    throw new ValidationFailedException("User not authorised to remove managing access.");
                }
                managedUser.setManagingUserId(null);
                List<com.tse.core_application.model.User> managedUserList = userRepository.findAllUserByManagingUserId(managingUser.getUserId());
                if (managedUserList.size() < 2) {
                    managingUser.setIsUserManaging(false);
                }
                userList.add(managedUser);
                userList.add(managingUser);
                userRepository.saveAll(userList);
                List<Long> accountIdList = userAccountRepository.findAllAccountIdsByUserIdAndIsActive(managingUser.getUserId(), true);

                if (accountIdList != null && !accountIdList.isEmpty()) {
                    for (Long accountId : accountIdList) {
                        RemoveOrgMemberRequest removeOrgMemberRequest = new RemoveOrgMemberRequest();
                        removeOrgMemberRequest.setAccountId(accountId);
                        notificationService.sendLogoutNotification(removeOrgMemberRequest, timeZone);
                    }
                }
            }
        } else {
            throw new AuthenticationException("Please enter a valid otp");
        }
    }

    public List<Long> getAllAccountIds (com.tse.core_application.model.User user) {
        List<Long> accountIdList = new ArrayList<>(userAccountRepository.findAllAccountIdsByUserIdInAndIsActiveAndIsVerifiedTrue(List.of(user.getUserId()), true));
        if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
            List<Long> userList = userRepository.findAllUserIdByManagingUserId(user.getUserId());
            accountIdList.addAll(userAccountRepository.findAllAccountIdsByUserIdInAndIsActiveAndIsVerifiedTrue(userList, true));
        }
        return accountIdList;
    }

    public List<Long> getManagedUserList (Long userId) {
        List<Long> userList = new ArrayList<>();
        userList.add(userId);
        com.tse.core_application.model.User user = userRepository.findByUserId(userId);
        if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
            userList.addAll(userRepository.findAllUserIdByManagingUserId(userId));
        }
        return userList;
    }

    public List<Long> getActiveAccountIdListForUserId(Long userId) {
        List<Long> accountIds = new ArrayList<Long>();
        List<UserAccount> userAccountsForUser = new ArrayList<>();
        userAccountsForUser = userAccountService.getAllUserAccountByUserIdAndIsActive(userId);

        return userAccountsForUser.stream().map(UserAccount::getAccountId).collect(Collectors.toList());
    }

    @Transactional
    public void changeUserName (UserNameChangeRequest userNameChangeRequest, Long requesterAccountId, String timeZone) {
        if (!accessDomainRepository.existsByEntityTypeIdAndAccountIdInAndIsActiveAndRoleIdIn(com.tse.core_application.model.Constants.EntityTypes.ORG, List.of(requesterAccountId), true, List.of(RoleEnum.ORG_ADMIN.getRoleId()))) {
            throw new ValidationFailedException("Only org admin can edit the name");
        }
        Optional<com.tse.core_application.model.User> userOptional = userRepository.findById(userAccountRepository.findUserIdByAccountId(userNameChangeRequest.getAccountId()));

        RestTemplate restTemplate = new RestTemplate();
        String url = conversationBaseUrl + ControllerConstants.Conversations.changeUserName;

        if (userOptional.isPresent()) {
            com.tse.core_application.model.User user = userOptional.get();
            user.setFirstName(userNameChangeRequest.getFirstName());
            user.setMiddleName(userNameChangeRequest.getMiddleName()!=null ? userNameChangeRequest.getMiddleName() : null);
            user.setLastName(userNameChangeRequest.getLastName());

            // calling conversation api to changeName in ConvDB too.
            HttpHeaders headers = new HttpHeaders();
            User tokenUser = new User();
            tokenUser.setUsername(user.getPrimaryEmail());
            String token = jwtUtil.generateToken(tokenUser, List.of(0L));

            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntity = new HttpEntity<>(null, headers);

            String uri = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("firstName", userNameChangeRequest.getFirstName())
                    .queryParam("middleName", userNameChangeRequest.getMiddleName())
                    .queryParam("lastName", userNameChangeRequest.getLastName())
                    .buildAndExpand(userNameChangeRequest.getAccountId())
                    .toUriString();
            try {
                ResponseEntity<String> userResponse = restTemplate.exchange(uri , HttpMethod.POST, requestEntity, String.class);
                String responseMessage = userResponse.getBody();
                logger.info("Response from Conversation after changing name: "+ responseMessage);
            } catch (Exception e){
                throw new IllegalStateException("Failed to execute operation in Conversation DB");
            }
            userRepository.save(user);
            // changing name from the invitation table
            List<Invite> invites = inviteRepository.findByPrimaryEmail(user.getPrimaryEmail());
            if(invites!=null && !invites.isEmpty()){
                invites = invites.stream().peek(invite -> {
                    if(userNameChangeRequest.getFirstName()!=null) invite.setFirstName(userNameChangeRequest.getFirstName());
                    if(userNameChangeRequest.getMiddleName()!=null) invite.setMiddleName(userNameChangeRequest.getMiddleName());
                    if(userNameChangeRequest.getLastName()!=null) invite.setLastName(userNameChangeRequest.getLastName());
                }).collect(Collectors.toList());
                inviteRepository.saveAll(invites);
            }
            List<HashMap<String, String>> payload = notificationService.sendNotificationForChangedUserName(user, userNameChangeRequest.getAccountId(), requesterAccountId, timeZone);
            taskServiceImpl.sendPushNotification(payload);
        } else {
            throw new ValidationFailedException("User doesn't exist");
        }
    }
}
