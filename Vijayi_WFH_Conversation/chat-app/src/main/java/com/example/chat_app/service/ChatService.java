package com.example.chat_app.service;

import com.example.chat_app.config.NewDataEncryptionConverter;
import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.ChatResponse;
import com.example.chat_app.dto.MessageDTO;
import com.example.chat_app.exception.UnauthorizedLoginException;
import com.example.chat_app.model.*;
import com.example.chat_app.repository.*;
import com.example.chat_app.utils.DateTimeUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ChatService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private PinnedChatsRepository pinnedChatsRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private FavouriteChatsRepository favouriteChatsRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Value("${pinnedChatsLimit}")
    private Long pinnedChatsLimit;

    @Autowired
    private NewDataEncryptionConverter encryptionConverter;

    @Autowired
    private UserGroupEventRepository userGroupEventRepository;

    @Autowired
    private MessageUserRepository messageUserRepository;

    public List<ChatResponse> getChatResponses(String query, List<Long> accountIds, String timeZone){
        List<Long> orgIds = userRepository.findOrgIdByAccountIdIn(accountIds);

        List<ChatResponse> usersChatResponses;

        if(query!=null && !query.isEmpty())
            usersChatResponses = userRepository.findUserChatResponses(orgIds, accountIds, query);
        else
            usersChatResponses = userRepository.findUserChatResponses(orgIds, accountIds);

        if (usersChatResponses != null && !usersChatResponses.isEmpty()) {
            usersChatResponses.forEach(chatResponse -> {
                if (chatResponse != null && chatResponse.getLastMessage() != null && !chatResponse.getLastMessage().isEmpty()
                        && !chatResponse.getLastMessage().equalsIgnoreCase(Constants.NotificationMessageContants.DELETED_MESSAGE)) {
                    String dmContent = (String) encryptionConverter.convertToEntityAttribute(chatResponse.getLastMessage());
                    dmContent = dmContent != null ? Jsoup.parse(dmContent).text() : "";
                    chatResponse.setLastMessage(dmContent.length() > 200 ? dmContent.substring(0, 200) + "..." : dmContent);
                }
            });
        }
        User user = userRepository.findFirstByAccountIdInAndIsActive(accountIds, true);

        List<Group> groups = groupService.getGroupsByUserAndOrgIds(user.getUserId(), orgIds, query);

        removeUnrelatedPersonalOrgUser(groups, usersChatResponses);

        List<ChatResponse> groupChatResponses = groups.stream()
                .map(group -> {
                    ChatResponse chatResponse = new ChatResponse(group);

                    boolean isMentioned = group.getMentionedUsers().stream()
                            .map(User::getAccountId)
                            .anyMatch(accountIds::contains);
                    chatResponse.setIsMentioned(isMentioned);
                    boolean isActive = (group.getIsActive()!=null && group.getIsActive()) && (group.getGroupUsers()!=null && group.getGroupUsers().stream().noneMatch(groupUser ->
                            accountIds.contains(groupUser.getUser().getAccountId()) && groupUser.getIsDeleted()));
                    chatResponse.setIsActive(isActive); //if a group is deleted or when that member is removed from the Group.
                    if(!isActive) {
                        setLastMessageForInactiveMembers(chatResponse, group.getGroupId(), accountIds);
                    }
                    chatResponse.setGroupIconColor(group.getGroupIconColor());
                    chatResponse.setGroupIconCode(group.getGroupIconCode());
                    return chatResponse;
                })
                .collect(Collectors.toList());

        //fetch all the messageUser which are no-delivered and non-read
        setUnreadMessagesCount(usersChatResponses, groupChatResponses, accountIds);

        List<ChatResponse> combinedList = new ArrayList<>();

        combinedList = Stream.concat(usersChatResponses.parallelStream(), groupChatResponses.parallelStream())
                .peek(chatResponse -> {
                    if(chatResponse.getLastMessage()!=null && chatResponse.getLastMessage().isEmpty() && chatResponse.getLastMessageId()!=null && chatResponse.getLastMessageTimestamp()!=null){
                        chatResponse.setLastMessage("Attachment"); // for case when Attachment has sent but with no messageContent.
                    }
                })
                .sorted(Comparator
                        .comparing((ChatResponse u) -> u.getLastMessage() == null || u.getLastMessage().isEmpty()) // Non-empty messages first
                        .thenComparing(u -> {
                            if (u.getLastMessage() != null && !u.getLastMessage().isEmpty()) {
                                return u.getLastMessageTimestamp(); // if non-empty then sort based on timestamp.
                            }
                            return null;
                        }, Comparator.nullsLast(Comparator.reverseOrder())) // Reverse chronological for non-empty
                        .thenComparing(ChatResponse::getIsActive, Comparator.reverseOrder()) // Active first for Empty message
                        .thenComparing(ChatResponse::getEntityName)) // Alphabetical sorting for all non-empty message entity
                .collect(Collectors.toList());

        combinedList.forEach(chatResponse -> {
            if (chatResponse.getEntityType() != null && chatResponse.getEntityType().equals(1L)) {
                // Check if this user chat is pinned
                PinnedChats pinnedChats = pinnedChatsRepository.findByAccountIdInAndChatTypeIdAndChatId(accountIds, Constants.PinAndFavouriteChatTypes.USER, chatResponse.getEntityId());
                if (pinnedChats != null)
                    chatResponse.setIsPinned(true);

                // Check if this user chat is favourited
                FavouriteChats favouriteChats = favouriteChatsRepository.findByAccountIdInAndChatTypeIdAndChatId(accountIds, Constants.PinAndFavouriteChatTypes.USER, chatResponse.getEntityId());
                if (favouriteChats != null)
                    chatResponse.setIsFavourite(true);

            }
            else if (chatResponse.getLastMessageSenderAccountId() != null) {
                // Check if this group chat is pinned
                PinnedChats pinnedChats = pinnedChatsRepository.findByAccountIdInAndChatTypeIdAndChatId(accountIds, Constants.PinAndFavouriteChatTypes.GROUP, chatResponse.getEntityId());
                if (pinnedChats != null)
                    chatResponse.setIsPinned(true);

                // Check if this group chat is favourited
                FavouriteChats favouriteChats = favouriteChatsRepository.findByAccountIdInAndChatTypeIdAndChatId(accountIds, Constants.PinAndFavouriteChatTypes.GROUP, chatResponse.getEntityId());
                if (favouriteChats != null)
                    chatResponse.setIsFavourite(true);
            }
            chatResponse.setLastMessageTimestamp(DateTimeUtils.convertServerDateToUserTimezone(chatResponse.getLastMessageTimestamp(), timeZone));
        });



        return combinedList;
    }

    public String pinChat(Long requesterAccountId, Long chatTypeId, Long chatId) {

        User requesterUser = userRepository.findByAccountId(requesterAccountId);

        if(Objects.equals(chatTypeId, Constants.PinAndFavouriteChatTypes.USER)) {
            User toPinUser = userRepository.findByAccountId(chatId);

            if (!verifyOrg(requesterUser, toPinUser))
                throw new UnauthorizedLoginException("Invalid accountIds provided.");
        }
        else if(Objects.equals(chatTypeId, Constants.PinAndFavouriteChatTypes.GROUP)) {
            Group group = groupRepository.findGroupWithUsers(chatId);
            if (group.getGroupUsers().stream().noneMatch(groupUser -> groupUser.getUser().equals(requesterUser))) {
                throw new UnauthorizedLoginException("User is not part of this group.");
            }
        }
        else throw new UnauthorizedLoginException("Invalid chatTypeId provided.");

        List<PinnedChats> pinnedChats = pinnedChatsRepository.findByAccountIdAndChatTypeIdAndChatId(requesterAccountId, chatTypeId, chatId);

        boolean exists = pinnedChats.stream()
                .anyMatch(pinnedChat -> pinnedChat.getAccountId().equals(requesterAccountId) && pinnedChat.getChatTypeId().equals(chatTypeId)&&pinnedChat.getChatId().equals(chatId));

        if (exists)
            throw new UnauthorizedLoginException("This chat is already pinned.");

        if(Objects.equals(pinnedChats.size(), pinnedChatsLimit))
            throw new UnauthorizedLoginException("Cannot pin more than " + pinnedChatsLimit +" chats.");

        PinnedChats pinChat = new PinnedChats();

        pinChat.setAccountId(requesterAccountId);
        pinChat.setChatTypeId(chatTypeId);
        pinChat.setChatId(chatId);
        pinChat = pinnedChatsRepository.save(pinChat);

        if(pinChat!=null)
            return "Pinned chat " + chatId + ".";
        return "Could not pin chat";
    }

    public String unpinChat(Long requesterAccountId, Long chatTypeId, Long chatId) {
        List<PinnedChats> pinnedChats = pinnedChatsRepository.findByAccountIdAndChatTypeIdAndChatId(requesterAccountId, chatTypeId, chatId);
        if (pinnedChats.size() == 1) {
            pinnedChatsRepository.delete(pinnedChats.get(0));
            return "Successfully unpinned.";
        }
        return "Could not unpin.";
    }

    public String favouriteChat(Long requesterAccountId, Long chatTypeId, Long chatId) {

        User requesterUser = userRepository.findByAccountId(requesterAccountId);

        // Check if the chat is a user chat
        if (Objects.equals(chatTypeId, Constants.PinAndFavouriteChatTypes.USER)) {
            User toFavouriteUser = userRepository.findByAccountId(chatId);
            if (!verifyOrg(requesterUser, toFavouriteUser))
                throw new UnauthorizedLoginException("Invalid accountIds provided.");
        }
        // Check if the chat is a group chat
        else if (Objects.equals(chatTypeId, Constants.PinAndFavouriteChatTypes.GROUP)) {
            Group group = groupRepository.findGroupWithUsers(chatId);
            if (group.getGroupUsers().stream().noneMatch(groupUser -> groupUser.getUser().equals(requesterUser))) {
                throw new UnauthorizedLoginException("User is not part of this group.");
            }
        }
        else {
            throw new UnauthorizedLoginException("Invalid chatTypeId provided.");
        }

        // Check if the chat is already favourited
        List<FavouriteChats> favouriteChats = favouriteChatsRepository.findByAccountIdAndChatTypeIdAndChatId(requesterAccountId, chatTypeId, chatId);

        boolean exists = favouriteChats.stream()
                .anyMatch(favouriteChat -> favouriteChat.getAccountId().equals(requesterAccountId) &&
                        favouriteChat.getChatTypeId().equals(chatTypeId) &&
                        favouriteChat.getChatId().equals(chatId));

        if (exists)
            throw new UnauthorizedLoginException("This chat is already favourited.");

        // Create and save the FavouriteChats entity
        FavouriteChats favouriteChat = new FavouriteChats();
        favouriteChat.setAccountId(requesterAccountId);
        favouriteChat.setChatTypeId(chatTypeId);
        favouriteChat.setChatId(chatId);
        favouriteChat = favouriteChatsRepository.save(favouriteChat);

        if (favouriteChat != null)
            return "Favourited chat " + chatId + ".";

        return "Could not favourite chat.";
    }

    public String unfavouriteChat(Long requesterAccountId, Long chatTypeId, Long chatId) {

        // Find if the chat is already favourited
        List<FavouriteChats> favouriteChats = favouriteChatsRepository.findByAccountIdAndChatTypeIdAndChatId(requesterAccountId, chatTypeId, chatId);

        if (favouriteChats.size() == 1) {
            // Remove the favourite chat entry
            favouriteChatsRepository.delete(favouriteChats.get(0));
            return "Successfully unfavourited.";
        }

        return "Could not unfavourite.";
    }

    public Boolean verifyOrg(User user1, User user2) {
        if (user1 != null && user2 != null && Objects.equals(user1.getOrgId(), user2.getOrgId()))
            return true;
        return false;
    }

    private void setUnreadMessagesCount(List<ChatResponse> usersChatResponses, List<ChatResponse> groupChatResponses, List<Long> accountIds){

        List<MessageDTO> messageDTOList = messageUserRepository.findUnreadMessagesByAccountIdIn(false, accountIds);
        Map<Long, Integer> groupUnreadCountMap = new HashMap<>();
        Map<Long, Integer> directUnreadCountMap = new HashMap<>();
        messageDTOList.forEach(messageDTO -> {
            if(messageDTO.getGroupId() != null && messageDTO.getGroupId() > 0) {
                groupUnreadCountMap.put(messageDTO.getGroupId(), groupUnreadCountMap.getOrDefault(messageDTO.getGroupId(), 0) + 1);
            } else {
                directUnreadCountMap.put(messageDTO.getSenderId(), directUnreadCountMap.getOrDefault(messageDTO.getSenderId(), 0) + 1);
            }
        });

        for(ChatResponse chatResponse : usersChatResponses){
            chatResponse.setUnreadMessageCount(directUnreadCountMap.getOrDefault(chatResponse.getEntityId(), 0));
        }

        for(ChatResponse chatResponse : groupChatResponses) {
            chatResponse.setUnreadMessageCount(groupUnreadCountMap.getOrDefault(chatResponse.getEntityId(), 0));
        }
    }

    private void removeUnrelatedPersonalOrgUser(List<Group> groups, List<ChatResponse> usersChatResponses){
        if(usersChatResponses!= null & groups != null) {
            List<ChatResponse> userWithPersonalOrg = usersChatResponses.stream().filter(chatResponse -> chatResponse.getOrgId().equals(0L)).collect(Collectors.toList());
            List<Long> personalTeamAccountIdList = groups.stream().filter(group -> group.getOrgId().equals(0L))
                    .map(Group::getGroupUsers)
                    .flatMap(groupUsers -> groupUsers.stream().map(groupUser -> groupUser.getUser().getAccountId()))
                    .collect(Collectors.toList());

            List<ChatResponse> unrelatedTeamMembers = userWithPersonalOrg.stream().filter(chatResponse ->
                    !personalTeamAccountIdList.contains(chatResponse.getEntityId())).collect(Collectors.toList());

            usersChatResponses.removeAll(unrelatedTeamMembers);
        }

    }

    private void setLastMessageForInactiveMembers(ChatResponse chatResponse, Long groupId, List<Long> accountIds) {
        UserGroupEvent userGroupEvent = userGroupEventRepository.findLastLeaveEventByGroupAndAccountId(groupId, accountIds);
        if (userGroupEvent != null) {
            chatResponse.setLastMessage(Constants.MessageStrings.RECENT_INACTIVE_GROUP_MESSAGE);
            chatResponse.setLastMessageTimestamp(LocalDateTime.ofInstant(userGroupEvent.getOccurredAt(), ZoneId.systemDefault()));
        }
    }
}
