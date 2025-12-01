package com.example.chat_app.service;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.ActivateDeactivateUserRequest;
import com.example.chat_app.dto.UserDto;
import com.example.chat_app.dto.WebSocketUrlHeaders;
import com.example.chat_app.model.GroupUser;
import com.example.chat_app.model.User;
import com.example.chat_app.model.UserGroupEvent;
import com.example.chat_app.repository.GroupUserRepository;
import com.example.chat_app.repository.UserGroupEventRepository;
import com.example.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupUserRepository groupUserRepository;

    @Autowired
    private UserGroupEventRepository userGroupEventRepository;

    @Autowired
    private FCMClient fcmClient;

    public User createUser(User user) {
        Optional<User> userDb = userRepository.findByUserIdAndOrgId(user.getUserId(), user.getOrgId());
        if(userDb.isEmpty()){
            user.setIsActive(true);
            return userRepository.save(user);
        } else {
            userDb.get().setIsActive(true);
            return userRepository.save(userDb.get());
        }
    }

    @Transactional
    public String deleteUser(Long accountId){
        User user = userRepository.findByAccountId(accountId);
        if(user != null){
            //had to create a Leave entry into the user_group_event table.
            List<GroupUser> groupUserList = new ArrayList<>();
            for (GroupUser groupUser : user.getGroups()) {
                groupUser.setIsDeleted(true);
                groupUser.setIsAdmin(false);
                groupUserList.add(groupUser);
            }
            if (!groupUserList.isEmpty()) {
                List<UserGroupEvent> userGroupEvents = groupUserList.parallelStream().map(groupUser ->
                        new UserGroupEvent(accountId, groupUser.getGroup().getGroupId(), Constants.EventType.LEAVE.getValue(), Instant.now())
                ).collect(Collectors.toList());

                if (!userGroupEvents.isEmpty()) {
                    userGroupEventRepository.saveAll(userGroupEvents);
                }
                groupUserRepository.saveAll(groupUserList);
            }
            user.setIsDeleted(true);
            user.setIsActive(false);
            userRepository.save(user);
            return "User Deleted Successfully";
        }
        return "No matched User to Delete";
    }

    public List<User> getUserById(Long userId) {
        return userRepository.findByUserId(userId);
    }

    public String changeUserName(Long accountId, String firstName, String middleName, String lastName) {
        User user = userRepository.findByAccountId(accountId);
        if(user!=null){
            if(firstName!=null) user.setFirstName(firstName);
            if(lastName!=null) user.setLastName(lastName);
            if(middleName!=null && !middleName.isBlank()) user.setMiddleName(middleName);
            userRepository.save(user);
            return "Username updated successfully";
        }
        return "User Not Found";
    }

    @Transactional
    public String activateDeactivateUserFromGroups(ActivateDeactivateUserRequest userRequest, WebSocketUrlHeaders urlHeaders){
        List<Long> accountIds = userRequest.getAccountIds();
        Boolean isToDeactivate = userRequest.getIsToDeactivate();
        List<UserGroupEvent> userGroupEvents;
        if(!accountIds.isEmpty()){
            List<User> userList = userRepository.findByAccountIdIn(accountIds);
            List<GroupUser> groupUserList = groupUserRepository.findAllByAccountIdIn(accountIds);

            if(isToDeactivate){
                for (User user : userList){
                    user.setIsActive(false);
                }
                for(GroupUser groupUser: groupUserList){
                    groupUser.setIsAdmin(false);
                    groupUser.setIsDeleted(true);
                }
                userGroupEvents = groupUserList.parallelStream().map(groupUser ->
                        new UserGroupEvent(groupUser.getUser().getAccountId(), groupUser.getGroup().getGroupId(), Constants.EventType.LEAVE.getValue(), Instant.now()))
                        .collect(Collectors.toList());
            }
            else {
                for (User user : userList){
                    user.setIsActive(true);
                }
                for(GroupUser groupUser: groupUserList){
                    groupUser.setIsDeleted(false);
                }
                userGroupEvents = groupUserList.parallelStream().map(groupUser ->
                                new UserGroupEvent(groupUser.getUser().getAccountId(), groupUser.getGroup().getGroupId(), Constants.EventType.JOIN.getValue(), Instant.now()))
                        .collect(Collectors.toList());
            }
            if(!userList.isEmpty() && !groupUserList.isEmpty() && !userGroupEvents.isEmpty()){
                userRepository.saveAll(userList);
                groupUserRepository.saveAll(groupUserList);
                userGroupEventRepository.saveAll(userGroupEvents);
                fcmClient.sendAddRemoveUserInGroupNotification(groupUserList, accountIds, isToDeactivate ? Constants.SystemAlertType.REMOVE : Constants.SystemAlertType.ADD, urlHeaders);
                return "User "+ (isToDeactivate ? "Deactivated": "Activated") + " Successfully";
            }
        }
        return "No matched User to Delete";
    }

    public List<UserDto> findAllUsers(List<Long> accountIds){
        List<User> users;
        if(!accountIds.contains(0L)) {
            List<Long> orgIds = userRepository.findOrgIdByAccountIdIn(accountIds);
            users = userRepository.findDistinctByOrgIdIn(orgIds);
        } else {
            users = userRepository.findAll();
        }
        return users.stream().map(user -> UserDto.builder()
                .email(user.getEmail())
                .accountId(user.getAccountId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .orgId(user.getOrgId())
                .isActive(user.getIsActive())
                .isAdmin(user.getIsAdmin())
                .userId(user.getUserId())
                .isDeleted(user.getIsDeleted())
                .isOrgAdmin(user.getIsOrgAdmin())
                .build()).collect(Collectors.toList());
    }
}
