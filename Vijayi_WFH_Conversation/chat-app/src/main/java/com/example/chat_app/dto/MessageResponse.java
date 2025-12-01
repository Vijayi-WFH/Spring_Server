package com.example.chat_app.dto;

import com.example.chat_app.model.MessageUser;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MessageResponse {

    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Long replyId;
//    private Boolean isDelivered = false;
//    private Boolean isRead = false;
    private Boolean isEdited = false;
    private Boolean isDeleted = false;
    private String timestamp;

    private Long groupId;

    private Long taskAttachmentId;
    private String messageAttachmentIds;
    private List<FileMetadata> fileMetadataList;

    private MessageResponse reply;

    private String messageType;
//    private List<MessageUser> messageUsers;
    private String tickStatus;
}
