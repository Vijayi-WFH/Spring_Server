package com.example.chat_app.service;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.*;
import com.example.chat_app.exception.*;
import com.example.chat_app.exception.IllegalStateException;
import com.example.chat_app.exception.NullPointerException;
import com.example.chat_app.model.*;
import com.example.chat_app.repository.*;
import com.example.chat_app.utils.DateTimeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.chat_app.constants.Constants.MessageStrings.INACCESSIBLE_GROUP_MESSAGE;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupUserRepository groupUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private FCMClient fcmClient;

    @Autowired
    private UserGroupEventRepository userGroupEventRepository;

    @Autowired
    private MessageRepository messageRepository;

    // Create a new group
    @Transactional
    public GroupResponseDto createGroup(GroupDTO groupDTO, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        Group group = new Group();
        Integer existingName = groupRepository.findGroupByName(groupDTO.getName());
        if(existingName!=null && existingName>1){
//            groupDTO.setName(groupDTO.getName() + "_" + (groupDTO.getEntityId() != null ? groupDTO.getEntityId() : groupDTO.getOrgId()));
            throw new ValidationFailedException("Found other Group with same name. Please try with different name");
        }
        BeanUtils.copyProperties(groupDTO, group);

        group.setGroupId(null);
        group.setGroupIconCode(Constants.GroupIconEnum.isIconPresent(groupDTO.getGroupIconCode()) ? groupDTO.getGroupIconCode() : Constants.GroupIconEnum.DEFAULT.getIconName());
        group.setGroupIconColor(Constants.GroupColorEnum.isColorPresent(groupDTO.getGroupIconColor()) ? groupDTO.getGroupIconColor() : Constants.GroupColorEnum.DARK_GRAY.getValue());
        group.setCreatedDate(LocalDateTime.now());
        group.setIsActive(true);

        if(!accountIds.contains(0L)) {
            group.setType("CUSTOM");
        }
        List<Long> userIds = groupDTO.getUsers();

        GroupResponseDto groupResDto = new GroupResponseDto();
        if (accountIds.size() != 1) {
            throw new UnauthorizedLoginException("Please provide exactly one accountId.");
        }

        if (Objects.equals(group.getType(), "CUSTOM")) {
            User user = userRepository.findByAccountIdAndIsActive(accountIds.get(0), true);
            group.setCreatedByAccountId(user.getAccountId());
            group.setOrgId(user.getOrgId());
            groupRepository.save(group);

            GroupUserId groupUserId = new GroupUserId(group.getGroupId(), accountIds.get(0));
            GroupUser groupUser = new GroupUser();
            groupUser.setId(groupUserId);
            groupUser.setGroup(group);
            groupUser.setUser(user);
            groupUser.setIsAdmin(true); // Set the user as an admin
            groupUser.setIsDeleted(false); // Set the user as not deleted

            if (group.getGroupUsers() == null)
                group.setGroupUsers(new ArrayList<>());
            group.getGroupUsers().add(groupUser);

            groupUserRepository.save(groupUser);
        }
        group = groupRepository.save(group);
        formatResponseToGroupDto(group, groupResDto);

        if(userIds!=null && !userIds.isEmpty()){
            groupResDto = bulkAddUsersToGroup(group.getGroupId(), userIds, accountIds, urlHeaders);
        }
        return groupResDto;
    }

    // Add a user to a group
    @Transactional
    public GroupResponseDto addUserToGroup(Long groupId, Long userId, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
        GroupResponseDto groupResponseDto = new GroupResponseDto();
        Optional<User> userOpt = Optional.empty();
        if (groupOpt.isPresent()) {
            if (!Objects.equals(accountIds.get(0), 0L) && (Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.ORG_DEFAULT) || groupOpt.get().getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())))
                throw new UnauthorizedLoginException("User is not an admin of this group.");
            userOpt = userRepository.findByUserIdAndOrgId(userId, groupOpt.get().getOrgId());
        }

        if (groupOpt.isPresent() && userOpt.isPresent()) {
            Group group = groupOpt.get();
            User user = userOpt.get();
            GroupUser deletedGroupUser =group.getGroupUsers().stream().filter(groupUser -> groupUser.getUser().getUserId().equals(userId) && groupUser.getIsDeleted()).findFirst().orElse(null);

            if (!Objects.equals(user.getOrgId(), group.getOrgId()))
                throw new UnauthorizedLoginException("Users should be from the same organisation.");

            if(Boolean.FALSE.equals(group.getIsActive())) throw new ValidationFailedException("Adding members is restricted in Inactive Group");

            if(!group.getType().equals(Constants.GroupTypes.CUSTOM) && !urlHeaders.getScreenName().equals(Constants.TseServerAPI.TSE_SCREEN_NAME)){
                throw new ValidationFailedException("Explicitly adding members in System Groups is not allowed");
            }

            if (group.getGroupUsers().stream().noneMatch(groupUser -> groupUser.getId().getAccountId().equals(user.getAccountId()) &&
                    !groupUser.getIsDeleted())) {
                GroupUser groupUser = new GroupUser();
                GroupUserId groupUserId = new GroupUserId(group.getGroupId(), user.getAccountId());
                groupUser.setId(groupUserId);
                groupUser.setGroup(group);
                groupUser.setUser(user);
                groupUser.setIsAdmin(false);
                groupUser.setIsDeleted(false);

                groupUserRepository.save(groupUser);
                userGroupEventRepository.save(new UserGroupEvent(user.getAccountId(), group.getGroupId(), Constants.EventType.JOIN.getValue(), Instant.now()));
                group.getGroupUsers().add(groupUser);
                fcmClient.sendAddRemoveUserInGroupNotification(List.of(groupUser), accountIds, Constants.SystemAlertType.ADD, urlHeaders);
            } else if (deletedGroupUser!=null){
                deletedGroupUser.setIsDeleted(false);
                groupUserRepository.save(deletedGroupUser);
                userGroupEventRepository.save(new UserGroupEvent(deletedGroupUser.getUser().getAccountId(), deletedGroupUser.getGroup().getGroupId(), Constants.EventType.JOIN.getValue(), Instant.now()));
                fcmClient.sendAddRemoveUserInGroupNotification(List.of(deletedGroupUser), accountIds, Constants.SystemAlertType.ADD, urlHeaders);
            }

            else
                throw new UnauthorizedLoginException("User is already part of this group.");
            group = groupRepository.save(group);
            formatResponseToGroupDto(group, groupResponseDto);
            return groupResponseDto;
        } else {
            throw new UnauthorizedLoginException("User or Group does not exist.");
        }
    }

    @Transactional
    public GroupResponseDto bulkAddUsersToGroup(Long groupId, List<Long> userIds, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);

        List<User> users = List.of();
        if (groupOpt.isPresent()) {
            if (!Objects.equals(accountIds.get(0), 0L) && (Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.ORG_DEFAULT) || groupOpt.get().getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())))
                throw new UnauthorizedLoginException("User is not an admin of this group.");
            users = userRepository.findByUserIdInAndOrgId(userIds, groupOpt.get().getOrgId());
        }

        if (groupOpt.isPresent() && !users.isEmpty()) {
            Group group = groupOpt.get();
            GroupResponseDto groupDto = new GroupResponseDto();
            if(group.getGroupUsers()==null) {
                group.setGroupUsers(new ArrayList<>());
            }
            if(Boolean.FALSE.equals(group.getIsActive())) throw new ValidationFailedException("Adding members is restricted in Inactive Group");

            if(!group.getType().equals(Constants.GroupTypes.CUSTOM) && !urlHeaders.getScreenName().equals(Constants.TseServerAPI.TSE_SCREEN_NAME)){
                throw new ValidationFailedException("Explicitly Adding members into System Groups is not allowed");
            }

            Set<Long> existingUserIds = group.getGroupUsers().stream()
                    .map(groupUser -> groupUser.getUser().getUserId())
                    .collect(Collectors.toSet());
            List<UserGroupEvent> userGroupEvents = new ArrayList<>();
            //if user is already deleted and adding again back to Group.
            List<GroupUser> deletedUsers = new ArrayList<>();
            List<Long> mutualUserIds = existingUserIds.stream().filter(userIds::contains).collect(Collectors.toList());
            if (!mutualUserIds.isEmpty()) {
                deletedUsers = group.getGroupUsers().stream().filter(groupUser -> mutualUserIds.contains(groupUser.getUser().getUserId())
                        && groupUser.getIsDeleted()).collect(Collectors.toList());
                deletedUsers.forEach(groupUser -> {
                    groupUser.setIsDeleted(false);
                    UserGroupEvent userGroupEvent = new UserGroupEvent(groupUser.getUser().getAccountId(), groupId, Constants.EventType.JOIN.getValue(), Instant.now());
                    userGroupEvents.add(userGroupEvent);
                });
//                fcmClient.sendAddRemoveUserInGroupNotification(deletedUsers, accountIds, Constants.SystemAlertType.ADD, urlHeaders);
            }
            List<GroupUser> newGroupUsers = users.stream()
                    .filter(user -> !existingUserIds.contains(user.getUserId()) && user.getIsActive())
                    .map(user -> {
                        UserGroupEvent userGroupEvent = new UserGroupEvent(user.getAccountId(), groupId, Constants.EventType.JOIN.getValue(), Instant.now());
                        GroupUser groupUser = new GroupUser();
                        GroupUserId groupUserId = new GroupUserId(group.getGroupId(), user.getAccountId());
                        groupUser.setId(groupUserId);
                        groupUser.setGroup(group);
                        groupUser.setUser(user);
                        groupUser.setIsAdmin(existingUserIds.isEmpty());
                        groupUser.setIsDeleted(false);
                        userGroupEvents.add(userGroupEvent);
                        return groupUser;
                    })
                    .collect(Collectors.toList());
            if(!newGroupUsers.isEmpty()) {
                group.getGroupUsers().addAll(newGroupUsers);
                groupUserRepository.saveAll(newGroupUsers);
                groupRepository.save(group);
                fcmClient.sendAddRemoveUserInGroupNotification(newGroupUsers, accountIds, Constants.SystemAlertType.ADD, urlHeaders);
            }
            if(!newGroupUsers.isEmpty() || !deletedUsers.isEmpty()) {
                newGroupUsers.addAll(deletedUsers);
                userGroupEventRepository.saveAll(userGroupEvents);
            }
            formatResponseToGroupDto(group, groupDto);
            return groupDto;
        } else {
            throw new IllegalStateException("Users or Group does not exist.");
        }
    }

    @Transactional
    public GroupResponseDto removeUserFromGroup(Long groupId, Long userId, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        // Retrieve the group and user by their IDs
        Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
        Optional<User> userOpt = Optional.empty();
        Group group = groupOpt.get();
        GroupResponseDto groupDto = new GroupResponseDto();

        if (groupOpt.isPresent()) {
            if (!Objects.equals(accountIds.get(0), 0L) && (Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.ORG_DEFAULT)
                    || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.PROJ_DEFAULT) || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.TEAM_DEFAULT)
                    || groupOpt.get().getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin()))) {
                throw new UnauthorizedActionException("User is not an admin of this group OR User trying to modify System Groups Directly");
            }
        }
        userOpt = userRepository.findByUserIdAndOrgId(userId, groupOpt.get().getOrgId());

        if(Boolean.FALSE.equals(group.getIsActive())) throw new ValidationFailedException("Removing members is restricted in Inactive Group");

        if (!userOpt.isPresent()) {
            throw new UnauthorizedLoginException("User not found");
        }
        User user = userOpt.get();
        if(!group.getType().equals(Constants.GroupTypes.CUSTOM) && user.getIsOrgAdmin()){
            throw new ValidationFailedException("Org Admin can't be remove from the system group");
        }

        if(!group.getType().equals(Constants.GroupTypes.CUSTOM) && !urlHeaders.getScreenName().equals(Constants.TseServerAPI.TSE_SCREEN_NAME)){
            throw new ValidationFailedException("Explicitly removing members from System Groups is not allowed");
        }
        Optional<GroupUser> groupUserOpt = groupUserRepository.findByAccountIdAndGroupId(user.getAccountId(), groupId);
        GroupUser groupUser = groupUserOpt.orElse(null);
        // Check if both the group and the user exist
        if (!groupOpt.isPresent() || groupUser==null) {
            throw new UnauthorizedLoginException("Group not found");
        }
        if(groupUser!=null && groupUser.getIsDeleted()){
            throw new IllegalStateException("User is already deleted from the group");
        }

        assignAdminOnRemovingUser(group, List.of(user.getAccountId()));
        groupUser.setIsAdmin(false);
        groupUser.setIsDeleted(true);
        groupUserRepository.save(groupUser);
        userGroupEventRepository.save(new UserGroupEvent(user.getAccountId(), group.getGroupId(), Constants.EventType.LEAVE.getValue(), Instant.now()));
        fcmClient.sendAddRemoveUserInGroupNotification(List.of(groupUser), accountIds, Constants.SystemAlertType.REMOVE, urlHeaders);
        formatResponseToGroupDto(group, groupDto);
        return groupDto;
    }

    @Transactional
    public GroupResponseDto bulkRemoveUsersFromGroup(Long groupId, List<Long> userId, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        // Retrieve the group and user by their IDs
        Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
        List<User> users;
        GroupResponseDto groupDto = new GroupResponseDto();
        if (groupOpt.isPresent())
            if (!Objects.equals(accountIds.get(0), 0L) && (Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.ORG_DEFAULT)
                    || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.PROJ_DEFAULT) || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.TEAM_DEFAULT)
                    || groupOpt.get().getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())))
                throw new UnauthorizedActionException("User is not an admin of this group OR User trying to modify System Groups Directly");

        Group group = groupOpt.get();
        if(Boolean.FALSE.equals(group.getIsActive())) throw new ValidationFailedException("Removing members is restricted in Inactive Group");

        users = userRepository.findByUserIdInAndOrgId(userId, groupOpt.get().getOrgId());
        if (!groupOpt.isPresent()) {
            throw new NotFoundException("Group not found");
        }
        if (users.isEmpty() || (!group.getType().equals(Constants.GroupTypes.CUSTOM) && users.stream().anyMatch(User::getIsOrgAdmin))) {
            throw new ValidationFailedException("OrgAdmin can't be removed from System groups or User not found");
        }
        if(!group.getType().equals(Constants.GroupTypes.CUSTOM) && !urlHeaders.getScreenName().equals(Constants.TseServerAPI.TSE_SCREEN_NAME)){
            throw new ValidationFailedException("Explicitly removing members from System Groups is not allowed");
        }

        List<Long> accountIdToDelete = users.stream().map(User::getAccountId).collect(Collectors.toList());

        List<GroupUser> groupUser = groupUserRepository.findByAccountIdInAndGroupId(accountIdToDelete, groupId);
        if(!groupUser.isEmpty() && groupUser.stream().anyMatch(grpUser -> grpUser.getIsDeleted().equals(Boolean.TRUE))){
            throw new IllegalStateException("One of the requested User is already Deleted from the group");
        }
        assignAdminOnRemovingUser(group, accountIdToDelete);

        groupUserRepository.markUsersAsDeletedInGroup(accountIdToDelete, groupId);
        userGroupEventRepository.saveAll(accountIdToDelete.stream().map(accountId -> new UserGroupEvent(accountId, groupId, Constants.EventType.LEAVE.getValue(), Instant.now())).collect(Collectors.toList()));
        if (!groupUser.isEmpty()) {
            fcmClient.sendAddRemoveUserInGroupNotification(groupUser, accountIds, Constants.SystemAlertType.REMOVE, urlHeaders);
        }
        formatResponseToGroupDto(group, groupDto);
        return groupDto;
    }

    public List<GroupResponseDto> getGroupsByUser(Long userId) {
        // Fetch the user by ID
        List<User> users = userService.getUserById(userId);
        List<GroupResponseDto> groupDtoList = new ArrayList<>();
        // Retrieve all groups for the user
        List<Group> groupDb = groupRepository.findByUsersInOrderByLastMessageTimestampDescGroupIdDesc(users);
        List<Group> groupList = new ArrayList<>();
        groupDb.forEach(group -> {
            GroupResponseDto responseDto = new GroupResponseDto();
            formatResponseToGroupDto(group, responseDto);
            groupDtoList.add(responseDto);
        });
        return groupDtoList;
    }

    public GroupResponseDto setAdminForGroup(Long groupId, Long userId, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
        GroupResponseDto groupDto = new GroupResponseDto();
        Optional<User> userOpt = Optional.empty();

        if (groupOpt.isPresent()) {
            if(Boolean.FALSE.equals(groupOpt.get().getIsActive())) throw new ValidationFailedException("Changing roles is restricted in Inactive Group");

            if(!groupOpt.get().getType().equals(Constants.GroupTypes.CUSTOM) && !urlHeaders.getScreenName().equals(Constants.TseServerAPI.TSE_SCREEN_NAME)){
                throw new ValidationFailedException("Explicitly modifying members role in System Groups is not allowed");
            }

            if (!Objects.equals(accountIds.get(0), 0L) && (Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.ORG_DEFAULT)
                    || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.PROJ_DEFAULT) || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.TEAM_DEFAULT)
                    || groupOpt.get().getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())))
                throw new UnauthorizedActionException("User is not an admin of this group.");

            userOpt = userRepository.findByUserIdAndOrgId(userId, groupOpt.get().getOrgId());
        }

        if (groupOpt.isPresent() && userOpt.isPresent()) {
            Group group = groupOpt.get();
            User user = userOpt.get();
            if(groupOpt.get().getGroupUsers().stream().anyMatch(groupUsr -> groupUsr.getUser().getAccountId().equals(user.getAccountId()) && (groupUsr.getIsDeleted() || groupUsr.getIsAdmin()))){
                throw new ValidationFailedException("User is already removed from Admin role or is already deleted from Group");
            }
            if (!Objects.equals(user.getOrgId(), group.getOrgId()))
                throw new UnauthorizedActionException("Users should be from the same organisation.");

            // Check if the user is part of the group
            Optional<GroupUser> groupUserOpt = group.getGroupUsers().stream()
                    .filter(groupUser -> groupUser.getUser().getAccountId().equals(user.getAccountId()))
                    .findFirst(); // Find the first matching GroupUser where the user is part of the group

            // If the user is in the group and is not already an admin, make them an admin
            if (groupUserOpt.isPresent()) {
                GroupUser groupUser = groupUserOpt.get();

                if (!groupUser.getIsAdmin()) {
                    groupUser.setIsAdmin(true);

                    groupUserRepository.save(groupUser);
                    groupRepository.save(group);
                }
            }
            else
                throw new UnauthorizedActionException("User requested to be made admin is not part of this group.");
            groupRepository.save(group);
            fcmClient.sendAddRemoveUserInGroupNotification(List.of(groupUserOpt.get()), accountIds, Constants.SystemAlertType.SET_ADMIN, urlHeaders);
            formatResponseToGroupDto(group, groupDto);
            return groupDto;
        } else {
            throw new NotFoundException("User or Group does not exist.");
        }
    }

    public GroupResponseDto removeAdminForGroup(Long groupId, Long userId, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
        GroupResponseDto groupDto = new GroupResponseDto();
        Optional<User> userOpt = Optional.empty();

        if (groupOpt.isPresent()) {
            if(Boolean.FALSE.equals(groupOpt.get().getIsActive())) throw new ValidationFailedException("Changing roles is restricted in Inactive Group");

            if (!Objects.equals(accountIds.get(0), 0L) && (Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.ORG_DEFAULT)
                    || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.PROJ_DEFAULT) || Objects.equals(groupOpt.get().getType(), Constants.GroupTypes.TEAM_DEFAULT)
                    || groupOpt.get().getGroupUsers().stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())))
                throw new UnauthorizedActionException("User is not an admin of this group.");
            userOpt = userRepository.findByUserIdAndOrgId(userId, groupOpt.get().getOrgId());
        }

        if (groupOpt.isPresent() && userOpt.isPresent()) {
            Group group = groupOpt.get();
            User user = userOpt.get();

            if (!Objects.equals(user.getOrgId(), group.getOrgId()))
                throw new UnauthorizedActionException("Users should be from the same organisation.");

            if(!group.getType().equals(Constants.GroupTypes.CUSTOM) && user.getIsOrgAdmin()){
                throw new ValidationFailedException("Org Admin can't be remove from Admin role in system groups.");
            }

            if(!groupOpt.get().getType().equals(Constants.GroupTypes.CUSTOM) && !urlHeaders.getScreenName().equals(Constants.TseServerAPI.TSE_SCREEN_NAME)){
                throw new ValidationFailedException("Explicitly modifying members role in System Groups is not allowed");
            }

            Optional<GroupUser> groupUserOpt = group.getGroupUsers().stream()
                    .filter(groupUser -> groupUser.getUser().getAccountId().equals(user.getAccountId()) && groupUser.getIsAdmin())
                    .findFirst();

            if (groupUserOpt.isPresent()) {
                GroupUser groupUser = groupUserOpt.get();
                long adminCount = group.getGroupUsers().stream()
                        .filter(GroupUser::getIsAdmin)
                        .count();

                if (adminCount == 1) {
                    throw new IllegalStateException("Cannot remove the only admin from the group.");
                }

                groupUser.setIsAdmin(false);

                groupUserRepository.save(groupUser);
                groupRepository.save(group);
                fcmClient.sendAddRemoveUserInGroupNotification(List.of(groupUserOpt.get()), accountIds, Constants.SystemAlertType.REMOVE_ADMIN, urlHeaders);
                formatResponseToGroupDto(group, groupDto);
            }
            else {
                throw new ValidationFailedException("User is already removed from the Admin role");
            }

            return groupDto;
        } else {
            throw new NotFoundException("User or Group does not exist.");
        }
    }

    public List<Group> getGroupsByUserAndOrgIds(Long userId, List<Long> orgIds, String query) {
        // Fetch the user by ID
        List<User> users = userService.getUserById(userId);

        // Retrieve all groups for the user
        if(query!=null && !query.isEmpty())
            return groupRepository.findByUsersInAndOrgIdInOrderByLastMessageTimestampDescGroupIdDesc(users, orgIds, query);
        return groupRepository.findByUserAndOrgIdOrderByLastMessageTimestampDescGroupIdDesc(userId, orgIds);
    }

    @Transactional
    public void addMentionsInGroup(Group group, Set<Long> accountIdsToAdd) {
        List<Long> existingMentions = entityManager.createNativeQuery(
                        "SELECT account_id FROM chat.user_mention WHERE group_id = :groupId")
                .setParameter("groupId", group.getGroupId())
                .getResultList();

        Set<Long> missingAccountIds = accountIdsToAdd.stream()
                .filter(accountId -> !existingMentions.contains(accountId))
                .collect(Collectors.toSet());

        for (Long accountId : missingAccountIds) {
            entityManager.createNativeQuery("INSERT INTO chat.user_mention (group_id, account_id) VALUES (:groupId, :accountId)")
                    .setParameter("groupId", group.getGroupId())
                    .setParameter("accountId", accountId)
                    .executeUpdate();
        }
    }

    @Transactional
    public void removeMentionsInGroup(GroupResponseDto group, Long accountIdToRemove) {
        if (accountIdToRemove != null) {
            entityManager.createNativeQuery("DELETE FROM chat.user_mention WHERE group_id = :groupId AND account_id = :accountId")
                    .setParameter("groupId", group.getGroupId())
                    .setParameter("accountId", accountIdToRemove)
                    .executeUpdate();
        }
    }

    private void assignAdminOnRemovingUser(Group group, List<Long> accountIdToDelete){
        if (group.getGroupUsers().stream().anyMatch(groupUser ->
                accountIdToDelete.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())) {
            List<Long> adminList = group.getGroupUsers().stream().map(groupUser ->
                            groupUser.getIsAdmin() ? groupUser.getUser().getAccountId() : 0)
                    .filter(val -> val != 0)
                    .collect(Collectors.toList());
            if (accountIdToDelete.containsAll(adminList)) {
                List<User> containedUser = group.getUsers().stream().filter(user ->
                        !accountIdToDelete.contains(user.getAccountId())).collect(Collectors.toList());
                if (!containedUser.isEmpty()) {
                    groupUserRepository.markUserAsAdminInGroup(containedUser.get(0).getAccountId(), group.getGroupId());
                }
            }
        }
    }

    public Group findGroupByEntityTypeAndEntityId(Long entityId, Long entityTypeId) {
        // only for SystemGroups as only they have entityTypeId and entityTypeId
        if(entityId!=null && entityTypeId!=null){
            return groupRepository.findTopByEntityTypeAndEntityTypeId(entityId, entityTypeId);
        }
        return null;
    }

    public Group updateGroupDetails(Long entityId, Long entityTypeId, GroupUpdateRequest groupRequest) {
        Group groupDb = findGroupByEntityTypeAndEntityId(entityId, entityTypeId);
        Group groupCopy = new Group();
        if(groupDb!=null){
            BeanUtils.copyProperties(groupDb, groupCopy);
            if(groupRequest.getGroupDesc()!=null){
                groupCopy.setName(groupRequest.getGroupName());
            }
            if(groupRequest.getGroupDesc()!=null){
                groupCopy.setDescription(groupRequest.getGroupDesc());
            }
            if(groupRequest.getIsActive()!=null){
                groupCopy.setIsActive(groupRequest.getIsActive());
            }
            return groupRepository.save(groupCopy);
        }
        return null;
    }

    @Transactional
    public GroupResponseDto editGroup(GroupDTO groupDTO, List<Long> accountIds){

        Optional<Group> groupDb = groupRepository.findByGroupId(groupDTO.getGroupId());
        Group updatedGroup = new Group();
        GroupResponseDto groupDto = new GroupResponseDto();

        Integer existingName = groupRepository.findGroupByName(groupDTO.getName());
        if(existingName!=null && existingName>1){
            throw new ValidationFailedException("Found other Group with same name. Please try with different name");
        }

        if(!(groupDb.isPresent() && groupDb.get().getGroupUsers().stream().anyMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsAdmin())))
            throw new UnauthorizedActionException("User is not an admin of this group.");
        else if (!Objects.equals(groupDb.get().getType(), Constants.GroupTypes.CUSTOM))
            throw new UnauthorizedActionException("User not allowed to edit default groups.");
        else {
            BeanUtils.copyProperties(groupDb.get(), updatedGroup);

            if (groupDTO.getIsActive()!=null && !Objects.equals(groupDTO.getIsActive(), updatedGroup.getIsActive())) {
                updatedGroup.setIsActive(groupDTO.getIsActive());
            }
            if (groupDTO.getGroupIconCode()!=null && !groupDTO.getGroupIconCode().equals(updatedGroup.getGroupIconCode())) {
                if(!Constants.GroupIconEnum.isIconPresent(groupDTO.getGroupIconCode())){
                    throw new IllegalStateException("Group Icon value is not valid");
                }
                updatedGroup.setGroupIconCode(groupDTO.getGroupIconCode());
            }
            if (groupDTO.getGroupIconColor()!=null && !groupDTO.getGroupIconColor().equals(updatedGroup.getGroupIconColor())) {
                if(!Constants.GroupColorEnum.isColorPresent(groupDTO.getGroupIconColor())){
                    throw new IllegalStateException("Group Color value is not valid");
                }
                updatedGroup.setGroupIconColor(groupDTO.getGroupIconColor());
            }
            if(groupDTO.getDescription()!=null && !groupDTO.getDescription().equals(updatedGroup.getDescription())){
                updatedGroup.setDescription(groupDTO.getDescription());
            }
            if(groupDTO.getName()!=null && !groupDTO.getName().equals(updatedGroup.getName())){
                updatedGroup.setName(groupDTO.getName());
            }
            groupRepository.save(updatedGroup);
            formatResponseToGroupDto(updatedGroup, groupDto);
            return groupDto;
        }
    }

    public GroupResponseDto getGroupByGroupId(Long groupId, List<Long> accountIds) {
        GroupResponseDto groupDto = new GroupResponseDto();
        if(groupId!=null){
            Group groupDb = groupRepository.findByGroupId(groupId).orElseThrow(() -> new NullPointerException("No Group found for this GroupID"));
            List<GroupUser> groupUsers = groupDb.getGroupUsers();
            if(groupUsers.stream().noneMatch(groupUser -> accountIds.contains(groupUser.getUser().getAccountId()))){
                throw new UnauthorizedActionException("User is not a part of this Group");
            }
            formatResponseToGroupDto(groupDb, groupDto);
            return groupDto;
        }
        return null;
    }

    private void formatResponseToGroupDto(Group group, GroupResponseDto groupDto){
        BeanUtils.copyProperties(group, groupDto);
        UserDto createdBy;
        if(group.getGroupUsers()!=null){
            List<UserDto> userDtoList = group.getGroupUsers().stream()
                    .map(groupUser -> {
                        UserDto userDto = new UserDto();
                        BeanUtils.copyProperties(groupUser.getUser(), userDto);
                        userDto.setIsDeleted(groupUser.getIsDeleted());
                        userDto.setIsAdmin(groupUser.getIsAdmin());
                        return userDto;
                    }).sorted(Comparator.comparingInt((UserDto userDto) ->
                                    (userDto.getIsActive() == null) ? 2 : (userDto.getIsActive() ? 0 : 1))
                            .thenComparingInt((UserDto userDto) ->
                                    (userDto.getIsAdmin() == null) ? 2 : (userDto.getIsAdmin() ? 0 : 1))
                            .thenComparing(UserDto::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(UserDto::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).collect(Collectors.toList());
            groupDto.setUsers(userDtoList);

            createdBy = userDtoList.stream() // Use the sorted list here as well if createdBy is among them
                    .filter(userDto -> userDto.getAccountId().equals(group.getCreatedByAccountId())).findFirst().orElse(null);
            groupDto.setCreatedByAccountId(createdBy);
        }
    }

    public List<GroupResponseDto> getAllGroupsData() {
        List<GroupResponseDto> groupDtoList = new ArrayList<>();
        List<Group> groupDb = groupRepository.findAll();
        groupDb.forEach(group -> {
            GroupResponseDto responseDto = new GroupResponseDto();
            formatResponseToGroupDto(group, responseDto);
            groupDtoList.add(responseDto);
        });
        return groupDtoList;
    }

    @Transactional
    public GroupResponseDto activeInactiveCustomGroup(ActiveInactiveGroupDto activeInactiveGroupDto, List<Long> accountIds, WebSocketUrlHeaders urlHeaders) {
        boolean isActive = activeInactiveGroupDto.getIsActive();
        Group groupDb = groupRepository.findByGroupId(activeInactiveGroupDto.getGroupId()).orElseThrow(() -> new NotFoundException("Group not found for requested groupId: " + activeInactiveGroupDto.getGroupId()));
        GroupResponseDto responseDto = new GroupResponseDto();
        BeanUtils.copyProperties(groupDb, responseDto);
        if (!groupDb.getType().equals(Constants.GroupTypes.CUSTOM)) {
            throw new UnauthorizedActionException("System Groups modification not allowed.");
        }
        List<Long> adminList = groupDb.getGroupUsers().stream().filter(groupUser -> !groupUser.getIsDeleted() && groupUser.getIsAdmin())
                .map(groupUser -> groupUser.getUser().getAccountId()).collect(Collectors.toList());
        if (accountIds.stream().noneMatch(adminList::contains)) {
            throw new UnauthorizedActionException("User is not an Admin, not allowed to Inactive the Group.");
        }
        if (isActive) {
            if (groupDb.getIsActive()) {
                responseDto.setLastMessage("Group is already active");
                return responseDto;
            }
            List<Message> messages = messageRepository.findByGroupIdOrderByTimestampDesc(activeInactiveGroupDto.getGroupId());
            if (!messages.isEmpty()) {
                Document doc = Jsoup.parse(messages.get(0).getContent());
                groupDb.setLastMessageTimestamp(messages.get(0).getTimestamp());
                groupDb.setLastMessage(doc.text().length() > 200 ? doc.text().substring(0, 200) + "..." : doc.text());
            } else {
                groupDb.setLastMessage(null);
                groupDb.setLastMessage(null);
            }
            groupDb.setIsActive(true);
            groupRepository.save(groupDb);
            BeanUtils.copyProperties(groupDb, responseDto);
            responseDto.setLastMessage("Successfully marked active");
            return responseDto;
        } else {
            if (!groupDb.getIsActive()) {
                responseDto.setLastMessage("Group is already in-active");
                return responseDto;
            }
            groupDb.setLastMessage(INACCESSIBLE_GROUP_MESSAGE);
            groupDb.setLastMessageTimestamp(LocalDateTime.now());
            groupDb.setIsActive(false);
            groupRepository.save(groupDb);
            BeanUtils.copyProperties(groupDb, responseDto);
            return responseDto;
        }
    }
}
