package com.example.chat_app.dto;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.model.Group;
import com.example.chat_app.model.GroupUser;
import com.example.chat_app.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String entityName;
    private Long entityType;
    private Long entityId;
    private Boolean isActive;

    private Long orgId;
    private String lastMessage;
    private Long lastMessageSenderAccountId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTimestamp;

    private UserDto user;

    private Boolean isPinned;
    private Boolean isFavourite;

    private Boolean isMentioned;
    private String groupIconColor;
    private String groupIconCode;
    private Integer unreadMessageCount;
//    private List<Long> unreadMessageIds;

    public ChatResponse(long accountId, Object firstName, long orgId, boolean isActive, Object lastName, Object lastMessage, long lastMessageSenderAccountId, long lastMessageId,
                        LocalDateTime lastMessageTimestamp, Long userId, Object email) {
        this.entityId = accountId;
        this.entityType = Constants.ChatTypes.USER;
        this.entityName = (firstName != null) ? firstName.toString() + ((lastName != null) ? (" " + lastName.toString()) : "") : ((lastName != null) ? lastName.toString() : "");
        this.orgId = orgId;
        this.isActive = isActive;
        this.lastMessage = (lastMessage != null) ? lastMessage.toString() : null;
        this.lastMessageSenderAccountId = lastMessageSenderAccountId;
        this.lastMessageId = lastMessageId;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.user = new UserDto(accountId, userId, (String) firstName, (String) lastName, orgId, isActive, (String) email);
    }
    public ChatResponse(Group group) {
        this.entityId = group.getGroupId();
        this.entityType = Constants.ChatTypes.valueOf(group.getType());
        this.entityName = group.getName();
        this.orgId = group.getOrgId();
        this.lastMessage = group.getLastMessage();
        this.lastMessageId = group.getLastMessageId();
        this.lastMessageTimestamp = group.getLastMessageTimestamp();
        this.lastMessageSenderAccountId = group.getLastMessageSenderAccountId();
        User userDb = group.getGroupUsers().stream()
                .map(GroupUser::getUser)
                .filter(groupUserUser -> Objects.equals(groupUserUser.getAccountId(), lastMessageSenderAccountId))
                .findFirst()
                .orElse(null);
        UserDto userdto = new UserDto();
        if (userDb!=null) {
            BeanUtils.copyProperties(userDb, userdto);
            this.setUser(userdto);
        } else {
            this.setUser(null);
        }

    }
}
