package com.example.chat_app.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ChatNotificationPayload {

    private String accountId;

    private String userId;

    private String notificationId;

    private String notificationType;

    private String categoryId;

    private String createdDateTime;

    private String title;

    private String body;

    private String scrollTo;

    private String teamId;

    private String entityId;

    private String entityTypeId;

    private String groupId;

    private String orgId;

    private String senderAccountId;
}
