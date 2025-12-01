package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupResponseDto {
    private Long groupId;
    private String name;
    private String description;
    private Long orgId;
    private String lastMessage;
    private Long lastMessageSenderAccountId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTimestamp;
    private List<UserDto> users;
    private String type;
    private Long entityId;
    private Long entityTypeId;
    private Boolean isActive;
    private String groupIconCode;
    private String groupIconColor;
    private UserDto createdByAccountId;
    private LocalDateTime createdDate;

}
