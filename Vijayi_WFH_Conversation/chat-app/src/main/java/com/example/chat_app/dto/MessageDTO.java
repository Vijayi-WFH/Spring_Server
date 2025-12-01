package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private String content;
    private Long accountId;

    public MessageDTO(Long receiverId, Long senderId, Long groupId){
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
    }

    public MessageDTO(Long messageId, Long senderId, Long receiverId, Long groupId, Long accountId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.accountId = accountId;
    }
}
