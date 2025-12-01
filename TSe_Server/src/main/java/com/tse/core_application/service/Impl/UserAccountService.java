package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.utilities.Pair;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.OrgId;
import com.tse.core_application.custom.model.RestRespWithData;
import com.tse.core_application.dto.AiMLDtos.AiUserApisResponse;
import com.tse.core_application.dto.BlockedByRequestDto;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.dto.conversations.ConversationUser;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.OrganizationDoesNotExistException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.IUserAccountService;
import com.tse.core_application.utils.JWTUtil;
import com.tse.core_application.validators.annotations.TrimmedSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.model.Constants.projectAccess;
import static com.tse.core_application.model.Constants.teamAccess;

@Service
public class UserAccountService implements IUserAccountService {

    private static final Logger logger = LogManager.getLogger(UserAccountService.class.getName());

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    UserAccountRepositoryV2 userAccountRepositoryV2;

    @Autowired
    UserRepository userRepository;
    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    UserAccountService userAccountService;

    @Autowired
    UserService userService;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    ConversationService conversationService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JavaMailSender emailSender;

    @Value("${conversation.application.root.path}")
    private String conversationBaseUrl;

    @Value("${system.admin.email}")
    private String superAdminMail;

    @Value("${spring.mail.username}")
    private String username;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private AiMlService aiMlService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    public List<Long> getUserActiveAccountIdsFromUserId(Long userId){
        return userAccountRepository.getUserAccountIdsFromUserIdAndIsActive(userId, true);
    }

    public List<Long> getActiveAccountIdsForUserId(Long userId, List<Long> orgIds) {
        List<Long> accountIds = new ArrayList<Long>();
        List<UserAccount> userAccountsForUser = new ArrayList<>();
        if (orgIds != null) {
            List<Long> userList = new ArrayList<>();
            User user = userRepository.findByUserId(userId);
            userList.add(userId);
            if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
                userList.addAll(userRepository.findAllUserIdByManagingUserId(user.getUserId()));
            }
            userAccountsForUser = userAccountRepository.findByFkUserIdUserIdInAndOrgIdInAndIsActive(userList, orgIds, true);
        } else {
            userAccountsForUser = userAccountService.getAllUserAccountByUserIdAndIsActive(userId);
        }

        return userAccountsForUser.stream().map(UserAccount::getAccountId).collect(Collectors.toList());
    }

    public List<Long> getOrganizationIdsForUserId(Long userId) {
        List<Long> organizationIds = new ArrayList<Long>();
        List<UserAccount> userAccountsForUser = userAccountService.getAllUserAccountByUserIdAndIsActive(userId);

//        22/04/2022: isDefault is not being used. Hence, commenting the code.
//        if (userAccountsForUser.size() > 0) {
//            organizationIds = userAccountsForUser.stream().filter(userAccount -> {
//                if (userAccount.getIsDefault()) {
//                    return true;
//                }
//                return false;
//            }).map(UserAccount::getOrgId).collect(Collectors.toList());
//        }

//        if (organizationIds.size() == 0 && userAccountsForUser.size() > 0) {
//            organizationIds.add(userAccountsForUser.get(0).getOrgId());
//        }

        for(UserAccount userAccount: userAccountsForUser) {
            organizationIds.add(userAccount.getOrgId());
        }

        return organizationIds;

    }


    @Override
    public UserAccount addUserAccount(UserAccount userAccount, ConversationGroup conversationGroup, User user, String timeZone) {
        UserAccount savedUserAccount = this.userAccountRepositoryV2.save(userAccount);
        registerDeregisterIntoAiService(userAccount.getAccountId(), true, timeZone);
        RestTemplate restTemplate = new RestTemplate();
        String url = conversationBaseUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        ConversationUser conversationUser = new ConversationUser();
        conversationUser.setUserId(savedUserAccount.getFkUserId().getUserId());
        conversationUser.setFirstName(savedUserAccount.getFkUserId().getFirstName());
        conversationUser.setLastName(savedUserAccount.getFkUserId().getLastName());
        conversationUser.setOrgId(savedUserAccount.getOrgId());
        conversationUser.setAccountId(savedUserAccount.getAccountId());
        conversationUser.setEmail(savedUserAccount.getEmail());
        conversationUser.setGroups(List.of());
        conversationUser.setIsOrgAdmin(conversationGroup.getEntityId() != null);
        conversationUser.setIsActive(true);
        conversationGroup = conversationService.getGroup(userAccount.getOrgId(), (long) com.tse.core_application.model.Constants.EntityTypes.ORG, user);
        ConversationUser savedConversationUser = new ConversationUser();

        com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
        tokenUser.setUsername(user.getPrimaryEmail());
        String token = jwtUtil.generateToken(tokenUser, List.of(0L));

        headers.add("Authorization", "Bearer " + token);
        headers.add("screenName", "TSE_Server");
        headers.add("accountIds", "0");

        //Create user in conversation
        try {
            HttpEntity<Object> requestEntityUser = new HttpEntity<>(conversationUser, headers);
            ResponseEntity<RestRespWithData<ConversationUser>> userResponse = restTemplate.exchange(url + ControllerConstants.Conversations.createUser, HttpMethod.POST, requestEntityUser, new ParameterizedTypeReference<>() {
            });
            savedConversationUser = Objects.requireNonNull(userResponse.getBody()).getData();
            //adding into group.
            conversationService.addUsersToGroup(conversationGroup, List.of(savedConversationUser.getUserId()), user);
        }
        catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not create new user in Chat DB, please contact " + superAdminMail + ". " + e, new Throwable(allStackTraces));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(superAdminMail);
            message.setSubject("Error occurred while creating Conversation Group");
            message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
            message.setFrom(username);
            emailSender.send(message);
        }
        return savedUserAccount;
    }

    private void registerDeregisterIntoAiService(Long accountId, boolean isRegister, String timezone) {
        if (isRegister) {
            aiMlService.registerUserIntoAiService(accountId, Constants.AiMlConstants.MAX_TOKENS, timezone);
        } else {
            aiMlService.removeUserFromAiService(accountId, timezone, "TseServer");
        }
    }

    public List<UserAccount> getAllUserAccountByUserIdAndIsActive(Long userId) {
        List<Long> userList = userService.getManagedUserList(userId);
        List<UserAccount> userAccounts = userAccountRepository.findByUserIdInAndIsActiveAndIsVerifiedTrue(userList, true);
        return userAccounts;
    }

    public UserAccount getActiveUserAccountByAccountId(Long accountId) {
        UserAccount userAccountDb = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
        if(userAccountDb != null) {
            return userAccountDb;
        } else {
            return null;
        }
    }

    public UserAccount getActiveUserAccountByPrimaryEmailAndOrgId(String primaryEmail, Long orgId) {
        UserAccount userAccountFoundDb = userAccountRepository.findByEmailAndOrgIdAndIsActive(primaryEmail, orgId, true);
        return userAccountFoundDb;
    }

    public ResponseEntity<Object> getAccountIdFormattedResponse(Organization organization, String orgName) {
        if(organization != null) {
            List<AccountId> accountIdsDb = userAccountRepository.findAccountIdByOrgIdAndIsActive(organization.getOrgId(), true);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, accountIdsDb);
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new OrganizationDoesNotExistException());
            logger.error("Organization = " + orgName + " does not exist.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new OrganizationDoesNotExistException();
        }
    }


    public ResponseEntity<Object> getUserByAccountIdFormattedResponse(EmailFirstLastAccountId emailFirstLastAccountId) {
        if(emailFirstLastAccountId != null) {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, emailFirstLastAccountId);
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No Data found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        }
    }

    public List<UserAccount> getAllUserAccountsByOrgId(Long orgId) {
        List<UserAccount> userAccounts = new ArrayList<>();
        if(orgId != null) {
            List<UserAccount> userAccountsFound = userAccountRepository.findByOrgId(orgId);
            userAccounts.addAll(userAccountsFound);
        }
        return userAccounts;
    }

    public List<UserAccount> getAllUserAccountsByAccountIds(List<AccountId> accountIds) {
        List<Long> accountIdsToFind = new ArrayList<>();

        for (AccountId accountId : accountIds) {
            accountIdsToFind.add(accountId.getAccountId());
        }
        return userAccountRepository.findByAccountIdInAndIsActive(accountIdsToFind, true);
    }

    /* This method gets distinct userIds from a list of accountIds */
    public List<Long> getUserIdsFromAccountIds(List<Long> accountIds){
        List<Long> userIds = userAccountRepository.findActiveUserIdsFromAccountIds(accountIds);
        return userIds;
    }

    public UserAccount getUserAccountByAccountIdAndOrgId(Long accountId, Long orgId) {
        return userAccountRepository.findByAccountIdAndOrgId(accountId, orgId);
    }

    public boolean isUserAccountExistsByAccountIdAndOrgId(Long accountId, Long orgId) {
        return userAccountRepository.existsByAccountIdAndOrgIdAndIsActive(accountId, orgId, true);
    }

    public boolean isActiveUserAccountExistsByUserIdAndOrgId(Long userId, Long orgId) {
        return userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(userId, orgId, true);
    }

    public List<Long> getAllOrgIdsByUserId(Long userId) {
        List<Long> allOrgIds = new ArrayList<>();
        List<OrgId> orgIds = userAccountRepository.findOrgIdByFkUserIdUserIdAndIsActive(userId, true);

        for(OrgId orgId : orgIds) {
            allOrgIds.add(orgId.getOrgId());
        }
        return allOrgIds;
    }

    public EmailFirstLastAccountId getEmailFirstLastNameByAccountId(Long accountId) {
        return userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountId);
    }

    public void markAccountAsInactive(Long accountId) {
        redisTemplate.opsForSet().add("INACTIVE_ACCOUNTS", accountId.toString());
        registerDeregisterIntoAiService(accountId, false, "Asia/Calcutta");
    }

    public void markAccountAsActive(Long accountId) {
        redisTemplate.opsForSet().remove("INACTIVE_ACCOUNTS", accountId.toString());
        registerDeregisterIntoAiService(accountId, true, "Asia/Calcutta");
    }

    public void deactivateUserAccount(String username, Integer deactivatedByRole, Long deactivatedByAccountId) {
        userAccountRepository.updateIsActiveAndIsDisabledBySamsByEmail(username, false, true, deactivatedByRole, deactivatedByAccountId);
        redisTemplate.opsForSet().add("DEACTIVATED_USERS", username);
    }

    public void reactivateUserAccount(String username) {
        userAccountRepository.updateIsActiveAndIsDisabledBySamsByEmail(username, true, false, null, null);
        redisTemplate.opsForSet().remove("DEACTIVATED_USERS", username);
    }

    public void removeUserFromRedis(String username) {
        redisTemplate.opsForSet().remove("DEACTIVATED_USERS", username);
    }

    public Pair<String, String> getAccountNameAndEmailByAccountId(Long accountId) {
        String fullName = "", email = "";
        UserAccount userAccount = userAccountRepository.findFirstByAccountId(accountId);
        if (userAccount != null) {
            User user = userAccount.getFkUserId();
            fullName = user.getFirstName() +
                    (user.getMiddleName() != null ? " " + user.getMiddleName() + " " : " ") +
                    (user.getLastName() != null ? user.getLastName() : "");
            email = userAccount.getEmail();
        }
        return new Pair<>(fullName, email);
    }

    @PostConstruct
    public void populateAllActiveAccounts() throws JsonProcessingException {
        // Retrieve all users
        List<User> allUserList = userRepository.findAll();

        for (User user : allUserList) {
            List<Long> accountIdList = new ArrayList<>(userAccountRepository.findAllAccountIdsByUserIdAndIsActive(user.getUserId(), true));
            if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
                List<Long> userList = userRepository.findAllUserIdByManagingUserId(user.getUserId());
                accountIdList.addAll(userAccountRepository.findAllAccountIdsByUserIdInAndIsActive(userList, true));
            }
            if (!accountIdList.isEmpty()) {
                addAccountToUsers(user.getUserId(), accountIdList);
            }
        }
    }

    public void addAccountToUsers(Long userId, List<Long> accountIdList) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String accountIds = objectMapper.writeValueAsString(accountIdList);
        redisTemplate.opsForHash().put("USERS", userId.toString(), accountIds);
    }

    public void removeAccountFromUsers(Long userId, List<Long> accountIdList) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String accountIds = (String) redisTemplate.opsForHash().get("USERS", userId.toString());
        if (accountIds != null) {
            List<Long> accountId = objectMapper.readValue(accountIds, new TypeReference<List<Long>>() {
            });
            accountId.removeAll(accountIdList);
            String accountIdString = objectMapper.writeValueAsString(accountId);
            redisTemplate.opsForHash().put("USERS", userId.toString(), accountIdString);
        }
    }

    public void validateIfUserDeactivated (String username) {
        Boolean isUserDeactivated = redisTemplate.opsForSet().isMember("DEACTIVATED_USERS", username);
        if (Boolean.TRUE.equals(isUserDeactivated)) {
            throw new ValidationFailedException("Account is deactivated. Please contact the System Administrator.");
        }
    }

    public List<EmailFirstLastAccountIdIsActive> excludeTeamMemberFromOrg(Long teamId) {
        Team team = teamRepository.findByTeamId(teamId);
        if (team == null) {
            throw new EntityNotFoundException("Team is either invalid or deleted");
        }
        List<EmailFirstLastAccountIdIsActive> userListResponses = null;
        if (organizationRepository.findByOrgId(team.getFkOrgId().getOrgId()).getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
            userListResponses = Collections.emptyList();
        } else {
            userListResponses = userAccountRepository.getEmailAccountIdFirstAndLastNameByOrgId(team.getFkOrgId().getOrgId());
            //exclude Team Member Logic need to add here
            List<Long> teamMembers = accessDomainRepository.findAllActiveAccountIdsByEntityAndTypeIds(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamId);
            // Exclude team members from org users
            if (userListResponses != null && !teamMembers.isEmpty()) {
                userListResponses = userListResponses.stream()
                        .filter(user -> !teamMembers.contains(user.getAccountId()))
                        .collect(Collectors.toList());
            }
        }
        if (userListResponses != null && !userListResponses.isEmpty()) {
            userListResponses.sort(Comparator
                    .comparing((EmailFirstLastAccountIdIsActive status) -> {
                        if (status.getIsActive() == null) return 2;
                        return status.getIsActive() ? 0 : 1;
                    })
                    .thenComparing(EmailFirstLastAccountIdIsActive::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountIdIsActive::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountIdIsActive::getAccountId, Comparator.nullsLast(Long::compareTo))
            );
        }
        return userListResponses;
    }

    public List<EmailFirstLastAccountIdIsActive> blockedByMembers(BlockedByRequestDto blockedByRequestDto, String accountIds) {
        if(blockedByRequestDto.getBlockReasonTypeId()==null || blockedByRequestDto.getBlockReasonTypeId().isEmpty())
        {
            blockedByRequestDto.setBlockReasonTypeId(com.tse.core_application.model.Constants.blockedTypesList);
        }
        Set<EmailFirstLastAccountIdIsActive> userListResponses = new HashSet<>();
        List<Long> blockedByAccountIds = new ArrayList<>();
        if (accountIds != null && !accountIds.isEmpty()) {
            blockedByAccountIds = Arrays.stream(accountIds.split(","))
                    .filter(id -> !id.isBlank())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        Set<Team> allTeamList = new HashSet<>();
        if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.ORG, blockedByRequestDto.getEntityTypeId())) {
            List<Project> projectList = projectRepository.findByOrgIdIn(List.of(blockedByRequestDto.getEntityId()));
            for (Project project : projectList) {
                List<Team> teamList = teamRepository.findByFkProjectIdProjectId(project.getProjectId());
                if (!blockedByAccountIds.isEmpty() &&
                        accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(
                                com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                                project.getProjectId(),
                                blockedByAccountIds.get(0),
                                true,
                                projectAccess)) {
                    allTeamList.addAll(teamList);
                } else {
                    for (Team team : teamList) {
                        boolean hasAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(
                                com.tse.core_application.model.Constants.EntityTypes.TEAM,
                                team.getTeamId(),
                                blockedByAccountIds.get(0),
                                true,
                                teamAccess);

                        if (hasAccess) {
                            allTeamList.add(team);
                        }
                    }
                }
            }
        } else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.PROJECT, blockedByRequestDto.getEntityTypeId())) {
            Long projectId = blockedByRequestDto.getEntityId();
            List<Team> teamList = teamRepository.findByFkProjectIdProjectId(projectId);
            if (!blockedByAccountIds.isEmpty() &&
                    accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(
                            com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                            projectId,
                            blockedByAccountIds.get(0),
                            true,
                            projectAccess)) {
                allTeamList.addAll(teamList);
            } else {
                for (Team team : teamList) {
                    if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(
                            com.tse.core_application.model.Constants.EntityTypes.TEAM,
                            team.getTeamId(),
                            blockedByAccountIds.get(0),
                            true,
                            teamAccess)) {
                        allTeamList.add(team);
                    }
                }
            }
        } else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.TEAM, blockedByRequestDto.getEntityTypeId())) {
            Team team = teamRepository.findById(blockedByRequestDto.getEntityId()).orElse(null);
            if (team != null) {
                Long teamId = team.getTeamId();
                Long projectId = team.getFkProjectId() != null ? team.getFkProjectId().getProjectId() : null;

                boolean hasTeamAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(
                        com.tse.core_application.model.Constants.EntityTypes.TEAM,
                        teamId,
                        blockedByAccountIds.get(0),
                        true,
                        teamAccess);
                boolean hasProjectAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActiveAndRoleIdIn(
                        com.tse.core_application.model.Constants.EntityTypes.PROJECT,
                        projectId,
                        blockedByAccountIds.get(0),
                        true,
                        projectAccess);
                if (hasTeamAccess || hasProjectAccess) {
                    allTeamList.add(team);
                }
            }
        }
        if (!allTeamList.isEmpty()) {
            List<Long> teamIds = allTeamList.stream()
                    .map(Team::getTeamId)
                    .collect(Collectors.toList());
            List<Long> userAccountIds = taskRepository.findBlockedByAccountIdsByTeamIdAndReasonTypeIdAndStatus(teamIds, blockedByRequestDto.getBlockReasonTypeId(), com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE);
            userListResponses.addAll(
                    userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(userAccountIds));
        }
        List<EmailFirstLastAccountIdIsActive> finalList = new ArrayList<>(userListResponses);
        if (!finalList.isEmpty()) {
            finalList.sort(Comparator
                    .comparing((EmailFirstLastAccountIdIsActive status) -> {
                        if (status.getIsActive() == null) return 2;
                        return status.getIsActive() ? 0 : 1;
                    })
                    .thenComparing(EmailFirstLastAccountIdIsActive::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountIdIsActive::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountIdIsActive::getAccountId, Comparator.nullsLast(Long::compareTo))
            );
        }
        return finalList;
    }
}

