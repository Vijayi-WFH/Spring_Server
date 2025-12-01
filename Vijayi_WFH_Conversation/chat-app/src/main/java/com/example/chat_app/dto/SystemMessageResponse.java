package com.example.chat_app.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMessageResponse {

    private List<Long> messageIds;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Long groupId;

}