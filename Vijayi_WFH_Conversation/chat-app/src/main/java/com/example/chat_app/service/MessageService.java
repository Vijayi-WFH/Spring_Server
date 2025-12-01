package com.example.chat_app.service;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.MessageResponse;
import com.example.chat_app.dto.MessageUserInfoRequest;
import com.example.chat_app.dto.MessageUserInfoResponse;
import com.example.chat_app.exception.ValidationFailedException;
import com.example.chat_app.model.*;
import com.example.chat_app.repository.*;
import com.example.chat_app.utils.DateTimeUtils;
import com.example.chat_app.utils.DeliveryAckScheduler;
import com.example.chat_app.utils.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private UserGroupEventRepository userGroupEventRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MessageUserRepository messageUserRepository;

    @Autowired
    private MessageStatsRepository messageStatsRepository;

    public Message createNewMessage(Message dbMessage, Long senderId, Long receiverId, String content) throws Exception {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        if(content == null)
            throw new Exception("Value must be at least 10");
        message.setContent(content);
        return message;
    }

    public Message editMessage() throws Exception {
        return new Message();
    }

    public List<Message> getMessagesByReceiverId(Long senderId, Long receiverId, String timeZone) {
        List<Message> messages = messageRepository.getMessagesBySenderIdAndReceiverId(senderId, receiverId);
        messages = messages.stream()
                .peek(message -> {
                    message.setTimestamp(DateTimeUtils.convertServerDateToUserTimezone(message.getTimestamp(), timeZone));
                    if(message.getIsDeleted()){
                        message.setContent("");
                        message.setMessageId(null);
                    }
                })
                .sorted(Comparator.comparing(Message::getTimestamp))
                .collect(Collectors.toList());
        return messages;
    }

    public Page<MessageResponse> getMessagesByReceiverId(Long senderId, Long receiverId, String timeZone, Long messageId, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Page<Message> messages;
        if (messageId == 0)
            messages = messageRepository.findBySenderIdAndReceiverId(senderId, receiverId, pageable);
        else
            messages = messageRepository.findByMessageIdLessThanAndSenderIdAndReceiverId(messageId, senderId, receiverId, pageable);

        List<Message> sortedMessages = messages.getContent().stream()
                .sorted(Comparator.comparing(Message::getTimestamp))
                .collect(Collectors.toList());

        List<Long> replyIds = messages.stream().map(Message::getReplyId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        List<MessageResponse> finalMessages = new ArrayList<>();
        sortedMessages.forEach(message -> {
            MessageResponse finalMessage = new MessageResponse();
            BeanUtils.copyProperties(message, finalMessage);
            finalMessage.setTimestamp(DateTimeUtils.convertServerDateToUserTimezone(message.getTimestamp(), timeZone).toString());
            if (finalMessage.getIsDeleted()) {
                finalMessage.setContent("");
            }
            finalMessages.add(finalMessage);
        });
        finalMessages.forEach(messageResponse ->
                messageResponse.setFileMetadataList(fileUtils.addFileMetaData(messageResponse.getMessageAttachmentIds())));
        if (!replyIds.isEmpty()) {
            setReplyMessagesToMessageDto(finalMessages, replyIds);
        }
        setMessageStatsInMessageResponse(finalMessages, senderId);

        return new PageImpl<>(finalMessages, pageable, messages.getTotalElements());
    }

    public List<Message> getMessagesByGroupId(Long groupId, String timeZone) {
        List<Message> messages = messageRepository.findByGroupIdOrderByTimestampDesc(groupId);
        messages = messages.stream()
                .peek(message -> {
                    message.setTimestamp(DateTimeUtils.convertServerDateToUserTimezone(message.getTimestamp(), timeZone));
                    if (message.getIsDeleted()){
                        message.setContent("");
                    }
                })
                .sorted(Comparator.comparing(Message::getTimestamp))
                .collect(Collectors.toList());
        return messages;
    }

    public Page<MessageResponse> getMessagesByGroupId(Long messageId, Long accountId, Long groupId, String timeZone, int size, int pageNo) {

        Pageable pageable = PageRequest.of(pageNo, size, Sort.by("timestamp").descending());

        List<UserGroupEvent> userGroupEvents = userGroupEventRepository.findByAccountIdAndGroupIdOrderByOccurredAtAsc(accountId, groupId);
        Page<Message> messages = null;
        if (userGroupEvents.isEmpty()) {
            messages = messageRepository.findByGroupIdWithCursor(groupId, messageId, pageable);
        }
        else if (userGroupEvents.size() == 1 && userGroupEvents.get(0).getEventType().equals(Constants.EventType.LEAVE.getValue())) {
            Instant leaveTimestamp = userGroupEvents.get(0).getOccurredAt();
            messages = messageRepository.findByGroupIdAndTimestampAndMessageIdLessThanEqual(groupId, leaveTimestamp, messageId, pageable);
        } else {
                messages = messageRepository.findMessagesWithIntervals(accountId, groupId, messageId, pageable);
        }
        List<Message> sortedMessages = messages.stream()
                .sorted(Comparator.comparing(Message::getTimestamp))
                .collect(Collectors.toList());

        List<Long> replyIds = messages.getContent().stream().map(Message::getReplyId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        List<MessageResponse> finalMessages = new ArrayList<>();
        sortedMessages.forEach(message -> {
            MessageResponse finalMessage = new MessageResponse();
            BeanUtils.copyProperties(message, finalMessage);
            finalMessage.setTimestamp(DateTimeUtils.convertServerDateToUserTimezone(message.getTimestamp(), timeZone).toString());
            if (finalMessage.getIsDeleted()) {
                finalMessage.setContent("");
            }
            finalMessages.add(finalMessage);
        });
        if (!replyIds.isEmpty()) {
            setReplyMessagesToMessageDto(finalMessages, replyIds);
        }
        finalMessages.forEach(messageResponse ->
                messageResponse.setFileMetadataList(fileUtils.addFileMetaData(messageResponse.getMessageAttachmentIds())));

        setMessageStatsInMessageResponse(finalMessages, accountId);

        return new PageImpl<>(finalMessages, pageable, messages.getTotalElements());
    }

    private void setReplyMessagesToMessageDto(List<MessageResponse> messageDto, List<Long> replyIds){
        List<Message> messageListDb = messageRepository.findByMessageIdIn(replyIds);
        Map<Long, Message> messageMap = messageListDb.stream().collect(Collectors.toMap(Message::getMessageId, message -> message));
        messageDto = messageDto.parallelStream().filter(messageResponse -> messageResponse.getReplyId()!=null).collect(Collectors.toList());
        messageDto.forEach(messageResponse -> {
            MessageResponse messageResponse1 = new MessageResponse();
            BeanUtils.copyProperties(messageMap.get(messageResponse.getReplyId()), messageResponse1);
            messageResponse.setReply(messageResponse1);
        });
    }

    public MessageUserInfoResponse getMessageUserInfo(MessageUserInfoRequest userInfoRequest, String timezone) {
        Map<Long, LocalDateTime> readUsers = new HashMap<>();
        Map<Long, LocalDateTime> deliveredUsers = new HashMap<>();
        List<Long> noInfoUser = new ArrayList<>();
        Message messageDb = messageRepository.findByMessageId(userInfoRequest.getMessageId()).orElseThrow(() -> new ValidationFailedException("Invalid messageId is provided in request."));
        if (userInfoRequest.getGroupId() != null) {
            if (!messageDb.getGroupId().equals(userInfoRequest.getGroupId())) {
                throw new ValidationFailedException("MessageId from request belongs to different Group.");
            }
        }
        if(!Objects.equals(messageDb.getSenderId(), userInfoRequest.getSenderId())) {
            throw new ValidationFailedException("MessageInfo can only be requested by the Sender Only!");
        }

        List<MessageUser> messageUserList = messageUserRepository.findByMessageId(userInfoRequest.getMessageId());
        for (MessageUser messageUser : messageUserList) {
            LocalDateTime deliveredAt = DateTimeUtils.convertServerDateToUserTimezone(messageUser.getDeliveredAt(), timezone);
            LocalDateTime readAt = DateTimeUtils.convertServerDateToUserTimezone(messageUser.getReadAt(), timezone);
            if (messageUser.getIsDelivered() && messageUser.getIsRead()) {
                readUsers.put(messageUser.getUser().getAccountId(), readAt);
            } else if (messageUser.getIsDelivered()) {
                deliveredUsers.put(messageUser.getUser().getAccountId(), deliveredAt);
            } else {
                noInfoUser.add(messageUser.getUser().getAccountId());
            }
        }

        return new MessageUserInfoResponse(readUsers, deliveredUsers, noInfoUser);
    }

    private void setMessageStatsInMessageResponse(List<MessageResponse> messages, Long receiverId) {
        List<Long> messageIdList = messages.stream().map(MessageResponse::getMessageId).collect(Collectors.toList());
        List<MessageStats> messageStatsList = messageStatsRepository.findAllByMessageIdIn(messageIdList, receiverId);
        Map<Long, MessageStats> messageStatsMap = messageStatsList.stream().collect(Collectors.toMap(MessageStats::getMessageId, messageStats -> messageStats));

        for(MessageResponse messageResponse : messages) {
            if(messageStatsMap.get(messageResponse.getMessageId()) != null) {
                MessageStats stats = messageStatsMap.get(messageResponse.getMessageId());
                String tickStatus = DeliveryAckScheduler.calculateTickStatus(stats.getDeliveredCount(), stats.getReadCount(), stats.getGroupSize());
                messageResponse.setTickStatus(tickStatus);
            }
        }
    }
}