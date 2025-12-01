package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.constants.Constants.IndicatorStatus;
import com.example.chat_app.dto.*;
import com.example.chat_app.exception.NotFoundException;
import com.example.chat_app.exception.ValidationFailedException;
import com.example.chat_app.model.*;
import com.example.chat_app.repository.*;
import com.example.chat_app.service.FCMClient;
import com.example.chat_app.service.GroupService;
import com.example.chat_app.utils.DateTimeUtils;
import com.example.chat_app.utils.DeliveryAckScheduler;
import com.example.chat_app.utils.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.example.chat_app.constants.Constants.MessageStatusType.*;

@Service
public class WebSocketController extends TextWebSocketHandler {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageUserRepository messageUserRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupUserRepository groupUserRepository;

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private FCMClient fcmClient;

    @Autowired
    private MessageStatsRepository messageStatsRepository;

    @Autowired
    private DeliveryAckScheduler deliveryAckScheduler;

    @Value("${updateMessageTime}")
    private Long updateMessageTime;

    private final ConcurrentHashMap<Long, List<SessionsWithTimeZone>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, List<byte[]>> fileChunks = new ConcurrentHashMap<>();
    private final Map<Long, Long> orgs = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketUrlHeaders> urlHeadersMap = new ConcurrentHashMap<>();
    private final Map<Long, ActivityStatus> activityIndicatorMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String UPLOAD_DIR = "uploads"; // Directory to save uploaded files

    private static final Logger log = LogManager.getLogger(WebSocketController.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = Long.parseLong(getParamFromURI(session.getUri(), "userId"));
        WebSocketUrlHeaders urlHeaders = (WebSocketUrlHeaders) session.getAttributes().get("HEADERS_ATTRIBUTE");
        String timeZone = urlHeaders.getTimeZone();

        ActivityStatus activityStatus = new ActivityStatus(userId, IndicatorStatus.AVAILABLE.getIndicatorId(), IndicatorStatus.AVAILABLE.getIndicatorMessage(), "ONLINE");
        if(userSessions.containsKey(userId) && activityIndicatorMap.containsKey(userId)){
            activityStatus = activityIndicatorMap.get(userId);
        }
        if(timeZone==null)
            timeZone = session.getHandshakeHeaders().getFirst("timeZone");
        urlHeadersMap.put(userId, urlHeaders);

        SessionsWithTimeZone sessionsWithTimeZone = new SessionsWithTimeZone(session, timeZone);
        //If user is not existed then Compute (creating a new Concurrent list) otherwise leave and add the new Session into it.
        userSessions.computeIfAbsent(userId, val -> new CopyOnWriteArrayList<>()).add(sessionsWithTimeZone);
        System.out.println("Session added for user: " + userId + ", session: " + session.getId() + ", timeZone: " + timeZone);

        broadcastActivityStatusToOrgMember(activityStatus, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = Long.parseLong(getParamFromURI(session.getUri(), "userId"));

        // if user is inactive then removing its session. If all session for same user is inactive then removed from userSession List
        List<SessionsWithTimeZone> sessionsWithTimeZones = userSessions.get(userId);
        if(sessionsWithTimeZones!=null){
            sessionsWithTimeZones.removeIf(socket -> socket.getSession().equals(session));
            if(sessionsWithTimeZones.isEmpty()){
                userSessions.remove(userId);
                broadcastActivityStatusToOrgMember(new ActivityStatus(userId, IndicatorStatus.OFFLINE.getIndicatorId(), IndicatorStatus.OFFLINE.getIndicatorMessage(), "OFFLINE"), session); //ping only when every connection is being closed.
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, Exception {
        try {
            Long userId = Long.parseLong(getParamFromURI(session.getUri(), "userId"));

            String payload = message.getPayload();
            // Parse the JSON message
            JsonNode jsonNode = objectMapper.readTree(payload);
            String action = jsonNode.has("action") ? jsonNode.get("action").asText() : null;

            Long taskAttacmentId = null;

            if(action == null){
                session.sendMessage(message);
                return;
            }
            // Handle other message types
            handleChatMessage(jsonNode, userId, session, taskAttacmentId);
        } catch (Exception e) {
            log.error("Unexpected error for WebSockets: " + e);
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    @Transactional
    private void handleChatMessage(JsonNode jsonNode, Long userId, WebSocketSession session, Long taskAttachmentId) throws Exception {
        String action = jsonNode.has("action") ? jsonNode.get("action").asText() : "new";
        Long messageId = jsonNode.has("messageId") ? jsonNode.get("messageId").asLong() : null;
        String content = jsonNode.has("content") ? jsonNode.get("content").asText() : null;
        Long senderId = jsonNode.has("senderId") ? jsonNode.get("senderId").asLong() : null;
        Long receiverId = jsonNode.has("receiverId") ? jsonNode.get("receiverId").asLong() : null;
        Long groupId = jsonNode.has("groupId") ? jsonNode.get("groupId").asLong() : null;
        List<Long> attachmentIdList = jsonNode.has("attachmentIdList") && jsonNode.get("attachmentIdList").isArray() ? convertToList(jsonNode, "attachmentIdList") : null;
        Long replyId = jsonNode.has("replyId") ? jsonNode.get("replyId").asLong() : null;

        //manual ping/pong setup for client
        if (jsonNode.hasNonNull("type") && jsonNode.get("type").asText().equals("ping")) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        Message chatMessage = new Message();
        chatMessage.setSenderId(senderId);
        chatMessage.setReceiverId(receiverId != null && receiverId > 0 ? receiverId : null);
        chatMessage.setGroupId(groupId);
        chatMessage.setContent(content);
        chatMessage.setTimestamp(LocalDateTime.now());
//        chatMessage.setIsDelivered(true);
        chatMessage.setReplyId(replyId);

        if (attachmentIdList != null && !attachmentIdList.isEmpty()) {
            String commaSeparatedString = attachmentIdList.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            chatMessage.setMessageAttachmentIds(commaSeparatedString);
        }

        User sender = userRepository.findByAccountIdAndIsActive(senderId, true);

        User receiver = null;
        if (receiverId != null && receiverId>0) {
            receiver = userRepository.findByAccountIdAndIsActive(receiverId, true);
            if (receiver == null) {
                MessageResponse errorChatResponse =new MessageResponse();
                errorChatResponse.setContent("Invalid receiver accountId was provided.");
                TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                sendErrorMessageToUserSessions(userId, textMessage, session);
                return;
            }
            else if(!Objects.equals(sender.getOrgId(),receiver.getOrgId())) {
                MessageResponse errorChatResponse =new MessageResponse();
                errorChatResponse.setContent("The sender and the receiver do not belong to the same organisation.");
                TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                sendErrorMessageToUserSessions(userId, textMessage, session);
                return;
            }
        }
        if(groupId != null) {
            Optional<Group> groupDbOptional = groupRepository.findByGroupId(groupId);
            if(groupDbOptional.isPresent() && !groupDbOptional.get().getIsActive()) {
                sendErrorMessageToUserSessions(sender.getUserId(), new TextMessage("WARN: You cannot send message to an INACTIVE Group!!"), session);
                return;
            }
        }

        switch (action) {
            case NEW:
            case GROUP:

                validateRepliedToMessage(chatMessage, userId, session);

                chatMessage = messageRepository.save(chatMessage);
                chatMessage = messageRepository.findByMessageId(chatMessage.getMessageId()).get();
                if (chatMessage.getReplyId() != null) {
                    Optional<Message> repliedMessage = messageRepository.findByMessageIdAndIsDeleted(chatMessage.getReplyId(), false);
                    if (repliedMessage.isPresent()) {
                        chatMessage.setReply(repliedMessage.get());
                        //For replying to a message that is replying to another message.
                        if (chatMessage.getReply().getReply() != null)
                            chatMessage.getReply().setReply(null);
                    } else {
                        chatMessage.setReply(null);
                        sendErrorMessageToUserSessions(userId, new TextMessage("WARN: You cannot reply to a deleted message!!"), session);
                        return;
                    }
                }

                //Saving messageId reference back into the MessageAttachment Table.
                try {
                    if(chatMessage.getMessageAttachmentIds()!=null && !chatMessage.getMessageAttachmentIds().isEmpty()){
                        messageAttachmentRepository.updateMessageIdByMessageAttachmentIdIn(attachmentIdList, chatMessage.getMessageId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                MessageUser messageUser = new MessageUser();
                MessageUserId messageUserId = new MessageUserId(chatMessage.getMessageId(), sender.getAccountId());
                messageUser.setId(messageUserId);
                messageUser.setMessage(chatMessage);
                messageUser.setUser(sender);
                messageUser.setIsDelivered(true);
                messageUser.setIsRead(true);
                messageUser.setDeliveredAt(LocalDateTime.now());
                messageUser.setReadAt(LocalDateTime.now());
                messageUser = messageUserRepository.save(messageUser);

                //marking the isRead and isDelivered flag to true to all other messages that have been sent by the user.
//                chatMessage.getMessageUsers().stream().forEach(messageUser1 -> {
//                    if (messageUser1.getId().getAccountId().equals(sender.getAccountId())) {
//                        messageUser1.setIsDelivered(true);
//                        messageUser1.setIsRead(true);
//                    }
//                });

                if(receiver!=null)
                    sendMessageToReceiver(chatMessage, userId, receiver.getUserId());
                else
                    sendMessageToGroup(chatMessage, groupId, session);
                break;

            case EDIT:
                if (messageId != null) {
                    Optional<Message> existingMessage = messageRepository.findByMessageId(messageId);
                    if (existingMessage.isPresent() && existingMessage.get().getSenderId().equals(senderId) && !existingMessage.get().getIsDeleted() && !existingMessage.get().getTimestamp().plusMinutes(updateMessageTime).isBefore(LocalDateTime.now())) {
                        Message updatedMessage = existingMessage.get();
                        updatedMessage.setContent(content);
                        updatedMessage.setIsEdited(true);

                        // Edited message should show original timestamp
//                        updatedMessage.setTimestamp(LocalDateTime.now());
                        updatedMessage = messageRepository.save(updatedMessage);
                        if(receiver!=null)
                            sendMessageToReceiver(updatedMessage, userId, receiver.getUserId());
                        else
                            sendMessageToGroup(updatedMessage, groupId, session);
                    }
                    else {
                        MessageResponse errorChatResponse = new MessageResponse();
                        errorChatResponse.setContent("Invalid messageId was provided or the message was deleted.");
                        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                        sendErrorMessageToUserSessions(userId, textMessage, session);
                        return;
                    }
                }
                break;

            case DELETE:
                if (messageId != null) {
                    Optional<Message> messageToDelete = messageRepository.findByMessageId(messageId);
                    if (messageToDelete.isPresent() && messageToDelete.get().getSenderId().equals(senderId) && !messageToDelete.get().getIsDeleted() &&
                            !messageToDelete.get().getTimestamp().plusMinutes(updateMessageTime).isBefore(LocalDateTime.now())) {
                        Message deletedMessage = messageToDelete.get();
                        deletedMessage.setIsDeleted(true);
                        deletedMessage.setIsDelivered(true);
                        deletedMessage = messageRepository.save(deletedMessage);
                        deletedMessage.setContent("");
                        if(receiver!=null)
                            sendMessageToReceiver(deletedMessage, userId, receiver.getUserId());
                        else
                            sendMessageToGroup(deletedMessage, groupId, session);
                    }
                    else {
                        MessageResponse errorChatResponse = new MessageResponse();
                        errorChatResponse.setContent("Invalid messageId was provided or the message was deleted.");
                        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                        sendErrorMessageToUserSessions(userId, textMessage, session);
                        return;
                    }
                }
                break;

            case TAG:
                if (messageId != null) {
                    Optional<Message> messageForTag = messageRepository.findByMessageId(messageId);
                    if (messageForTag.isPresent()) {
                        String tagContent = jsonNode.get("tagContent").asText();
                        Tag tag = new Tag();
                        tag.setMessageId(messageForTag.get().getMessageId());
                        tag.setTagContent(tagContent);
                        tag.setTimestamp(LocalDateTime.now());
                        tagRepository.save(tag);
                    }
                }
                break;

            case "MARK_READ":
                if (messageId != null) {
                    Optional<Message> existingMessage = messageRepository.findByMessageId(messageId);
                    if (existingMessage.isPresent() && !existingMessage.get().getIsDeleted()) {
                        Message updatedMessage = existingMessage.get();

                        MessageUser messageUserToMarkRead = new MessageUser();
                        MessageUserId messageUserToMarkReadId = new MessageUserId(updatedMessage.getMessageId(), senderId);
                        messageUserToMarkRead.setId(messageUserToMarkReadId);
                        messageUserToMarkRead.setMessage(updatedMessage);
                        messageUserToMarkRead.setUser(sender);
                        messageUserToMarkRead.setIsDelivered(true);
                        messageUserToMarkRead.setIsRead(true);
                        messageUserToMarkRead = messageUserRepository.save(messageUserToMarkRead);
                        updatedMessage.getMessageUsers().stream().forEach(messageUser1 -> {
                            if (messageUser1.getId().getAccountId().equals(sender.getAccountId())) {
                                messageUser1.setIsDelivered(true);
                                messageUser1.setIsRead(true);
                            }
                        });

                        if(receiver!=null)
                            sendMessageToReceiver(updatedMessage, userId, receiver.getUserId());
                        else
                            sendMessageToGroup(updatedMessage, groupId, session);
                    }
                    else {
                        MessageResponse errorChatResponse = new MessageResponse();
                        errorChatResponse.setContent("Invalid messageId was provided or the message was deleted.");
                        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                        sendErrorMessageToUserSessions(userId, textMessage, session);
                        return;
                    }
                }
                break;
            case REACT:
                if(messageId !=null) {
                    Optional<Message> existingMessage = messageRepository.findByMessageId(messageId);
                    if (existingMessage.isPresent() && !existingMessage.get().getIsDeleted()) {
                        Message updatedMessage = existingMessage.get();

                        MessageUser messageUserToMarkRead = new MessageUser();
                        MessageUserId messageUserToMarkReadId = new MessageUserId(updatedMessage.getMessageId(), senderId);
                        messageUserToMarkRead.setId(messageUserToMarkReadId);
                        messageUserToMarkRead.setMessage(updatedMessage);
                        messageUserToMarkRead.setUser(sender);
                        messageUserToMarkRead.setIsDelivered(true);
                        messageUserToMarkRead.setIsRead(true);
                        messageUserToMarkRead = messageUserRepository.save(messageUserToMarkRead);
                        updatedMessage.getMessageUsers().stream().forEach(messageUser1 -> {
                            if (messageUser1.getId().getAccountId().equals(sender.getAccountId())) {
                                messageUser1.setIsDelivered(true);
                                messageUser1.setIsRead(true);
                            }
                        });

                        if(receiver!=null)
                            sendMessageToReceiver(updatedMessage, userId, receiver.getUserId());
                        else
                            sendMessageToGroup(updatedMessage, groupId, session);
                    }
                    else {
                        MessageResponse errorChatResponse = new MessageResponse();
                        errorChatResponse.setContent("Invalid messageId was provided or the message was deleted.");
                        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
//                        sessions.get(userId).sendMessage(textMessage);
                        sendErrorMessageToUserSessions(userId, textMessage, session);
                    }
                }
                break;
            //when a user receives the message will send the Acknowledgement
            case DELIVERY_ACK:
                // this acknowledgement to send via receiver of a message sent by sender.
                if (messageId != null && senderId != null) {
                    if(groupId != null && groupId >= 0) {
                        deliveryAckScheduler.addMessageAckValue(senderId, DELIVERY_ACK, messageId, GROUP, groupId);
                    } else {
                        deliveryAckScheduler.addMessageAckValue(senderId, DELIVERY_ACK, messageId, DIRECT, receiverId);
                    }
                }
                else {
                    MessageResponse errorChatResponse = new MessageResponse();
                    errorChatResponse.setContent("Invalid messageId was provided or the message was deleted.");
                    TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                    sendErrorMessageToUserSessions(userId, textMessage, session);
                }
                break;

            case READ_ACK:
                if (messageId != null && senderId != null) {
                    if(groupId != null && groupId >= 0) {
                        deliveryAckScheduler.addMessageAckValue(senderId, READ_ACK, messageId, GROUP, groupId);
                    } else {
                        deliveryAckScheduler.addMessageAckValue(senderId, READ_ACK, messageId, DIRECT, receiverId);
                    }
                } else {
                    MessageResponse errorChatResponse = new MessageResponse();
                    errorChatResponse.setContent("Invalid messageId was provided or the message was deleted.");
                    TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                    sendErrorMessageToUserSessions(userId, textMessage, session);
                }
                break;

            case INDICATOR:
                ActivityStatus request = new ActivityStatus();
                if(jsonNode.hasNonNull("status")){
                    request.setStatus(IndicatorStatus.getStatusMessageCollection().contains(jsonNode.get("status").asText()) ? jsonNode.get("status").asText() : null);
                    request.setUserId(jsonNode.get("userId").asLong());
                }
                if(jsonNode.hasNonNull("statusId")){
                    request.setStatusId(IndicatorStatus.getStatusIdCollection().contains(jsonNode.get("statusId").asInt()) ? jsonNode.get("statusId").asInt() : -1);
                    request.setCustomStatusMessage(jsonNode.has("customStatusMessage") ? jsonNode.get("customStatusMessage").asText() : "");
                }
                if(request.getStatus() == null || request.getStatusId() < 0){
                    throw new NotFoundException("Requested status and statusId for Activity Status is not found");
                }
                broadcastActivityStatusToOrgMember(request, session);
                break;

            default:
                SystemMessageResponse errorChatResponse = new SystemMessageResponse();
                errorChatResponse.setContent("Invalid Action Type!!");
                TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                sendErrorMessageToUserSessions(userId, textMessage, session);
        }
    }

    private void sendMessageToReceiver(Message message,Long senderUserId, Long receiverUserId) {
        try {
            MessageResponse chatMessage = new MessageResponse();
            BeanUtils.copyProperties(message, chatMessage);

            //messageStats from sender side.
            MessageStats messageStats = new MessageStats();
            messageStats.setMessageId(message.getMessageId());
            messageStats.setDeliveredCount(1);
            messageStats.setReadCount(1);
            messageStats.setSenderId(message.getSenderId());
            messageStats.setGroupSize(2);
            messageStatsRepository.save(messageStats);

            chatMessage.setFileMetadataList(fileUtils.addFileMetaData(message.getMessageAttachmentIds()));
            if (chatMessage.getReceiverId() != null) {
                Optional<List<SessionsWithTimeZone>> receiverSessions = Optional.ofNullable(userSessions.get(receiverUserId));
                Optional<List<SessionsWithTimeZone>> senderSessions = Optional.ofNullable(userSessions.get(senderUserId));

                User receiver = userRepository.findByAccountId(message.getReceiverId());
                MessageUser messageUser = getMessageUserObject(message, receiver);
                messageUserRepository.save(messageUser);

                if(receiverSessions.isPresent()){
                    boolean isAnySessionOpen = receiverSessions.get().stream().anyMatch(socket-> socket.getSession().isOpen());
                    if(isAnySessionOpen){
                        receiverSessions.get().forEach(socket-> {
                            if(socket.getSession().isOpen()){
                                String timeZone = socket.getTimeZone();
                                setReplyAndConvertToUserTimeZone(message, chatMessage, timeZone);
                                try {
                                    TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(chatMessage));
                                    socket.getSession().sendMessage(textMessage);
                                } catch (IOException e) {
                                    System.err.println("Error sending message to receiver: " + e.getMessage());
                                }
                            }
                        });
                    }
                }
                //Sending responseMessage to sender when sender!=receiver(not self message) and when receiver is Offline
                if (senderSessions.isPresent()) {
                    boolean isAnyCommonSession = senderSessions.get().stream().anyMatch(sender ->
                            receiverSessions.isPresent() && receiverSessions.get().stream().anyMatch(receiverSession -> receiverSession.getSession().equals(sender.getSession())));
                    if (!isAnyCommonSession) {
                        boolean isMessageSend = false;
                        for (SessionsWithTimeZone socket : senderSessions.get()) {
                            if (socket.getSession().isOpen()) {
                                try {
                                    String timeZone = socket.getTimeZone();
                                    setReplyAndConvertToUserTimeZone(message, chatMessage, timeZone);
                                    TextMessage textMessageResponse = new TextMessage(objectMapper.writeValueAsString(chatMessage));
                                    socket.getSession().sendMessage(textMessageResponse);
                                    isMessageSend = true;
                                } catch (IOException e) {
                                    System.err.println("Error sending response to sender: " + e.getMessage());
                                }
                            }
                        }
                        if(isMessageSend) {fcmClient.formatPayloadForDirectMessages(message, receiverUserId, urlHeadersMap.get(senderUserId));}
                    }
                }
            }
        } catch (BeansException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageUser getMessageUserObject(Message message, User receiver) {
        MessageUserId messageUserId = new MessageUserId(message.getMessageId(), receiver.getAccountId());
        MessageUser messageUser = new MessageUser();
        messageUser.setId(messageUserId);
        messageUser.setMessage(message);
        messageUser.setUser(receiver);
        return messageUser;
    }

    private void sendMessageToGroup(Message message, Long groupId, WebSocketSession session) {
        MessageResponse chatMessage = new MessageResponse();
        BeanUtils.copyProperties(message, chatMessage);
        chatMessage.setFileMetadataList(fileUtils.addFileMetaData(message.getMessageAttachmentIds()));

        if (groupId != null) {
            Group groupDb = groupRepository.findGroupWithUsers(groupId);
            User senderUser = userRepository.findByAccountId(message.getSenderId());

            // If message is deleted
            if (message.getIsDeleted()) {
                groupDb.setLastMessage(Constants.NotificationMessageContants.DELETED_MESSAGE);
                groupDb.setLastMessageSenderAccountId(message.getSenderId());
                groupDb.setLastMessageId(message.getMessageId());
                groupDb.setLastMessageTimestamp(message.getTimestamp());
            } else if (groupDb.getLastMessageId() == null || (message.getIsEdited() && Objects.equals(groupDb.getLastMessageId(), message.getMessageId())) || groupDb.getLastMessageId() < message.getMessageId()) {
                String content = Jsoup.parse(message.getContent()).text();
                groupDb.setLastMessage(Jsoup.parse(content).text().substring(0, Math.min(content.length(), 150)));
                groupDb.setLastMessageSenderAccountId(message.getSenderId());
                groupDb.setLastMessageId(message.getMessageId());
                groupDb.setLastMessageTimestamp(message.getTimestamp());
            }
            groupDb = groupRepository.save(groupDb);
            MessageStats messageStats = new MessageStats();
            messageStats.setMessageId(message.getMessageId());
            messageStats.setGroupSize(groupDb.getGroupUsers().size());
            messageStats.setGroupId(groupId);
            messageStats.setDeliveredCount(1);
            messageStats.setReadCount(1);
            messageStats.setSenderId(senderUser.getAccountId());
            messageStatsRepository.save(messageStats);

            Set<Long> mentions = new HashSet<>();
            if(chatMessage.getContent()!=null) {
                mentions = processMessageForMentions(message, groupDb);
            }

            List<GroupUser> activeGroupUser = groupDb.getGroupUsers().stream().filter(groupUser -> !groupUser.getIsDeleted()).collect(Collectors.toList());
            List<MessageUser> messageUserList = new ArrayList<>();
            for (GroupUser groupUser : activeGroupUser)  {
                User user = groupUser.getUser();
                if (!Objects.equals(message.getSenderId(), user.getAccountId())) {
                    messageUserList.add(getMessageUserObject(message, user));
                }

                if(userSessions.containsKey(user.getUserId())) {
                    boolean isAnySessionOpen = userSessions.get(user.getUserId()).stream().anyMatch(socket -> socket.getSession().isOpen());
                    if (isAnySessionOpen) {
                        userSessions.get(user.getUserId()).forEach(socket -> {
                            try {
                                if (socket.getSession().isOpen()) {
                                    String timeZone = socket.getTimeZone();
                                    setReplyAndConvertToUserTimeZone(message, chatMessage, timeZone);
                                    socket.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
                                }
                            } catch(IOException e){
                                System.err.println("Error sending message to group member: " + e.getMessage());
                            }
                        });
                    }
                }
            }
            messageUserRepository.saveAll(messageUserList);
            fcmClient.formatPayloadForGroupMessages(message, groupId, urlHeadersMap.get(senderUser.getUserId()), mentions);
        }
    }

    private void sendErrorToClient(String errorMessage, String fileName) {
        try {
            // Example of sending an error message back to the client
            //ToDO: Need to Review this commented Code.
            TextMessage errorMsg = new TextMessage("{\"error\":\"" + errorMessage + "\",\"fileName\":\"" + fileName + "\"}");
//            for (WebSocketSession session : sessions.values()) {
//                if (session.isOpen()) {
//                    session.sendMessage(errorMsg);
//                }
//            }
        } catch (Exception e) {
            System.err.println("Error sending error message to client: " + e.getMessage());
        }
    }

    private void validateRepliedToMessage(Message chatMessage, Long userId, WebSocketSession session) throws IOException {
        if (chatMessage.getReplyId() == null)
            return;
        Optional<Message> repliedMessage = messageRepository.findByMessageId(chatMessage.getReplyId());
        if (repliedMessage.isPresent()) {
            if (repliedMessage.get().getGroupId() != null) {
                Group group = groupRepository.findGroupWithUsers(repliedMessage.get().getGroupId());
                if (group==null) {
                    MessageResponse errorChatResponse = new MessageResponse();
                    errorChatResponse.setContent("User is not part of this group.");
                    TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                    sendErrorMessageToUserSessions(userId, textMessage, session);
                    throw new RuntimeException("Replied message group is invalid");
                }
                if (group.getGroupUsers().stream().noneMatch(groupUser -> groupUser.getUser().getAccountId().equals(chatMessage.getSenderId()))) {
                    MessageResponse errorChatResponse = new MessageResponse();
                    errorChatResponse.setContent("User is not part of this group.");
                    TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                    sendErrorMessageToUserSessions(userId, textMessage, session);
                    throw new RuntimeException("Cannot reply to this message.");
                }
            }
            else if (Objects.equals(chatMessage.getSenderId(), repliedMessage.get().getSenderId()) || Objects.equals(chatMessage.getSenderId(), repliedMessage.get().getReceiverId())) {
                return;
            } else {
                MessageResponse errorChatResponse = new MessageResponse();
                errorChatResponse.setContent("User is not part of this group.");
                TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(errorChatResponse));
                sendErrorMessageToUserSessions(userId, textMessage, session);
                throw new RuntimeException("Cannot reply to this message.");
            }
        }
    }

    private void setReplyAndConvertToUserTimeZone(Message message, MessageResponse chatMessage, String timeZone) {
        if(message.getReply()!=null) {
            MessageResponse replyMessage = new MessageResponse();
            BeanUtils.copyProperties(message.getReply(), replyMessage);
            replyMessage.setTimestamp(DateTimeUtils.convertServerDateToUserTimezone(message.getReply().getTimestamp(), timeZone).toString());
            chatMessage.setReply(replyMessage);
        }
        chatMessage.setTimestamp(DateTimeUtils.convertServerDateToUserTimezone(message.getTimestamp(), timeZone).toString());
    }

    public static String getParamFromURI(URI uri, String paramName) throws Exception {
        String query = uri.getQuery();

        if (query == null) {
            return null;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");

            String key = URLDecoder.decode(keyValue[0], "UTF-8");
            String value = (keyValue.length > 1) ? URLDecoder.decode(keyValue[1], "UTF-8") : "";

            if (key.equals(paramName)) {
                return value;
            }
        }
        return null;
    }

    public Page<Message> markMessagesAsReadAndPingReceiver(Page<Message> messages, Long senderId, Long receiverId) {
        User receiver = userRepository.findByAccountId(receiverId);

        AtomicBoolean changeReadStatus = new AtomicBoolean(false);
        messages.getContent().forEach(message -> {
            MessageUser messageUser = new MessageUser();
            MessageUserId messageUserId = new MessageUserId(message.getMessageId(), receiver.getAccountId());
            messageUser.setId(messageUserId);
            messageUser.setMessage(message);
            messageUser.setUser(receiver);
            messageUser.setIsDelivered(true);
            messageUser.setIsRead(true);
            messageUserRepository.save(messageUser);
            changeReadStatus.set(true);
        });

        if(changeReadStatus.get()) {
            User user = userRepository.findByAccountId(receiverId);
            User sender = userRepository.findByAccountIdAndIsActive(senderId, true);
            pingRecipientUsers(user, messages, sender);
        }
        return messages;
    }

    public void markMessagesAsReadAndPingGroupMembers(Page<Message> messages, User viewer, Long groupId) {

        Group groupDb = groupRepository.findByGroupId(groupId).get();

        AtomicBoolean changeReadStatus = new AtomicBoolean(false);
        messages.getContent().forEach(message -> {
            MessageUser messageUser = new MessageUser();
            MessageUserId messageUserId = new MessageUserId(message.getMessageId(), viewer.getAccountId());
            messageUser.setId(messageUserId);
            messageUser.setMessage(message);
            messageUser.setUser(viewer);
            messageUser.setIsDelivered(true);
            messageUser.setIsRead(true);
            messageUserRepository.save(messageUser);
            changeReadStatus.set(true);
        });

        if(changeReadStatus.get()) {
            for (GroupUser groupUser : groupDb.getGroupUsers()) {
                User user = groupUser.getUser();
                pingRecipientUsers(user, messages, viewer);
            }
        }
    }

    public void pingRecipientUsers (User user, Page<Message> messages, User viewer) {
        if(userSessions.containsKey(user.getUserId())
                && userSessions.get(user.getUserId()).stream().anyMatch(socket -> socket.getSession().isOpen())) {
            userSessions.get(user.getUserId()).stream().forEach(socket -> {
                if (socket.getSession().isOpen()) {
                    try {
                        MessageResponse chatMessage = new MessageResponse();
                        chatMessage.setMessageId(messages.getContent().get(messages.getContent().size() - 1).getMessageId());
                        chatMessage.setSenderId(viewer.getAccountId());
                        chatMessage.setMessageType("SYSTEM");
                        chatMessage.setContent("All messages up to messageId = " + messages.getContent().get(messages.getContent().size() - 1).getMessageId() + " have been read by senderId = " + viewer.getAccountId() + ".");
                        socket.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
                    } catch (IOException e) {
                        log.error("Error sending message to group member: " + e.getMessage());
                    }
                }
            });
        }

    }

    public Set<Long> processMessageForMentions(Message message, Group group) {
        HashSet<Long> accountIds = new HashSet<>();
        String messageContent = message.getContent();
        messageContent = messageContent.replace("\\&quot;", "").replace("\\\"", "\"");

        Document doc = Jsoup.parse(messageContent);
        // Extract all span tags with the class 'user-tag'
        Elements taggedUsers = doc.select("span.user-tag");
        for (Element tag : taggedUsers) {
            String userEmail = tag.attr("data-email");
            Long accountId = Long.parseLong(tag.attr("data-accountid"));
            String nameWithAt = tag.text().trim(); // Get the name including '@'

            // Remove the '@' symbol from the beginning of the name
            if (nameWithAt.startsWith("@")) {
                nameWithAt = nameWithAt.substring(1);
            }
            if(accountId.equals(-1L)) {
                return group.getGroupUsers().stream().map(groupUser -> groupUser.getUser().getAccountId()).collect(Collectors.toSet());
            }
            String[] nameWords = nameWithAt.split("\\s+");

            // Assuming the first word is the first name and the last word is the last name
            String firstName = nameWords[0];
            String lastName = nameWords[nameWords.length - 1];

            User user = null;

            if(accountId.equals(0L)) {
                accountIds.addAll(group.getUsers().stream().map(User::getAccountId).collect(Collectors.toList()));
            }
            else
                user = userRepository.findByAccountIdAndIsActive(accountId, true);

            // Verify the extracted name against the user account's first name, last name, and middle name
            if (user != null) {
                if (!firstName.equalsIgnoreCase(user.getFirstName()) || !lastName.equalsIgnoreCase(user.getLastName())) {
                    throw new ValidationFailedException("Tagged user's name doesn't match");
                }
            } else {
                throw new NotFoundException("No user account found for the tagged user");
            }
            accountIds.add(accountId);
        }

        return accountIds;
    }

    public void pingRecipientUsersForAttachmentMessage(Message message) {
        if(message.getReceiverId()!=null) {
            pingUserWithAccountId(message, message.getReceiverId());
            pingUserWithAccountId(message, message.getSenderId());
        }
        else {
            List<Long> userIds = userRepository.findUserIdsByGroupId(message.getGroupId());
            for (Long userId : userIds){
                if(userSessions.containsKey(userId)
                        && userSessions.get(userId).stream().anyMatch(socket -> socket.getSession().isOpen())){
                    userSessions.get(userId).stream().forEach(socket -> {
                        if (socket.getSession().isOpen()) {
                            try {
                                socket.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                            } catch (IOException e) {
                                System.err.println("Error sending message to group member: " + e.getMessage());
                            }
                        }
                    });
                }
            }
        }
    }

    public void pingUserWithAccountId (Message message, Long accountId) {
        User user = userRepository.findByAccountId(accountId);
        if(userSessions.containsKey(user.getUserId())
                && userSessions.get(user.getUserId()).stream().anyMatch(socket -> socket.getSession().isOpen())){
            userSessions.get(user.getUserId()).stream().forEach(socket -> {
                if (socket.getSession().isOpen()) {
                    try {
                        socket.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                    } catch (IOException e) {
                        System.err.println("Error sending message to userId: " + e.getMessage());
                    }
                }
            });
        }
    }

    private void sendErrorMessageToUserSessions(Long userId, TextMessage textMessage, WebSocketSession session){
        //It sends socket error message only to that user's session.
        userSessions.get(userId).forEach(socket -> {
            try {
                if(socket.getSession().equals(session)){
                    socket.getSession().sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.error("Error sending message ping: " + e.getMessage());
            }
        });
    }

    private List<Long> convertToList(JsonNode jsonNode, String node) {
        List<Long> longList = new ArrayList<>();
        if (jsonNode.get(node).isArray()) {
            for (JsonNode element : jsonNode.get(node)) {
                if (element.isNumber()) {
                    longList.add(element.asLong());
                }
            }
        }
        return longList;
    }

    public void sendAcknowledgementReceivedStatus(String statusType, Long senderId, ReadReceiptsResponse readReceiptsResponse) {
        Long userId = userRepository.findUserIdByAccountId(senderId);

        Optional<List<SessionsWithTimeZone>> sessions = Optional.ofNullable(userSessions.get(userId));
        sessions.ifPresent(sessionsWithTimeZones -> sessionsWithTimeZones.forEach(socket -> {
            if (socket.getSession().isOpen()) {
                try {
                    TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(readReceiptsResponse));
                    socket.getSession().sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Error sending status update: " + statusType + " " + e.getMessage());
                }
            }
        }));
    }

    /* No longer need as Read Receipt implementation been changed.
    public void sendUnreadMessageNotification(Long receiverId, Long senderId, Long groupId, String sentType){
        List<Message> unreadMessages = null;
        boolean isDelivered = sentType.equals(DELIVERED);
        if (groupId != null && groupId >0 ) {
            unreadMessages = messageRepository.findMessageUserBySenderIdInGroupAndIsDelivered(List.of(groupId), isDelivered);
        }
        else if (receiverId != null && receiverId >0 ) {
            unreadMessages = messageRepository.findAllBySenderIdInIsReadAndAccountId(senderId, List.of(receiverId), isDelivered);
        }

        if (unreadMessages != null) {
            Map<Long, List<Long>> senderMessageIdMap = unreadMessages.stream().collect(Collectors.groupingBy(Message::getSenderId, Collectors.mapping(Message::getMessageId, Collectors.toList())));
            for (Map.Entry<Long, List<Long>> senderEntry : senderMessageIdMap.entrySet()) {
                for (Long messId : senderEntry.getValue()) {
                    MessageUserId id = new MessageUserId(messId, senderId);
                    MessageUser messageUserToAckRead = messageUserRepository.findById(id).orElseThrow();
                    messageUserToAckRead.setIsDelivered(true);
                    messageUserToAckRead.setDeliveredAt(LocalDateTime.now());
                    if (!isDelivered) {
                        messageUserToAckRead.setIsRead(true);
                        messageUserToAckRead.setReadAt(LocalDateTime.now());
                    }
                    messageUserRepository.save(messageUserToAckRead);
                }
//                sendStatusUpdate(senderEntry.getKey(), sentType, senderEntry.getValue(), senderId, groupId);
            }
        }
    } */

    private void broadcastActivityStatusToOrgMember(ActivityStatus activityStatus, WebSocketSession session) throws IOException {
        Long userId = activityStatus.getUserId();
        List<Long> userIds = userRepository.findUserIdsByOrgIdsOfUserId(userId);
        // sending ping to all users in the same org and who are active to all its devices.
        for(Long userIdToSendPing : userIds) {
            if(userSessions.containsKey(userIdToSendPing)) {
                userSessions.get(userIdToSendPing).forEach(socket -> {
                    if(socket.getSession().isOpen()) {
                        try {
                            socket.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(new SocketFormatter(INDICATOR, activityStatus))));
                        } catch (Exception e) {
                            System.err.println("Error sending message to group member: " + e.getMessage());
                        }
                        activityIndicatorMap.put(userId, activityStatus);
                    }
                });
            }
        }
        if(activityStatus.getStatusId() != IndicatorStatus.OFFLINE.getIndicatorId()){
            //when user comes back online, this map would help to managed existing record
            if(session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new SocketFormatter(INDICATOR, activityIndicatorMap))));
            }
        }
    }
}
