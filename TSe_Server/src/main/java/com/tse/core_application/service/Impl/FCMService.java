package com.tse.core_application.service.Impl;

import com.google.firebase.messaging.*;
import com.tse.core_application.constants.NotificationTypeToCategory;
import com.tse.core_application.dto.PushNotificationRequest;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class FCMService {

//    private final FirebaseMessaging firebaseMessaging;
//
//    public FCMService(FirebaseMessaging firebaseMessaging) {
//        this.firebaseMessaging = firebaseMessaging;
//    }

    public String sendMessageToToken(PushNotificationRequest pushNotificationRequest) throws FirebaseMessagingException {

        Notification notification = Notification
                .builder()
                .setTitle(pushNotificationRequest.getPayload().get("title"))
                .setBody(pushNotificationRequest.getPayload().get("body"))
                .build();

//        Message message = Message
//                .builder()
//                .setToken(pushNotificationRequest.getTargetToken())
//                .setNotification(notification)
//                .build();
        boolean isNotifiyForConversation = pushNotificationRequest.getPayload().get("categoryId").equals(String.valueOf(NotificationTypeToCategory.CONVERSATION.getCategoryId()));
        String threadId = isNotifiyForConversation ? pushNotificationRequest.getPayload().get("entityTypeId") + "_" + pushNotificationRequest.getPayload().get("entityId")
                : pushNotificationRequest.getPayload().get("categoryId");

        // id would be for DM/group messages -> entityTypeId_entityId_messageId and for alerts -> groupId and for every other except conversation its notificationId
        String apnsCollapseId = isNotifiyForConversation ? threadId + "_" + pushNotificationRequest.getPayload().get("notificationId") : pushNotificationRequest.getPayload().get("notificationId");
        boolean isSystemAlerts = pushNotificationRequest.getPayload().get("notificationType").equals("CHAT_SYSTEM_ALERTS");
        String randomId = UUID.randomUUID().toString();

        Message message = Message
                .builder()
                .setToken(pushNotificationRequest.getTargetToken().getToken())
                .setNotification(Objects.equals(pushNotificationRequest.getDeviceType(), "iOS") ? notification : null)
                .setApnsConfig(
                        ApnsConfig.builder()
                                .putHeader("apns-collapse-id", isSystemAlerts ? randomId : apnsCollapseId)
                                .putHeader("apns-id", isSystemAlerts ? randomId : apnsCollapseId)
                                .setAps(Aps.builder()
                                                .setContentAvailable(pushNotificationRequest.getPayload().get("categoryId").equals(String.valueOf(NotificationTypeToCategory.SILENT_UPDATES.getCategoryId())))
                                                .setMutableContent(isNotifiyForConversation)
                                        .setThreadId(threadId)
                                        .build())
                                .build()
                )
                .putAllData(pushNotificationRequest.getPayload())
                .build();

//        return FirebaseMessaging.getInstance().send(message);
       FirebaseMessaging.getInstance().sendAsync(message);
       return "Success";
    }
}
