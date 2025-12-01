package com.tse.core_application.service.Impl;

import com.google.gson.Gson;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.custom.model.RestRespWithData;
import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.conversations.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.Access;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final Logger logger = LogManager.getLogger(ConversationService.class.getName());

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private JavaMailSender emailSender;

    @Value("${conversation.application.root.path}")
    private String conversationBaseUrl;

    @Value("${system.admin.email}")
    private String superAdminMail;

    @Value("${spring.mail.username}")
    private String username;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;

    public ConversationGroup createNewGroup(ConversationGroup conversationGroup, String groupName, String groupType, Long orgId, User user) {
        conversationGroup.setName(groupName);
        conversationGroup.setType(groupType);
        conversationGroup.setUsers(List.of());
        conversationGroup.setOrgId(orgId);
        String url = conversationBaseUrl;
        ConversationGroup savedGroup;
        RestTemplate restTemplate = new RestTemplate();
        try {
            com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
            tokenUser.setUsername(user.getPrimaryEmail());
            String token = jwtUtil.generateToken(tokenUser, List.of(0L));

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntityGroup = new HttpEntity<>(conversationGroup, headers);

            ResponseEntity<RestRespWithData<ConversationGroup>> groupResponse = restTemplate.exchange(url + ControllerConstants.Conversations.createGroup, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<>() {
            });
            savedGroup = Objects.requireNonNull(groupResponse.getBody()).getData();
            return savedGroup;
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e, new Throwable(allStackTraces));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(superAdminMail);
            message.setSubject("Error occurred while creating Conversation Group");
            message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
            message.setFrom(username);
            emailSender.send(message);
        }
        return null;
    }

    public ConversationGroup getGroup(Long entityId, Long entityTypeId, User user) {
        try {
            com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
            tokenUser.setUsername(user.getPrimaryEmail());
            String token = jwtUtil.generateToken(tokenUser, List.of(0L));
            String url = conversationBaseUrl;

            RestTemplate restTemplate = new RestTemplate();
            String getGroupUrl = UriComponentsBuilder.fromHttpUrl(url + ControllerConstants.Conversations.getGroupByEntityIdAndEntityTypeId).buildAndExpand(entityId, entityTypeId).toUriString();

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntityGroup = new HttpEntity<>(headers);

            ResponseEntity<RestRespWithData<ConversationGroup>> groupResponse = restTemplate.exchange(getGroupUrl, HttpMethod.GET, requestEntityGroup, new ParameterizedTypeReference<>() {
            });

            return groupResponse.getBody().getData();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e, new Throwable(allStackTraces));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(superAdminMail);
            message.setSubject("Error occurred while creating Conversation Group");
            message.setText("Could not create Group in Chat DB: please contact " + superAdminMail + ". " + e + allStackTraces);
            message.setFrom(username);
            emailSender.send(message);
        }
        return new ConversationGroup();
    }

    public void addUsersToGroup(ConversationGroup conversationGroup, List<Long> userIds, User user) {
        String url = conversationBaseUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

        RestTemplate restTemplate = new RestTemplate();
        ConversationUser savedConversationUser = new ConversationUser();

        com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
        tokenUser.setUsername(user.getPrimaryEmail());
        String token = jwtUtil.generateToken(tokenUser, List.of(0L));

        headers.add("Authorization", "Bearer " + token);
        headers.add("screenName", "TSE_Server");
        headers.add("accountIds", "0");

        try {
            GroupAndUsersDTO groupAndUsersDTO = new GroupAndUsersDTO(conversationGroup.getGroupId(), userIds);
            HttpEntity<Object> requestEntityGroupAndUser = new HttpEntity<>(groupAndUsersDTO, headers);
            ResponseEntity<RestResponseWithData> response = restTemplate.exchange(url + ControllerConstants.Conversations.addUsersToGroup, HttpMethod.POST, requestEntityGroupAndUser, new ParameterizedTypeReference<>() {
            });
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not add users to group " + conversationGroup.getName() + "in Chat DB, please contact " + superAdminMail + ". " + e, new Throwable(allStackTraces));
        }
    }

    public void updateGroupDetails(Long entityId, Long entityTypeId, GroupUpdateRequest groupRequest, User user) {
        RestTemplate restTemplate = new RestTemplate();
        String updateGroupUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl +
                ControllerConstants.Conversations.updateGroupDetails).buildAndExpand(entityId, entityTypeId).toUriString();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
        tokenUser.setUsername(user.getPrimaryEmail());
        String token = jwtUtil.generateToken(tokenUser, List.of(0L));

        headers.add("Authorization", "Bearer " + token);
        headers.add("screenName", "TSE_Server");
        headers.add("accountIds", "0");

        try {
            HttpEntity<Object> requestEntityUser = new HttpEntity<>(groupRequest, headers);
            ResponseEntity<String> response = restTemplate.exchange(updateGroupUrl, HttpMethod.POST, requestEntityUser, new ParameterizedTypeReference<>() {
            });
            logger.info("Conversation Group Update status: " + response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not update details to group " + groupRequest.getGroupName() + "in Chat DB, please contact " + superAdminMail + ". " + e, new Throwable(allStackTraces));
        }
    }

    public void activateDeactivateUserInConversation(List<Long> accountIds, String username, Boolean isToDeactivate) {
        RestTemplate restTemplate = new RestTemplate();
        String updateGroupUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl +
                ControllerConstants.Conversations.activateDeactivateUser).toUriString();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
        tokenUser.setUsername(username);
        String token = jwtUtil.generateToken(tokenUser, List.of(0L));
        ActivateDeactivateUserRequest request = new ActivateDeactivateUserRequest(accountIds, isToDeactivate);

        headers.add("Authorization", "Bearer " + token);
        headers.add("screenName", "TSE_Server");
        headers.add("accountIds", "0");

        try {
            HttpEntity<Object> requestEntityUser = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(updateGroupUrl, HttpMethod.POST, requestEntityUser, new ParameterizedTypeReference<>() {
            });
            logger.info("Conversation User Activate/Deactivate status: " + response);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Could not Activate/Deactivate for User in Chat DB, please contact " + superAdminMail + ". " + e, new Throwable(allStackTraces));
        }
    }

    public void removeUsersFromGroup(List<Long> removedUserIds, Long entityId, Integer entityTypeId, User user) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String getUserUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl + ControllerConstants.Conversations.removeUsersFromGroupV2).toUriString();

            com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
            tokenUser.setUsername(user.getPrimaryEmail());
            String token = jwtUtil.generateToken(tokenUser, List.of(0L));

            GroupByEntityUsersRequest groupByEntityUsersRequest = GroupByEntityUsersRequest.builder()
                    .entityId(entityId)
                    .entityTypeId(entityTypeId)
                    .usersIds(removedUserIds)
                    .build();

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntity = new HttpEntity<>(groupByEntityUsersRequest, headers);

            ResponseEntity<Object> userResponse = restTemplate.exchange(getUserUrl, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
            });
            logger.info("removed users from Group: " + userResponse.getBody());
        } catch (HttpClientErrorException e) {
            System.out.println("Exception thrown while removing member from group in Conversation service: " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                logger.warn("User is already been removed from the group or either not a part of it. Message from the " + e.getLocalizedMessage());
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Could not remove the member from Conversations DB " + e, new Throwable(allStackTraces));
                throw e;
            }
        }
    }

    public String addNonExistentSystemGroupsIntoConversation(String accountIds) {

        //fetch all the users from the conversation Table to validate the users are already there or not.
        RestTemplate restTemplate = new RestTemplate();
        Gson gson = new Gson();
        List<Long> accountIdsLong = Arrays.stream(accountIds.split(",")).map(Long::parseLong).collect(Collectors.toList());
        try {
            String getAllUsersUrl = UriComponentsBuilder.fromHttpUrl(conversationBaseUrl + ControllerConstants.Conversations.getAllUsers).toUriString();

            com.tse.core_application.dto.User tokenUser = new com.tse.core_application.dto.User();
            tokenUser.setUsername("gauravnit006@gmail.com");
            String token = jwtUtil.generateToken(tokenUser, accountIdsLong);

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Authorization", "Bearer " + token);
            headers.add("screenName", "TSE_Server");
            headers.add("accountIds", "0");
            HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

            // Fetching all the users from conversation table and validating the missing users with the tse's table,
            // if found some then sending the insert request for the remaining users.
            ResponseEntity<RestRespWithData<List<ConversationUser>>> usersResponse = restTemplate.exchange(getAllUsersUrl, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {
            });

            int isUsersMigrationDone = 0;
            if (usersResponse.getBody() != null && usersResponse.getBody().getData() != null) {
                //validate that current users number is similar with db_tse.
                System.out.println("Users presents in Convo DB: " + gson.toJson(usersResponse.getBody().getData()));
                List<ConversationUser> conversationUsers = usersResponse.getBody().getData();

                List<UserAccount> userAccountList = userAccountRepository.findAll().stream().filter(UserAccount::getIsActive).collect(Collectors.toList());
                Map<Long, UserAccount> tseUsersMap = userAccountList.stream().collect(Collectors.toMap(UserAccount::getAccountId, account -> account));

                if (conversationUsers != null && !conversationUsers.isEmpty()) {
                    Set<Long> conversationUserAccountSet = conversationUsers.stream().map(ConversationUser::getAccountId).collect(Collectors.toSet());
                    List<Long> exclusiveUsersList = new ArrayList<>();
                    userAccountList.forEach(account -> {
                        if (!conversationUserAccountSet.contains(account.getAccountId())) {
                            exclusiveUsersList.add(account.getAccountId());
                        }
                    });
                    if (!exclusiveUsersList.isEmpty()) {
                        System.out.println("Users not presents in Convo DB: " + gson.toJson(exclusiveUsersList));
                        Map<Long, EmailFirstLastAccountIdIsActive> userDataDtoMap = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(exclusiveUsersList).stream()
                                .collect(Collectors.toMap(EmailFirstLastAccountIdIsActive::getAccountId, user -> user));
                        List<ConversationUser> newUsers = new ArrayList<>();
                        exclusiveUsersList.forEach(id -> {
                            UserAccount account = tseUsersMap.get(id);
                            EmailFirstLastAccountIdIsActive userData = userDataDtoMap.get(id);
                            ConversationUser conversationUser = ConversationUser.builder()
                                    .email(account.getEmail())
                                    .userId(account.getFkUserId().getUserId())
                                    .accountId(account.getAccountId())
                                    .firstName(userData.getFirstName())
                                    .lastName(userData.getLastName())
                                    .isActive(userData.getIsActive())
                                    .orgId(account.getOrgId())
                                    .build();
                            newUsers.add(conversationUser);
                        });
                        isUsersMigrationDone = 1;
                        if (!newUsers.isEmpty()) {
                            HttpEntity<Object> requestEntityUser = new HttpEntity<>(newUsers, headers);
                            ResponseEntity<RestRespWithData<List<ConversationUser>>> userResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.bulkCreateUser, HttpMethod.POST, requestEntityUser, new ParameterizedTypeReference<>() {
                            });
                            isUsersMigrationDone = 2;
                            System.out.println("users which are newly added into the conversationDB " + gson.toJson(Objects.requireNonNull(userResponse.getBody()).getData()));
                        }
                    }
                }
                if (isUsersMigrationDone == 1) {
                    throw new ValidationFailedException("There are some user's data which are failed to migrate !!! Please check it once..");
                }

                ResponseEntity<RestRespWithData<List<ConversationGroup>>> groupsResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.getAllGroups, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {
                });
                if (groupsResponse.getBody() != null) {
                    List<ConversationGroup> groupsData = Objects.requireNonNull(groupsResponse.getBody().getData());
//                    System.out.println("GroupsData from the conversation table: " + gson.toJson(groupsData));

                    //assumption: I am assuming every entry in the groups is having entityId and entityTypeId
                    Map<String, ConversationGroup> groupsDataMap = groupsData.stream().filter(group -> group.getEntityId() != null && group.getIsActive())
                            .collect(Collectors.toMap(group -> group.getEntityTypeId().toString() + "_" + group.getEntityId(), group -> group));

                    List<UserAccount> allUserAccounts = userAccountRepository.findAll();
                    Map<Long, List<UserAccount>> userAccountOrgMap = allUserAccounts.stream().collect(Collectors.groupingBy(UserAccount::getOrgId));
                    Map<Long, Long> accountIdUserIdMap = allUserAccounts.stream().collect(Collectors.toMap(UserAccount::getAccountId, account -> account.getFkUserId().getUserId()));
                    List<GroupAndUsersDTO> groupAndUsersDTOList = new ArrayList<>();
                    Map<Long, List<AccessDomain>> teamAccountIds = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndIsActive(Constants.EntityTypes.TEAM, true).stream().collect(Collectors.groupingBy(AccessDomain::getEntityId));
                    Map<Long, List<AccessDomain>> projectAccountIds = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndIsActive(Constants.EntityTypes.PROJECT, true).stream().collect(Collectors.groupingBy(AccessDomain::getEntityId));

                    //segregate the groups
                    List<Organization> allOrgs = organizationRepository.findAll().stream().filter(org -> org.getIsDisabled() == null || !org.getIsDisabled()).collect(Collectors.toList());
                    Map<Long, Organization> allOrgsMap = allOrgs.stream().collect(Collectors.toMap(Organization::getOrgId, org -> org));
                    List<Long> exclusiveOrgs = new ArrayList<>();

                    List<Team> allTeams = teamRepository.findAll().stream().filter(team -> !(team.getIsDisabled() != null && team.getIsDisabled()) || !(team.getIsDeleted() != null && team.getIsDeleted())).collect(Collectors.toList());
                    Map<Long, Team> allTeamMap = allTeams.stream().collect(Collectors.toMap(Team::getTeamId, team -> team));
                    List<Long> exclusiveTeams = new ArrayList<>();

                    List<Project> allProjects = projectRepository.findAll().stream().filter(proj -> !(proj.getIsDisabled() != null && proj.getIsDisabled()) || !(proj.getIsDeleted() != null && proj.getIsDeleted())).collect(Collectors.toList());
                    List<Long> exclusiveProjects = new ArrayList<>();
                    Map<Long, Project> allProjectMap = allProjects.stream().collect(Collectors.toMap(Project::getProjectId, project -> project));

                    allOrgs.forEach(org -> {
                        String key = Constants.EntityTypes.ORG + "_" + org.getOrgId();
                        if (groupsDataMap.get(key) == null) {
                            exclusiveOrgs.add(org.getOrgId());
                        } else {
                            List<Long> existingOrgMembersList = groupsDataMap.get(key).getUsers().stream().map(ConversationUser::getAccountId).collect(Collectors.toList());
                            List<UserAccount> userAccountList1 = userAccountOrgMap.get(org.getOrgId());
                            if (userAccountList1.size() != existingOrgMembersList.size()) {
                                GroupAndUsersDTO groupAndUsersDTO = new GroupAndUsersDTO(groupsDataMap.get(key).getGroupId(),
                                        userAccountList1.stream().filter(account -> !existingOrgMembersList.contains(account.getAccountId())).map(account -> account.getFkUserId().getUserId()).distinct().collect(Collectors.toList())
                                );
                                if(!groupAndUsersDTO.getUserIds().isEmpty()) {
                                    groupAndUsersDTOList.add(groupAndUsersDTO);
                                    System.out.println("Org Members not in org : " + org.getOrganizationName() +" : "+ org.getOrgId() + " are " + gson.toJson(groupAndUsersDTO));
                                }
                            }
                        }
                    });
                    allTeams.forEach(team -> {
                        String key = Constants.EntityTypes.TEAM + "_" + team.getTeamId();
                        if (groupsDataMap.get(key) == null) {
                            exclusiveTeams.add(team.getTeamId());
                        } else {
                            List<Long> existingTeamMembersList = groupsDataMap.get(key).getUsers().stream().map(ConversationUser::getAccountId).collect(Collectors.toList());
                            List<Long> userAccountList1 = teamAccountIds.get(team.getTeamId()) != null ? teamAccountIds.get(team.getTeamId()).stream().map(AccessDomain::getAccountId).collect(Collectors.toList()) : new ArrayList<>();
                            if (userAccountList1.size() != existingTeamMembersList.size()) {
                                GroupAndUsersDTO groupAndUsersDTO = new GroupAndUsersDTO(groupsDataMap.get(key).getGroupId(),
                                        userAccountList1.stream().filter(account -> !existingTeamMembersList.contains(account)).map(accountIdUserIdMap::get).distinct().collect(Collectors.toList())
                                );
                                if(!groupAndUsersDTO.getUserIds().isEmpty()) {
                                    groupAndUsersDTOList.add(groupAndUsersDTO);
                                    System.out.println("Members not in team : " + team.getTeamName() + " : " + team.getTeamId() + " are " + gson.toJson(groupAndUsersDTO));
                                }
                            }
                        }
                    });
                    allProjects.forEach(project -> {
                        String key = Constants.EntityTypes.PROJECT + "_" + project.getProjectId();
                        if (!project.getProjectName().equals(com.tse.core_application.constants.Constants.PROJECT_NAME)) {
                            if (groupsDataMap.get(key) == null) {
                                exclusiveProjects.add(project.getProjectId());
                            } else {
                                List<Long> existingProjectMembersList = groupsDataMap.get(key).getUsers().stream().map(ConversationUser::getAccountId).collect(Collectors.toList());
                                List<Long> userAccountList1 = projectAccountIds.get(project.getProjectId()) != null ? projectAccountIds.get(project.getProjectId()).stream().map(AccessDomain::getAccountId).collect(Collectors.toList()) : new ArrayList<>();
                                if (userAccountList1.size() != existingProjectMembersList.size()) {
                                    GroupAndUsersDTO groupAndUsersDTO = new GroupAndUsersDTO(groupsDataMap.get(key).getGroupId(),
                                            userAccountList1.stream().filter(account -> !existingProjectMembersList.contains(account)).map(accountIdUserIdMap::get).distinct().collect(Collectors.toList())
                                    );
                                    if (!groupAndUsersDTO.getUserIds().isEmpty()) {
                                        groupAndUsersDTOList.add(groupAndUsersDTO);
                                        System.out.println("Members not in project : " + project.getProjectName() + " : " + project.getProjectId() + " are " + gson.toJson(groupAndUsersDTO));
                                    }
                                }
                            }
                        }
                    });

                    if (!groupAndUsersDTOList.isEmpty()) {
                        for(GroupAndUsersDTO dto : groupAndUsersDTOList) {
                            HttpEntity<Object> requestEntityGroup = new HttpEntity<>(dto, headers);
                            try {
                                ResponseEntity<RestRespWithData<ConversationGroup>> groupResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.addUsersToGroup, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<>() {
                                });
                                ConversationGroup savedGroup = Objects.requireNonNull(groupResponse.getBody()).getData();
                                System.out.println("Members added in group : " + dto.getGroupId() + " --- " + gson.toJson(savedGroup));
                            } catch (Exception e) {
                                System.out.println("Exception in adding members in group : " + gson.toJson(dto) + " ------- " + e.getMessage());
                            }
                        }
                    }
                    if (!exclusiveOrgs.isEmpty()) {
                        System.out.println("Orgs not presents in Convo DB: " + gson.toJson(exclusiveOrgs));
                        exclusiveOrgs.remove(0L);
                        Set<Long> orgSet = new HashSet<>(exclusiveOrgs);
                        Map<Long, List<Long>> orgAccountIdMap = userAccountList.stream().filter(account -> orgSet.contains(account.getOrgId()))
                                .collect(Collectors.groupingBy(UserAccount::getOrgId, Collectors.mapping(account -> account.getFkUserId().getUserId(), Collectors.toList())));
                        orgAccountIdMap.forEach((orgId, userIds) -> {
                            Organization org = allOrgsMap.get(orgId);
                            ConversationGroup conversationGroup = ConversationGroup.builder()
                                    .name(org.getOrganizationName())
                                    .description(org.getOrganizationDisplayName())
                                    .entityId(orgId)
                                    .entityTypeId((long) Constants.EntityTypes.ORG)
                                    .orgId(orgId)
                                    .type("SYSTEM_ORG")
                                    .isActive(org.getIsDisabled())
                                    .usersDto(userIds)
                                    .groupIconCode("DEFAULT")
                                    .groupIconColor("DARK_GRAY")
                                    .createdDate(LocalDateTime.now())
                                    .build();

                            HttpEntity<Object> requestEntityGroup = new HttpEntity<>(conversationGroup, headers);
                            try {
                                ResponseEntity<RestRespWithData<ConversationGroup>> groupResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.createGroup, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<>() {
                                });
                                ConversationGroup savedGroup = Objects.requireNonNull(groupResponse.getBody()).getData();
                                System.out.println("New Org Group added: " + gson.toJson(savedGroup));
                            } catch (Exception e) {
                                System.out.println("Exception in adding Org : " + gson.toJson(conversationGroup) + " ------- " + e.getMessage());
                            }
                        });
                    }

                    if (!exclusiveTeams.isEmpty()) {
                        System.out.println("Teams not presents in Convo DB: " + gson.toJson(exclusiveTeams));
                        List<AccessDomain> accessDomainList = accessDomainRepository.findByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, exclusiveTeams, true);
                        Map<Long, List<Long>> teamAccountIdMap = accessDomainList.stream()
                                .collect(Collectors.groupingBy(AccessDomain::getEntityId, Collectors.mapping(accessDomain -> accessDomain.getUserAccount().getFkUserId().getUserId(), Collectors.toList())));
                        teamAccountIdMap.forEach((teamId, userAccountIds) -> {
                            Team team = allTeamMap.get(teamId);
                            ConversationGroup conversationGroup = ConversationGroup.builder()
                                    .name(team.getTeamName())
                                    .description(team.getTeamDesc())
                                    .entityId(teamId)
                                    .entityTypeId((long) Constants.EntityTypes.TEAM)
                                    .orgId(team.getFkOrgId().getOrgId())
                                    .type("SYSTEM_TEAM")
                                    .isActive(true)
                                    .usersDto(userAccountIds)
                                    .groupIconCode("DEFAULT")
                                    .groupIconColor("DARK_GRAY")
                                    .createdDate(LocalDateTime.now())
                                    .build();
                            HttpEntity<Object> requestEntityGroup = new HttpEntity<>(conversationGroup, headers);
                            try {
                                ResponseEntity<RestRespWithData<ConversationGroup>> groupResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.createGroup, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<>() {
                                });
                                ConversationGroup savedGroup = Objects.requireNonNull(groupResponse.getBody()).getData();
                                System.out.println("New Team Group added: " + gson.toJson(savedGroup));
                            } catch (Exception e) {
                                System.out.println("Exception in adding Team : " + gson.toJson(conversationGroup) + " ------- " + e.getMessage());
                            }
                        });
                    }

                    if (!exclusiveProjects.isEmpty()) {
                        System.out.println("projects not presents in Convo DB: " + gson.toJson(exclusiveProjects));
                        exclusiveProjects.remove(0L);
                        List<AccessDomain> accessDomainList = accessDomainRepository.findByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.PROJECT, exclusiveProjects, true);
                        Map<Long, List<Long>> projectAccountIdMap = accessDomainList.stream()
                                .collect(Collectors.groupingBy(AccessDomain::getEntityId, Collectors.mapping(accessDomain -> accessDomain.getUserAccount().getFkUserId().getUserId(), Collectors.toList())));
                        projectAccountIdMap.forEach((projectId, userAccountIds) -> {
                            Project project = allProjectMap.get(projectId);
                            if (project.getProjectName().equals(com.tse.core_application.constants.Constants.PROJECT_NAME)) {
                                return;
                            }
                            ConversationGroup conversationGroup = ConversationGroup.builder()
                                    .name(project.getProjectName())
                                    .description(project.getProjectDesc())
                                    .entityId(projectId)
                                    .entityTypeId((long) Constants.EntityTypes.PROJECT)
                                    .orgId(project.getOrgId())
                                    .type("SYSTEM_PROJ")
                                    .isActive(true)
                                    .usersDto(userAccountIds)
                                    .groupIconCode("DEFAULT")
                                    .groupIconColor("DARK_GRAY")
                                    .createdDate(LocalDateTime.now())
                                    .build();
                            HttpEntity<Object> requestEntityGroup = new HttpEntity<>(conversationGroup, headers);
                            try {
                                ResponseEntity<RestRespWithData<ConversationGroup>> groupResponse = restTemplate.exchange(conversationBaseUrl + ControllerConstants.Conversations.createGroup, HttpMethod.POST, requestEntityGroup, new ParameterizedTypeReference<>() {
                                });
                                ConversationGroup savedGroup = Objects.requireNonNull(groupResponse.getBody()).getData();
                                System.out.println("New Project Group added: " + gson.toJson(savedGroup));
                            } catch (Exception e) {
                                System.out.println("Exception in adding Project : " + gson.toJson(conversationGroup) + " ------- " + e.getMessage());
                            }
                        });
                    }
                }
                return "Migration Done successfully";
            }


        } catch (Exception e) {
            System.out.println("Exception: " + e.getLocalizedMessage());
            throw e;
        }

        return "Migration Done successfully";
    }
}
