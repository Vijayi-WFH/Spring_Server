package com.tse.core_application.dto.conversations;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationGroup implements Serializable {

    private Long groupId;
    private String name;
    private String description;
    private String type;
    private List<ConversationUser> users;
    private List<Long> usersDto;
    private Long orgId;
    private List<ConversationUser> admins;
    private String lastMessage;
    private Long lastMessageSenderAccountId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTimestamp;
    private Long entityTypeId;
    private Long entityId;
    private Boolean isActive;
    private String groupIconCode;
    private String groupIconColor;
    private LocalDateTime createdDate;
}
