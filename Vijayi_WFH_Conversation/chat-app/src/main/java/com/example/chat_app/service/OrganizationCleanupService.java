package com.example.chat_app.service;

import com.example.chat_app.model.Group;
import com.example.chat_app.model.GroupUser;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationCleanupService {

    private static final Logger logger = LogManager.getLogger(OrganizationCleanupService.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupUserRepository groupUserRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageUserRepository messageUserRepository;

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FavouriteChatsRepository favouriteChatsRepository;

    @Autowired
    private PinnedChatsRepository pinnedChatsRepository;

    @Autowired
    private UserGroupEventRepository userGroupEventRepository;

    @Transactional
    public void deactivateAccounts(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            logger.info("No accounts to deactivate");
            return;
        }

        logger.info("Deactivating {} accounts in chat database", accountIds.size());

        List<User> users = userRepository.findByAccountIdIn(accountIds);
        for (User user : users) {
            user.setIsActive(false);
        }
        userRepository.saveAll(users);

        List<GroupUser> groupUsers = groupUserRepository.findAllByAccountIdIn(accountIds);
        for (GroupUser groupUser : groupUsers) {
            groupUser.setIsDeleted(true);
        }
        groupUserRepository.saveAll(groupUsers);

        logger.info("Deactivated {} users and {} group memberships", users.size(), groupUsers.size());
    }

    @Transactional
    public void reactivateAccounts(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            logger.info("No accounts to reactivate");
            return;
        }

        logger.info("Reactivating {} accounts in chat database", accountIds.size());

        List<User> users = userRepository.findByAccountIdIn(accountIds);
        for (User user : users) {
            user.setIsActive(true);
        }
        userRepository.saveAll(users);

        List<GroupUser> groupUsers = groupUserRepository.findAllByAccountIdIn(accountIds);
        for (GroupUser groupUser : groupUsers) {
            groupUser.setIsDeleted(false);
        }
        groupUserRepository.saveAll(groupUsers);

        logger.info("Reactivated {} users and {} group memberships", users.size(), groupUsers.size());
    }

    @Transactional
    public void deleteOrganizationData(Long orgId, List<Long> accountIds) {
        logger.info("Starting deletion of chat data for organization: {}", orgId);

        List<Group> orgGroups = groupRepository.findByOrgId(orgId);
        List<Long> groupIds = orgGroups.stream().map(Group::getGroupId).collect(Collectors.toList());

        logger.info("Found {} groups and {} accounts to delete for org {}", groupIds.size(), accountIds.size(), orgId);

        if (!groupIds.isEmpty()) {
            deleteGroupRelatedData(groupIds);
        }

        if (!accountIds.isEmpty()) {
            deleteUserRelatedData(accountIds);
        }

        if (!orgGroups.isEmpty()) {
            groupRepository.deleteAll(orgGroups);
            logger.info("Deleted {} groups", orgGroups.size());
        }

        List<User> orgUsers = userRepository.findByAccountIdIn(accountIds);
        if (!orgUsers.isEmpty()) {
            userRepository.deleteAll(orgUsers);
            logger.info("Deleted {} users", orgUsers.size());
        }

        logger.info("Completed deletion of chat data for organization: {}", orgId);
    }

    private void deleteGroupRelatedData(List<Long> groupIds) {
        logger.info("Deleting data related to {} groups", groupIds.size());

        messageUserRepository.deleteByGroupIdIn(groupIds);

        messageAttachmentRepository.deleteByGroupIdIn(groupIds);

        messageRepository.deleteByGroupIdIn(groupIds);

        groupUserRepository.deleteByGroupIdIn(groupIds);

        historyRepository.deleteByGroupIdIn(groupIds);

        tagRepository.deleteByGroupIdIn(groupIds);

        favouriteChatsRepository.deleteByGroupIdIn(groupIds);

        pinnedChatsRepository.deleteByGroupIdIn(groupIds);

        userGroupEventRepository.deleteByGroupIdIn(groupIds);

        logger.info("Deleted all group-related data for {} groups", groupIds.size());
    }

    private void deleteUserRelatedData(List<Long> accountIds) {
        logger.info("Deleting data related to {} accounts", accountIds.size());

        messageUserRepository.deleteByUserIdIn(accountIds);

        favouriteChatsRepository.deleteByUserIdIn(accountIds);

        pinnedChatsRepository.deleteByUserIdIn(accountIds);

        userGroupEventRepository.deleteByAccountIdIn(accountIds);

        messageRepository.deleteBySenderIdInOrReceiverIdIn(accountIds, accountIds);

        logger.info("Deleted all user-related data for {} accounts", accountIds.size());
    }
}
