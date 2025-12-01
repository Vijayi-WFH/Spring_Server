package com.example.chat_app.service;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.constants.Constants.SystemAlertType;
import com.example.chat_app.dto.ChatNotificationPayload;
import com.example.chat_app.dto.ChatPayloadDto;
import com.example.chat_app.dto.WebSocketUrlHeaders;
import com.example.chat_app.exception.NotFoundException;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.model.Group;
import com.example.chat_app.model.GroupUser;
import com.example.chat_app.model.Message;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.GroupRepository;
import com.example.chat_app.repository.GroupUserRepository;
import com.example.chat_app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.ValidationException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FCMClient {

    private static final Logger log = LogManager.getLogger(FCMClient.class);
    @Autowired
    UserRepository userRepository;

    @Autowired
    GroupUserRepository groupUserRepository;

    @Autowired
    GroupRepository groupRepository;

    RestTemplate restTemplate = new RestTemplate();

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tseserver.application.root.path}")
    private String baseUrl;

    public ResponseEntity<Object> sendMessageNotification(List<HashMap<String, String>> payloadList, WebSocketUrlHeaders wsUrlHeaders){
        try {

            if(payloadList!=null && !payloadList.isEmpty()){
                ChatPayloadDto payloadDto = new ChatPayloadDto(payloadList);
                String url = baseUrl + Constants.TseServerAPI.NOTIFICATION_API;

                HttpHeaders headers = new HttpHeaders();

                headers.add("Authorization", wsUrlHeaders.getAuthorization());
                headers.add("accountIds", wsUrlHeaders.getAccountIds());
                headers.add("timeZone", wsUrlHeaders.getTimeZone()!=null && !wsUrlHeaders.getTimeZone().isEmpty() ? wsUrlHeaders.getTimeZone() : "Asia/Calcutta");
                headers.add("userId", wsUrlHeaders.getUserId().toString());
                headers.add("screenName", "Conversation");

                System.out.println(payloadList +"-------- headers ------"+ headers);
                HttpEntity<ChatPayloadDto> requestEntity = new HttpEntity<>(payloadDto, headers);

                ResponseEntity<Object> notificationResponse = restTemplate.exchange(url , HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
                });
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, notificationResponse.getBody());
            }
        } catch (Exception e) {
            log.error("Error while sending Notification via FCM:  for accountIds={}, requestURI={}", ThreadContext.get("accountIds"), ThreadContext.get("requestURI"), e);
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SERVER_ERROR, "Payload is null or Empty");
    }

    public void formatPayloadForDirectMessages(Message message, Long receiverUserId, WebSocketUrlHeaders wsUrlHeaders) {

        List<User> receiverUser = userRepository.findByUserId(receiverUserId);
        User senderUser = userRepository.findByAccountId(message.getSenderId());
        String name = senderUser.getFirstName() + " " + (senderUser.getLastName() != null ? senderUser.getLastName() : "");
        String content = "";
        if (message.getIsDeleted() && (message.getContent() == null || message.getContent().isEmpty())) {
            content = Constants.NotificationMessageContants.DELETED_MESSAGE;
        } else if ((message.getMessageAttachmentIds() != null && !message.getMessageAttachmentIds().isEmpty()) &&
                (message.getContent() == null || message.getContent().isEmpty())) {
            content = Constants.NotificationMessageContants.ATTACHMENT_CONTENT;
        } else {
            content = Jsoup.parse(message.getContent()).text();
            if (content.length() >= 150)
                content = content.substring(0, 150) + "...";
        }

        ChatNotificationPayload payload = ChatNotificationPayload.builder()
                .accountId(receiverUser.get(0).getAccountId().toString())
                .title(Constants.NotificationMessageContants.DM_TITLE + name)
                .body(content)
                .userId(receiverUserId.toString())
                .createdDateTime(convertServerDateToUserTimezoneWithSeconds(LocalDateTime.now(), wsUrlHeaders.getTimeZone()).toString())
                .orgId(senderUser.getOrgId().toString())
                .entityTypeId(Constants.ChatEntityTypes.USER)
                .entityId(message.getSenderId().toString())
                .notificationType(Constants.NotificationType.CHAT_MESSAGE_ALERTS)
                .senderAccountId(message.getSenderId().toString())
                .categoryId(Constants.ChatEntityTypes.CONVERSATION_CATEGORY_ID)
                .scrollTo("").teamId("").groupId("")
                .notificationId(message.getMessageId().toString())
                .build();

        HashMap<String, String> payloadMap = objectMapper.convertValue(payload, HashMap.class);
        sendMessageNotification(List.of(payloadMap), wsUrlHeaders);
    }

    public void formatPayloadForGroupMessages(Message message, Long groupId, WebSocketUrlHeaders wsUrlHeaders, Set<Long> mentionsIds) {

        Group group = groupRepository.findByGroupId(groupId).orElseThrow(() -> new ValidationException("Group is not Valid!"));
        List<Long> receiverAccountIdList = groupUserRepository.findAllAccountIdByGroupIdAndIsDeleted(groupId, false);
        receiverAccountIdList.remove(message.getSenderId());
        User senderUser = userRepository.findByAccountId(message.getSenderId());
        String name = senderUser.getFirstName() + " " + (senderUser.getLastName() != null ? senderUser.getLastName() : "");
        List<HashMap<String, String>> payloadList = new ArrayList<>();

        String content;
        if (message.getIsDeleted() && (message.getContent() == null || message.getContent().isEmpty())) {
            content = Constants.NotificationMessageContants.DELETED_MESSAGE;
        } else if ((message.getMessageAttachmentIds() != null && !message.getMessageAttachmentIds().isEmpty()) &&
                (message.getContent() == null || message.getContent().isEmpty()))
            content = name + ": " + Constants.NotificationMessageContants.ATTACHMENT_CONTENT;
        else {
            String parsedContent = Jsoup.parse(message.getContent()).text();
            content = name + ": " + parsedContent.substring(0, Math.min(parsedContent.length(), 150)) + (parsedContent.length() >= 150 ? "..." : "");
        }
        for (Long accountId : receiverAccountIdList) {
            //for mentioned users in a group notification message would be different
            String modifiedString ="";
            if (message.getContent()!=null && mentionsIds!=null && !mentionsIds.isEmpty() && mentionsIds.contains(accountId)) {
                modifiedString = content.replaceFirst(": ", " Mentioned You: ");
            }

            ChatNotificationPayload payload = ChatNotificationPayload.builder()
                    .accountId(accountId.toString())
                    .title(Constants.NotificationMessageContants.GROUP_TITLE + group.getName())
                    .body(modifiedString.isEmpty() ? content : modifiedString)
                    .createdDateTime(convertServerDateToUserTimezoneWithSeconds(LocalDateTime.now(), wsUrlHeaders.getTimeZone()).toString())
                    .orgId(group.getOrgId().toString())
                    .groupId(groupId.toString())
                    .notificationType(Constants.NotificationType.CHAT_MESSAGE_ALERTS)
                    .categoryId(Constants.ChatEntityTypes.CONVERSATION_CATEGORY_ID)
                    .senderAccountId(message.getSenderId().toString())
                    .entityId(groupId.toString())
                    .entityTypeId(Constants.ChatEntityTypes.GROUP)
                    .notificationId(message.getMessageId().toString())
                    .scrollTo("").teamId("").userId("")
                    .build();

            HashMap<String, String> payloadMap = objectMapper.convertValue(payload, HashMap.class);
            payloadList.add(payloadMap);
        }
        sendMessageNotification(payloadList, wsUrlHeaders);
    }

    public LocalDateTime convertServerDateToUserTimezoneWithSeconds(LocalDateTime serverDateToConvert, String localTimeZone) {
        if (serverDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnn");
        ZoneId serverTimeZone = ZoneId.of(String.valueOf(ZoneId.systemDefault()));
        String stringDate = serverDateToConvert.format(timeFormatter);
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, timeFormatter);
        ZonedDateTime zonedDateTimeInServerTimeZone = ZonedDateTime.of(localDateTime, serverTimeZone);
        ZonedDateTime zonedDateTimeInLocalTimeZone = zonedDateTimeInServerTimeZone.withZoneSameInstant(ZoneId.of(localTimeZone));
        LocalDateTime localDateTimeInLocalTimeZone = LocalDateTime.from(zonedDateTimeInLocalTimeZone);
        LocalDate onlyLocalDate = localDateTimeInLocalTimeZone.toLocalDate();
        LocalTime onlyLocalTime = localDateTimeInLocalTimeZone.toLocalTime();
        return LocalDateTime.of(onlyLocalDate, onlyLocalTime);
    }

    public void sendAddRemoveUserInGroupNotification(List<GroupUser> newGroupUsers, List<Long> accountIds, String alertType, WebSocketUrlHeaders urlHeaders) {
        List<Long> receiverAccountIdList = newGroupUsers.parallelStream().map(groupUser -> groupUser.getUser().getAccountId()).collect(Collectors.toList());
        Group group = newGroupUsers.get(0).getGroup();
        String name;
        if(!accountIds.contains(0L)){
            User senderUser = userRepository.findByAccountId(accountIds.get(0));
            name = senderUser.getFirstName() + " " + (senderUser.getLastName() != null ? senderUser.getLastName() : "");
            urlHeaders.setUserId(senderUser.getUserId());
        } else {
            name = "System Admin";
            urlHeaders.setUserId(0L);
        }
        List<HashMap<String, String>> payloadList = new ArrayList<>();
        String body;

        switch (alertType){
            case SystemAlertType.ADD:
                body = name + Constants.NotificationMessageContants.ADD_USER_CONTENT + group.getName();
                break;
            case SystemAlertType.REMOVE:
                body = name + Constants.NotificationMessageContants.REMOVE_USER_CONTENT + group.getName();
                break;
            case SystemAlertType.SET_ADMIN:
                body = name + Constants.NotificationMessageContants.SET_ADMIN_CONTENT + group.getName();
                break;
            case SystemAlertType.REMOVE_ADMIN:
                body = name + Constants.NotificationMessageContants.REMOVE_ADMIN_CONTENT;
                break;
            default:
                throw new NotFoundException("Alert type is missing while sending the CHAT_SYSTEM_ALERTS");
        }

        for (Long accountId : receiverAccountIdList) {
            ChatNotificationPayload payload = ChatNotificationPayload.builder()
                    .accountId(accountId.toString())
                    .title(group.getName())
                    .body(body)
                    .createdDateTime(convertServerDateToUserTimezoneWithSeconds(LocalDateTime.now(), "Asia/Calcutta").toString())
                    .orgId(group.getOrgId().toString())
                    .groupId(group.getGroupId().toString())
                    .notificationType(Constants.NotificationType.CHAT_SYSTEM_ALERTS)
                    .categoryId(Constants.ChatEntityTypes.CONVERSATION_CATEGORY_ID)
                    .senderAccountId(accountIds.get(0).toString())
                    .entityId(group.getGroupId().toString())
                    .entityTypeId(Constants.ChatEntityTypes.GROUP)
                    .notificationId(group.getGroupId().toString())
                    .scrollTo("").teamId("").userId("")
                    .build();
            HashMap<String, String> payloadMap = objectMapper.convertValue(payload, HashMap.class);
            payloadList.add(payloadMap);
        }
        sendMessageNotification(payloadList, urlHeaders);
    }
}
